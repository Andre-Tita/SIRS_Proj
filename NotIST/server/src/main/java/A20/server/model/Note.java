package A20.server.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Note {
    private int n_id;
    private int owner_id;
    private int version;
    private String title;
    private String content;
    private String createdDate;

    // Firstly created
    public Note(int owner_id, String title, String content) {
        this.owner_id = owner_id;
        this.version = 1;
        this.title = title;
        this.content = content;
        this.createdDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")); // Timestamp
    }

    // To receive from db
    public Note(int id, int owner_id, int version, String title, String content, String createdDate) {
        this.n_id = id;
        this.owner_id = owner_id;
        this.version = version;
        this.title = title;
        this.content = content;
        this.createdDate = createdDate; 
    }

    // Setters
    public void setOwnerId(int o_id) { this.owner_id = o_id; }
    public void setTitle(String t) { this.title = t; }
    public void setContent(String c) { this.content = c; }
    
    // Getters
    
    public int getNoteId() { return this.n_id; }
    public int getOwnerId() { return this.owner_id; }
    public int getVersion() { return this.version; }
    public String getTitle() { return this.title; }
    public String getContent() { return this.content; }
    public String getCreatedDate() { return this.createdDate; }
    
    // Others
    public void incrementVersion() { this.version++; }
    
    @Override
    public String toString() {
        return "Note{" + "id=" + n_id +
            ", ownerId=" + owner_id +
            ", title='" + title + '\'' +
            ", content='" + content + '\'' +
            ", createdDate=" + createdDate +
            '}';
    }
}