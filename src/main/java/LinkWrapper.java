import org.sbml.x2001.ns.celldesigner.CelldesignerBaseProductDocument.CelldesignerBaseProduct;
import org.sbml.x2001.ns.celldesigner.CelldesignerBaseReactantDocument.CelldesignerBaseReactant;
import org.sbml.x2001.ns.celldesigner.CelldesignerBoundsDocument.CelldesignerBounds;
import org.sbml.x2001.ns.celldesigner.CelldesignerConnectSchemeDocument.CelldesignerConnectScheme;
import org.sbml.x2001.ns.celldesigner.CelldesignerModificationDocument.CelldesignerModification;
import org.sbml.x2001.ns.celldesigner.CelldesignerProductLinkDocument.CelldesignerProductLink;
import org.sbml.x2001.ns.celldesigner.CelldesignerReactantLinkDocument.CelldesignerReactantLink;
import org.sbml.x2001.ns.celldesigner.ReactionDocument.Reaction;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

public class LinkWrapper {

    private AbstractLinkableCDEntity start, end;

    private List<Point2D.Float> pointList;
    private List<Point2D.Float> sbgnSpacePointList;
    private int index; // used to get the position in xml modifier list
    private String cdClass;
    private String sbgnClass;

    public LinkWrapper (AbstractLinkableCDEntity start, AbstractLinkableCDEntity end, List<Point2D.Float> pointList) {
        this.start = start;
        this.end = end;
        this.pointList = pointList;
    }

    public LinkWrapper (AbstractLinkableCDEntity start, AbstractLinkableCDEntity end, List<Point2D.Float> pointList, int index) {
        this(start, end, pointList);
        this.index = index;
    }

    public LinkWrapper (AbstractLinkableCDEntity start, AbstractLinkableCDEntity end, List<Point2D.Float> pointList, int index, String cdClass) {
        this(start, end, pointList, index);
        this.cdClass = cdClass;
        this.sbgnClass = getSbgnClass(cdClass);
    }

    // for the base links
    public LinkWrapper (Reaction reaction) {

    }

    // for links pointing to the process
    // and links going to a logic gate pointing to a process
    public LinkWrapper (CelldesignerModification modif) {

    }

    // for additional reactants
    public LinkWrapper (CelldesignerReactantLink reactantLink) {

    }

    // for additional reactants
    public LinkWrapper (CelldesignerProductLink reactantLink) {

    }

    public String getSbgnClass(String cdClass) {
        if(cdClass.startsWith("REDUCED")) {
            cdClass = cdClass.replaceFirst("REDUCED_", "");
        }

        switch(cdClass) {
            // modifier part
            case "CATALYSIS": return "catalysis";
            case "UNKNOWN_CATALYSIS": return "unknown influence"; // TODO check if correct
            case "INHIBITION": return "inhibition";
            case "UNKNOWN_INHIBITION": return "unknown influence"; // TODO check if correct
            //case "TRANSPORT": return ""; // TODO check what to do
            //case "HETERODIMER_ASSOCIATION": return "";
            case "TRANSCRIPTIONAL_ACTIVATION": return "stimulation";
            case "TRANSCRIPTIONAL_INHIBITION": return "inhibition";
            case "TRANSLATIONAL_ACTIVATION": return "stimulation";
            case "TRANSLATIONAL_INHIBITION": return "inhibition";
            case "PHYSICAL_STIMULATION": return "stimulation";
            case "MODULATION": return "modulation";
            case "TRIGGER": return "modulation"; // TODO check if correct
            /*case "BOOLEAN_LOGIC_GATE_AND": return "";
            case "BOOLEAN_LOGIC_GATE_OR": return "";
            case "BOOLEAN_LOGIC_GATE_NOT": return "";
            case "BOOLEAN_LOGIC_GATE_UNKNOWN": return "";*/
        }
        throw new IllegalArgumentException("Could not infer SBGN class from modifier/link class: "+cdClass);
    }

