import java.io.*;
import java.net.*;
import java.util.*;

public class VacationRes{
    private static final String HOST = "localhost";
    private static final int PORT = 110;
    private static final String USER = "johnny";
    private static final String PASS = "bins";

    public static void main(String[] args) {
        try (Socket socket = new Socket(HOST, PORT);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             Scanner scanner = new Scanner(System.in)) {

            in.readLine(); // Greeting
            out.println("USER " + USER); in.readLine();
            out.println("PASS " + PASS); in.readLine();

            out.println("STAT");
            String[] stat = in.readLine().split(" ");
            int totalMsgs = Integer.parseInt(stat[1]);

            System.out.println("\n--- Mailbox Summary ---");
            for (int i = 1; i <= totalMsgs; i++) {
                // Get Subject and From
                out.println("TOP " + i + " 0");
                String line, subject = "", from = "";
                while (!(line = in.readLine()).equals(".")) {
                    if (line.toLowerCase().startsWith("subject: ")) subject = line.substring(9);
                    if (line.toLowerCase().startsWith("from: ")) from = line.substring(6);
                }
                
                // Get Size
                out.println("LIST " + i);
                String size = in.readLine().split(" ")[2];
                
                System.out.printf("[%d] From: %s | Subject: %s | Size: %s bytes\n", i, from, subject, size);
            }

            System.out.print("\nEnter message numbers to DELETE (comma separated, e.g. 1,3,5) or 'none': ");
            String input = scanner.nextLine();

            if (!input.equalsIgnoreCase("none")) {
                String[] toDelete = input.split(",");
                for (String num : toDelete) {
                    out.println("DELE " + num.trim());
                    System.out.println("Marked " + num.trim() + " for deletion: " + in.readLine());
                }
            }

            out.println("QUIT");
            System.out.println("Closing session. Deletions finalized.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
