package ui;

import swingextended.AlignedLayer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;

public class NKPasswordDialog extends JDialog implements ActionListener, WindowFocusListener {
    // A reference to the text box in which the user will enter a password
    private JPasswordField passwordField;
    // References the application's main window
    private final JFrame owner;
    // References to the dialog's buttons
    private JButton okButton, cancelButton;
    // A flag set when the user hits Ok or presses Enter
    private boolean submitted;

    /**
     * Creates a new {@link NKPasswordDialog} object
     * @param owner A reference to the dialog's owner
     */
    public NKPasswordDialog(JFrame owner) {
        super(owner);
        this.owner = owner;
        // Ensure the dialog maintains focus while open
        setModal(true);
        setModalityType(ModalityType.APPLICATION_MODAL);
        // Change the dialog's title
        setTitle("Password required");
        // Prevent the user from resizing the dialog
        setResizable(false);
        // Begin listening for when the window gains focus
        addWindowFocusListener(this);
        create();
    }

    /**
     * Clears the text in the password field
     */
    public void clear() {
        passwordField.setText("");
    }

    /**
     * Displays the password dialog and waits until the user enters the information or cancels
     * @return true if the user entered a password, otherwise false (the user canceled)
     */
    public boolean showAndWait() {
        // Reset the submitted flag
        submitted = false;
        // Ensure the password field has focused
        passwordField.requestFocus();
        // Move the dialog so it's centered over the parent frame
        setLocationRelativeTo(getParent());
        // Show the dialog and wait until the dialog is closed
        setVisible(true);
        // Return the result (flag is manipulated by another thread)
        return submitted;
    }

    /**
     * Returns a char array containing the password field's text.
     * NOTE: The char array should be overwritten after use for security purposes
     * @return the password
     */
    public char[] getPassword() {
        return passwordField.getPassword();
    }

    /**
     * Method called when the user hits the Enter key, Ok or Cancel
     * @param e Event information
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();
        // Did the user click the OK button or hit enter?
        if(source == okButton || source == passwordField)
            // Yes; set the submitted flag
            submitted = true;
        if(source == cancelButton || source == okButton || source == passwordField)
            // Hide the dialog and go back to the main application frame
            setVisible(false);
    }

    /**
     * Ensures the main application window is brought to the front when the application loses and then regains
     * focus while the dialog is open
     * @param e Event information passed by Swing
     */
    @Override
    public void windowGainedFocus(WindowEvent e) {
        if(owner != null)
            owner.toFront();
    }

    @Override
    public void windowLostFocus(WindowEvent e) {

    }

    /**
     * Creates the dialog frame's components, adds them to itself and resizes to fit its children
     */
    private void create() {
        // Add padding around the dialog's contents
        ((JPanel)getContentPane()).setBorder(new EmptyBorder(5, 10, 5, 10));
        // Lay out children vertically
        setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));

        // Create a new left-aligned layer
        Box layer = new AlignedLayer(AlignedLayer.LEFT);
        // Add a descriptive label to it
        layer.add(new JLabel("Enter password:"));
        // Add the layer to the dialog's content pane
        add(layer);

        layer = new AlignedLayer(AlignedLayer.LEFT);
        // Create a new password field sized to fit 24 characters
        passwordField = new JPasswordField(24);
        // Add self as an action listener for the password field (activates when the user presses enter when the
        // field has focus
        passwordField.addActionListener(this);
        layer.add(passwordField);
        add(layer);

        layer = new AlignedLayer(AlignedLayer.CENTER);

        okButton = new JButton("OK");
        okButton.addActionListener(this);
        layer.add(okButton);

        cancelButton = new JButton("Cancel");
        layer.add(cancelButton);
        cancelButton.addActionListener(this);

        add(layer);
        // Resize the dialog to fit its children
        pack();
    }
}
