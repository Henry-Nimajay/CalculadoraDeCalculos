/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.calculadora.cientifica.core;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.integration.SimpsonIntegrator;
import org.apache.commons.math3.exception.MaxCountExceededException;
import org.apache.commons.math3.exception.TooManyEvaluationsException;
import org.matheclipse.core.eval.ExprEvaluator;
import org.matheclipse.core.expression.F;
import org.matheclipse.core.interfaces.IExpr;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Capito
 * Motor numérico: integra de forma robusta en [a,b].
 * - Intenta simbólico con Symja (N(Integrate(...))) y si no es finito, cae a numérico.
 * - Maneja constantes, bordes no finitos y subdivisión adaptativa.
 * - Añade puntos de ruptura conocidos para |sin(kx)| y |cos(kx)|.
 */
public class NumericEngine {

    private final ExprEvaluator evaluator;
    private final SimpsonIntegrator integrator;

    // Límite de evaluaciones para Simpson
    private static final int MAX_EVAL  = 20_000;
    // Profundidad máxima de subdivisión adaptativa
    private static final int MAX_DEPTH = 10;

    public NumericEngine() {
        F.initSymbols();
        this.evaluator = new ExprEvaluator();

        double relTol = 1e-9;
        double absTol = 1e-12;
        int minIter = 8;
        int maxIter = 64;

        this.integrator = new SimpsonIntegrator(relTol, absTol, minIter, maxIter);
    }

    public double integrateDefinite(String expr, String variable, double a, double b) {
        if (expr == null || expr.isEmpty()) {
            throw new IllegalArgumentException("Expresión vacía.");
        }
        if (!("x".equals(variable) || "y".equals(variable))) {
            throw new IllegalArgumentException("Variable inválida (use x o y).");
        }
        if (!Double.isFinite(a) || !Double.isFinite(b)) {
            throw new IllegalArgumentException("Límites a y b deben ser finitos.");
        }
        if (a == b) {
            return 0.0d;
        }

        final String cleaned = ExpressionValidator.cleanExpression(expr);
        final double lower = Math.min(a, b);
        final double upper = Math.max(a, b);
        final double sign  = (a <= b) ? 1.0 : -1.0;

        // Expresión constante (no depende de la variable)
        if (!cleaned.contains(variable)) {
            double c = evalNumeric(cleaned);
            if (!Double.isFinite(c)) {
                throw new IllegalArgumentException("La expresión constante es no finita.");
            }
            return sign * c * (upper - lower);
        }

        // Intento simbólico directo
        double symbolic = trySymbolicDefinite(cleaned, variable, lower, upper);
        if (Double.isFinite(symbolic)) {
            return sign * symbolic;
        }

        // Función evaluable f(t)
        final UnivariateFunction f = new UnivariateFunction() {
            @Override public double value(double t) { return evalAt(cleaned, variable, t); }
        };

        // Puntos de ruptura sugeridos (kinks) para estabilizar integrales con |sin(kx)| o |cos(kx)|
        List<Double> breaks = suggestedBreakpoints(cleaned, variable, lower, upper);

        // Segmentación base + ruptura sugerida
        final int baseSegments = 8;
        for (int i = 1; i < baseSegments; i++) {
            double cut = lower + (upper - lower) * i / baseSegments;
            breaks.add(cut);
        }
        breaks.add(lower);
        breaks.add(upper);
        Collections.sort(breaks);

        // Integración por tramos, protegiendo bordes no finitos
        double total = 0.0d;
        double eps = Math.ulp(lower + upper) * 16.0;

        for (int i = 0; i < breaks.size() - 1; i++) {
            double L = breaks.get(i);
            double R = breaks.get(i + 1);
            if (R <= L) continue;

            double l = L;
            double r = R;

            double fl = safeValue(f, l);
            if (!Double.isFinite(fl)) {
                l = nextInside(L, R, eps);
            }
            double fr = safeValue(f, r);
            if (!Double.isFinite(fr)) {
                r = prevInside(L, R, eps);
            }

            if (r > l) {
                double area = integrateSegment(f, l, r, 0);
                if (Double.isFinite(area)) {
                    total += area;
                }
            }
        }

        if (!Double.isFinite(total)) {
            throw new IllegalArgumentException("Error durante la integración numérica.");
        }
        return sign * total;
    }

