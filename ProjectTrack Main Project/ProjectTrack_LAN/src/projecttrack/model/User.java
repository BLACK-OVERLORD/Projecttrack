package projecttrack.model;

import java.io.Serializable;

public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    public int id;
    public String name;
    public String username;
    public String password;
    public String role;

    public User(int id, String name, String username, String password, String role) {
        this.id = id;
        this.name = name;
        this.username = username;
        this.password = password;
        this.role = role;
    }

    public User copy() {
        return new User(id, name, username, password, role);
    }

    public User publicCopy() {
        return new User(id, name, username, "", role);
    }
}
