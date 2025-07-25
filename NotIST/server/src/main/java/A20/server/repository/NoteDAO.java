package A20.server.repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import A20.server.model.Note;

public class NoteDAO {

    // Create a note
    public void addNote(Note note) throws SQLException {
        String noteQuery = "INSERT INTO notes (title, owner_id, data_created, write_lock) VALUES (?, ?, ?, FALSE) " +
                           "ON CONFLICT (title) DO NOTHING RETURNING note_id";
        String versionQuery = "INSERT INTO note_versions (note_id, version, content, data_created, modified_at, modified_by) " +
                              "VALUES (?, ?, ?, ?, ?, ?)";
    
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement noteStmt = conn.prepareStatement(noteQuery, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement versionStmt = conn.prepareStatement(versionQuery)) {
    
            // Start a transaction
            conn.setAutoCommit(false);
    
            try {
                // Insert the note metadata
                noteStmt.setString(1, note.getTitle());
                noteStmt.setInt(2, note.getOwnerId());
                noteStmt.setTimestamp(3, Timestamp.valueOf(note.getDataCreated()));
                noteStmt.executeUpdate();
    
                int noteId = -1;
                try (ResultSet rs = noteStmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        noteId = rs.getInt(1); // New note inserted
                    }
                }
    
                // If no new note, fetch the existing note_id
                if (noteId == -1) {
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
    
                // Fetch the latest version number for the note
                String fetchVersionNumber = "SELECT COALESCE(MAX(version), 0) AS max_version FROM note_versions WHERE note_id = ?";
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
                versionStmt.setTimestamp(4, Timestamp.valueOf(note.getDataCreated()));
                versionStmt.setTimestamp(5, Timestamp.valueOf(note.getDateModified()));
                versionStmt.setInt(6, note.getLastModifiedBy());
                versionStmt.executeUpdate();
    
                // Commit the transaction
                conn.commit();
    
            } catch (SQLException e) {
                // Rollback in case of an error
                conn.rollback();
                throw e;
            } finally {
                // Reset auto-commit to true (important for subsequent database operations)
                conn.setAutoCommit(true);
            }
        }
    }

