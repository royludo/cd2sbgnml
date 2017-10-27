package fr.curie.cd2sbgnml.xmlcdwrappers;

import org.sbml.x2001.ns.celldesigner.*;
import org.sbml.x2001.ns.celldesigner.CelldesignerCompartmentAliasDocument.CelldesignerCompartmentAlias;
import org.sbml.x2001.ns.celldesigner.CelldesignerComplexSpeciesAliasDocument.CelldesignerComplexSpeciesAlias;
import org.sbml.x2001.ns.celldesigner.CelldesignerDoubleLineDocument.CelldesignerDoubleLine;
import org.sbml.x2001.ns.celldesigner.CelldesignerLineDocument.CelldesignerLine;
import org.sbml.x2001.ns.celldesigner.CelldesignerModificationDocument.CelldesignerModification;
import org.sbml.x2001.ns.celldesigner.CelldesignerPaintDocument.CelldesignerPaint;
import org.sbml.x2001.ns.celldesigner.CelldesignerProductLinkDocument.CelldesignerProductLink;
import org.sbml.x2001.ns.celldesigner.CelldesignerReactantLinkDocument.CelldesignerReactantLink;
import org.sbml.x2001.ns.celldesigner.CelldesignerSpeciesAliasDocument.CelldesignerSpeciesAlias;
import org.sbml.x2001.ns.celldesigner.CelldesignerUsualViewDocument.CelldesignerUsualView;
import org.sbml.x2001.ns.celldesigner.ReactionDocument.Reaction;
import org.w3c.dom.Node;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Stores the style properties found for some elements
 *
 * For compartments, thickness + outer/2 + inner/2 are fused together into lineWidth.
 * Gradients are not considered.
 * CellDesigner stores colors in argb format, all colors will be stored as lowercase argb hex string, no #.
 */
public class StyleInfo {

    private static final String DEFAULT_BG_COLOR = "00ffffff"; // white transparent as default default
    private static final String DEFAULT_LINE_COLOR = "ff000000"; // black opaque default
    private static final float DEFAULT_FONT_SIZE = 12;
    private static final float DEFAULT_LINE_WIDTH = 1;

    private float lineWidth;
    private String bgColor;
    private float fontSize;
    private String lineColor;
    private String refId;
    private String id;

    /**
     * Default value constructor
     */
    public StyleInfo(String refId) {
        this.bgColor = DEFAULT_BG_COLOR;
        this.fontSize = DEFAULT_FONT_SIZE;
        this.lineColor = DEFAULT_LINE_COLOR;
        this.lineWidth = DEFAULT_LINE_WIDTH;
        this.refId = refId;
        this.id = generateStyleId();
    }

    /**
     * Create style info from other style info, changing the reference.
     */
    public StyleInfo(StyleInfo previousInfo, String refId) {
        this.bgColor = previousInfo.getBgColor();
        this.fontSize = previousInfo.getFontSize();
        this.lineColor = previousInfo.getLineColor();
        this.lineWidth = previousInfo.getLineWidth();
        this.refId = refId;
        this.id = generateStyleId();
    }

    public StyleInfo(CelldesignerCompartmentAlias compAlias, String refId) {
        this.bgColor = DEFAULT_BG_COLOR;
        this.fontSize = DEFAULT_FONT_SIZE; // default
        this.refId = refId;

        CelldesignerPaint paint = compAlias.getCelldesignerPaint();
        CelldesignerDoubleLine doubleLine = compAlias.getCelldesignerDoubleLine();

        this.lineWidth = Float.parseFloat(doubleLine.getThickness())
                + Float.parseFloat(doubleLine.getOuterWidth()) / 2
                + Float.parseFloat(doubleLine.getInnerWidth()) / 2;
        this.lineColor = paint.getColor().getStringValue().toLowerCase();
        this.id = generateStyleId();
    }

    public StyleInfo(CelldesignerComplexSpeciesAlias complexAlias, String refId) {
        this.refId = refId;
        this.fontSize = Float.parseFloat(complexAlias.getCelldesignerFont().getSize().getStringValue());
        this.lineColor = DEFAULT_LINE_COLOR;
        CelldesignerUsualView usualView = complexAlias.getCelldesignerUsualView();
        this.lineWidth = Float.parseFloat(usualView.getCelldesignerSingleLine().getWidth());
        this.bgColor = usualView.getCelldesignerPaint().getColor().getStringValue().toLowerCase();
        this.id = generateStyleId();
    }

