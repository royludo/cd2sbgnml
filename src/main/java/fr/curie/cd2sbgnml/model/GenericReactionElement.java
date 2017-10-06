package fr.curie.cd2sbgnml.model;

import fr.curie.cd2sbgnml.graphics.Glyph;

public abstract class GenericReactionElement {

    private Glyph glyph;

    public GenericReactionElement(Glyph glyph){
        this.glyph = glyph;
    }

    public Glyph getGlyph() {
        return glyph;
    }





}
