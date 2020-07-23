package application;

import ui.NKMainFrame;

import javax.swing.*;
import java.io.File;

/**
 * The application's entry point
 */
public class Main {

    /**
     * <p>Application entry point. The first argument passed will be assumed to be a file the user wants to open
     * and edit, prompting the user for the password to the document. All other arguments are ignored.</p>
     * <p>If no arguments are passed, the application starts with a fresh document.</p>
     * @param args Arguments passed from the command line, provided automatically by the system
     */
    public static void main(String[] args) {
        File file = null;
        // Was at least one argument provided by the system?
        if(args.length >= 1) {
            // Yes; grab the argument and create a File object with it
            file = new File(args[0]);
            // Does the file exist? If not, throw an error
            assert file.exists() : "Error: File does not exist!";
        }
        System.out.println(getExecutableDirectory());
        // Create a new application frame
        NKMainFrame frame = new NKMainFrame(file);
        // Create and show the UI on the Swing UI thread
        SwingUtilities.invokeLater(frame::createAndShow);
    }

    /**
     * Gets the root directory of the currently running program, for the sake of loading configuration
     * @return a {@link String} representing the directory of the program
     */
    public static String getExecutableDirectory() {
        String path = Main.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        int lastIndex = path.lastIndexOf("/");
        return path.substring(0, lastIndex+1);
    }

}
