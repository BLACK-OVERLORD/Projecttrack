# ProjectTrack LAN Architecture

## 1. Architecture

The project uses a client/server model.

### Server

- Accepts TCP connections.
- Authenticates users.
- Performs all writes to shared application state.
- Saves the shared state to disk.
- Broadcasts the updated state to authenticated clients.

### Client

- Provides the Swing desktop interface.
- Sends user actions as `Request` objects.
- Maintains the most recent `AppState` received from the server.
- Listens continuously for `Message` objects.
- Rebuilds the visible tabs when a `STATE_UPDATE` message arrives.

## 2. Message flow

```text
Client A -> LOGIN -> Server
Server   -> AUTH_RESULT -> Client A

Client B -> UPDATE_TICKET -> Server
Server   -> save shared state
Server   -> STATE_UPDATE -> Client A
Server   -> STATE_UPDATE -> Client B
```

## 3. Concurrency

- Each client has a dedicated server-side handler thread.
- The server protects the shared `AppState` with a lock.
- Client output streams are synchronized before writing messages.
- The server uses a thread-safe client list.

## 4. Persistence

The server serializes the complete `AppState` to:

```text
data/projecttrack-lan.dat
```

A temporary file is written first and then moved over the previous file to reduce the chance of leaving a partially written state.

## 5. Live state model

The application intentionally sends a fresh state snapshot after each mutation. This is simple and appropriate for a small LAN/academic application. A production system with very large data volumes would normally use a database plus incremental events/WebSocket-style messaging instead.
