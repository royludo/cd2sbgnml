import org.sbml.x2001.ns.celldesigner.CelldesignerBaseProductDocument.CelldesignerBaseProduct;
import org.sbml.x2001.ns.celldesigner.CelldesignerBaseReactantDocument.CelldesignerBaseReactant;
import org.sbml.x2001.ns.celldesigner.CelldesignerBoundsDocument.CelldesignerBounds;
import org.sbml.x2001.ns.celldesigner.CelldesignerConnectSchemeDocument.CelldesignerConnectScheme;
import org.sbml.x2001.ns.celldesigner.CelldesignerModificationDocument.CelldesignerModification;
import org.sbml.x2001.ns.celldesigner.CelldesignerProductLinkDocument.CelldesignerProductLink;
import org.sbml.x2001.ns.celldesigner.CelldesignerReactantLinkDocument.CelldesignerReactantLink;
import org.sbml.x2001.ns.celldesigner.ReactionDocument.Reaction;

import java.awt.geom.Point2D;
import java.util.List;

public class LinkWrapper {

    private AbstractLinkableCDEntity start, end;

    private List<Point2D> pointList;
    private int index; // used to get the position in xml modifier list
    private String cdClass;
    private String sbgnClass;

    public LinkWrapper (AbstractLinkableCDEntity start, AbstractLinkableCDEntity end, List<Point2D> pointList) {
        this.start = start;
        this.end = end;
        this.pointList = pointList;
    }

    public LinkWrapper (AbstractLinkableCDEntity start, AbstractLinkableCDEntity end, List<Point2D> pointList, int index) {
        this(start, end, pointList);
        this.index = index;
    }

    public LinkWrapper (AbstractLinkableCDEntity start, AbstractLinkableCDEntity end, List<Point2D> pointList, int index, String cdClass) {
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
    public void normalizeEndPoints() {

    }


    public List<Point2D> getPointList() {
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

}
