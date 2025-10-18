/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.calculadora.cientifica.util;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 
 * @author Capito
 */
public final class HistoryModel {

    public enum Type { EVAL, DERIV, INTEGRAL_INDEF, INTEGRAL_DEF }

    private static final HistoryModel INSTANCE = new HistoryModel();
    public static HistoryModel getInstance() { return INSTANCE; }

    private final List<Entry> items = Collections.synchronizedList(new ArrayList<>());
    private int maxSize = 500;

    public HistoryModel() {}

    public void add(String expression, String result) {
        addEntry(new Entry(Type.EVAL, expression, result, null, null, null, null, LocalDateTime.now()));
    }

    public void addDerivative(String f, String var, String fprime) {
        addEntry(new Entry(Type.DERIV, f, fprime, var, null, null, null, LocalDateTime.now()));
    }

    public void addIntegralIndef(String f, String var, String F) {
        addEntry(new Entry(Type.INTEGRAL_INDEF, f, F, var, null, null, null, LocalDateTime.now()));
    }

    public void addIntegralDef(String f, String var, Double a, Double b, String value) {
        addEntry(new Entry(Type.INTEGRAL_DEF, f, value, var, a, b, null, LocalDateTime.now()));
    }

    public void addCustom(Type type, String expression, String result, String var, Double a, Double b, String note) {
        addEntry(new Entry(type == null ? Type.EVAL : type, expression, result, var, a, b, note, LocalDateTime.now()));
    }

    public List<Entry> getAll() {
        synchronized (items) {
            return new ArrayList<>(items);
        }
    }

    public List<Entry> getRecent(int n) {
        synchronized (items) {
            int sz = items.size();
            int from = Math.max(0, sz - Math.max(0, n));
            return new ArrayList<>(items.subList(from, sz));
        }
    }

    public void clear() {
        synchronized (items) {
            items.clear();
        }
    }

    public void setMaxSize(int maxSize) {
        if (maxSize < 1) throw new IllegalArgumentException("maxSize debe ser ≥ 1");
        synchronized (items) {
            this.maxSize = maxSize;
            trimIfNeeded();
        }
    }

    public int getMaxSize() {
        synchronized (items) {
            return maxSize;
        }
    }

    public int size() {
        synchronized (items) {
            return items.size();
        }
    }

    public static String formatLine(Entry e) {
        String var = e.getVariable() == null ? "" : e.getVariable();
        String expr = com.calculadora.cientifica.core.MathFormatter.toDisplay(e.getExpression());
        String res  = com.calculadora.cientifica.core.MathFormatter.toDisplay(e.getResult());
        switch (e.getType()) {
            case DERIV:
                return "d/d" + var + ": " + expr + " = " + res;
            case INTEGRAL_INDEF: {
                String shown = res;
                String trimmed = res == null ? "" : res.trim();
                if (!trimmed.endsWith("+ C")) {
                    shown = (trimmed.isEmpty() ? "" : trimmed) + " + C";
                }
                return "∫ " + var + ": " + expr + " = " + shown;
            }
            case INTEGRAL_DEF: {
                String a = e.getA() == null ? "?" : com.calculadora.cientifica.core.MathFormatter.formatDouble(e.getA());
                String b = e.getB() == null ? "?" : com.calculadora.cientifica.core.MathFormatter.formatDouble(e.getB());
                return "∫[" + a + "," + b + "] d" + var + ": " + expr + " = " + res;
            }
            default:
                return expr + " = " + res;
        }
    }

    public List<String> toLines() {
        List<String> lines = new ArrayList<>();
        List<Entry> snapshot;
        synchronized (items) {
            snapshot = new ArrayList<>(items);
        }
        for (Entry e : snapshot) {
            lines.add(formatLine(e));
        }
        return lines;
    }

    public List<String> toLinesRecent(int n) {
        List<String> lines = new ArrayList<>();
        List<Entry> snapshot;
        synchronized (items) {
            int sz = items.size();
            int from = Math.max(0, sz - Math.max(0, n));
            snapshot = new ArrayList<>(items.subList(from, sz));
        }
        for (Entry e : snapshot) {
            lines.add(formatLine(e));
        }
        return lines;
    }

    private void addEntry(Entry e) {
        if (e == null) return;
        synchronized (items) {
            items.add(e);
            trimIfNeeded();
        }
    }

    private void trimIfNeeded() {
        int extra = items.size() - maxSize;
        if (extra > 0) {
            items.subList(0, extra).clear();
        }
    }

    public static final class Entry {
        private final Type type;
        private final String expression;
        private final String result;
        private final String variable;
        private final Double a;
        private final Double b;
        private final String note;
        private final LocalDateTime timestamp;

        public Entry(Type type, String expression, String result, String variable,
                     Double a, Double b, String note, LocalDateTime timestamp) {
            this.type = type;
            this.expression = expression == null ? "" : expression;
            this.result = result == null ? "" : result;
            this.variable = variable;
            this.a = a;
            this.b = b;
            this.note = note;
            this.timestamp = timestamp == null ? LocalDateTime.now() : timestamp;
        }

        public Type getType() { return type; }
        public String getExpression() { return expression; }
        public String getResult() { return result; }
        public String getVariable() { return variable; }
        public Double getA() { return a; }
        public Double getB() { return b; }
        public String getNote() { return note; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }
}