    // Notes a certain user has access to
    public List<String> getUsersAccessNotes(int userId) throws SQLException {
        // Query for notes where the user is the owner
        String queryOwner = 
            "SELECT n.title AS title, nv.version AS version " +
            "FROM notes n " +
            "LEFT JOIN note_versions nv ON n.note_id = nv.note_id " +
            "WHERE n.owner_id = ? " +
            "ORDER BY n.title, nv.version";

        // Query for notes where the user has access via access_logs
        String queryAccess = 
            "SELECT n.title AS title, nv.version AS version, al.user_role AS user_role " +
            "FROM access_logs al " +
            "JOIN notes n ON al.note_id = n.note_id " +
            "LEFT JOIN note_versions nv ON n.note_id = nv.note_id " +
            "WHERE al.user_id = ? " +
            "ORDER BY n.title, nv.version";

        List<String> notesTitles = new ArrayList<>();

        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmtOwner = conn.prepareStatement(queryOwner);
             PreparedStatement stmtAccess = conn.prepareStatement(queryAccess)) {

            // Query for notes owned by the user
            stmtOwner.setInt(1, userId);
            try (ResultSet rs = stmtOwner.executeQuery()) {
                while (rs.next()) {
                    String title = rs.getString("title");
                    int version = rs.getInt("version");
                    notesTitles.add(title + " | Version: " + version + " | Role: OWNER");
                }
            }
            
            // Query for notes where the user has access
            stmtAccess.setInt(1, userId);
            try (ResultSet rs = stmtAccess.executeQuery()) {
                while (rs.next()) {
                    String title = rs.getString("title");
                    int version = rs.getInt("version");
                    String role = rs.getString("user_role");
                    notesTitles.add(title + " | Version: " + version + " | Role: " + role);
                }
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
        String query = "SELECT *" +
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
                    rs.getString("title"),
                    rs.getString("content"),
                    (rs.getTimestamp("data_created")).toLocalDateTime(),
                    (rs.getTimestamp("modified_at")).toLocalDateTime(),
                    rs.getInt("modified_by"),
                    rs.getInt("version"),
                    rs.getInt("owner_id")
                );
            }
        }
        return null;
    }

    // Returns the note with that title and version
    public Note getNoteByTitleAndVersion (String title, int version) throws SQLException {
        String query = "SELECT nv.version_id, nv.content, nv.version, n.note_id, n.owner_id, n.title, n.data_created, nv.modified_at, nv.modified_by " +
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
                    rs.getString("title"),
                    rs.getString("content"),
                    rs.getTimestamp("data_created").toLocalDateTime(),
                    rs.getTimestamp("modified_at").toLocalDateTime(),
                    rs.getInt("modified_by"),
                    rs.getInt("version"),
                    rs.getInt("owner_id")
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

    // Returns a list of all users that can view the note
    public List<Integer> getNoteViewers(int note_id) throws SQLException {
        List<Integer> viewers = new ArrayList<>();
        String query = "SELECT * FROM access_logs WHERE note_id = ? AND user_role = 'VIEWER'";
        try (Connection conn = DatabaseConnector.getConnection();
        PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, note_id);

            ResultSet rs = stmt.executeQuery();
            while(rs.next()) {
                viewers.add(rs.getInt("user_id"));
            }
        }

        return viewers;
    }

    // Returns a list of all users that can edit the note
    public List<Integer> getNoteEditors(int note_id) throws SQLException {
        List<Integer> editors = new ArrayList<>();
        String query = "SELECT * FROM access_logs WHERE note_id = ? AND user_role = 'EDITOR'";
        try (Connection conn = DatabaseConnector.getConnection();
        PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, note_id);

            ResultSet rs = stmt.executeQuery();
            while(rs.next()) {
                editors.add(rs.getInt("user_id"));
            }
        }

        return editors;
    } 

    // Removes all accesses (viewer/editor) from a certain note
    public void removeAccesses(int note_id) throws SQLException {
        String query = "DELETE FROM access_logs WHERE note_id = ?";
        try (Connection conn = DatabaseConnector.getConnection();
        PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, note_id);

            stmt.executeUpdate();
        }
    }

    // Inserts a new version of a note
    public void insertNote(Note note) throws SQLException {
        String query = "INSERT INTO note_versions "+
        "(note_id, version, content, data_created, modified_at, modified_by) " +
        "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnector.getConnection();
        PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, note.getNoteId());
            stmt.setInt(2, note.getVersion());
            stmt.setString(3, note.getContent());
            stmt.setTimestamp(4, Timestamp.valueOf(note.getDataCreated()));
            stmt.setTimestamp(5, Timestamp.valueOf(note.getDateModified()));
            stmt.setInt(6, note.getLastModifiedBy());
            
            stmt.executeUpdate();
        }
    }

    // Lock/Unlock a note's to write lock
    public void lockNote(int note_id, boolean lock) throws SQLException {
        String query = "UPDATE notes SET write_lock = ? WHERE note_id = ?";
        try (Connection conn = DatabaseConnector.getConnection();
        PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setBoolean(1, lock);
            stmt.setInt(2, note_id);
            stmt.executeUpdate();
        }

    }

    // Checks if a note is locked
    public boolean isLocked(int note_id) throws SQLException {
        String query = "SELECT write_lock FROM notes WHERE note_id = ?";
        try (Connection conn = DatabaseConnector.getConnection();
        PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, note_id);

            try (ResultSet rs = stmt.executeQuery()) { // Execute the query
                if (rs.next()) { // Check if a result exists
                    return rs.getBoolean("write_lock"); // Return the boolean value
                } else {
                    throw new SQLException("Note not found for ID: " + note_id);
                }
            }
        }
    }
}
