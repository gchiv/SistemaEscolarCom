package org.example.ui;

import org.example.db.ResultSetTableModel;

import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.time.LocalDate; // Importante para la fecha actual

public class AsistenciasPanel extends JPanel {

    // Consulta para mostrar los datos de una forma más legible para el usuario
    private static final String SQL_SELECT = """
            SELECT * FROM asistencias;
            """;

    private final Connection conn;
    private final JTable table = new JTable();
    private final JButton btnReload = new JButton("Recargar");
    // --- NUEVO BOTÓN ---
    private final JButton btnInsert = new JButton("Realizar Insert");

    public AsistenciasPanel(Connection conn) {
        this.conn = conn;
        setLayout(new BorderLayout(8, 8));

        // --- PANEL SUPERIOR MODIFICADO PARA ALINEAR A LA DERECHA ---
        JPanel top = new JPanel(new BorderLayout()); // Se cambia a BorderLayout
        top.add(new JLabel("Asistencias"), BorderLayout.WEST); // Título a la izquierda

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT)); // Panel para botones a la derecha
        buttonPanel.add(btnInsert); // Se añade el nuevo botón
        buttonPanel.add(btnReload);

        top.add(buttonPanel, BorderLayout.EAST); // Se añade el panel de botones a la derecha del panel superior

        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);

        // --- ACCIONES DE LOS BOTONES ---
        btnReload.addActionListener(e -> loadData());
        btnInsert.addActionListener(e -> showInsertDialog()); // Acción para el nuevo botón

        loadData(); // Carga inicial de datos
    }

    private void loadData() {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(SQL_SELECT)) { // Usamos la consulta con JOINs

            table.setModel(ResultSetTableModel.fromResultSet(rs));
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error al cargar asistencias:\n" + ex.getMessage(),
                    "SQL Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // --- NUEVO MÉTODO PARA MOSTRAR LA VENTANA DE INSERCIÓN ---
    private void showInsertDialog() {
        // 1. Crear los componentes del formulario
        JTextField idInscripcionField = new JTextField(10);
        JTextField fechaField = new JTextField(LocalDate.now().toString()); // Sugiere la fecha de hoy

        // 2. Organizar los componentes en un panel
        JPanel formPanel = new JPanel(new GridLayout(0, 2, 5, 5));
        formPanel.add(new JLabel("ID Inscripción:"));
        formPanel.add(idInscripcionField);
        formPanel.add(new JLabel("Fecha (YYYY-MM-DD):"));
        formPanel.add(fechaField);

        // 3. Mostrar el JOptionPane con el panel personalizado
        int result = JOptionPane.showConfirmDialog(this, formPanel, "Registrar Asistencia",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        // 4. Procesar la entrada si el usuario hace clic en "OK"
        if (result == JOptionPane.OK_OPTION) {
            try {
                int idInscripcion = Integer.parseInt(idInscripcionField.getText());
                String fecha = fechaField.getText();

                // Validar que la fecha no esté vacía
                if (fecha.trim().isEmpty()) {
                    JOptionPane.showMessageDialog(this, "La fecha no puede estar vacía.", "Error de Validación", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // 5. Ejecutar el INSERT con PreparedStatement para seguridad
                String sql = "INSERT INTO asistencias (id_inscripcion, fecha) VALUES (?, ?);";
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setInt(1, idInscripcion);
                    pstmt.setDate(2, Date.valueOf(fecha)); // Convierte String a java.sql.Date

                    int affectedRows = pstmt.executeUpdate();
                    if (affectedRows > 0) {
                        JOptionPane.showMessageDialog(this, "Asistencia registrada exitosamente!");
                        loadData(); // Recargar la tabla para ver el nuevo registro
                    }
                }

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "El ID de inscripción debe ser un número.", "Error de Formato", JOptionPane.ERROR_MESSAGE);
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(this, "El formato de la fecha es incorrecto. Use YYYY-MM-DD.", "Error de Formato", JOptionPane.ERROR_MESSAGE);
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error al guardar en la base de datos:\n" + ex.getMessage(), "Error SQL", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}