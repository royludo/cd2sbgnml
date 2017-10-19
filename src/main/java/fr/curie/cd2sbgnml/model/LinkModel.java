package fr.curie.cd2sbgnml.model;

import fr.curie.cd2sbgnml.graphics.*;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LinkModel {
    GenericReactionElement start, end;
    Link link;
    private String id;
    private String sbgnClass;

    public LinkModel(GenericReactionElement start, GenericReactionElement end, Link link, String id, String clazz) {
        this.start = start;
        this.end = end;
        this.link = link;
        this.id = id;
        this.sbgnClass = clazz;
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



}
