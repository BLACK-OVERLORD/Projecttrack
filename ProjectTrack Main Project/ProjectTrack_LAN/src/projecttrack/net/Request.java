package projecttrack.net;

import projecttrack.model.Project;
import projecttrack.model.Ticket;
import projecttrack.model.User;

import java.io.Serializable;

public class Request implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Type {
        LOGIN,
        ADD_PROJECT,
        UPDATE_PROJECT,
        DELETE_PROJECT,
        ADD_TICKET,
        UPDATE_TICKET,
        DELETE_TICKET,
        ADD_COMMENT,
        ADD_USER,
        PING,
        LOGOUT
    }

    public Type type;
    public String username;
    public String password;
    public String text;
    public int id;
    public Project project;
    public Ticket ticket;
    public User user;

    private Request(Type type) {
        this.type = type;
    }

    public static Request login(String username, String password) {
        Request r = new Request(Type.LOGIN);
        r.username = username;
        r.password = password;
        return r;
    }

    public static Request addProject(Project project) {
        Request r = new Request(Type.ADD_PROJECT);
        r.project = project;
        return r;
    }

    public static Request updateProject(Project project) {
        Request r = new Request(Type.UPDATE_PROJECT);
        r.project = project;
        return r;
    }

    public static Request deleteProject(int id) {
        Request r = new Request(Type.DELETE_PROJECT);
        r.id = id;
        return r;
    }

    public static Request addTicket(Ticket ticket) {
        Request r = new Request(Type.ADD_TICKET);
        r.ticket = ticket;
        return r;
    }

    public static Request updateTicket(Ticket ticket) {
        Request r = new Request(Type.UPDATE_TICKET);
        r.ticket = ticket;
        return r;
    }

    public static Request deleteTicket(int id) {
        Request r = new Request(Type.DELETE_TICKET);
        r.id = id;
        return r;
    }

    public static Request addComment(int ticketId, String text) {
        Request r = new Request(Type.ADD_COMMENT);
        r.id = ticketId;
        r.text = text;
        return r;
    }

    public static Request addUser(User user) {
        Request r = new Request(Type.ADD_USER);
        r.user = user;
        return r;
    }

    public static Request ping() {
        return new Request(Type.PING);
    }

    public static Request logout() {
        return new Request(Type.LOGOUT);
    }
}
