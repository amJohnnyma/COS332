import java.io.*;
import java.net.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.nio.file.Files;

public class TelnetServer {
    private static final int PORT = 8001;
    private static final String DB_FILE = "appointments.txt";

    // In real assignment: decide if appointments are shared or per-user
    private static List<Appointment> sharedAppointments = new ArrayList<>();

    static class Appointment {
        LocalDateTime datetime;
        String withWho;
        String description;
        String location;

        Appointment(LocalDateTime dt, String who, String desc, String loc) {
            this.datetime = dt;
            this.withWho = who;
            this.description = desc;
            this.location = loc;
        }

        @Override
        public String toString() {
            return String.format("%s  %-20s  %s  (%s)",
                datetime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                withWho, description, location);
        }

        // Add these getter methods to resolve the "undefined" errors
        public String getDate() { 
            return datetime.toLocalDate().toString(); // Returns YYYY-MM-DD
        }
        
        public String getTime() { 
            return datetime.toLocalTime().toString(); // Returns HH:MM
        }
        
        public String getWho() { 
            return withWho; 
        }
        
        public String getLocation() { 
            return location; 
        }
    }

    

    public static void main(String[] args) throws IOException {
        loadAppointments();

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Appointment server listening on port " + PORT);

            while (true) {
                Socket client = serverSocket.accept();
                new Thread(new ClientHandler(client)).start();
            }
        }
    }

    public static synchronized void loadAppointments() {
        sharedAppointments.clear(); 
        File file = new File(DB_FILE);
        if (!file.exists()) return;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\|");
                if (parts.length == 4) {
                    // Reconstruct LocalDateTime from the stored Date and Time strings
                    LocalDate date = LocalDate.parse(parts[0]);
                    LocalTime time = LocalTime.parse(parts[1]);
                    
                    Appointment appt = new Appointment(
                        LocalDateTime.of(date, time), 
                        parts[2].trim(), 
                        "Description", // Adjust if you add a 5th column for desc
                        parts[3].trim()
                    );
                    sharedAppointments.add(appt);
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading: " + e.getMessage());
        }
    }

    public static synchronized void saveAppointments() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(DB_FILE))) {
            for (Appointment appt : sharedAppointments) {
                // Now matches the parts.length == 4 logic in load
                writer.printf("%s|%s|%s|%s%n",
                    appt.getDate(),
                    appt.getTime(),
                    appt.getWho(),
                    appt.getLocation()
                );
            }
            writer.flush(); 
        } catch (IOException e) {
            System.err.println("Save error: " + e.getMessage());
        }
    }

    static class ClientHandler implements Runnable {
        private final Socket socket;
        private PrintWriter out;
        private BufferedReader in;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                out = new PrintWriter(socket.getOutputStream(), true);
                in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                // Very simple ANSI welcome
                out.print("\u001B[2J");           // clear screen
                out.print("\u001B[1;1H");         // top left
                out.println("\u001B[1;34m=== Appointment Book ===\u001B[0m");
                out.println("Type 'help' for commands");
                out.flush();

                String line;
                while ((line = in.readLine()) != null) {
                    // Echo what user typed (very important for this assignment!)
                    out.println("\u001B[1;32m> " + line + "\u001B[0m");

                    String cmd = line.trim().toLowerCase();
                    if (cmd.startsWith("quit") || cmd.startsWith("exit")) {
                        break;
                    } else if (cmd.equals("help")) {
                        showHelp();
                    } else if (cmd.equals("list")) {
                        listAppointments();
                    } else if (cmd.startsWith("add ")) {
                        addAppointment(line.substring(4).trim());
                    } else if (cmd.startsWith("delete ")) {
                        // parse number etc.
                    } else {
                        out.println("Unknown command. Type 'help'");
                    }
                    out.flush();
                }
            } catch (Exception e) {
                // silent fail on client disconnect
            } finally {
                try { socket.close(); } catch (Exception ignored) {}
            }
        }

        private void showHelp() {
            out.println("\u001B[33mCommands:");
            out.println("  list                - show all appointments");
            out.println("  add YYYY-MM-DD HH:MM \"Person\" \"What\" \"Where\"");
            out.println("  delete <number>");
            out.println("  quit / exit");
            out.println("\u001B[0m");
        }

        private void listAppointments() {
            if (sharedAppointments.isEmpty()) {
                out.println("\u001B[90mNo appointments yet.\u001B[0m");
                return;
            }
            int i = 1;
            for (Appointment a : sharedAppointments) {
                out.printf("\u001B[36m%2d)\u001B[0m %s\n", i++, a);
            }
        }

        private void addAppointment(String args) {
            // Very naive parsing — improve for real submission
            // Expected: 2026-03-15 14:30 "Dr Smith" "Follow-up" "Room 12"
            out.println("\u001B[90m(add not fully implemented yet)\u001B[0m");
        }
    }
}
