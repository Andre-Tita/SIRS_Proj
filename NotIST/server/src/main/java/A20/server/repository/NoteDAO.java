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

    public List<Note> getNotesByUserId(int user_id) throws SQLException {
        String query = "SELECT * FROM notes WHERE owner_id = ?";
        List<Note> notes = new ArrayList<>();
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, user_id);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                notes.add(new Note(
                    rs.getInt("note_id"),
                    rs.getInt("owner_id"),
                    rs.getInt("version"),
                    rs.getString("title"),
                    rs.getString("content"),
                    rs.getString("created_at")
                ));
            }
        }
        return notes;
    }

    public List<String> getNotesTtileByUserId(int user_id) throws SQLException {
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

    public Note getNoteByTitle (String title) throws SQLException{
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

    public boolean canAccessNote(int user_id, int note_id) throws SQLException{
        

        return false;
    }
}
