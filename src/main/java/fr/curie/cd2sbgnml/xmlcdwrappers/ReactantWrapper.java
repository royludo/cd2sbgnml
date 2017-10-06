package fr.curie.cd2sbgnml.xmlcdwrappers;

import fr.curie.cd2sbgnml.AbstractLinkableCDEntity;
import fr.curie.cd2sbgnml.graphics.AnchorPoint;
import fr.curie.cd2sbgnml.LinkWrapper;
import fr.curie.cd2sbgnml.model.Process;
import org.sbml.x2001.ns.celldesigner.CelldesignerBaseProductDocument.CelldesignerBaseProduct;
import org.sbml.x2001.ns.celldesigner.CelldesignerBaseReactantDocument.CelldesignerBaseReactant;
import org.sbml.x2001.ns.celldesigner.CelldesignerListOfModificationDocument.CelldesignerListOfModification;
import org.sbml.x2001.ns.celldesigner.CelldesignerModificationDocument.CelldesignerModification;
import org.sbml.x2001.ns.celldesigner.CelldesignerProductLinkDocument.CelldesignerProductLink;
import org.sbml.x2001.ns.celldesigner.CelldesignerReactantLinkDocument.CelldesignerReactantLink;
import org.sbml.x2001.ns.celldesigner.ReactionDocument.Reaction;
import org.w3c.dom.Node;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * group reactant description
 *  = baseReactant, baseProduct, reactantLink and productLink
 */
public class ReactantWrapper {

    public enum ReactantType {BASE_REACTANT, BASE_PRODUCT, ADDITIONAL_REACTANT, ADDITIONAL_PRODUCT, MODIFICATION}


    private ReactantType reactantType;
    private AliasWrapper aliasW;
    //private LinkWrapper link;
    private AnchorPoint anchorPoint;

    private ReactantWrapper (CelldesignerBaseReactant baseReactant, AliasWrapper aliasW) {
        this.reactantType = ReactantType.BASE_REACTANT;
        this.aliasW = aliasW;

        // get reactanct's link anchor point
        if(baseReactant.isSetCelldesignerLinkAnchor() &&
                ! baseReactant.getCelldesignerLinkAnchor().getDomNode().getAttributes().
                        getNamedItem("position").getNodeValue().equals("INACTIVE")) {
            this.anchorPoint = AnchorPoint.valueOf(baseReactant.getCelldesignerLinkAnchor().getPosition().toString());
        }
        else {
            this.anchorPoint = AnchorPoint.CENTER;
        }

    }

    private ReactantWrapper (CelldesignerBaseProduct baseProduct, AliasWrapper aliasW) {
        this.reactantType = ReactantType.BASE_PRODUCT;
        this.aliasW = aliasW;

        // get reactanct's link anchor point
        // value can be INACTIVE which shouldn't be possible
        // need to fetch it manually to avoid API exception
        if(baseProduct.isSetCelldesignerLinkAnchor() &&
                ! baseProduct.getCelldesignerLinkAnchor().getDomNode().getAttributes().
                        getNamedItem("position").getNodeValue().equals("INACTIVE")) {
                this.anchorPoint = AnchorPoint.valueOf(baseProduct.getCelldesignerLinkAnchor().getPosition().toString());
        }
        else {
            this.anchorPoint = AnchorPoint.CENTER;
        }
    }

    private ReactantWrapper (CelldesignerReactantLink reactantLink, AliasWrapper aliasW) {
        this.reactantType = ReactantType.ADDITIONAL_REACTANT;
        this.aliasW = aliasW;

        // get reactanct's link anchor point
        if(reactantLink.isSetCelldesignerLinkAnchor()) {
            // !!!!!!! limitation of the API here, getCelldesignerLinkAnchor() crashes !!!

            //this.anchorPoint = AnchorPoint.valueOf(reactantLink.getCelldesignerLinkAnchor().getPosition().toString());
            for(int i=0; i < reactantLink.getDomNode().getChildNodes().getLength(); i++) {
                Node n = reactantLink.getDomNode().getChildNodes().item(i);
                if(n.getNodeName().equals("celldesigner_linkAnchor")) {
                    this.anchorPoint = AnchorPoint.valueOf(n.getAttributes().getNamedItem("position").getNodeValue());
                    break;
                }
            }
        }
        else {
            this.anchorPoint = AnchorPoint.CENTER;
        }

    }

