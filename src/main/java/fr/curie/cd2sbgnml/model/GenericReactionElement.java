package fr.curie.cd2sbgnml.model;

import fr.curie.cd2sbgnml.graphics.Glyph;

public abstract class GenericReactionElement {

    private Glyph glyph;
    private String id;

    public GenericReactionElement(Glyph glyph, String id){
        this.glyph = glyph;
        this.id = id;
    }

    public Glyph getGlyph() {
        return glyph;
    }

    public String getId() {
        return id;
    }




}
