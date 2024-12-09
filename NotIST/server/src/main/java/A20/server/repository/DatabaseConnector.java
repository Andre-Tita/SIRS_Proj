package A20.server.repository;

import java.sql.*;

public class DatabaseConnector {
    private static final String URL = "jdbc:postgresql://localhost:5432/notist";  // Postgres URL format
    private static final String USER = "postgres";  // Username for PostgreSQL
    private static final String PASSWORD = "postgres";  // Password

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
