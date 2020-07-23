package swingextended;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;

/**
 * A dialog window that will grab a line and column number for the user for the sake of navigating a document
 */
public class GoToDialog extends JDialog implements ActionListener, WindowFocusListener {

    // Reference to the last selected location (or null if the dialog was closed without choosing one)
    private DocumentLocation selectedLocation;

    // Reference to the parent window that will create the dialog, for positioning purposes
    private final JFrame parent;
    // Reference to the Go button, to determine if it called actionPerformed
    private final JButton goButton;
    // Reference to the locationField, to determine if it called actionPerformed and to get the user's input
    private final JTextField locationField;

    /**
     * Creates the dialog and its children. Does not display the dialog. (See {@link GoToDialog#showAndWait})
     * @param parent A reference to the parent window which will spawn the dialog window
     */
    public GoToDialog(JFrame parent) {
        super(parent);
        this.parent = parent;
        // Ensure the dialog maintains focus while open
        setModal(true);
        setModalityType(ModalityType.APPLICATION_MODAL);

        setTitle("Go to Line/Column");

        JPanel contentPane = (JPanel) getContentPane();
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));

        AlignedLayer layer = new AlignedLayer(AlignedLayer.LEFT);
        layer.add(new JLabel("[line][:column]:"));
        add(layer);

        layer = new AlignedLayer(AlignedLayer.LEFT);
        locationField = new JTextField(12);
        locationField.addActionListener(this);
        layer.add(locationField);
        add(layer);

        layer = new AlignedLayer(AlignedLayer.CENTER);
        goButton = new JButton("Go");
        goButton.addActionListener(this);
        layer.add(goButton);

        layer.add(Box.createHorizontalStrut(5));

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(this);
        layer.add(cancelButton);
        layer.validate();
        goButton.setPreferredSize(cancelButton.getPreferredSize());

        add(layer);

        pack();
    }

    /**
     * Gets the last selected document position from the dialog
     * @return a {@link DocumentLocation} with the line number and column of the last selected position
     */
    public DocumentLocation getSelectedLocation() {
        return selectedLocation;
    }

    /**
     * Displays the dialog window with the default text set to the current cursor position in the document. Method
     * blocks the current Thread until the dialog closes.
     * @param textArea The text area to pull a starting location from
     * @return The location that the user chose, or null if they canceled
     */
    public DocumentLocation showAndWait(JTextArea textArea) {
        // Translate the current position into a swingextended.DocumentLocation
        locationField.setText(DocumentLocation.translateIndex(textArea.getSelectionStart(), textArea).toString());
        // Ensure the text field has focus when the dialog opens and select all text so the user may immediately
        // begin typing
        locationField.requestFocus();
        locationField.selectAll();
        // Center the dialog in front of its parent
        setLocationRelativeTo(parent);
        // Show the document (Thread blocks until visibility is set to false)
        setVisible(true);
        return selectedLocation;
    }

    /**
     * Method called when the Go or Cancel buttons are pushed, or the user hits the Enter key while the text field
     * has focus. If Go was pressed, or the Enter key was pressed, the dialog closes, returning to the Thread that
     * originally called {@link GoToDialog#showAndWait}.
     * @param e Event information passed by Swing
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        // Did the user hit the Go button or hit Enter in the location field?
        if(e.getSource() == goButton || e.getSource() == locationField) {
            // Yes; attempt to parse the location given
            String locationText = locationField.getText();
            DocumentLocation location = DocumentLocation.parseLocation(locationText);
            // Was a valid location given as input?
            if(location == null)
                // No; Show an error message
                JOptionPane.showMessageDialog(this, "Invalid input", "Error",
                        JOptionPane.ERROR_MESSAGE);
            else {
                // Yes; remember the position and hide the window (causes showAndWait to stop blocking)
                selectedLocation = location;
                setVisible(false);
            }
        } else {
            // No; cancel was clicked instead. Close the window (causes showAndWait to stop blocking)
            selectedLocation = null;
            setVisible(false);
        }
    }

    /**
     * Method called when the dialog gains focus. Ensures the parent window is always visible behind the dialog
     * in the event the application loses focus.
     * @param e Event information passed by Swing
     */
    @Override
    public void windowGainedFocus(WindowEvent e) {
        parent.toFront();
    }

    /**
     * An unused method - required to implement the WindowFocusListener interface
     * @param e Event information passed by Swing
     */
    @Override
    public void windowLostFocus(WindowEvent e) {

    }
}