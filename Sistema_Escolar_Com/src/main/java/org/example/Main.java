package org.example;

import java.sql.*;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

public class Main {
    public static void main(String []args) throws JSchException {
        String hostname = "fi.jcaguilar.dev";
        String sshUser = "patito";
        String sshPass = "cuack";

        String dbUser = "becario";
        String dbPass = "FdI-its-5a";

        JSch jsch = new JSch();
        // ssh patito@fi.jcaguilar.dev => Equivalencia en la terminal
        Session session = jsch.getSession(sshUser, hostname);
        session.setPassword(sshPass); // introducir la contraseña

        // Deshabilita los mensajes de error
        session.setConfig("StrictHostKeyChecking", "no");

        // Obtenemos un puerto redireccional
        session.connect();

        int port = session.setPortForwardingL(0, "localhost", 3306);
        String conString = "jdbc:mariadb://localhost:"+ port +"/its5a";
        System.out.println(conString);

        try (Connection conexion = DriverManager.getConnection(conString, dbUser, dbPass);
             Statement stmt = conexion.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM personas_escuela")) {

            System.out.println("Conectado correctamente a la base de datos");
            while (rs.next()) {
                String nombre = rs.getString(1);
                String apellido = rs.getString(2);
                System.out.println(nombre + " " + apellido);
            }
        } catch (SQLException e) {
            System.out.println("Error SQL: " + e.getMessage());
        }

        //Exit
        session.disconnect();
    }
}
