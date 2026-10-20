# Project Scope - Networked Edition

## Implemented

- Windows desktop Java Swing client.
- Windows desktop Java Swing server.
- TCP LAN networking.
- Multiple simultaneous client connections.
- Centralized server-side persistent data.
- User authentication against server data.
- Projects CRUD.
- Tickets CRUD.
- Ticket assignment.
- Ticket status and progress.
- Ticket comments and history.
- Search and sorting.
- Dashboard metrics.
- Reports and Desktop CSV export.
- Live state broadcasts to all authenticated clients.
- Server-side save after each successful mutation.
- Server restart persistence.

## Not implemented

- Internet/public WAN access.
- TLS encryption.
- Database server such as MySQL/PostgreSQL.
- Conflict resolution beyond last successful server write.
- Granular role restrictions on every operation.
- Offline client editing / synchronization queues.

These are deliberate scope choices for a Windows LAN academic project.
