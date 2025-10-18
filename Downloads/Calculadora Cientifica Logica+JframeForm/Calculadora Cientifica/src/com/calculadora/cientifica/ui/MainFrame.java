/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package com.calculadora.cientifica.ui;
import com.calculadora.cientifica.core.CalculationService;
import com.calculadora.cientifica.core.ExpressionValidator;
import com.calculadora.cientifica.core.MathFormatter;
import com.calculadora.cientifica.core.VariableContext;
import com.calculadora.cientifica.util.HistoryModel;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JScrollPane;
import javax.swing.text.JTextComponent;



/**
 *
 * @author Capito
 */
public class MainFrame extends javax.swing.JFrame {
    
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(MainFrame.class.getName());
    private final com.calculadora.cientifica.core.SymbolicEngine sym = new com.calculadora.cientifica.core.SymbolicEngine();
    private javax.swing.text.JTextComponent lastTarget;
    
    
     // Servicios / estado 
    private final CalculationService calc = new CalculationService();
    private final VariableContext varCtx = new VariableContext();
    private HtmlMathPreview preview;
    // UI: conmutar editor/preview en el mismo espacio
    private java.awt.CardLayout displayCards;
    private javax.swing.JPanel displayStack;
    private static final String CARD_EDIT = "edit";
    private static final String CARD_VIEW = "view";

    private HistoryModel historyModel; // si tu clase está vacía, uso fallback interno
   
    //Creates new form MainFrame
    public MainFrame() {
        initComponents();
        postInit(); 
   
}

