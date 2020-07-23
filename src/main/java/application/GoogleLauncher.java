package application;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Used to launch a Google search using the user's default browser
 */
public class GoogleLauncher {

    /**
     * Opens a new Google search with the given search query. The user's OS will handle opening the URL.
     * @param query The non-encoded text to search for
     */
    public static void launchGoogleSearch(String query) {
        try {
            URI uri = new URI("https://www.google.com/search?q=" + URLEncoder.encode(query, StandardCharsets.UTF_8));
            Desktop.getDesktop().browse(uri);
        } catch (URISyntaxException | IOException e) {
            e.printStackTrace();
        }

    }

}
