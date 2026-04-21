import java.io.*;
import java.net.*;
import java.util.*;

public class VacationProgram{
    private static final String POP3_HOST = "localhost"; 
    private static final int POP3_PORT = 110;
    private static final String SMTP_HOST = "localhost";
    private static final int SMTP_PORT = 25;
    
    private static final String USER = "johnny";
    private static final String PASS = "bins";
    
    // To ensure we only inform a person once
    private static Set<String> repliedSenders = new HashSet<>();

    public static void main(String[] args) {
        System.out.println("Vacation Program started. Checking every 30 seconds...");
        while (true) {
            try {
                checkAndReply();
                Thread.sleep(30000); // Wait 30 seconds
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }
        }
    }

    private static void checkAndReply() throws IOException {
        Socket socket = new Socket(POP3_HOST, POP3_PORT);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

        readResponse(in); // Welcome message
        sendCommand(out, in, "USER " + USER);
        sendCommand(out, in, "PASS " + PASS);

        // Get number of messages
        out.println("STAT");
        String stat = in.readLine();
        int count = Integer.parseInt(stat.split(" ")[1]);

        for (int i = 1; i <= count; i++) {
            // Get headers only (TOP msg_num 0 lines of body)
            out.println("TOP " + i + " 0");
            String line;
            String sender = "";
            boolean isPrac7 = false;

            while (!(line = in.readLine()).equals(".")) {
                if (line.toLowerCase().startsWith("from: ")) sender = line.substring(6).trim();
                if (line.toLowerCase().startsWith("subject: ") && line.toLowerCase().contains("prac7")) isPrac7 = true;
            }

            if (isPrac7 && !sender.isEmpty()&& !repliedSenders.contains(sender)) {
                sendAutoReply(sender);
                repliedSenders.add(sender);
            }
        }
        sendCommand(out, in, "QUIT");
        socket.close();
    }

    private static void sendAutoReply(String recipient) throws IOException {
        Socket socket = new Socket(SMTP_HOST, SMTP_PORT);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

        readResponse(in);
        sendCommand(out, in, "HELO " + SMTP_HOST);
        sendCommand(out, in, "MAIL FROM:<" + USER + "@example.com>");
        sendCommand(out, in, "RCPT TO:<" + recipient + ">");
        sendCommand(out, in, "DATA");
        
        out.println("Subject: Out of Office");
        out.println("To: " + recipient);
        out.println();
        out.println("I am currently on vacation and will reply to your message 'prac7' when I return.");
        out.println(".");
        readResponse(in);
        
        sendCommand(out, in, "QUIT");
        socket.close();
        System.out.println("Replied to: " + recipient);
    }

    private static void sendCommand(PrintWriter out, BufferedReader in, String cmd) throws IOException {
        out.println(cmd);
        readResponse(in);
    }

    private static void readResponse(BufferedReader in) throws IOException {
        String res = in.readLine();
        if (res == null || res.startsWith("-ERR") || res.startsWith("5")) {
            throw new IOException("Server Error: " + res);
        }
    }
}