    private String symjaEvalNumeric(String expr) {
    // expr puede venir “bonito”; normalizamos y evaluamos constante con fallback a simplify
    String norm = com.calculadora.cientifica.core.ExpressionValidator.cleanExpression(
            com.calculadora.cientifica.core.MathFormatter.toEngine(expr));
    Double v = sym.tryEvaluateConstant(norm);
    return (v != null) ? com.calculadora.cientifica.core.MathFormatter.formatDouble(v)
                       : sym.simplify(norm);
}

private String symjaSimplify(String expr) {
    String norm = com.calculadora.cientifica.core.ExpressionValidator.cleanExpression(
            com.calculadora.cientifica.core.MathFormatter.toEngine(expr));
    return sym.simplify(norm);
}

private void postInit() {
    setStatus("Listo.");
    installKeyBindings();
    historyModel = com.calculadora.cientifica.util.HistoryModel.getInstance();

    varCtx.setVariable((String) comboVariable.getSelectedItem());
    varCtx.setMode(com.calculadora.cientifica.core.VariableContext.CalcMode.NONE);
    comboVariable.addActionListener(e -> varCtx.setVariable((String) comboVariable.getSelectedItem()));

    // Monta una pila editor/preview en el mismo lugar del txtDisplay
    preview = new HtmlMathPreview();
    displayStack = new javax.swing.JPanel();
    displayCards = new java.awt.CardLayout(0, 0);
    displayStack.setLayout(displayCards);

    // Sacar el txtDisplay del panel y volverlo a poner dentro del stack
    panelTop.remove(txtDisplay);
    displayStack.add(txtDisplay, CARD_EDIT);
    displayStack.add(preview,   CARD_VIEW);

    // Poner el stack donde antes estaba el txtDisplay
    panelTop.add(displayStack, java.awt.BorderLayout.CENTER);
    panelTop.revalidate();
    panelTop.repaint();

    // Arrancar en modo edición vacío
    preview.setExpression("");
    showEditor();
    wireFocusTracking();                // empieza a recordar el último campo con foco
    disableButtonFocus(panelNumerico);  // que los botones no roben el foco
    disableButtonFocus(panelCientifico);
    txtDisplay.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
    addHelpMenu();

}
// En MainFrame
private void addHelpMenu() {
    JMenuBar mb = new JMenuBar();
    JMenu ayuda = new JMenu("Ayuda");

    JMenuItem manual = new JMenuItem("Manual de usuario");
    manual.addActionListener(e -> showManualDialog());

    ayuda.add(manual);
    mb.add(ayuda);
    setJMenuBar(mb);
}

private void showManualDialog() {
    JDialog dlg = new JDialog(this, "Manual de usuario", true);
    dlg.setSize(720, 560);
    dlg.setLocationRelativeTo(this);

    JEditorPane html = new JEditorPane();
    html.setEditable(false);
    html.setContentType("text/html");

    try (var in = getClass().getResourceAsStream("manual_usuario.html")) {
        if (in != null) {
            String text = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            html.setText(text);
            html.setCaretPosition(0);
        } else {
            html.setText("<html><body><h2>No se encontró manual_usuario.html</h2></body></html>");
        }
    } catch (Exception ex) {
        html.setText("<html><body><h2>Error cargando el manual</h2><pre>" +
                (ex.getMessage() == null ? ex.toString() : ex.getMessage()) +
                "</pre></body></html>");
    }

    dlg.getContentPane().add(new JScrollPane(html));
    dlg.setVisible(true);
}


private void wireFocusTracking() {
    java.awt.event.FocusListener fl = new java.awt.event.FocusAdapter() {
        @Override public void focusGained(java.awt.event.FocusEvent e) {
            if (e.getComponent() instanceof javax.swing.text.JTextComponent) {
                lastTarget = (javax.swing.text.JTextComponent) e.getComponent();
            }
        }
    };
    txtDisplay.addFocusListener(fl);
    txtA.addFocusListener(fl);
    txtB.addFocusListener(fl);
}
private static void disableButtonFocus(java.awt.Container c) {
    for (java.awt.Component comp : c.getComponents()) {
        if (comp instanceof javax.swing.JButton) {
            comp.setFocusable(false);
        } else if (comp instanceof java.awt.Container) {
            disableButtonFocus((java.awt.Container) comp);
        }
    }
}

/** Devuelve el campo de texto destino: el que tiene foco o el último que lo tuvo. */
private javax.swing.text.JTextComponent targetField() {
    java.awt.Component fo = getFocusOwner();
    if (fo instanceof javax.swing.text.JTextComponent) {
        javax.swing.text.JTextComponent tc = (javax.swing.text.JTextComponent) fo;
        if (tc == txtA || tc == txtB || tc == txtDisplay) {
            lastTarget = tc; // actualiza también
            return tc;
        }
    }
    if (lastTarget != null) return lastTarget;
    return txtDisplay; // fallback
}


/** Enfoca el campo de texto decidido por targetField(). */
private void focusTargetField() {
    JTextComponent tc = targetField();
    if (tc != null) tc.requestFocusInWindow();
}


private void installKeyBindings() {
    // 1) Bindings locales cuando el foco está en el editor (por compatibilidad)
    javax.swing.InputMap imLocal = txtDisplay.getInputMap(javax.swing.JComponent.WHEN_FOCUSED);
    javax.swing.ActionMap amLocal = txtDisplay.getActionMap();

    imLocal.put(javax.swing.KeyStroke.getKeyStroke("ENTER"), "EVAL");
    imLocal.put(javax.swing.KeyStroke.getKeyStroke("ESCAPE"), "CLR");
    imLocal.put(javax.swing.KeyStroke.getKeyStroke("BACK_SPACE"), "BACK");

    imLocal.put(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_D, java.awt.event.InputEvent.CTRL_DOWN_MASK), "DERIV_MODE");
    imLocal.put(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_I, java.awt.event.InputEvent.CTRL_DOWN_MASK), "INT_MODE");
    imLocal.put(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_I, java.awt.event.InputEvent.CTRL_DOWN_MASK | java.awt.event.InputEvent.SHIFT_DOWN_MASK), "INT_DEF_FOCUS");
    imLocal.put(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_L, java.awt.event.InputEvent.CTRL_DOWN_MASK), "CLR_ALL");
    imLocal.put(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F4, 0), "TOGGLE_VIEW");

    amLocal.put("EVAL", new javax.swing.AbstractAction() { @Override public void actionPerformed(java.awt.event.ActionEvent e) { btnEqualsActionPerformed(null); }});
    amLocal.put("CLR",  new javax.swing.AbstractAction() { @Override public void actionPerformed(java.awt.event.ActionEvent e) { btnClearActionPerformed(null); }});
    amLocal.put("BACK", new javax.swing.AbstractAction() { @Override public void actionPerformed(java.awt.event.ActionEvent e) { btnBackActionPerformed(null); }});

    amLocal.put("DERIV_MODE", new javax.swing.AbstractAction() { @Override public void actionPerformed(java.awt.event.ActionEvent e) { enterDerivMode(); }});
    amLocal.put("INT_MODE",   new javax.swing.AbstractAction() { @Override public void actionPerformed(java.awt.event.ActionEvent e) { enterIntegralMode(); }});
    amLocal.put("INT_DEF_FOCUS", new javax.swing.AbstractAction() { @Override public void actionPerformed(java.awt.event.ActionEvent e) { txtA.requestFocusInWindow(); }});
    amLocal.put("CLR_ALL", new javax.swing.AbstractAction() { @Override public void actionPerformed(java.awt.event.ActionEvent e) { btnClearActionPerformed(null); }});
    amLocal.put("TOGGLE_VIEW", new javax.swing.AbstractAction() {
        @Override public void actionPerformed(java.awt.event.ActionEvent e) {
            if (displayCards != null && displayStack != null) {
                java.awt.Component showing = null;
                for (java.awt.Component c : displayStack.getComponents()) {
                    if (c.isVisible()) { showing = c; break; }
                }
                if (showing == txtDisplay) showPreview(txtDisplay.getText());
                else showEditor();
            }
        }
    });

    // 2) Bindings globales: funcionan aunque el foco esté en un botón u otro campo
    javax.swing.JRootPane root = getRootPane();
    javax.swing.InputMap imGlobal = root.getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW);
    javax.swing.ActionMap amGlobal = root.getActionMap();

    imGlobal.put(javax.swing.KeyStroke.getKeyStroke("ENTER"), "EVAL_GLOBAL");
    imGlobal.put(javax.swing.KeyStroke.getKeyStroke("ESCAPE"), "CLR_GLOBAL");
    imGlobal.put(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F4, 0), "TOGGLE_VIEW_GLOBAL");

    amGlobal.put("EVAL_GLOBAL", new javax.swing.AbstractAction() { @Override public void actionPerformed(java.awt.event.ActionEvent e) { btnEqualsActionPerformed(null); }});
    amGlobal.put("CLR_GLOBAL",  new javax.swing.AbstractAction() { @Override public void actionPerformed(java.awt.event.ActionEvent e) { btnClearActionPerformed(null); }});
    amGlobal.put("TOGGLE_VIEW_GLOBAL", amLocal.get("TOGGLE_VIEW"));

    // 3) Botón por defecto para Enter cuando un componente no consume ActionEvent
    getRootPane().setDefaultButton(btnEquals);
    // 4) Enter en los campos a/b ejecuta ∫ a→b directamente
javax.swing.InputMap imA = txtA.getInputMap(javax.swing.JComponent.WHEN_FOCUSED);
javax.swing.ActionMap amA = txtA.getActionMap();
javax.swing.InputMap imB = txtB.getInputMap(javax.swing.JComponent.WHEN_FOCUSED);
javax.swing.ActionMap amB = txtB.getActionMap();

imA.put(javax.swing.KeyStroke.getKeyStroke("ENTER"), "INTEGRATE_DEF");
imB.put(javax.swing.KeyStroke.getKeyStroke("ENTER"), "INTEGRATE_DEF");

javax.swing.Action doIntDef = new javax.swing.AbstractAction() {
    @Override public void actionPerformed(java.awt.event.ActionEvent e) {
        doIntegrateDef();
    }
};
amA.put("INTEGRATE_DEF", doIntDef);
amB.put("INTEGRATE_DEF", doIntDef);

}

private void enterDerivMode() {
    varCtx.setMode(com.calculadora.cientifica.core.VariableContext.CalcMode.DERIV);
    setStatus("Modo derivada (d/d" + varCtx.getVariable() + "). Presiona =");
    txtDisplay.requestFocusInWindow();
}

