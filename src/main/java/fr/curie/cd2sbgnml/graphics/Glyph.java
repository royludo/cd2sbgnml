package fr.curie.cd2sbgnml.graphics;

import java.awt.geom.Point2D;

/**
 * Something that can be placed on a map, that is not a link.
 */
public class Glyph {

    private Point2D.Float center;
    private float width;
    private float height;
    private CdShape cdShape;
    private SbgnShape sbgnShape;
    private String label;

    public Glyph (Point2D.Float center, float width, float height, CdShape cdShape, SbgnShape sbgnShape) {
        this.center = center;
        this.width = width;
        this.height = height;
        this.cdShape = cdShape;
        this.sbgnShape = sbgnShape;
    }

    public Point2D.Float getCenter() {
        return center;
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }

    public CdShape getCdShape() {
        return cdShape;
    }

    public SbgnShape getSbgnShape() {
        return sbgnShape;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return "[Center: "+this.getCenter()+" width: "+this.width+" height: "+this.height+ "]";
    }
}
