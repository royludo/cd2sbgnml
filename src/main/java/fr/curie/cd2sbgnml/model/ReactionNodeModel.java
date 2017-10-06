package fr.curie.cd2sbgnml.model;

import fr.curie.cd2sbgnml.graphics.Glyph;

/**
 * Abstraction of glyphs displayed for a reaction that don't have a concrete existence in celldesigner format,
 * but that are still important for the layout of the reaction.
 * Like process, association/dissociation and logic gates.
 */
public class ReactionNodeModel extends GenericReactionElement {

    public ReactionNodeModel(GenericReactionModel genericReactionModel, Glyph glyph) {
        super(glyph);
    }
}
