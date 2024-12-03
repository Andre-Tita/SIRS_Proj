package A20.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnector {
    private static final String URL = "jdbc:postgresql://localhost:5432/notist";  // Postgres URL format
    private static final String USER = "postgres";  // Username for PostgreSQL
    private static final String PASSWORD = "";  // Password

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
