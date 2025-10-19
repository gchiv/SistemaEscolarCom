package org.example.ui;

import org.example.ui.components.PanelHeader;
import org.example.ui.util.AdvancedUI;
import com.formdev.flatlaf.FlatClientProperties;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.time.LocalDate;

public class PersonasPanel extends JPanel {
    private final Connection conn;
    private final JTable table = new JTable();
    private final JLabel lblTotal = new JLabel("Total registros: 0");

    public PersonasPanel(Connection conn) {
        this.conn = conn;
        setLayout(new BorderLayout(8, 8));

        // --- Header con botones unificados ---
        PanelHeader header = new PanelHeader("Personas");
        add(header, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(lblTotal, BorderLayout.SOUTH);

        lblTotal.setHorizontalAlignment(SwingConstants.RIGHT);
        lblTotal.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 12));

        table.setAutoCreateRowSorter(true);
        table.setRowHeight(28);
        table.getTableHeader().setReorderingAllowed(false);
        table.putClientProperty("terminateEditOnFocusLost", true);
        AdvancedUI.enableRowHover(table);

        // --- Eventos header ---
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

        // --- Carga inicial ---
        loadData();

        // --- Guardar cambios automáticamente al editar celdas ---
        table.getModel().addTableModelListener(ev -> {
            if (ev.getType() != javax.swing.event.TableModelEvent.UPDATE) return;
            int row = ev.getFirstRow();
            int col = ev.getColumn();
            if (row < 0 || col < 0) return;

            String columnName = table.getColumnName(col);
            if (columnName.equalsIgnoreCase("id_persona")) return; // proteger ID

            Object newValue = table.getValueAt(row, col);
            int id = Integer.parseInt(table.getValueAt(row, 0).toString());

            String sql = "UPDATE personas_escuela SET " + columnName + " = ? WHERE id_persona = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                if (columnName.equalsIgnoreCase("fecha_nacimiento")) {
                    ps.setDate(1, Date.valueOf(String.valueOf(newValue)));
                } else {
                    ps.setObject(1, newValue);
                }
                ps.setInt(2, id);
                ps.executeUpdate();
                AdvancedUI.showToast(PersonasPanel.this, "✅ Valor actualizado");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(PersonasPanel.this,
                        "Error al guardar cambio:\n" + ex.getMessage(),
                        "SQL Error", JOptionPane.ERROR_MESSAGE);
                loadData(); // revertir si falla
            }
        });
    }

    // ==========================================================
    // CARGA DE DATOS
    // ==========================================================
    public void loadData() { loadData("ASC"); }

    public void loadData(String order) {
        String sql = "SELECT * FROM personas_escuela ORDER BY id_persona " + order;
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
        String[] colNames = new String[cols];
        for (int i = 1; i <= cols; i++) colNames[i - 1] = meta.getColumnName(i);

        DefaultTableModel model = new DefaultTableModel(colNames, 0) {
            @Override public boolean isCellEditable(int row, int column) {
                return !getColumnName(column).equalsIgnoreCase("id_persona");
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

    // ==========================================================
    // BÚSQUEDA
    // ==========================================================
    private void loadDataFiltered(String filtro) {
        String sql = """
                SELECT * FROM personas_escuela
                WHERE nombre LIKE ? OR apellido LIKE ? OR rol LIKE ?
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 1; i <= 3; i++) ps.setString(i, "%" + filtro + "%");
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

    // ==========================================================
    // ELIMINAR
    // ==========================================================
    private void deleteSelected() {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Seleccione una fila para eliminar.");
            return;
        }

        int id = Integer.parseInt(table.getValueAt(row, 0).toString());
        int confirm = JOptionPane.showConfirmDialog(this, "¿Eliminar registro?",
                "Confirmar", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM personas_escuela WHERE id_persona = ?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
                AdvancedUI.showToast(this, "🗑 Registro eliminado");
                loadData();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error SQL: " + ex.getMessage());
            }
        }
    }

    // ==========================================================
    // INSERTAR NUEVA PERSONA
    // ==========================================================
    private void showInsertDialog() {
        JTextField nombre = new JTextField();
        JTextField apellido = new JTextField();
        JComboBox<String> sexo = new JComboBox<>(new String[]{"M", "F"});
        JTextField fecha = new JTextField(LocalDate.now().toString());
        JTextField rol = new JTextField();

        nombre.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Ej. Ana");
        apellido.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Ej. López");
        fecha.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "YYYY-MM-DD");
        rol.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Alumno / Docente");

        JPanel form = new JPanel(new GridLayout(0, 2, 10, 10));
        form.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20));
        form.add(new JLabel("Nombre:")); form.add(nombre);
        form.add(new JLabel("Apellido:")); form.add(apellido);
        form.add(new JLabel("Sexo:")); form.add(sexo);
        form.add(new JLabel("Fecha Nac:")); form.add(fecha);
        form.add(new JLabel("Rol:")); form.add(rol);

        JButton save = new JButton("Guardar"), cancel = new JButton("Cancelar");
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(save); buttons.add(cancel);

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "➕ Nueva Persona", true);
        dialog.setLayout(new BorderLayout());
        dialog.add(form, BorderLayout.CENTER);
        dialog.add(buttons, BorderLayout.SOUTH);
        dialog.setSize(420, 270);
        dialog.setLocationRelativeTo(this);

        cancel.addActionListener(e -> dialog.dispose());
        save.addActionListener(e -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO personas_escuela (nombre, apellido, sexo, fecha_nacimiento, rol) VALUES (?, ?, ?, ?, ?)")) {
                ps.setString(1, nombre.getText());
                ps.setString(2, apellido.getText());
                ps.setString(3, sexo.getSelectedItem().toString());
                ps.setDate(4, Date.valueOf(fecha.getText()));
                ps.setString(5, rol.getText());
                ps.executeUpdate();
                AdvancedUI.showToast(this, "✅ Persona insertada");
                loadData();
                dialog.dispose();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Error: " + ex.getMessage());
            }
        });

        dialog.setVisible(true);
    }
}
