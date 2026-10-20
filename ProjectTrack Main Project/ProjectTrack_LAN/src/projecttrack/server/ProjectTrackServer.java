package projecttrack.server;

import projecttrack.model.AppState;
import projecttrack.model.Comment;
import projecttrack.model.Project;
import projecttrack.model.Ticket;
import projecttrack.model.User;
import projecttrack.net.Message;
import projecttrack.net.Request;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class ProjectTrackServer {
    public static final int DEFAULT_PORT = 5050;
    private static final SimpleDateFormat DF = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    private final int port;
    private final Path dataFile;
    private final List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private final Consumer<String> logger;
    private final Object stateLock = new Object();

    private volatile boolean running;
    private ServerSocket serverSocket;
    private Thread acceptThread;
    private AppState state;

    public ProjectTrackServer(int port, Consumer<String> logger) {
        this.port = port;
        this.logger = logger == null ? s -> {} : logger;
        this.dataFile = Paths.get("data", "projecttrack-lan.dat");
    }

    public void start() throws IOException {
        synchronized (this) {
            if (running) return;
            state = loadState();
            serverSocket = new ServerSocket(port);
            running = true;
            acceptThread = new Thread(this::acceptLoop, "ProjectTrack-Acceptor");
            acceptThread.setDaemon(true);
            acceptThread.start();
        }
        log("Server started on TCP port " + port + ".");
        log("LAN addresses: " + localAddresses());
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (IOException ignored) {
        }
        for (ClientHandler c : clients) c.close();
        clients.clear();
        log("Server stopped.");
    }

    public boolean isRunning() {
        return running;
    }

    public int getClientCount() {
        return clients.size();
    }

    public String localAddresses() {
        List<String> addresses = new ArrayList<>();
        try {
            var interfaces = java.net.NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                var ni = interfaces.nextElement();
                if (!ni.isUp() || ni.isLoopback()) continue;
                var inet = ni.getInetAddresses();
                while (inet.hasMoreElements()) {
                    InetAddress addr = inet.nextElement();
                    if (addr instanceof java.net.Inet4Address) addresses.add(addr.getHostAddress());
                }
            }
        } catch (Exception e) {
            addresses.add("unavailable");
        }
        if (addresses.isEmpty()) addresses.add("127.0.0.1");
        return String.join(", ", addresses);
    }

    private void acceptLoop() {
        while (running) {
            try {
                Socket socket = serverSocket.accept();
                socket.setTcpNoDelay(true);
                ClientHandler handler = new ClientHandler(socket);
                clients.add(handler);
                log("Connection received from " + socket.getRemoteSocketAddress());
                handler.start();
            } catch (SocketException e) {
                if (running) log("Socket error: " + e.getMessage());
            } catch (IOException e) {
                if (running) log("Accept error: " + e.getMessage());
            }
        }
    }

    private AppState loadState() {
        try {
            Files.createDirectories(dataFile.getParent());
            if (Files.exists(dataFile)) {
                try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(dataFile))) {
                    return (AppState) in.readObject();
                }
            }
        } catch (Exception e) {
            log("Could not load saved state; starting with demo data. " + e.getMessage());
        }
        AppState fresh = createDemoState();
        saveState(fresh);
        return fresh;
    }

    private void saveState(AppState stateToSave) {
        try {
            Files.createDirectories(dataFile.getParent());
            Path temp = dataFile.resolveSibling(dataFile.getFileName() + ".tmp");
            try (var out = new java.io.ObjectOutputStream(Files.newOutputStream(temp))) {
                out.writeObject(stateToSave);
            }
            try {
                Files.move(temp, dataFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                        java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException e) {
                Files.move(temp, dataFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            log("Save failed: " + e.getMessage());
        }
    }

    private AppState createDemoState() {
        AppState d = new AppState();
        d.users.add(new User(d.nextUserId++, "Administrator", "admin", "admin123", "Admin"));
        d.users.add(new User(d.nextUserId++, "Project Manager", "manager", "manager123", "Project Manager"));
        d.projects.add(new Project(
                d.nextProjectId++,
                "Demo Software Project",
                "Sample LAN project for testing ProjectTrack",
                new SimpleDateFormat("yyyy-MM-dd").format(new Date()),
                new SimpleDateFormat("yyyy-MM-dd").format(new Date(System.currentTimeMillis() + 30L * 86400000)),
                "In Progress",
                2
        ));

        Ticket t1 = new Ticket(d.nextTicketId++);
        t1.title = "Create login module";
        t1.description = "Implement secure user login";
        t1.projectId = 1;
        t1.assignedTo = 2;
        t1.createdBy = 1;
        t1.priority = "High";
        t1.status = "Completed";
        t1.progress = 100;
        t1.due = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        d.tickets.add(t1);

        Ticket t2 = new Ticket(d.nextTicketId++);
        t2.title = "Build dashboard";
        t2.description = "Create live project progress dashboard";
        t2.projectId = 1;
        t2.assignedTo = 2;
        t2.createdBy = 1;
        t2.priority = "Medium";
        t2.status = "In Progress";
        t2.progress = 50;
        t2.due = new SimpleDateFormat("yyyy-MM-dd").format(new Date(System.currentTimeMillis() + 7L * 86400000));
        d.tickets.add(t2);
        return d;
    }

    private boolean authenticated(ClientHandler client) {
        return client.currentUser != null;
    }

    private void handleRequest(ClientHandler client, Request request) {
        try {
            switch (request.type) {
                case LOGIN -> handleLogin(client, request);
                case PING -> client.send(Message.pong());
                case LOGOUT -> client.close();
                default -> {
                    if (!authenticated(client)) {
                        client.send(Message.error("Not authenticated."));
                        return;
                    }
                    handleAuthenticatedRequest(client, request);
                }
            }
        } catch (Exception e) {
            log("Request error for " + client.description() + ": " + e.getMessage());
            client.send(Message.error("Server error: " + e.getMessage()));
        }
    }

    private void handleLogin(ClientHandler client, Request request) {
        synchronized (stateLock) {
            User user = state.authenticate(request.username == null ? "" : request.username.trim(), request.password == null ? "" : request.password);
            if (user == null) {
                client.send(Message.authResult(false, "Invalid username or password.", null, null));
                return;
            }
            client.currentUser = user;
            client.send(Message.authResult(true, "Login successful.", user.publicCopy(), state.publicCopy()));
        }
        log("User '" + client.currentUser.username + "' connected from " + client.description());
    }

    private void handleAuthenticatedRequest(ClientHandler client, Request request) {
        AppState snapshot;
        String updateMessage;
        synchronized (stateLock) {
            switch (request.type) {
                case ADD_PROJECT -> {
                    Project p = request.project;
                    if (p == null || p.name == null || p.name.isBlank()) throw new IllegalArgumentException("Project name is required.");
                    p.id = state.nextProjectId++;
                    p.managerId = client.currentUser.id;
                    if (p.start == null || p.start.isBlank()) p.start = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
                    state.projects.add(p);
                    updateMessage = "Project created by " + client.currentUser.name + ".";
                }
                case UPDATE_PROJECT -> {
                    Project incoming = request.project;
                    Project existing = state.project(incoming == null ? 0 : incoming.id);
                    if (existing == null) throw new IllegalArgumentException("Project not found.");
                    existing.name = incoming.name;
                    existing.description = incoming.description;
                    existing.due = incoming.due;
                    existing.status = incoming.status;
                    updateMessage = "Project updated by " + client.currentUser.name + ".";
                }
                case DELETE_PROJECT -> {
                    int id = request.id;
                    state.projects.removeIf(p -> p.id == id);
                    state.tickets.removeIf(t -> t.projectId == id);
                    updateMessage = "Project deleted by " + client.currentUser.name + ".";
                }
                case ADD_TICKET -> {
                    Ticket incoming = request.ticket;
                    if (incoming == null || incoming.title == null || incoming.title.isBlank()) throw new IllegalArgumentException("Ticket title is required.");
                    if (state.project(incoming.projectId) == null) throw new IllegalArgumentException("Project not found.");
                    incoming.id = state.nextTicketId++;
                    incoming.createdBy = client.currentUser.id;
                    incoming.created = DF.format(new Date());
                    incoming.comments = new ArrayList<>();
                    incoming.history = new ArrayList<>();
                    incoming.history.add("Ticket created by " + client.currentUser.name + " at " + incoming.created);
                    state.tickets.add(incoming);
                    updateMessage = "Ticket #" + incoming.id + " created by " + client.currentUser.name + ".";
                }
                case UPDATE_TICKET -> {
                    Ticket incoming = request.ticket;
                    Ticket existing = state.ticket(incoming == null ? 0 : incoming.id);
                    if (existing == null) throw new IllegalArgumentException("Ticket not found.");
                    String oldStatus = existing.status;
                    existing.title = incoming.title;
                    existing.description = incoming.description;
                    existing.projectId = incoming.projectId;
                    existing.priority = incoming.priority;
                    existing.status = incoming.status;
                    existing.assignedTo = incoming.assignedTo;
                    existing.progress = Math.max(0, Math.min(100, incoming.progress));
                    existing.due = incoming.due;
                    if (!oldStatus.equals(existing.status)) {
                        existing.history.add("Status: " + oldStatus + " -> " + existing.status + " by " + client.currentUser.name + " at " + DF.format(new Date()));
                    }
                    updateMessage = "Ticket #" + existing.id + " updated by " + client.currentUser.name + ".";
                }
                case DELETE_TICKET -> {
                    int id = request.id;
                    state.tickets.removeIf(t -> t.id == id);
                    updateMessage = "Ticket #" + id + " deleted by " + client.currentUser.name + ".";
                }
                case ADD_COMMENT -> {
                    Ticket t = state.ticket(request.id);
                    if (t == null) throw new IllegalArgumentException("Ticket not found.");
                    if (request.text == null || request.text.isBlank()) throw new IllegalArgumentException("Comment cannot be empty.");
                    String now = DF.format(new Date());
                    t.comments.add(new Comment(client.currentUser.id, request.text.trim(), now));
                    t.history.add("Comment added by " + client.currentUser.name + " at " + now);
                    updateMessage = "Comment added to ticket #" + t.id + " by " + client.currentUser.name + ".";
                }
                case ADD_USER -> {
                    if (!"Admin".equalsIgnoreCase(client.currentUser.role)) throw new SecurityException("Only an Admin can add users.");
                    User incoming = request.user;
                    if (incoming == null || incoming.username == null || incoming.username.isBlank()) throw new IllegalArgumentException("Username is required.");
                    boolean duplicate = state.users.stream().anyMatch(u -> u.username.equalsIgnoreCase(incoming.username));
                    if (duplicate) throw new IllegalArgumentException("Username already exists.");
                    incoming.id = state.nextUserId++;
                    state.users.add(incoming);
                    updateMessage = "User '" + incoming.username + "' created by " + client.currentUser.name + ".";
                }
                default -> throw new IllegalArgumentException("Unsupported request.");
            }
            saveState(state);
            snapshot = state.publicCopy();
        }
        broadcast(Message.stateUpdate(snapshot, updateMessage));
    }

    private void broadcast(Message message) {
        for (ClientHandler client : clients) {
            if (client.currentUser != null) {
                try {
                    client.send(message);
                } catch (Exception e) {
                    client.close();
                }
            }
        }
        log(message.text == null ? "State updated." : message.text + " Live state broadcast to " + getAuthenticatedClientCount() + " client(s).");
    }

    public int getAuthenticatedClientCount() {
        int count = 0;
        for (ClientHandler c : clients) if (c.currentUser != null) count++;
        return count;
    }

    private void removeClient(ClientHandler handler) {
        clients.remove(handler);
        log("Disconnected: " + handler.description());
    }

    private void log(String message) {
        logger.accept("[" + new SimpleDateFormat("HH:mm:ss").format(new Date()) + "] " + message);
    }

    private final class ClientHandler implements Runnable {
        private final Socket socket;
        private ObjectOutputStream out;
        private ObjectInputStream in;
        private Thread thread;
        private volatile User currentUser;

        private ClientHandler(Socket socket) {
            this.socket = socket;
        }

        void start() {
            thread = new Thread(this, "ProjectTrack-Client-" + socket.getPort());
            thread.setDaemon(true);
            thread.start();
        }

        String description() {
            return socket.getRemoteSocketAddress().toString();
        }

        @Override
        public void run() {
            try {
                out = new ObjectOutputStream(socket.getOutputStream());
                out.flush();
                in = new ObjectInputStream(socket.getInputStream());
                while (running && !socket.isClosed()) {
                    Object obj = in.readObject();
                    if (!(obj instanceof Request request)) continue;
                    handleRequest(this, request);
                }
            } catch (Exception e) {
                if (running && !(e instanceof java.net.SocketException)) {
                    log("Client session ended: " + e.getMessage());
                }
            } finally {
                close();
                removeClient(this);
            }
        }

        synchronized void send(Message message) {
            try {
                if (out == null || socket.isClosed()) return;
                out.writeObject(message);
                out.flush();
                out.reset();
            } catch (IOException e) {
                close();
            }
        }

        void close() {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }
}
