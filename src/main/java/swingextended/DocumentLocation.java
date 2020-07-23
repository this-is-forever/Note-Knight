package swingextended;

import javax.swing.*;
import javax.swing.text.BadLocationException;

/**
 * Represents a spot in a document via row and column
 */
public class DocumentLocation {

    // The row (line number) and column of this DocumentLocation
    public int row, col;

    /**
     * Translates an index within a JTextArea into a {@link DocumentLocation} object. If the given index falls outside
     * of the text area's length, it will instead return a {@link DocumentLocation} representing the last index in the
     * text area.
     * @param index The index to translate
     * @param textArea The textArea within which to translate the index
     * @return A new {@link DocumentLocation} object with the corresponding row (line numnber) and column.
     */
    public static DocumentLocation translateIndex(int index, JTextArea textArea) {
        try {
            // Determine the line number of the start of the selection
            int row = textArea.getLineOfOffset(index);
            // Use that number to determine the column by subtracting the start's offset from the index of the start
            // of the line
            // Add one to both so we aren't starting from zero
            int col = index - textArea.getLineStartOffset(row) + 1;
            row++;
            return new DocumentLocation(row, col);
        } catch (BadLocationException ex) {
            return translateIndex(textArea.getText().length(), textArea);
        }
    }

    /**
     * Translates a {@link DocumentLocation} object into a valid index within a {@link JTextArea}. If a valid row
     * (line number) is given but the column falls outside the length of the line, the index of the end of the line
     * is instead given. If an invalid row (line number) is given, it returns the index of the end of the document.
     * @param location The location to translate
     * @param textArea The text area to translate within
     * @return the index after translating
     */
    public static int translateLocation(DocumentLocation location, JTextArea textArea) {
        try {
            int transRow = location.row - 1;
            int pos = textArea.getLineStartOffset(transRow);
            pos += location.col - 1;
            int endOfLineIndex = textArea.getLineEndOffset(transRow) - 1;
            if(pos > endOfLineIndex)
                pos = endOfLineIndex;
            return Math.min(pos, textArea.getText().length());
        } catch (BadLocationException e) {
            return textArea.getText().length();
        }
    }

    /**
     * Parses a string in the format 'row' or 'row:column' into a {@link DocumentLocation} object. If the format of
     * the {@link String} does not follow the previously defined format, null is returned.
     * @param s The {@link String} to format
     * @return A new {@link DocumentLocation} derived from the given text, or null if invalid text was given
     */
    public static DocumentLocation parseLocation(String s) {
        String[] strings = s.split(":", 2);
        try {
            int row, col;
            if(strings.length == 1) {
                row = Integer.parseInt(strings[0]);
                col = 1;
            } else {
                row = Integer.parseInt(strings[0]);
                col = Integer.parseInt(strings[1]);
            }
            if(row < 1)
                row = 1;
            if(col < 1)
                col = 1;
            return new DocumentLocation(row, col);
        } catch(NumberFormatException e) {
            return null;
        }
    }

    /**
     * Converts the object into a {@link String}, in the format 'row:col'
     * @return A {@link String} object formatted with the object's information
     */
    @Override
    public String toString() {
        return String.format("%d:%d", row, col);
    }

    /**
     * Instantiates a new object with the given row and col
     * @param row The row (line number) of the location
     * @param col The column of the location
     */
    public DocumentLocation(int row, int col) {
        this.row = row;
        this.col = col;
    }

    /**
     * Moves the cursor of a text area to this object's location
     * @param textArea A {@link JTextArea} whose cursor should be moved
     */
    public void navigate(JTextArea textArea) {
        textArea.setCaretPosition(translateLocation(this, textArea));
    }
}
