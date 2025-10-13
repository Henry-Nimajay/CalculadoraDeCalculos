/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package gestorjugadores;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.*;

/**
 * Clase principal que implementa una interfaz gráfica (GUI) para realizar
 * operaciones CRUD sobre la tabla 'jugadores' de la base de datos 'videojuegos'
 * utilizando Java Swing y JDBC.
 */
public class GestorJugadores extends JFrame {

    // --- CONFIGURACIÓN DE LA CONEXIÓN A LA BASE DE DATOS ---
    private static final String DB_URL = "jdbc:mysql://localhost:3306/videojuegos";
    private static final String USER = "root"; 
    private static final String PASS = "admin"; 
    
    private Connection conexion;

    // Componentes de entrada para Agregar Jugador
    private JTextField nombreField, nivelField, puntosField;
    
    // Componentes de entrada para Actualizar/Banear
    private JTextField idUpdateField, nuevoNivelField, idBanField;
    
    // Componente para mostrar la tabla de jugadores
    private JTextArea displayArea;
    private JButton conectarButton; 

    /**
     * Constructor que configura la ventana principal y los paneles de pestañas (CRUD).
     */
    public GestorJugadores() {
        super("🎮 Gestor CRUD de Jugadores (Videojuegos)");

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        // Ajustamos el tamaño para que la GUI sea cómoda
        setSize(800, 400); 
        setLocationRelativeTo(null);
        
        // Botón de conexión en la parte superior
        conectarButton = new JButton("Conectar a la Base de Datos");
        conectarButton.addActionListener(e -> conectarABaseDeDatos());
        
        // JTabbedPane para organizar las 4 operaciones
        JTabbedPane tabbedPane = new JTabbedPane();
        
        tabbedPane.addTab("🧍‍♂️ 1. Agregar Jugador", crearPanelAgregar());
        tabbedPane.addTab("📋 2. Mostrar Jugadores", crearPanelMostrar());
        tabbedPane.addTab("⬆️ 3. Actualizar Nivel", crearPanelActualizar());
        tabbedPane.addTab("😅 4. Banear Jugador", crearPanelBanear());
        
        // Contenedor principal
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        // Agregar el botón de conexión en el norte
        mainPanel.add(conectarButton, BorderLayout.NORTH);
        // Agregar el panel de pestañas en el centro
        mainPanel.add(tabbedPane, BorderLayout.CENTER);

        add(mainPanel);
    }
    
