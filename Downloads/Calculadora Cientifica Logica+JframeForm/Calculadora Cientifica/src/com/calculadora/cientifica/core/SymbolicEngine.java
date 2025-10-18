/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.calculadora.cientifica.core;

import org.matheclipse.core.eval.ExprEvaluator;
import org.matheclipse.core.expression.F;
import org.matheclipse.core.interfaces.IExpr;

/**
 * @author Capito
 * Motor simbólico: derivación, integración e identificación de constantes.
 * Usa Symja con simplificación estable.
 */
public class SymbolicEngine {
    private final ExprEvaluator evaluator;

    public SymbolicEngine() {
        F.initSymbols();
        this.evaluator = new ExprEvaluator();
    }

    /** Derivada simbólica d/d{variable} (expr) con simplificación. */
    public String derive(String expr, String variable) {
        if (expr == null || variable == null) {
            throw new IllegalArgumentException("Expresión y variable son requeridas.");
        }
        String e0 = expr.trim();
        if (e0.isEmpty()) {
            throw new IllegalArgumentException("Expresión vacía.");
        }
        String v = normalizeVar(variable);
        String e = toEngineSafe(e0);

        // Atajo: si no depende de la variable, la derivada es 0
        if (!e.contains(v)) {
            return "0";
        }

        String cmd = "Simplify(Together(Expand(D(" + e + "," + v + "))))";
        try {
            IExpr out = evaluator.eval(cmd);
            return MathFormatter.toDisplay(out.toString());
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("Error durante la derivación simbólica.");
        }
    }

    /** Integral indefinida ∫ (expr) d{variable} con simplificación. */
    public String integrate(String expr, String variable) {
        if (expr == null || variable == null) {
            throw new IllegalArgumentException("Expresión y variable son requeridas.");
        }
        String e0 = expr.trim();
        if (e0.isEmpty()) {
            throw new IllegalArgumentException("Expresión vacía.");
        }
        String v = normalizeVar(variable);
        String e = toEngineSafe(e0);

        // Atajo: si no depende de la variable, ∫ c dx = c*x + C
        if (!e.contains(v)) {
            // Mantiene la forma simbólica sin evaluar c numéricamente
            String res = "(" + e + ")*" + v + " + C";
            return MathFormatter.toDisplay(res);
        }

        String cmd = "Simplify(Together(Integrate(" + e + "," + v + ")))";
        try {
            IExpr out = evaluator.eval(cmd);
            String pretty = MathFormatter.toDisplay(out.toString());

            // Añade “+ C” si el resultado parece un antiderivado puro (estilo calculadora)
            // Symja no agrega la constante de integración.
            if (!pretty.isEmpty() && !pretty.contains("+ C")) {
                pretty = pretty + " + C";
            }
            return pretty;
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("Error durante la integración simbólica.");
        }
    }

    /** Simplificación algebraica general. */
    public String simplify(String expr) {
        if (expr == null) return "";
        String e0 = expr.trim();
        if (e0.isEmpty()) return "";
        String e = toEngineSafe(e0);
        try {
            IExpr out = evaluator.eval("Simplify(Together(Expand(" + e + ")))");
            return MathFormatter.toDisplay(out.toString());
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("Error durante la simplificación.");
        }
    }

    /** Evalúa constantes puras; devuelve null si no es constante numérica. */
    public Double tryEvaluateConstant(String exprEngine) {
        if (exprEngine == null || exprEngine.trim().isEmpty()) return null;
        try {
            IExpr out = evaluator.eval("N(" + exprEngine + ")");
            try {
                return Double.parseDouble(out.toString());
            } catch (NumberFormatException ex) {
                return null;
            }
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private static String normalizeVar(String variable) {
        String v = variable.trim().toLowerCase();
        if (!("x".equals(v) || "y".equals(v))) {
            throw new IllegalArgumentException("Variable inválida (use x o y).");
        }
        return v;
    }

    private static String toEngineSafe(String expr) {
        String s = MathFormatter.toEngine(expr);
        s = ExpressionValidator.cleanExpression(s);
        return s;
    }
}

