package projecttrack.model;

import java.io.Serializable;

public class Project implements Serializable {
    private static final long serialVersionUID = 1L;

    public int id;
    public int managerId;
    public String name;
    public String description;
    public String start;
    public String due;
    public String status;

    public Project(int id, String name, String description, String start, String due, String status, int managerId) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.start = start;
        this.due = due;
        this.status = status;
        this.managerId = managerId;
    }

    public Project copy() {
        return new Project(id, name, description, start, due, status, managerId);
    }
}
