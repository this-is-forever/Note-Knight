package ui;

import swingextended.*;
import application.*;
import cryptography.Crypto;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

/**
 * The main window frame for the application
 */
public class NKMainFrame extends JFrame implements DocumentListener, WindowListener {
    // The name of the application
    public static final String APPLICATION_NAME = "Note Knight";

    // The file to which the application's settings are saved (font and word wrapping)
    private static final File CONFIGURATION_FILE = new File(
            Main.getExecutableDirectory() + "config.properties");
    // The extension the application is associated with
    private static final String EXTENSION = "nkx";
    // The extension with a dot in front of it
    private static final String DOT_EXTENSION = "." + EXTENSION;
    // The default font for the application, in case there isn't one saved in the config
    private static final Font DEFAULT_FONT = new Font("Courier New", Font.PLAIN, 16);
    // Used to create time stamps
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("h:mm a");
    // Used to insert date and time
    private static final SimpleDateFormat DATE_TIME_FORMAT = new SimpleDateFormat("h:mm a M/d/yyyy");
    // File description for the file chooser
    private static final String FILE_DESCRIPTION = APPLICATION_NAME + " Files";
    // Format for the application's title
    private static final String APPLICATION_TITLE_FORMAT = "%s - %s";

    // References the frame's file chooser for the opening and saving process
    private JFileChooser fileChooser;
    // References the password dialog which is used to retrieve a password from the user
    private NKPasswordDialog passwordDialog;
    // References the font chooser which allows the user the change the editor's font
    private NKFontChooser fontChooser;
    // References the Find dialog
    private NKFindDialog findDialog;
    // References the Go To window
    private GoToDialog goToDialog;
    // References the editor's text area
    private NKTextArea textArea;
    // References the application's word wrapping option in the Format menu
    private JCheckBoxMenuItem wordWrapMenuItem;
    // References the menu Undo and Redo menu items under the Edit menu
    private JMenuItem undoItem, redoItem;
    // References the menu bar
    private final JMenuBar menuBar;
    // References the two pieces of text displayed in the status bar
    private JLabel statusText, caretPositionText;

    // References the file open in the editor and the file the user intends to open
    private File documentFile, openingFile;
    // The document's name (Untitled or something.extension)
    private String documentName;
    // A time stamp for the last time the document was saved
    private String lastSaveTimestamp;

    // Stores the configuration information loaded on start and saved on close
    private final Properties configuration;

    // Used to launch new application windows when the user selects New Window
    private final ProcessBuilder processBuilder;

    // Flag which is set when the document is altered by the user
    private boolean changesMade;
    // Flag which is set when the user intends to create a new document (New was clicked)
    private boolean clearAfterSaving;
    // Flag which is set when the program was launched with a file to open from the command line
    private boolean openingFromCommandLine;

    /**
     * Instantiates a new {@link NKMainFrame} object with optional starting file
     * @param openingFile A file the application should attempt to open upon start, or null to start a blank one
     */
    public NKMainFrame(File openingFile) {
        // Remember the file passed to the application by the command line, if one was given
        this.openingFile = openingFile;

        // Prevent the X and Exit buttons from disposing the window or ending the process
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        // Start listening for Window-related events, this way we know when the user hits the X button
        addWindowListener(this);

        // Create a new Properties object that we will use to load and save configuration information
        configuration = new Properties();

        // Determine the path to the user's JRE /bin/java directory
        String separator = System.getProperty("file.separator");
        String classpath = System.getProperty("java.class.path");
        String path = System.getProperty("java.home") + separator + "bin" + separator + "java";
        // Create a process builder which will launch a new instance of the JVM that will load the application
        processBuilder =
                new ProcessBuilder(path, "-cp", classpath, Main.class.getName());

        // Create the menu bar
        menuBar = new JMenuBar();

        // Intialize the cryptographic library used by the application
        initCrypto();
    }

