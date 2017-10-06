package fr.curie.cd2sbgnml.graphics;

import java.awt.*;
import java.awt.geom.*;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;


public class GeometryUtils {

    /**
     * Given 3 points in global map coordinate, defining an origin and 2 unit vectors,
     * returns a list of transforms that can be applied to convert local coordinates (in the space
     * defined by those 3 points) to global coordinates (same system the 3 points are provided with).

     * @return list of AffineTransform that, if applied in order on a local coordinates, yield the global coordinates.
     */
    public static List<AffineTransform> getTransformsToGlobalCoords(Point2D origin, Point2D px) {//, Point2D py) {

        Point2D originCopy = new Point2D.Double(origin.getX(), origin.getY());
        Point2D pxCopy = new Point2D.Double(px.getX(), px.getY());

        // set new coordinate system origin to center of start element
        AffineTransform t1_5 = new AffineTransform();
        t1_5.translate(-originCopy.getX(), -originCopy.getY());
        //t1_5.transform(origin, origin);
        t1_5.transform(pxCopy, pxCopy);

        //System.out.println("after set to origin: " + originCopy + " " + pxCopy);

        // get angle of destination element from X axis
        double angle = angle(new Point2D.Float(1,0), pxCopy);
        //System.out.println("angle: " + pxCopy + " " + angle);

        // rotate system to align destination on X axis
        AffineTransform t2 = new AffineTransform();
        t2.rotate(-angle);
        //t2.transform(origin, origin);
        t2.transform(pxCopy, pxCopy);

        //System.out.println("after align destination on X: " + pxCopy);

        // scale to have destination element at 1 on X
        AffineTransform t3 = new AffineTransform();
        t3.scale(1 / pxCopy.getX(), 1 / pxCopy.getX());
        //t3.transform(origin, origin);
        t3.transform(pxCopy, pxCopy);

        //System.out.println("after scale destination to 1 on X: " + pxCopy);

        List<AffineTransform> tList = new ArrayList<>();
        try {
            tList.add(t3.createInverse());
            tList.add(t2.createInverse());
            tList.add(t1_5.createInverse());
        } catch (NoninvertibleTransformException e) {
            e.printStackTrace();
        }

        return tList;
    }

    public static List<AffineTransform> getTransformsToGlobalCoords(Point2D origin, Point2D px, Point2D py) {

        // copy points to avoid altering them
        Point2D originCopy = new Point2D.Double(origin.getX(), origin.getY());
        Point2D pxCopy = new Point2D.Double(px.getX(), px.getY());
        Point2D pyCopy = new Point2D.Double(py.getX(), py.getY());

        // set new coordinate system origin to center of start element
        AffineTransform t1 = new AffineTransform();
        t1.translate(-originCopy.getX(), -originCopy.getY());
        t1.transform(pxCopy, pxCopy);
        t1.transform(pyCopy, pyCopy);

        //System.out.println("after set to origin: " + pxCopy+" "+pyCopy);

        // get angle of destination element from X axis
        // see https://stackoverflow.com/a/2150475
        double angle = angle(new Point2D.Float(1,0), pxCopy);
        //System.out.println("angle to X axis: " + pxCopy + " " + angle);

        // rotate system to align destination on X axis
        AffineTransform t2 = new AffineTransform();
        t2.rotate(-angle);
        t2.transform(pxCopy, pxCopy);
        t2.transform(pyCopy, pyCopy);

        //System.out.println("after align destination on X:  "+ pxCopy+" "+pyCopy);

        double shearFactor = pyCopy.getX() / pyCopy.getY();

        // shear to put the Y axis perpendicular
        AffineTransform t3 = new AffineTransform();
        t3.shear(-shearFactor, 0);
        t3.transform(pxCopy, pxCopy);
        t3.transform(pyCopy, pyCopy);

        //System.out.println("after shear X axis to align Y: " + pxCopy+" "+pyCopy);

        // scale to have both axis at 1
        AffineTransform t4 = new AffineTransform();
        t4.scale(1 / pxCopy.getX(), 1 / pyCopy.getY());
        t4.transform(pxCopy, pxCopy);
        t4.transform(pyCopy, pyCopy);

        //System.out.println("after scale destination to 1 on X: " + pxCopy+" "+pyCopy);


        List<AffineTransform> tList = new ArrayList<>();
        try {
            tList.add(t4.createInverse());
            tList.add(t3.createInverse());
            tList.add(t2.createInverse());
            tList.add(t1.createInverse());
        } catch (NoninvertibleTransformException e) {
            e.printStackTrace();
        }

        return tList;
    }

