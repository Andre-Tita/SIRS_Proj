package A20.server.repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import A20.server.model.Note;

public class NoteDAO {

    // Create a note
    public void addNote(Note note) throws SQLException {
        String noteQuery = "INSERT INTO notes (title, owner_id) VALUES (?, ?) ON CONFLICT (title) DO NOTHING RETURNING note_id";
        String versionQuery = "INSERT INTO note_versions (note_id, version, content) VALUES (?, ?, ?)";

        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement noteStmt = conn.prepareStatement(noteQuery, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement versionStmt = conn.prepareStatement(versionQuery)) {

            // Insert or find the note metadata
            noteStmt.setString(1, note.getTitle());
            noteStmt.setInt(2, note.getOwnerId());
            noteStmt.executeUpdate();

            int noteId;
            try (ResultSet rs = noteStmt.getGeneratedKeys()) {
                if (rs.next()) {
                    noteId = rs.getInt(1); // New note
                } else {
                    // Fetch existing note_id
                    String fetchNoteId = "SELECT note_id FROM notes WHERE title = ?";
                    try (PreparedStatement fetchStmt = conn.prepareStatement(fetchNoteId)) {
                        fetchStmt.setString(1, note.getTitle());
                        try (ResultSet fetchRs = fetchStmt.executeQuery()) {
                            if (fetchRs.next()) {
                                noteId = fetchRs.getInt("note_id");
                            } else {
                                throw new SQLException("Failed to retrieve note_id.");
                            }
                        }
                    }
                }
            }

            // Fetch latest version number for the note
            String fetchVersionNumber = "SELECT MAX(version) AS max_version FROM note_versions WHERE note_id = ?";
            int latestVersion = 0;
            try (PreparedStatement versionStmtFetch = conn.prepareStatement(fetchVersionNumber)) {
                versionStmtFetch.setInt(1, noteId);
                try (ResultSet rs = versionStmtFetch.executeQuery()) {
                    if (rs.next()) {
                        latestVersion = rs.getInt("max_version");
                    }
                }
            }

            // Insert the new version
            versionStmt.setInt(1, noteId);
            versionStmt.setInt(2, latestVersion + 1);
            versionStmt.setString(3, note.getContent());
            versionStmt.executeUpdate();
        }
    }

    // Notes a certain user has access to
    public List<String> getUsersAccessNotes(int user_id) throws SQLException {
        String query = "SELECT n.title, al.user_role, n.owner_id " +
                       "FROM notes n " +
                       "LEFT JOIN access_logs al ON n.note_id = al.note_id " +
                       "WHERE (n.owner_id = ? OR al.user_id = ?)";

        List<String> notesTitles = new ArrayList<>();
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, user_id);
            stmt.setInt(2, user_id);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                if (rs.getInt("owner_id") == user_id) {
                    String note = rs.getString("title") + " | w Role: OWNER";
                    notesTitles.add(note);
                    continue;
                }

                String note = rs.getString("title") + " | w Role: " + rs.getString("user_role");
                notesTitles.add(note);
            }
        }

        return notesTitles;
    }

    // Notes in which a user is owner
    public List<String> getNotesTitleByUserId(int user_id) throws SQLException {
        String query = "SELECT DISTINCT n.title " +
                       "FROM notes n " +
                       "WHERE n.owner_id = ?";

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

    // Returns the latest note with that title
    public Note getNoteByTitle (String title) throws SQLException {
        String query = "SELECT nv.version_id, nv.content, nv.version, n.note_id, n.owner_id, n.title, n.date_created " +
                        "FROM notes n " +
                        "JOIN note_versions nv ON n.note_id = nv.note_id " +
                        "WHERE n.title = ? " +
                        "ORDER BY nv.version DESC LIMIT 1";

        try (Connection conn = DatabaseConnector.getConnection();
                PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, title);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return new Note(
                    rs.getInt("note_id"),
                    rs.getInt("owner_id"),
                    rs.getInt("version"),
                    rs.getString("title"),
                    rs.getString("content"),
                    rs.getTimestamp("date_created").toString()
                );
            }
        }
        return null;
    }

    // Returns the note with that title and version
    public Note getNoteByTitleAndVersion (String title, int version) throws SQLException {
        String query = "SELECT nv.version_id, nv.content, nv.version, n.note_id, n.owner_id, n.title, n.date_created " +
                        "FROM notes n " +
                        "JOIN note_versions nv ON n.note_id = nv.note_id " +
                        "WHERE n.title = ? AND nv.version = ?";

        try (Connection conn = DatabaseConnector.getConnection();
            PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, title);
            stmt.setInt(2, version);

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return new Note(
                    rs.getInt("note_id"),
                    rs.getInt("owner_id"),
                    rs.getInt("version"),
                    rs.getString("title"),
                    rs.getString("content"),
                    rs.getTimestamp("date_created").toString()
                );
            }
        }
        return null;
    }

    // Checks if a user is owner of a certain note
    public Boolean isOwner(int user_id, String title) throws SQLException {
        String query = "SELECT 1 " +
                       "FROM notes " +
                       "WHERE owner_id = ? " +
                       "AND title = ?";

        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, user_id);
            stmt.setString(2, title);

            ResultSet rs = stmt.executeQuery();

            return rs.next();
        }
    }

    // Checks if a user has a certain role
    public Boolean hasAccess(int user_id, String title, String role) throws SQLException {
        String query = "SELECT * FROM notes n " +
                       "LEFT JOIN access_logs al ON n.note_id = al.note_id " +
                       "WHERE (n.owner_id = ? OR (al.user_id = ? AND al.user_role = ?)) " +
                       "AND n.title = ?";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, user_id);
            stmt.setInt(2, user_id);
            stmt.setString(3, role);
            stmt.setString(4, title);
            return stmt.executeQuery().next();
        }
    }
    
    // Grants access to a user to view or edit a note
    public void grantAccessNote(int other_userId, int noteId, int userId, String role) throws SQLException {
        String query = "INSERT INTO access_logs (user_id, note_id, owner_id, user_role) VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseConnector.getConnection();
            PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, other_userId);
            stmt.setInt(2, noteId);
            stmt.setInt(3, userId);
            stmt.setString(4, role);
            stmt.executeUpdate();
        }
    }
}
