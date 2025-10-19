package org.example;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import org.example.ui.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Main {

    private static float zoomFactor = 1.0f;
    private static boolean showGrid = true;
    private static JFrame mainFrame;

    public static void main(String[] args) {
        try { FlatMacDarkLaf.setup(); } catch (Exception ignored) {}

        final String hostname = "fi.jcaguilar.dev";
        final String sshUser = "patito";
        final String sshPass = "cuack";
        final String dbUser = "becario";
        final String dbPass = "FdI-its-5a";

        Session session = null;
        Connection conn = null;

        try {
            // --- 1) Túnel SSH ---
            JSch jsch = new JSch();
            session = jsch.getSession(sshUser, hostname, 22);
            session.setPassword(sshPass);
            java.util.Properties cfg = new java.util.Properties();
            cfg.put("StrictHostKeyChecking", "no");
            session.setConfig(cfg);
            session.connect();

            int localPort = session.setPortForwardingL(0, "localhost", 3306);

            // --- 2) Conexión JDBC ---
            String jdbcUrl = "jdbc:mariadb://localhost:" + localPort + "/its5a";
            conn = DriverManager.getConnection(jdbcUrl, dbUser, dbPass);

            final Session fSession = session;
            final Connection fConn = conn;

            EventQueue.invokeLater(() -> {
                mainFrame = new JFrame("Sistema Escolar Profesional");
                mainFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
                mainFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);

                // === ICONO DE LA APP ===
                try {
                    ImageIcon icon = new ImageIcon(Main.class.getResource("/icons/school.png"));
                    mainFrame.setIconImage(icon.getImage());
                } catch (Exception ex) {
                    System.err.println("⚠ No se encontró school.png");
                }

                // === BARRA SUPERIOR ===
                JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));
                toolbar.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

                // Logo a la izquierda
                JLabel logoLabel = new JLabel();
                try {
                    ImageIcon logo = new ImageIcon(Main.class.getResource("/icons/school.png"));
                    Image scaled = logo.getImage().getScaledInstance(32, 32, Image.SCALE_SMOOTH);
                    logoLabel.setIcon(new ImageIcon(scaled));
                } catch (Exception e) {
                    logoLabel.setText("🏫");
                }

                toolbar.add(logoLabel);
                toolbar.add(Box.createHorizontalStrut(8));

                JButton btnGrid = iconButton("icons/grid.svg", "Mostrar / ocultar cuadrícula");
                JButton btnZoomOut = iconButton("icons/zoom-out.svg", "Reducir zoom");
                JButton btnZoomIn = iconButton("icons/zoom-in.svg", "Aumentar zoom");
                JButton btnReset = iconButton("icons/zoom-reset.svg", "Restablecer zoom");

                toolbar.add(btnGrid);
                toolbar.add(btnZoomOut);
                toolbar.add(btnZoomIn);
                toolbar.add(btnReset);

                // === PANELES ===
                JTabbedPane tabs = new JTabbedPane();
                tabs.addTab("👤 Personas", new PersonasPanel(fConn));
                tabs.addTab("📚 Materias", new MateriasPanel(fConn));
                tabs.addTab("📝 Inscripciones", new InscripcionesPanel(fConn));
                tabs.addTab("✅ Asistencias", new AsistenciasPanel(fConn));

                JPanel mainPanel = new JPanel(new BorderLayout());
                mainPanel.add(toolbar, BorderLayout.NORTH);
                mainPanel.add(tabs, BorderLayout.CENTER);
                mainFrame.setContentPane(mainPanel);

                // === ACCIONES ===
                btnZoomIn.addActionListener(e -> adjustZoom(0.1f));
                btnZoomOut.addActionListener(e -> adjustZoom(-0.1f));
                btnReset.addActionListener(e -> resetZoom());
                btnGrid.addActionListener(e -> toggleGridExternally());

                // === ATAJOS ===
                mainFrame.addKeyListener(new KeyAdapter() {
                    @Override
                    public void keyPressed(KeyEvent e) {
                        if (!e.isControlDown()) return;
                        switch (e.getKeyCode()) {
                            case KeyEvent.VK_EQUALS -> adjustZoom(0.1f);
                            case KeyEvent.VK_MINUS -> adjustZoom(-0.1f);
                            case KeyEvent.VK_0 -> resetZoom();
                            case KeyEvent.VK_G -> toggleGridExternally();
                        }
                    }
                });

                mainFrame.setVisible(true);
                applyZoom(mainFrame);

                // === CIERRE SEGURO ===
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    try { if (!fConn.isClosed()) fConn.close(); } catch (SQLException ignored) {}
                    if (fSession.isConnected()) fSession.disconnect();
                }));
            });

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null,
                    "Error al conectar:\n" + e.getMessage(),
                    "Error de conexión", JOptionPane.ERROR_MESSAGE);
            if (session != null && session.isConnected()) session.disconnect();
            if (conn != null) try { conn.close(); } catch (SQLException ignored) {}
        }
    }

    // ======================================================
    // 🔍 ZOOM
    // ======================================================
    private static void adjustZoom(float delta) {
        zoomFactor = Math.max(0.7f, Math.min(1.8f, zoomFactor + delta));
        applyZoom(mainFrame);
    }

    private static void resetZoom() {
        zoomFactor = 1.0f;
        applyZoom(mainFrame);
    }

    private static void applyZoom(JFrame frame) {
        if (frame == null) return;
        float base = 14f;
        int newSize = Math.round(base * zoomFactor);
        Font font = new Font("Segoe UI", Font.PLAIN, newSize);
        updateFontsRecursively(frame, font);
        SwingUtilities.updateComponentTreeUI(frame);
    }

    private static void updateFontsRecursively(Component comp, Font font) {
        comp.setFont(font);
        if (comp instanceof Container container) {
            for (Component child : container.getComponents())
                updateFontsRecursively(child, font);
        }
    }

    // ======================================================
    // 🔲 CUADRÍCULA
    // ======================================================
    private static void toggleGridExternally() {
        showGrid = !showGrid;
        toggleGrid(mainFrame, showGrid);
    }

    private static void toggleGrid(Container container, boolean visible) {
        for (Component c : container.getComponents()) {
            if (c instanceof JScrollPane scroll &&
                    scroll.getViewport().getView() instanceof JTable table)
                table.setShowGrid(visible);
            if (c instanceof Container sub) toggleGrid(sub, visible);
        }
    }

    // ======================================================
    // 🖱️ BOTONES SVG CON HOVER
    // ======================================================
    private static JButton iconButton(String iconPath, String tooltip) {
        FlatSVGIcon icon = new FlatSVGIcon(iconPath, 18, 18);
        String hoverPath = iconPath.replace("icons/", "icons/hover/");
        FlatSVGIcon iconHover = new FlatSVGIcon(hoverPath, 18, 18);

        JButton b = new JButton(icon);
        b.setToolTipText(tooltip);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        b.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) { b.setIcon(iconHover); }
            @Override public void mouseExited(java.awt.event.MouseEvent e) { b.setIcon(icon); }
        });
        
        return b;
    }
}