    private ReactantWrapper (CelldesignerProductLink productLink, AliasWrapper aliasW) {
        this.reactantType = ReactantType.ADDITIONAL_PRODUCT;
        this.aliasW = aliasW;

        // get reactanct's link anchor point
        if(productLink.isSetCelldesignerLinkAnchor()) {
            // !!!!!!! limitation of the API here, getCelldesignerLinkAnchor() crashes !!!
            //this.anchorPoint = AnchorPoint.valueOf(productLink.getCelldesignerLinkAnchor().getPosition().toString());
            for(int i=0; i < productLink.getDomNode().getChildNodes().getLength(); i++) {
                Node n = productLink.getDomNode().getChildNodes().item(i);
                if(n.getNodeName().equals("celldesigner_linkAnchor")) {
                    this.anchorPoint = AnchorPoint.valueOf(n.getAttributes().getNamedItem("position").getNodeValue());
                    break;
                }
            }
        }
        else {
            this.anchorPoint = AnchorPoint.CENTER;
        }
    }

    // used when guaranteed that modifier is single entity (= no logic gate)
    private ReactantWrapper (CelldesignerModification modification, AliasWrapper aliasW, int index) {
        this.reactantType = ReactantType.MODIFICATION;
        this.aliasW = aliasW;

        // get reactanct's link anchor point
        if(modification.isSetCelldesignerLinkTarget()) {
            // !!!!!!! limitation of the API here, getCelldesignerLinkAnchor() crashes !!!

            //this.anchorPoint = AnchorPoint.valueOf(
             //       modification.getCelldesignerLinkTarget().getCelldesignerLinkAnchor()
             //               getCelldesignerLinkAnchor().getPosition().toString());
            for(int i=0; i < modification.getDomNode().getChildNodes().getLength(); i++) {
                Node n = modification.getDomNode().getChildNodes().item(i);
                if(n.getNodeName().equals("celldesigner_linkTarget")) {
                    for(int j=0; j < n.getChildNodes().getLength(); j++) {
                        Node n2 = n.getChildNodes().item(j);
                        if(n2.getNodeName().equals("celldesigner_linkAnchor")) {
                            try{
                                this.anchorPoint = AnchorPoint.valueOf(n2.getAttributes().getNamedItem("position").getNodeValue());
                            }
                            // here the position can be INACTIVE
                            catch(IllegalArgumentException e) {
                                this.anchorPoint = AnchorPoint.CENTER;
                            }
                            break;
                        }
                    }
                }
            }
        }
        else {
            this.anchorPoint = AnchorPoint.CENTER;
        }
    }

    public static List<ReactantWrapper> fromReaction (Reaction reaction, ModelWrapper modelW) {
        List<ReactantWrapper> reactantList = new ArrayList<>();

        // add all types of reactant species/alias
        Arrays.asList(reaction.getAnnotation().getCelldesignerBaseReactants().getCelldesignerBaseReactantArray())
                .forEach(e -> reactantList.add(new ReactantWrapper(e, modelW.getAliasWrapperFor(e.getAlias()))));

        Arrays.asList(reaction.getAnnotation().getCelldesignerBaseProducts().getCelldesignerBaseProductArray())
                .forEach(e -> reactantList.add(new ReactantWrapper(e, modelW.getAliasWrapperFor(e.getAlias()))));

        if(reaction.getAnnotation().getCelldesignerListOfReactantLinks() != null) {
            Arrays.asList(reaction.getAnnotation().getCelldesignerListOfReactantLinks().getCelldesignerReactantLinkArray())
                    .forEach(e -> reactantList.add(new ReactantWrapper(e, modelW.getAliasWrapperFor(e.getAlias()))));
        }

        if(reaction.getAnnotation().getCelldesignerListOfProductLinks() != null) {
            Arrays.asList(reaction.getAnnotation().getCelldesignerListOfProductLinks().getCelldesignerProductLinkArray())
                    .forEach(e -> reactantList.add(new ReactantWrapper(e, modelW.getAliasWrapperFor(e.getAlias()))));
        }

        if(reaction.getAnnotation().getCelldesignerListOfModification() != null) {
            reactantList.addAll(ReactantWrapper.fromListOfModifications(reaction.getAnnotation().getCelldesignerListOfModification(), modelW));
        }

        return reactantList;
    }

