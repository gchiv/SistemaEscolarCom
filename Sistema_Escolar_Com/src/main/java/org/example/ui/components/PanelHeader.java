package org.example.ui.components;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import javax.swing.*;
import java.awt.*;

/**
 * Encabezado reutilizable para cada panel principal del sistema escolar.
 * Incluye título, búsqueda, orden, recarga, inserción, eliminación,
 * exportación, zoom y alternar cuadrícula.
 */
public class PanelHeader extends JPanel {

    public final JLabel titleLabel;
    public final JTextField searchField;

    public final JButton btnSearch;
    public final JButton btnOrderAsc;
    public final JButton btnOrderDesc;
    public final JButton btnInsert;
    public final JButton btnDelete;
    public final JButton btnExport;
    public final JButton btnReload;


    public PanelHeader(String title) {
        setLayout(new BorderLayout(12, 0));
        setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        setBackground(new Color(30, 30, 30));

        // título
        titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLabel.setForeground(Color.WHITE);

        // campo búsqueda
        searchField = new JTextField();
        searchField.setPreferredSize(new Dimension(220, 32));
        searchField.putClientProperty("JTextField.placeholderText", "Buscar...");

        btnSearch = makeIconButton("search.svg", "Buscar");
        btnOrderAsc = makeIconButton("sort-asc.svg", "Orden ascendente");
        btnOrderDesc = makeIconButton("sort-desc.svg", "Orden descendente");
        btnInsert = makeIconButton("plus.svg", "Insertar nuevo registro");
        btnDelete = makeIconButton("trash.svg", "Eliminar seleccionado");
        btnExport = makeIconButton("book.svg", "Exportar a Excel o CSV");
        btnReload = makeIconButton("reload.svg", "Recargar tabla");

        // --- Panel de búsqueda ---
        JPanel searchPanel = new JPanel(new BorderLayout(5, 0));
        searchPanel.setOpaque(false);
        searchPanel.add(searchField, BorderLayout.CENTER);
        searchPanel.add(btnSearch, BorderLayout.EAST);

        // --- Panel derecho con botones ---
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        buttonPanel.setOpaque(false);

        // agrupar botones para mantener orden
        buttonPanel.add(searchPanel);
        buttonPanel.add(btnOrderAsc);
        buttonPanel.add(btnOrderDesc);
        buttonPanel.add(btnInsert);
        buttonPanel.add(btnDelete);
        buttonPanel.add(btnExport);
        buttonPanel.add(btnReload);
        buttonPanel.add(new JSeparator(SwingConstants.VERTICAL));


        add(titleLabel, BorderLayout.WEST);
        add(buttonPanel, BorderLayout.EAST);
    }

    // --------------------------------------------------------
    // MÉTODO AUXILIAR PARA CREAR BOTONES CON ICONO SVG
    // --------------------------------------------------------
    private JButton makeIconButton(String iconFile, String tooltip) {
        JButton btn = new JButton(new FlatSVGIcon("icons/" + iconFile, 0.9f));
        btn.setToolTipText(tooltip);
        btn.setFocusPainted(false);
        btn.setBackground(new Color(45, 45, 45));
        btn.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        btn.setPreferredSize(new Dimension(34, 34));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                btn.setBackground(new Color(70, 70, 70));
            }
            @Override public void mouseExited(java.awt.event.MouseEvent e) {
                btn.setBackground(new Color(45, 45, 45));
            }
        });
        return btn;
    }
}
