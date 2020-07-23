package ui;

import swingextended.AlignedLayer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.util.HashMap;
import java.util.Map;

/**
 * A font chooser dialog used by the application that allows the user to change the document's font.
 */
public class NKFontChooser extends JDialog implements WindowFocusListener {

    // Determines the amount of pixels between components, using borders and rigid areas
    private static final int COMPONENT_SPACING = 5;
    // Defines the minimum and maximum font sizes the user may set for the application's editor
    private static final int FONT_SIZE_MINIMUM = 6, FONT_SIZE_MAXIMUM = 72;

    // References the application's window for the sake of positioning and modality
    private final NKMainFrame owner;
    // The list component that will hold the fonts to choose from
    private final JList<FontOption> fontList;
    // The font size chooser component
    private final JSpinner fontSizeChooser;

    /**
     * Represents an option in the font list, {@link NKFontChooser#fontList}
     */
    private static class FontOption {
        private final Font font;

        public FontOption(Font f) {
            font = f;
        }

        @Override
        public String toString() {
            return font.getName();
        }
    }

    /**
     * Used by {@link NKFontChooser#fontList} to render each font in the list in its font
     */
    private static class FontRenderer extends JLabel implements ListCellRenderer<FontOption> {

        // Determine the default colors for list items within the user's OS L&F
        private final Color background = UIManager.getLookAndFeel().getDefaults().getColor("List.background"),
                foreground = UIManager.getLookAndFeel().getDefaults().getColor("List.foreground"),
                selectionBackground = UIManager.getLookAndFeel().getDefaults().getColor("List.selectionBackground"),
                selectionForeground = UIManager.getLookAndFeel().getDefaults().getColor("List.selectionForeground");

        /**
         * Renders a list item in its font.
         * @param list The list being rendered
         * @param value The {@link FontOption} being rendered during this iteration
         * @param index The index of the rendered item
         * @param isSelected Flag set when the item being rendered is selected by the user
         * @param cellHasFocus Flag set when the item being rendered has focus
         * @return A reference to the this {@link FontRenderer}
         */
        @Override
        public Component getListCellRendererComponent(JList<? extends FontOption> list, FontOption value, int index, boolean isSelected, boolean cellHasFocus) {
            // Set the font and the text of the item
            setFont(value.font);
            setText(value.font.getName());
            // Determine the appropriate colors to draw
            Color foreground;
            Color background;
            if(isSelected) {
                background = selectionBackground;
                foreground = selectionForeground;
            } else {
                background = this.background;
                foreground = this.foreground;
            }
            setBackground(background);
            setForeground(foreground);
            // Ensure a background is drawn behind the text
            setOpaque(true);
            // Return a reference to this object
            return this;
        }
    }

    /**
     * Sets up the dialog with a starting font and begins notifying the main window when the user changes the
     * selected font
     * @param owner A reference to the application's main window
     * @param startingFont A reference to the text area's starting font
     */
    public NKFontChooser(NKMainFrame owner, Font startingFont) {
        // Set the title of the window
        setTitle(NKMainFrame.APPLICATION_NAME + " - Font Chooser");
        // Ensure the dialog maintains focus while open
        setModal(true);
        setModalityType(ModalityType.APPLICATION_MODAL);
        // Begin listening for when the window gains focus
        addWindowFocusListener(this);
        // Keep a reference to the main application window for later
        this.owner = owner;
        // Create a map so we can map font names to items in the font list
        // Used to look up fonts by name in the list
        Map<String, FontOption> fontMap = new HashMap<>();
        // Load all system fonts and load them into the map so they may be retreived via name
        Font[] fonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts();
        FontOption[] fontOptions = new FontOption[fonts.length];
        for(int i = 0; i < fontOptions.length; i++) {
            FontOption f = new FontOption(fonts[i].deriveFont(16.0f));
            fontOptions[i] = f;
            fontMap.put(f.font.getName(), f);
        }

        // Create and add the components to the dialog
        setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));

        AlignedLayer layer = new AlignedLayer(AlignedLayer.LEFT);
        // Place a predefined amount of space between components
        layer.setBorder(new EmptyBorder(COMPONENT_SPACING, COMPONENT_SPACING, COMPONENT_SPACING, COMPONENT_SPACING));
        layer.add(new JLabel("Choose a font:"));
        add(layer);

        // Create the font list component
        fontList = new JList<>(fontOptions);
        // Only allow one item to be chosen at a time
        fontList.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        // Lay out list items vertically
        fontList.setLayoutOrientation(JList.VERTICAL);
        // Ignore the visible row count
        fontList.setVisibleRowCount(-1);
        // Set the renderer so that fonts are displayed properly
        fontList.setCellRenderer(new FontRenderer());

        fontList.addListSelectionListener(e -> owner.fontSelected(getSelectedFont()));

        // Add a scroll bar to the list so the user can see all of the fonts listed
        JScrollPane scroller = new JScrollPane(fontList);
        scroller.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroller.setPreferredSize(new Dimension(300, 150));
        scroller.setBorder(new EmptyBorder(0, COMPONENT_SPACING, COMPONENT_SPACING, COMPONENT_SPACING));
        add(scroller);

        layer = new AlignedLayer(AlignedLayer.LEFT);
        layer.setBorder(new EmptyBorder(0, COMPONENT_SPACING, COMPONENT_SPACING, COMPONENT_SPACING));
        layer.add(new JLabel("Size:"));
        layer.add(Box.createHorizontalStrut(COMPONENT_SPACING));

        // Create the font size spinner
        fontSizeChooser = new JSpinner();
        // Set the spinner's text field up to have two digits (this is ugly!)
        (((JSpinner.DefaultEditor)fontSizeChooser.getEditor()).getTextField()).setColumns(2);
        // Set the spinner up with the current font's size and set its min and max values, setting the step size to 1
        fontSizeChooser.setModel(new SpinnerNumberModel(startingFont.getSize(), FONT_SIZE_MINIMUM, FONT_SIZE_MAXIMUM, 1));
        // Finish setting up the spinner and add it to the frame
        fontSizeChooser.addChangeListener(e -> owner.fontSelected(getSelectedFont()));
        layer.add(fontSizeChooser);
        // Hacky way of determining the correct size for the spinner so that it displays the correct size
        layer.validate();
        Dimension d = fontSizeChooser.getPreferredSize();
        fontSizeChooser.setMaximumSize(d);

        add(layer);
        // Resize the dialog to fit its children
        pack();

        // Scroll to the default font
        fontList.setSelectedValue(fontMap.get(startingFont.getName()), true);
    }

    /**
     * Ensures the main application window is brought to the front when the application loses and then regains
     * focus while the dialog is open
     * @param e Event information passed by Swing
     */
    @Override
    public void windowGainedFocus(WindowEvent e) {
        owner.toFront();
    }

    @Override
    public void windowLostFocus(WindowEvent e) {

    }

    /**
     * Overrides the setVisible method of JDialog so the dialog is positioned relative to the application window
     * @param visibility Flag set if the window should show
     */
    @Override
    public void setVisible(boolean visibility) {
        if(visibility)
            setLocationRelativeTo(owner);
        super.setVisible(visibility);
    }

    /**
     * Gets the font currently selected by the user
     * @return A {@link Font} object with the selected font name and size
     */
    private Font getSelectedFont() {
        return fontList.getSelectedValue().font
                .deriveFont(((Integer)fontSizeChooser.getValue()).floatValue());
    }

}
