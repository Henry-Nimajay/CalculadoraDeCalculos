/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.calculadora.cientifica.core;

/**
 * 
 * @author Capito
 */
public class VariableContext {

    public enum CalcMode { NONE, DERIV, INTEGRAL }
    public enum AngleMode { RAD, DEG }

    private String variable = "x";
    private double precision = 1e-6;

    private CalcMode mode = CalcMode.NONE;
    private AngleMode angleMode = AngleMode.RAD;

    // Preferencias visuales de salida
    private boolean useMiddleDot = true;   // 2·x en display
    private boolean useUnicodePi = true;   // π en display

    public VariableContext() { }

    public VariableContext(String variable, double precision) {
        setVariable(variable);
        setPrecision(precision);
    }

    // Variable activa: valida solo x o y
    public void setVariable(String var) {
        if (var == null) throw new IllegalArgumentException("Variable requerida (x o y).");
        String v = var.trim().toLowerCase();
        if (!("x".equals(v) || "y".equals(v)))
            throw new IllegalArgumentException("Variable inválida: " + var + " (use x o y).");
        this.variable = v;
    }

    public String getVariable() {
        return variable;
    }

    // Precisión numérica: mínima 1e-12, máxima 1e-2
    public void setPrecision(double p) {
        if (Double.isNaN(p) || Double.isInfinite(p) || p <= 0.0)
            throw new IllegalArgumentException("La precisión debe ser positiva y finita.");
        if (p < 1e-12) p = 1e-12;
        if (p > 1e-2) p = 1e-2;
        this.precision = p;
    }

    public double getPrecision() {
        return precision;
    }

    // Modo de cálculo
    public void setMode(CalcMode m) {
        this.mode = (m == null) ? CalcMode.NONE : m;
    }

    public CalcMode getMode() {
        return mode;
    }

    // Modo angular (RAD/DEG)
    public void setAngleMode(AngleMode am) {
        this.angleMode = (am == null) ? AngleMode.RAD : am;
    }

    public AngleMode getAngleMode() {
        return angleMode;
    }

    // Preferencias de visualización
    public void setUseMiddleDot(boolean use) {
        this.useMiddleDot = use;
    }

    public boolean isUseMiddleDot() {
        return useMiddleDot;
    }

    public void setUseUnicodePi(boolean use) {
        this.useUnicodePi = use;
    }

    public boolean isUseUnicodePi() {
        return useUnicodePi;
    }

    // Clonado rápido del contexto actual
    public VariableContext copy() {
        VariableContext c = new VariableContext();
        c.variable = this.variable;
        c.precision = this.precision;
        c.mode = this.mode;
        c.angleMode = this.angleMode;
        c.useMiddleDot = this.useMiddleDot;
        c.useUnicodePi = this.useUnicodePi;
        return c;
    }
}


