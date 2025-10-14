package org.example.ui;

import org.example.db.ResultSetTableModel;

import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class AsistenciasPanel extends JPanel {
    // Opción A (tabla cruda):
    // private static final String SQL = "SELECT * FROM asistencias";

    // Opción B (JOIN legible):
    private static final String SQL = """
            SELECT a.id AS asistencia_id,
                   p.id AS persona_id, p.nombre AS persona_nombre, p.apellido AS persona_apellido,
                   m.id AS materia_id, m.nombre AS materia_nombre,
                   a.fecha, a.presente
            FROM asistencias a
            JOIN personas_escuela p ON p.id = a.persona_id
            JOIN materias m         ON m.id = a.materia_id
            ORDER BY a.fecha DESC
            """;

    private final Connection conn;
    private final JTable table = new JTable();
    private final JButton btnReload = new JButton("Recargar");

    public AsistenciasPanel(Connection conn) {
        this.conn = conn;
        setLayout(new BorderLayout(8,8));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Asistencias"));
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
            JOptionPane.showMessageDialog(this, "Error al cargar asistencias:\n" + ex.getMessage(),
                    "SQL Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
