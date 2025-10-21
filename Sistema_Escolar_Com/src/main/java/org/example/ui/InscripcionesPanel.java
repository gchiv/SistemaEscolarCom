package org.example.ui;

import org.example.ui.components.PanelHeader;
import org.example.ui.util.AdvancedUI;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class InscripcionesPanel extends JPanel {
    private final Connection conn;
    private final JTable table = new JTable();
    private final JLabel lblTotal = new JLabel("Total registros: 0");

    public InscripcionesPanel(Connection conn) {
        this.conn = conn;
        setLayout(new BorderLayout(8, 8));

        // Header con título y botones (reutiliza tu PanelHeader)
        PanelHeader header = new PanelHeader("Inscripciones");
        add(header, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(lblTotal, BorderLayout.SOUTH);

        lblTotal.setHorizontalAlignment(SwingConstants.RIGHT);
        lblTotal.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 12));

        // Tabla
        table.setAutoCreateRowSorter(true);
        table.setRowHeight(28);
        table.getTableHeader().setReorderingAllowed(false);
        table.putClientProperty("terminateEditOnFocusLost", true);
        AdvancedUI.enableRowHover(table);

        // Botones del header
        header.btnReload.addActionListener(e -> loadData());
        header.btnInsert.addActionListener(e -> showInsertDialog());
        header.btnDelete.addActionListener(e -> deleteSelected());
        header.btnExport.addActionListener(e -> AdvancedUI.exportChooser(this, table));
        header.btnOrderAsc.addActionListener(e -> loadData("ASC"));
        header.btnOrderDesc.addActionListener(e -> loadData("DESC"));

        // Búsqueda en vivo
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

        // Carga inicial
        loadData();

        // Guardar cambios automáticamente al editar celdas
        table.getModel().addTableModelListener(ev -> {
            if (ev.getType() != javax.swing.event.TableModelEvent.UPDATE) return;
            int row = ev.getFirstRow();
            int col = ev.getColumn();
            if (row < 0 || col < 0) return;

            String columnName = table.getColumnName(col);
            if (columnName.equalsIgnoreCase("id_inscripcion")) return; // proteger PK

            Object newValue = table.getValueAt(row, col);
            int id = Integer.parseInt(table.getValueAt(row, 0).toString());

            String sql = "UPDATE inscripciones SET " + columnName + " = ? WHERE id_inscripcion = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                // Tipos: id_persona e id_materia son INT
                if (columnName.equalsIgnoreCase("id_persona") || columnName.equalsIgnoreCase("id_materia")) {
                    ps.setInt(1, Integer.parseInt(String.valueOf(newValue)));
                } else {
                    ps.setObject(1, newValue);
                }
                ps.setInt(2, id);
                ps.executeUpdate();
                AdvancedUI.showToast(InscripcionesPanel.this, "Valor actualizado");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(InscripcionesPanel.this,
                        "Error al guardar cambio:\n" + ex.getMessage(),
                        "SQL Error", JOptionPane.ERROR_MESSAGE);
                loadData(); // revertir si falla
            }
        });
    }

    // ============================
    // CARGA / MODELO EDITABLE
    // ============================
    public void loadData() { loadData("ASC"); }

    public void loadData(String order) {
        String sql = "SELECT * FROM inscripciones ORDER BY id_inscripcion " + order;
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            table.setModel(buildEditableModel(rs));
            autosizeColumns();
            lblTotal.setText("Total registros: " + table.getRowCount());
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error al cargar:\n" + ex.getMessage(),
                    "SQL Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private DefaultTableModel buildEditableModel(ResultSet rs) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        int cols = meta.getColumnCount();
        String[] names = new String[cols];
        for (int i = 1; i <= cols; i++) names[i - 1] = meta.getColumnName(i);

        DefaultTableModel model = new DefaultTableModel(names, 0) {
            @Override public boolean isCellEditable(int row, int column) {
                return !getColumnName(column).equalsIgnoreCase("id_inscripcion");
            }
        };

        while (rs.next()) {
            Object[] rowData = new Object[cols];
            for (int i = 1; i <= cols; i++) rowData[i - 1] = rs.getObject(i);
            model.addRow(rowData);
        }
        return model;
    }

    private void autosizeColumns() {
        for (int c = 0; c < table.getColumnCount(); c++) {
            table.getColumnModel().getColumn(c).setPreferredWidth(140);
        }
    }

    // ============================
    // BÚSQUEDA
    // ============================
    private void loadDataFiltered(String filtro) {
        String sql = """
                SELECT * FROM inscripciones
                WHERE CAST(id_estudiante AS CHAR) LIKE ? OR CAST(id_materia AS CHAR) LIKE ?
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + filtro + "%");
            ps.setString(2, "%" + filtro + "%");
            try (ResultSet rs = ps.executeQuery()) {
                table.setModel(buildEditableModel(rs));
                autosizeColumns();
                lblTotal.setText("Total registros: " + table.getRowCount());
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error al buscar:\n" + ex.getMessage(),
                    "SQL Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ============================
    // ELIMINAR
    // ============================
    private void deleteSelected() {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Seleccione una fila para eliminar.");
            return;
        }
        int id = Integer.parseInt(table.getValueAt(row, 0).toString());
        int confirm = JOptionPane.showConfirmDialog(this, "¿Eliminar inscripción?",
                "Confirmar", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM inscripciones WHERE id_inscripcion = ?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
                AdvancedUI.showToast(this, "Registro eliminado");
                loadData();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error SQL: " + ex.getMessage());
            }
        }
    }

    // ============================
    // INSERTAR NUEVO
    // ============================
    private void showInsertDialog() {
        JTextField idPersona = new JTextField();
        JTextField idMateria = new JTextField();

        JPanel form = new JPanel(new GridLayout(0, 2, 10, 10));
        form.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20));
        form.add(new JLabel("ID Persona:")); form.add(idPersona);
        form.add(new JLabel("ID Materia:")); form.add(idMateria);

        JButton save = new JButton("Guardar"), cancel = new JButton("Cancelar");
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(save); buttons.add(cancel);

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Nueva Inscripción", true);
        dialog.setLayout(new BorderLayout());
        dialog.add(form, BorderLayout.CENTER);
        dialog.add(buttons, BorderLayout.SOUTH);
        dialog.setSize(400, 200);
        dialog.setLocationRelativeTo(this);

        cancel.addActionListener(e -> dialog.dispose());
        save.addActionListener(e -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO inscripciones (id_estudiante, id_materia) VALUES (?, ?)")) {
                ps.setInt(1, Integer.parseInt(idPersona.getText()));
                ps.setInt(2, Integer.parseInt(idMateria.getText()));
                ps.executeUpdate();
                AdvancedUI.showToast(this, "Inscripción agregada");
                loadData();
                dialog.dispose();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Error: " + ex.getMessage());
            }
        });

        dialog.setVisible(true);
    }
}
