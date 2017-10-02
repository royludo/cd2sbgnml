import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
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
}
