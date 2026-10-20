# Code Explanation

## model/AppState.java

`AppState` is the shared in-memory representation of users, projects, tickets and ID counters. `copy()` creates a deep-enough snapshot for safe network broadcasting.

## model/User.java

Stores login identity information and role. `copy()` creates a serializable snapshot.

## model/Project.java

Stores project identity, description, dates, status and manager.

## model/Ticket.java

Stores ticket fields plus comments and status history. `copy()` duplicates the ticket and its nested lists.

## model/Comment.java

Stores the user who wrote a comment, its text and time.

## net/Request.java

Defines commands sent from clients to the server. Examples: `LOGIN`, `ADD_TICKET`, `UPDATE_TICKET`, `ADD_COMMENT`.

## net/Message.java

Defines server-to-client messages. `AUTH_RESULT` is used during login; `STATE_UPDATE` is used for live state broadcasts; `ERROR` reports server-side validation failures.

## server/ProjectTrackServer.java

The central network service.

- Opens a `ServerSocket` on TCP port 5050.
- Accepts multiple clients.
- Creates a `ClientHandler` thread for each connection.
- Authenticates clients against the server-side state.
- Handles project/ticket/user changes.
- Saves changes to `data/projecttrack-lan.dat`.
- Broadcasts a state snapshot after a successful mutation.

## server/ServerMain.java

Windows Swing control panel for starting and stopping the server. It shows the TCP port, LAN IP addresses, connected-client count and server activity log.

## server/SmokeTestMain.java

Automated command-line test that proves two socket clients can authenticate simultaneously and that a state change from one client reaches the other client.

## client/LanConnection.java

Encapsulates the TCP connection. It logs in synchronously, then starts a background listener thread that receives live messages from the server.

## client/Main.java

The Swing client application. It keeps the previous ProjectTrack features and replaces local persistence with network requests. Each create/update/delete/comment action sends a `Request` to the server. Incoming `STATE_UPDATE` messages cause the client UI to rebuild from the latest shared state.
