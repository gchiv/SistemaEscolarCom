package org.example.ui;

import org.example.db.ResultSetTableModel;

import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class MateriasPanel extends JPanel {
    private static final String TABLE_NAME = "materias";
    private final Connection conn;
    private final JTable table = new JTable();
    private final JButton btnReload = new JButton("Recargar");

    public MateriasPanel(Connection conn) {
        this.conn = conn;
        setLayout(new BorderLayout(8,8));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Listado de Materias (tabla: " + TABLE_NAME + ")"));
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
            JOptionPane.showMessageDialog(this, "Error al cargar materias:\n" + ex.getMessage(),
                    "SQL Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
