import java.io.*;
import java.net.*;
import java.time.*;
import java.util.*;

public class BirthdayReminder {
    private static final String SMTP_SERVER = "localhost";
    private static final int SMTP_PORT = 1025;

    public static void main(String[] args) {
        String fileName = "events.txt";
        List<String> upcomingEvents = new ArrayList<>();
        
        LocalDate targetDate = LocalDate.now().plusDays(6);
        int targetDay = targetDate.getDayOfMonth();
        int targetMonth = targetDate.getMonthValue();

        try (Scanner fileScanner = new Scanner(new File(fileName))) {
            while (fileScanner.hasNextLine()) {
                String line = fileScanner.nextLine().trim();
                if (line.isEmpty()) continue;

                String[] parts = line.split("\\s+", 2);
                if (parts.length < 2) continue;

                String[] dateParts = parts[0].split("/");
                int d = Integer.parseInt(dateParts[0]);
                int m = Integer.parseInt(dateParts[1]);

                if (d == targetDay && m == targetMonth) {
                    upcomingEvents.add(parts[1]);
                }
            }
        } catch (FileNotFoundException e) {
            System.err.println("File not found: " + fileName);
            return;
        }

        if (!upcomingEvents.isEmpty()) {
            sendSmtpEmail(upcomingEvents, targetDate);
        } else {
            System.out.println("No events 6 days from now (" + targetDate + "). Terminating.");
        }
    }

    private static void sendSmtpEmail(List<String> events, LocalDate date) {
        try (Socket socket = new Socket(SMTP_SERVER, SMTP_PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), false);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            readResponse(in); 
            
            sendCommand(out, in, "HELO localhost");
            sendCommand(out, in, "MAIL FROM:<student@up.ac.za>");
            sendCommand(out, in, "RCPT TO:<user@localhost>");
            sendCommand(out, in, "DATA");

            // RFC 5322 Headers
            out.print("From: COS332 Reminder <student@up.ac.za>\r\n");
            out.print("To: Student <user@localhost>\r\n");
            out.print("Subject: Reminder: Events on " + date + "\r\n");
            out.print("\r\n"); 

            out.print("Events happening in 6 days:\r\n");
            for (String event : events) {
                out.print("- " + event + "\r\n");
            }
            
            out.print(".\r\n"); 
            out.flush();
            readResponse(in);

            sendCommand(out, in, "QUIT");

        } catch (IOException e) {
            System.err.println("SMTP Error: " + e.getMessage());
        }
    }

    private static void sendCommand(PrintWriter out, BufferedReader in, String cmd) throws IOException {
        out.print(cmd + "\r\n");
        out.flush();
        System.out.println("CLIENT: " + cmd);
        readResponse(in);
    }

    private static void readResponse(BufferedReader in) throws IOException {
        String line = in.readLine();
        if (line != null) System.out.println("SERVER: " + line);
    }
}