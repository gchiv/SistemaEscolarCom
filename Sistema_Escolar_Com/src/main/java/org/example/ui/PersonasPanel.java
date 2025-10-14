package org.example.ui;

import org.example.db.ResultSetTableModel;

import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class PersonasPanel extends JPanel {
    private static final String TABLE_NAME = "personas_escuela"; // ajusta si es singular
    private final Connection conn;
    private final JTable table = new JTable();
    private final JButton btnReload = new JButton("Recargar");

    public PersonasPanel(Connection conn) {
        this.conn = conn;
        setLayout(new BorderLayout(8, 8));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel title = new JLabel("Listado de Personas (tabla: " + TABLE_NAME + ")");
        top.add(title);
        top.add(btnReload);

        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);

        btnReload.addActionListener(e -> loadData());
        loadData();
    }

    private void loadData() {
        String sql = "SELECT * FROM " + TABLE_NAME;
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            table.setModel(ResultSetTableModel.fromResultSet(rs));
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error al cargar personas:\n" + ex.getMessage(),
                    "SQL Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
