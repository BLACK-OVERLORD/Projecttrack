package projecttrack.net;

import projecttrack.model.AppState;
import projecttrack.model.User;

import java.io.Serializable;

public class Message implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Type {
        AUTH_RESULT,
        STATE_UPDATE,
        ERROR,
        PONG,
        INFO
    }

    public Type type;
    public boolean success;
    public String text;
    public User user;
    public AppState state;

    public static Message authResult(boolean success, String text, User user, AppState state) {
        Message m = new Message();
        m.type = Type.AUTH_RESULT;
        m.success = success;
        m.text = text;
        m.user = user;
        m.state = state;
        return m;
    }

    public static Message stateUpdate(AppState state, String text) {
        Message m = new Message();
        m.type = Type.STATE_UPDATE;
        m.success = true;
        m.text = text;
        m.state = state;
        return m;
    }

    public static Message error(String text) {
        Message m = new Message();
        m.type = Type.ERROR;
        m.success = false;
        m.text = text;
        return m;
    }

    public static Message pong() {
        Message m = new Message();
        m.type = Type.PONG;
        m.success = true;
        m.text = "PONG";
        return m;
    }

    public static Message info(String text) {
        Message m = new Message();
        m.type = Type.INFO;
        m.success = true;
        m.text = text;
        return m;
    }
}
