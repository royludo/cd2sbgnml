package fr.curie.cd2sbgnml.model;

import fr.curie.cd2sbgnml.graphics.*;
import fr.curie.cd2sbgnml.xmlcdwrappers.StyleInfo;

import org.sbgn.ArcClazz;

import java.awt.geom.Point2D;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A geometric link with more information about the reaction it is involved in, like its source and target
 * elements, its style and its class (determining arrow shapes).
 */
public class LinkModel {
    GenericReactionElement start, end;
    Link link;
    private String id;
    private String sbgnClass;
    private StyleInfo styleInfo;

    /**
     * We need to keep the information if a link has been reversed, so we can set it to the correct port of a process.
     * For example, if we reverse a consumption, a production is now starting at the IN port, which isn't the supposed
     * way a production arc should go.
     * TODO we probably should simply store where a link is pointing to, semantically (the glyph or its ports), not
     * TODO just as a point.
     */
    private boolean reversed = false;

    public LinkModel(GenericReactionElement start, GenericReactionElement end, Link link, String id, String clazz, StyleInfo styleInfo) {
        this.start = start;
        this.end = end;
        this.link = link;
        this.id = id;
        this.sbgnClass = clazz;
        this.styleInfo = styleInfo;
    }

    public static String getSbgnClass(String reactionType) {
        switch(reactionType) {
            case "STATE_TRANSITION": return "production";
            case "KNOWN_TRANSITION_OMITTED": return "production";
            case "UNKNOWN_TRANSITION": return "production";
            case "TRANSPORT": return "production";
            case "TRUNCATION": return "production";
            case "TRANSCRIPTION": return "production";
            case "TRANSLATION": return "production";
            case "HETERODIMER_ASSOCIATION": return "production";
            case "DISSOCIATION": return "production";

            case "CATALYSIS": return "catalysis";
            case "UNKNOWN_CATALYSIS": return "catalysis";
            case "INHIBITION": return "inhibition";
            case "UNKNOWN_INHIBITION": return "inhibition";

            case "TRANSCRIPTIONAL_ACTIVATION": return "production"; // TODO check these 4
            case "TRANSCRIPTIONAL_INHIBITION": return "production";
            case "TRANSLATIONAL_ACTIVATION": return "production";
            case "TRANSLATIONAL_INHIBITION": return "production";

            case "PHYSICAL_STIMULATION": return "stimulation";
            case "MODULATION": return "modulation";
            case "TRIGGER": return "necessary stimulation";

            /*case "BOOLEAN_LOGIC_GATE_AND": return "";
            case "BOOLEAN_LOGIC_GATE_OR": return "";
            case "BOOLEAN_LOGIC_GATE_NOT": return "";
            case "BOOLEAN_LOGIC_GATE_UNKNOWN": return "";*/

            // some direct connection types
            case "NEGATIVE_INFLUENCE": return "inhibition";
            case "POSITIVE_INFLUENCE": return "stimulation";
            case "REDUCED_MODULATION": return "modulation";
            case "REDUCED_PHYSICAL_STIMULATION": return "stimulation";
            case "REDUCED_TRIGGER": return "necessary stimulation";
            case "UNKNOWN_NEGATIVE_INFLUENCE": return "inhibition";
            case "UNKNOWN_POSITIVE_INFLUENCE": return "stimulation";
            case "UNKNOWN_REDUCED_MODULATION": return "modulation";
            case "UNKNOWN_REDUCED_TRIGGER": return "necessary stimulation";
            case "UNKNOWN_REDUCED_PHYSICAL_STIMULATION": return "stimulation";

        }
        throw new IllegalArgumentException("Could not infer SBGN class from reaction type: "+reactionType);
    }

    public static String getCdClass(ArcClazz sbgnClass) {
        switch(sbgnClass) {
            case CONSUMPTION:
            case PRODUCTION:
            case CATALYSIS:
                return "CATALYSIS";
            case INHIBITION:
                return "INHIBITION";
            case MODULATION:
                return "MODULATION";
            case STIMULATION:
                return "PHYSICAL_STIMULATION";
            case NECESSARY_STIMULATION:
                return "TRIGGER";
        }
        throw new IllegalArgumentException("Could not infer CellDesigner class from SBGN arc class: "+sbgnClass);
    }

    public static String getReducedCdClass(ArcClazz sbgnClass) {
        switch(sbgnClass) {
            case CATALYSIS:
                return "CATALYSIS";
            case INHIBITION:
                return "NEGATIVE_INFLUENCE";
            case MODULATION:
                return "REDUCED_MODULATION";
            case STIMULATION:
                return "POSITIVE_INFLUENCE";
            case NECESSARY_STIMULATION:
                return "REDUCED_TRIGGER";
            case EQUIVALENCE_ARC:
                return "POSITIVE_INFLUENCE";
        }
        throw new IllegalArgumentException("Could not infer CellDesigner class from SBGN arc class: "+sbgnClass);
    }

    /**
     * Modify in place the LinkModel, set it as production if it was consumption, and vice versa.
     * Used for reversible reactions.
     */
    public void reverse() {

        // reverse linked elements
        GenericReactionElement orig_start = this.getStart();
        this.start = this.end;
        this.end = orig_start;

        // reverse link direction and edit points
        Point2D.Float newStart = this.getLink().getEnd();
        Point2D.Float newEnd = this.getLink().getStart();
        List<Point2D.Float> shallowCopy = this.getLink().getEditPoints().subList(
                0, this.getLink().getEditPoints().size());
        Collections.reverse(shallowCopy);
        List<Point2D.Float> newEditPoints = new ArrayList<>();
        newEditPoints.add(newStart);
        newEditPoints.addAll(shallowCopy);
        newEditPoints.add(newEnd);

        this.link = new Link(newEditPoints);

        if(this.getSbgnClass().equals("production")) {
            this.sbgnClass = "consumption";
        }
        else if(this.getSbgnClass().equals("consumption")) {
            this.sbgnClass = "production";
        }

        // change reverse flag
        this.reversed = ! this.reversed;

    }

    /**
     * Merge 2 links together, and create a new LinkModel as a result.
     * It is assumed that data are consistent between the 2 merged links (same style, end of the first is
     * more or less the same as start of the 2nd, same middle element...)
     *
     * @param m2
     * @return
     */
    public LinkModel mergeWith(LinkModel m2, String newClass, String newId) {
        List<Point2D.Float> newPoints = new ArrayList<>();

        newPoints.add(this.getLink().getStart());
        newPoints.addAll(this.getLink().getEditPoints());
        newPoints.add(this.getLink().getEnd());

        newPoints.addAll(m2.getLink().getEditPoints());
        newPoints.add(m2.getLink().getEnd());

        Link newLink = new Link(newPoints);
        return new LinkModel(this.getStart(), m2.getEnd(),
                newLink, newId, newClass, new StyleInfo(this.getStyleInfo(), newId));
    }

    public GenericReactionElement getStart() {
        return start;
    }

    public GenericReactionElement getEnd() {
        return end;
    }

    public Link getLink() {
        return link;
    }

    public String getId() {
        return id;
    }

    public String getSbgnClass() {
        return sbgnClass;
    }

    public StyleInfo getStyleInfo() {
        return styleInfo;
    }


    public boolean isReversed() {
        return reversed;
    }
}