    /**
     * return the signed angle in radian between 2 vectors at origin
     * @param v1
     * @param v2
     * @return signed angle in radian
     */
    public static double angle(Point2D v1, Point2D v2) {
        return Math.atan2(
                v1.getX() * v2.getY()
                        - v1.getY() * v2.getX(),
                v1.getX() * v2.getX()
                        + v1.getY() * v2.getY() );
        //return Math.atan2(v2.getY(), v2.getX()) - Math.atan2(v1.getY(), v1.getX());
    }

    /**
     *
     * @return angle in degree starting from positive X axis (= East)
     */
    public static float perimeterAnchorPointToAngle(AnchorPoint anchorPoint) {
        switch(anchorPoint){
            case N: return 90; //0;
            case NNE: return 67.5f; //22.5f;
            case NE: return 45;
            case ENE: return 22.5f; //67.5f;
            case E: return 0; //90;
            case ESE: return -22.5f; //337.5f; //112.5f;
            case SE: return -45; //315; //135;
            case SSE: return -67.5f; //292.5f; //157.5f;
            case S: return -90; //270; //180;
            case SSW: return -112.5f; //247.5f; //202.5f;
            case SW: return -135; //225;
            case WSW: return -157.5f; //202.5f; //247.5f;
            case W: return 180; //270;
            case WNW: return 157.5f; //292.5f;
            case NW: return 135; //315;
            case NNW: return 112.5f; //337.5f;
            case CENTER: throw new RuntimeException("Cannot infer angle from link starting at center");
        }
        throw new RuntimeException("Unexpected error, should not be able to reach this point.");
    }

    public static Point2D.Float getRelativeRectangleAnchorPosition(AnchorPoint anchorPoint, float width, float height){
        Point.Float pl = new Point.Float();
        switch(anchorPoint) {
            case E:
                pl.x = 0.5f * width;
                pl.y = 0;
                break;
            case ENE:
                pl.x = 0.5f * width;
                pl.y = 0.25f * height;
                break;
            case NE:
                pl.x = 0.5f * width;
                pl.y = 0.5f * height;
                break;
            case ESE:
                pl.x = 0.5f * width;
                pl.y = -0.25f * height;
                break;
            case SE:
                pl.x = 0.5f * width;
                pl.y = -0.5f * height;
                break;
            case W:
                pl.x = -0.5f * width;
                pl.y = 0;
                break;
            case WNW:
                pl.x = -0.5f * width;
                pl.y = 0.25f * height;
                break;
            case NW:
                pl.x = -0.5f * width;
                pl.y = 0.5f * height;
                break;
            case WSW:
                pl.x = -0.5f * width;
                pl.y = -0.25f * height;
                break;
            case SW:
                pl.x = -0.5f * width;
                pl.y = -0.5f * height;
                break;
            case N:
                pl.x = 0;
                pl.y = 0.5f * height;
                break;
            case NNW:
                pl.x = -0.25f * width;
                pl.y = 0.5f * height;
                break;
            case NNE:
                pl.x = 0.25f * width;
                pl.y = 0.5f * height;
                break;
            case S:
                pl.x = 0;
                pl.y = -0.5f * height;
                break;
            case SSW:
                pl.x = -0.25f * width;
                pl.y = -0.5f * height;
                break;
            case SSE:
                pl.x = 0.25f * width;
                pl.y = -0.5f * height;
                break;
        }
        // all that is given in a coordinate system where Y points up.
        // but it always points down for us.
        return new Point2D.Float((float)pl.getX(), (float)-pl.getY());
    }

