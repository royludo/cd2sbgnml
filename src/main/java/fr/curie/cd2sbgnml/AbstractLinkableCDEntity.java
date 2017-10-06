package fr.curie.cd2sbgnml;

import fr.curie.cd2sbgnml.graphics.CdShape;
import fr.curie.cd2sbgnml.xmlcdwrappers.SpeciesWrapper;

import java.awt.geom.Point2D;

/**
 * Represents things that can be linked, even without a concrete existence in CellDesigner
 * like processes and logic gate. Also represents an abstraction of species as glyphs on
 * the map.
 * Everything that can be linked.
 */
public abstract class AbstractLinkableCDEntity {

    public enum AbstractLinkableCDEntityType {SPECIES, LOGIC_GATE, PROCESS}
    private AbstractLinkableCDEntityType type;

    private Point2D.Float coords;
    private float height;
    private float width;


    public AbstractLinkableCDEntity(AbstractLinkableCDEntityType type) {
        this.type = type;
    }

    public AbstractLinkableCDEntityType getType() {
        return type;
    }

    public abstract Point2D.Float getCoords();

    public abstract float getHeight();

    public abstract float getWidth();

    public abstract CdShape getShape();


}
