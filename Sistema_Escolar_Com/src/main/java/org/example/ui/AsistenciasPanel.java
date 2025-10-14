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
            SELECT * FROM asistencias;
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