    /**
     * angle of the right and left "arrows" of the phenotype are fixed (45°)
     * the diagonals on the left and right are the diagonals of 4 squares of side h/2
     * @param width
     * @param height
     * @return
     */
    public static Point2D.Float getRelativePhenotypeAnchorPosition(AnchorPoint anchorPoint, float width, float height){

        float halfW = 0.5f * width;
        float halfH = 0.5f * height;
        float quartH = 0.25f * height;

        Point.Float pl = new Point.Float();
        switch(anchorPoint) {
            case E:
                pl.x = halfW;
                pl.y = 0;
                break;
            case ENE:
                pl.x = halfW - quartH;
                pl.y = quartH;
                break;
            case NE:
                pl.x = halfW - halfH;
                pl.y = halfH;
                break;
            case ESE:
                pl.x = halfW - quartH;
                pl.y = -quartH;
                break;
            case SE:
                pl.x = halfW - halfH;
                pl.y = -halfH;
                break;
            case W:
                pl.x = -halfW;
                pl.y = 0;
                break;
            case WNW:
                pl.x = quartH - halfW;
                pl.y = quartH;
                break;
            case NW:
                pl.x = halfH - halfW;
                pl.y = halfH;
                break;
            case WSW:
                pl.x = quartH - halfW;
                pl.y = -quartH;
                break;
            case SW:
                pl.x = halfH - halfW;
                pl.y = -halfH;
                break;
            case N:
                pl.x = 0;
                pl.y = halfH;
                break;
            case NNW:
                pl.x = 0.5f * (halfH - halfW);
                pl.y = halfH;
                break;
            case NNE:
                pl.x = 0.5f * (halfW - halfH);
                pl.y = halfH;
                break;
            case S:
                pl.x = 0;
                pl.y = -halfH;
                break;
            case SSW:
                pl.x = 0.5f * (halfH - halfW);
                pl.y = -halfH;
                break;
            case SSE:
                pl.x = 0.5f * (halfW - halfH);
                pl.y = -halfH;
                break;
        }
        // all that is given in a coordinate system where Y points up.
        // but it always points down for us.
        return new Point2D.Float((float)pl.getX(), (float)-pl.getY());
    }

    /**
     * right and left side also have 45° slope
     * @param anchorPoint
     * @param width
     * @param height
     * @return
     */
    public static Point2D.Float getRelativeRightParallelogramAnchorPosition(AnchorPoint anchorPoint, float width, float height){

        float halfW = 0.5f * width;
        float halfH = 0.5f * height;
        float quartH = 0.25f * height;

        Point.Float pl = new Point.Float();
        switch(anchorPoint) {
            case E:
                pl.x = halfW - halfH;
                pl.y = 0;
                break;
            case ENE:
                pl.x = halfW - quartH;
                pl.y = quartH;
                break;
            case NE:
                pl.x = halfW;
                pl.y = halfH;
                break;
            case ESE:
                pl.x = halfW - height + quartH;
                pl.y = -quartH;
                break;
            case SE:
                pl.x = halfW - height;
                pl.y = -halfH;
                break;
            case W:
                pl.x = halfH - halfW;
                pl.y = 0;
                break;
            case WNW:
                pl.x = height - halfW - quartH;
                pl.y = quartH;
                break;
            case NW:
                pl.x = height - halfW;
                pl.y = halfH;
                break;
            case WSW:
                pl.x = quartH - halfW;
                pl.y = -quartH;
                break;
            case SW:
                pl.x = -halfW;
                pl.y = -halfH;
                break;
            case N:
                pl.x = -halfW + height + 0.5f * (width - height);
                pl.y = halfH;
                break;
            case NNW:
                pl.x = -halfW + height + 0.25f * (width - height);
                pl.y = halfH;
                break;
            case NNE:
                pl.x = halfW - 0.25f * (width - height);
                pl.y = halfH;
                break;
            case S:
                pl.x = halfW - height - 0.5f * (width - height);
                pl.y = -halfH;
                break;
            case SSW:
                pl.x = -halfW + 0.25f * (width - height);
                pl.y = -halfH;
                break;
            case SSE:
                pl.x = halfW - height - 0.25f * (width - height);
                pl.y = -halfH;
                break;
        }
        // all that is given in a coordinate system where Y points up.
        // but it always points down for us.
        return new Point2D.Float((float)pl.getX(), (float)-pl.getY());
    }

