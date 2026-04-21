import java.io.*;
import java.net.*;

public class SmtpServer {

    /** Port the SMTP server listens on. Change to 2525+ to avoid needing sudo. */
    private static final int PORT = 25;

    /**
     * Starts the SMTP server and accepts incoming client connections.
     * Each client is handled in its own thread.
     *
     */
    public void start() throws IOException {
        ServerSocket server = new ServerSocket(PORT);
        System.out.println("[SMTP] Listening on port " + PORT);
        while (true) {
            Socket client = server.accept();
            new Thread(() -> handleClient(client)).start();
        }
    }

    /**
     * Handles a single SMTP client session from greeting to QUIT.
     * Parses the SMTP envelope and message DATA, then stores the result in {@link Mailbox}.
     *
     */
    private void handleClient(Socket socket) {
        try (
            BufferedReader in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter    out = new PrintWriter(socket.getOutputStream(), true)
        ) {
            String clientAddr = socket.getInetAddress().getHostAddress();
            System.out.println("[SMTP] Client connected: " + clientAddr);

            out.println("220 localhost Test SMTP Server Ready");

            String        from    = "";
            String        to      = "";
            StringBuilder body    = new StringBuilder();
            String        subject = "";
            boolean       inData  = false;

            String line;
            while ((line = in.readLine()) != null) {

                // DATA mode — accumulate body lines until lone "."
                if (inData) {
                    if (line.equals(".")) {
                        // End of DATA — store message in shared mailbox
                        Mailbox.addMessage(new Mailbox.Message(from, subject, body.toString()));
                        System.out.println("[SMTP] Stored | From: " + from + " To: " + to + " Subject: " + subject);
                        out.println("250 OK Message accepted");
                        inData  = false;
                        body    = new StringBuilder();
                        subject = "";
                    } else {
                        // Parse Subject header if present in DATA block
                        if (line.toLowerCase().startsWith("subject: ")) {
                            subject = line.substring(9).trim();
                        }
                        body.append(line).append("\n");
                    }
                    continue;
                }

                System.out.println("[SMTP] << " + line);
                String upper = line.toUpperCase();

                if (upper.startsWith("HELO") || upper.startsWith("EHLO")) {
                    out.println("250 Hello");

                } else if (upper.startsWith("MAIL FROM:")) {
                    from = extractAddress(line);
                    out.println("250 OK");

                } else if (upper.startsWith("RCPT TO:")) {
                    to = extractAddress(line);
                    out.println("250 OK");

                } else if (upper.equals("DATA")) {
                    out.println("354 Start input, end with <CRLF>.<CRLF>");
                    inData = true;

                } else if (upper.equals("RSET")) {
                    from = ""; to = ""; body = new StringBuilder(); subject = "";
                    out.println("250 OK Reset");

                } else if (upper.equals("NOOP")) {
                    out.println("250 OK");

                } else if (upper.equals("QUIT")) {
                    out.println("221 Bye");
                    break;

                } else {
                    out.println("500 Unknown command");
                }
            }
            System.out.println("[SMTP] Client disconnected: " + clientAddr);
        } catch (Exception e) {
            System.err.println("[SMTP] Error: " + e.getMessage());
        }
    }

    /**
     * Extracts the email address from a MAIL FROM or RCPT TO line.
     * Handles both angle-bracket format ({@code <addr>}) and plain format.
     *
     * Examples:
     *   "MAIL FROM:<alice@example.com>" → "alice@example.com"
     *   "RCPT TO: bob@example.com"      → "bob@example.com"
     *
     */
    private String extractAddress(String line) {
        int start = line.indexOf('<');
        int end   = line.indexOf('>');
        if (start != -1 && end != -1) return line.substring(start + 1, end);
        return line.split(":")[1].trim();
    }
}