    /**
     * Initializes the frame's components, adds them to the frame and makes the frame visible.
     */
    public void createAndShow() {
        // Windows-ify the window skin
        setLookAndFeel();

        // Create the password dialog that will be displayed when the user attempts to save a new file, chooses
        // Save As, or opens a file
        passwordDialog = new NKPasswordDialog(this);

        // Was a file given as an argument when the process launched?
        if(openingFile != null) {
            // If so, remember that we're opening a file specified earlier
            openingFromCommandLine = true;
            // Prompt the user for the password to the document
            if (promptPassword()) {
                // Attempt to open the document if the user entered a password
                openDocument();
            } else {
                // The user canceled password input; end the program
                System.exit(0);
            }
        }
        // load configuration information from the configuration file
        loadConfiguration();

        // Start with a predefined default font, in case the configuration file is missing (first time launch)
        Font font = DEFAULT_FONT;

        // Pull information that was stored in the configuration file
        String fontFamily = configuration.getProperty("font-family");
        String fontSize = configuration.getProperty("font-size");
        String wordWrap = configuration.getProperty("word-wrap");

        // Was a font family and font size in the file?
        if(fontFamily != null && fontSize != null) {
            // Parse the font size
            int size;
            try {
                size = Integer.parseInt(fontSize);
            } catch(NumberFormatException e) {
                size = -1;
            }
            // If the size is within the valid range for fonts, create a new font
            // The font will be used later by the textArea and the fontChooser
            if(size >= 6 && size <= 72)
                font = new Font(fontFamily, Font.PLAIN, size);
        }

        // Determine whether word wrapping was turned on or off in the previous configuration (off by default)
        boolean wordWrapEnabled = Boolean.parseBoolean(wordWrap);

        // Lay all components added directly to the frame vertically, top to bottom, in the order they are added
        BoxLayout mainLayout = new BoxLayout(getContentPane(), BoxLayout.Y_AXIS);
        setLayout(mainLayout);

        // Variable used as we tweak each menu item
        JMenuItem item;

        // Create the File menu and its menu options
        JMenu fileMenu = new JMenu("File");

        // Create the New menu option, assign its hot key (CTRL + N), and make it call newDocument() when it's clicked
        fileMenu.add(item = new JMenuItem("New"));
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.CTRL_DOWN_MASK));
        item.addActionListener(e -> newDocument());

        fileMenu.add(item = new JMenuItem("New Window"));
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK));
        item.addActionListener(e -> spawnNewInstance());

        fileMenu.add(item = new JMenuItem("Open..."));
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.CTRL_DOWN_MASK));
        item.addActionListener(e -> open());

        fileMenu.add(item = new JMenuItem("Save"));
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK));
        item.addActionListener(e -> save());

        fileMenu.add(item = new JMenuItem("Save As..."));
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK));
        item.addActionListener(e -> saveAs());

        // Add a separator between the last menu option and the next
        fileMenu.addSeparator();

        fileMenu.add(item = new JMenuItem("Exit"));
        item.addActionListener(e -> dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING)));
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F4, KeyEvent.ALT_DOWN_MASK));

        // Add the File menu's JMenu object to the menu bar
        menuBar.add(fileMenu);

        // Create the Edit menu
        JMenu editMenu = new JMenu("Edit");

        editMenu.add(undoItem = new JMenuItem("Undo"));
        undoItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, KeyEvent.CTRL_DOWN_MASK));
        undoItem.addActionListener(e -> textArea.undo());
        undoItem.setEnabled(false);

        editMenu.add(redoItem = new JMenuItem("Redo"));
        redoItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z,
                KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK));
        redoItem.addActionListener(e -> textArea.redo());
        redoItem.setEnabled(false);

        editMenu.addSeparator();

        editMenu.add(item = new JMenuItem("Cut"));
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, KeyEvent.CTRL_DOWN_MASK));
        item.addActionListener(e -> textArea.cut());

        editMenu.add(item = new JMenuItem("Copy"));
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.CTRL_DOWN_MASK));
        item.addActionListener(e -> textArea.copy());

        editMenu.add(item = new JMenuItem("Paste"));
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.CTRL_DOWN_MASK));
        item.addActionListener(e -> textArea.paste());

        editMenu.add(item = new JMenuItem("Delete"));
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
        item.addActionListener(e -> textArea.delete());

        editMenu.addSeparator();

        final JMenuItem searchItem = new JMenuItem();
        editMenu.add(searchItem);
        searchItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, KeyEvent.CTRL_DOWN_MASK));
        searchItem.addActionListener(e -> search());
        // Update the Search Google for.. menu item to show the selected text / disable if no text is selected
        editMenu.addMenuListener(new MenuListener() {
            @Override
            public void menuSelected(MenuEvent e) {
                textArea.updateSearchItem(searchItem);
            }

            @Override
            public void menuDeselected(MenuEvent e) {

            }

            @Override
            public void menuCanceled(MenuEvent e) {

            }
        });

        editMenu.add(item = new JMenuItem("Find / Replace..."));
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.CTRL_DOWN_MASK));
        item.addActionListener(e -> findDialog.setVisible(true));

        editMenu.add(item = new JMenuItem("Find Next"));
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0));
        item.addActionListener(e -> findDialog.find(true));

        editMenu.add(item = new JMenuItem("Find Previous"));
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F3, KeyEvent.SHIFT_DOWN_MASK));
        item.addActionListener(e -> findDialog.find(false));

        editMenu.add(item = new JMenuItem("Go To..."));
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_G, KeyEvent.CTRL_DOWN_MASK));
        item.addActionListener(e -> goTo());

        editMenu.addSeparator();

        editMenu.add(item = new JMenuItem("Select All"));
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, KeyEvent.CTRL_DOWN_MASK));
        item.addActionListener(e -> textArea.selectAll());

        editMenu.add(item = new JMenuItem("Insert Date & Time"));
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));
        item.addActionListener(e -> textArea.replaceSelection(DATE_TIME_FORMAT.format(new Date())));

        menuBar.add(editMenu);

        // Create the Format menu
        JMenu formatMenu = new JMenu("Format");

        formatMenu.add(wordWrapMenuItem = new JCheckBoxMenuItem("Word Wrap"));
        // Set whether the menu item is 'checked' on or off, based on the prior configuration
        wordWrapMenuItem.setState(wordWrapEnabled);
        wordWrapMenuItem.addActionListener(e -> toggleWordWrap());
        menuBar.add(formatMenu);

        formatMenu.add(item = new JMenuItem("Font..."));
        item.addActionListener(e -> fontChooser.setVisible(true));
        menuBar.add(formatMenu);

        // Set the frame's menu bar to the created menu bar
        setJMenuBar(menuBar);

        // Create the main text area and add it to the frame
        textArea = new NKTextArea(this);
        // Set the editor's line wrapping to the previously saved configuration (off by default)
        textArea.setLineWrap(wordWrapEnabled);
        // Don't cut words in half when they wrap
        textArea.setWrapStyleWord(true);
        // Begin listening for document changes so that we know when changes are made to the document
        textArea.getDocument().addDocumentListener(this);
        // Set the editor's font to the font determined earlier
        textArea.setFont(font);

        // Create a scroll pane for the text area that can be used when the document is too large for the frame
        JScrollPane textAreaScroller = new JScrollPane(textArea);
        try {
            // Determine the OS's default border color for text fields (used in the next step)
            LineBorder border = (LineBorder) UIManager.getLookAndFeel().getDefaults().getBorder("TextField.border");
            // Remove all borders on the scroll pane, except for the bottom
            textAreaScroller.setBorder(new MatteBorder(0, 0, 1, 0, border.getLineColor()));
        } catch(ClassCastException e) {
            System.out.println("Unable to set hide JScrollPane borders");
        }
        // Only show scroll bars if they're needed
        textAreaScroller.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        textAreaScroller.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        // Add the scroller to the pane
        add(textAreaScroller);

        // Create a horizontal box that will contain the next row of components
        Box statusBarBox = Box.createHorizontalBox();
        // Create 5px padding on all sides of the row, for aesthetics
        statusBarBox.setBorder(new EmptyBorder(5, 5, 5, 5));

        // Create "status" text that will display in the lower left corner of the editor
        statusText = new JLabel();
        statusBarBox.add(statusText);
        // Add horizontal glue after the status text, causing it to align left
        statusBarBox.add(Box.createHorizontalGlue());

        // Create the caret location text that will be shown in the bottom right corner
        caretPositionText = new JLabel("1:1");
        // Add it to the box after the glue, causing it to align right
        statusBarBox.add(caretPositionText);
        // Add the layer to the frame
        add(statusBarBox);

        // Set up the application with no file
        if(openingFile == null)
            setDocumentFile(null);

        // Set up the file chooser
        fileChooser = new JFileChooser();
        // Only show files with the application's associated extension
        fileChooser.setFileFilter(new FileNameExtensionFilter(FILE_DESCRIPTION, EXTENSION));

        // Set up the font chooser
        fontChooser = new NKFontChooser(this, textArea.getFont());

        findDialog = new NKFindDialog(this);

        goToDialog = new GoToDialog(this);

        // Resize the frame to fit its child components
        pack();
        // Set the size to 800 x 600
        // TO-DO: Save user's preference for window size?
        setSize(800, 600);
        // Move the frame to the OS's usual spot for new windows
        setLocationByPlatform(true);
        // Show the frame
        setVisible(true);
    }

    /**
     * Method called by the {@link NKTextArea}, {@link NKMainFrame#textArea}, when the text area's caret is moved during
     * user input.
     */
    public void caretMoved() {
        String text;

        int selected = textArea.getSelectionLength();
        if(selected == 0) {
            text = textArea.getSelectionStartRow() + ":" + textArea.getSelectionStartColumn();
        } else {
            text = textArea.getSelectionStartRow() + ":" + textArea.getSelectionStartColumn() + " - "
                    + textArea.getSelectionEndRow() + ":" + textArea.getSelectionEndColumn()
                    + " (" + selected + " chars)";
        }

        caretPositionText.setText(text);
    }

    /**
     * Method called by the {@link NKFontChooser}, {@link NKMainFrame#fontChooser}, when the user changes the font
     * size or font family, updating the editor's font.
     * @param f A {@link Font} object with the chosen font's information.
     */
    public void fontSelected(Font f) {
        textArea.setFont(f);
    }

    /**
     * Attempts to launch a Google search for the selected text in the user's default browser
     */
    public void search() {
        if(textArea.isTextSelected() && !textArea.getSelectedText().isBlank())
            GoogleLauncher.launchGoogleSearch(textArea.getSelectedText().strip());
    }

    /**
     * Enables and disables the Undo and Redo options under the Edit menu, based on the document's history
     * @param canUndo Flag set if the document can Undo
     * @param canRedo Flag set if the document can Redo
     */
    public void updateUndoRedo(boolean canUndo, boolean canRedo) {
        undoItem.setEnabled(canUndo);
        redoItem.setEnabled(canRedo);
    }

    /**
     * Method called when text is inserted into the document.
     * @param e Event information.
     */
    @Override
    public void insertUpdate(DocumentEvent e) {
        changesMade();
    }

    /**
     * Method called when text is removed from the document.
     * @param e Event information.
     */
    @Override
    public void removeUpdate(DocumentEvent e) {
        changesMade();
    }

    /**
     * Method called when the document is changed.
     * @param e Event information.
     */
    @Override
    public void changedUpdate(DocumentEvent e) {
        changesMade();
    }

    /**
     * Method called when the user attempts to close the window, either by clicking the OS 'close window' button,
     * or by choosing "Exit" in the File menu.
     * @param e A {@link WindowEvent} object with information about the event.
     */
    @Override
    public void windowClosing(WindowEvent e) {
        // Were changes made to the document prior to closing?
        if(changesMade) {
            // Yes. Does the user want to save?
            int choice = JOptionPane.showConfirmDialog(this,
                    "Do you want to save changes to " + documentName + "?",
                    "Action required",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );
            if(choice == JOptionPane.YES_OPTION) {
                // They do. Attempt to save the file
                if(!save())
                    // Cancel exiting if the user cancels during saving
                    return;
            } else if(choice == JOptionPane.CANCEL_OPTION)
                // The user canceled closing the program. Go back
                return;
        }
        // Either no changes were made, the user saved the document, or the user didn't care about saving changes
        // Save the user's settings
        saveConfiguration();
        // Clean up and exit the program
        passwordDialog.dispose();
        fontChooser.dispose();
        findDialog.dispose();
        goToDialog.dispose();
        dispose();
    }

    @Override
    public void windowOpened(WindowEvent e) {
    }
    @Override
    public void windowClosed(WindowEvent e) {
    }

    @Override
    public void windowIconified(WindowEvent e) {

    }

    @Override
    public void windowDeiconified(WindowEvent e) {

    }

    @Override
    public void windowActivated(WindowEvent e) {

    }

    @Override
    public void windowDeactivated(WindowEvent e) {

    } // Unused event listener methods (required when using Swing's interfacing)

    /**
     * Gets a reference to the main frame's text area
     * @return A {@link NKTextArea} representing the application's text area
     */
    public NKTextArea getTextArea() {
        return textArea;
    }

    /**
     * Changes the document's file and alters the frame's title to reflect the given name.
     * @param f The file that the document will be saved to when Save is clicked.
     */
    private void setDocumentFile(File f) {
        // Remember the file we have open
        documentFile = f;
        // Change the document name to reflect the new file (Untitled if we are working with a new document)
        if(f != null)
            documentName = documentFile.getName();
        else
            documentName = "Untitled";
        // Update the frame's title to reflect the new document's name
        setTitle(String.format(APPLICATION_TITLE_FORMAT, documentName, APPLICATION_NAME));
    }

    /**
     * Method to be called once the document has been saved; removes the "changes made" asterisk and
     * changes this frame's title to reflect the name of the document.
     */
    private void documentSaved() {
        // Reset the changesMade flag; the document was saved
        changesMade = false;
        // Remove the asterisk from the title
        setTitle(String.format(APPLICATION_TITLE_FORMAT, documentName, APPLICATION_NAME));
        // Remember when we saved the document so it can be displayed in the status bar
        lastSaveTimestamp = TIME_FORMAT.format(new Date());
    }

    /**
     * Prompts the user for a password.
     * @return true if the user entered a password and hit OK or the Enter key, or false if the user closed the
     * dialog without entering in a password
     */
    private boolean promptPassword() {
        // Show the dialog and wait for the dialog to close
        // Return true if a password was entered, otherwise false (user canceled)
        return passwordDialog.showAndWait();
    }

    /**
     * Method called when "Open..." option is clicked
     */
    private void open() {
        // Ask if the user wants to save changes (this will open save or save as if they do)
        if(!promptSaveChanges())
            // The user canceled opening at some point in the process; stop
            return;
        // The user either said Yes and saved it, or hit No

        // Forget the previous file, but stay within the directory it was in
        File f = fileChooser.getSelectedFile();
        if(f != null) {
            fileChooser.setSelectedFile(new File(""));
            fileChooser.setCurrentDirectory(f.getParentFile());
        }
        // Forget the old password
        passwordDialog.clear();

        // Prompt the user to choose a file. Only proceed if they hit Open (or enter)
        if(fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            f = fileChooser.getSelectedFile();
            // Show an error message if the selected file doesn't exist
            if(!f.exists()) {
                JOptionPane.showMessageDialog(this, "That file doesn't exist!", "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            // Prompt the user for the password to the file
            // Only proceed if they enter one without canceling
            if(promptPassword()) {
                // Remember the file they selected
                openingFile = fileChooser.getSelectedFile();
                textArea.setEnabled(false);
                enableDisableMenus(false);
                // Begin opening the file on a new thread (so we don't bog down the UI)
                new Thread(this::openDocument).start();
            }
        }
    }

    /**
     * Method called when the Save menu item is clicked; if the user hasn't yet saved the document,
     * {@link NKMainFrame#saveAs()} is called instead, requiring the user to choose a location for the document to be
     * saved and a password to encrypt it with.
     * @return true if saving was successful, or false if the user canceled at some point (during Save as)
     */
    private boolean save() {
        // Are we working with an open file? If not, prompt the user to name the file
        if(documentFile == null)
            return saveAs();
        else {
            // We are, so begin saving it
            // Disable the text area so it isn't edited during saving
            textArea.setEnabled(false);
            // Disable the menu bar so changes can't be made during saving
            enableDisableMenus(false);
            // Let the user know we're saving
            statusText.setText("Saving...");
            // Save the document
            saveDocument();
            return true;
        }
    }

    /**
     * Method called when Save As... menu item is clicked
     * @return true if the user saved the document, false if they canceled
     */
    private boolean saveAs() {
        // Show the file chooser and have the user select a file
        // Only proceed if a file was chosen (Save or enter was pressed)
        if(fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            passwordDialog.clear();
            // Prompt the user for a password for the file. Only proceed if one is entered
            if(promptPassword()) {
                // Remember the document's file location and name, updating the title bar to reflect them
                updateDocumentFile();
                // Save it
                saveDocument();
                return true;
            }
        }
        // The user canceled at some point, return false
        return false;
    }

    /**
     * Saves the document and modifies the UI while doing so
     */
    private void saveDocument() {
        // Should we clear after saving?
        boolean clear = clearAfterSaving;
        // Unset the flag so we don't clear the document next time unless we are supposed to
        clearAfterSaving = false;
        // Update the UI to reflect the fact that we're saving and they need to wait
        statusText.setText("Saving...");
        textArea.setEnabled(false);
        enableDisableMenus(false);
        // Get the most recent password provided by the user
        char[] password = passwordDialog.getPassword();
        // Remember what file we're writing to (documentFile may change before we save otherwise - yay concurrency)
        File target = documentFile;
        // Begin writing to the file on a new thread, so we don't bog down the UI
        new Thread(() -> {
            // Encrypt the data and write it to the file
            writeFile(target, password);
            // Clear the password array to prevent memory leaks
            Crypto.wipeArray(password);
            // Update the UI on the UI thread so the user knows saving was successful
            SwingUtilities.invokeLater(() -> {
                statusText.setText("Save successful");
                // Are we clearing the text? If so, start a new document by clearing the text area and forgetting
                // the previous document's information
                if(clear) {
                    setDocumentFile(null);
                    textArea.setText("");
                }
                documentSaved();
                // Allow user input again
                textArea.setEnabled(true);
                enableDisableMenus(true);
            });
        }).start();
    }

    /**
     * Opens the document and modifies the UI while doing so
     */
    private void openDocument() {
        // Get the last entered password and remember it for decryption
        char[] password = passwordDialog.getPassword();
        // Remember what file we're opening (openFile may change before the new thread touches it)
        File target = openingFile;

        // Start a new thread off the UI thread so we don't bog down the UI as we open and decrypt the file
        new Thread(() -> {
            // Read data from the file and decrypt it, obtaining a String with the info provided
            String plaintext = readFile(target, password);
            // Was decryption successful?
            if(plaintext == null) {
                // No; let them know the password was wrong
                JOptionPane.showMessageDialog(this, "Invalid password!",
                        "Unable to open file", JOptionPane.WARNING_MESSAGE);
                // Were we opening a file passed as a command line argument?
                if(openingFromCommandLine)
                    // Yes; exit!
                    System.exit(0);
                // We weren't; go back
                return;
            }
            // Reset the command line opening flag
            openingFromCommandLine = false;
            // Set the document file to the file that was opened
            setDocumentFile(target);
            // Write over the password array to prevent memory leaks
            Crypto.wipeArray(password);
            // Update the UI so the user knows opening was successful; do so on the UI thread
            SwingUtilities.invokeLater(() -> {
                textArea.setText(plaintext);
                textArea.clearEditHistory();
                textArea.setEnabled(true);
                enableDisableMenus(true);
                statusText.setText("Successfully opened " + documentName);
                documentSaved();
            });
        }).start();
    }

    /**
     * Enables or disables the frame's menus (File, Edit.. etc). Used to enable or disable the menus during
     * opening and saving.
     * @param enabled Flag set if the menus should be enabled, otherwise false
     */
    private void enableDisableMenus(boolean enabled) {
        for(MenuElement m : menuBar.getSubElements())
            if(m instanceof JMenu) {
                JMenu menuItem = (JMenu)m;
                menuItem.setEnabled(enabled);
            }
    }

    /**
     * Method called when the New menu option is clicked
     */
    private void newDocument() {
        // Set a flag so we know to clear the document after saving
        clearAfterSaving = true;
        // Prompt the user to save changes
        promptSaveChanges();
    }

    /**
     * Empties the text area and changes the document's name to Untitled.
     */
    private void clearDocument() {
        // Erase the document's contents
        textArea.setText("");
        // Clear the edit history
        textArea.clearEditHistory();

        // Set the document's file to none
        setDocumentFile(null);
        // Notify the document was "saved" (started fresh)
        documentSaved();
        // Update the status text
        setStatus("New document started");
    }

    /**
     * Reads data from a given file, decrypts it using the given password, and returns the result
     * @param f The file to read from
     * @param password The password to use during decryption
     * @return the resulting {@link String}
     */
    private String readFile(File f, char[] password) {
        return Crypto.decryptStringFromFile(f, password);
    }

    /**
     * Writes the document to a given file, encrypting it with the given password for later decryption
     * @param f The file to write the data to
     * @param password The password to use during encryption
     */
    private void writeFile(File f, char[] password) {
        if(Crypto.encryptStringToFile(f, textArea.getText(), password))
            System.out.println("Save successful");
    }

    /**
     * Method called when the "Word Wrap" checkbox is clicked; changes {@link NKMainFrame#textArea}'s line wrapping to
     * reflect the change, toggling the document's word wrapping.
     */
    private void toggleWordWrap() {
        // Determine whether the Word Wrap menu item is currently 'checked'
        boolean wordWrapping = wordWrapMenuItem.getState();
        // Set the editor's line wrapping to on or off depending on the above
        textArea.setLineWrap(wordWrapping);
    }

    /**
     * Method called after the user chooses a file in the file chooser
     */
    private void updateDocumentFile() {
        // Get the selected file
        File f = fileChooser.getSelectedFile();
        String path = f.getAbsolutePath();
        // Add the application's file extension if the user didn't provide one
        if(!path.endsWith(DOT_EXTENSION))
            f = new File(path + DOT_EXTENSION);
        // Set the document's file to the new file
        setDocumentFile(f);
    }

    /**
     * Method called when the user makes changes to the text area; sets the {@link NKMainFrame#changesMade} flag to true
     * and adds an asterisk next to the document's name in the title bar.
     */
    private void changesMade() {
        // Is this the first time changes were made since the last save?
        if(!changesMade) {
            // Yes; set the flag
            changesMade = true;
            // Add an asterisk in front of the title
            setTitle("*" + getTitle());
            // Update the status text depending on whether we have a new document or an opened one
            if(documentFile != null)
                setStatus("Changes last saved at " + lastSaveTimestamp);
            else
                setStatus("Document has not been saved");
        }
    }

    /**
     * Changes the status text in the lower left corner of the editor
     * @param status Text to display
     */
    private void setStatus(String status) {
        statusText.setText(status);
    }

    /**
     * Creates a new process that launches a new application window
     * Credit to a Stackoverflow user for their code
     */
    private void spawnNewInstance() {
        try {
            // Attempt to create a new process that launches another instance of the application with a blank document
            processBuilder.start();
        } catch (IOException e) {
            System.out.println("Unable to spawn new process.");
        }
    }

    /**
     * Shows the Go To dialog and navigates to the user selection if a selection was chosen
     */
    private void goTo() {
        DocumentLocation location = goToDialog.showAndWait(textArea);
        if(location != null)
            location.navigate(textArea);
    }

    /**
     * Prompts the user, asking if they'd like to save changes to the document before proceeding.
     * @return true if the user saved the document or chose to discard changes, false if canceled at any
     * point during the saving process.
     */
    private boolean promptSaveChanges() {
        // Were changes made?
        if(changesMade) {
            // Yes; Ask them if they want to save
            int choice = JOptionPane.showConfirmDialog(this,
                    "Do you want to save changes to " + documentName + "?",
                    "Action required",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );
            if(choice == JOptionPane.YES_OPTION) {
                // They do; attempt to save
                if(!save())
                    // Saving was canceled; return failure
                    return false;
            } else if(choice == JOptionPane.CANCEL_OPTION)
                // User canceled; return failure
                return false;
            // Are we creating a new document AND did the user choose not to save the document
            if(clearAfterSaving && choice == JOptionPane.NO_OPTION)
                // Yes; clear the document
                clearDocument();
            // Reset the clear after saving flag
            clearAfterSaving = false;
            // Clear the password dialog (we're working with a new document now)
            passwordDialog.clear();
        } else if(clearAfterSaving) {
            // No; clear the document
            clearDocument();
            // Reset the clear after saving flag
            clearAfterSaving = false;
            // Clear the password dialog
            passwordDialog.clear();
        }
        // Either changes weren't made, the user saved the file, or chose No; return success
        return true;
    }

    /**
     * Loads application configuration from the file defined as {@link NKMainFrame#CONFIGURATION_FILE}, storing configuration information
     * in {@link NKMainFrame#configuration}.
     */
    private void loadConfiguration() {
        // Attempt to open and read the user settings from the configuration file
        try(FileInputStream in = new FileInputStream(CONFIGURATION_FILE)) {
            configuration.load(in);
        } catch(FileNotFoundException e) {
            System.out.println("No configuration file found");
        } catch (IOException e) {
            System.out.println("Error while reading ");
        }
    }

    /**
     * Saves application configuration to the file defined as {@link NKMainFrame#CONFIGURATION_FILE} so that it may be loaded
     * the next time the application runs.
     */
    private void saveConfiguration() {
        // Grab the editor's current font
        Font font = textArea.getFont();
        // Remember the font's family and size, along with whether the user had word wrapping turned on
        configuration.put("font-family", font.getName());
        configuration.put("font-size", Integer.toString(font.getSize()));
        configuration.put("word-wrap", Boolean.toString(wordWrapMenuItem.getState()));
        // Attempt to write the user's settings to the configuration file
        try(FileOutputStream out = new FileOutputStream(CONFIGURATION_FILE)) {
            configuration.store(out, String.format("%s Configuration", APPLICATION_NAME));
        } catch (IOException ex) {
            System.out.println("Error: Unable to save configuration");
        }
    }

    // Flag used by initCrypto to ensure initialization only occurs once
    private static boolean cryptoInitialized;

    /**
     * Initializes the cryptographic library used for encrypting data.
     */
    private static void initCrypto() {
        // Only initialize once
        if(cryptoInitialized)
            return;
        cryptoInitialized = true;
        System.out.println("cryptography.Crypto initialized successfully.");
    }

    // Flag used by setLookAndFeel to ensure the look and feel is only loaded once
    private static boolean lookAndFeelSet;

    /**
     * Sets the look and feel to match the user's operating system.
     */
    private static void setLookAndFeel() {
        // Only set the L&F once
        if(lookAndFeelSet)
            return;
        // Attempt to set the look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            lookAndFeelSet = true;
            System.out.println("L&F set successfully.");
        } catch(ClassNotFoundException | IllegalAccessException |
                InstantiationException | UnsupportedLookAndFeelException e) {
            System.out.println("Error: Unable to load system look and feel.");
        }
    }
}
