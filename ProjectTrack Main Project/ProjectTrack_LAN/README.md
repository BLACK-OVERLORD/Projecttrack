# ProjectTrack LAN - Networked Ticket-Based Project Tracking System

## What changed

This version converts the previous single-PC ProjectTrack application into a **client/server LAN application**.

- One Windows PC runs `ProjectTrackServer.jar`.
- Multiple Windows PCs/devices on the same Local Area Network run `ProjectTrackClient.jar`.
- All clients connect to the same server over TCP.
- The server owns the shared project/ticket data and saves it centrally in `data/projecttrack-lan.dat`.
- Whenever a client creates, edits, deletes, or comments on data, the server persists the change and broadcasts a fresh state to all connected clients.
- Each client refreshes its Swing UI when a live `STATE_UPDATE` arrives.

The result is simultaneous multi-device access with live updates inside a LAN.

## Technology stack

- Java 21+ (no external libraries required)
- Java Swing
- TCP sockets: `ServerSocket` / `Socket`
- Java object streams: `ObjectInputStream` / `ObjectOutputStream`
- Local persistence on the server using Java serialization
- Windows batch files for build/run

## Folder structure

```text
ProjectTrack_LAN/
├── src/projecttrack/client/           Client GUI + networking
├── src/projecttrack/server/           Server + server GUI + smoke test
├── src/projecttrack/model/             Shared data classes
├── src/projecttrack/net/               Network request/message classes
├── data/                               Server-side shared data file
├── dist/                               Generated JAR files
├── resources/ProjectTrack.ico          Project icon
├── build.bat
├── run_server.bat
├── run_projecttrack.bat
├── run_smoke_test.bat
├── allow_firewall_5050.bat
├── NETWORK_SETUP.md
├── ARCHITECTURE.md
├── CODE_EXPLANATION.md
├── TESTING.md
└── README.md
```

## Requirements

Install a **JDK**, not only a JRE. JDK 21 or newer is recommended.

Check:

```cmd
java -version
javac -version
```

No XAMPP, MySQL, Maven, JavaFX or internet connection is required.

## Build

From the project folder, double-click `build.bat`.

It creates:

- `dist\ProjectTrackClient.jar`
- `dist\ProjectTrackServer.jar`
- `dist\ProjectTrackLAN_SmokeTest.jar`

## Run the server

On the Windows PC that will act as the server:

1. Run `build.bat` once.
2. Double-click `run_server.bat`.
3. The server window lists its LAN IPv4 addresses and listens on TCP port 5050.
4. Keep this server window running.

If Windows Firewall blocks incoming connections, run `allow_firewall_5050.bat` as Administrator and choose a Private network profile when Windows asks.

## Run the clients

On every Windows device that should use the system:

1. Copy the project folder or at least the client JAR to the device.
2. Make sure Java is installed.
3. Run `run_projecttrack.bat`.
4. Enter the server PC's LAN IP shown in the server window, for example `192.168.1.10`.
5. Port: `5050`.
6. Log in.

Demo accounts:

- Admin: `admin` / `admin123`
- Project Manager: `manager` / `manager123`

## Live update example

1. Client A logs in and opens Tickets.
2. Client B logs in and opens Tickets.
3. Client B changes a ticket status or creates a ticket.
4. Server writes the change to `data/projecttrack-lan.dat`.
5. Server broadcasts `STATE_UPDATE` to all authenticated clients.
6. Client A receives the state and rebuilds its tabs automatically.

## Data location

The authoritative data is stored on the **server PC**:

```text
data\projecttrack-lan.dat
```

Clients do not maintain independent copies of the application data.

## Important operational rule

Run the server on exactly one PC for a given LAN project workspace. All clients should connect to that same server PC.

## Common problems

### Connection refused
- Confirm the server is running.
- Confirm the IP address is correct.
- Confirm TCP port 5050 is allowed through Windows Firewall.
- Confirm both devices are on the same LAN.

### Server IP changed
If DHCP assigns a new IP, restart the server and use the new IPv4 address shown in the server window.

### Want to reset all data
Stop the server and delete:

```text
data\projecttrack-lan.dat
```

Then start the server again; it will create demo data.


## Windows self-contained EXEs

For distribution to PCs that do not have Java installed, use a Windows PC with JDK 21+.

Run:

```cmd
build.bat
package_windows.bat
verify_windows_packages.bat
```

This creates:

```text
release\ProjectTrack Client\ProjectTrack Client.exe
release\ProjectTrack Server\ProjectTrack Server.exe
```

Each app-image contains its own `runtime` folder. Client PCs therefore do not need a separate Java installation.

The packaging computer must itself have a JDK because `jpackage` is a JDK packaging tool.
See `WINDOWS_BUNDLED_RUNTIME.md` for details.
