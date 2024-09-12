package org.example;

import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.webapp.WebAppContext;
import org.example.servlets.FhirServlet;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args ) throws Exception {
        // Create a server that listens on port 8080.
        Server server = new Server(8080);

        // Hardcoded connection details
        String jdbcUrl = "jdbc:postgresql://localhost:5432/postgres";
        String dbUser = "postgres";
        String dbPassword = "postgres";

        try (Connection conn = DriverManager.getConnection(jdbcUrl, dbUser, dbPassword)) {
            if (conn != null) {
                System.out.println("Connected to the database!");
                FhirServlet fhirServlet = new FhirServlet(conn);
                ServletContextHandler handler = new ServletContextHandler();
                handler.addServlet(new ServletHolder(fhirServlet), "/*");
                server.setHandler(handler);
                // Start the server! 🚀
                server.start();
                System.out.println("Server started!");

                // Keep the main thread alive while the server is running.
                server.join();
            } else {
                System.out.println("Failed to connect to the database.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }
}
