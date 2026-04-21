import java.io.*;
import java.net.*;
import java.util.*;

public class Pop3Server {

    /** Port the POP3 server listens on. Change to 1100+ to avoid needing sudo. */
    private static final int PORT = 110;

    /** Valid username for authentication. Must match VacationProgram / VacationRes. */
    private static final String VALID_USER = "johnny";

    /** Valid password for authentication. Must match VacationProgram / VacationRes. */
    private static final String VALID_PASS = "bins";

    /**
     * Starts the POP3 server and accepts incoming client connections.
     * Each client is handled in its own thread.
     *
     * @throws IOException if the server socket cannot be created
     */
    public void start() throws IOException {
        ServerSocket server = new ServerSocket(PORT);
        System.out.println("[POP3] Listening on port " + PORT);
        while (true) {
            Socket client = server.accept();
            new Thread(() -> handleClient(client)).start();
        }
    }

    /**
     * Handles a single POP3 client session from greeting to QUIT.
     * Runs the POP3 state machine: AUTHORIZATION → TRANSACTION → UPDATE.
     *
     */
    private void handleClient(Socket socket) {
        try (
            BufferedReader in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter    out = new PrintWriter(socket.getOutputStream(), true)
        ) {
            String clientAddr = socket.getInetAddress().getHostAddress();
            System.out.println("[POP3] Client connected: " + clientAddr);

            out.println("+OK POP3 Test Server Ready");

            boolean authenticated = false;
            String  user          = "";
            Set<Integer> markedForDeletion = new HashSet<>();

            String line;
            while ((line = in.readLine()) != null) {
                System.out.println("[POP3] << " + line);
                String upper = line.toUpperCase();

                if (upper.startsWith("USER ")) {
                    user = line.substring(5).trim();
                    out.println(user.equals(VALID_USER) ? "+OK User accepted" : "-ERR Unknown user");

                } else if (upper.startsWith("PASS ")) {
                    String pass = line.substring(5).trim();
                    if (user.equals(VALID_USER) && pass.equals(VALID_PASS)) {
                        authenticated = true;
                        out.println("+OK Authenticated");
                    } else {
                        out.println("-ERR Invalid credentials");
                    }

                } else if (upper.equals("STAT")) {
                    if (!authenticated) { out.println("-ERR Not authenticated"); continue; }
                    List<Mailbox.Message> msgs     = Mailbox.getActiveMessages();
                    int                  totalSize = msgs.stream().mapToInt(Mailbox.Message::size).sum();
                    out.println("+OK " + msgs.size() + " " + totalSize);

                } else if (upper.startsWith("LIST")) {
                    if (!authenticated) { out.println("-ERR Not authenticated"); continue; }
                    List<Mailbox.Message> msgs  = Mailbox.getActiveMessages();
                    String[]              parts = line.split(" ");
                    if (parts.length == 2) {
                        // LIST n — single message
                        int n = Integer.parseInt(parts[1]);
                        if (n < 1 || n > msgs.size()) {
                            out.println("-ERR No such message");
                        } else {
                            out.println("+OK " + n + " " + msgs.get(n - 1).size());
                        }
                    } else {
                        // LIST — all messages
                        out.println("+OK " + msgs.size() + " messages");
                        for (int i = 0; i < msgs.size(); i++) {
                            out.println((i + 1) + " " + msgs.get(i).size());
                        }
                        out.println(".");
                    }

                } else if (upper.startsWith("TOP ")) {
                    if (!authenticated) { out.println("-ERR Not authenticated"); continue; }
                    // TOP n l — n = message number, l = body lines (ignored; we send headers only)
                    String[] parts  = line.split(" ");
                    int      msgNum = Integer.parseInt(parts[1]);
                    List<Mailbox.Message> msgs = Mailbox.getActiveMessages();
                    if (msgNum < 1 || msgNum > msgs.size()) {
                        out.println("-ERR No such message");
                    } else {
                        Mailbox.Message m = msgs.get(msgNum - 1);
                        out.println("+OK");
                        out.println("From: "    + m.from);
                        out.println("To: your_username@example.com");
                        out.println("Subject: " + m.subject);
                        out.println(".");
                    }

                } else if (upper.startsWith("RETR ")) {
                    if (!authenticated) { out.println("-ERR Not authenticated"); continue; }
                    int msgNum = Integer.parseInt(line.split(" ")[1]);
                    List<Mailbox.Message> msgs = Mailbox.getActiveMessages();
                    if (msgNum < 1 || msgNum > msgs.size()) {
                        out.println("-ERR No such message");
                    } else {
                        Mailbox.Message m = msgs.get(msgNum - 1);
                        out.println("+OK " + m.size() + " octets");
                        out.print(m.toRaw());
                        out.println(".");
                    }

                } else if (upper.startsWith("DELE ")) {
                    if (!authenticated) { out.println("-ERR Not authenticated"); continue; }
                    int msgNum = Integer.parseInt(line.split(" ")[1]);
                    List<Mailbox.Message> msgs = Mailbox.getActiveMessages();
                    if (msgNum < 1 || msgNum > msgs.size()) {
                        out.println("-ERR No such message");
                    } else {
                        markedForDeletion.add(msgNum);
                        out.println("+OK Message " + msgNum + " marked for deletion");
                    }

                } else if (upper.equals("RSET")) {
                    markedForDeletion.clear();
                    out.println("+OK Deletions reset");

                } else if (upper.equals("NOOP")) {
                    out.println("+OK");

                } else if (upper.equals("QUIT")) {
                    // UPDATE state — commit any DELE marks
                    if (authenticated && !markedForDeletion.isEmpty()) {
                        Mailbox.commitDeletions(markedForDeletion);
                        System.out.println("[POP3] Committed deletions: " + markedForDeletion);
                    }
                    out.println("+OK Bye");
                    break;

                } else {
                    out.println("-ERR Unknown command");
                }
            }
            System.out.println("[POP3] Client disconnected: " + clientAddr);
        } catch (Exception e) {
            System.err.println("[POP3] Error: " + e.getMessage());
        }
    }
}
