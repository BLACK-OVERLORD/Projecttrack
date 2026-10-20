package projecttrack.model;

import java.io.Serializable;

public class Comment implements Serializable {
    private static final long serialVersionUID = 1L;

    public int userId;
    public String text;
    public String time;

    public Comment(int userId, String text, String time) {
        this.userId = userId;
        this.text = text;
        this.time = time;
    }

    public Comment copy() {
        return new Comment(userId, text, time);
    }
}