    /**
     * Establece la conexión con la base de datos 'videojuegos'.
     */
    private void conectarABaseDeDatos() {
        try {
            // Cargar el driver JDBC
            Class.forName("com.mysql.cj.jdbc.Driver");
            // Establecer la conexión
            conexion = DriverManager.getConnection(DB_URL, USER, PASS);
            JOptionPane.showMessageDialog(this, "Conexión exitosa a la base de datos 'videojuegos'.", "Conexión", JOptionPane.INFORMATION_MESSAGE);
            conectarButton.setEnabled(false); // Deshabilitar después de la conexión exitosa
        } catch (ClassNotFoundException e) {
            JOptionPane.showMessageDialog(this, "Error: No se encontró el driver JDBC de MySQL. Asegúrate de incluir el JAR.", "Error de Conexión", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al conectar: Verifica DB_URL, USER y PASS. Mensaje: " + e.getMessage(), "Error de Conexión", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    /**
     * Verifica si la conexión está activa antes de cualquier operación SQL.
     */
    private void checkConnection() throws SQLException {
        if (conexion == null || conexion.isClosed()) {
            JOptionPane.showMessageDialog(this, "Error: ¡No hay conexión activa a la base de datos! Presiona 'Conectar'.", "Error de Conexión", JOptionPane.ERROR_MESSAGE);
            throw new SQLException("No hay conexión activa");
        }
    }

    // Panel para Agregar un nuevo jugador
    private JPanel crearPanelAgregar() {
        JPanel panel = new JPanel(new GridLayout(4, 2, 10, 10));
        
        nombreField = new JTextField(20);
        nivelField = new JTextField(20);
        puntosField = new JTextField(20);
        JButton guardarButton = new JButton("Guardar Jugador (Activo)");
        
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        panel.add(new JLabel("Nombre del Jugador:"));
        panel.add(nombreField);
        panel.add(new JLabel("Nivel Inicial (INT):"));
        panel.add(nivelField);
        panel.add(new JLabel("Puntos Iniciales (INT):"));
        panel.add(puntosField);
        panel.add(new JLabel("")); 
        panel.add(guardarButton);
        
        guardarButton.addActionListener(e -> agregarJugador());
        return panel;
    }

    // Panel para Mostrar todos los jugadores
    private JPanel crearPanelMostrar() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        displayArea = new JTextArea();
        displayArea.setFont(new Font("Monospaced", Font.PLAIN, 12)); // Fuente monoespaciada para alineación
        displayArea.setEditable(false);
        JButton mostrarButton = new JButton("Cargar y Mostrar Tabla Completa");
        
        mostrarButton.addActionListener(e -> mostrarJugadores());
        
        panel.add(mostrarButton, BorderLayout.NORTH);
        panel.add(new JScrollPane(displayArea), BorderLayout.CENTER);
        return panel;
    }

    // Panel para Actualizar el Nivel de un jugador
    private JPanel crearPanelActualizar() {
        JPanel panel = new JPanel(new GridLayout(3, 2, 10, 10));
        
        idUpdateField = new JTextField(20);
        nuevoNivelField = new JTextField(20);
        JButton actualizarButton = new JButton("Actualizar Nivel");
        
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        panel.add(new JLabel("ID del Jugador a Actualizar:"));
        panel.add(idUpdateField);
        panel.add(new JLabel("Nuevo Nivel (INT):"));
        panel.add(nuevoNivelField);
        panel.add(new JLabel(""));
        panel.add(actualizarButton);
        
        actualizarButton.addActionListener(e -> actualizarNivel());
        return panel;
    }

    // Panel para Banear (Eliminación Lógica) a un jugador
    private JPanel crearPanelBanear() {
        JPanel panel = new JPanel(new GridLayout(2, 2, 10, 10));
        
        idBanField = new JTextField(20);
        JButton banearButton = new JButton("Banear Jugador (Estado = 0)");
        
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        panel.add(new JLabel("ID del Jugador a BANEAR:"));
        panel.add(idBanField);
        panel.add(new JLabel("")); 
        panel.add(banearButton);
        
        banearButton.addActionListener(e -> banearJugador());
        return panel;
    }
    
    // 1. AGREGAR NUEVO JUGADOR (C)
    private void agregarJugador() {
        try {
            checkConnection();
            
            String nombre = nombreField.getText();
            // Parsear a entero, puede lanzar NumberFormatException
            int nivel = Integer.parseInt(nivelField.getText());
            int puntos = Integer.parseInt(puntosField.getText());

            String SQL = "INSERT INTO jugadores (nombre, nivel, puntos) VALUES (?, ?, ?)";
            try (PreparedStatement pstmt = conexion.prepareStatement(SQL)) {
                
                pstmt.setString(1, nombre);
                pstmt.setInt(2, nivel);
                pstmt.setInt(3, puntos);
                
                int filasAfectadas = pstmt.executeUpdate();
                if (filasAfectadas > 0) {
                    JOptionPane.showMessageDialog(this, "✅ Jugador '" + nombre + "' agregado exitosamente. Estado: Activo (1).", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                    // Limpiar campos
                    nombreField.setText("");
                    nivelField.setText("");
                    puntosField.setText("");
                }
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Error: Nivel y Puntos deben ser números enteros válidos.", "Error de Formato", JOptionPane.ERROR_MESSAGE);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "❌ Error al agregar el jugador: " + e.getMessage(), "Error SQL", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    // 2. MOSTRAR LA TABLA COMPLETA (R)
    private void mostrarJugadores() {
        try {
            checkConnection();
            
            String SQL = "SELECT id, nombre, nivel, puntos, estado FROM jugadores ORDER BY nivel DESC, puntos DESC";
            
            try (Statement stmt = conexion.createStatement();
                 ResultSet rs = stmt.executeQuery(SQL)) {
                
                StringBuilder sb = new StringBuilder();
                // Encabezado de la tabla formateado
                sb.append(String.format("%-5s | %-20s | %-6s | %-10s | %-15s\n", "ID", "NOMBRE", "NIVEL", "PUNTOS", "ESTADO"));
                sb.append("--------------------------------------------------------------------------\n");

                while (rs.next()) {
                    int id = rs.getInt("id");
                    String nombre = rs.getString("nombre");
                    int nivel = rs.getInt("nivel");
                    int puntos = rs.getInt("puntos");
                    int estado = rs.getInt("estado");
                    String estadoStr = (estado == 1) ? "ACTIVO 🟢" : "BANEADO 🔴";
                    
                    sb.append(String.format("%-5d | %-20s | %-6d | %-10d | %-15s\n", id, nombre, nivel, puntos, estadoStr));
                }
                displayArea.setText(sb.toString());
                
            }
        } catch (SQLException e) {
            displayArea.setText("❌ Error al mostrar jugadores: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // 3. ACTUALIZAR EL NIVEL DE UN JUGADOR (U)
    private void actualizarNivel() {
        try {
            checkConnection();

            int idJugador = Integer.parseInt(idUpdateField.getText());
            int nuevoNivel = Integer.parseInt(nuevoNivelField.getText());

            String SQL = "UPDATE jugadores SET nivel = ? WHERE id = ?";
            try (PreparedStatement pstmt = conexion.prepareStatement(SQL)) {
                
                pstmt.setInt(1, nuevoNivel);
                pstmt.setInt(2, idJugador);
                
                int filasAfectadas = pstmt.executeUpdate();
                if (filasAfectadas > 0) {
                    JOptionPane.showMessageDialog(this, "⬆️ Nivel del jugador ID " + idJugador + " actualizado a " + nuevoNivel + ".", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                    idUpdateField.setText("");
                    nuevoNivelField.setText("");
                } else {
                    JOptionPane.showMessageDialog(this, "⚠️ No se encontró ningún jugador con el ID " + idJugador + ".", "Advertencia", JOptionPane.WARNING_MESSAGE);
                }
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Error: ID y Nuevo Nivel deben ser números enteros válidos.", "Error de Formato", JOptionPane.ERROR_MESSAGE);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "❌ Error al actualizar el nivel: " + e.getMessage(), "Error SQL", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    // 4. ELIMINAR JUGADOR "BANEADO" (D - Lógica)
    private void banearJugador() {
        try {
            checkConnection();

            int idJugador = Integer.parseInt(idBanField.getText());

            // La eliminación es lógica: cambiar estado a 0 (Inactivo/Baneado)
            String SQL = "UPDATE jugadores SET estado = 0 WHERE id = ?";
            try (PreparedStatement pstmt = conexion.prepareStatement(SQL)) {
                
                pstmt.setInt(1, idJugador);
                
                int filasAfectadas = pstmt.executeUpdate();
                if (filasAfectadas > 0) {
                    JOptionPane.showMessageDialog(this, "❌ Jugador ID " + idJugador + " ha sido BANEADO (Estado: 0 Inactivo).", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                    idBanField.setText("");
                } else {
                    JOptionPane.showMessageDialog(this, "⚠️ No se encontró ningún jugador con el ID " + idJugador + ".", "Advertencia", JOptionPane.WARNING_MESSAGE);
                }
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Error: El ID del jugador debe ser un número entero válido.", "Error de Formato", JOptionPane.ERROR_MESSAGE);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "❌ Error al banear el jugador: " + e.getMessage(), "Error SQL", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
    
    public static void main(String[] args) {
        // Crear y mostrar la ventana en el hilo de la interfaz gráfica
        SwingUtilities.invokeLater(() -> new GestorJugadores().setVisible(true));
    }
}