    // Subdivisión adaptativa con Simpson y retroceso si falla
    private double integrateSegment(UnivariateFunction f, double l, double r, int depth) {
        if (depth > MAX_DEPTH) return 0.0d;
        if (r <= l) return 0.0d;

        // Intervalos extremadamente cortos: regla del rectángulo
        if (r - l <= Math.ulp(l + r) * 64.0) {
            double mid = 0.5 * (l + r);
            double fm = safeValue(f, mid);
            return Double.isFinite(fm) ? fm * (r - l) : 0.0d;
        }

        try {
            double area = integrator.integrate(MAX_EVAL, f, l, r);
            if (Double.isFinite(area)) return area;
        } catch (TooManyEvaluationsException ex) {
            // continuará subdividiendo
        } catch (MaxCountExceededException ex) {
            // continuará subdividiendo
        } catch (RuntimeException ex) {
            // continuará subdividiendo
        }

        double m = 0.5 * (l + r);
        double left  = integrateSegment(f, l, m, depth + 1);
        double right = integrateSegment(f, m, r, depth + 1);
        double sum = left + right;
        return Double.isFinite(sum) ? sum : 0.0d;
    }

    // Integración definida simbólica con evaluación numérica
    private double trySymbolicDefinite(String cleanedExpr, String var, double a, double b) {
        String cmd = "N(Integrate(" + cleanedExpr + ", {" + var + ", " + dbl(a) + ", " + dbl(b) + "}))";
        try {
            IExpr out = evaluator.eval(cmd);
            double v = parseDoubleOrNaN(out);
            return Double.isFinite(v) ? v : Double.NaN;
        } catch (Exception ex) {
            return Double.NaN;
        }
    }

    // Evaluación numérica de una expresión sin variables
    private double evalNumeric(String cleanedExpr) {
        try {
            IExpr out = evaluator.eval("N(" + cleanedExpr + ")");
            double v = parseDoubleOrNaN(out);
            if (!Double.isFinite(v)) {
                throw new IllegalArgumentException("Evaluación numérica no finita.");
            }
            return v;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Evaluación numérica inválida.");
        }
    }

    // Evaluación numérica en un punto
    private double evalAt(String cleanedExpr, String var, double t) {
        String cmd = "N(With({" + var + "=" + dbl(t) + "}," + cleanedExpr + "))";
        try {
            IExpr out = evaluator.eval(cmd);
            return parseDoubleOrNaN(out);
        } catch (Exception ex) {
            return Double.NaN;
        }
    }

    // Puntos de ruptura sugeridos para estabilizar integrales con |sin(kx)| y |cos(kx)|
    private static List<Double> suggestedBreakpoints(String expr, String var, double a, double b) {
        List<Double> pts = new ArrayList<Double>();

        // Normalizamos espacios y mayúsculas para los patrones
        String s = expr.replace(" ", "");
        String v = var;

        // Detectar Abs(Sin(k*v)) o Abs(Cos(k*v)) con k real simple
        // Patrones típicos después de limpieza: Abs(Sin(5*x)), Abs(Cos(3.2*x))
        int idxAbs = s.indexOf("Abs(");
        while (idxAbs >= 0) {
            int endAbs = findMatchingParen(s, idxAbs + 3); // posición de ')'
            if (endAbs > idxAbs) {
                String inside = s.substring(idxAbs + 4, endAbs);
                if (inside.startsWith("Sin(") || inside.startsWith("Cos(")) {
                    boolean isSin = inside.startsWith("Sin(");
                    int startArg = idxAbs + 4 + 4; // "Abs(" + "Sin(" o "Cos("
                    int endArg = findMatchingParen(s, startArg - 1);
                    if (endArg > startArg) {
                        String arg = s.substring(startArg, endArg);
                        double k = extractLinearCoeff(arg, v); // intenta leer k en k*v
                        if (Double.isFinite(k) && k != 0.0d) {
                            // Zeros de sin(kx): x = n*pi/k
                            // Zeros de cos(kx): x = (pi/2 + n*pi)/k
                            if (isSin) {
                                addGridPoints(pts, a, b, Math.PI / Math.abs(k), 0.0d);
                            } else {
                                addGridPoints(pts, a, b, Math.PI / Math.abs(k), 0.5 * Math.PI / Math.abs(k));
                            }
                        }
                    }
                }
            }
            idxAbs = s.indexOf("Abs(", idxAbs + 4);
        }

        return pts;
    }

