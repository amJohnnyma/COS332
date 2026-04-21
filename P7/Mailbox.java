import java.util.*;

public class Mailbox {

    /**
     * Message
     *
     * Represents a single email message in the mailbox.
     * Stores sender, subject, body, and deletion flag.
     */
    public static class Message {

        /** Email address of the sender (From: header). */
        public String from;

        /** Subject line of the message. */
        public String subject;

        /** Plain-text body of the message. */
        public String body;

        /**
         * Whether this message has been marked for deletion via DELE.
         * Deletion is only committed when QUIT is issued (POP3 spec).
         */
        public boolean deleted = false;

        /**
         * Constructs a new Message.
         *
         */
        public Message(String from, String subject, String body) {
            this.from = from;
            this.subject = subject;
            this.body = body;
        }

        /**
         * Returns the full RFC-822-style raw message string.
         * Used by POP3 RETR command to send the complete message.
         *
         */
        public String toRaw() {
            return "From: " + from + "\r\n" +
                   "To: your_username@example.com\r\n" +
                   "Subject: " + subject + "\r\n" +
                   "\r\n" +
                   body + "\r\n";
        }

        /**
         * Returns the byte size of the raw message.
         * Used by POP3 LIST and STAT commands.
         *
         */
        public int size() {
            return toRaw().length();
        }
    }

    /** Backing store for all messages (including deleted ones until QUIT). */
    private static final List<Message> messages = new ArrayList<>();

    static {
        // Pre-loaded test messages — 2 with subject "prac7" to trigger VacationProgram
        messages.add(new Message("alice@example.com", "prac7",      "Hey, are you there?"));
        messages.add(new Message("bob@example.com",   "Hello",      "Just saying hi."));
        messages.add(new Message("carol@example.com", "prac7",      "Need urgent help!"));
        messages.add(new Message("alice@example.com", "Re: prac7",  "Following up again."));
    }

    /**
     * Returns the full internal message list including deleted messages.
     *
     */
    public static synchronized List<Message> getMessages() {
        return messages;
    }

    /**
     * Adds a new message to the mailbox.
     * Called by the SMTP server when a message is fully received (after DATA + ".").
     *
     */
    public static synchronized void addMessage(Message m) {
        messages.add(m);
        System.out.println("[MAILBOX] New message from " + m.from + " | Subject: " + m.subject);
    }

    /**
     * Returns only non-deleted messages — the POP3 view of the mailbox.
     * Message indices here correspond to POP3 message numbers (1-based).
     *
     */
    public static synchronized List<Message> getActiveMessages() {
        List<Message> active = new ArrayList<>();
        for (Message m : messages) {
            if (!m.deleted) active.add(m);
        }
        return active;
    }

    /**
     * Commits pending deletions from a POP3 session.
     * Called when a POP3 client issues QUIT after marking messages with DELE.
     * Indices are 1-based, relative to the active (non-deleted) message list.
     *
     */
    public static synchronized void commitDeletions(Set<Integer> toDelete) {
        List<Message> active = getActiveMessages();
        for (int idx : toDelete) {
            if (idx >= 1 && idx <= active.size()) {
                active.get(idx - 1).deleted = true;
            }
        }
    }
}
