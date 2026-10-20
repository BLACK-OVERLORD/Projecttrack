package projecttrack.model;

import java.io.Serializable;
import java.util.ArrayList;

public class AppState implements Serializable {
    private static final long serialVersionUID = 1L;

    public ArrayList<User> users = new ArrayList<>();
    public ArrayList<Project> projects = new ArrayList<>();
    public ArrayList<Ticket> tickets = new ArrayList<>();
    public int nextUserId = 1;
    public int nextProjectId = 1;
    public int nextTicketId = 1001;

    public AppState copy() {
        AppState s = new AppState();
        s.nextUserId = nextUserId;
        s.nextProjectId = nextProjectId;
        s.nextTicketId = nextTicketId;
        for (User u : users) s.users.add(u.copy());
        for (Project p : projects) s.projects.add(p.copy());
        for (Ticket t : tickets) s.tickets.add(t.copy());
        return s;
    }


    public AppState publicCopy() {
        AppState s = new AppState();
        s.nextUserId = nextUserId;
        s.nextProjectId = nextProjectId;
        s.nextTicketId = nextTicketId;
        for (User u : users) s.users.add(u.publicCopy());
        for (Project p : projects) s.projects.add(p.copy());
        for (Ticket t : tickets) s.tickets.add(t.copy());
        return s;
    }

    public User authenticate(String username, String password) {
        return users.stream()
                .filter(u -> u.username.equals(username) && u.password.equals(password))
                .findFirst()
                .map(User::copy)
                .orElse(null);
    }

    public Project project(int id) {
        return projects.stream().filter(p -> p.id == id).findFirst().orElse(null);
    }

    public Ticket ticket(int id) {
        return tickets.stream().filter(t -> t.id == id).findFirst().orElse(null);
    }
}
