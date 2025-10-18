/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.calculadora.cientifica.ui;

import com.calculadora.cientifica.core.MathFormatter;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 
 * @author Capito
 */
public class HtmlMathPreview extends JPanel {

    // Cache simple para evitar repintados innecesarios
    private volatile String lastHtml = null;
    private final JLabel lblExpr; // muestra la expresión que se operó


    // Patrones precompilados para reemplazos HTML compatibles con Swing
    private static final Pattern FRAC_PATTERN = Pattern.compile(
            "<span class='mfrac'><span class='num'>(.*?)</span><span class='bar'></span><span class='den'>(.*?)</span></span>",
            Pattern.DOTALL
    );

    private static final Pattern HTML_OPEN = Pattern.compile("^\\s*<html>", Pattern.CASE_INSENSITIVE);

    private final JLabel label;

    public HtmlMathPreview() {
        super(new BorderLayout());
        setOpaque(false);
        setBorder(new EmptyBorder(6, 8, 6, 8));        // margen
        setPreferredSize(new Dimension(56, 68));
        setMinimumSize(new Dimension(10,68));// altura suficiente para superíndices
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 80)); // nuevo: evita recortes

        // JLabel con antialiasing forzado para HTML
        label = new AALabel();
        label.setOpaque(false);
        label.setVerticalAlignment(SwingConstants.CENTER);
        label.setHorizontalAlignment(SwingConstants.RIGHT);
        label.setFont(label.getFont().deriveFont(Font.PLAIN, 18f));

        // Label superior pequeño para mostrar la expresión original
        lblExpr = new JLabel();
        lblExpr.setOpaque(false);
        lblExpr.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblExpr.setForeground(new Color(90, 90, 90));
        lblExpr.setHorizontalAlignment(SwingConstants.RIGHT);
        lblExpr.setBorder(new EmptyBorder(0, 4, 2, 4));
        add(lblExpr, BorderLayout.NORTH);

        // Label principal para el resultado
        add(label, BorderLayout.CENTER);
        setExpression("");

    }

    // Actualiza la vista con la expresión (input lineal o salida de motor)
    public void setExpression(String expr) {
        final String html = adaptForSwing(MathFormatter.toHtmlDisplay(expr));
        updateTextOnEDT(html);
    }
    // Muestra dos fragmentos en una sola línea: izquierda = derecha
public void setInline(String leftExpr, String rightExpr) {
    String leftHtml  = adaptFragment(MathFormatter.toHtmlDisplay(leftExpr));
    String rightHtml = adaptFragment(MathFormatter.toHtmlDisplay(rightExpr));

    String fg = toHex(UIManager.getColor("Label.foreground"), "#111111");

    String html =
        "<html><div style='font-family:Segoe UI, sans-serif; font-size:18pt; color:" + fg + "; " +
        "line-height:1.2; text-align:right; white-space:nowrap;'>" +
            "<table cellspacing='0' cellpadding='0' style='display:inline-table; vertical-align:middle;'>" +
                "<tr>" +
                    "<td align='right' style='padding-right:8px;'>" + leftHtml + "</td>" +
                    "<td style='padding:0 8px;'>=</td>" +
                    "<td align='left'>" + rightHtml + "</td>" +
                "</tr>" +
            "</table>" +
        "</div></html>";

    updateTextOnEDT(html);
}