    public static Point2D.Float getRelativeLeftParallelogramAnchorPosition(AnchorPoint anchorPoint, float width, float height){

        float halfW = 0.5f * width;
        float halfH = 0.5f * height;
        float quartH = 0.25f * height;

        Point.Float pl = new Point.Float();
        switch(anchorPoint) {
            case E:
                pl.x = halfW - halfH;
                pl.y = 0;
                break;
            case ENE:
                pl.x = halfW - height + quartH;
                pl.y = quartH;
                break;
            case NE:
                pl.x = halfW - height;
                pl.y = halfH;
                break;
            case ESE:
                pl.x = halfW - quartH;
                pl.y = -quartH;
                break;
            case SE:
                pl.x = halfW;
                pl.y = -halfH;
                break;
            case W:
                pl.x = halfH - halfW;
                pl.y = 0;
                break;
            case WNW:
                pl.x = -halfW + quartH;
                pl.y = quartH;
                break;
            case NW:
                pl.x = -halfW;
                pl.y = halfH;
                break;
            case WSW:
                pl.x = height - halfW - quartH;
                pl.y = -quartH;
                break;
            case SW:
                pl.x = -halfW + height;
                pl.y = -halfH;
                break;
            case N:
                pl.x = -halfW + 0.5f * (width - height);
                pl.y = halfH;
                break;
            case NNW:
                pl.x = 0.25f * (-width - height);
                pl.y = halfH;
                break;
            case NNE:
                pl.x = -halfW + 0.75f * (width - height);
                pl.y = halfH;
                break;
            case S:
                pl.x = halfW - 0.5f * (width - height);
                pl.y = -halfH;
                break;
            case SSW:
                pl.x = halfW - 0.75f * (width - height);
                pl.y = -halfH;
                break;
            case SSE:
                pl.x = 0.25f * (width + height);
                pl.y = -halfH;
                break;
        }
        // all that is given in a coordinate system where Y points up.
        // but it always points down for us.
        return new Point2D.Float((float)pl.getX(), (float)-pl.getY());
    }

    /**
     * Assuming the ellipse is aligned to axis and center of ellipse is 0,0
     * @param bboxWidth
     * @param bboxHeight
     * @param deg
     * @return
     */
    public static Point2D.Float ellipsePerimeterPointFromAngle(float bboxWidth, float bboxHeight, float deg) {
        double theta = deg * Math.PI / 180;
        return new Point2D.Float(
                (float) ((bboxWidth / 2) * Math.cos(theta)),
                (float) (-(bboxHeight / 2) * Math.sin(theta)));
    }

    /**
     * return between 0 and 4 points
     * @param line
     * @param rect
     * @return
     */
    public static List<Point2D.Float> getLineRectangleIntersection(Line2D.Float line, Rectangle2D.Float rect) {
        Point2D.Float p1 = new Point2D.Float(
                (float) (rect.getX() - rect.getWidth() / 2),
                (float) (rect.getY() - rect.getHeight() / 2));
        Point2D.Float p2 = new Point2D.Float(
                (float) (rect.getX() + rect.getWidth() / 2),
                (float) (rect.getY() - rect.getHeight() / 2));
        Point2D.Float p3 = new Point2D.Float(
                (float) (rect.getX() + rect.getWidth() / 2),
                (float) (rect.getY() + rect.getHeight() / 2));
        Point2D.Float p4 = new Point2D.Float(
                (float) (rect.getX() - rect.getWidth() / 2),
                (float) (rect.getY() + rect.getHeight() / 2));
        Line2D.Float l1 = new Line2D.Float(p1, p2);
        Line2D.Float l2 = new Line2D.Float(p2, p3);
        Line2D.Float l3 = new Line2D.Float(p3, p4);
        Line2D.Float l4 = new Line2D.Float(p4, p1);
        System.out.println(rect+" -- "+line);
        Point2D.Float i1 = getLineLineIntersection(line, l1);
        Point2D.Float i2 = getLineLineIntersection(line, l2);
        Point2D.Float i3 = getLineLineIntersection(line, l3);
        Point2D.Float i4 = getLineLineIntersection(line, l4);

        List<Point2D.Float> result = new ArrayList<>();
        // if parallele lines, we get Nan and infinity
        if(i1 != null && isDefinedAndFinite(i1.getX()) && isDefinedAndFinite(i1.getY())) {
            result.add(i1);
        }
        if(i2 != null && isDefinedAndFinite(i2.getX()) && isDefinedAndFinite(i2.getY())) {
            result.add(i2);
        }
        if(i3 != null && isDefinedAndFinite(i3.getX()) && isDefinedAndFinite(i3.getY())) {
            result.add(i3);
        }
        if(i4 != null && isDefinedAndFinite(i4.getX()) && isDefinedAndFinite(i4.getY())) {
            result.add(i4);
        }

        return result;
    }

    public static boolean isDefinedAndFinite(double n) {
        return Double.isFinite(n) && !Double.isNaN(n);
    }

