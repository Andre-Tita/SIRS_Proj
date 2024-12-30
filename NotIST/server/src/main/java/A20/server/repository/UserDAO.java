package A20.server.repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import A20.server.model.User;

public class UserDAO {

    public void addUser(User user) throws SQLException {
        String query = "INSERT INTO users (username, password_hash, public_key, is_loggedin) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getPassword());
            stmt.setString(3, user.getPublicKey());
            stmt.setBoolean(4, true);
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
                    rs.getString("username")
                );
            }
        }
        return null;
    }

    public User getUserByUserId(int user_id) throws SQLException {
        String query = "SELECT * FROM users WHERE user_id = ?";
        try (Connection conn = DatabaseConnector.getConnection();
        PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, user_id);

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return new User(
                    rs.getInt("user_id"),
                    rs.getString("username")
                );
            }
        }

        return null;
    }

    public List<User> getUsersByUserIds(List<Integer> users_ids) throws SQLException {
        List<User> users = new ArrayList<>();
        for(Integer id : users_ids) {
            users.add(this.getUserByUserId(id));
        }

        return users;
    }

    public void login(int user_id) throws SQLException {
        String query = "UPDATE users SET is_loggedin = TRUE WHERE user_id = ?";
    
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
    
            stmt.setInt(1, user_id);
            stmt.executeUpdate();
        }
    }
    
    public void logout(int user_id) throws SQLException {
        String query = "UPDATE users SET is_loggedin = FALSE WHERE user_id = ?";
    
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
    
            stmt.setInt(1, user_id);
            stmt.executeUpdate();
        }
    }

    public void logoutAllUsers() throws SQLException {
        String query = "UPDATE users SET is_loggedin = FALSE";

        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.executeUpdate();
        }
    }

    
    public boolean isUserLoggedIn(int user_id) throws SQLException {
        String query = "SELECT * FROM users WHERE user_id = ? AND is_loggedin = true";

        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
    
            stmt.setInt(1, user_id);

            ResultSet rs = stmt.executeQuery();

            return rs.next();
        }
    }

}