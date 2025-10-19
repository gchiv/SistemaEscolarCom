package org.example.ui.util;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.Component;
import java.awt.Point;
import java.awt.Color;
import java.awt.Font;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Utilidades gráficas avanzadas para el sistema escolar:
 * - Toasts visuales
 * - Exportar tablas a CSV o Excel
 * - Hover visual en filas de JTable
 */
public class AdvancedUI {

    // ===========================================================
    // ✅ 1. Toast visual tipo notificación flotante
    // ===========================================================
    public static void showToast(Component parent, String message) {
        JWindow toast = new JWindow();
        JLabel lbl = new JLabel(message, SwingConstants.CENTER);
        lbl.setOpaque(true);
        lbl.setBackground(new Color(45, 45, 45, 230));
        lbl.setForeground(Color.WHITE);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lbl.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        toast.add(lbl);
        toast.pack();

        Point p = parent.getLocationOnScreen();
        toast.setLocation(p.x + (parent.getWidth() - toast.getWidth()) / 2, p.y + 60);

        new Thread(() -> {
            toast.setVisible(true);
            try { Thread.sleep(1800); } catch (InterruptedException ignored) {}
            toast.setVisible(false);
            toast.dispose();
        }).start();
    }

    // ===========================================================
    // ✅ 2. Exportar tabla a CSV
    // ===========================================================
    public static void exportTableToCSV(JTable table, String filePath) throws IOException {
        try (FileWriter csv = new FileWriter(filePath)) {
            TableModel model = table.getModel();

            // encabezado
            for (int i = 0; i < model.getColumnCount(); i++) {
                csv.write(model.getColumnName(i));
                if (i < model.getColumnCount() - 1) csv.write(",");
            }
            csv.write("\n");

            // filas
            for (int r = 0; r < model.getRowCount(); r++) {
                for (int c = 0; c < model.getColumnCount(); c++) {
                    Object value = model.getValueAt(r, c);
                    csv.write(value == null ? "" : value.toString());
                    if (c < model.getColumnCount() - 1) csv.write(",");
                }
                csv.write("\n");
            }
        }
    }

    // ===========================================================
    // ✅ 3. Exportar tabla a Excel (.xlsx)
    // ===========================================================
    public static void exportTableToExcel(JTable table, String filePath) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Datos");
            TableModel model = table.getModel();

            // --- Encabezado ---
            Row header = sheet.createRow(0);
            CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            for (int c = 0; c < model.getColumnCount(); c++) {
                Cell cell = header.createCell(c);
                cell.setCellValue(model.getColumnName(c));
                cell.setCellStyle(headerStyle);
            }

            // --- Filas ---
            for (int r = 0; r < model.getRowCount(); r++) {
                Row row = sheet.createRow(r + 1);
                for (int c = 0; c < model.getColumnCount(); c++) {
                    Object val = model.getValueAt(r, c);
                    row.createCell(c).setCellValue(val == null ? "" : val.toString());
                }
            }

            // Autoajustar ancho
            for (int c = 0; c < model.getColumnCount(); c++) {
                sheet.autoSizeColumn(c);
            }

            try (FileOutputStream out = new FileOutputStream(filePath)) {
                workbook.write(out);
            }
        }
    }

    // ===========================================================
    // ✅ 4. ExportChooser → pide al usuario CSV o Excel
    // ===========================================================
    public static void exportChooser(Component parent, JTable table) {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Exportar tabla");
        fc.setSelectedFile(new java.io.File("tabla.xlsx"));
        if (fc.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION) {
            String path = fc.getSelectedFile().getAbsolutePath();
            try {
                if (path.endsWith(".csv")) {
                    exportTableToCSV(table, path);
                } else {
                    if (!path.endsWith(".xlsx")) path += ".xlsx";
                    exportTableToExcel(table, path);
                }
                showToast(parent, "✅ Exportado correctamente");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(parent, "Error al exportar:\n" + ex.getMessage());
            }
        }
    }

    // ===========================================================
    // ✅ 5. Hover visual para filas de JTable
    // ===========================================================
    public static void enableRowHover(JTable table) {
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            private int hoverRow = -1;
            private final Color hoverColor = new Color(60, 80, 110);
            private final Color selectedColor = new Color(90, 130, 200);

            {
                table.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
                    public void mouseMoved(java.awt.event.MouseEvent e) {
                        int row = table.rowAtPoint(e.getPoint());
                        if (row != hoverRow) {
                            hoverRow = row;
                            table.repaint();
                        }
                    }
                });
                table.addMouseListener(new java.awt.event.MouseAdapter() {
                    public void mouseExited(java.awt.event.MouseEvent e) {
                        hoverRow = -1;
                        table.repaint();
                    }
                });
            }

            @Override
            public Component getTableCellRendererComponent(
                    JTable tbl, Object value, boolean isSelected, boolean hasFocus, int row, int col) {

                Component c = super.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, col);
                if (isSelected) {
                    c.setBackground(selectedColor);
                    c.setForeground(Color.WHITE);
                } else if (row == hoverRow) {
                    c.setBackground(hoverColor);
                    c.setForeground(Color.WHITE);
                } else {
                    c.setBackground(tbl.getBackground());
                    c.setForeground(tbl.getForeground());
                }
                return c;
            }
        });
    }
}
