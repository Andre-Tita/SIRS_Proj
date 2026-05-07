package A20.server.model;

public class User {
    private int u_id;
    private String username;
    private String password;
    private String pubKey;

    // Firstly created
    public User(String username, String password, String pubKey) {
        this.username = username;
        this.password = password;
        this.pubKey = pubKey;
    }

    // To receive from db
    public User(int id, String username, String password) {
        this.u_id = id;
        this.username = username;
        this.password = password;
    }

    public User(int id, String username) {
        this.u_id = id;
        this.username = username;
    }

    // Setters
    public void setUsername(String username) { this.username = username; }

    public void setPassword(String password) { this.password = password; }

    public void setPubKey(String pubKey) { this.pubKey = pubKey; }

    // Getters

    public int getUserId() { return this.u_id; }

    public String getUsername () { return this.username; }

    public String getPassword () { return this.password; }

    public String getPubKey() { return this.pubKey; }

    @Override
    public String toString() {
        return "User { " + "id= " + this.u_id +
        ", Username: " + this.username +
        ", Password: " + this.password +
        ", Public Key: " + this.pubKey;
    }
}
