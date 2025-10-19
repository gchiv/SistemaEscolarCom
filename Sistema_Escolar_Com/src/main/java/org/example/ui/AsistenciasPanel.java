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

public class AsistenciasPanel extends JPanel {

    private final Connection conn;
    private final JTable table = new JTable();
    private final JLabel lblTotal = new JLabel("Total registros: 0");

    public AsistenciasPanel(Connection conn) {
        this.conn = conn;
        setLayout(new BorderLayout(8, 8));

        // Header con título (usa tus botones del PanelHeader)
        PanelHeader header = new PanelHeader("Asistencias");
        add(header, BorderLayout.NORTH);

        // Centro: tabla con scroll
        add(new JScrollPane(table), BorderLayout.CENTER);

        // Pie: total de registros
        lblTotal.setHorizontalAlignment(SwingConstants.RIGHT);
        lblTotal.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 12));
        add(lblTotal, BorderLayout.SOUTH);

        // Tabla
        table.setAutoCreateRowSorter(true);
        table.setRowHeight(28);
        table.getTableHeader().setReorderingAllowed(false);
        table.putClientProperty("terminateEditOnFocusLost", true);
        AdvancedUI.enableRowHover(table);

        // Eventos header
        header.btnReload.addActionListener(e -> loadData());
        header.btnInsert.addActionListener(e -> showInsertDialog());
        header.btnOrderAsc.addActionListener(e -> loadData("ASC"));
        header.btnOrderDesc.addActionListener(e -> loadData("DESC"));

        header.btnDelete.addActionListener(e -> deleteSelected());
        header.btnExport.addActionListener(e -> exportChooser());

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
        header.btnSearch.addActionListener(e -> {
            String q = header.searchField.getText().trim();
            if (q.isEmpty()) loadData();
            else loadDataFiltered(q);
        });

        // Carga inicial
        loadData();

        // Listener para guardar automáticamente cuando cambie una celda
        table.getModel().addTableModelListener(ev -> {
            if (ev.getType() != javax.swing.event.TableModelEvent.UPDATE) return;
            int row = ev.getFirstRow();
            int col = ev.getColumn();
            if (row < 0 || col < 0) return;

            String columnName = table.getColumnName(col);
            if (columnName.equalsIgnoreCase("id_asistencia")) return; // no editar ID

            Object newValue = table.getValueAt(row, col);
            int id = Integer.parseInt(table.getValueAt(row, 0).toString());

            // UPDATE con manejo de tipos para fecha / int
            String sql = "UPDATE asistencias SET " + columnName + " = ? WHERE id_asistencia = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                if (columnName.equalsIgnoreCase("fecha")) {
                    ps.setDate(1, Date.valueOf(String.valueOf(newValue)));
                } else if (columnName.equalsIgnoreCase("id_inscripcion")) {
                    ps.setInt(1, Integer.parseInt(String.valueOf(newValue)));
                } else {
                    ps.setObject(1, newValue);
                }
                ps.setInt(2, id);
                ps.executeUpdate();
                AdvancedUI.showToast(AsistenciasPanel.this, "✅ Valor actualizado");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(AsistenciasPanel.this,
                        "Error al guardar cambio:\n" + ex.getMessage(),
                        "SQL Error", JOptionPane.ERROR_MESSAGE);
                // Revertir vista si falla
                loadData();
            }
        });
    }

    // ===============================
    // CARGA / MODELO EDITABLE
    // ===============================
    public void loadData() { loadData("ASC"); }

    public void loadData(String order) {
        String sql = "SELECT * FROM asistencias ORDER BY fecha " + order;
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            table.setModel(buildEditableModel(rs));
            autosizeColumns();
            lblTotal.setText("Total registros: " + table.getRowCount());

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                    "Error al cargar asistencias:\n" + ex.getMessage(),
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
                // Bloquear la primera columna si es el ID
                return !getColumnName(column).equalsIgnoreCase("id_asistencia");
            }
        };

        while (rs.next()) {
            Object[] row = new Object[cols];
            for (int i = 1; i <= cols; i++) row[i - 1] = rs.getObject(i);
            model.addRow(row);
        }
        return model;
    }

    private void autosizeColumns() {
        for (int c = 0; c < table.getColumnCount(); c++) {
            var col = table.getColumnModel().getColumn(c);
            col.setPreferredWidth(140);
        }
    }

    // ===============================
    // BÚSQUEDA
    // ===============================
    private void loadDataFiltered(String filtro) {
        String sql = """
                SELECT * FROM asistencias
                 WHERE CAST(id_inscripcion AS CHAR) LIKE ?
                    OR CAST(fecha AS CHAR) LIKE ?
                 ORDER BY fecha DESC
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
            JOptionPane.showMessageDialog(this,
                    "Error al buscar:\n" + ex.getMessage(),
                    "SQL Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ===============================
    // ELIMINAR
    // ===============================
    private void deleteSelected() {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Seleccione un registro para eliminar.");
            return;
        }
        int id = Integer.parseInt(table.getValueAt(row, 0).toString());
        int confirm = JOptionPane.showConfirmDialog(this, "¿Eliminar registro seleccionado?",
                "Confirmar", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM asistencias WHERE id_asistencia = ?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
                loadData();
                AdvancedUI.showToast(this, "🗑 Registro eliminado");
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error SQL: " + ex.getMessage());
            }
        }
    }

    // ===============================
    // EXPORTAR
    // ===============================
    private void exportChooser() {
        JFileChooser fc = new JFileChooser();
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                String path = fc.getSelectedFile().getAbsolutePath();
                if (path.toLowerCase().endsWith(".csv")) {
                    AdvancedUI.exportTableToCSV(table, path);
                } else {
                    if (!path.toLowerCase().endsWith(".xlsx")) path += ".xlsx";
                    AdvancedUI.exportTableToExcel(table, path);
                }
                AdvancedUI.showToast(this, "Exportado con éxito");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error exportando: " + ex.getMessage());
            }
        }
    }

    // ===============================
    // INSERTAR
    // ===============================
    private void showInsertDialog() {
        JTextField idInscripcionField = new JTextField(10);
        JTextField fechaField = new JTextField(LocalDate.now().toString());

        idInscripcionField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Ej. 101");
        fechaField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "YYYY-MM-DD");
        idInscripcionField.putClientProperty(FlatClientProperties.STYLE, "arc:15;");
        fechaField.putClientProperty(FlatClientProperties.STYLE, "arc:15;");

        JPanel formPanel = new JPanel(new GridLayout(0, 2, 10, 10));
        formPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20));
        formPanel.add(new JLabel("ID Inscripción:"));
        formPanel.add(idInscripcionField);
        formPanel.add(new JLabel("Fecha (YYYY-MM-DD):"));
        formPanel.add(fechaField);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        JButton btnSave = new JButton("Guardar");
        JButton btnCancel = new JButton("Cancelar");
        buttons.add(btnSave);
        buttons.add(btnCancel);

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "➕ Nueva Asistencia", true);
        dialog.setLayout(new BorderLayout());
        dialog.add(formPanel, BorderLayout.CENTER);
        dialog.add(buttons, BorderLayout.SOUTH);
        dialog.setSize(420, 230);
        dialog.setLocationRelativeTo(this);

        btnCancel.addActionListener(e -> dialog.dispose());
        btnSave.addActionListener(e -> {
            try {
                int idInscripcion = Integer.parseInt(idInscripcionField.getText());
                String fecha = fechaField.getText();

                if (idInscripcion <= 0 || fecha.isEmpty()) {
                    JOptionPane.showMessageDialog(dialog, "Datos incompletos o inválidos.");
                    return;
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO asistencias (id_inscripcion, fecha) VALUES (?, ?)")) {
                    ps.setInt(1, idInscripcion);
                    ps.setDate(2, Date.valueOf(fecha));
                    ps.executeUpdate();
                    AdvancedUI.showToast(this, "✅ Registro insertado");
                    loadData();
                    dialog.dispose();
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Error: " + ex.getMessage());
            }
        });

        dialog.setVisible(true);
    }
}
