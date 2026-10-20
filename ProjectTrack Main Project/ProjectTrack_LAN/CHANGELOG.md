## Windows bundled-runtime packaging\n\n- Added `package_windows.bat` to create self-contained Windows app-images for server and client.\n- Added `verify_windows_packages.bat` and packaging documentation.\n- Client and server app-images include their own Java runtime, so target PCs do not need Java installed.\n\n# Changelog

## Networked LAN Edition

- Converted local single-user data flow to a client/server architecture.
- Added TCP socket communication.
- Added multi-client server handling.
- Added live `STATE_UPDATE` broadcasting.
- Moved authoritative persistence to the server.
- Added LAN address display on the server.
- Added Windows firewall helper.
- Added automated two-client smoke test.
- Retained project, ticket, comment, history, dashboard, users and CSV reporting features.
