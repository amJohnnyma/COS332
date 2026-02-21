import java.util.Random; 
import java.util.Map; 

public class Prac_1_Home {
    public static void main(String[] args) {
        Random randomGenerator = new Random(); 
        int randomOne = randomGenerator.nextInt(100); 
        int randomTwo = randomGenerator.nextInt(100); 
        
        System.out.println("<!DOCTYPE html><html><head>");
                
                
        System.out.println("<title>Prac 1 Home</title>");
        System.out.println("<meta http-equiv=\"Cache-Control\" content=\"no-cache, no-store, must-revalidate\">");
        System.out.println("<meta http-equiv=\"Pragma\" content=\"no-cache\">");
        System.out.println("<meta http-equiv=\"Expires\" content=\"0\">");
        
        System.out.println("</head><body>");

        // Infinite loop. Cannot exit since exit is a client side operation
        /*
         * This assignment deals with server-side processing. Using
         * client-side computation (for example, using JavaScript) would
         * lead to a mark of 0 for this assignment (and will be heavily
         * penalised in any other assignment in this module).
        */
        while (randomOne == randomTwo)
        {
            randomOne = randomGenerator.nextInt(100); 
        }

        if (randomOne > randomTwo) {
               System.out.println(" <H1> Choose one </H1>");
               System.out.println(" <div style='background-color:orange; padding:10px;width:30%'><a href=\"/cgi-bin/Prac_1_Right.cgi\">"+ randomOne +"</a></div>");
               System.out.println(" <div style='background-color:orange; padding:10px;width:30%'><a href=\"/cgi-bin/Prac_1_Wrong.cgi\">"+ randomTwo +"</a></div>");
        }
        else {
               System.out.println(" <H1> Choose one </H1>");
               System.out.println(" <div style='background-color:orange; padding:10px;width:30%'><a href=\"/cgi-bin/Prac_1_Wrong.cgi\">"+ randomOne +"</a></div>");
               System.out.println(" <div style='background-color:orange; padding:10px;width:30%'><a href=\"/cgi-bin/Prac_1_Right.cgi\">"+ randomTwo +"</a></div>");
        }

        
        System.out.println("</body></html>");
    }
}