private void enterIntegralMode() {
    varCtx.setMode(com.calculadora.cientifica.core.VariableContext.CalcMode.INTEGRAL);
    setStatus("Modo integral (∫ d" + varCtx.getVariable() + "). Presiona =");
    if (txtDisplay.getText().trim().isEmpty()){
        insertText("(");
    }
    txtDisplay.requestFocusInWindow();
}


private void insertText(String s) {
    JTextComponent field = targetField();
    String t = field.getText();
    int start = field.getSelectionStart();
    int end = field.getSelectionEnd();
    int caret = field.getCaretPosition();

    if (start != end) {
        field.setText(t.substring(0, start) + s + t.substring(end));
        field.setCaretPosition(start + s.length());
    } else {
        field.setText(t.substring(0, caret) + s + t.substring(caret));
        field.setCaretPosition(caret + s.length());
    }
    if (field == txtDisplay) showEditor();
    focusTargetField();
}


private void insertToken(String token, boolean ensureParen) {
    String tk = (ensureParen && !token.endsWith("(")) ? token + "(" : token;
    insertText(tk);
}


private void wrapWith(String left, String right) {
    JTextComponent field = targetField();
    int start = field.getSelectionStart();
    int end = field.getSelectionEnd();
    String t = field.getText();
    if (start == end) {
        int caret = field.getCaretPosition();
        field.setText(t.substring(0, caret) + left + right + t.substring(caret));
        field.setCaretPosition(caret + left.length());
    } else {
        String wrapped = left + t.substring(start, end) + right;
        field.setText(t.substring(0, start) + wrapped + t.substring(end));
        field.setCaretPosition(start + wrapped.length());
    }
    if (field == txtDisplay) showEditor();
    focusTargetField();
}


private void backspace() {
    JTextComponent field = targetField();
    String t = field.getText();
    int start = field.getSelectionStart();
    int end = field.getSelectionEnd();
    int caret = field.getCaretPosition();
    if (start != end) {
        field.setText(t.substring(0, start) + t.substring(end));
        field.setCaretPosition(start);
    } else if (caret > 0) {
        field.setText(t.substring(0, caret - 1) + t.substring(caret));
        field.setCaretPosition(caret - 1);
    }
    if (field == txtDisplay) showEditor();
    focusTargetField();
}


private void deleteForward() {
    JTextComponent field = targetField();
    String t = field.getText();
    int start = field.getSelectionStart();
    int end = field.getSelectionEnd();
    int caret = field.getCaretPosition();
    if (start != end) {
        field.setText(t.substring(0, start) + t.substring(end));
        field.setCaretPosition(start);
    } else if (caret < t.length()) {
        field.setText(t.substring(0, caret) + t.substring(caret + 1));
        field.setCaretPosition(caret);
    }
    if (field == txtDisplay) showEditor();
    focusTargetField();
}

// Muestra el editor de texto
private void showEditor() {
    if (displayCards != null && displayStack != null) {
        displayCards.show(displayStack, CARD_EDIT);
        txtDisplay.requestFocusInWindow();
        txtDisplay.setCaretPosition(txtDisplay.getText().length());
    }
}
// Muestra en una sola línea: izquierda = derecha, y actualiza preview
private void showInlineResult(String inputPretty, String resultPretty) {
    String left  = (inputPretty  == null) ? "" : inputPretty.trim();
    String right = (resultPretty == null) ? "" : resultPretty.trim();
    String eq = left + " = " + right;

    // Mantén el texto editable con la ecuación completa
    txtDisplay.setText(eq);
    txtDisplay.setCaretPosition(eq.length());

    // Render “inline” estable (evita el salto a la línea de arriba)
    if (preview != null) preview.setInline(left, right);

    if (displayCards != null && displayStack != null) {
        displayCards.show(displayStack, CARD_VIEW);
    }
}


// Muestra el render HTML con la expresión formateada
private void showPreview(String rendered) {
    if (preview != null) preview.setExpression(rendered == null ? "" : rendered);
    if (displayCards != null && displayStack != null) {
        displayCards.show(displayStack, CARD_VIEW);
    }
}

private void clearAll() {
    txtDisplay.setText("");
    setStatus("Listo.");
    if (preview != null) preview.clear();
    showEditor();
}

private void setStatus(String msg) {
    if (javax.swing.SwingUtilities.isEventDispatchThread()) {
        lblEstado.setText(msg);
    } else {
        javax.swing.SwingUtilities.invokeLater(() -> lblEstado.setText(msg));
    }
}

// Asegura la notación de constante de integración en display.
private String ensurePlusC(String s) {
    String t = (s == null) ? "" : s.trim();
    return t.endsWith("+ C") ? t : (t.isEmpty() ? "C" : t + " + C");
}