    /**
     * Assuming the point list is definitively set in absolute coordinates,
     * will translate the start and end points of the link, if they point to centers,
     * to the correct perimeter point of the shape.
     */
    public List<Point2D.Float> getNormalizedEndPoints(GeometryUtils.AnchorPoint startAnchor, GeometryUtils.AnchorPoint endAnchor) {
        System.out.println("NORMALIZE");
        Point2D cdSpaceStart = this.getPointList().get(0);
        Point2D cdSpaceEnd = this.getPointList().get(this.getPointList().size() - 1);

        // normalize end
        SpeciesWrapper.CdShape startShape;
        if(this.start.getType() != AbstractLinkableCDEntity.AbstractLinkableCDEntityType.LOGIC_GATE) {
            startShape = start.getShape();
        }
        else {
            throw new RuntimeException("Logic gate not managed yet");
        }

        SpeciesWrapper.CdShape endShape;
        if(this.end.getType() != AbstractLinkableCDEntity.AbstractLinkableCDEntityType.LOGIC_GATE) {
            endShape = end.getShape();
        }
        else {
            throw new RuntimeException("Logic gate not managed yet");
        }


        List<Point2D.Float> result = new ArrayList<>();

        // normalize if needed (shape wants it, or links points at center
        if(startShape == SpeciesWrapper.CdShape.LEFT_PARALLELOGRAM
                || startShape == SpeciesWrapper.CdShape.RIGHT_PARALLELOGRAM
                || startAnchor == GeometryUtils.AnchorPoint.CENTER) {
            // normalize start
            Point2D.Float nextPoint = this.getPointList().get(1);
            Rectangle2D.Float startReactant = new Rectangle2D.Float(
                    (float) start.getCoords().getX(),
                    (float) start.getCoords().getY(),
                    start.getWidth(),
                    start.getHeight());
            Line2D.Float segment2 = new Line2D.Float(cdSpaceStart, nextPoint);
            List<Point2D.Float> intersections2 = GeometryUtils.getLineRectangleIntersection(segment2, startReactant);
            System.out.println(intersections2);
            Point2D.Float normalizedStart = GeometryUtils.getClosest(intersections2, nextPoint);
            result.add(normalizedStart);
        }
        // else just take the point already defined
        else {
            result.add(this.pointList.get(0));
        }

        for(int i=1; i < this.pointList.size() - 1; i++) {
            result.add(this.pointList.get(i));
        }

        if(endShape == SpeciesWrapper.CdShape.LEFT_PARALLELOGRAM
                || endShape == SpeciesWrapper.CdShape.RIGHT_PARALLELOGRAM
                || endAnchor == GeometryUtils.AnchorPoint.CENTER) {

            Point2D.Float previousPoint = this.getPointList().get(this.getPointList().size() - 2);
            Rectangle2D.Float endReactant = new Rectangle2D.Float(
                    (float) end.getCoords().getX(),
                    (float) end.getCoords().getY(),
                    end.getWidth(),
                    end.getHeight());
            Line2D.Float segment = new Line2D.Float(previousPoint, cdSpaceEnd);
            List<Point2D.Float> intersections = GeometryUtils.getLineRectangleIntersection(segment, endReactant);
            Point2D.Float normalizedEnd = GeometryUtils.getClosest(intersections, previousPoint);
            result.add(normalizedEnd);
        }
        else {
            result.add(this.pointList.get(this.pointList.size() - 1));
        }

        return result;
    }


    public List<Point2D.Float> getPointList() {
        return pointList;
    }

    public int getIndex() {
        return index;
    }

    public String getCdClass() {
        return cdClass;
    }

    public void setCdClass(String cdClass) {
        this.cdClass = cdClass;
    }

    public String getSbgnClass() {
        return sbgnClass;
    }

    public void setSbgnClass(String sbgnClass) {
        this.sbgnClass = sbgnClass;
    }

    public List<Point2D.Float> getSbgnSpacePointList() {
        return sbgnSpacePointList;
    }

    public void setSbgnSpacePointList(List<Point2D.Float> sbgnSpacePointList) {
        this.sbgnSpacePointList = sbgnSpacePointList;
    }


}
