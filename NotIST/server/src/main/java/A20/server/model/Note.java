package A20.server.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Note {
    private int id;
    private String title;
    private String content;
    private LocalDateTime date_created;
    private LocalDateTime date_modified;
    private int last_modified_by;
    private int version;
    private int owner_id;

    // Firstly created
    public Note(String title, String content, int owner_id) {
        this.title = title;
        this.content = content;
        this.date_created = LocalDateTime.now();
        this.date_modified = date_created;
        this.last_modified_by = owner_id;
        this.version = 1;
        this.owner_id = owner_id;
    }

    // To receive from db
    public Note(int id, String title, String content, LocalDateTime date_created, LocalDateTime date_modified, int last_modified_by, int version, int owner_id) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.date_created = date_created;
        this.date_modified = date_modified;
        this.last_modified_by = last_modified_by; 
        this.version = version;
        this.owner_id = owner_id;
    }

    // Setters
    public void setOwnerId(int o_id) { this.owner_id = o_id; }
    public void setTitle(String t) { this.title = t; }
    public void setContent(String c) { this.content = c; }

    public void setLastModifiedBy(int last_modified_by) { this.last_modified_by = last_modified_by; }
    public void setDateModified() { this.date_modified = LocalDateTime.now(); }
    
    // Getters
    
    public int getNoteId() { return this.id; }
    public String getTitle() { return this.title; }
    public String getContent() { return this.content; }
    public String getDataCreated() { return data_created.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")); }
    public String getDateModified() { return date_modified.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")); }
    public int getLastModifiedBy() { return this.last_modified_by; }
    public int getVersion() { return this.version; }
    public int getOwnerId() { return this.owner_id; }
    
    @Override
    public String toString() {
        return "Note: " + "id =" + this.id + '\n' +
            "title ='" + this.title + '\'' + '\n' +
            "content ='" + this.content + '\'' + '\n' +
            "date_created =" + this.date_created + '\n' +
            "date_modified =" + this.date_modified + '\n' +
            "last_modified_by =" + this.last_modified_by + '\n' +
            "version = " + this.version + '\n' +
            "ownerId =" + this.owner_id;
    }
}