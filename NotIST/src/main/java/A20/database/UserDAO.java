package A20.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import A20.classes.User;

public class UserDAO {

    public void addUser(User user) throws SQLException {
        String query = "INSERT INTO Users (username, email, password) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getEmail());
            stmt.setString(3, user.getPassword());
            stmt.executeUpdate();
        }
    }

    public User getUserByUsername(String username) throws SQLException {
        String query = "SELECT * FROM Users WHERE username = ?";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new User(
                    rs.getInt("user_id"),
                    rs.getString("username"),
                    rs.getString("email"),
                    rs.getString("password")
                );
            }
        }
        return null;
    }

    public List<User> getAllUsers() throws SQLException {
        String query = "SELECT * FROM Users";
        List<User> users = new ArrayList<>();
        try (Connection conn = DatabaseConnector.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                users.add(new User(
                    rs.getInt("user_id"),
                    rs.getString("username"),
                    rs.getString("email"),
                    rs.getString("password")
                ));
            }
        }
        return users;
    }
}