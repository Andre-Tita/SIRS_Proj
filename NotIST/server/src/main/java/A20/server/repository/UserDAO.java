package A20.server.repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import A20.server.model.User;

public class UserDAO {

    public void addUser(User user) throws SQLException {
        String query = "INSERT INTO users (username, password_hash, public_key) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getPassword());
            stmt.setString(3, user.getPublicKey());
            stmt.executeUpdate();
        }
    }

    public User getUserByUsernameAndPass(String username, String password_hash) throws SQLException {
        // #Use hashed password comparison here.
        String query = "SELECT * FROM users WHERE username = ? AND password_hash = ?";
        
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, username);
            stmt.setString(2, password_hash);
            
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return new User(
                    rs.getInt("user_id"),
                    rs.getString("username"),
                    rs.getString("password_hash"),
                    rs.getString("public_key")
                );
            }
        }
        return null;
    }

    public User getUserByUsername(String username) throws SQLException {
        // #Use hashed password comparison here.
        String query = "SELECT * FROM users WHERE username = ?";
        
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, username);
            
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return new User(
                    rs.getInt("user_id"),
                    rs.getString("username"),
                    "",
                    ""
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
                    rs.getString("password_hash"),          // # maybe not return this
                    rs.getString("public_key")              // # maybe not return this
                ));
            }
        }
        return users;
    }
}