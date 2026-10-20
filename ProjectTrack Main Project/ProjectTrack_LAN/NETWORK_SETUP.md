# ProjectTrack LAN Network Setup

## Network layout

```text
                     Local Area Network
                            |
             +--------------+--------------+
             |                             |
     Windows Server PC             Client PC / Laptop
     ProjectTrackServer.jar        ProjectTrackClient.jar
             |                             |
             |<-------- TCP :5050 ------->|
             |                             |
             +--------- Client 2 ----------+
             |
             +--------- Client 3 ----------+

Shared state lives on the server PC.
```

## Step-by-step

### 1. Choose the server machine

Use one Windows PC that will remain available to the project team.

### 2. Find the server's LAN IP

Start `run_server.bat`. The server window displays one or more IPv4 addresses such as:

```text
LAN addresses: 192.168.1.10
```

### 3. Open port 5050

Run `allow_firewall_5050.bat` as Administrator if Windows Firewall blocks clients.

### 4. Connect clients

On each client device, run `run_projecttrack.bat` and enter:

```text
Server IP: 192.168.1.10
Port:      5050
```

### 5. Test live updates

Open the same ticket list on two clients. Edit the ticket on one device. The other device should refresh automatically after the server broadcasts the update.

## Port

Default TCP port: **5050**.

To change the port in this version, edit `ServerMain.java` and the client login port field/default. If a different port is used, the Windows firewall rule must also be changed.

## Security note

This version is intended for a trusted LAN / academic project environment. It uses Java object serialization over a TCP socket and sends passwords as part of the login object. Do not expose this service directly to the public internet.


## Self-contained Windows client

The packaged `release\ProjectTrack Client\ProjectTrack Client.exe` includes a Java runtime. After `package_windows.bat` has been run on a Windows build computer, client PCs do not need a JDK/JRE installed. Copy the entire client app-image folder, including `runtime`, to each client PC.
