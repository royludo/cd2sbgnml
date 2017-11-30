package fr.curie.cd2sbgnml;

import fr.curie.cd2sbgnml.xmlcdwrappers.StyleInfo;
import org.sbgn.ArcClazz;
import org.sbgn.GlyphClazz;
import org.sbgn.bindings.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.*;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.sbgn.GlyphClazz.*;

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

                if(g.getAttribute("fontSize") != null && !g.getAttribute("fontSize").isEmpty()) {
                    styleInfo.setFontSize(Float.parseFloat(g.getAttribute("fontSize")));
                }

                if(g.getAttribute("strokeWidth") != null) {
                    styleInfo.setLineWidth(Float.parseFloat(g.getAttribute("strokeWidth")));
                }

                if(g.getAttribute("stroke") != null) {
                    styleInfo.setLineColor(colorMap.get(g.getAttribute("stroke")));
                }

                if(g.getAttribute("fill") != null && !g.getAttribute("fill").isEmpty()) {
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

    /**
     * Return wether the glyph has at least one unit of information with label matching regexp.
     * Case insensitive.
     * @param glyph
     * @param regexp
     * @return
     */
    public static boolean hasUnitOfInfo(Glyph glyph, String regexp) {
        Pattern p = Pattern.compile(regexp, Pattern.CASE_INSENSITIVE);
        for(Glyph subglyph: glyph.getGlyph()) {
            if(subglyph.getClazz().equals("unit of information")) {
                String info = subglyph.getLabel().getText();
                Matcher m = p.matcher(info);
                if(m.find()){
                    return true;
                }
            }
        }
        return false;
    }

    public static Optional<Glyph> getUnitOfInfo(Glyph glyph, String regexp) {
        Pattern p = Pattern.compile(regexp, Pattern.CASE_INSENSITIVE);
        for(Glyph subglyph: glyph.getGlyph()) {
            if(subglyph.getClazz().equals("unit of information")) {
                String info = subglyph.getLabel().getText();
                Matcher m = p.matcher(info);
                if(m.find()){
                    return Optional.of(subglyph);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Will check the presence of a unit of information with content: "N:"+multimer count
     * and return multimer count
     * @param glyph
     * @return 0 if no unit of info concerning multimer, else number of multimer
     */
    public static int getMultimerFromInfo(Glyph glyph) {
        if(SBGNUtils.hasUnitOfInfo(glyph, "N\\s*:\\s*\\d+")){
            Pattern p = Pattern.compile("N\\s*:\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
            for(Glyph subglyph: glyph.getGlyph()) {
                if(subglyph.getClazz().equals("unit of information")) {
                    String info = subglyph.getLabel().getText();
                    Matcher m = p.matcher(info);
                    if(m.find()){
                        return Integer.parseInt(m.group(1));
                    }
                }
            }
        }
        return 0;
    }

    /**
     * Given a list of arcs all belonging to the same reaction (= attached to the same process glyph),
     * determines wether the reaction is reversible or not.
     * It is considered reversible if no consumption arc is present.
     * @param arcs a list of arcs all having the same process as source or target
     * @return
     */
    public static boolean isReactionReversible(List<Arc> arcs) {
        for(Arc arc: arcs){
            ArcClazz clazz = ArcClazz.fromClazz(arc.getClazz());
            if(clazz == ArcClazz.CONSUMPTION) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns a list of 3 lists containing:
     *  0 - the reactants consumed by the reaction
     *  1 - the products of the reaction
     *  2 - all other types of interactions (catalysis, stimulation...)
     *
     *  If the reaction is reversible, then no consumption arc is present. But CellDesigner still needs
     *  a base reactant and base product. So we arbitrarily select one arc of the reaction to be a base
     *  reactant arc, and another one to be the base products. This choice shouldn't impact anything.
     *
     * @param arcs a list of arcs all having the same process as source or target
     * @return list of 3 lists of arcs
     */
    public static List<List<Arc>> getReactantTypes(List<Arc> arcs, boolean isReversible) {
        List<List<Arc>> result = new ArrayList<>();
        List<Arc> products = new ArrayList<>();
        List<Arc> reactants = new ArrayList<>();
        List<Arc> modifiers = new ArrayList<>();
        for(Arc arc: arcs){
            ArcClazz clazz = ArcClazz.fromClazz(arc.getClazz());
            switch(clazz) {
                case CONSUMPTION: reactants.add(arc); break;
                case PRODUCTION: products.add(arc); break;
                default: modifiers.add(arc);
            }
        }

        if(isReversible) {
            // transfer one product to the reactant list
            reactants.add(products.remove(0));
        }

        result.add(reactants);
        result.add(products);
        result.add(modifiers);
        return result;
    }

    /**
     * Determines which arcs are baseReactants and the baseProduct for a boolean logic gate reaction type
     * @param arcs
     * @return
     */
    public static List<List<Arc>> getBLGReactantTypes(List<Arc> arcs) {
        List<List<Arc>> result = new ArrayList<>();
        List<Arc> product = new ArrayList<>();
        List<Arc> reactants = new ArrayList<>();
        for(Arc arc: arcs){
            ArcClazz clazz = ArcClazz.fromClazz(arc.getClazz());
            switch(clazz) {
                case LOGIC_ARC: reactants.add(arc); break;
                default: product.add(arc);
            }
        }
        if(product.size() > 1) {
            // logic gates shouldn't have more than 1 output arc
            // TODO output list of arc id
            logger.error("Logic gate has more than 1 output arc, only the first arc will be considered, others are" +
                    "discarded.");
        }

        result.add(reactants);
        result.add(product);
        return result;
    }

    /**
     * might be too complicated for now
     * @param process
     * @param reactants
     * @param products
     * @return
     */
    public static boolean isReactionAssociation(Glyph process, List<Arc> reactants, List<Arc> products) {
        if(process.getClazz().equals("association")) { // trivial case of specific process node
            return true;
        }

        // we need at least 2 reactants to be associated
        // TODO what about reversible associations with only products ?
        /*if(reactants.size() < 2) {
            return false;
        }*/

        // TODO better check
        // we need at least 1 complex as output
        /*Glyph complex = null;
        for(Arc arc: products) {
            // can ports be involved here ?
            Glyph product = (Glyph) arc.getTarget();
        }*/

        return false;
    }

    public static boolean isReactionDissociation(Glyph process, List<Arc> reactants, List<Arc> products) {
        if(process.getClazz().equals("dissociation")) { // trivial case of specific process node
            return true;
        }

        // we need at least 2 products to be dissociated
        if(products.size() < 2) {
            return false;
        }

        return false;
    }

    /**
     * Return a list of the start, intermediate and end points of an arc.
     * @param arc
     * @return
     */
    public static List<Point2D.Float> getPoints(Arc arc) {
        List<Point2D.Float> points = new ArrayList<>();
        float startX = arc.getStart().getX();
        float startY = arc.getStart().getY();
        points.add(new Point2D.Float(startX, startY));
        for(Arc.Next n: arc.getNext()){
            points.add(new Point2D.Float(n.getX(), n.getY()));
        }
        float endX = arc.getEnd().getX();
        float endY = arc.getEnd().getY();
        points.add(new Point2D.Float(endX, endY));

        return points;
    }


    public static void sanitizeSubGlyphs(List<Glyph> subglyphs) {
        for(Glyph g: subglyphs) {
            g.setId(g.getId().replaceAll("-", "_"));
            sanitizeSubGlyphs(g.getGlyph());
        }
    }

    /**
     * CellDesigner doesn't like ids with '-' we need to go over all the sbgn and change that to '_'
     * This is because SBML ids aren't defined as xsd:id.
     *
     * See:
     * http://sbml.org/Special/specifications/sbml-level-2/version-1/html/sbml-level-2.html#SECTION00034000000000000000
     *
     * @param sbgn
     * @return
     */
    public static Sbgn sanitizeIds(Sbgn sbgn) {
        for(Glyph g: sbgn.getMap()/*.get(0)*/.getGlyph()){

            g.setId(g.getId().replaceAll("-", "_"));

            sanitizeSubGlyphs(g.getGlyph());

            if(g.getPort().size() > 0) {
                for(Port p: g.getPort()) {
                     p.setId(p.getId().replaceAll("-", "_"));
                }
            }
        }

        for(Arc a: sbgn.getMap()/*.get(0)*/.getArc()) {
            a.setId(a.getId().replaceAll("-", "_"));
        }

        // change ids in style
        if(sbgn.getMap()/*.get(0)*/.getExtension() != null) {
            for (Element e : sbgn.getMap()/*.get(0)*/.getExtension().getAny()) {
                if (e.getTagName().equals("renderInformation")) {
                    NodeList nodeList = e.getElementsByTagName("style");

                    for (int i = 0; i < nodeList.getLength(); i++) {
                        Element e2 = (Element) nodeList.item(i);
                        e2.setAttribute("idList", e2.getAttribute("idList").replaceAll("-", "_"));
                    }
                }
            }
        }

        return sbgn;
    }

    /**
     * Converts an SBGN glyph's bbox to a Rectangle2D
     * @param g
     * @return
     */
    public static Rectangle2D.Float getRectangleFromGlyph(Glyph g) {
        return new Rectangle2D.Float(
                g.getBbox().getX(),
                g.getBbox().getY(),
                g.getBbox().getW(),
                g.getBbox().getH()
        );
    }

    public static boolean isLogicGate(Glyph g) {
        return GlyphClazz.fromClazz(g.getClazz()) == AND
                || GlyphClazz.fromClazz(g.getClazz()) == OR
                || GlyphClazz.fromClazz(g.getClazz()) == NOT;
    }

    public static List<Glyph> getStateVariables(Glyph g) {
        int i=1;
        List<Glyph> result = new ArrayList<>();
        for(Glyph subglyph: g.getGlyph()) {
            if(GlyphClazz.fromClazz(subglyph.getClazz()) == STATE_VARIABLE) {
                //Glyph.State state = subglyph.getState();
                /*String stateVar = subglyph.getState().getVariable();
                String value = subglyph.getState().getValue();

                ResidueWrapper resW = new ResidueWrapper("rs"+i);
                resW.useAngle = true;
                resW.name = stateVar;
                resW.state = ResidueWrapper.getLongState(value);
                result.add(resW);
                i++;*/
                result.add(subglyph);
            }
        }
        return result;
    }
}