// CALCULO 
private void evalEquals() {
    final String raw = txtDisplay.getText();
    final String v = (String) comboVariable.getSelectedItem();
    if (raw == null || raw.trim().isEmpty()) { setStatus("Ingrese una expresión."); return; }

    var mode = varCtx.getMode();
    if (mode == com.calculadora.cientifica.core.VariableContext.CalcMode.DERIV) { doDerive(); varCtx.setMode(com.calculadora.cientifica.core.VariableContext.CalcMode.NONE); return; }
    if (mode == com.calculadora.cientifica.core.VariableContext.CalcMode.INTEGRAL) { doIntegrateIndef(); varCtx.setMode(com.calculadora.cientifica.core.VariableContext.CalcMode.NONE); return; }

    final String exprEngine = com.calculadora.cientifica.core.MathFormatter.toEngine(raw);
    final String expr = com.calculadora.cientifica.core.ExpressionValidator.cleanExpression(exprEngine);
    if (!com.calculadora.cientifica.core.ExpressionValidator.isValidExpression(expr, v)) {
        setStatus("Expresión inválida.");
        return;
    }

    setStatus("Calculando...");
    btnEquals.setEnabled(false);

    new javax.swing.SwingWorker<String, Void>() {
        @Override protected String doInBackground() {
        final String varRegex = "\\b" + java.util.regex.Pattern.quote(v) + "\\b";
        final boolean contieneVar = java.util.regex.Pattern.compile(varRegex).matcher(expr).find();
        return contieneVar ? symjaSimplify(expr) : symjaEvalNumeric(expr);
    }
        @Override protected void done() {
            try {
                String res = get();
                String shown = com.calculadora.cientifica.core.MathFormatter.toDisplay(res);
                String left = com.calculadora.cientifica.core.MathFormatter.toDisplay(raw);
                showInlineResult(left, shown);
                // Evita duplicados consecutivos en historial
                java.util.List<String> lines = com.calculadora.cientifica.util.HistoryModel.getInstance().toLines();
                String last = (lines.isEmpty() ? null : lines.get(lines.size()-1));
                String current = expr + " = " + shown;
                if (last == null || !last.endsWith(current)) {
                    com.calculadora.cientifica.util.HistoryModel.getInstance().add(expr, shown);
                }
                setStatus("OK");
            } catch (Exception ex) {
                setStatus("Error: " + ex.getMessage());
            } finally {
                btnEquals.setEnabled(true);
            }
        }
    }.execute();
}

private void doDerive() {
    String v = (String) comboVariable.getSelectedItem();
    String expr = ExpressionValidator.cleanExpression(MathFormatter.toEngine(txtDisplay.getText()));
    if (!ExpressionValidator.isValidExpression(expr, v)) { setStatus("Expresión inválida para derivar."); return; }
    setStatus("Derivando...");
    try {
        String d = calc.derive(expr, v);
        String shown = MathFormatter.toDisplay(d);
        String left = "d/d" + v + " " + MathFormatter.toDisplay(expr);
        showInlineResult(left, shown);
        com.calculadora.cientifica.util.HistoryModel.getInstance().addDerivative(expr, v, shown);
        setStatus("OK");
    } catch (Throwable ex) {
        setStatus("Error: " + ex.getMessage());
    }
}


private void doIntegrateIndef() {
    String v = (String) comboVariable.getSelectedItem();
    String expr = ExpressionValidator.cleanExpression(MathFormatter.toEngine(txtDisplay.getText()));
    if (!ExpressionValidator.isValidExpression(expr, v)) { setStatus("Expresión inválida para integrar."); return; }
    setStatus("Integrando...");
    try {
        String F = calc.integrate(expr, v);
        String shown = ensurePlusC(MathFormatter.toDisplay(F));
        String left = "∫ " + MathFormatter.toDisplay(expr) + " d" + v;
        showInlineResult(left, shown);
        com.calculadora.cientifica.util.HistoryModel.getInstance().addIntegralIndef(expr, v, shown);
        setStatus("OK");
    } catch (Throwable ex) {
        setStatus("Error: " + ex.getMessage());
    }
}

private void doIntegrateDef() {
    String v = (String) comboVariable.getSelectedItem();
    String expr = ExpressionValidator.cleanExpression(MathFormatter.toEngine(txtDisplay.getText()));
    if (!ExpressionValidator.isValidExpression(expr, v)) { setStatus("Expresión inválida."); return; }

    String aText = (txtA.getText() == null) ? "" : txtA.getText().trim();
    String bText = (txtB.getText() == null) ? "" : txtB.getText().trim();
    if (aText.isEmpty() || bText.isEmpty()) {
        setStatus("Complete a y b.");
        if (aText.isEmpty()) txtA.requestFocusInWindow(); else txtB.requestFocusInWindow();
        return;
    }

    final double a, b;
    try {
        a = com.calculadora.cientifica.core.CalculationService.parseBound(aText);
        b = com.calculadora.cientifica.core.CalculationService.parseBound(bText);
    } catch (Exception e) {
        setStatus("Intervalo a/b inválido.");
        return;
    }
    setStatus("Integrando en [" + MathFormatter.formatDouble(a) + ", " + MathFormatter.formatDouble(b) + "]...");
    btnIntegrarDef.setEnabled(false);
    btnEquals.setEnabled(false);

    // Trabajo pesado fuera del EDT para evitar congelar la UI.
    new javax.swing.SwingWorker<Double, Void>() {
        @Override protected Double doInBackground() {
            return calc.integrateDefinite(expr, v, a, b);
        }
        @Override protected void done() {
            try {
                double area = get();
                String res = MathFormatter.formatDouble(area);
                String left = "∫[" + MathFormatter.formatDouble(a) + "," + MathFormatter.formatDouble(b) + "] "
                              + MathFormatter.toDisplay(expr) + " d" + v;
                showInlineResult(left, res);
                com.calculadora.cientifica.util.HistoryModel.getInstance().addIntegralDef(expr, v, a, b, res);
                setStatus("OK");
            } catch (Exception ex) {
                setStatus("Error: " + ex.getMessage());
            } finally {
                btnIntegrarDef.setEnabled(true);
                btnEquals.setEnabled(true);
            }
        }
    }.execute();
}

