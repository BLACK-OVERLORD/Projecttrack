package projecttrack.client;

import projecttrack.model.AppState;
import projecttrack.model.User;
import projecttrack.net.Message;
import projecttrack.net.Request;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.function.Consumer;

public class LanConnection {
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private Thread listener;
    private volatile boolean connected;
    private volatile AppState state;
    private volatile User currentUser;
    private final Consumer<Message> messageConsumer;

    public LanConnection(Consumer<Message> messageConsumer) {
        this.messageConsumer = messageConsumer == null ? m -> {} : messageConsumer;
    }

    public synchronized Message connect(String host, int port, String username, String password) throws Exception {
        close();
        socket = new Socket(host, port);
        socket.setTcpNoDelay(true);
        out = new ObjectOutputStream(socket.getOutputStream());
        out.flush();
        in = new ObjectInputStream(socket.getInputStream());
        sendInternal(Request.login(username, password));
        Object first = in.readObject();
        if (!(first instanceof Message message) || message.type != Message.Type.AUTH_RESULT) {
            throw new IOException("Invalid server response.");
        }
        if (!message.success) {
            close();
            return message;
        }
        connected = true;
        currentUser = message.user;
        state = message.state;
        listener = new Thread(this::listen, "ProjectTrack-Network-Listener");
        listener.setDaemon(true);
        listener.start();
        return message;
    }

    private void listen() {
        try {
            while (connected && socket != null && !socket.isClosed()) {
                Object obj = in.readObject();
                if (obj instanceof Message message) {
                    if (message.state != null) state = message.state;
                    messageConsumer.accept(message);
                }
            }
        } catch (EOFException | java.net.SocketException e) {
            // Normal when the server or client closes the connection.
        } catch (Exception e) {
            messageConsumer.accept(Message.error("Connection lost: " + e.getMessage()));
        } finally {
            connected = false;
        }
    }

    public synchronized void send(Request request) throws IOException {
        if (!connected) throw new IOException("Not connected to the server.");
        sendInternal(request);
    }

    private void sendInternal(Request request) throws IOException {
        out.writeObject(request);
        out.flush();
        out.reset();
    }

    public AppState getState() {
        return state;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public boolean isConnected() {
        return connected;
    }

    public synchronized void close() {
        connected = false;
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
        socket = null;
        out = null;
        in = null;
        listener = null;
    }
}
