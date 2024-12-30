package A20.server.model;

public class User {
    private int u_id;
    private String username;
    private String password;
    // Firstly created
    public User(String username, String password) {
        this.username = username;
        this.password = password;
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

    // Getters

    public int getUserId() { return this.u_id; }

    public String getUsername () { return this.username; }

    public String getPassword () { return this.password; }

    @Override
    public String toString() {
        return "User { " + "id= " + this.u_id +
        ", Username: " + this.username +
        ", Password: " + this.password;
    }
}
