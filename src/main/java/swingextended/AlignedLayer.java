package swingextended;

import javax.swing.*;
import java.awt.*;

/**
 * A class that allows the creation of a layer of components, laid out left-to-right, with the components aligned
 * left, right or center within the layer. Layers are meant to be added to a larger container with a vertical layout
 * manager, such as a {@link BoxLayout} with {@link BoxLayout#Y_AXIS} display.
 */
public class AlignedLayer extends Box {

    // Used to signal alignment of components during instantiation
    public static final int LEFT = 1; /* Signals left alignment of components */
    public static final int RIGHT = 2; /* Signals right alignment of components */
    public static final int CENTER = 3; /* Signals center alignment of components */

    // Masks used to determine where to place horizontal glue within the layer
    private static final int LEFT_GLUE_MASK = 2, RIGHT_GLUE_MASK = 1;

    // Flags set when glue is placed to the left or to the right of the components for the sake of alignment
    private boolean leftGlue, rightGlue;

    /**
     * Creates a new layer with the given alignment and adds all listed components to the layer
     * @param alignment The desired alignment for components within the layer
     * @param components The components to add to the layer
     */
    public AlignedLayer(int alignment, Component ... components) {
        super(BoxLayout.X_AXIS);
        if((alignment & LEFT_GLUE_MASK) > 0) {
            add(Box.createHorizontalGlue());
            leftGlue = true;
        }
        for(Component c : components)
            add(c);
        if((alignment & RIGHT_GLUE_MASK) > 0) {
            add(Box.createHorizontalGlue());
            rightGlue = true;
        }
    }

    /**
     * Creates a new, empty later with the given alignment of components
     * @param alignment The desired alignment
     */
    public AlignedLayer(int alignment) {
        super(BoxLayout.X_AXIS);
        if((alignment & LEFT_GLUE_MASK) > 0) {
            add(Box.createHorizontalGlue());
            leftGlue = true;
        }
        if((alignment & RIGHT_GLUE_MASK) > 0) {
            add(Box.createHorizontalGlue());
            rightGlue = true;
        }
    }

    /**
     * Adds a component to the layer. New components are added to the right of existing components
     * @param c The {@link Component} to add to the layer.
     * @return A reference to the {@link Component} added
     */
    @Override
    public Component add(Component c) {
        int index = getComponentCount();
        if(rightGlue)
            index--;
        return super.add(c, index);
    }
}