// HISTORIAL 
private void openHistoryDialog() {
    javax.swing.JDialog dlg = new javax.swing.JDialog(this, "Historial", true);
    javax.swing.DefaultListModel<String> model = new javax.swing.DefaultListModel<>();
    java.util.List<String> lines = com.calculadora.cientifica.util.HistoryModel.getInstance().toLines();
    for (String line : lines) model.addElement(line);

    javax.swing.JList<String> list = new javax.swing.JList<>(model);
    list.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
    list.addListSelectionListener(e -> {
        if (!e.getValueIsAdjusting()) {
            String item = list.getSelectedValue();
            if (item != null) {
                int idx = item.indexOf(": ");
                String right = idx >= 0 ? item.substring(idx + 2) : item;
                int eq = right.indexOf(" = ");
                String expr = eq > 0 ? right.substring(0, eq) : right;
                txtDisplay.setText(expr);
                txtDisplay.setCaretPosition(expr.length());
                txtDisplay.requestFocusInWindow();   // <-- agrega
                showEditor();                        // <-- agrega
            }
        }
    });
    dlg.getContentPane().add(new javax.swing.JScrollPane(list));
    dlg.setSize(420, 320);
    dlg.setLocationRelativeTo(this);
    dlg.setVisible(true);
}

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        panelTop = new javax.swing.JPanel();
        panelSuperior = new javax.swing.JPanel();
        lblVariable = new javax.swing.JLabel();
        comboVariable = new javax.swing.JComboBox<>();
        btnDerivar = new javax.swing.JButton();
        btnIntegrar = new javax.swing.JButton();
        btnHistorial = new javax.swing.JButton();
        jLabel2 = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        txtDisplay = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        panelCentro = new javax.swing.JPanel();
        panelCientifico = new javax.swing.JPanel();
        btnSin = new javax.swing.JButton();
        btnCos = new javax.swing.JButton();
        btnTan = new javax.swing.JButton();
        btnLog = new javax.swing.JButton();
        btnLn = new javax.swing.JButton();
        btnExp = new javax.swing.JButton();
        btnPow = new javax.swing.JButton();
        btnSqrt = new javax.swing.JButton();
        btnLPar = new javax.swing.JButton();
        btnRPar = new javax.swing.JButton();
        btnPi = new javax.swing.JButton();
        btnE = new javax.swing.JButton();
        btnAbs = new javax.swing.JButton();
        btnAbsWrap = new javax.swing.JButton();
        btnSqr = new javax.swing.JButton();
        panelNumerico = new javax.swing.JPanel();
        btnN7 = new javax.swing.JButton();
        btnN8 = new javax.swing.JButton();
        btnN9 = new javax.swing.JButton();
        btnDiv = new javax.swing.JButton();
        btnN4 = new javax.swing.JButton();
        btnN5 = new javax.swing.JButton();
        btnN6 = new javax.swing.JButton();
        btnMul = new javax.swing.JButton();
        btnN1 = new javax.swing.JButton();
        btnN2 = new javax.swing.JButton();
        btnN3 = new javax.swing.JButton();
        btnSub = new javax.swing.JButton();
        btnN0 = new javax.swing.JButton();
        btnDot = new javax.swing.JButton();
        btnEquals = new javax.swing.JButton();
        btnAdd = new javax.swing.JButton();
        btnInsert = new javax.swing.JButton();
        btnBack = new javax.swing.JButton();
        btnClear = new javax.swing.JButton();
        btnDel = new javax.swing.JButton();
        panelInferior = new javax.swing.JPanel();
        lblIntervalo = new javax.swing.JLabel();
        txtA = new javax.swing.JTextField();
        txtB = new javax.swing.JTextField();
        btnIntegrarDef = new javax.swing.JButton();
        lblEstado = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Calculadora Científica");
        setFont(new java.awt.Font("Agency FB", 0, 18)); // NOI18N
        setMinimumSize(new java.awt.Dimension(640, 520));
        setPreferredSize(new java.awt.Dimension(700, 600));
        getContentPane().setLayout(new java.awt.BorderLayout(6, 6));

        panelTop.setLayout(new java.awt.BorderLayout(6, 10));

        panelSuperior.setBackground(new java.awt.Color(255, 255, 255));
        panelSuperior.setAlignmentY(48.0F);
        java.awt.FlowLayout flowLayout1 = new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 12, 15);
        flowLayout1.setAlignOnBaseline(true);
        panelSuperior.setLayout(flowLayout1);

        lblVariable.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lblVariable.setText("Variable:");
        panelSuperior.add(lblVariable);

        comboVariable.setBackground(new java.awt.Color(102, 102, 255));
        comboVariable.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        comboVariable.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "x", "y" }));
        panelSuperior.add(comboVariable);

        btnDerivar.setBackground(new java.awt.Color(102, 102, 255));
        btnDerivar.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        btnDerivar.setForeground(new java.awt.Color(255, 255, 255));
        btnDerivar.setText("Derivar");
        btnDerivar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDerivarActionPerformed(evt);
            }
        });
        panelSuperior.add(btnDerivar);

        btnIntegrar.setBackground(new java.awt.Color(102, 102, 255));
        btnIntegrar.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        btnIntegrar.setForeground(new java.awt.Color(255, 255, 255));
        btnIntegrar.setText("Integral indef.");
        btnIntegrar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnIntegrarActionPerformed(evt);
            }
        });
        panelSuperior.add(btnIntegrar);

        btnHistorial.setBackground(new java.awt.Color(255, 102, 51));
        btnHistorial.setForeground(new java.awt.Color(255, 255, 255));
        btnHistorial.setText("Historial");
        btnHistorial.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnHistorialActionPerformed(evt);
            }
        });
        panelSuperior.add(btnHistorial);

        jLabel2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/img/simbolo-de-interfaz-de-historial-de-navegacion-de-un-reloj-con-una-flecha.png"))); // NOI18N
        panelSuperior.add(jLabel2);
        panelSuperior.add(jLabel1);

        panelTop.add(panelSuperior, java.awt.BorderLayout.PAGE_START);

        txtDisplay.setFont(new java.awt.Font("Segoe UI", 0, 20)); // NOI18N
        txtDisplay.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        txtDisplay.setAlignmentY(20.0F);
        txtDisplay.setPreferredSize(new java.awt.Dimension(40, 44));
        txtDisplay.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtDisplayActionPerformed(evt);
            }
        });
        panelTop.add(txtDisplay, java.awt.BorderLayout.CENTER);

        jLabel3.setIcon(new javax.swing.ImageIcon(getClass().getResource("/img/derivado.png"))); // NOI18N
        panelTop.add(jLabel3, java.awt.BorderLayout.LINE_START);

        getContentPane().add(panelTop, java.awt.BorderLayout.NORTH);

        panelCentro.setLayout(new java.awt.BorderLayout(6, 6));

        panelCientifico.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 8, 8, 8));
        panelCientifico.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        panelCientifico.setPreferredSize(new java.awt.Dimension(50, 140));
        panelCientifico.setRequestFocusEnabled(false);
        panelCientifico.setLayout(new java.awt.GridLayout(3, 2, 8, 10));

        btnSin.setBackground(new java.awt.Color(153, 153, 255));
        btnSin.setText("SIN");
        btnSin.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSinActionPerformed(evt);
            }
        });
        panelCientifico.add(btnSin);

        btnCos.setBackground(new java.awt.Color(153, 153, 255));
        btnCos.setText("COS");
        btnCos.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCosActionPerformed(evt);
            }
        });
        panelCientifico.add(btnCos);

        btnTan.setBackground(new java.awt.Color(153, 153, 255));
        btnTan.setText("TAN");
        btnTan.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnTanActionPerformed(evt);
            }
        });
        panelCientifico.add(btnTan);

        btnLog.setBackground(new java.awt.Color(153, 153, 255));
        btnLog.setText("LOG");
        btnLog.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnLogActionPerformed(evt);
            }
        });
        panelCientifico.add(btnLog);

        btnLn.setBackground(new java.awt.Color(153, 153, 255));
        btnLn.setText("LN");
        btnLn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnLnActionPerformed(evt);
            }
        });
        panelCientifico.add(btnLn);

        btnExp.setBackground(new java.awt.Color(153, 153, 255));
        btnExp.setText("EXP");
        btnExp.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnExpActionPerformed(evt);
            }
        });
        panelCientifico.add(btnExp);

        btnPow.setBackground(new java.awt.Color(153, 153, 255));
        btnPow.setText("^");
        btnPow.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnPowActionPerformed(evt);
            }
        });
        panelCientifico.add(btnPow);

        btnSqrt.setBackground(new java.awt.Color(153, 153, 255));
        btnSqrt.setText("√");
        btnSqrt.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSqrtActionPerformed(evt);
            }
        });
        panelCientifico.add(btnSqrt);

        btnLPar.setBackground(new java.awt.Color(153, 153, 255));
        btnLPar.setText("(");
        btnLPar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnLParActionPerformed(evt);
            }
        });
        panelCientifico.add(btnLPar);

        btnRPar.setBackground(new java.awt.Color(153, 153, 255));
        btnRPar.setText(")");
        btnRPar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRParActionPerformed(evt);
            }
        });
        panelCientifico.add(btnRPar);

        btnPi.setBackground(new java.awt.Color(153, 153, 255));
        btnPi.setText("π");
        btnPi.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnPiActionPerformed(evt);
            }
        });
        panelCientifico.add(btnPi);

        btnE.setBackground(new java.awt.Color(153, 153, 255));
        btnE.setText("e");
        btnE.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnEActionPerformed(evt);
            }
        });
        panelCientifico.add(btnE);

        btnAbs.setBackground(new java.awt.Color(245, 145, 61));
        btnAbs.setText("abs");
        btnAbs.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAbsActionPerformed(evt);
            }
        });
        panelCientifico.add(btnAbs);

        btnAbsWrap.setBackground(new java.awt.Color(244, 143, 61));
        btnAbsWrap.setText("|x|");
        btnAbsWrap.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAbsWrapActionPerformed(evt);
            }
        });
        panelCientifico.add(btnAbsWrap);

        btnSqr.setBackground(new java.awt.Color(244, 143, 61));
        btnSqr.setText("x^2");
        btnSqr.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSqrActionPerformed(evt);
            }
        });
        panelCientifico.add(btnSqr);

        panelCentro.add(panelCientifico, java.awt.BorderLayout.NORTH);

        panelNumerico.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 8, 8, 8));
        panelNumerico.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        panelNumerico.setMinimumSize(new java.awt.Dimension(306, 100));
        panelNumerico.setPreferredSize(new java.awt.Dimension(700, 250));
        panelNumerico.setLayout(new java.awt.GridLayout(5, 4, 6, 6));

        btnN7.setText("7");
        btnN7.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnN7ActionPerformed(evt);
            }
        });
        panelNumerico.add(btnN7);

        btnN8.setText("8");
        btnN8.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnN8ActionPerformed(evt);
            }
        });
        panelNumerico.add(btnN8);

        btnN9.setText("9");
        btnN9.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnN9ActionPerformed(evt);
            }
        });
        panelNumerico.add(btnN9);

        btnDiv.setText("/");
        btnDiv.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDivActionPerformed(evt);
            }
        });
        panelNumerico.add(btnDiv);

        btnN4.setText("4");
        btnN4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnN4ActionPerformed(evt);
            }
        });
        panelNumerico.add(btnN4);

        btnN5.setText("5");
        btnN5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnN5ActionPerformed(evt);
            }
        });
        panelNumerico.add(btnN5);

        btnN6.setText("6");
        btnN6.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnN6ActionPerformed(evt);
            }
        });
        panelNumerico.add(btnN6);

        btnMul.setText("*");
        btnMul.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnMulActionPerformed(evt);
            }
        });
        panelNumerico.add(btnMul);

        btnN1.setText("1");
        btnN1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnN1ActionPerformed(evt);
            }
        });
        panelNumerico.add(btnN1);

        btnN2.setText("2");
        btnN2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnN2ActionPerformed(evt);
            }
        });
        panelNumerico.add(btnN2);

        btnN3.setText("3");
        btnN3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnN3ActionPerformed(evt);
            }
        });
        panelNumerico.add(btnN3);

        btnSub.setText("-");
        btnSub.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSubActionPerformed(evt);
            }
        });
        panelNumerico.add(btnSub);

        btnN0.setText("0");
        btnN0.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnN0ActionPerformed(evt);
            }
        });
        panelNumerico.add(btnN0);

        btnDot.setFont(new java.awt.Font("Segoe UI", 0, 24)); // NOI18N
        btnDot.setText(".");
        btnDot.setAlignmentY(0.1F);
        btnDot.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDotActionPerformed(evt);
            }
        });
        panelNumerico.add(btnDot);

        btnEquals.setBackground(new java.awt.Color(102, 255, 51));
        btnEquals.setText("=");
        btnEquals.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnEqualsActionPerformed(evt);
            }
        });
        panelNumerico.add(btnEquals);

        btnAdd.setText("+");
        btnAdd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAddActionPerformed(evt);
            }
        });
        panelNumerico.add(btnAdd);

        btnInsert.setBackground(new java.awt.Color(255, 204, 153));
        btnInsert.setText("INS");
        btnInsert.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnInsertActionPerformed(evt);
            }
        });
        panelNumerico.add(btnInsert);

        btnBack.setBackground(new java.awt.Color(255, 204, 153));
        btnBack.setIcon(new javax.swing.ImageIcon(getClass().getResource("/img/borrador-borrar (2).png"))); // NOI18N
        btnBack.setText("<-");
        btnBack.setDisplayedMnemonicIndex(1);
        btnBack.setMaximumSize(new java.awt.Dimension(50, 50));
        btnBack.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnBackActionPerformed(evt);
            }
        });
        panelNumerico.add(btnBack);

        btnClear.setBackground(new java.awt.Color(102, 255, 51));
        btnClear.setText("CLR");
        btnClear.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnClearActionPerformed(evt);
            }
        });
        panelNumerico.add(btnClear);

        btnDel.setBackground(new java.awt.Color(255, 51, 51));
        btnDel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/img/borrar (1).png"))); // NOI18N
        btnDel.setText("DEL");
        btnDel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDelActionPerformed(evt);
            }
        });
        panelNumerico.add(btnDel);

        panelCentro.add(panelNumerico, java.awt.BorderLayout.CENTER);

        panelInferior.setBorder(javax.swing.BorderFactory.createEmptyBorder(6, 8, 6, 8));
        panelInferior.setMinimumSize(new java.awt.Dimension(28, 40));
        panelInferior.setPreferredSize(new java.awt.Dimension(706, 60));
        panelInferior.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 10, 8));

        lblIntervalo.setText("Integral definida a→b:");
        panelInferior.add(lblIntervalo);

        txtA.setPreferredSize(new java.awt.Dimension(70, 30));
        txtA.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtAActionPerformed(evt);
            }
        });
        panelInferior.add(txtA);

        txtB.setPreferredSize(new java.awt.Dimension(70, 30));
        txtB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtBActionPerformed(evt);
            }
        });
        panelInferior.add(txtB);

        btnIntegrarDef.setText("∫ a→b");
        btnIntegrarDef.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnIntegrarDefActionPerformed(evt);
            }
        });
        panelInferior.add(btnIntegrarDef);

        lblEstado.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        lblEstado.setText("Listo.");
        panelInferior.add(lblEstado);

        jLabel4.setIcon(new javax.swing.ImageIcon(getClass().getResource("/img/calculadora.png"))); // NOI18N
        panelInferior.add(jLabel4);

        panelCentro.add(panelInferior, java.awt.BorderLayout.SOUTH);

        getContentPane().add(panelCentro, java.awt.BorderLayout.CENTER);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnDerivarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDerivarActionPerformed
        // TODO add your handling code here:
        enterDerivMode();
    }//GEN-LAST:event_btnDerivarActionPerformed

    private void btnIntegrarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnIntegrarActionPerformed
        // TODO add your handling code here:
         enterIntegralMode();

    }//GEN-LAST:event_btnIntegrarActionPerformed

    private void btnEqualsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEqualsActionPerformed
        // TODO add your handling code here:
        evalEquals();
    }//GEN-LAST:event_btnEqualsActionPerformed

    private void btnInsertActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnInsertActionPerformed
        // TODO add your handling code here:
        setStatus("Inserción");
    }//GEN-LAST:event_btnInsertActionPerformed

    private void btnBackActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBackActionPerformed
        // TODO add your handling code here:
        backspace();
    }//GEN-LAST:event_btnBackActionPerformed

    private void btnDelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDelActionPerformed
        // TODO add your handling code here:
        deleteForward();
    }//GEN-LAST:event_btnDelActionPerformed

    private void btnClearActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnClearActionPerformed
        // TODO add your handling code here:
        clearAll();
    }//GEN-LAST:event_btnClearActionPerformed

    private void btnSinActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSinActionPerformed
        // TODO add your handling code here:
        insertToken("sin(", true);
    }//GEN-LAST:event_btnSinActionPerformed

    private void btnCosActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCosActionPerformed
        // TODO add your handling code here:
        insertToken("cos(", true);
    }//GEN-LAST:event_btnCosActionPerformed

    private void btnTanActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnTanActionPerformed
        // TODO add your handling code here:
        insertToken("tan(", true);
    }//GEN-LAST:event_btnTanActionPerformed

    private void btnLogActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnLogActionPerformed
        // TODO add your handling code here:
        insertToken("log10(", true);
    }//GEN-LAST:event_btnLogActionPerformed

    private void btnLnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnLnActionPerformed
        // TODO add your handling code here:
        insertToken("ln(", true);
    }//GEN-LAST:event_btnLnActionPerformed

    private void btnExpActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnExpActionPerformed
        // TODO add your handling code here:
      insertToken("exp(", true);
    }//GEN-LAST:event_btnExpActionPerformed

    private void btnIntegrarDefActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnIntegrarDefActionPerformed
        // TODO add your handling code here:
        doIntegrateDef();
    }//GEN-LAST:event_btnIntegrarDefActionPerformed

    private void btnHistorialActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnHistorialActionPerformed
        // TODO add your handling code here:
        openHistoryDialog();

    }//GEN-LAST:event_btnHistorialActionPerformed

    private void txtDisplayActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtDisplayActionPerformed
        // TODO add your handling code here:
        evalEquals();
    }//GEN-LAST:event_txtDisplayActionPerformed

    private void btnRParActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRParActionPerformed
        // TODO add your handling code here:
        insertText(")");
    }//GEN-LAST:event_btnRParActionPerformed

    private void btnPowActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPowActionPerformed
        // TODO add your handling code here:
       insertText("^");
    }//GEN-LAST:event_btnPowActionPerformed

    private void btnSqrtActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSqrtActionPerformed
        // TODO add your handling code here:
        insertToken("√(", true);
    }//GEN-LAST:event_btnSqrtActionPerformed

    private void btnLParActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnLParActionPerformed
        // TODO add your handling code here:
        insertText("(");
    }//GEN-LAST:event_btnLParActionPerformed

    private void btnPiActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPiActionPerformed
        // TODO add your handling code here:
        insertText("π");
    }//GEN-LAST:event_btnPiActionPerformed

    private void btnEActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEActionPerformed
        // TODO add your handling code here:
         insertText("e");
    }//GEN-LAST:event_btnEActionPerformed

    private void btnAbsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAbsActionPerformed
        // TODO add your handling code here:
         insertToken("abs(", true);
    }//GEN-LAST:event_btnAbsActionPerformed

    private void btnAbsWrapActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAbsWrapActionPerformed
        // TODO add your handling code here:
        wrapWith("|", "|");
    }//GEN-LAST:event_btnAbsWrapActionPerformed

    private void btnSqrActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSqrActionPerformed
        // TODO add your handling code here:
        insertText("^2"); 
    }//GEN-LAST:event_btnSqrActionPerformed

    private void btnN7ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnN7ActionPerformed
        // TODO add your handling code here:
        insertText("7");
    }//GEN-LAST:event_btnN7ActionPerformed

    private void btnN8ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnN8ActionPerformed
        // TODO add your handling code here:
        insertText("8");
    }//GEN-LAST:event_btnN8ActionPerformed

    private void btnN9ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnN9ActionPerformed
        // TODO add your handling code here:
        insertText("9");
    }//GEN-LAST:event_btnN9ActionPerformed

    private void btnN4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnN4ActionPerformed
        // TODO add your handling code here:
        insertText("4");
    }//GEN-LAST:event_btnN4ActionPerformed

    private void btnN5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnN5ActionPerformed
        // TODO add your handling code here:
        insertText("5");
    }//GEN-LAST:event_btnN5ActionPerformed

    private void btnN6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnN6ActionPerformed
        // TODO add your handling code here:
        insertText("6");
    }//GEN-LAST:event_btnN6ActionPerformed

    private void btnN1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnN1ActionPerformed
        // TODO add your handling code here:
        insertText("1");
    }//GEN-LAST:event_btnN1ActionPerformed

    private void btnN2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnN2ActionPerformed
        // TODO add your handling code here:
        insertText("2");
    }//GEN-LAST:event_btnN2ActionPerformed

    private void btnN3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnN3ActionPerformed
        // TODO add your handling code here:
        insertText("3");
    }//GEN-LAST:event_btnN3ActionPerformed

    private void btnN0ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnN0ActionPerformed
        // TODO add your handling code here:
        insertText("0");
    }//GEN-LAST:event_btnN0ActionPerformed

    private void btnDotActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDotActionPerformed
        // TODO add your handling code here:
        insertText(".");
    }//GEN-LAST:event_btnDotActionPerformed

    private void btnDivActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDivActionPerformed
        // TODO add your handling code here:
        insertText("/");
    }//GEN-LAST:event_btnDivActionPerformed

    private void btnMulActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnMulActionPerformed
        // TODO add your handling code here:
        insertText("*");
    }//GEN-LAST:event_btnMulActionPerformed

    private void btnSubActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSubActionPerformed
        // TODO add your handling code here:
        insertText("-");
    }//GEN-LAST:event_btnSubActionPerformed

    private void btnAddActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAddActionPerformed
        // TODO add your handling code here:
        insertText("+");
    }//GEN-LAST:event_btnAddActionPerformed

    private void txtAActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtAActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtAActionPerformed

    private void txtBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtBActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtBActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException ex) {
            logger.log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> new MainFrame().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnAbs;
    private javax.swing.JButton btnAbsWrap;
    private javax.swing.JButton btnAdd;
    private javax.swing.JButton btnBack;
    private javax.swing.JButton btnClear;
    private javax.swing.JButton btnCos;
    private javax.swing.JButton btnDel;
    private javax.swing.JButton btnDerivar;
    private javax.swing.JButton btnDiv;
    private javax.swing.JButton btnDot;
    private javax.swing.JButton btnE;
    private javax.swing.JButton btnEquals;
    private javax.swing.JButton btnExp;
    private javax.swing.JButton btnHistorial;
    private javax.swing.JButton btnInsert;
    private javax.swing.JButton btnIntegrar;
    private javax.swing.JButton btnIntegrarDef;
    private javax.swing.JButton btnLPar;
    private javax.swing.JButton btnLn;
    private javax.swing.JButton btnLog;
    private javax.swing.JButton btnMul;
    private javax.swing.JButton btnN0;
    private javax.swing.JButton btnN1;
    private javax.swing.JButton btnN2;
    private javax.swing.JButton btnN3;
    private javax.swing.JButton btnN4;
    private javax.swing.JButton btnN5;
    private javax.swing.JButton btnN6;
    private javax.swing.JButton btnN7;
    private javax.swing.JButton btnN8;
    private javax.swing.JButton btnN9;
    private javax.swing.JButton btnPi;
    private javax.swing.JButton btnPow;
    private javax.swing.JButton btnRPar;
    private javax.swing.JButton btnSin;
    private javax.swing.JButton btnSqr;
    private javax.swing.JButton btnSqrt;
    private javax.swing.JButton btnSub;
    private javax.swing.JButton btnTan;
    private javax.swing.JComboBox<String> comboVariable;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel lblEstado;
    private javax.swing.JLabel lblIntervalo;
    private javax.swing.JLabel lblVariable;
    private javax.swing.JPanel panelCentro;
    private javax.swing.JPanel panelCientifico;
    private javax.swing.JPanel panelInferior;
    private javax.swing.JPanel panelNumerico;
    private javax.swing.JPanel panelSuperior;
    private javax.swing.JPanel panelTop;
    private javax.swing.JTextField txtA;
    private javax.swing.JTextField txtB;
    private javax.swing.JTextField txtDisplay;
    // End of variables declaration//GEN-END:variables
}