    // Intenta extraer k de una expresión lineal k*v, por ejemplo "5*x", "2.5*x"
    private static double extractLinearCoeff(String arg, String var) {
        // Formatos esperados tras limpieza: "5*x", "-3*x", "x" (coef 1), "-x" (coef -1)
        if (arg.equals(var)) {
            return 1.0d;
        }
        if (arg.equals("-" + var)) {
            return -1.0d;
        }
        String needle = "*" + var;
        int p = arg.indexOf(needle);
        if (p > 0 && p == arg.length() - needle.length()) {
            try {
                return Double.parseDouble(arg.substring(0, p));
            } catch (NumberFormatException ignore) {
                return Double.NaN;
            }
        }
        return Double.NaN;
    }

    // Agrega puntos en rejilla: a + offset + n*step dentro de [a,b]
    private static void addGridPoints(List<Double> out, double a, double b, double step, double offset) {
        if (!(Double.isFinite(step) && step > 0.0d)) return;

        // Ajuste inicial hacia el primer punto >= a
        double start = a;
        double x0 = offset;
        // desplaza x0 por múltiplos de step hasta estar >= a
        if (x0 < a) {
            double n = Math.ceil((a - x0) / step);
            x0 = x0 + n * step;
        }
        // asegura que también cubrimos caso offset negativo
        while (x0 - step >= a) {
            x0 -= step;
        }

        double x = x0;
        int guard = 0;
        while (x < b && guard < 100000) {
            if (x > a && x < b) {
                out.add(x);
            }
            x += step;
            guard++;
        }
    }

    // Busca paréntesis de cierre correspondiente a partir de posOpen (posición del '(')
    private static int findMatchingParen(String s, int posOpen) {
        int depth = 0;
        for (int i = posOpen; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '(') depth++;
            else if (c == ')') {
                depth--;
                if (depth == 0) return i;
            }
        }
        return -1;
    }

    // Serialización segura de dobles para Symja
    private static String dbl(double x) {
        if (Double.isNaN(x) || Double.isInfinite(x)) return "Indeterminate";
        return Double.toString(x);
    }

    // Parser robusto de IExpr -> double
    private static double parseDoubleOrNaN(IExpr out) {
        if (out == null) return Double.NaN;
        try {
            return Double.parseDouble(out.toString());
        } catch (Exception ignore) {
            return Double.NaN;
        }
    }

    private static double safeValue(UnivariateFunction f, double x) {
        try {
            return f.value(x);
        } catch (RuntimeException ex) {
            return Double.NaN;
        }
    }

    private static double nextInside(double a, double b, double eps) {
        double x = a + Math.max(eps, Math.abs(b - a) * 1e-12);
        return (x < b) ? x : (a + (b - a) * 1e-6);
    }

    private static double prevInside(double a, double b, double eps) {
        double x = b - Math.max(eps, Math.abs(b - a) * 1e-12);
        return (x > a) ? x : (b - (b - a) * 1e-6);
    }
}



