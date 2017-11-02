package fr.curie.cd2sbgnml.xmlcdwrappers;

import org.sbml._2001.ns.celldesigner.*;

import javax.sound.sampled.Port;
import java.awt.geom.Point2D;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * This class is used to wrap information concerning the line of a reactant.
 *  = connectscheme + line + editpoints elements
 */
public class LineWrapper {

    private float lineWidth;
    private String lineColor;
    private String lineType;

    private List<Point2D.Float> editPoints;
    private int num0, num1, num2;
    private int tShapeIndex;

    private String connectPolicy = "direct";
    private String rectangleIndex;
    private List<LineDirection> lineDirectionList;

    public LineWrapper(ConnectScheme connectScheme, EditPoints editPoints, Line line) {
        this(connectScheme,
                line.getWidth().floatValue(),
                line.getColor(), null);
        setEditPoints(editPoints);

    }

    public LineWrapper(ConnectScheme connectScheme, EditPoints editPoints, LineType2 line) {
        this(connectScheme, line.getWidth().floatValue(), line.getColor(), line.getType());
        setEditPoints(editPoints);
    }

    public LineWrapper(ConnectScheme connectScheme, List<String> editPoints, Line line) {
        this(connectScheme, line.getWidth().floatValue(), line.getColor(), null);
        List<Point2D.Float> pointList = new ArrayList<>();
        if(editPoints != null) {
            for (String pointString : editPoints) {
                String[] tmp = pointString.split(",");
                float x = Float.parseFloat(tmp[0]);
                float y = Float.parseFloat(tmp[1]);
                pointList.add(new Point2D.Float(x, y));
            }
        }
        this.editPoints = pointList;
    }

    private void setEditPoints(EditPoints editPoints){
        if(editPoints != null) {
            if(editPoints.getNum0() != null) {
                this.num0 = editPoints.getNum0();
            }
            if(editPoints.getNum1() != null) {
                this.num1 = editPoints.getNum1();
            }
            if(editPoints.getNum2() != null) {
                this.num2 = editPoints.getNum2();
            }
            if(editPoints.getTShapeIndex() != null) {
                this.tShapeIndex = editPoints.getTShapeIndex();
            }
            if(editPoints.getValue() != null) { // should never be null
                List<Point2D.Float> pointList = new ArrayList<>();
                for (String pointString : editPoints.getValue()) {
                    String[] tmp = pointString.split(",");
                    float x = Float.parseFloat(tmp[0]);
                    float y = Float.parseFloat(tmp[1]);
                    pointList.add(new Point2D.Float(x, y));
                }
                this.editPoints = pointList;
            }
        }
    }

    private LineWrapper(ConnectScheme connectScheme, float width, String color, String type) {
        this.lineWidth = width;
        this.lineColor = color;
        this.lineType = type;

        this.connectPolicy = connectScheme.getConnectPolicy();
        this.rectangleIndex = connectScheme.getRectangleIndex();
        this.lineDirectionList = connectScheme.getListOfLineDirection().getLineDirection();
    }

    public float getLineWidth() {
        return lineWidth;
    }

    public void setLineWidth(float lineWidth) {
        this.lineWidth = lineWidth;
    }

    public String getLineColor() {
        return lineColor;
    }

    public void setLineColor(String lineColor) {
        this.lineColor = lineColor;
    }

    public String getLineType() {
        return lineType;
    }

    public void setLineType(String lineType) {
        this.lineType = lineType;
    }

    public List<Point2D.Float> getEditPoints() {
        if(editPoints == null) {
            return new ArrayList<>();
        }
        return editPoints;
    }

    public void setEditPoints(List<Point2D.Float> editPoints) {
        this.editPoints = editPoints;
    }

    public int getNum0() {
        return num0;
    }

    public void setNum0(int num0) {
        this.num0 = num0;
    }

    public int getNum1() {
        return num1;
    }

    public void setNum1(int num1) {
        this.num1 = num1;
    }

    public int getNum2() {
        return num2;
    }

    public void setNum2(int num2) {
        this.num2 = num2;
    }

    public int gettShapeIndex() {
        return tShapeIndex;
    }

    public void settShapeIndex(int tShapeIndex) {
        this.tShapeIndex = tShapeIndex;
    }

    public String getConnectPolicy() {
        return connectPolicy;
    }

    public void setConnectPolicy(String connectPolicy) {
        this.connectPolicy = connectPolicy;
    }

    public String getRectangleIndex() {
        return rectangleIndex;
    }

    public void setRectangleIndex(String rectangleIndex) {
        this.rectangleIndex = rectangleIndex;
    }

    public List<LineDirection> getLineDirectionList() {
        return lineDirectionList;
    }

    public void setLineDirectionList(List<LineDirection> lineDirectionList) {
        this.lineDirectionList = lineDirectionList;
    }
}
