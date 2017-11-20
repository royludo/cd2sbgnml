package fr.curie.cd2sbgnml.model;

import fr.curie.cd2sbgnml.graphics.GeometryUtils;
import fr.curie.cd2sbgnml.graphics.Glyph;
import fr.curie.cd2sbgnml.xmlcdwrappers.StyleInfo;

import java.awt.geom.Point2D;

/**
 * Abstraction of glyphs displayed for a reaction that don't have a concrete existence in celldesigner format,
 * but that are still important for the layout of the reaction.
 * Like process, association/dissociation and logic gates.
 * All those glyphs can have ports.
 */
public class ReactionNodeModel extends GenericReactionElement {

    /**
     * Corresponds to the possible orientation of PNs as described in SBGN-ML
     */
    public enum Orientation {HORIZONTAL, VERTICAL}

    /**
     * Ports will be located at a distance relative to the size of the concerned glyph.
     * This defines that ratio.
     */
    private float portDistanceRatio;

    private StyleInfo styleInfo;

    /**
     * The reaction nodes as defined by this class are simple square or circle shapes. One number is enough to
     * determine their size.
     */
    private float size;

    /**
     * One port is called 'In' but it can also have output/production arcs, in the case of reversible reactions.
     */
    private Point2D.Float portIn;
    private Point2D.Float portOut;
    private Orientation orientation;

    public ReactionNodeModel(Glyph glyph, String id,
                             float size, float portDistanceRatio, StyleInfo styleInfo) {
        super(glyph, id);
        this.styleInfo = styleInfo;
        this.size = size;
        this.portDistanceRatio = portDistanceRatio;
    }

    /**
     * Given 2 points forming a line going through the process and connecting the base reactant and products,
     * determines what is the orientation of the process and where are the ports.
     */
    public void setPorts(Point2D pIn, Point2D pOut) {

        // first get the slope to determine if horizontal or vertical
        float slope = GeometryUtils.lineSlope(pIn, pOut);
        if(slope > -1 && slope < 1) {
            this.orientation = Orientation.HORIZONTAL;

            // create 2 positions for the ports on each side of the horizontal line going through the center
            Point2D.Float p1 = new Point2D.Float(
                    (float) (this.getGlyph().getCenter().getX() - this.getSize() / 2 - this.getSize() * this.getPortDistanceRatio()),
                    (float) (this.getGlyph().getCenter().getY()));
            Point2D.Float p2 = new Point2D.Float(
                    (float) (this.getGlyph().getCenter().getX() + this.getSize() / 2 + this.getSize() * this.getPortDistanceRatio()),
                    (float) (this.getGlyph().getCenter().getY()));

            if(pIn.getX() < pOut.getX()) { // left to right
                this.portIn = p1;
                this.portOut = p2;
            }
            else { // right to left
                this.portIn = p2;
                this.portOut = p1;
            }
        }
        else {
            this.orientation = Orientation.VERTICAL;

            // create 2 positions for the ports on each side of the vertical line going through the center
            Point2D.Float p1 = new Point2D.Float(
                    (float) (this.getGlyph().getCenter().getX()),
                    (float) (this.getGlyph().getCenter().getY() - this.getSize() / 2 - this.getSize() * this.getPortDistanceRatio()));
            Point2D.Float p2 = new Point2D.Float(
                    (float) (this.getGlyph().getCenter().getX()),
                    (float) (this.getGlyph().getCenter().getY() + this.getSize() / 2 + this.getSize() * this.getPortDistanceRatio()));

            if(pIn.getY() < pOut.getY()) { // top to bottom
                this.portIn = p1;
                this.portOut = p2;
            }
            else { // bottom to top
                this.portIn = p2;
                this.portOut = p1;
            }
        }
    }

    public StyleInfo getStyleInfo() {
        return styleInfo;
    }

    public float getSize() {
        return size;
    }

    public Point2D.Float getPortIn() {
        return portIn;
    }

    public Point2D.Float getPortOut() {
        return portOut;
    }

    public Orientation getOrientation() {
        return orientation;
    }

    public float getPortDistanceRatio() {
        return portDistanceRatio;
    }
}
