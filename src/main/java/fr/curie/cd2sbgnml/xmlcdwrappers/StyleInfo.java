package fr.curie.cd2sbgnml.xmlcdwrappers;

import org.sbml._2001.ns.celldesigner.*;

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

    public StyleInfo(CompartmentAlias compAlias, String refId) {
        this.bgColor = DEFAULT_BG_COLOR;
        this.fontSize = DEFAULT_FONT_SIZE; // default
        this.refId = refId;

        Paint paint = compAlias.getPaint();
        DoubleLine doubleLine = compAlias.getDoubleLine();

        this.lineWidth = doubleLine.getThickness().floatValue()
                + doubleLine.getOuterWidth().floatValue() / 2
                + doubleLine.getInnerWidth().floatValue() / 2;
        this.lineColor = paint.getColor().toLowerCase();
        this.id = generateStyleId();
    }

    public StyleInfo(ComplexSpeciesAlias complexAlias, String refId) {
        this.refId = refId;
        this.fontSize = complexAlias.getFont().getSize();
        this.lineColor = DEFAULT_LINE_COLOR;
        this.lineWidth = complexAlias.getUsualView().getSingleLine().getWidth().floatValue();
        this.bgColor = complexAlias.getUsualView().getPaint().getColor().toLowerCase();
        this.id = generateStyleId();
    }

    public StyleInfo(SpeciesAlias speciesAlias, String refId) {
        this.refId = refId;
        this.fontSize = speciesAlias.getFont().getSize();
        this.lineColor = DEFAULT_LINE_COLOR;
        this.lineWidth = speciesAlias.getUsualView().getSingleLine().getWidth().floatValue();
        this.bgColor = speciesAlias.getUsualView().getPaint().getColor().toLowerCase();
        this.id = generateStyleId();
    }

    /**
     * Base reaction link style.
     * @param refId
     */
    public StyleInfo(float width, String color, String refId) {
        this.refId = refId;
        this.lineWidth = width;
        this.lineColor = color;
        this.bgColor = DEFAULT_BG_COLOR;
        this.fontSize = DEFAULT_FONT_SIZE;
        this.id = generateStyleId();
    }

    public StyleInfo(Modification modif, String refId) {
        this.refId = refId;
        Line line = modif.getLine();
        this.lineWidth = line.getWidth().floatValue();
        this.lineColor = line.getColor().toLowerCase();
        this.bgColor = DEFAULT_BG_COLOR;
        this.fontSize = DEFAULT_FONT_SIZE;
        this.id = generateStyleId();
    }

    public StyleInfo(ReactantLink reactantLink, String refId) {
        this.refId = refId;

        this.lineWidth = reactantLink.getLine().getWidth().floatValue();
        this.lineColor = reactantLink.getLine().getColor().toLowerCase();
        this.bgColor = DEFAULT_BG_COLOR;
        this.fontSize = DEFAULT_FONT_SIZE;
        this.id = generateStyleId();

    }

    public StyleInfo(ProductLink productLink, String refId) {
        this.refId = refId;
        this.lineWidth = productLink.getLine().getWidth().floatValue();
        this.lineColor = productLink.getLine().getColor().toLowerCase();
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