    public static List<ReactantWrapper> fromListOfModifications(CelldesignerListOfModification listOfModification, ModelWrapper modelW) {
        List<ReactantWrapper> reactantList = new ArrayList<>();

        int i=0;
        for(CelldesignerModification modif: listOfModification.getCelldesignerModificationArray()) {
            if(isLogicGate(modif) || modif.getTargetLineIndex().getStringValue().endsWith("0")){
                // TODO logic gate management
            }
            else {
                reactantList.add(new ReactantWrapper(modif, modelW.getAliasWrapperFor(modif.getAliases()), i));
            }
            i++;
        }

        return reactantList;
    }



    public Point2D.Float getCenterPoint() {
        return this.aliasW.getCenterPoint();
    }



    /**
     *
     * @param rectHeight
     * @param rectWidth
     * @param deg
     * @return intersection point relative to rectangle center
     */
    public Point2D rectanglePerimeterPointFromAngle(float rectWidth, float rectHeight, float deg) {
        //System.out.println("width: "+rectWidth+" height: "+rectHeight);
        double twoPI = Math.PI*2;
        double theta = deg * Math.PI / 180;
        //System.out.println(theta);

        /*while (theta < -Math.PI) {
            theta += twoPI;
        }

        while (theta > Math.PI) {
            theta -= twoPI;
        }*/
        //System.out.println(theta);

        double rectAtan = Math.atan2(rectHeight, rectWidth);
        double tanTheta = Math.tan(theta);
        //System.out.println(rectAtan+" "+tanTheta);
        int region;

        if ((theta > -rectAtan) && (theta <= rectAtan)) {
            region = 1;
        } else if ((theta > rectAtan) && (theta <= (Math.PI - rectAtan))) {
            region = 2;
        } else if ((theta > (Math.PI - rectAtan)) || (theta <= -(Math.PI - rectAtan))) {
            region = 3;
        } else {
            region = 4;
        }
        //System.out.println("region: "+region);

        Point2D edgePoint;
        //System.out.println("edgepoint1 "+edgePoint);
        int xFactor = 1;
        int yFactor = 1;

        switch (region) {
            case 1: yFactor = -1; break;
            case 2: yFactor = -1; break;
            case 3: xFactor = -1; break;
            case 4: xFactor = -1; break;
        }
        //System.out.println(xFactor+" "+yFactor);

        if ((region == 1) || (region == 3)) {
            edgePoint = new Point2D.Float(
                    xFactor * (rectWidth / 2),
                    (float) (yFactor * (rectHeight / 2) * tanTheta));
        } else {
            edgePoint = new Point2D.Float(
                    (float) (xFactor * (rectWidth / (2 * tanTheta))),
                    yFactor * (rectHeight /  2));
        }
        //System.out.println("edgepoint "+edgePoint);
        return edgePoint;
    }


    public static int getProcessAnchorIndex(CelldesignerModification modif) {
        String targetLineIndex = modif.getTargetLineIndex().getStringValue();
        return Integer.parseInt(targetLineIndex.split(",")[1]);
    }

    public static boolean isLogicGate(CelldesignerModification modif) {
        return modif.getModifiers().split(",").length > 1;
    }

    public AliasWrapper getAliasW() {
        return aliasW;
    }

    public ReactantType getReactantType() {
        return reactantType;
    }

    /*public LinkWrapper getLink() {
        return link;
    }*/

    /*public void setLink(LinkWrapper link) {
        this.link = link;
    }*/

    public float getHeight() {
        return Float.parseFloat(this.aliasW.getBounds().getH());
    }

    public float getWidth() {
        return Float.parseFloat(this.aliasW.getBounds().getW());
    }

    /*@Override
    public SpeciesWrapper.CdShape getShape() {
        return this.aliasW.getSpeciesW().getCdShape();
    }*/

    public AnchorPoint getAnchorPoint() {
        return anchorPoint;
    }

}
