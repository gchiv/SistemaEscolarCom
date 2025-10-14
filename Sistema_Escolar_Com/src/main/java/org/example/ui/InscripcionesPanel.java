package org.example.ui;

import org.example.db.ResultSetTableModel;

import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class InscripcionesPanel extends JPanel {
    // Opción A: tabla simple
    // private static final String SQL = "SELECT * FROM inscripciones";

    // Opción B: vista combinada (ajusta nombres de columnas/llaves)
    private static final String SQL = """
            SELECT i.id AS inscripcion_id,
                   p.id AS persona_id, p.nombre AS persona_nombre, p.apellido AS persona_apellido,
                   m.id AS materia_id, m.nombre AS materia_nombre,
                   i.fecha AS fecha_inscripcion
            FROM inscripciones i
            JOIN personas_escuela p ON p.id = i.persona_id
            JOIN materias m         ON m.id = i.materia_id
            ORDER BY i.fecha DESC
            """;

    private final Connection conn;
    private final JTable table = new JTable();
    private final JButton btnReload = new JButton("Recargar");

    public InscripcionesPanel(Connection conn) {
        this.conn = conn;
        setLayout(new BorderLayout(8,8));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Inscripciones"));
        top.add(btnReload);

        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);

        btnReload.addActionListener(e -> loadData());
        loadData();
    }

    private void loadData() {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(SQL)) {

            table.setModel(ResultSetTableModel.fromResultSet(rs));
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error al cargar inscripciones:\n" + ex.getMessage(),
                    "SQL Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
