# ProjectTrack LAN Testing

## Build test

Run:

```cmd
build.bat
```

Expected: client, server and smoke-test JARs are created in `dist`.

## Automated LAN smoke test

Run:

```cmd
run_smoke_test.bat
```

The smoke test starts the server on port 5099, opens two local TCP clients, authenticates both, updates a ticket from Client B and verifies that Client A receives a `STATE_UPDATE` with the changed ticket.

Expected output includes:

```text
SMOKE TEST PASSED: two LAN clients authenticated and Client A received Client B's live ticket update.
```

## Manual multi-device test

Use at least two Windows devices on the same LAN.

1. Start the server on Device 1.
2. Note the server's IPv4 address.
3. Start a client on Device 1 or Device 2.
4. Log in.
5. Start another client on a second device.
6. Log in.
7. Create a ticket from Client A.
8. Verify the ticket appears automatically on Client B.
9. Change the ticket status on Client B.
10. Verify the status and dashboard counts change automatically on Client A.
11. Restart the server.
12. Verify the data is still present after restart.
