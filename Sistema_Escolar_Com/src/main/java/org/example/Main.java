package org.example;

import java.awt.EventQueue;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import javax.swing.*;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import org.example.ui.PersonasPanel;
import org.example.ui.MateriasPanel;
import org.example.ui.InscripcionesPanel;
import org.example.ui.AsistenciasPanel;

public class Main {

    public static void main(String[] args) {
        // Credenciales/host: (los que tú nos diste)
        final String hostname = "fi.jcaguilar.dev";
        final String sshUser  = "patito";
        final String sshPass  = "cuack";

        final String dbUser = "becario";
        final String dbPass = "FdI-its-5a";

        Session session = null;
        Connection conn = null;

        try {
            // 1) Túnel SSH
            JSch jsch = new JSch();
            session = jsch.getSession(sshUser, hostname);
            session.setPassword(sshPass);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();

            // Puerto local aleatorio -> 3306 del host remoto
            int localPort = session.setPortForwardingL(0, "localhost", 3306);

            // 2) Conexión JDBC
            final String jdbcUrl = "jdbc:mariadb://localhost:" + localPort + "/its5a";
            System.out.println("Conectando a: " + jdbcUrl);
            conn = DriverManager.getConnection(jdbcUrl, dbUser, dbPass);
            System.out.println("Conectado correctamente a la base de datos");

            // 3) UI (Swing) en EDT
            final Connection fConn = conn;        // pasar referencias efectivas a la EDT
            final Session fSession = session;

            EventQueue.invokeLater(() -> {
                JFrame frame = new JFrame("Escuela - Paneles");
                frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
                frame.setSize(1100, 700);
                frame.setLocationRelativeTo(null);

                JTabbedPane tabs = new JTabbedPane();

                tabs.addTab("Personas",      new PersonasPanel(fConn));
                tabs.addTab("Materias",      new MateriasPanel(fConn));
                tabs.addTab("Inscripciones", new InscripcionesPanel(fConn));
                tabs.addTab("Asistencias",   new AsistenciasPanel(fConn));

                frame.setContentPane(tabs);
                frame.setVisible(true);

                // Agrega un hook para cerrar bien recursos al salir
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    try {
                        if (!fConn.isClosed()) fConn.close();
                    } catch (SQLException ignored) {}
                    if (fSession.isConnected()) fSession.disconnect();
                }));
            });

        } catch (JSchException | SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null,
                    "Error al conectar:\n" + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            // Cerrar si falló
            if (session != null && session.isConnected()) session.disconnect();
            if (conn != null) try { conn.close(); } catch (SQLException ignored) {}
        }
    }
}
