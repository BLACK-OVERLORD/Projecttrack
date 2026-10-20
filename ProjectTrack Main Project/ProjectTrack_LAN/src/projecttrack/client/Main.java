package projecttrack.client;

import projecttrack.model.AppState;
import projecttrack.model.Comment;
import projecttrack.model.Project;
import projecttrack.model.Ticket;
import projecttrack.model.User;
import projecttrack.net.Message;
import projecttrack.net.Request;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Main {
    static final Color BG = new Color(245, 247, 250);
    static final Color PRIMARY = new Color(42, 67, 101);
    static final SimpleDateFormat DF = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    private LanConnection connection;
    private User currentUser;
    private JFrame mainFrame;
    private JTabbedPane tabs;
    private JLabel connectionLabel;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Main().showLogin());
    }

    private void showLogin() {
        JFrame f = new JFrame("ProjectTrack LAN - Login");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setSize(520, 380);
        f.setLocationRelativeTo(null);

        JPanel root = new JPanel(new GridBagLayout());
        root.setBackground(BG);
        JPanel card = new JPanel(new GridBagLayout());
        card.setBorder(new EmptyBorder(22, 28, 22, 28));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("PROJECTTRACK LAN", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        title.setForeground(PRIMARY);
        JLabel sub = new JLabel("Ticket-Based Project Progress Tracking", SwingConstants.CENTER);
        JTextField host = new JTextField("127.0.0.1", 18);
        JTextField port = new JTextField("5050", 18);
        JTextField user = new JTextField("admin", 18);
        JPasswordField pass = new JPasswordField(18);
        JButton login = new JButton("Connect & Login");
        JLabel hint = new JLabel("Server PC and all clients must be on the same LAN.", SwingConstants.CENTER);

        add(card, title, c, 0);
        add(card, sub, c, 1);
        add(card, new JLabel("Server IP / Hostname"), c, 2);
        add(card, host, c, 3);
        add(card, new JLabel("Port"), c, 4);
        add(card, port, c, 5);
        add(card, new JLabel("Username"), c, 6);
        add(card, user, c, 7);
        add(card, new JLabel("Password"), c, 8);
        add(card, pass, c, 9);
        add(card, login, c, 10);
        add(card, hint, c, 11);

        root.add(card);
        f.setContentPane(root);
        f.setVisible(true);

        Runnable connectAction = () -> {
            try {
                int p = Integer.parseInt(port.getText().trim());
                connection = new LanConnection(this::handleMessage);
                Message result = connection.connect(host.getText().trim(), p, user.getText().trim(), new String(pass.getPassword()));
                if (!result.success) {
                    JOptionPane.showMessageDialog(f, result.text, "Login Failed", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                currentUser = result.user;
                f.dispose();
                showMain(host.getText().trim(), p);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(f,
                        "Could not connect to server.\n\n" + ex.getMessage(),
                        "Connection Failed", JOptionPane.ERROR_MESSAGE);
            }
        };
        login.addActionListener(e -> connectAction.run());
        pass.addActionListener(e -> connectAction.run());
    }

    private void handleMessage(Message message) {
        SwingUtilities.invokeLater(() -> {
            if (message.type == Message.Type.STATE_UPDATE && mainFrame != null) {
                rebuildTabs();
                setConnectionText("Connected • Live update received");
            } else if (message.type == Message.Type.ERROR) {
                JOptionPane.showMessageDialog(mainFrame, message.text, "Server Message", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private void showMain(String host, int port) {
        mainFrame = new JFrame("ProjectTrack LAN - " + currentUser.name);
        mainFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        mainFrame.setSize(1180, 760);
        mainFrame.setLocationRelativeTo(null);

        tabs = new JTabbedPane();
        rebuildTabs();

        JButton logout = new JButton("Logout");
        logout.addActionListener(e -> {
            if (connection != null) {
                try { connection.send(Request.logout()); } catch (Exception ignored) {}
                connection.close();
            }
            mainFrame.dispose();
            showLogin();
        });

        JPanel top = new JPanel(new BorderLayout());
        top.setBorder(new EmptyBorder(8, 12, 8, 12));
        top.setBackground(PRIMARY);
        connectionLabel = new JLabel("ProjectTrack LAN   |   " + currentUser.name + " (" + currentUser.role + ")   |   Server: " + host + ":" + port);
        connectionLabel.setForeground(Color.WHITE);
        connectionLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        top.add(connectionLabel, BorderLayout.CENTER);
        top.add(logout, BorderLayout.EAST);

        mainFrame.add(tabs, BorderLayout.CENTER);
        mainFrame.add(top, BorderLayout.NORTH);
        mainFrame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosing(java.awt.event.WindowEvent e) {
                logout.doClick();
            }
        });
        mainFrame.setVisible(true);
    }

    private void rebuildTabs() {
        if (tabs == null) return;
        int selected = tabs.getSelectedIndex();
        tabs.removeAll();
        tabs.addTab("Dashboard", dashboard());
        tabs.addTab("Projects", projects());
        tabs.addTab("Tickets", tickets());
        tabs.addTab("Users", users());
        tabs.addTab("Reports", reports());
        if (selected >= 0 && selected < tabs.getTabCount()) tabs.setSelectedIndex(selected);
    }

    private void setConnectionText(String stateText) {
        if (connectionLabel != null) connectionLabel.setText("ProjectTrack LAN   |   " + currentUser.name + " (" + currentUser.role + ")   |   " + stateText);
    }

    private JPanel dashboard() {
        AppState store = connection.getState();
        JPanel p = base();
        p.setLayout(new BorderLayout(10, 10));
        JPanel cards = new JPanel(new GridLayout(1, 5, 10, 10));
        cards.setOpaque(false);

        long projects = store.projects.size();
        long open = store.tickets.stream().filter(t -> !t.status.equals("Completed") && !t.status.equals("Closed")).count();
        long done = store.tickets.stream().filter(t -> t.status.equals("Completed") || t.status.equals("Closed")).count();
        long overdue = store.tickets.stream().filter(this::overdue).count();
        long critical = store.tickets.stream().filter(t -> t.priority.equals("Critical") && !t.status.equals("Closed") && !t.status.equals("Completed")).count();

        cards.add(card("Projects", String.valueOf(projects)));
        cards.add(card("Open Tickets", String.valueOf(open)));
        cards.add(card("Completed", String.valueOf(done)));
        cards.add(card("Overdue", String.valueOf(overdue)));
        cards.add(card("Critical", String.valueOf(critical)));
        p.add(cards, BorderLayout.NORTH);

        DefaultTableModel m = model("Project", "Tickets", "Completed", "Progress");
        for (Project pr : store.projects) {
            long total = store.tickets.stream().filter(t -> t.projectId == pr.id).count();
            long c = store.tickets.stream().filter(t -> t.projectId == pr.id && (t.status.equals("Completed") || t.status.equals("Closed"))).count();
            int pct = total == 0 ? 0 : (int) Math.round(c * 100.0 / total);
            m.addRow(new Object[]{pr.name, total, c, pct + "%"});
        }

        JTable table = new JTable(m);
        table.setRowHeight(28);
        p.add(new JScrollPane(table), BorderLayout.CENTER);
        JLabel note = new JLabel("Live updates are received from the LAN server whenever any client changes project data.");
        p.add(note, BorderLayout.SOUTH);
        return p;
    }

    private JPanel card(String a, String b) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(210, 215, 220)),
                new EmptyBorder(15, 10, 15, 10)
        ));
        JLabel x = new JLabel(a, SwingConstants.CENTER);
        JLabel y = new JLabel(b, SwingConstants.CENTER);
        x.setFont(new Font("SansSerif", Font.PLAIN, 13));
        y.setFont(new Font("SansSerif", Font.BOLD, 25));
        p.add(x, BorderLayout.NORTH);
        p.add(y, BorderLayout.CENTER);
        return p;
    }

    private JPanel projects() {
        JPanel p = base();
        p.setLayout(new BorderLayout(8, 8));
        DefaultTableModel m = model("ID", "Project", "Description", "Start", "Due", "Status", "Manager");
        JTable t = new JTable(m);
        t.setAutoCreateRowSorter(true);
        refreshProjects(m);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton add = new JButton("Add Project");
        JButton edit = new JButton("Edit Project");
        JButton del = new JButton("Delete");
        buttons.add(add);
        buttons.add(edit);
        buttons.add(del);
        p.add(buttons, BorderLayout.NORTH);
        p.add(new JScrollPane(t), BorderLayout.CENTER);

        add.addActionListener(e -> projectDialog(null, m));
        edit.addActionListener(e -> {
            int r = t.getSelectedRow();
            if (r >= 0) {
                int id = (int) t.getValueAt(t.convertRowIndexToModel(r), 0);
                projectDialog(connection.getState().project(id), m);
            }
        });
        del.addActionListener(e -> {
            int r = t.getSelectedRow();
            if (r >= 0) {
                int id = (int) t.getValueAt(t.convertRowIndexToModel(r), 0);
                int confirm = JOptionPane.showConfirmDialog(mainFrame, "Delete this project and its tickets?", "Confirm", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) send(Request.deleteProject(id));
            }
        });
        return p;
    }

    private void refreshProjects(DefaultTableModel m) {
        m.setRowCount(0);
        AppState store = connection.getState();
        for (Project x : store.projects) {
            m.addRow(new Object[]{x.id, x.name, x.description, x.start, x.due, x.status, userName(x.managerId)});
        }
    }

    private void projectDialog(Project existing, DefaultTableModel m) {
        boolean isNew = existing == null;
        Project x = isNew ? new Project(0, "", "", "", "", "Planned", currentUser.id) : existing;
        JTextField name = new JTextField(x.name);
        JTextField desc = new JTextField(x.description);
        JTextField due = new JTextField(x.due);
        JComboBox<String> status = new JComboBox<>(new String[]{"Planned", "In Progress", "On Hold", "Completed"});
        status.setSelectedItem(x.status);
        JPanel q = new JPanel(new GridLayout(0, 2, 6, 6));
        q.add(new JLabel("Name")); q.add(name);
        q.add(new JLabel("Description")); q.add(desc);
        q.add(new JLabel("Due date (yyyy-MM-dd)")); q.add(due);
        q.add(new JLabel("Status")); q.add(status);

        if (JOptionPane.showConfirmDialog(mainFrame, q, isNew ? "Add Project" : "Edit Project", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            if (name.getText().isBlank()) return;
            Project payload = new Project(x.id, name.getText().trim(), desc.getText().trim(), x.start, due.getText().trim(), (String) status.getSelectedItem(), currentUser.id);
            if (isNew) {
                payload.start = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
                send(Request.addProject(payload));
            } else {
                send(Request.updateProject(payload));
            }
        }
    }

    private JPanel tickets() {
        JPanel p = base();
        p.setLayout(new BorderLayout(8, 8));
        DefaultTableModel m = model("ID", "Title", "Project", "Priority", "Status", "Assigned", "Progress", "Due");
        JTable t = new JTable(m);
        t.setAutoCreateRowSorter(true);
        refreshTickets(m);

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton add = new JButton("Create Ticket");
        JButton update = new JButton("Update Selected");
        JButton del = new JButton("Delete");
        JButton comment = new JButton("Add Comment");
        JTextField search = new JTextField(20);
        JButton find = new JButton("Search");
        JButton all = new JButton("Show All");
        bar.add(add); bar.add(update); bar.add(comment); bar.add(del);
        bar.add(new JLabel("Search:")); bar.add(search); bar.add(find); bar.add(all);
        p.add(bar, BorderLayout.NORTH);
        p.add(new JScrollPane(t), BorderLayout.CENTER);

        add.addActionListener(e -> ticketDialog(null, m));
        update.addActionListener(e -> {
            int r = t.getSelectedRow();
            if (r >= 0) ticketDialog(connection.getState().ticket((int) t.getValueAt(t.convertRowIndexToModel(r), 0)), m);
        });
        del.addActionListener(e -> {
            int r = t.getSelectedRow();
            if (r >= 0) {
                int id = (int) t.getValueAt(t.convertRowIndexToModel(r), 0);
                send(Request.deleteTicket(id));
            }
        });
        comment.addActionListener(e -> {
            int r = t.getSelectedRow();
            if (r >= 0) {
                int id = (int) t.getValueAt(t.convertRowIndexToModel(r), 0);
                String c = JOptionPane.showInputDialog(mainFrame, "Comment");
                if (c != null && !c.isBlank()) send(Request.addComment(id, c));
            }
        });
        find.addActionListener(e -> filterTickets(m, search.getText()));
        all.addActionListener(e -> refreshTickets(m));
        return p;
    }

    private void refreshTickets(DefaultTableModel m) {
        m.setRowCount(0);
        AppState store = connection.getState();
        for (Ticket x : store.tickets) {
            m.addRow(new Object[]{x.id, x.title, projectName(x.projectId), x.priority, x.status, userName(x.assignedTo), x.progress + "%", x.due});
        }
    }

    private void filterTickets(DefaultTableModel m, String s) {
        m.setRowCount(0);
        String q = s == null ? "" : s.toLowerCase();
        for (Ticket x : connection.getState().tickets) {
            if (("" + x.id).contains(q) || x.title.toLowerCase().contains(q) || projectName(x.projectId).toLowerCase().contains(q) || x.status.toLowerCase().contains(q) || x.priority.toLowerCase().contains(q)) {
                m.addRow(new Object[]{x.id, x.title, projectName(x.projectId), x.priority, x.status, userName(x.assignedTo), x.progress + "%", x.due});
            }
        }
    }

    private void ticketDialog(Ticket source, DefaultTableModel m) {
        boolean isNew = source == null;
        Ticket x = isNew ? new Ticket(0) : source;
        JTextField title = new JTextField(x.title);
        JTextField desc = new JTextField(x.description);
        JTextField due = new JTextField(x.due);
        JComboBox<String> project = new JComboBox<>();
        for (Project q : connection.getState().projects) project.addItem(q.id + " - " + q.name);
        if (!connection.getState().projects.isEmpty() && x.projectId > 0) project.setSelectedIndex(Math.max(0, indexProject(x.projectId)));
        JComboBox<String> pri = new JComboBox<>(new String[]{"Low", "Medium", "High", "Critical"});
        pri.setSelectedItem(x.priority);
        JComboBox<String> status = new JComboBox<>(new String[]{"New", "Assigned", "In Progress", "Testing", "On Hold", "Completed", "Closed", "Reopened", "Cancelled"});
        status.setSelectedItem(x.status);
        JComboBox<String> ass = new JComboBox<>();
        for (User u : connection.getState().users) ass.addItem(u.id + " - " + u.name);
        if (x.assignedTo > 0) ass.setSelectedIndex(Math.max(0, indexUser(x.assignedTo)));
        JSpinner prog = new JSpinner(new SpinnerNumberModel(x.progress, 0, 100, 5));

        JPanel q = new JPanel(new GridLayout(0, 2, 6, 6));
        q.add(new JLabel("Title")); q.add(title);
        q.add(new JLabel("Description")); q.add(desc);
        q.add(new JLabel("Project")); q.add(project);
        q.add(new JLabel("Priority")); q.add(pri);
        q.add(new JLabel("Status")); q.add(status);
        q.add(new JLabel("Assigned To")); q.add(ass);
        q.add(new JLabel("Progress %")); q.add(prog);
        q.add(new JLabel("Due (yyyy-MM-dd)")); q.add(due);

        if (JOptionPane.showConfirmDialog(mainFrame, q, isNew ? "Create Ticket" : "Update Ticket", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            if (title.getText().isBlank() || project.getSelectedItem() == null) return;
            Ticket payload = x.copy();
            payload.title = title.getText().trim();
            payload.description = desc.getText().trim();
            payload.projectId = selectedId((String) project.getSelectedItem());
            payload.priority = (String) pri.getSelectedItem();
            payload.status = (String) status.getSelectedItem();
            payload.assignedTo = selectedId((String) ass.getSelectedItem());
            payload.progress = (int) prog.getValue();
            payload.due = due.getText().trim();
            if (isNew) {
                send(Request.addTicket(payload));
            } else {
                send(Request.updateTicket(payload));
            }
        }
    }

    private int selectedId(String s) {
        return s == null ? 0 : Integer.parseInt(s.split(" - ")[0]);
    }

    private int indexProject(int id) {
        AppState store = connection.getState();
        for (int i = 0; i < store.projects.size(); i++) if (store.projects.get(i).id == id) return i;
        return 0;
    }

    private int indexUser(int id) {
        AppState store = connection.getState();
        for (int i = 0; i < store.users.size(); i++) if (store.users.get(i).id == id) return i;
        return 0;
    }

    private JPanel users() {
        JPanel p = base();
        p.setLayout(new BorderLayout());
        DefaultTableModel m = model("ID", "Name", "Username", "Role");
        for (User u : connection.getState().users) m.addRow(new Object[]{u.id, u.name, u.username, u.role});
        JTable t = new JTable(m);
        JButton add = new JButton("Add User");
        add.setEnabled("Admin".equalsIgnoreCase(currentUser.role));
        add.addActionListener(e -> {
            JTextField n = new JTextField();
            JTextField u = new JTextField();
            JPasswordField pw = new JPasswordField();
            JComboBox<String> r = new JComboBox<>(new String[]{"Admin", "Project Manager", "Employee", "Viewer"});
            JPanel q = new JPanel(new GridLayout(0, 2));
            q.add(new JLabel("Name")); q.add(n);
            q.add(new JLabel("Username")); q.add(u);
            q.add(new JLabel("Password")); q.add(pw);
            q.add(new JLabel("Role")); q.add(r);
            if (JOptionPane.showConfirmDialog(mainFrame, q, "Add User", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
                User newUser = new User(0, n.getText().trim(), u.getText().trim(), new String(pw.getPassword()), (String) r.getSelectedItem());
                send(Request.addUser(newUser));
            }
        });
        p.add(add, BorderLayout.NORTH);
        p.add(new JScrollPane(t), BorderLayout.CENTER);
        return p;
    }

    private JPanel reports() {
        JPanel p = base();
        p.setLayout(new BorderLayout(10, 10));
        JTextArea out = new JTextArea();
        out.setEditable(false);
        JButton gen = new JButton("Generate Report");
        JButton csv = new JButton("Export Tickets CSV to Desktop");
        JPanel b = new JPanel();
        b.add(gen); b.add(csv);
        p.add(b, BorderLayout.NORTH);
        p.add(new JScrollPane(out), BorderLayout.CENTER);

        gen.addActionListener(e -> {
            StringBuilder s = new StringBuilder("PROJECTTRACK LAN REPORT\nGenerated: " + DF.format(new Date()) + "\n\n");
            for (Project pr : connection.getState().projects) {
                long total = connection.getState().tickets.stream().filter(t -> t.projectId == pr.id).count();
                long done = connection.getState().tickets.stream().filter(t -> t.projectId == pr.id && (t.status.equals("Completed") || t.status.equals("Closed"))).count();
                s.append(pr.name).append(" - ").append(done).append("/").append(total).append(" completed\n");
            }
            s.append("\nTicket Summary\n");
            for (Ticket t : connection.getState().tickets) {
                s.append("#").append(t.id).append(" | ").append(t.title).append(" | ").append(t.status).append(" | ").append(t.priority).append("\n");
            }
            out.setText(s.toString());
        });

        csv.addActionListener(e -> {
            try {
                Path path = Paths.get(System.getProperty("user.home"), "Desktop", "projecttrack_tickets.csv");
                try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(path))) {
                    w.println("ID,Title,Project,Priority,Status,Assigned,Progress,Due");
                    for (Ticket t : connection.getState().tickets) {
                        w.printf("%d,\"%s\",\"%s\",%s,%s,\"%s\",%d,%s%n",
                                t.id,
                                t.title.replace("\"", "\"\""),
                                projectName(t.projectId),
                                t.priority,
                                t.status,
                                userName(t.assignedTo),
                                t.progress,
                                t.due);
                    }
                }
                JOptionPane.showMessageDialog(mainFrame, "Exported to " + path.toAbsolutePath());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(mainFrame, ex.getMessage(), "Export Failed", JOptionPane.ERROR_MESSAGE);
            }
        });
        return p;
    }

    private JPanel base() {
        JPanel p = new JPanel();
        p.setBackground(BG);
        p.setBorder(new EmptyBorder(12, 12, 12, 12));
        return p;
    }

    private DefaultTableModel model(String... x) {
        return new DefaultTableModel(x, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
    }

    private boolean overdue(Ticket t) {
        try {
            return !t.status.equals("Completed") && !t.status.equals("Closed") && new SimpleDateFormat("yyyy-MM-dd").parse(t.due).before(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    private String userName(int id) {
        for (User u : connection.getState().users) if (u.id == id) return u.name;
        return "Unassigned";
    }

    private String projectName(int id) {
        Project p = connection.getState().project(id);
        return p == null ? "Unknown" : p.name;
    }

    private void send(Request request) {
        try {
            connection.send(request);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(mainFrame, "Network error: " + e.getMessage(), "Network Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void add(JPanel p, Component x, GridBagConstraints c, int y) {
        c.gridy = y;
        p.add(x, c);
    }
}
