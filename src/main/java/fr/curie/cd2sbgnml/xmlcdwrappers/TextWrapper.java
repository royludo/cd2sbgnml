package fr.curie.cd2sbgnml.xmlcdwrappers;

import org.sbml.x2001.ns.celldesigner.CelldesignerLayerSpeciesAliasDocument;
import org.sbml.x2001.ns.celldesigner.CelldesignerLayerSpeciesAliasDocument.CelldesignerLayerSpeciesAlias;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

public class TextWrapper {

    private final Logger logger = LoggerFactory.getLogger(TextWrapper.class);

    private boolean visible;
    private String targetType;
    private String targetId;
    private Point2D.Float refPoint;
    private String text;
    private float fontSize;
    private String color;
    private Rectangle2D.Float bbox;

    public TextWrapper(CelldesignerLayerSpeciesAlias layerAlias, boolean visible) {
        this.visible = visible;

        this.text = layerAlias.getCelldesignerLayerNotes().getDomNode().getChildNodes().item(0).getNodeValue();
        this.refPoint = new Point2D.Float(
                Float.parseFloat(layerAlias.getX().getStringValue()),
                Float.parseFloat(layerAlias.getY().getStringValue()));
        /*
        The incomplete API of celldesignerParser prevent from getting targetType and Id

        this.targetType = layerAlias.getDomNode().getAttributes().getNamedItem("targetType").getNodeValue();
        this.targetId = layerAlias.getDomNode().getAttributes().getNamedItem("targetId").getNodeValue();
        */
        this.fontSize = Float.parseFloat(layerAlias.getCelldesignerFont().getSize().getStringValue());
        this.color = layerAlias.getCelldesignerPaint().getColor().getStringValue();
        this.bbox = new Rectangle2D.Float(
                Float.parseFloat(layerAlias.getCelldesignerBounds().getX()),
                Float.parseFloat(layerAlias.getCelldesignerBounds().getY()),
                Float.parseFloat(layerAlias.getCelldesignerBounds().getW()),
                Float.parseFloat(layerAlias.getCelldesignerBounds().getH())
        );

        /*logger.debug("Check TextWrapper");
        logger.debug("visible: "+this.visible);
        logger.debug("text: " + this.text);
        logger.debug("refPoint: " + this.refPoint);
        //logger.debug("targetType " + this.targetType);
        //logger.debug("targetId: " + this.targetId);
        logger.debug("fontSize: " + this.fontSize);
        logger.debug("color: " + this.color);
        logger.debug("bbox: "+this.bbox);*/

    }

    public boolean isVisible() {
        return visible;
    }

    public Point2D.Float getRefPoint() {
        return refPoint;
    }

    public String getText() {
        return text;
    }

    public float getFontSize() {
        return fontSize;
    }

    public String getColor() {
        return color;
    }

    public Rectangle2D.Float getBbox() {
        return bbox;
    }

}
