package org.example.ui.util;

import javax.swing.*;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;
import java.util.Map;

public class UIUtils {

    private static float zoomFactor = 1.0f;

    // Aplica zoom global a todo el JFrame
    public static void enableGlobalZoom(JFrame frame) {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(e -> {
            if ((e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) != 0) {
                if (e.getID() == KeyEvent.KEY_PRESSED) {
                    if (e.getKeyCode() == KeyEvent.VK_EQUALS || e.getKeyCode() == KeyEvent.VK_ADD) {
                        adjustZoom(frame, 0.1f);
                        return true;
                    } else if (e.getKeyCode() == KeyEvent.VK_MINUS || e.getKeyCode() == KeyEvent.VK_SUBTRACT) {
                        adjustZoom(frame, -0.1f);
                        return true;
                    } else if (e.getKeyCode() == KeyEvent.VK_0) {
                        resetZoom(frame);
                        return true;
                    }
                }
            }
            return false;
        });
    }

    private static void adjustZoom(JFrame frame, float delta) {
        zoomFactor = Math.max(0.5f, Math.min(2.0f, zoomFactor + delta));
        scaleFonts(frame);
    }

    private static void resetZoom(JFrame frame) {
        zoomFactor = 1.0f;
        scaleFonts(frame);
    }

    private static void scaleFonts(Component component) {
        if (component == null) return;

        Font f = component.getFont();
        if (f != null) {
            float newSize = 14f * zoomFactor;
            component.setFont(f.deriveFont(newSize));
        }

        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) {
                scaleFonts(child);
            }
        }
        component.revalidate();
        component.repaint();
    }

    // -------------------------------------------------------
    // MENÚ PARA MOSTRAR/OCULTAR COLUMNAS DE UN JTABLE
    // -------------------------------------------------------
    public static void enableColumnToggle(JTable table) {
        Map<String, TableColumn> hiddenColumns = new HashMap<>();

        table.getTableHeader().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    JPopupMenu menu = new JPopupMenu();
                    var model = table.getColumnModel();

                    // Mostrar columnas visibles
                    for (int i = 0; i < model.getColumnCount(); i++) {
                        TableColumn col = model.getColumn(i);
                        String name = col.getHeaderValue().toString();
                        JCheckBoxMenuItem item = new JCheckBoxMenuItem(name, true);
                        item.addActionListener(ev -> {
                            hiddenColumns.put(name, col);
                            model.removeColumn(col);
                        });
                        menu.add(item);
                    }

                    // Mostrar columnas ocultas
                    if (!hiddenColumns.isEmpty()) {
                        menu.addSeparator();
                        JMenu submenu = new JMenu("Mostrar columnas ocultas");
                        for (String colName : hiddenColumns.keySet()) {
                            JMenuItem item = new JMenuItem(colName);
                            item.addActionListener(ev -> {
                                TableColumn col = hiddenColumns.remove(colName);
                                model.addColumn(col);
                            });
                            submenu.add(item);
                        }
                        menu.add(submenu);
                    }

                    menu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
    }
}
