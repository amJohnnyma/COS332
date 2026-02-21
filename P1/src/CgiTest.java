import java.util.Map;

public class CgiTest {
    public static void main(String[] args) {
        // Step 1: Print the mandatory CGI Header
        // This tells the browser to expect HTML content
        System.out.println(); // Blank line signals end of headers

        // Step 2: Print the HTML Body
        System.out.println("<!DOCTYPE html><html><head><title>Java CGI Test</title></head><body>");
        System.out.println("<h1>Java CGI is Working!</h1>");
        
        System.out.println("<h3>Standard CGI Environment Variables:</h3>");
        System.out.println("<table border='1'>");
        
        // Step 3: Access and display environment variables passed by Apache
        Map<String, String> env = System.getenv();
        for (String key : new String[]{"REQUEST_METHOD", "QUERY_STRING", "REMOTE_ADDR", "SERVER_SOFTWARE", "SCRIPT_NAME"}) {
            String value = env.getOrDefault(key, "Not Set");
            System.out.println("<tr><td><b>" + key + "</b></td><td>" + value + "</td></tr>");
        }
        
        System.out.println("</table>");
        System.out.println("</body></html>");
    }
}
