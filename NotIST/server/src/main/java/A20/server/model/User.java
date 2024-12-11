package A20.server.model;

import java.util.ArrayList;
import java.util.List;

public class User {
    private int u_id;
    private String username;
    private String publicKey;
    private String password;
    //private List<Integer> notes;

    // Firstly created
    public User(String username, String password, String publicKey) {
        this.username = username;
        this.publicKey = publicKey;
        this.password = password;
        //this.notes = new ArrayList<>();
    }

    // To receive from db
    public User(int id, String username, String password, String publicKey) {
        this.u_id = id;
        this.username = username;
        this.publicKey = publicKey;
        this.password = password;
        //this.notes = new ArrayList<>();
    }

    // Setters
    public void setUsername(String username) { this.username = username; }

    public void setPassword(String password) { this.password = password; }

    public void setPublicKey(String publicKey) { this.publicKey = publicKey; }

    // Getters

    public int getUserId() { return this.u_id; }

    public String getUsername () { return this.username; }

    public String getPassword () { return this.password; }

    public String getPublicKey () { return this.publicKey; }

    // Notes op's
    /* 
    public void addNotes(int... newNotes) {
        for (int note : newNotes) {
            if (!this.notes.contains(note))
                this.notes.add(note);
        }
    }
    
    
    public void removeNotes(int... notesToRemove) {
        for (int note : notesToRemove) {
            if(this.notes.contains((Integer) note))
                this.notes.remove(note);
        }
    }

    public List<Integer> getNotes() {
        return this.notes;
    } 
    */

    @Override
    public String toString() {
        return "User { " + "id= " + this.u_id +
        ", Username: " + this.username +
        ", Password: " + this.password +
        ", Public Key: " + this.publicKey;
    }
}
