package A20.server.repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import A20.server.model.Note;

public class NoteDAO {
    public void addNote(Note note) throws SQLException {
        String query = "INSERT INTO notes (title, content, owner_id) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, note.getTitle());
            stmt.setString(2, note.getContent());
            stmt.setInt(3, note.getOwnerId());
            stmt.executeUpdate();
        }
    }

    public List<String> getUsersAccessNotes(int user_id) throws SQLException {
        String query = "SELECT n.note_id, n.title, n.content, n.version, n.is_encrypted, n.created_at, " +
               "CASE WHEN n.owner_id = ? THEN 'Owner' ELSE al.user_role END AS access_role " +
               "FROM notes n " +
               "LEFT JOIN access_logs al ON n.note_id = al.note_id " +
               "WHERE n.owner_id = ? OR al.user_id = ?";

        List<String> notesTitles = new ArrayList<>();
        try (Connection conn = DatabaseConnector.getConnection();
            PreparedStatement stmt = conn.prepareStatement(query)){
            
            stmt.setInt(1, user_id);
            stmt.setInt(2, user_id);
            stmt.setInt(3, user_id);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                notesTitles.add(rs.getString("title"));
            }
        }

        return notesTitles;
    }

    public List<String> getNotesTitleByUserId(int user_id) throws SQLException {
        String query = "SELECT * FROM notes WHERE owner_id = ?";
        List<String> titles = new ArrayList<>();
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, user_id);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                titles.add(rs.getString("title"));
            }
        }
        return titles;
    }

    public Note getNoteByTitle (String title) throws SQLException {
        String query = "SELECT * FROM notes WHERE title = ?";
        try (Connection conn = DatabaseConnector.getConnection();
            PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, title);
            
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return new Note (rs.getInt("note_id"),
                    rs.getInt("owner_id"),
                    rs.getInt("version"),
                    rs.getString("title"),
                    rs.getString("content"),
                    rs.getTimestamp("created_at").toString()
                );
            }
        }
        return null;
    }

    public Boolean isOwner (int user_id, String title) throws SQLException {
        String query = "SELECT * FROM notes WHERE owner_id = ? AND title = ?";
        try (Connection conn = DatabaseConnector.getConnection();
            PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, user_id);
            stmt.setString(2, title);

            ResultSet rs = stmt.executeQuery();

            return rs.next();
        } 
    }

    public Boolean canAccessNote (int user_id, int note_id) throws SQLException {
        String query = "SELECT * FROM notes n " +
                   "LEFT JOIN access_logs al ON n.note_id = al.note_id " +
                   "WHERE (n.owner_id = ? OR al.user_id = ?) AND n.note_id = ?";
        try (Connection conn = DatabaseConnector.getConnection();
            PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, user_id);
            stmt.setInt(2, user_id); 
            stmt.setInt(3, note_id); 

            ResultSet rs = stmt.executeQuery();

            return rs.next();
        }
    } 

    public void grantAccessNote(int user_id, int note_id, int owner_id) throws SQLException{
        String query = "INSERT INTO access_logs (user_id, note_id, owner_id, user_role) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnector.getConnection();
            PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, user_id);
            stmt.setInt(2, note_id);
            stmt.setInt(3, owner_id);
            stmt.setString(4, "Viewer");
            stmt.executeUpdate();
        }
    }
}
