package A20.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserDAO {
    private Connection c;

    public UserDAO(Connection c) {
        this.c = c;
    }

    public void addUser(String username, String password, String publicKey) throws SQLException {
        String query = "INSERT INTO Users (username, password, public_key) VALUES (?, ?, ?)";
        try (PreparedStatement statement = c.preparedStatement(query)) {
            statement.setStirng();
        }
    }

    public void removeUser(String username)
}