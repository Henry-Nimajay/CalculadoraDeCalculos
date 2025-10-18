/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.calculadora.cientifica.core;

/**
 * Servicio orquestador: valida entrada, normaliza variable y
 * delega en los motores simbólico y numérico.
 * 
 * Solo lanza IllegalArgumentException para errores de entrada.
 * No usa multi-catch.
 * @author Capito
 */
public class CalculationService {

    private final SymbolicEngine symbolicEngine = new SymbolicEngine();
    private final NumericEngine numericEngine   = new NumericEngine();

    // Tolerancia para considerar límites prácticamente iguales (escala relativa)
    private static final double EPS_EQ = 1e-12;

    /** Derivada simbólica d/d{variable} (expr). */
    public String derive(String expr, String variable) {
        String v = normalizeVar(variable);
        String e = preprocess(expr, v);
        return symbolicEngine.derive(e, v);
    }

    /** Integral indefinida ∫ (expr) d{variable}. */
    public String integrate(String expr, String variable) {
        String v = normalizeVar(variable);
        String e = preprocess(expr, v);
        return symbolicEngine.integrate(e, v);
    }

    /** Integral definida ∫_a^b (expr) d{variable}. Acepta a > b (invierte con signo). */
    public double integrateDefinite(String expr, String variable, double a, double b) {
        if (Double.isNaN(a) || Double.isNaN(b)) {
            throw new IllegalArgumentException("Los límites a y b deben ser numéricos.");
        }
        if (almostEqual(a, b)) {
            return 0.0d;
        }

        // Permitir a > b invirtiendo el intervalo con cambio de signo (convención estándar)
        boolean reversed = a > b;
        double lo = reversed ? b : a;
        double hi = reversed ? a : b;

        String v = normalizeVar(variable);
        String e = preprocess(expr, v);

        double value = numericEngine.integrateDefinite(e, v, lo, hi);

        // Defenderse de resultados no finitos (propagar como error de entrada/cálculo)
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            throw new IllegalArgumentException("La integral definida no pudo evaluarse de forma finita en [" + lo + ", " + hi + "].");
        }
        return reversed ? -value : value;
    }

    /** Sobrecarga que recibe límites como texto (π, pi/2, -e, etc.). */
    public double integrateDefinite(String expr, String variable, String aStr, String bStr) {
        double a = parseBound(aStr);
        double b = parseBound(bStr);
        return integrateDefinite(expr, variable, a, b);
    }

    /** Parser simple de límites numéricos con soporte para pi, e, fracciones y multiplicación básica. */
    public static double parseBound(String s) {
        if (s == null) throw new IllegalArgumentException("Límite requerido.");
        String t = sanitizeBoundString(s);
        if (t.isEmpty()) throw new IllegalArgumentException("Límite vacío.");

        // Prohibir infinitos (el motor numérico no soporta integrales impropias aquí)
        if (equalsIgnoreCaseAscii(t, "inf") || equalsIgnoreCaseAscii(t, "+inf") || "∞".equals(t)) {
            throw new IllegalArgumentException("Límite infinito no soportado: " + s);
        }
        if (equalsIgnoreCaseAscii(t, "-inf") || "−∞".equals(t)) {
            throw new IllegalArgumentException("Límite infinito no soportado: " + s);
        }

        // Quitar paréntesis envolventes
        if (t.startsWith("(") && t.endsWith(")") && t.length() > 2) {
            t = t.substring(1, t.length() - 1).trim();
        }

        // Fracción a nivel superior: n/d
        int slash = indexOfTopLevel(t, '/');
        if (slash > 0 && slash < t.length() - 1) {
            double n = parseBound(t.substring(0, slash));
            double d = parseBound(t.substring(slash + 1));
            if (d == 0.0d) throw new IllegalArgumentException("División por cero en el límite: " + s);
            return n / d;
        }

        // Multiplicación simple a nivel superior: a*b (incluye casos como 2*pi, pi*2)
        int star = indexOfTopLevel(t, '*');
        if (star > 0 && star < t.length() - 1) {
            double a = parseBound(t.substring(0, star));
            double b = parseBound(t.substring(star + 1));
            return a * b;
        }

        // Signo unario
        if (t.startsWith("+")) return parseBound(t.substring(1));
        if (t.startsWith("-")) return -parseBound(t.substring(1));

        // Coeficiente por constante al final: k*pi, k*e (también "pi" y "e" puros)
        if (equalsIgnoreCaseAscii(t, "pi")) return Math.PI;
        if (equalsIgnoreCaseAscii(t, "e"))  return Math.E;

        if (t.endsWith("pi")) {
            String k = t.substring(0, t.length() - 2);
            double coef = k.isEmpty() ? 1.0d : parseBound(k);
            return coef * Math.PI;
        }
        if (t.endsWith("e")) {
            String k = t.substring(0, t.length() - 1);
            double coef = k.isEmpty() ? 1.0d : parseBound(k);
            return coef * Math.E;
        }

        // Número directo
        try {
            return Double.parseDouble(t);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Límite inválido: " + s);
        }
    }

    /** Normalización + validación de expresión antes de delegar a los motores. */
    private String preprocess(String expr, String v) {
        String e = MathFormatter.toEngine(expr);
        e = ExpressionValidator.cleanExpression(e);
        if (!ExpressionValidator.isValidExpression(e, v)) {
            throw new IllegalArgumentException("Expresión inválida para la variable '" + v + "'.");
        }
        return e;
    }

    /** Solo 'x' o 'y'. */
    private String normalizeVar(String variable) {
        if (variable == null) throw new IllegalArgumentException("Variable requerida (x o y).");
        String v = variable.trim().toLowerCase();
        if (!("x".equals(v) || "y".equals(v))) {
            throw new IllegalArgumentException("Variable inválida: " + variable + " (use x o y).");
        }
        return v;
    }

    private static String sanitizeBoundString(String s) {
        // Normaliza separadores y símbolos comunes en los límites
        String t = s.trim()
                .replace("\u00A0", "")    // NBSP
                .replace(" ", "")
                .replace(",", ".")
                .replace("·", "*")        // multiplicación típica en UI → *
                .replace("∗", "*")        // asterisco matemático
                .replace("×", "*")        // multiplicación unicode
                .replace("⁄", "/")        // frac slash
                .replace("∕", "/")        // division slash
                .replace("π", "pi")
                .replace("−", "-");       // minus unicode
        return t;
    }

    private static boolean equalsIgnoreCaseAscii(String a, String b) {
        return a != null && b != null && a.length() == b.length() && a.equalsIgnoreCase(b);
    }

    private static int indexOfTopLevel(String t, char op) {
        int depth = 0;
        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
            if (c == '(') depth++;
            else if (c == ')') depth = Math.max(0, depth - 1);
            else if (c == op && depth == 0) return i;
        }
        return -1;
    }

    private static boolean almostEqual(double a, double b) {
        double scale = Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)));
        return Math.abs(a - b) <= EPS_EQ * scale;
    }
}

