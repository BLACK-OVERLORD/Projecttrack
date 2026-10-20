package projecttrack.server;

import projecttrack.model.AppState;
import projecttrack.net.Message;
import projecttrack.net.Request;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/** Lightweight command-line test: starts a server and verifies LAN login + live update broadcast. */
public class SmokeTestMain {
    public static void main(String[] args) throws Exception {
        int port = 5099;
        ProjectTrackServer server = new ProjectTrackServer(port, s -> System.out.println("SERVER: " + s));
        server.start();

        try (Socket a = new Socket("127.0.0.1", port);
             Socket b = new Socket("127.0.0.1", port)) {
            a.setTcpNoDelay(true);
            b.setTcpNoDelay(true);
            ObjectOutputStream outA = new ObjectOutputStream(a.getOutputStream());
            outA.flush();
            ObjectInputStream inA = new ObjectInputStream(a.getInputStream());
            ObjectOutputStream outB = new ObjectOutputStream(b.getOutputStream());
            outB.flush();
            ObjectInputStream inB = new ObjectInputStream(b.getInputStream());

            outA.writeObject(Request.login("admin", "admin123"));
            outA.flush();
            Message loginA = (Message) inA.readObject();
            require(loginA.success, "Client A login failed");

            outB.writeObject(Request.login("manager", "manager123"));
            outB.flush();
            Message loginB = (Message) inB.readObject();
            require(loginB.success, "Client B login failed");

            AppState state = loginA.state;
            int original = state.tickets.size();
            var ticket = state.tickets.get(0).copy();
            ticket.title = "LAN smoke test update";
            outB.writeObject(Request.updateTicket(ticket));
            outB.flush();

            Message updateA = waitForState(inA);
            require(updateA.type == Message.Type.STATE_UPDATE, "Client A did not receive live update");
            require(updateA.state.tickets.size() == original, "Unexpected ticket count after update");
            require(updateA.state.ticket(ticket.id).title.equals("LAN smoke test update"), "Updated ticket title not broadcast");
            System.out.println("SMOKE TEST PASSED: two LAN clients authenticated and Client A received Client B's live ticket update.");
        } finally {
            server.stop();
        }
    }

    private static Message waitForState(ObjectInputStream in) throws Exception {
        long deadline = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < deadline) {
            Object obj = in.readObject();
            if (obj instanceof Message m && m.type == Message.Type.STATE_UPDATE) return m;
        }
        throw new IllegalStateException("Timed out waiting for STATE_UPDATE");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
