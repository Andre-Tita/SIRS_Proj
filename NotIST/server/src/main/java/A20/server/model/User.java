package A20.server.model;

public class User {
    private int u_id;
    private String username;
    private String publicKey;
    private String password;

    // Firstly created
    public User(String username, String password, String publicKey) {
        this.username = username;
        this.publicKey = publicKey;
        this.password = password;
    }

    // To receive from db
    public User(int id, String username, String password, String publicKey) {
        this.u_id = id;
        this.username = username;
        this.publicKey = publicKey;
        this.password = password;
    }

    public User(int id, String username) {
        this.u_id = id;
        this.username = username;
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

    @Override
    public String toString() {
        return "User { " + "id= " + this.u_id +
        ", Username: " + this.username +
        ", Password: " + this.password +
        ", Public Key: " + this.publicKey;
    }
}
