/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.calculadora.cientifica.core;

/**
 * @author Capito
 * Normaliza y valida expresiones de la calculadora.
 * - Limpia espacios y símbolos visuales.
 * - Aplica multiplicación implícita de forma segura.
 * - Convierte variantes de funciones a la notación del engine (Symja).
 * - Valida paréntesis y vocabulario permitido.
 *
 * Nota: Se usan marcadores internos "§" para proteger funciones
 * durante la inserción de multiplicación implícita. Se eliminan antes de devolver.
 */
public class ExpressionValidator {

    /** Limpia, normaliza y aplica multiplicación implícita de forma segura. */
    public static String cleanExpression(String expr) {
        if (expr == null) return "";
        String cleaned = expr.trim();
        if (cleaned.isEmpty()) return "";

        // Normalización de espacios y glifos
        cleaned = cleaned
                .replace("\u00A0", "")           // NBSP
                .replace("\u200B", "")           // ZWSP
                .replace("\u200C", "")           // ZWNJ
                .replace("\u200D", "")           // ZWJ
                .replace("\uFEFF", "");          // BOM
        cleaned = cleaned.replaceAll("\\s+", "");

        // Normalización de símbolos
        cleaned = cleaned.replace(",", ".")
                         .replace("÷", "/")
                         .replace("×", "*")
                         .replace("∗", "*")
                         .replace("·", "*")
                         .replace("⁄", "/")
                         .replace("∕", "/")
                         .replace("／", "/")
                         .replace("√", "sqrt")
                         .replace("π", "pi")
                         .replace("Π", "pi")
                         .replace("−", "-")
                         .replace("－", "-")
                         .replace("-", "-")       // non-breaking hyphen
                         .replace("X", "x")
                         .replace("Y", "y");

        // Constante e (standalone). No afecta a 'exp' ni a números en notación científica.
        cleaned = cleaned.replaceAll("\\be\\b", "E");

        // |x| -> Abs(x) (repetido para anidamientos simples)
        String prev;
        do {
            prev = cleaned;
            cleaned = cleaned.replaceAll("\\|([^|]+)\\|", "Abs($1)");
        } while (!cleaned.equals(prev));

        // Nombres de funciones hacia el engine (con paréntesis)
        cleaned = cleaned.replaceAll("(?i)sin\\s*\\(", "Sin(")
                         .replaceAll("(?i)cos\\s*\\(", "Cos(")
                         .replaceAll("(?i)tan\\s*\\(", "Tan(")
                         .replaceAll("(?i)exp\\s*\\(", "Exp(")
                         .replaceAll("(?i)sqrt\\s*\\(", "Sqrt(")
                         .replaceAll("(?i)abs\\s*\\(", "Abs(")
                         .replaceAll("(?i)ln\\s*\\(", "Log(")            // ln(x) -> Log(x)
                         .replaceAll("(?i)log10\\s*\\(", "Log(10,")      // log10(x) -> Log(10,x)
                         .replaceAll("(?i)log\\s*\\(", "Log(");          // log(x) -> Log(x) (natural)

        // Protege funciones para no insertar '*' entre nombre y '('
        cleaned = cleaned.replaceAll("(Sin|Cos|Tan|Exp|Sqrt|Abs|Log)\\(", "$1§(");

        // Multiplicación implícita
        // 2x -> 2*x, x2 -> x*2
        cleaned = cleaned.replaceAll("(?<=[0-9])(?=[a-zA-Z])", "*");
        cleaned = cleaned.replaceAll("(?<=[a-zA-Z])(?=[0-9])", "*");

        // pi2, pix -> pi*2, pi*x ; E2, Ex -> E*2, E*x
        // Evitar E*xp§(  (protege 'Exp(')
        cleaned = cleaned.replaceAll("(?<=pi)(?=[0-9a-zA-Z])", "*");
        cleaned = cleaned.replaceAll("(?<=E)(?=[0-9a-zA-Z])(?!xp§\\()", "*");

        // xy -> x*y
        cleaned = cleaned.replaceAll("(?<=[xy])(?=[xy])", "*");

        // )x, )2 -> )*x, )*2
        cleaned = cleaned.replaceAll("(?<=\\))(?=[0-9a-zA-Z])", "*");

        // x( -> x*(  ;  2( -> 2*(  ;  )( -> )*(
        cleaned = cleaned.replaceAll("(?<=[0-9a-zA-Z\\)])(?=\\()", "*");

        // Restaura funciones protegidas y elimina cualquier marcador residual
        cleaned = cleaned.replace("§(", "(").replace("§", "");

        // Simplificación de signos
        cleaned = cleaned.replaceAll("\\+\\+", "+")
                         .replaceAll("--", "+")
                         .replaceAll("\\+-", "-")
                         .replaceAll("-\\+", "-");

        // Trim de '+' inicial redundante
        if (cleaned.startsWith("+")) cleaned = cleaned.substring(1);

        // Balanceo mínimo de paréntesis
        int open = countChar(cleaned, '(');
        int close = countChar(cleaned, ')');
        for (int i = 0; i < open - close; i++) cleaned += ")";

        return cleaned;
    }

    /** Valida gramática básica, símbolos permitidos y paréntesis. */
    public static boolean isValidExpression(String expr, String variable) {
        if (expr == null || expr.isEmpty()) return false;
        if (!("x".equals(variable) || "y".equals(variable))) return false;

        // Solo caracteres previstos (el marcador § ya no existe a estas alturas)
        if (!expr.matches("[0-9a-zA-Z+\\-*/^()._,]*")) return false;

        // Paréntesis balanceados
        if (countChar(expr, '(') != countChar(expr, ')')) return false;

        // Validación de identificadores
        String tmp = expr;

        // Elimina funciones conocidas (con o sin espacio)
        tmp = tmp.replaceAll("(?i)(Sin|Cos|Tan|Exp|Sqrt|Abs|Log)\\s*\\(", "(");
        tmp = tmp.replaceAll("(?i)\\b(Sin|Cos|Tan|Exp|Sqrt|Abs|Log)\\b", "");

        // Elimina constantes y variables permitidas
        tmp = tmp.replaceAll("(?i)pi|E", "");
        tmp = tmp.replaceAll("[xy]", "");

        // Elimina símbolos matemáticos permitidos
        tmp = tmp.replaceAll("[0-9+\\-*/^()._,]", "");

        // No deben quedar letras
        return !tmp.matches(".*[A-Za-z].*");
    }

    private static int countChar(String s, char c) {
        int n = 0;
        for (int i = 0; i < s.length(); i++) if (s.charAt(i) == c) n++;
        return n;
    }
}
