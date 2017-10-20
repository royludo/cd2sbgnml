package fr.curie.cd2sbgnml.model;

import fr.curie.cd2sbgnml.graphics.Glyph;
import fr.curie.cd2sbgnml.xmlcdwrappers.StyleInfo;

/**
 * Abstraction of glyphs displayed for a reaction that don't have a concrete existence in celldesigner format,
 * but that are still important for the layout of the reaction.
 * Like process, association/dissociation and logic gates.
 */
public class ReactionNodeModel extends GenericReactionElement {

    private StyleInfo styleInfo;

    public ReactionNodeModel(GenericReactionModel genericReactionModel, Glyph glyph, String id, StyleInfo styleInfo) {
        super(glyph, id);
        this.styleInfo = styleInfo;
    }

    public StyleInfo getStyleInfo() {
        return styleInfo;
    }
}
