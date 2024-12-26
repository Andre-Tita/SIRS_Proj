package A20.server.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.*;

public class Note {
    private int id;
    private String title;
    private String content;
    private LocalDateTime data_created;
    private LocalDateTime date_modified;
    private int last_modified_by;
    private int version;
    private int owner_id;
    private List<User> viewers;
    private List<User> editors;

    // Firstly created
    public Note(String title, String content, int owner_id) {
        this.title = title;
        this.content = content;
        this.data_created = LocalDateTime.now();
        this.date_modified = data_created;
        this.last_modified_by = owner_id;
        this.version = 1;
        this.owner_id = owner_id;
        this.viewers = new ArrayList<>();
        this.editors = new ArrayList<>();
    }

    // To receive from db
    public Note(int id, String title, String content, LocalDateTime data_created, LocalDateTime date_modified, int last_modified_by, int version, int owner_id) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.data_created = data_created;
        this.date_modified = date_modified;
        this.last_modified_by = last_modified_by; 
        this.version = version;
        this.owner_id = owner_id;
        this.viewers = new ArrayList<>();
        this.editors = new ArrayList<>();
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
    public LocalDateTime getDataCreated() { return data_created; }
    public LocalDateTime getDateModified() { return date_modified; }
    public String getDataCreatedString() { return data_created.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")); }
    public String getDateModifiedString() { return date_modified.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")); }
    public int getLastModifiedBy() { return this.last_modified_by; }
    public int getVersion() { return this.version; }
    public int getOwnerId() { return this.owner_id; }
    
    // Others
    public void addViewer(User u) { this.viewers.add(u); }
    public void addEditor(User u) { this.editors.add(u); }
    public void removeViewer(int u_id) {
        for(int i = 0; i < this.viewers.size(); i++) {
            if (this.viewers.get(i).getUserId() == u_id) {
                this.viewers.remove(i);
                return;
            }
        }
    }
    public void removeEditor(int u_id) {
        for(int i = 0; i < this.editors.size(); i++) {
            if (this.editors.get(i).getUserId() == u_id) {
                this.editors.remove(i);
                return;
            }
        }
    }

    @Override
    public String toString() {
        return "Note: " + "id =" + this.id + '\n' +
            "title ='" + this.title + '\'' + '\n' +
            "content ='" + this.content + '\'' + '\n' +
            "data_created =" + this.data_created + '\n' +
            "date_modified =" + this.date_modified + '\n' +
            "last_modified_by =" + this.last_modified_by + '\n' +
            "version = " + this.version + '\n' +
            "ownerId =" + this.owner_id;
    }

    public JsonObject toJSON() {
        JsonObject noteJson = new JsonObject();
        JsonArray viewersArray = new JsonArray();
        JsonArray editorsArray = new JsonArray();
    
        // Basic fields
        noteJson.addProperty("id", this.id);
        noteJson.addProperty("title", this.title);
        noteJson.addProperty("content", this.content);
        noteJson.addProperty("data_created", this.data_created.format(DateTimeFormatter.ISO_DATE_TIME));
        noteJson.addProperty("date_modified", this.date_modified.format(DateTimeFormatter.ISO_DATE_TIME));
        noteJson.addProperty("last_modified_by", this.last_modified_by);
        noteJson.addProperty("version", this.version);
        noteJson.addProperty("owner_id", this.owner_id);
    
        // Add viewers to JsonArray
        for (User viewer : this.viewers) {
            JsonObject viewerJson = new JsonObject();
            viewerJson.addProperty("id", viewer.getUserId());
            viewerJson.addProperty("username", viewer.getUsername());
            viewersArray.add(viewerJson);
        }
    
        // Add editors to JsonArray
        for (User editor : this.editors) {
            JsonObject editorJson = new JsonObject();
            editorJson.addProperty("id", editor.getUserId());
            editorJson.addProperty("username", editor.getUsername());
            editorsArray.add(editorJson);
        }
    
        // Add arrays to noteJson
        noteJson.add("viewers", viewersArray);
        noteJson.add("editors", editorsArray);
    
        return noteJson;
    }
    
}