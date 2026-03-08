import java.net.*;
import java.io.*;
import java.time.*;
import java.time.format.*;
import java.util.*;

public class WorldClockServer 
{
    public static void main(String[] args) throws IOException
    {
        // port from the specs
        ServerSocket serverSocket = new ServerSocket(55555);
        System.out.println("Server running on port 55555...");

        while (true)
        {
            Socket client = serverSocket.accept();
            new Thread( () -> handleClient(client)).start();
        }
    }

    static void handleClient(Socket client)
    {
        try
        {
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(client.getInputStream())
                    );
            OutputStream out = client.getOutputStream();

            // read the first line GET /South Africa/1.1
            String requestLine = in.readLine();

            String line;
            while(!(line = in.readLine()).isEmpty())
            {
                // consume headers
            }

            // extract the path
            String[] parts = requestLine.split(" ");
            String method = parts[0]; // GET or HEAD
            String path = parts[1]; // /London tokyo etc.

            // handle this to avoid errors
            if (path.equals("/favicon.ico")) {
                PrintWriter pw = new PrintWriter(out);
                pw.print("HTTP/1.1 404 Not Found\r\n");
                pw.print("Content-Length: 0\r\n");
                pw.print("\r\n");
                pw.flush();
                return;
            }

            // build and send response
            String html = buildPage(path);
            PrintWriter pw = new PrintWriter(out);
            pw.print("HTTP/1.1 200 OK\r\n");
            pw.print("Content-Type: text/html\r\n");
            pw.print("Content-Length: " + html.getBytes().length + "\r\n");
            pw.print("\r\n");

            if(method.equals("GET"))
            {
                pw.print(html);
            }
            pw.flush();

        }
        catch(IOException e)
        {
            System.out.println("Error: " + e.getMessage());
        }
        finally
        {
            try { client.close(); } catch(IOException e) {}
        }
    }

    static Map<String, String> getCities()
    {
        Map<String, String> cities = new LinkedHashMap<>();
        cities.put("London",    "Europe/London");
        cities.put("New_York",  "America/New_York");
        cities.put("Tokyo",     "Asia/Tokyo");
        cities.put("Sydney",    "Australia/Sydney");
        cities.put("Dubai",     "Asia/Dubai");
        cities.put("Nairobi",   "Africa/Nairobi");

        return cities;
    }

    static String buildPage(String path)
    {
        // strip the leading /
        String strippedPath= path.substring(1); // "" or "London" or "favicon.ico"
        
        Map<String, String> cities = getCities();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm:ss");
        String saTime = ZonedDateTime.now(ZoneId.of("Africa/Johannesburg")).format(fmt);

        List<String> selected = new ArrayList<>();
        String toRemove = null;

        // remove selected
        if(strippedPath.contains("/remove/"))
        {
            String[] splitRemove = strippedPath.split("/remove/");
            toRemove = splitRemove[1];
            if(!splitRemove[0].isEmpty())
            {
                selected.addAll(Arrays.asList(splitRemove[0].split(",")));
            }
            selected.remove(toRemove);
        }
        else if (!strippedPath.isEmpty())
        {
            selected.addAll(Arrays.asList(strippedPath.split(",")));
        }
        // build current selected list as comma separated string for URLs
        String selectedStr = String.join(",", selected);

        StringBuilder html = new StringBuilder();
        html.append("<html><head>");
        html.append("<meta http-equiv='REFRESH' content='1; url=/").append(selectedStr).append("'>");
        html.append("</head><body>");

        // Always show SA time
        html.append("<h2>South Africa (Johannesburg): ").append(saTime).append("</h2>");
        html.append("<hr>");

        // Show all selected cities with their times + a remove link
        if (selected.isEmpty()) {
            html.append("<p><i>No cities selected. Click a city below to add it.</i></p>");
        } else {
            for (String city : selected) {
                if (cities.containsKey(city)) {
                    String cityTime = ZonedDateTime.now(ZoneId.of(cities.get(city))).format(fmt);
                    html.append("<h3>")
                        .append(city).append(": ").append(cityTime)
                        .append(" &nbsp; <a href='/") // no break space to ensure a space between names
                        .append(selectedStr).append("/remove/").append(city)
                        .append("'>[Remove]</a>")
                        .append("</h3>");
                }
            }
        }

        html.append("<hr><p><b>Add a city:</b><br>");

        // Show clickable city links (only cities not already selected)
        for (String c : cities.keySet()) {
            if (!selected.contains(c)) {
                // Add city to selected list
                String newSelected = selectedStr.isEmpty() ? c : selectedStr + "," + c;
                html.append("<a href='/").append(newSelected).append("'>").append(c).append("</a> &nbsp; ");
            }
        }

        html.append("</p>");
        html.append("<br><a href='/'>Home (clear all)</a>");
        html.append("</body></html>");

        return html.toString();

    }
}