    /**
     * https://stackoverflow.com/a/5185725
     * return intersection even if out of segment. It extends segments.
     * @param line1
     * @param line2
     * @return
     */
    public static Point2D.Float getLineLineIntersection(Line2D.Float line1, Line2D.Float line2) {

        final double x1,y1, x2,y2, x3,y3, x4,y4;
        x1 = line1.x1; y1 = line1.y1; x2 = line1.x2; y2 = line1.y2;
        x3 = line2.x1; y3 = line2.y1; x4 = line2.x2; y4 = line2.y2;
        final double x = (
                (x2 - x1)*(x3*y4 - x4*y3) - (x4 - x3)*(x1*y2 - x2*y1)
        ) /
                (
                        (x1 - x2)*(y3 - y4) - (y1 - y2)*(x3 - x4)
                );
        final double y = (
                (y3 - y4)*(x1*y2 - x2*y1) - (y1 - y2)*(x3*y4 - x4*y3)
        ) /
                (
                        (x1 - x2)*(y3 - y4) - (y1 - y2)*(x3 - x4)
                );


        if(line1.intersectsLine(line2)) {
            return new Point2D.Float((float)x, (float)y);
        }

        return null;
    }

    /**
     * return the point from the list that is the closest to Point p
     * @param pointList
     * @param ref
     * @return
     */
    public static Point2D.Float getClosest(List<Point2D.Float> pointList, Point2D.Float ref) {
        if(pointList.size() == 0) {
            throw new IllegalArgumentException("Point list should not be empty.");
        }

        // init with first element
        double minDist = pointList.get(0).distance(ref);
        Point2D.Float closest = pointList.get(0);

        for(int i=1; i < pointList.size(); i++) {
            if(pointList.get(i).distance(ref) < minDist) {
                closest = pointList.get(i);
            }
        }

        return closest;
    }

    /**
     * Apply a list of affine transforms to a list of points.
     * Used to change the coordinate system of a set of points.
     * @param points a list of 2D coordinates
     * @param transforms a list of transforms to be applied
     * @return a new list of points
     */
    public static List<Point2D.Float> convertPoints(List<Point2D.Float> points, List<AffineTransform> transforms) {

        List<Point2D.Float> convertedPoints = new ArrayList<>();
        for (Point2D editP : points) {
            Point2D p = new Point2D.Double(editP.getX(), editP.getY());

            for(AffineTransform t: transforms) {
                t.transform(p, p);
            }

            System.out.println("result: " + editP + " -> " + p.toString());
            convertedPoints.add(new Point2D.Float((float) p.getX(), (float) p.getY()));

        }
        return convertedPoints;
    }

    public static Point2D.Float getMiddle(Point2D.Float p1, Point2D.Float p2) {
        return new Point2D.Float(
                p1.x + ((p2.x - p1.x) / 2),
                p1.y + ((p2.y - p1.y) / 2));
    }

    /**
     * Given a polyline defined by a list of points, and a segment index (starting at 0 for the first segment of the
     * polyline), returns the middle of the specified segment.
     * Polyline has to be valid, point list has to have at least 2 points.
     * @param points
     * @param segment
     * @return
     */
    public static Point2D.Float getMiddleOfPolylineSegment(List<Point2D.Float> points, int segment) {
        if(points.size() < 2) {
            throw new IllegalArgumentException("Polyline needs to have at least 2 points, "+ points.size()+" points provided.");
        }
        if(segment < 0 || segment > points.size() - 1) {
            throw new IllegalArgumentException("segment has to be between 0 and polyline segment count, "+segment+" was provided.");
        }

        Point2D.Float p1 = points.get(segment);
        Point2D.Float p2 = points.get(segment + 1);
        System.out.println("middle of "+p1+" "+p2+" -> "+getMiddle(p1, p2));
        return getMiddle(p1, p2);
    }

