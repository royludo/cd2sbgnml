package fr.curie.cd2sbgnml.xmlcdwrappers;

import org.sbml._2001.ns.celldesigner.LayerSpeciesAlias;
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

    public TextWrapper(LayerSpeciesAlias layerAlias, boolean visible) {
        this.visible = visible;

        this.text = layerAlias.getLayerNotes();
        this.refPoint = new Point2D.Float(
                layerAlias.getX().floatValue(),
                layerAlias.getY().floatValue());

        this.targetType = layerAlias.getTarget();
        this.targetId = layerAlias.getTargetId();

        this.fontSize = layerAlias.getFont().getSize().floatValue();
        this.color = layerAlias.getPaint().getColor();
        this.bbox = new Rectangle2D.Float(
                layerAlias.getBounds().getX().floatValue(),
                layerAlias.getBounds().getY().floatValue(),
                layerAlias.getBounds().getW().floatValue(),
                layerAlias.getBounds().getH().floatValue()
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

    public String getTargetType() {
        return targetType;
    }

    public String getTargetId() {
        return targetId;
    }
}