    public StyleInfo(CelldesignerSpeciesAlias speciesAlias, String refId) {
        this.refId = refId;
        this.fontSize = Float.parseFloat(speciesAlias.getCelldesignerFont().getSize().getStringValue());
        this.lineColor = DEFAULT_LINE_COLOR;
        CelldesignerUsualView usualView = speciesAlias.getCelldesignerUsualView();
        this.lineWidth = Float.parseFloat(usualView.getCelldesignerSingleLine().getWidth());
        this.bgColor = usualView.getCelldesignerPaint().getColor().getStringValue().toLowerCase();
        this.id = generateStyleId();
    }

    /**
     * Base reaction link style.
     * @param reaction
     * @param refId
     */
    public StyleInfo(Reaction reaction, String refId) {
        this.refId = refId;
        CelldesignerLine line = reaction.getAnnotation().getCelldesignerLine();
        this.lineWidth = Float.parseFloat(line.getWidth());
        this.lineColor = line.getColor().toLowerCase();
        this.bgColor = DEFAULT_BG_COLOR;
        this.fontSize = DEFAULT_FONT_SIZE;
        this.id = generateStyleId();
    }

    public StyleInfo(CelldesignerModification modif, String refId) {
        this.refId = refId;
        CelldesignerLine line = modif.getCelldesignerLine();
        this.lineWidth = Float.parseFloat(line.getWidth());
        this.lineColor = line.getColor().toLowerCase();
        this.bgColor = DEFAULT_BG_COLOR;
        this.fontSize = DEFAULT_FONT_SIZE;
        this.id = generateStyleId();
    }

    public StyleInfo(CelldesignerReactantLink reactantLink, String refId) {
        this.refId = refId;

        for(int i=0; i < reactantLink.getDomNode().getChildNodes().getLength(); i++) {
            Node n = reactantLink.getDomNode().getChildNodes().item(i);
            if(n.getNodeName().equals("celldesigner_line")) {
                this.lineWidth = Float.parseFloat(n.getAttributes().getNamedItem("width").getNodeValue());
                this.lineColor = n.getAttributes().getNamedItem("color").getNodeValue().toLowerCase();
            }
        }
        this.bgColor = DEFAULT_BG_COLOR;
        this.fontSize = DEFAULT_FONT_SIZE;
        this.id = generateStyleId();

    }

    public StyleInfo(CelldesignerProductLink productLink, String refId) {
        this.refId = refId;
        for(int i=0; i < productLink.getDomNode().getChildNodes().getLength(); i++) {
            Node n = productLink.getDomNode().getChildNodes().item(i);
            if(n.getNodeName().equals("celldesigner_line")) {
                this.lineWidth = Float.parseFloat(n.getAttributes().getNamedItem("width").getNodeValue());
                this.lineColor = n.getAttributes().getNamedItem("color").getNodeValue().toLowerCase();
            }
        }
        this.bgColor = DEFAULT_BG_COLOR;
        this.fontSize = DEFAULT_FONT_SIZE;
        this.id = generateStyleId();

    }

    /**
     * Generate a kind of hash of the style, to be used as a unique id for it.
     * @return
     */
    private String generateStyleId() {
        return "style_"+this.lineWidth+this.bgColor+this.fontSize+this.lineColor;
    }

    public static Map<String, String> getMapOfColorDefinitions(List<StyleInfo> styleInfoList) {
        Map<String, String> result = new HashMap<>();
        int i = 1; // color index

        for(StyleInfo sinfo: styleInfoList) {
            if(!result.containsKey(sinfo.getBgColor())) {
                result.put(sinfo.getBgColor(), "color_"+i);
                i++;
            }
            if(!result.containsKey(sinfo.getLineColor())) {
                result.put(sinfo.getLineColor(), "color_"+i);
                i++;
            }
        }

        return result;
    }

    public float getLineWidth() {
        return lineWidth;
    }

    public String getBgColor() {
        return bgColor;
    }

    public float getFontSize() {
        return fontSize;
    }

    public String getLineColor() {
        return lineColor;
    }

    public String getRefId() {
        return refId;
    }

    public String getId() {
        return id;
    }

    public String toString() {
        return "BgColor: "+this.bgColor+" lineColor: "+this.lineColor+" lineWidth: "+this.lineWidth+" fontSize: "+this.fontSize;
    }

    public void setLineWidth(float lineWidth) {
        this.lineWidth = lineWidth;
    }

    public void setBgColor(String bgColor) {
        this.bgColor = bgColor;
    }

    public void setFontSize(float fontSize) {
        this.fontSize = fontSize;
    }

    public void setLineColor(String lineColor) {
        this.lineColor = lineColor;
    }
}
