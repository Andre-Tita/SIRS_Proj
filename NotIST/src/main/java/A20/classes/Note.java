package A20.classes;

import java.time.LocalDateTime;

public class Note {
    private int n_id;
    private int owner_id;
    private int version;
    private String title;
    private String content;
    private LocalDateTime createdDate;

    public Note(int id, int owner_id, String title, String content) {
        this.n_id = id;
        this.owner_id = owner_id;
        this.title = title;
        this.content = content;
        this.createdDate = LocalDateTime.now();
    }

    // Setters
    public void setOwnerId(int o_id) { this.owner_id = o_id; }
    public void setVersion(int v) { this.version = v; }
    public void setTitle(String t) { this.title = t; }
    public void setContent(String c) { this.content = c; }

    // Getters

    public int getOwnerId() { return this.owner_id; }
    public int getVersion() { return this.version; }
    public String getTitle() { return this.title; }
    public String getContent() { return this.content; }

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