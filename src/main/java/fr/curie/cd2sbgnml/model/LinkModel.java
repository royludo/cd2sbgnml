package fr.curie.cd2sbgnml.model;

import fr.curie.cd2sbgnml.graphics.AnchorPoint;
import fr.curie.cd2sbgnml.graphics.CdShape;
import fr.curie.cd2sbgnml.graphics.GeometryUtils;
import fr.curie.cd2sbgnml.graphics.Link;

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

    /**
     * Assuming the point list is definitively set in absolute coordinates,
     * will translate the start and end points of the link, if they point to centers,
     * to the correct perimeter point of the shape.
     */
    public static List<Point2D.Float> getNormalizedEndPoints(List<Point2D.Float> points,
                                                             GenericReactionElement startElement,
                                                             GenericReactionElement endElement,
                                                             AnchorPoint startAnchor,
                                                             AnchorPoint endAnchor) {
        System.out.println("NORMALIZE");
        Point2D cdSpaceStart = points.get(0);
        Point2D cdSpaceEnd = points.get(points.size() - 1);

        // normalize end
        CdShape startShape;
        if(startElement instanceof LogicGate) {
            throw new RuntimeException("Logic gate not managed yet");
        }
        else {
            startShape = startElement.getGlyph().getCdShape();
        }

        CdShape endShape;
        if(endElement instanceof LogicGate) {
            throw new RuntimeException("Logic gate not managed yet");
        }
        else {
            endShape = endElement.getGlyph().getCdShape();
        }


        List<Point2D.Float> result = new ArrayList<>();

        // normalize if needed (shape wants it, or links points at center
        if(startShape == CdShape.LEFT_PARALLELOGRAM
                || startShape == CdShape.RIGHT_PARALLELOGRAM
                || startAnchor == AnchorPoint.CENTER) {
            // normalize start
            Point2D.Float nextPoint = points.get(1);
            Rectangle2D.Float startReactant = new Rectangle2D.Float(
                    (float) startElement.getGlyph().getCenter().getX(),
                    (float) startElement.getGlyph().getCenter().getY(),
                    startElement.getGlyph().getWidth(),
                    startElement.getGlyph().getHeight());
            Line2D.Float segment2 = new Line2D.Float(cdSpaceStart, nextPoint);
            List<Point2D.Float> intersections2 = GeometryUtils.getLineRectangleIntersection(segment2, startReactant);
            System.out.println(intersections2);
            Point2D.Float normalizedStart = GeometryUtils.getClosest(intersections2, nextPoint);
            result.add(normalizedStart);
        }
        // else just take the point already defined
        else {
            result.add(points.get(0));
        }

        for(int i=1; i < points.size() - 1; i++) {
            result.add(points.get(i));
        }

        if(endShape == CdShape.LEFT_PARALLELOGRAM
                || endShape == CdShape.RIGHT_PARALLELOGRAM
                || endAnchor == AnchorPoint.CENTER) {

            Point2D.Float previousPoint = points.get(points.size() - 2);
            Rectangle2D.Float endReactant = new Rectangle2D.Float(
                    (float) endElement.getGlyph().getCenter().getX(),
                    (float) endElement.getGlyph().getCenter().getY(),
                    endElement.getGlyph().getWidth(),
                    endElement.getGlyph().getHeight());
            Line2D.Float segment = new Line2D.Float(previousPoint, cdSpaceEnd);
            List<Point2D.Float> intersections = GeometryUtils.getLineRectangleIntersection(segment, endReactant);
            Point2D.Float normalizedEnd = GeometryUtils.getClosest(intersections, previousPoint);
            result.add(normalizedEnd);
        }
        else {
            result.add(points.get(points.size() - 1));
        }

        return result;
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
