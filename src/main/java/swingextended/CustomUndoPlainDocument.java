package swingextended;

import javax.swing.event.UndoableEditEvent;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;
import javax.swing.undo.CompoundEdit;

/**
 * <p>Credit to Stack Overflow user 'aterai' @ https://stackoverflow.com/a/24450716</p>
 *
 * <p>A replacement {@link PlainDocument} implementation that ensures text
 * replacement doesn't add two edits to an UndoManager. This fixes a bug in the UndoManager class that caused
 * text replacements (such as while using the application's replace function) to add an extra history edit showing
 * a blank document.</p>
 */
class CustomUndoPlainDocument extends PlainDocument {
    private CompoundEdit compoundEdit;
    @Override protected void fireUndoableEditUpdate(UndoableEditEvent e) {
        if (compoundEdit == null) {
            super.fireUndoableEditUpdate(e);
        } else {
            compoundEdit.addEdit(e.getEdit());
        }
    }
    @Override public void replace(
            int offset, int length,
            String text, AttributeSet attrs) throws BadLocationException {
        if (length == 0) {
            super.replace(offset, length, text, attrs);
        } else {
            compoundEdit = new CompoundEdit();
            super.fireUndoableEditUpdate(new UndoableEditEvent(this, compoundEdit));
            super.replace(offset, length, text, attrs);
            compoundEdit.end();
            compoundEdit = null;
        }
    }
}