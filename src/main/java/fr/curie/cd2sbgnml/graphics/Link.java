package fr.curie.cd2sbgnml.graphics;


import java.awt.geom.Point2D;

import java.util.ArrayList;
import java.util.List;

/**
 * Something that links glyphs together. Only defines a set of point.
 */
public class Link {

    private Point2D.Float start;
    private Point2D.Float end;
    private List<Point2D.Float> editPoints;

    public Link(List<Point2D.Float> pointList) {
        this.start = pointList.get(0);
        this.editPoints = new ArrayList<>();
        for(int i=1; i < pointList.size() - 1; i++) {
            this.editPoints.add(pointList.get(i));
        }
        this.end = pointList.get(pointList.size() - 1);
    }

    public List<Point2D.Float> getAllPoints() {
        List<Point2D.Float> result = new ArrayList<>();
        result.add(this.start);
        result.addAll(this.editPoints);
        result.add(this.end);
        return result;
    }

    public Point2D.Float getStart() {
        return start;
    }

    public Point2D.Float getEnd() {
        return end;
    }

    public List<Point2D.Float> getEditPoints() {
        return editPoints;
    }
}
