import java.awt.*;
import java.awt.geom.Point2D;

public class Process extends AbstractLinkableCDEntity {

    public static final float PROCESS_SIZE = 10;

    private Point2D coords;

    public Process(Point2D coords) {
        super(AbstractLinkableCDEntityType.PROCESS);
        this.coords = coords;
    }

    public Point2D getCoords() {
        return coords;
    }

    /**
     * Take a celldesigner index representing the possible anchor points on a process glyph,
     * and return the corresponding coordinates, relative to the center of the process, with center at origin.
     * @param index
     * @return relative coordinates corresponding to the anchor index point
     */
    public static Point2D getRelativeAnchorCoords(int index) {
        float halfSize = PROCESS_SIZE / 2;
        switch(index) {
            case 2: return new Point2D.Float(0, -halfSize);
            case 3: return new Point2D.Float(0, halfSize);
            case 4: return new Point2D.Float(-halfSize, -halfSize);
            case 5: return new Point2D.Float(halfSize, -halfSize);
            case 6: return new Point2D.Float(-halfSize, halfSize);
            case 7: return new Point2D.Float(halfSize, halfSize);
        }
        throw new IllegalArgumentException("Celldesigner index for process anchor point should be an integer from 2 to 7.");
    }

    public Point2D getAbsoluteAnchorCoords(int index) {
        Point2D relativeCoords = getRelativeAnchorCoords(index);
        return new Point2D.Float(
                (float) (relativeCoords.getX() + this.getCoords().getX()),
                (float) (relativeCoords.getY() + this.getCoords().getY()));
    }

}
