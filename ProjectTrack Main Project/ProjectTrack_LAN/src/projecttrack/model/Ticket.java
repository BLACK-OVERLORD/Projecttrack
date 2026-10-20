package projecttrack.model;

import java.io.Serializable;
import java.util.ArrayList;

public class Ticket implements Serializable {
    private static final long serialVersionUID = 1L;

    public int id;
    public int projectId;
    public int assignedTo;
    public int createdBy;
    public int progress;
    public String title = "";
    public String description = "";
    public String priority = "Medium";
    public String status = "New";
    public String due = "";
    public String created = "";
    public ArrayList<Comment> comments = new ArrayList<>();
    public ArrayList<String> history = new ArrayList<>();

    public Ticket(int id) {
        this.id = id;
    }

    public Ticket copy() {
        Ticket t = new Ticket(id);
        t.projectId = projectId;
        t.assignedTo = assignedTo;
        t.createdBy = createdBy;
        t.progress = progress;
        t.title = title;
        t.description = description;
        t.priority = priority;
        t.status = status;
        t.due = due;
        t.created = created;
        for (Comment c : comments) t.comments.add(c.copy());
        t.history.addAll(history);
        return t;
    }
}
