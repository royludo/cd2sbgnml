package fr.curie.cd2sbgnml.graphics;

import org.junit.Before;
import org.junit.Test;

import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class GeometryUtilsTest {

    Point2D.Float p0;
    Point2D.Float p10;
    Point2D.Float pneg5;
    Point2D.Float psmall;
    List<Point2D.Float> poly1;
    List<Point2D.Float> poly4;
    Point2D.Float pY50;
    Glyph square10AtOrigin, square10AtX50, square10At5050;
    List<Point2D.Float> directp0p50;
    List<Point2D.Float> _3segmentp0p50;
    Line2D.Float Xaxis, Yaxis, positiveAxisDiag, positiveInverseDiagAt3, inverseDiagAt50;
    Rectangle2D.Float rectAtOrigin;
    Point2D.Float p2010, p2050;

    @Before
    public void setUp() {
        p0 = new Point2D.Float(0,0);
        p10 = new Point2D.Float(10, 10);
        pneg5 = new Point2D.Float(-5, -5);
        psmall = new Point2D.Float(0.00002f, 0.00002f);
        poly1 = Arrays.asList(p0, p10);
        poly4 = Arrays.asList(p0, p10,
                new Point2D.Float(15, 5),
                new Point2D.Float(15, 20),
                new Point2D.Float(10, 20));
        square10AtOrigin = new Glyph(p0, 10, 10, CdShape.RECTANGLE, SbgnShape.RECTANGLE);
        pY50 = new Point2D.Float(0, 50);
        square10AtX50 = new Glyph(pY50, 10, 10, CdShape.RECTANGLE, SbgnShape.RECTANGLE);
        directp0p50 = Arrays.asList(p0, pY50);
        _3segmentp0p50 = Arrays.asList(p0,
                new Point2D.Float(20, 20),
                new Point2D.Float(20, 30),
                pY50);
        Xaxis = new Line2D.Float(p0, new Point2D.Float(1,0));
        Yaxis = new Line2D.Float(p0, new Point2D.Float(0,1));
        positiveAxisDiag = new Line2D.Float(p0, new Point2D.Float(1,1));
        positiveInverseDiagAt3 = new Line2D.Float(new Point2D.Float(0,3), new Point2D.Float(3,0));
        inverseDiagAt50 = new Line2D.Float(new Point2D.Float(50,0), new Point2D.Float(0, 50));
        rectAtOrigin = new Rectangle2D.Float(0, 0, 10, 10);
        p2010 = new Point2D.Float(20, 10);
        p2050 = new Point2D.Float(20, 50);

    }


    //    <<<<< START getMiddle >>>>>

    @Test
    public void getMiddleFromOrigin() {
        Point2D.Float m = GeometryUtils.getMiddle(p0, p10);
        Point2D.Float res = new Point2D.Float(5, 5);
        assertEquals(res, m);
    }

    @Test
    public void getMiddleFromOriginNeg() {
        Point2D.Float m = GeometryUtils.getMiddle(p0, pneg5);
        Point2D.Float res = new Point2D.Float(-2.5f, -2.5f);
        assertEquals(res, m);
    }

    @Test
    public void getMiddleFromOriginSmall() {
        Point2D.Float m = GeometryUtils.getMiddle(p0, psmall);
        Point2D.Float res = new Point2D.Float(0.00001f, 0.00001f);
        assertEquals(res, m);
    }

    @Test
    public void getMiddleFromAnywhere() {
        Point2D.Float m = GeometryUtils.getMiddle(p10, pneg5);
        Point2D.Float res = new Point2D.Float(2.5f, 2.5f);
        assertEquals(res, m);
    }

    @Test
    public void getMiddleFromSamePoints() {
        Point2D.Float m = GeometryUtils.getMiddle(p10, p10);
        Point2D.Float res = new Point2D.Float(10f, 10f);
        assertEquals(res, m);
    }

    //    <<<<< END getMiddle >>>>>

    //    <<<<< START getMiddleOfPolylineSegment >>>>>

    @Test
    public void getMiddleOfPolylineSegment1Segment() {
        Point2D.Float m = GeometryUtils.getMiddleOfPolylineSegment(poly1, 0);
        Point2D.Float res = new Point2D.Float(5, 5);
        assertEquals(res, m);
    }

    @Test
    public void getMiddleOfPolylineSegment4SegmentsIndex0() {
        Point2D.Float m = GeometryUtils.getMiddleOfPolylineSegment(poly4, 0);
        Point2D.Float res = new Point2D.Float(5, 5);
        assertEquals(res, m);
    }

    @Test
    public void getMiddleOfPolylineSegment4SegmentsIndex1() {
        Point2D.Float m = GeometryUtils.getMiddleOfPolylineSegment(poly4, 1);
        Point2D.Float res = new Point2D.Float(12.5f, 7.5f);
        assertEquals(res, m);
    }

    @Test
    public void getMiddleOfPolylineSegment4SegmentsIndex2() {
        Point2D.Float m = GeometryUtils.getMiddleOfPolylineSegment(poly4, 2);
        Point2D.Float res = new Point2D.Float(15f, 12.5f);
        assertEquals(res, m);
    }

    @Test
    public void getMiddleOfPolylineSegment4SegmentsIndex3() {
        Point2D.Float m = GeometryUtils.getMiddleOfPolylineSegment(poly4, 3);
        Point2D.Float res = new Point2D.Float(12.5f, 20f);
        assertEquals(res, m);
    }

    @Test(expected = RuntimeException.class)
    public void getMiddleOfPolylineSegment4SegmentsIndexTooHigh() {
        Point2D.Float m = GeometryUtils.getMiddleOfPolylineSegment(poly4, 4);
    }

    //    <<<<< END getMiddleOfPolylineSegment >>>>>

    //    <<<<< START getNormalizedEndPoints >>>>>

    @Test
    public void getNormalizedEndPoints1SegmentCenter(){
        List<Point2D.Float> res = GeometryUtils.getNormalizedEndPoints(
                directp0p50,
                square10AtOrigin,
                square10AtX50,
                AnchorPoint.CENTER,
                AnchorPoint.CENTER);
        assertEquals(2, res.size());
        assertEquals(new Point2D.Float(0,5), res.get(0));
        assertEquals(new Point2D.Float(0,45), res.get(1));

    }

    @Test
    public void getNormalizedEndPoints1SegmentAnchors(){
        List<Point2D.Float> res = GeometryUtils.getNormalizedEndPoints(
                directp0p50,
                square10AtOrigin,
                square10AtX50,
                AnchorPoint.E,
                AnchorPoint.W);
        assertEquals(2, res.size());
        assertEquals(new Point2D.Float(0,0), res.get(0));
        assertEquals(new Point2D.Float(0,50), res.get(1));

    }

    @Test
    public void getNormalizedEndPoints1SegmentAnchorsAndCenter(){
        List<Point2D.Float> res = GeometryUtils.getNormalizedEndPoints(
                directp0p50,
                square10AtOrigin,
                square10AtX50,
                AnchorPoint.CENTER,
                AnchorPoint.N);
        assertEquals(2, res.size());
        assertEquals(new Point2D.Float(0,5), res.get(0));
        assertEquals(new Point2D.Float(0,50), res.get(1));

    }

    @Test
    public void getNormalizedEndPoints3SegmentCenter(){
        List<Point2D.Float> res = GeometryUtils.getNormalizedEndPoints(
                _3segmentp0p50,
                square10AtOrigin,
                square10AtX50,
                AnchorPoint.CENTER,
                AnchorPoint.CENTER);
        assertEquals(4, res.size());
        assertEquals(new Point2D.Float(5,5), res.get(0));
        assertEquals(new Point2D.Float(5,45), res.get(res.size() - 1));

    }

    //    <<<<< END getNormalizedEndPoints >>>>>


    //    <<<<< START getLineLineIntersection >>>>>
    @Test
    public void getLineLineIntersectionAxis(){
        Point2D.Float res = GeometryUtils.getLineLineIntersection(Xaxis, Yaxis);
        assertEquals(p0, res);
    }

    @Test
    public void getLineLineIntersectionDiagWithX(){
        Point2D.Float res = GeometryUtils.getLineLineIntersection(Xaxis, positiveAxisDiag);
        assertEquals(new Point2D.Float(0,0), res);
    }

    @Test
    public void getLineLineIntersectionDiagWithDistantInverseDiag(){
        Point2D.Float res = GeometryUtils.getLineLineIntersection(positiveAxisDiag, positiveInverseDiagAt3);
        assertEquals(null, res);
    }

    //    <<<<< END getLineLineIntersection >>>>>

    //    <<<<< START getLineRectangleIntersection >>>>>
    @Test
    public void getLineRectangleIntersectionRectOriginXaxis(){
        List<Point2D.Float> res = GeometryUtils.getLineRectangleIntersection(new Line2D.Float(p0, new Point2D.Float(5,0)), rectAtOrigin);
        assertEquals(1, res.size());
        assertEquals(new Point2D.Float(5, 0), res.get(0));

    }

    @Test
    public void getLineRectangleIntersectionRectOriginDiag(){
        List<Point2D.Float> res = GeometryUtils.getLineRectangleIntersection(new Line2D.Float(p0, new Point2D.Float(5,5)), rectAtOrigin);
        assertEquals(2, res.size());
        assertTrue(res.stream().anyMatch(e -> e.equals(new Point2D.Float(5, 5))));
    }

    @Test
    public void getLineRectangleIntersectionRectOriginFarLine(){
        List<Point2D.Float> res = GeometryUtils.getLineRectangleIntersection(inverseDiagAt50, rectAtOrigin);

        assertEquals(0, res.size());
        /*assertTrue(res.stream().anyMatch(e -> e.equals(new Point2D.Float(5, 5))));
        assertTrue(res.stream().anyMatch(e -> e.equals(new Point2D.Float(-5, -5))));*/
    }

    //    <<<<< END getLineRectangleIntersection >>>>>



    //    <<<<< START rectanglePerimeterPointFromAngle >>>>>
    @Test
    public void rectanglePerimeterPointFromAngleRectOrigin90(){
        Point2D.Float res = GeometryUtils.rectanglePerimeterPointFromAngle(rectAtOrigin,  90);
        assertEquals(5, res.getX(),1E-6);
        assertEquals(0, res.getY(), 1E-6);
    }

    @Test
    public void rectanglePerimeterPointFromAngleRectOrigin180(){
        Point2D.Float res = GeometryUtils.rectanglePerimeterPointFromAngle(rectAtOrigin, 180);
        assertEquals(0, res.getX(),1E-6);
        assertEquals(5, res.getY(), 1E-6);
    }

    @Test
    public void rectanglePerimeterPointFromAngleRectOriginNeg180(){
        Point2D.Float res = GeometryUtils.rectanglePerimeterPointFromAngle(rectAtOrigin,  -180);
        assertEquals(0, res.getX(),1E-6);
        assertEquals(5, res.getY(), 1E-6);
    }

    @Test
    public void rectanglePerimeterPointFromAngleRectOrigin360(){
        Point2D.Float res = GeometryUtils.rectanglePerimeterPointFromAngle(rectAtOrigin, 360);
        assertEquals(10, res.getX(),1E-6);
        assertEquals(5, res.getY(), 1E-6);
    }

    @Test
    public void rectanglePerimeterPointFromAngleRectOrigin32PI(){
        Point2D.Float res = GeometryUtils.rectanglePerimeterPointFromAngle(rectAtOrigin, -90);
        assertEquals(5, res.getX(),1E-6);
        assertEquals(10, res.getY(), 1E-6);
    }

    @Test
    public void rectanglePerimeterPointFromAngleRectOriginBottomLeft(){
        Point2D.Float res = GeometryUtils.rectanglePerimeterPointFromAngle(rectAtOrigin, 225);
        assertEquals(0, res.getX(),1E-6);
        assertEquals(10, res.getY(), 1E-6);
    }

    @Test
    public void rectanglePerimeterPointFromAngleRectOriginTopLeft(){
        Point2D.Float res = GeometryUtils.rectanglePerimeterPointFromAngle(rectAtOrigin, 135);
        assertEquals(0, res.getX(),1E-6);
        assertEquals(0, res.getY(), 1E-6);
    }

    @Test
    public void rectanglePerimeterPointFromAngleRectOriginTopRight(){
        Point2D.Float res = GeometryUtils.rectanglePerimeterPointFromAngle(rectAtOrigin, 45);
        assertEquals(10, res.getX(),1E-6);
        assertEquals(0, res.getY(), 1E-6);
    }

    //    <<<<< END rectanglePerimeterPointFromAngle >>>>>

    //    <<<<< START convertPoints >>>>>

    @Test
    public void convertWithDiagAxis1() {

        AffineTransform t = GeometryUtils.getTransformsToGlobalCoords(p0, p10);
        Point2D.Float res = GeometryUtils.convertPoints(Arrays.asList(new Point2D.Float(0.5f,0)), t).get(0);
        assertEquals(5, res.getX(), 1E-6);
        assertEquals(5, res.getY(), 1E-6);

    }

    @Test
    public void convertWithDiagAxis2() {

        AffineTransform t = GeometryUtils.getTransformsToGlobalCoords(p0, p10);
        Point2D.Float res = GeometryUtils.convertPoints(Arrays.asList(new Point2D.Float(0,1)), t).get(0);
        assertEquals(-10, res.getX(), 1E-6);
        assertEquals(10, res.getY(), 1E-6);

    }

    @Test
    public void convertArbitraryAxis() {

        AffineTransform t = GeometryUtils.getTransformsToGlobalCoords(p2010, p2050);
        Point2D.Float res = GeometryUtils.convertPoints(Arrays.asList(new Point2D.Float(0,1)), t).get(0);
        assertEquals(-20, res.getX(), 1E-6);
        assertEquals(10, res.getY(), 1E-6);

    }

    @Test
    public void convertArbitraryAxis2() {

        AffineTransform t = GeometryUtils.getTransformsToGlobalCoords(p2010, p2050);
        Point2D.Float res = GeometryUtils.convertPoints(Arrays.asList(new Point2D.Float(0.5f,-0.5f)), t).get(0);
        assertEquals(40, res.getX(), 1E-6);
        assertEquals(30, res.getY(), 1E-6);

    }

    //    <<<<< END convertPoints >>>>>
}
