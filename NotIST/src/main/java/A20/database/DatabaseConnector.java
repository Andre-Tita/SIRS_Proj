package A20.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnector {
    private static final String URL = "jdbc:mysql://localhost:3306/NotIST";
    private static final String USER = "root"; // Mysql Username # Not done !
    private static final String PASSWORD = "root"; // Mysql password ## Not done !

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
