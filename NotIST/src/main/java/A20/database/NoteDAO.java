import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class NoteDAO {
    public void addNote(Note note) throws SQLException {
        String query = "INSERT INTO Notes (title, content, owner_id) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, note.getTitle());
            stmt.setString(2, note.getContent());
            stmt.setInt(3, note.getOwnerId());
            stmt.executeUpdate();
        }
    }

    public List<Note> getNotesByUserId(int userId) throws SQLException {
        String query = "SELECT * FROM Notes WHERE owner_id = ?";
        List<Note> notes = new ArrayList<>();
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                notes.add(new Note(
                    rs.getInt("note_id"),
                    rs.getInt("owner_id"),
                    rs.getString("title"),
                    rs.getString("content")
                ));
            }
        }
        return notes;
    }
}
