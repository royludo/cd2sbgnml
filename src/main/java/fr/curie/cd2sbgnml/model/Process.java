package fr.curie.cd2sbgnml.model;

import fr.curie.cd2sbgnml.graphics.CdShape;
import fr.curie.cd2sbgnml.graphics.GeometryUtils;
import fr.curie.cd2sbgnml.graphics.Glyph;
import fr.curie.cd2sbgnml.graphics.SbgnShape;
import fr.curie.cd2sbgnml.xmlcdwrappers.StyleInfo;

import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;

/**
 * The process is the central point of the reaction. Unlike logic gates and assocation/dissociation glyphs, it
 * has anchor points in CellDesigner.
 */
public class Process extends ReactionNodeModel {

    private static final float PROCESS_SIZE = 10;
    private static final float PORT_DISTANCE_RATIO = 1;

    /**
     * Represents the oriented line on which the process is oriented.
     * Gives a direction to the reaction, and influences where the anchor points are located
     * on the process glyph.
     * It is assumed that the center of the process lies in the middle of this line.
     * Coordinates of the axis are assumed to be absolute coordinates.
     */
    private Line2D.Float axis;

    public Process(Glyph glyph, String id,
                   Line2D.Float axis, StyleInfo styleInfo) {
        super(glyph, id, PROCESS_SIZE, PORT_DISTANCE_RATIO, styleInfo);
        this.axis = axis;
    }

    public Process(Point2D.Float centerCoords,
                   String id, Line2D.Float axis, StyleInfo styleInfo) {
        super(new Glyph(
                    centerCoords,
                    PROCESS_SIZE,
                    PROCESS_SIZE,
                    CdShape.RECTANGLE,
                    SbgnShape.RECTANGLE),
                id, PROCESS_SIZE, PORT_DISTANCE_RATIO, styleInfo);
        this.axis = axis;
    }

    /**
     * Take a celldesigner index representing the possible anchor points on a process glyph,
     * and return the corresponding coordinates, relative to the center of the process, with center at origin.
     * @param index
     * @return relative coordinates corresponding to the anchor index point
     */
    public Point2D.Float getRelativeAnchorCoords (int index) {
        float halfSize = PROCESS_SIZE / 2;
        switch(index) {
            case 0:
                float distance = (float) this.getAxis().getP1().distance(this.getAxis().getP2()) * 0.2f;
                return new Point2D.Float(-distance, 0);
            case 1:
                distance = (float) this.getAxis().getP1().distance(this.getAxis().getP2()) * 0.2f;
                return new Point2D.Float(distance, 0);
            case 2: return new Point2D.Float(0, -halfSize);
            case 3: return new Point2D.Float(0, halfSize);
            case 4: return new Point2D.Float(-halfSize, -halfSize);
            case 5: return new Point2D.Float(halfSize, -halfSize);
            case 6: return new Point2D.Float(-halfSize, halfSize);
            case 7: return new Point2D.Float(halfSize, halfSize);
        }
        throw new IllegalArgumentException("Celldesigner index for process anchor point should be an integer from 2 to 7. Was provided: "+index);
    }

    public Point2D.Float getAbsoluteAnchorCoords(int index) {
        Point2D.Float relativeCoords = getRelativeAnchorCoords(index);

        /*
         * for 0 and 1 (additional reactants and products) we need the exact point on the line of the process
         */
        if( index == 0) {
            Point2D.Float absolute;
                absolute = GeometryUtils.interpolationByRatio(this.getGlyph().getCenter(),
                        (Point2D.Float) this.getAxis().getP1(), 0.2f);
            return absolute;

        }
        else if (index == 1) {
            Point2D.Float absolute;
            absolute =  GeometryUtils.interpolationByRatio(this.getGlyph().getCenter(),
                        (Point2D.Float) this.getAxis().getP2(), 0.2f);
            return absolute;
        }
        /*
         * for other anchor points, only get relative position to the center without taking the orientation of the
         * process. This leads to inversion of the top and bottom of anchor points in the case of the process' reaction
         * going from right to left.
         */
        else {
            Point2D p2AtOrigin = new Point2D.Double(this.getAxis().getP2().getX() - this.getGlyph().getCenter().getX(),
                    this.getAxis().getP2().getY() - this.getGlyph().getCenter().getY());
            double angle = GeometryUtils.angle(new Point2D.Float(1,0), p2AtOrigin);
            AffineTransform t2 = new AffineTransform();
            t2.rotate(angle);

            Point2D.Float afterRotate = new Point2D.Float();
            t2.transform(relativeCoords, afterRotate);
            Point2D.Float absolute = new Point2D.Float(
                    (float) (afterRotate.getX() + this.getGlyph().getCenter().getX()),
                    (float) (afterRotate.getY() + this.getGlyph().getCenter().getY()));

            return absolute;
        }
    }

    public static String getSbgnClass(String reactionType) {
        switch(reactionType) {
            case "STATE_TRANSITION": return "process";
            case "KNOWN_TRANSITION_OMITTED": return "omitted process";
            case "UNKNOWN_TRANSITION": return "uncertain process";
            case "TRANSPORT": return "process";
            case "TRUNCATION": return "process";
            case "TRANSCRIPTION": return "process";
            case "TRANSLATION": return "process";
            case "HETERODIMER_ASSOCIATION": return "process";
            case "DISSOCIATION": return "process";

            /*case "CATALYSIS": return "";
            case "UNKNOWN_CATALYSIS": return "";
            case "INHIBITION": return "";
            case "UNKNOWN_INHIBITION": return "";

            case "TRANSCRIPTIONAL_ACTIVATION": return "";
            case "TRANSCRIPTIONAL_INHIBITION": return "";
            case "TRANSLATIONAL_ACTIVATION": return "";
            case "TRANSLATIONAL_INHIBITION": return "";*/

        }
        throw new IllegalArgumentException("Could not infer SBGN class from reaction type: "+reactionType);
    }

    public Line2D.Float getAxis() {
        return axis;
    }


}
