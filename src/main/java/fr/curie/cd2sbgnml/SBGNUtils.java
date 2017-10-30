package fr.curie.cd2sbgnml;

import fr.curie.cd2sbgnml.xmlcdwrappers.StyleInfo;
import jdk.nashorn.internal.runtime.regexp.joni.Regex;
import org.sbgn.bindings.Bbox;
import org.sbgn.bindings.Glyph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.awt.geom.Rectangle2D;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SBGNUtils {

    private static final Logger logger = LoggerFactory.getLogger(SBGNUtils.class);

    /**
     * returns the size of a map
     * @param map
     * @return
     */
    public static Rectangle2D getMapBounds(org.sbgn.bindings.Map map) {
        if(map.getGlyph().size() == 0) {
            return new Rectangle2D.Float(0,0,0,0);
        }

        float minX, maxX, minY, maxY;
        Bbox firstBox = map.getGlyph().get(0).getBbox();
        minX = firstBox.getX();
        maxX = minX + firstBox.getW();
        minY = firstBox.getY();
        maxY = minY + firstBox.getH();

        for(Glyph glyph: map.getGlyph()){
            Bbox b = glyph.getBbox();
            float currentX = b.getX();
            float currentMaxX = b.getX() + b.getW();
            float currentY = b.getY();
            float currentMaxY = b.getY() + b.getH();


            minX = Float.min(currentX, minX);
            maxX = Float.max(currentMaxX, maxX);
            minY = Float.min(currentY, minY);
            maxY = Float.max(currentMaxY, maxY);
        }

        int sizeX = Math.round(maxX - minX) + 200; // add some margin
        int sizeY = Math.round(maxY - minY) + 200;
        float X = minX - 100;
        float Y = minY - 100;
        return new Rectangle2D.Float(X, Y, sizeX, sizeY);
    }

    /**
     * Parses the renderInformation element and maps ids to StyleInfo objects
     * @param renderInformation
     * @return
     */
    public static Map<String, StyleInfo> mapStyleinfo(Element renderInformation) {
        Map<String, StyleInfo> result = new HashMap<>();

        NodeList listOfColors = renderInformation.getElementsByTagName("colorDefinition");
        Map<String, String> colorMap = new HashMap<>();
        for(int i=0; i < listOfColors.getLength(); i++) {
            Element color = (Element) listOfColors.item(i);
            String colorValue = color.getAttribute("value").replaceFirst("#", "");
            // if transparency is set it is rgba, celldesigner wants argb
            if(colorValue.length() == 8) {
                String alpha = colorValue.substring(6,8);
                colorValue = alpha + colorValue.substring(0,6);
            }
            else if(colorValue.length() == 6) {
                colorValue = "ff" + colorValue;
            }
            colorMap.put(color.getAttribute("id"), colorValue);
        }

        NodeList listOfStyles = renderInformation.getElementsByTagName("style");
        for(int i=0; i < listOfStyles.getLength(); i++) {
            Element style = (Element) listOfStyles.item(i);
            String idListAttr = style.getAttribute("idList");
            List<String> idList = Arrays.asList(idListAttr.split(" "));
            Element g = (Element) style.getElementsByTagName("g").item(0);

            for(String refId: idList) {
                StyleInfo styleInfo = new StyleInfo(refId);

                if(g.getAttribute("fontSize") != null) {
                    styleInfo.setFontSize(Float.parseFloat(g.getAttribute("fontSize")));
                }

                if(g.getAttribute("strokeWidth") != null) {
                    styleInfo.setLineWidth(Float.parseFloat(g.getAttribute("strokeWidth")));
                }

                if(g.getAttribute("stroke") != null) {
                    styleInfo.setLineColor(colorMap.get(g.getAttribute("stroke")));
                }

                if(g.getAttribute("fill") != null) {
                    styleInfo.setBgColor(colorMap.get(g.getAttribute("fill")));
                }

                result.put(refId, styleInfo);
            }
        }

        return result;
    }

    /**
     * Take a url of the form http://identifiers.org/<db>:<id> and converts it to urn
     * @param url
     * @return something of the form urn:miriam:<db>:<id>
     */
    public static String urlToUrn(String url) {
        Matcher m = Pattern.compile("http://identifiers\\.org/(\\S+):(\\S+)").matcher(url);
        if(m.matches()) {
            String db = m.group(1);
            String id = m.group(2);
            return "urn:miriam:"+db+":"+id;
        }
        throw new IllegalArgumentException("Annotation URL: "+url+" not recognized, must be of the form: http://identifiers.org/<db>:<id>");
    }

    public static Element sanitizeRdfURNs(Element rdf) {
        Element result = (Element) rdf.cloneNode(true);

        NodeList nodeList = result.getElementsByTagName("rdf:li");
        for(int i=0; i < nodeList.getLength(); i++) {
            Element e = (Element) nodeList.item(i);
            String resource = e.getAttribute("rdf:resource");
            String convertedResource;
            if(resource.contains("http://identifiers.org/")) {
                convertedResource = urlToUrn(resource);
            }
            else if(resource.contains("urn:miriam:")) {
                convertedResource = resource;
            }
            else {
                logger.warn("Annotation resource: "+resource+" isn't of any known format (urn or identifier url) and" +
                        "may be ignored and lost by CellDesigner");
                convertedResource = resource;
            }
            e.setAttribute("rdf:resource", convertedResource);
        }
        return result;
    }
}