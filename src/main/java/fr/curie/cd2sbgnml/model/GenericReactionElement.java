package fr.curie.cd2sbgnml.model;

import fr.curie.cd2sbgnml.graphics.Glyph;

/**
 * The most basic abstraction of what is something that is involved in a reaction. Which is a simple
 * geometric glyph that is identified by a unique id.
 * The glyph can represent an real entity (protein, chemical...) or a process/logic gate or any other thing.
 */
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