    public static AbstractMap.SimpleEntry<List<Point2D.Float>, List<Point2D.Float>> splitPolylineAtSegment(List<Point2D.Float> points, int segment) {
        if(points.size() < 2) {
            throw new IllegalArgumentException("Polyline needs to have at least 2 points, "+ points.size()+" points provided.");
        }
        if(segment < 0 || segment > points.size() - 1) {
            throw new IllegalArgumentException("segment has to be between 0 and polyline segment count, "+segment+" was provided.");
        }

        List<Point2D.Float> subLinkPoints1 = new ArrayList<>();
        List<Point2D.Float> subLinkPoints2 = new ArrayList<>();
        List<Point2D.Float> currentSubLink = subLinkPoints1;

        for(int i=0; i < points.size() - 1; i++) {
            System.out.println("i: "+i);
            Point2D.Float currentStartPoint = points.get(i);
            Point2D.Float currenEndPoint = points.get(i + 1);

            currentSubLink.add(currentStartPoint);

            if(i == segment) { // split this segment in 2
                System.out.println("process segment");
                Point2D.Float middle = getMiddle(currentStartPoint, currenEndPoint);
                System.out.println(currentStartPoint+" "+middle+" "+currenEndPoint);
                currentSubLink.add(middle);
                currentSubLink = subLinkPoints2;
                currentSubLink.add(middle);
            }

            if(i == points.size() - 2) {
                currentSubLink.add(currenEndPoint);
            }

        }

        System.out.println(subLinkPoints1+" //// "+subLinkPoints2);

        return new AbstractMap.SimpleEntry<>(subLinkPoints1, subLinkPoints2);
    }

    /**
     * Assuming the point list is definitively set in absolute coordinates,
     * will translate the start and end points of the link, if they point to centers,
     * to the correct perimeter point of the shape.
     */
    public static List<Point2D.Float> getNormalizedEndPoints(List<Point2D.Float> points,
                                                             Glyph startGlyph,
                                                             Glyph endGlyph,
                                                             AnchorPoint startAnchor,
                                                             AnchorPoint endAnchor) {
        System.out.println("NORMALIZE points: " + points);
        Point2D.Float cdSpaceStart = points.get(0);
        Point2D.Float cdSpaceEnd = points.get(points.size() - 1);

        // normalize end
        CdShape startShape = startGlyph.getCdShape();
        CdShape endShape = endGlyph.getCdShape();


        List<Point2D.Float> result = new ArrayList<>();

        // normalize if needed (shape wants it, or links points at center
        if(startShape == CdShape.LEFT_PARALLELOGRAM
                || startShape == CdShape.RIGHT_PARALLELOGRAM
                || startAnchor == AnchorPoint.CENTER) {
            // normalize start
            Point2D.Float nextPoint = points.get(1);
            Rectangle2D.Float startReactant = new Rectangle2D.Float(
                    (float) startGlyph.getCenter().getX(),
                    (float) startGlyph.getCenter().getY(),
                    startGlyph.getWidth(),
                    startGlyph.getHeight());
            Line2D.Float segment2 = new Line2D.Float(cdSpaceStart, nextPoint);
            System.out.println("Intersect segement: "+segment2.getP1()+" "+segment2.getP2()+" with rectangle "+startReactant);
            List<Point2D.Float> intersections2 = GeometryUtils.getLineRectangleIntersection(segment2, startReactant);
            System.out.println("INTERSECTION START"+intersections2);
            if(intersections2.isEmpty()) {
                result.add(cdSpaceStart);
            }
            else {
                Point2D.Float normalizedStart = GeometryUtils.getClosest(intersections2, nextPoint);
                result.add(normalizedStart);
            }
        }
        // else just take the point already defined
        else {
            result.add(points.get(0));
        }
        System.out.println("normalized start: " + result);

        for(int i=1; i < points.size() - 1; i++) {
            result.add(points.get(i));
        }
        System.out.println("middle points: " + result);

        if(endShape == CdShape.LEFT_PARALLELOGRAM
                || endShape == CdShape.RIGHT_PARALLELOGRAM
                || endAnchor == AnchorPoint.CENTER) {

            Point2D.Float previousPoint = points.get(points.size() - 2);
            Rectangle2D.Float endReactant = new Rectangle2D.Float(
                    (float) endGlyph.getCenter().getX(),
                    (float) endGlyph.getCenter().getY(),
                    endGlyph.getWidth(),
                    endGlyph.getHeight());
            Line2D.Float segment = new Line2D.Float(previousPoint, cdSpaceEnd);
            System.out.println("Intersect segement: "+segment+" with rectangle "+endReactant);
            List<Point2D.Float> intersections = GeometryUtils.getLineRectangleIntersection(segment, endReactant);
            System.out.println("INTERSECTIONS "+intersections);
            if(intersections.isEmpty()) {
                result.add(cdSpaceEnd);
            }
            else {
                Point2D.Float normalizedEnd = GeometryUtils.getClosest(intersections, previousPoint);
                System.out.println("normalized end: "+normalizedEnd);
                result.add(normalizedEnd);
            }
        }
        else {
            result.add(cdSpaceEnd);
        }

        System.out.println("NORMALIZE RESULT: " + result);

        return result;
    }
}