// Igual que adaptForSwing pero devuelve el fragmento interno listo para incrustar en una celda
private static String adaptFragment(String html) {
    if (html == null || html.isEmpty()) return "";
    String s = html;

    // Reemplazar fracciones apiladas por tabla inline (mismo que adaptForSwing)
    Matcher m = FRAC_PATTERN.matcher(s);
    StringBuffer sb = new StringBuffer();
    while (m.find()) {
        String num = m.group(1);
        String den = m.group(2);
        String table = "<table cellspacing='0' cellpadding='0' style='display:inline-table;vertical-align:middle;'>"
                + "<tr><td align='center' style='font-size:16pt;'>" + num + "</td></tr>"
                + "<tr><td align='center' style='border-top:1px solid; font-size:16pt;'>" + den + "</td></tr>"
                + "</table>";
        m.appendReplacement(sb, Matcher.quoteReplacement(table));
    }
    m.appendTail(sb);
    s = sb.toString();

    // Quitar <html> envolvente que trae MathFormatter.toHtmlDisplay(...)
    s = s.replaceFirst("(?i)^\\s*<html>\\s*", "").replaceFirst("(?i)\\s*</html>\\s*$", "");

    return s;
}

    // Mensaje de estado/error en la misma zona
    public void setMessage(String msg) {
        String safe = (msg == null) ? "" : escape(msg);
        String color = toHex(UIManager.getColor("Label.errorForeground"), "#B00020");
        String html = "<html><div style='color:" + color + ";font-family:Segoe UI, sans-serif;font-size:12pt;'>" + safe + "</div></html>";
        updateTextOnEDT(html);
    }

    public void clear() {
     setExpression("");
     lblExpr.setText("");
 }

        // Muestra arriba qué operación se realizó (por ejemplo, "sin(π/2)")
    public void setOriginalExpr(String expr) {
        String clean = (expr == null) ? "" : expr.trim();
        if (clean.isEmpty()) {
            lblExpr.setText("");
            return;
        }
        String display = MathFormatter.toDisplay(clean);
        lblExpr.setText("<html><span style='color:#666666;font-family:Segoe UI;font-size:12pt;'>" 
                        + display + "</span></html>");
    }


    //  Internos 
    private void updateTextOnEDT(String html) {
        if (Objects.equals(html, lastHtml)) return; // evita trabajo si no hay cambios
        lastHtml = html;
        if (SwingUtilities.isEventDispatchThread()) {
            label.setText(html);
        } else {
            SwingUtilities.invokeLater(() -> label.setText(html));
        }
    }

    // Adapta el HTML generado a lo soportado por JLabel
    private static String adaptForSwing(String html) {
        if (html == null || html.isEmpty()) return "<html></html>";
        String s = html;

        // Reemplazo de fracciones apiladas por tabla inline
        Matcher m = FRAC_PATTERN.matcher(s);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String num = m.group(1);
            String den = m.group(2);
            String table = "<table cellspacing='0' cellpadding='0' style='display:inline-table;vertical-align:middle;'>"
                    + "<tr><td align='center' style='font-size:16pt;'>" + num + "</td></tr>"
                    + "<tr><td align='center' style='border-top:1px solid; font-size:16pt;'>" + den + "</td></tr>"
                    + "</table>";
            m.appendReplacement(sb, Matcher.quoteReplacement(table));
        }
        m.appendTail(sb);
        s = sb.toString();

        // Estilo base inline (color según LAF)
        String fg = toHex(UIManager.getColor("Label.foreground"), "#111111");
        String open = "<html><div style='font-family:Segoe UI, sans-serif; font-size:18pt; color:" + fg + "; line-height:1.2; text-align:right; white-space:nowrap;'>";
        if (HTML_OPEN.matcher(s).find()) {
            s = s.replaceFirst("(?i)<html>", open);
        } else {
            s = open + s;
        }
        if (s.endsWith("</html>")) {
            s = s.substring(0, s.length() - "</html>".length()) + "</div></html>";
        } else if (!s.endsWith("</div></html>")) {
            s = s + "</div></html>";
        }

        return s;
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String toHex(Color c, String fallbackHex) {
        if (c == null) return fallbackHex;
        return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }

    // JLabel con antialiasing activado para mejorar render de HTML
    private static final class AALabel extends JLabel {
        @Override
        protected void paintComponent(Graphics g) {
            if (g instanceof Graphics2D) {
                ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            }
            super.paintComponent(g);
        }
    }
}
