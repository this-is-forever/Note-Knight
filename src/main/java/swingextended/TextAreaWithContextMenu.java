package swingextended;

import javax.swing.*;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.BadLocationException;
import javax.swing.undo.UndoManager;

/**
 * A {@link JTextArea} with a context menu which will appear when the user right clicks or presses the context menu key
 * within the text area. Also contains extra methods that allow tracking of the caret's row and column within its text.
 */
public class TextAreaWithContextMenu extends JTextArea implements CaretListener, UndoableEditListener {

    // References the context menu
    private final TextAreaContextMenu contextMenu;
    // Stores the location of the caret start and end as caret events fire
    private int selectionStartRow, selectionStartCol;
    private int selectionEndRow, selectionEndCol;
    // Stores the length of the selection as caret events fire
    private int selectionLength;

    private final UndoManager historyManager;

    public TextAreaWithContextMenu() {
        // Fix the Replace bug that prevents Undo from working correctly
        setDocument(new CustomUndoPlainDocument());

        // Create the context menu
        contextMenu = new TextAreaContextMenu(this);
        // Allow the context menu to listen to mouse and key events
        addMouseListener(contextMenu);
        addKeyListener(contextMenu);

        // Begin listening for changes to the caret's position
        addCaretListener(this);
        // Begin tracking document history for undo/redo
        historyManager = new UndoManager();
        getDocument().addUndoableEditListener(this);
    }

    /**
     * Deletes the text area's selected text.
     */
    public void delete() {
        replaceSelection("");
    }

    /**
     * Determines whether the text area contains no text.
     * @return true if it's empty, otherwise false
     */
    public boolean isEmpty() {
        return getText().isEmpty();
    }

    /**
     * Determines whether the text area is empty
     * @return true if it's empty, otherwise false.
     */
    public boolean isTextSelected() {
        return getSelectedText() != null;
    }

    /**
     * Gets the column of the selection's start
     * @return the column of the start of the selection
     */
    public int getSelectionStartColumn() {
        return selectionStartCol;
    }

    /**
     * Gets the row of the selection's start
     * @return the row of the start of the selection
     */
    public int getSelectionStartRow() {
        return selectionStartRow;
    }

    /**
     * Gets the column of the selection's end
     * @return the column of the end of the selection
     */
    public int getSelectionEndColumn() {
        return selectionEndCol;
    }

    /**
     * Gets the row of the selection's end
     * @return the row of the end of the selection
     */
    public int getSelectionEndRow() {
        return selectionEndRow;
    }

    /**
     * Gets the number of characters currently selected in the text area
     * @return the length of the selection
     */
    public int getSelectionLength() {
        return selectionLength;
    }

    /**
     * Called by the Swing event thread when the text area's caret is moved
     * @param e Event information passed by Swing
     */
    @Override
    public void caretUpdate(CaretEvent e) {
        updateCaretInformation(getSelectionStart(), getSelectionEnd());
    }

    /**
     * Returns a reference to the text area's context menu
     * @return a {@link TextAreaContextMenu} referencing the text area's menu
     */
    public TextAreaContextMenu getContextMenu () {
        return contextMenu;
    }

    /**
     * Clears the text area's undo/redo history
     */
    public void clearEditHistory() {
        historyManager.discardAllEdits();
    }

    /**
     * Registers a new edit to the document that can be undone/redone
     * @param e Event information passed by Swing
     */
    @Override
    public void undoableEditHappened(UndoableEditEvent e) {
        historyManager.addEdit(e.getEdit());
    }

    /**
     * Updates the context menu's Undo and Redo items, based on the current history of the document
     */
    public void updateUndoRedo() {
        contextMenu.updateUndoRedoItems(historyManager.canUndo(), historyManager.canRedo());
    }

    /**
     * Determines whether the document's current history allows an Undo action
     * @return true if an Undo action is possible, otherwise false
     */
    public boolean canUndo() {
        return historyManager.canUndo();
    }

    /**
     * Determines whether the document's current history allows an Redo action
     * @return true if an Redo action is possible, otherwise false
     */
    public boolean canRedo() {
        return historyManager.canRedo();
    }

    /**
     * If possible, undoes the most recent change to the document
     */
    public void undo() {
        if(historyManager.canUndo())
            historyManager.undo();
    }

    /**
     * If possible, redoes the most recent undone-change to the document
     */
    public void redo() {
        if(historyManager.canRedo())
            historyManager.redo();
    }

    /**
     * Updates the object's internal tracking of the caret's position. Called by
     * {@link TextAreaWithContextMenu#caretUpdate}
     * @param start The beginning index of the selection
     * @param end The end index of the selection
     */
    private void updateCaretInformation(int start, int end) {
        // Determine the length of the selection and save it for later
        selectionLength = end - start;

        try {
            // Determine the line number of the start of the selection
            selectionStartRow = getLineOfOffset(start);
            // Use that number to determine the column by subtracting the start's offset from the index of the start
            // of the line
            // Add one to both so we aren't starting from zero
            selectionStartCol = start - getLineStartOffset(selectionStartRow) + 1;
            selectionStartRow++;
            // Do the same with the selection end
            selectionEndRow = getLineOfOffset(end);
            selectionEndCol = end - getLineStartOffset(selectionEndRow) + 1;
            selectionEndRow++;
        } catch (BadLocationException ex) {
            ex.printStackTrace();
        }
    }
}
