package ui;

import swingextended.AlignedLayer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NKFindDialog extends JDialog implements DocumentListener, WindowFocusListener {

    // Defines the amount of space, in pixels, to leave between components
    private static final int COMPONENT_SPACING = 5;

    // References the application's main window for positioning and modality
    private final NKMainFrame owner;
    // References the field for text to search for and the text to replace with
    private final JTextField textField, replacementField;
    // References the "match case" and "regex" check boxes
    private final JCheckBox matchCaseBox, regexBox;

    // Flag set when a search has been done
    private boolean patternCompiled;

    // References the pattern which will be used to search the document
    private Pattern query;

    public NKFindDialog(NKMainFrame owner) {
        this.owner = owner;
        // Begin listening for when the window gains focus
        addWindowFocusListener(this);
        // Prevent resizing
        setResizable(false);
        // Alter the window's title
        setTitle("Find Text");
        // Ensure the dialog maintains focus while open
        setModal(true);
        setModalityType(ModalityType.APPLICATION_MODAL);
        // Vertically lay out components
        JPanel contentPane = (JPanel)getContentPane();
        setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
        // Create COMPONENT_SPACING padding around the content pane
        contentPane.setBorder(
                new EmptyBorder(COMPONENT_SPACING, COMPONENT_SPACING, COMPONENT_SPACING, COMPONENT_SPACING));

        AlignedLayer layer = new AlignedLayer(AlignedLayer.LEFT);
        layer.add(new JLabel("Find:"));
        add(layer);

        layer = new AlignedLayer(AlignedLayer.LEFT);
        textField = new JTextField();
        textField.getDocument().addDocumentListener(this);
        layer.add(textField);
        add(layer);

        layer = new AlignedLayer(AlignedLayer.LEFT);
        matchCaseBox = new JCheckBox("Match case");
        matchCaseBox.addActionListener(e -> searchChanged());
        layer.add(matchCaseBox);
        layer.add(createGap());
        regexBox = new JCheckBox("Regex");
        //regexBox.setEnabled(false);
        regexBox.addActionListener(e -> searchChanged());
        layer.add(regexBox);
        add(layer);

        layer = new AlignedLayer(AlignedLayer.CENTER);
        JButton previousButton = new JButton("Previous");
        previousButton.addActionListener(e -> find(false));
        layer.add(previousButton);
        layer.add(createGap());
        JButton nextButton = new JButton("Next");
        nextButton.addActionListener(e -> find(true));
        layer.add(nextButton);
        add(layer);

        layer = new AlignedLayer(AlignedLayer.LEFT);
        layer.add(new JLabel("Replace with:"));
        add(layer);

        layer = new AlignedLayer(AlignedLayer.LEFT);
        replacementField = new JTextField();
        layer.add(replacementField);
        add(layer);

        layer = new AlignedLayer(AlignedLayer.CENTER);
        JButton replaceButton = new JButton("Replace");
        replaceButton.addActionListener(e -> replace());
        layer.add(replaceButton);
        layer.add(createGap());
        JButton replaceAllButton = new JButton("Replace All");
        replaceAllButton.addActionListener(e -> replaceAll());
        layer.add(replaceAllButton);
        layer.validate();
        Dimension buttonSize = replaceAllButton.getPreferredSize();
        add(layer);

        // Make all the buttons the same size
        replaceButton.setPreferredSize(buttonSize);
        nextButton.setPreferredSize(buttonSize);
        previousButton.setPreferredSize(buttonSize);

        pack();
    }

    public void replace() {
        NKTextArea textArea = owner.getTextArea();
        if(textArea.isTextSelected()) {
            String replacementText = replacementField.getText();
            textArea.replaceSelection(replacementText);
        }
        find(true);
    }

    public void replaceAll() {
        if(!patternCompiled)
            compilePattern();
        NKTextArea textArea = owner.getTextArea();
        String documentText = textArea.getText();
        String replacementText = replacementField.getText();
        Matcher matcher;
        matcher = query.matcher(documentText);
        textArea.setText(matcher.replaceAll(replacementText));
    }

    public void find(boolean forward) {
        if(!patternCompiled)
            compilePattern();
        NKTextArea textArea = owner.getTextArea();
        String documentText = textArea.getText();
        int caret = textArea.getSelectionStart();
        int selectionSize = textArea.getSelectionLength();
        int matchStart = -1, matchEnd = -1;
        Matcher matcher;
        if(forward) {
            matcher = query.matcher(documentText);
            if(matcher.find(caret + selectionSize)) {
                matchStart = matcher.start();
                matchEnd = matcher.end();
            }
        } else {
            matcher = query.matcher(documentText);
            while(matcher.find()) {
                int start = matcher.start();
                if(start >= caret)
                    break;
                matchStart = start;
                matchEnd = matcher.end();
            }
        }

        if(matchStart >= 0) {
            textArea.setSelectionStart(matchStart);
            textArea.setSelectionEnd(matchEnd);
        }
    }

    @Override
    public void insertUpdate(DocumentEvent e) {
        searchChanged();
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        searchChanged();
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
        searchChanged();
    }

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

    private void compilePattern() {
        String textToFind = textField.getText();
        patternCompiled = true;
        int options = 0; //Pattern.DOTALL; ?
        if(!matchCaseBox.isSelected())
            options |= Pattern.CASE_INSENSITIVE;
        if(regexBox.isSelected())
            query = Pattern.compile(textToFind, options);
        else
            query = Pattern.compile(Pattern.quote(textToFind), options);
    }

    private static Component createGap() {
        return Box.createHorizontalStrut(COMPONENT_SPACING);
    }

    private void searchChanged() {
        patternCompiled = false;
    }
}
