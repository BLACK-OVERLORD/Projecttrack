package projecttrack.server;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class ServerMain {
    private ProjectTrackServer server;
    private final JTextArea log = new JTextArea();
    private final JLabel status = new JLabel("Stopped");
    private final JTextField portField = new JTextField(String.valueOf(ProjectTrackServer.DEFAULT_PORT), 8);
    private final JLabel clients = new JLabel("Clients: 0");
    private final JLabel addresses = new JLabel("LAN addresses: -");
    private JButton start;
    private JButton stop;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ServerMain().show());
    }

    private void show() {
        JFrame frame = new JFrame("ProjectTrack LAN - Server");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 560);
        frame.setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(new EmptyBorder(12, 12, 12, 12));

        JPanel top = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);
        c.anchor = GridBagConstraints.WEST;

        c.gridx = 0; c.gridy = 0; top.add(new JLabel("TCP Port:"), c);
        c.gridx = 1; top.add(portField, c);
        start = new JButton("Start Server");
        stop = new JButton("Stop Server");
        stop.setEnabled(false);
        c.gridx = 2; top.add(start, c);
        c.gridx = 3; top.add(stop, c);
        c.gridx = 4; top.add(new JLabel("Status:"), c);
        c.gridx = 5; top.add(status, c);
        c.gridx = 6; top.add(clients, c);

        c.gridx = 0; c.gridy = 1; c.gridwidth = 7;
        top.add(addresses, c);

        root.add(top, BorderLayout.NORTH);
        log.setEditable(false);
        log.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        root.add(new JScrollPane(log), BorderLayout.CENTER);
        JLabel help = new JLabel("Start this server on one Windows PC. Clients on the same LAN connect to one of the listed IPv4 addresses.");
        root.add(help, BorderLayout.SOUTH);

        start.addActionListener(e -> startServer());
        stop.addActionListener(e -> stopServer());

        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosing(java.awt.event.WindowEvent e) { stopServer(); }
        });
        frame.setContentPane(root);
        frame.setVisible(true);
    }

    private void startServer() {
        try {
            int port = Integer.parseInt(portField.getText().trim());
            server = new ProjectTrackServer(port, this::appendLog);
            server.start();
            status.setText("Running");
            addresses.setText("LAN addresses: " + server.localAddresses());
            start.setEnabled(false);
            stop.setEnabled(true);
            Timer timer = new Timer(1000, e -> {
                if (server != null) clients.setText("Clients: " + server.getAuthenticatedClientCount());
            });
            timer.start();
            appendLog("Share one of the LAN addresses with client devices, e.g. 192.168.1.10:" + port);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, e.getMessage(), "Server Start Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void stopServer() {
        if (server != null) server.stop();
        status.setText("Stopped");
        clients.setText("Clients: 0");
        start.setEnabled(true);
        stop.setEnabled(false);
    }

    private void appendLog(String text) {
        SwingUtilities.invokeLater(() -> {
            log.append(text + System.lineSeparator());
            log.setCaretPosition(log.getDocument().getLength());
        });
    }
}
