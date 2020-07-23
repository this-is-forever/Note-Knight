package ui;

import swingextended.TextAreaContextMenu;
import swingextended.TextAreaWithContextMenu;

import javax.swing.*;
import javax.swing.event.CaretEvent;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.event.UndoableEditEvent;

/**
 * A {@link TextAreaWithContextMenu} with additional behavior relevant to the application. The component will notify
 * the parent {@link NKMainFrame} when its caret moves so that the application can display the caret location.
 */
public class NKTextArea extends TextAreaWithContextMenu implements PopupMenuListener {

    private static final int SEARCH_STRING_MAXIMUM_LENGTH = 12;
    private static final String DEFAULT_SEARCH_ITEM_TEXT = "Search with Google";
    private static final String SEARCH_OPTION_PATTERN = "Search Google for \"%s\"";

    // Reference to the application's main frame
    private final NKMainFrame parent;

    // References the 'Search Google for...' menu item
    private final JMenuItem searchItem;

    /**
     * Creates a new component and remembers its parent for later
     * @param parent A reference to the application's main window
     */
    public NKTextArea(NKMainFrame parent) {
        // Keep a reference to the parent window for event-related purposes
        this.parent = parent;

        // Grab a reference to this text area's context menu
        TextAreaContextMenu contextMenu = getContextMenu();
        // Begin listening for when the context menu pops up
        contextMenu.addPopupMenuListener(this);
        // Create the 'Search Google for...' item and its functionality
        contextMenu.addSeparator();
        searchItem = new JMenuItem();
        searchItem.addActionListener(e -> parent.search());
        contextMenu.add(searchItem);
    }

    /**
     * Method called when the editor's caret position changes
     * @param e Event information
     */
    @Override
    public void caretUpdate(CaretEvent e) {
        super.caretUpdate(e);
        parent.caretMoved();
    }

    /**
     * Updates the text of a {@link JMenuItem} to with the text "Search Google for "X" where X is the selected text.
     * If no non-whitespace characters are selected, the menu item is instead disabled.
     * @param item The {@link JMenuItem} whose text should be updated
     */
    public void updateSearchItem(JMenuItem item) {
        // Is non-whitespace currently selected?
        if(isTextSelected() && !getSelectedText().isBlank()) {
            // Yes; updated the item's text to reflect it
            String searchQuery;
            // Remove leading and trailing whitespace from the query
            String selectedText = getSelectedText().strip();
            // Only show the first SEARCH_STRING_MAXIMUM_LENGTH characters of the selected text in the menu item
            if(selectedText.length() > SEARCH_STRING_MAXIMUM_LENGTH)
                searchQuery = selectedText.substring(0, SEARCH_STRING_MAXIMUM_LENGTH) + "...";
            else
                searchQuery = selectedText;
            // Set the text and enable the item
            item.setText(String.format(SEARCH_OPTION_PATTERN, searchQuery));
            item.setEnabled(true);
        } else {
            // No; disable the context menu and reset its text to the default text
            item.setText(DEFAULT_SEARCH_ITEM_TEXT);
            item.setEnabled(false);
        }
    }

    /**
     * Clears the editor's history and updates the main window's and context menu's Undo and Redo menu items
     */
    @Override
    public void clearEditHistory() {
        super.clearEditHistory();
        updateUndoRedo();
    }

    /**
     * Updates the document's text and resets the Undo/Redo options for associated menus
     * @param text The new text for the document
     */
    @Override
    public void setText(String text) {
        super.setText(text);
        updateUndoRedo();
    }

    /**
     * Replaces the selected text with the given replacement text. Updates the Undo/Redo options of associated menus
     * in the process
     * @param replacement The text to replace the text area's current text
     */
    @Override
    public void replaceSelection(String replacement) {
        super.replaceSelection(replacement);
        updateUndoRedo();
    }

    /**
     * Method called by Swing when the context menu is about to appear. Updates the context menu's text
     * @param e Event information provided by Swing
     */
    @Override
    public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
        updateSearchItem(searchItem);
        updateUndoRedo();
    }

    @Override
    public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {

    }

    /**
     * Unused method; required to use {@link PopupMenuListener}
     * @param e Event information given by Swing. Unused.
     */
    @Override
    public void popupMenuCanceled(PopupMenuEvent e) {

    }

    /**
     * Method called when a recordable edit is made to the document, for Undo and Redo purposes. Updates the
     * Undo/Redo menu options when called.
     * @param e Event information passed by Swing
     */
    @Override
    public void undoableEditHappened(UndoableEditEvent e) {
        super.undoableEditHappened(e);
        parent.updateUndoRedo(canUndo(), canRedo());
    }

    /**
     * Ensures the context menu and Edit menu's Undo and Redo options reflect the current history of the document
     */
    @Override
    public void updateUndoRedo() {
        super.updateUndoRedo();
        parent.updateUndoRedo(canUndo(), canRedo());
    }

    /**
     * Undoes the most recent change to the document and updates related menus
     */
    @Override
    public void undo() {
        super.undo();
        updateUndoRedo();
    }

    /**
     * Redoes the most recent undone-change to the document and updates related menus
     */
    @Override
    public void redo() {
        super.redo();
        updateUndoRedo();
    }
}
