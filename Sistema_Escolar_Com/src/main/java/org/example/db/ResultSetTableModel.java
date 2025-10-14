package org.example.db;

import javax.swing.table.DefaultTableModel;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

public class ResultSetTableModel {
    public static DefaultTableModel fromResultSet(ResultSet rs) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        int cols = meta.getColumnCount();

        // Nombres de columnas
        String[] columnNames = new String[cols];
        for (int i = 1; i <= cols; i++) {
            columnNames[i - 1] = meta.getColumnLabel(i);
        }

        // Datos
        DefaultTableModel model = new DefaultTableModel(columnNames, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };

        while (rs.next()) {
            Object[] row = new Object[cols];
            for (int i = 1; i <= cols; i++) {
                row[i - 1] = rs.getObject(i);
            }
            model.addRow(row);
        }
        return model;
    }
}