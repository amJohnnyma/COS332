import java.io.*;
import java.net.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.*;

public class TelnetServer {
    private static final int PORT = 8001;
    private static final String DB_FILE = "appointments.txt";

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
            return String.format("%s  %-22s  %-30s  (%s)",
                    datetime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                    withWho,
                    description,
                    location);
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
                if (parts.length == 5) {
                    LocalDate date = LocalDate.parse(parts[0]);
                    LocalTime time = LocalTime.parse(parts[1]);
                    
                    Appointment appt = new Appointment(
                        LocalDateTime.of(date, time), 
                        parts[2].trim(), 
                        parts[3].trim(),
                        parts[4].trim()
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
                writer.printf("%s|%s|%s|%s|%s%n",
                    appt.datetime.toLocalDate().toString(),
                    appt.datetime.toLocalTime().toString(),
                    appt.withWho,
                    appt.description,
                    appt.location
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

                out.print("\u001B[2J");           // clear screen
                out.print("\u001B[1;1H");         // top left
                out.println("\u001B[1;34m=== Appointment Book ===\u001B[0m");
                out.println("Type 'help' for commands");
                out.flush();

                String line;
                while ((line = in.readLine()) != null) {
                    // Echo what user typed
                    out.println("\u001B[1;32m> " + line + "\u001B[0m");

                    String cmd = line.trim().toLowerCase();
                    if (cmd.startsWith("quit") || cmd.startsWith("exit")) {
                        saveAppointments();
                        break;
                    } else if (cmd.equals("help")) {
                        showHelp();
                    } else if (cmd.equals("list")) {
                        listAppointments();
                    } else if (cmd.startsWith("add ")) {
                        addAppointment(line.substring(4).trim());
                    } else if (cmd.startsWith("delete ")) {
                        deleteAppointment(line.substring(7).trim());  // strips "delete " → passes "3"
                    } else if (cmd.startsWith("search ")) {
                        searchAppointment(line.substring(7).trim());  // strips "search " → passes "Dr Nel"
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
            out.println("  search \"Person\"");
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

        private void deleteAppointment(String input) {
            // input is expected to be just the number (1-based index)
            input = input.trim();

            if (input.isEmpty()) {
                out.println("\u001B[31mInvalid format. Use:");
                out.println("delete <number>");
                out.println("Example:");
                out.println("delete 3\u001B[0m");
                return;
            }

            try {
                int index = Integer.parseInt(input) - 1; // convert to 0-based

                if (index < 0 || index >= sharedAppointments.size()) {
                    out.println("\u001B[31mNo appointment at position " + (index + 1) + ". "
                            + "There are " + sharedAppointments.size() + " appointment(s).\u001B[0m");
                    return;
                }

                Appointment removed = sharedAppointments.remove(index);
                out.println("\u001B[32mDeleted: " + removed.toString() + "\u001B[0m");
                // saveAppointments();

            } catch (NumberFormatException e) {
                out.println("\u001B[31mInvalid number: \"" + input + "\". Please provide a valid integer.\u001B[0m");
            } catch (Exception e) {
                out.println("\u001B[31mError: " + e.getMessage() + "\u001B[0m");
            }
        }

        private void searchAppointment(String input) {
            // input is the name to search for (partial, case-insensitive)
            input = input.trim();

            if (input.isEmpty()) {
                out.println("\u001B[31mInvalid format. Use:");
                out.println("search <name>");
                out.println("Example:");
                out.println("search Dr Nel\u001B[0m");
                return;
            }

            final String query = input.toLowerCase();

            List<Appointment> results = new ArrayList<>();
            for (int i = 0; i < sharedAppointments.size(); i++) {
                Appointment appt = sharedAppointments.get(i);
                if (appt.withWho.toLowerCase().contains(query)) {
                    results.add(appt);
                }
            }

            if (results.isEmpty()) {
                out.println("\u001B[33mNo appointments found for: \"" + input + "\"\u001B[0m");
                return;
            }

            out.println("\u001B[32mFound " + results.size() + " appointment(s) for \"" + input + "\":\u001B[0m");
            for (int i = 0; i < results.size(); i++) {
                out.println("  " + (i + 1) + ". " + results.get(i).toString());
            }
        }

        private void addAppointment(String input) {
            // ^              start of string
            // (\S+)          date (non-whitespace)
            // \s+            one or more spaces
            // (\S+)          time (non-whitespace)
            // \s+            
            // "([^"]*)"      quoted person (captures inside quotes, allows spaces)
            // \s+            
            // "([^"]*)"      quoted description
            // (?:\s+         non-capturing group for optional location
            //   "([^"]*)"    quoted location
            // )?             optional
            // \s*$           optional trailing whitespace, end of string

            String regex = "^(\\S+)\\s+(\\S+)\\s+\"([^\"]*)\"\\s+\"([^\"]*)\"(?:\\s+\"([^\"]*)\")?\\s*$";

            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(input.trim());

            if (!matcher.matches()) {
                out.println("\u001B[31mInvalid format. Use:");
                out.println("add YYYY-MM-DD HH:MM \"Person name\" \"Description can have spaces\" [\"Location optional\"]\u001B[0m");
                out.println("Example:");
                out.println("add 2025-10-15 14:30 \"Dr Nel\" \"Annual checkup\" \"Room 4B\"\u001B[0m");
                return;
            }

            try {
                String dateStr   = matcher.group(1);
                String timeStr   = matcher.group(2);
                String person    = matcher.group(3).trim();     // inside first quotes
                String desc      = matcher.group(4).trim();     // inside second quotes
                String location  = matcher.group(5) != null 
                    ? matcher.group(5).trim() 
                    : "unspecified";

                if (person.isEmpty() || desc.isEmpty()) {
                    out.println("\u001B[31mPerson and description cannot be empty.\u001B[0m");
                    return;
                }

                LocalDate date = LocalDate.parse(dateStr);
                LocalTime time = LocalTime.parse(timeStr);
                LocalDateTime dt = LocalDateTime.of(date, time);

                Appointment appt = new Appointment(dt, person, desc, location);
                sharedAppointments.add(appt);

                out.println("\u001B[32mAdded: " + appt.toString() + "\u001B[0m");
                saveAppointments();
            } catch (DateTimeParseException e) {
                out.println("\u001B[31mInvalid date or time format. Use YYYY-MM-DD HH:MM\u001B[0m");
            } catch (Exception e) {
                out.println("\u001B[31mError: " + e.getMessage() + "\u001B[0m");
            }
        }
    }
}

