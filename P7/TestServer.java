public class TestServer {

    /**
     * Application entry point.
     * Launches POP3 and SMTP servers on separate daemon threads.
     *
     */
    public static void main(String[] args) {
        System.out.println("=== Test Mail Server Starting ===");
        System.out.println("User: johnny | Pass: bins");
        System.out.println("Pre-loaded 4 test messages in mailbox.");
        System.out.println("=================================\n");

        // Start POP3 server on port 110
        new Thread(() -> {
            try {
                new Pop3Server().start();
            } catch (Exception e) {
                System.err.println("[POP3] Failed to start: " + e.getMessage());
                System.err.println("Tip: Port 110 needs root. Use sudo or change PORT in Pop3Server.java to 1100.");
            }
        }).start();

        // Start SMTP server on port 25
        new Thread(() -> {
            try {
                new SmtpServer().start();
            } catch (Exception e) {
                System.err.println("[SMTP] Failed to start: " + e.getMessage());
                System.err.println("Tip: Port 25 needs root. Use sudo or change PORT in SmtpServer.java to 2525.");
            }
        }).start();
    }
}
