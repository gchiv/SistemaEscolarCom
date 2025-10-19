package org.example.ui;

import org.example.ui.components.PanelHeader;
import org.example.ui.util.AdvancedUI;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class MateriasPanel extends JPanel {
    private final Connection conn;
    private final JTable table = new JTable();
    private final JLabel lblTotal = new JLabel("Total registros: 0");

    public MateriasPanel(Connection conn) {
        this.conn = conn;
        setLayout(new BorderLayout(8, 8));

        PanelHeader header = new PanelHeader("Materias");
        add(header, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(lblTotal, BorderLayout.SOUTH);

        lblTotal.setHorizontalAlignment(SwingConstants.RIGHT);
        lblTotal.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 12));

        table.setRowHeight(28);
        table.setAutoCreateRowSorter(true);
        table.getTableHeader().setReorderingAllowed(false);

        // --- Configurar edición de celdas ---
        table.putClientProperty("terminateEditOnFocusLost", true);

        // --- Botones ---
        header.btnReload.addActionListener(e -> loadData());
        header.btnInsert.addActionListener(e -> showInsertDialog());
        header.btnDelete.addActionListener(e -> deleteSelected());
        header.btnExport.addActionListener(e -> AdvancedUI.exportChooser(this, table));
        header.btnOrderAsc.addActionListener(e -> loadData("ASC"));
        header.btnOrderDesc.addActionListener(e -> loadData("DESC"));

        // --- Búsqueda en vivo ---
        header.searchField.getDocument().addDocumentListener(new DocumentListener() {
            private void search() {
                String q = header.searchField.getText().trim();
                if (q.isEmpty()) loadData();
                else loadDataFiltered(q);
            }

            @Override public void insertUpdate(DocumentEvent e) { search(); }
            @Override public void removeUpdate(DocumentEvent e) { search(); }
            @Override public void changedUpdate(DocumentEvent e) { search(); }
        });

        // Cargar datos iniciales
        loadData();

        // --- Guardar cambios automáticamente ---
        table.getModel().addTableModelListener(e -> {
            if (e.getType() != javax.swing.event.TableModelEvent.UPDATE) return;
            int row = e.getFirstRow();
            int col = e.getColumn();
            if (row < 0 || col < 0) return;

            String columnName = table.getColumnName(col);
            if (columnName.startsWith("id_")) return; // no editar ID

            Object newValue = table.getValueAt(row, col);
            int id = Integer.parseInt(table.getValueAt(row, 0).toString());

            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE materias SET " + columnName + " = ? WHERE id_materia = ?")) {
                ps.setObject(1, newValue);
                ps.setInt(2, id);
                ps.executeUpdate();
                AdvancedUI.showToast(MateriasPanel.this, "✅ Valor actualizado");
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(MateriasPanel.this,
                        "Error al guardar cambio:\n" + ex.getMessage(),
                        "SQL Error", JOptionPane.ERROR_MESSAGE);
                loadData(); // revertir vista
            }
        });
    }

    // ===========================================================
    // CARGAR DATOS
    // ===========================================================
    public void loadData() { loadData("ASC"); }

    public void loadData(String order) {
        String sql = "SELECT * FROM materias ORDER BY id_materia " + order;
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            DefaultTableModel model = buildEditableModel(rs);
            table.setModel(model);
            lblTotal.setText("Total registros: " + table.getRowCount());
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error al cargar:\n" + ex.getMessage(),
                    "SQL Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private DefaultTableModel buildEditableModel(ResultSet rs) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        int cols = meta.getColumnCount();
        String[] colNames = new String[cols];
        for (int i = 1; i <= cols; i++) colNames[i - 1] = meta.getColumnName(i);

        DefaultTableModel model = new DefaultTableModel(colNames, 0) {
            @Override public boolean isCellEditable(int row, int column) {
                return column != 0; // evitar editar el ID
            }
        };

        while (rs.next()) {
            Object[] rowData = new Object[cols];
            for (int i = 1; i <= cols; i++) rowData[i - 1] = rs.getObject(i);
            model.addRow(rowData);
        }

        return model;
    }

    // ===========================================================
    // BUSCAR
    // ===========================================================
    private void loadDataFiltered(String filtro) {
        String sql = "SELECT * FROM materias WHERE nombre LIKE ? OR descripcion LIKE ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + filtro + "%");
            ps.setString(2, "%" + filtro + "%");
            try (ResultSet rs = ps.executeQuery()) {
                table.setModel(buildEditableModel(rs));
                lblTotal.setText("Total registros: " + table.getRowCount());
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error al buscar:\n" + ex.getMessage(),
                    "SQL Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ===========================================================
    // ELIMINAR
    // ===========================================================
    private void deleteSelected() {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Seleccione una fila para eliminar.");
            return;
        }

        int id = Integer.parseInt(table.getValueAt(row, 0).toString());
        int confirm = JOptionPane.showConfirmDialog(this, "¿Eliminar materia?", "Confirmar", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM materias WHERE id_materia = ?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
                AdvancedUI.showToast(this, "🗑 Registro eliminado");
                loadData();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error SQL: " + ex.getMessage());
            }
        }
    }

    // ===========================================================
    // NUEVO REGISTRO
    // ===========================================================
    private void showInsertDialog() {
        JTextField nombre = new JTextField();
        JTextField descripcion = new JTextField();

        JPanel form = new JPanel(new GridLayout(0, 2, 10, 10));
        form.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20));
        form.add(new JLabel("Nombre:"));
        form.add(nombre);
        form.add(new JLabel("Descripción:"));
        form.add(descripcion);

        JButton save = new JButton("Guardar"), cancel = new JButton("Cancelar");
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(save);
        buttons.add(cancel);

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "➕ Nueva Materia", true);
        dialog.setLayout(new BorderLayout());
        dialog.add(form, BorderLayout.CENTER);
        dialog.add(buttons, BorderLayout.SOUTH);
        dialog.setSize(400, 200);
        dialog.setLocationRelativeTo(this);

        cancel.addActionListener(e -> dialog.dispose());
        save.addActionListener(e -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO materias (nombre, descripcion) VALUES (?, ?)")) {
                ps.setString(1, nombre.getText());
                ps.setString(2, descripcion.getText());
                ps.executeUpdate();
                AdvancedUI.showToast(this, "✅ Materia insertada");
                loadData();
                dialog.dispose();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Error: " + ex.getMessage());
            }
        });

        dialog.setVisible(true);
    }
}
