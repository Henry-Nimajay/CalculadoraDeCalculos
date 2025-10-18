/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.calculadora.cientifica.core;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Capito
 * Formatea y convierte expresiones entre notación de motor (Symja) y notación visual de la UI.
 * - Números: formato estable sin agrupación y con punto decimal.
 * - toDisplay: de engine → UI (π, √, ln, log10, ·, superíndices, fracciones).
 * - toEngine: de UI → engine (pi, sqrt, *, ^, /, E, Abs()).
 * - toHtmlDisplay: salida HTML ligera para previsualización (superíndices y fracciones apiladas).
 */
public class MathFormatter {

    private static final DecimalFormat DECIMAL_FORMAT;

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setDecimalSeparator('.');
        DecimalFormat df = new DecimalFormat("0.############", symbols);
        df.setGroupingUsed(false);
        DECIMAL_FORMAT = df;
    }

    /** Formato numérico estable para la UI. */
    public static String formatDouble(double value) {
        if (Double.isNaN(value)) return "NaN";
        if (Double.isInfinite(value)) return (value > 0) ? "+∞" : "-∞";
        // Evita "-0" por redondeo binario
        if (value == 0.0d) value = 0.0d;
        return DECIMAL_FORMAT.format(value);
    }

    /** Convierte de notación del engine a notación visual de la UI. */
    public static String toDisplay(String expr) {
        if (expr == null) return "";
        String s = expr.trim();
        if (s.isEmpty()) return "";

        // Abs(arg) -> |arg| (repetido para anidamientos simples sin romper paréntesis anidados profundos)
        String prev;
        do {
            prev = s;
            s = s.replaceAll("\\bAbs\\(([^()]+)\\)", "|$1|");
        } while (!s.equals(prev));

        // Constantes y funciones (orden: casos específicos antes de genéricos)
        s = s.replace("Pi", "π").replace("pi", "π");
        s = s.replaceAll("\\bE\\b", "e");
        s = s.replaceAll("(?i)sqrt\\s*\\(", "√(");
        s = s.replaceAll("\\bSin\\s*\\(", "sin(");
        s = s.replaceAll("\\bCos\\s*\\(", "cos(");
        s = s.replaceAll("\\bTan\\s*\\(", "tan(");
        s = s.replaceAll("\\bExp\\s*\\(", "exp(");
        // Primero Log(10,...) -> log10(...); luego Log(x) -> ln(x)
        s = s.replaceAll("\\bLog\\s*\\(\\s*10\\s*,", "log10(");
        s = s.replaceAll("\\bLog\\s*\\(", "ln(");

        // Multiplicación visual
        s = s.replace("*", "·");

        // Superíndices comunes
        s = s.replaceAll("\\^2", "²").replaceAll("\\^3", "³");

        // Compacta multiplicación solo dentro de exponentes ^(...)
        Pattern p = Pattern.compile("\\^\\(([^()]+)\\)");
        Matcher m = p.matcher(s);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String inner = m.group(1);
            String compact = inner.replaceAll("(?<=[0-9A-Za-zπe])·(?=[A-Za-zπe])", "");
            m.appendReplacement(sb, Matcher.quoteReplacement("^(" + compact + ")"));
        }
        m.appendTail(sb);
        s = sb.toString();

        // Fracciones visuales (variantes comunes)
        s = s.replaceAll("\\(([^()]+)\\)/\\(([^()]+)\\)", "($1) ⁄ ($2)");
        s = s.replaceAll("([^()]+)/\\(([^()]+)\\)", "$1 ⁄ ($2)");
        s = s.replaceAll("\\(([^()]+)\\)/([^()]+)", "($1) ⁄ $2");
        s = s.replaceAll("([A-Za-zπe][A-Za-z0-9πe^()·]+)\\s*/\\s*([0-9]+(?:\\.[0-9]+)?)", "$1 ⁄ $2");

        // Factorización: X ⁄ n -> (1 ⁄ n)·X  (mantiene legibilidad)
        s = s.replaceAll("(e\\^\\([^()]+\\))\\s*⁄\\s*([0-9]+(?:\\.[0-9]+)?)", "(1 ⁄ $2)·$1");
        s = s.replaceAll("(?<!\\(1 ⁄ [0-9.]+\\)·)([A-Za-zπe][A-Za-z0-9πe^()·]+)\\s*⁄\\s*([0-9]+(?:\\.[0-9]+)?)", "(1 ⁄ $2)·$1");

        // Espaciado final
        s = s.replaceAll("\\s+", " ").trim();
        return s;
    }

    /** Convierte de notación visual de la UI a notación del engine (Symja). */
    public static String toEngine(String expr) {
        if (expr == null) return "";
        String s = expr.trim();
        if (s.isEmpty()) return "";

        s = s.replace("π", "pi")
             .replace("√", "sqrt")
             .replace("·", "*")
             .replace("²", "^2")
             .replace("³", "^3")
             .replace("⁄", "/")
             .replace("−", "-")
             .replace("∫", "")
             .replace("Pi", "pi");

        // |x| -> Abs(x) (repetido para anidamientos simples)
        String prev;
        do {
            prev = s;
            s = s.replaceAll("\\|([^|]+)\\|", "Abs($1)");
        } while (!s.equals(prev));

        // ln( -> Log( (natural)
        s = s.replaceAll("(?i)\\bln\\s*\\(", "Log(");

        // Constante e de Euler como símbolo E cuando es aislada
        s = s.replaceAll("\\be\\b", "E");

        return s;
    }

    /** Normaliza variantes menores para previsualización textual en la UI. */
    public static String normalizeExpression(String expr) {
        if (expr == null) return "";
        String normalized = expr.trim();
        if (normalized.isEmpty()) return "";
        normalized = normalized.replace("**", "^")
                               .replaceAll("\\s+", " ")
                               .replaceAll("(?i)sqrt\\s*\\(", "√(");
        return normalized;
    }

    /** Genera HTML ligero para previsualización (superíndices reales y fracciones apiladas). */
    public static String toHtmlDisplay(String expr) {
        if (expr == null) return "<html></html>";
        String s = toDisplay(expr);
        s = escapeHtml(s);

        // Oculta el punto en multiplicación implícita en contextos comunes
        s = s.replaceAll("(?<=\\d)·(?=[a-zA-Zπ√(])", "");
        s = s.replaceAll("(?<=[a-zA-Zπ)])·(?=[a-zA-Zπ√(])", "");
        s = s.replaceAll("\\)·\\(", ")(");

        s = applyExponentHtml(s);
        s = applyStackedFractions(s);

        s = s.replace("|", "&#124;");

        return "<html><span class='math'>" + s + "</span></html>";
    }

    //  Internos HTML 

    private static String escapeHtml(String s) {
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private static String applyExponentHtml(String s) {
        s = s.replaceAll("e\\^\\(([^()]+)\\)", "e<sup>$1</sup>");
        s = s.replaceAll("([A-Za-z0-9π\\)])\\^\\(([^()]+)\\)", "$1<sup>$2</sup>");
        s = s.replaceAll("e\\^([A-Za-z0-9π])", "e<sup>$1</sup>");
        s = s.replaceAll("([A-Za-z0-9π\\)])\\^([A-Za-z0-9π])", "$1<sup>$2</sup>");
        s = s.replace("²", "<sup>2</sup>").replace("³", "<sup>3</sup>");
        return s;
    }

    private static String applyStackedFractions(String s) {
        s = s.replaceAll("\\(([^()]+)\\)\\s*⁄\\s*\\(([^()]+)\\)",
                "<span class='mfrac'><span class='num'>$1</span><span class='bar'></span><span class='den'>$2</span></span>");
        s = s.replaceAll("([^()\\s]+)\\s*⁄\\s*\\(([^()]+)\\)",
                "<span class='mfrac'><span class='num'>$1</span><span class='bar'></span><span class='den'>$2</span></span>");
        s = s.replaceAll("\\(([^()]+)\\)\\s*⁄\\s*([^()\\s]+)",
                "<span class='mfrac'><span class='num'>$1</span><span class='bar'></span><span class='den'>$2</span></span>");
        return s;
    }
}
