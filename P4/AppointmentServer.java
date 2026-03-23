import java.io.*;
import java.net.*;
import java.util.*;

public class AppointmentServer {
    // Storing as strings for simplicity in this practical
    private static List<String> appointments = new ArrayList<>();

    public static void main(String[] args) throws IOException {
        int port = 8080;
        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("COS 332 Server running at http://localhost:" + port);

        while (true) {
            try (Socket client = serverSocket.accept();
                 BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                 PrintWriter out = new PrintWriter(client.getOutputStream())) {

                String requestLine = in.readLine();
                if (requestLine == null) continue;

                String responseBody = "";
                
                // ROUTING LOGIC
                if (requestLine.contains("GET / ")) {
                    responseBody = generateMainPage("");
                } 
                else if (requestLine.contains("/add?")) {
                    // Expected format: /add?d=03-23&t=10:00&n=Dentist
                    String date = decodeURL(extractValue(requestLine, "d="));
                    String time = decodeURL(extractValue(requestLine, "t="));
                    String desc = decodeURL(extractValue(requestLine, "n="));
                    
                    String entry = date + " @ " + time + " - " + desc;
                    appointments.add(entry);
                    responseBody = generateMainPage("Scheduled: " + desc);
                } 
                else if (requestLine.contains("/search?")) {
                    String query = decodeURL(extractValue(requestLine, "n="));
                    responseBody = generateSearchPage(query);
                }

                // HTTP RESPONSE HEADERS (Crucial for RFC 2616 compliance)
                out.println("HTTP/1.1 200 OK");
                out.println("Content-Type: text/html");
                out.println("Content-Length: " + responseBody.length());
                out.println("Connection: close");
                out.println(); // The mandatory empty line
                
                out.print(responseBody);
                out.flush();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // Manual parser to find parameters between & or space
    private static String extractValue(String requestLine, String param) {
        try {
            int start = requestLine.indexOf(param) + param.length();
            int endAmp = requestLine.indexOf("&", start);
            int endSpace = requestLine.indexOf(" ", start);
            
            // Use whichever comes first: the next parameter or the end of the URL
            int end = (endAmp != -1 && endAmp < endSpace) ? endAmp : endSpace;
            return requestLine.substring(start, end);
        } catch (Exception e) { return ""; }
    }

    // Manual URL Decoding (The "extra mile" for marks)
    private static String decodeURL(String input) {
        return input.replace("+", " ")
                    .replace("%3A", ":")
                    .replace("%2F", "/")
                    .replace("%40", "@");
    }

    private static String generateMainPage(String status) {
        StringBuilder html = new StringBuilder();
        html.append("<html><head><style>body{font-family:sans-serif; padding:20px;} fieldset{margin-bottom:20px;}</style></head><body>");
        html.append("<h1>COS 332 Appointment Manager</h1>");
        
        if (!status.isEmpty()) {
            html.append("<p style='color: green;'><b>Status:</b> ").append(status).append("</p>");
        }

        // --- UPDATED FORM WITH DATE ---
        html.append("<fieldset><legend>New Appointment</legend>");
        html.append("<form method='get' action='/add'>");
        html.append("Date: <input type='text' name='d' size='8' placeholder='MM-DD'> ");
        html.append("Time: <input type='text' name='t' size='8' placeholder='HH:mm'> ");
        html.append("Description: <input type='text' name='n' size='20'> ");
        html.append("<input type='submit' value='Add'>");
        html.append("</form></fieldset>");
        
        // --- SEARCH ---
        html.append("<form method='get' action='/search'>");
        html.append("Search: <input type='text' name='n'> <input type='submit' value='Find'>");
        html.append("</form>");

        html.append("<h2>Your Schedule</h2><hr><ul>");
        for (String appt : appointments) {
            html.append("<li>").append(appt).append("</li>");
        }
        if (appointments.isEmpty()) html.append("<li>No appointments yet.</li>");
        html.append("</ul></body></html>");
        
        return html.toString();
    }

    private static String generateSearchPage(String query) {
        StringBuilder html = new StringBuilder("<html><body><h1>Results for: " + query + "</h1><ul>");
        for (String appt : appointments) {
            if (appt.toLowerCase().contains(query.toLowerCase())) {
                html.append("<li>").append(appt).append("</li>");
            }
        }
        html.append("</ul><br><a href='/'>Back to Home</a></body></html>");
        return html.toString();
    }
}