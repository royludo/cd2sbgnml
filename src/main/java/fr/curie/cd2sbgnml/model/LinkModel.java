package fr.curie.cd2sbgnml.model;

import fr.curie.cd2sbgnml.graphics.*;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

public class LinkModel {
    GenericReactionElement start, end;
    Link link;

    public LinkModel(GenericReactionElement start, GenericReactionElement end, Link link) {
        this.start = start;
        this.end = end;
        this.link = link;
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


}
