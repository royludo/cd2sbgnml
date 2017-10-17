package fr.curie.cd2sbgnml.xmlcdwrappers;

import fr.curie.cd2sbgnml.graphics.AnchorPoint;
import org.sbml.x2001.ns.celldesigner.CelldesignerBaseProductDocument.CelldesignerBaseProduct;
import org.sbml.x2001.ns.celldesigner.CelldesignerBaseReactantDocument.CelldesignerBaseReactant;
import org.sbml.x2001.ns.celldesigner.CelldesignerListOfModificationDocument.CelldesignerListOfModification;
import org.sbml.x2001.ns.celldesigner.CelldesignerModificationDocument.CelldesignerModification;
import org.sbml.x2001.ns.celldesigner.CelldesignerProductLinkDocument.CelldesignerProductLink;
import org.sbml.x2001.ns.celldesigner.CelldesignerReactantLinkDocument.CelldesignerReactantLink;
import org.sbml.x2001.ns.celldesigner.ReactionDocument.Reaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

import java.awt.geom.Point2D;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * group reactant description
 *  = baseReactant, baseProduct, reactantLink and productLink
 */
public class ReactantWrapper {

    private static final Logger logger = LoggerFactory.getLogger(ReactantWrapper.class);

    public enum ReactantType {BASE_REACTANT, BASE_PRODUCT, ADDITIONAL_REACTANT, ADDITIONAL_PRODUCT, MODIFICATION}


    private ReactantType reactantType;
    private AliasWrapper aliasW;
    //private LinkWrapper link;
    private AnchorPoint anchorPoint;
    /**
     * The index of the reactant in the corresponding list (modification,  additional product...) in the
     * xml file
     */
    private int positionIndex;

    /**
     * If the reactantWrapper is connected to a logic gate, store the reference of the gate.
     */
    private LogicGateWrapper logicGate;

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

    private ReactantWrapper (CelldesignerReactantLink reactantLink, AliasWrapper aliasW, int index) {
        this.reactantType = ReactantType.ADDITIONAL_REACTANT;
        this.aliasW = aliasW;
        this.positionIndex = index;

        // get reactanct's link anchor point
        this.anchorPoint = AnchorPoint.CENTER; // default to center
        if(reactantLink.isSetCelldesignerLinkAnchor()) {
            // !!!!!!! limitation of the API here, getCelldesignerLinkAnchor() crashes !!!

            //this.anchorPoint = AnchorPoint.valueOf(reactantLink.getCelldesignerLinkAnchor().getPosition().toString());
            for(int i=0; i < reactantLink.getDomNode().getChildNodes().getLength(); i++) {
                Node n = reactantLink.getDomNode().getChildNodes().item(i);
                if(n.getNodeName().equals("celldesigner_linkAnchor") &&
                        ! n.getAttributes().getNamedItem("position").getNodeValue().equals("INACTIVE")) {
                    this.anchorPoint = AnchorPoint.valueOf(n.getAttributes().getNamedItem("position").getNodeValue());
                    break;
                }
            }
        }

    }

    private ReactantWrapper (CelldesignerProductLink productLink, AliasWrapper aliasW, int index) {
        this.reactantType = ReactantType.ADDITIONAL_PRODUCT;
        this.aliasW = aliasW;
        this.positionIndex = index;

        // get reactanct's link anchor point
        this.anchorPoint = AnchorPoint.CENTER; // default to center
        if(productLink.isSetCelldesignerLinkAnchor()) {
            // !!!!!!! limitation of the API here, getCelldesignerLinkAnchor() crashes !!!
            //this.anchorPoint = AnchorPoint.valueOf(productLink.getCelldesignerLinkAnchor().getPosition().toString());
            for(int i=0; i < productLink.getDomNode().getChildNodes().getLength(); i++) {
                Node n = productLink.getDomNode().getChildNodes().item(i);
                if(n.getNodeName().equals("celldesigner_linkAnchor") &&
                        ! n.getAttributes().getNamedItem("position").getNodeValue().equals("INACTIVE")) {
                    this.anchorPoint = AnchorPoint.valueOf(n.getAttributes().getNamedItem("position").getNodeValue());
                    break;
                }
            }
        }
    }

    // used when guaranteed that modifier is single entity (= no logic gate)
    private ReactantWrapper (CelldesignerModification modification, AliasWrapper aliasW, int index) {
        this.reactantType = ReactantType.MODIFICATION;
        this.aliasW = aliasW;
        this.positionIndex = index;

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

    // used for modifiers linked to a logic gate
    private ReactantWrapper (CelldesignerModification modification, AliasWrapper aliasW, int index, LogicGateWrapper gateRef) {
        this(modification, aliasW, index);
        this.logicGate = gateRef;
    }

    public static SimpleEntry<List<ReactantWrapper>, List<LogicGateWrapper>> fromReaction (Reaction reaction, ModelWrapper modelW) {
        List<ReactantWrapper> reactantList = new ArrayList<>();
        List<LogicGateWrapper> logicGateWrapperList = new ArrayList<>();

        // add all types of reactant species/alias
        Arrays.asList(reaction.getAnnotation().getCelldesignerBaseReactants().getCelldesignerBaseReactantArray())
                .forEach(e -> reactantList.add(new ReactantWrapper(e, modelW.getAliasWrapperFor(e.getAlias()))));

        Arrays.asList(reaction.getAnnotation().getCelldesignerBaseProducts().getCelldesignerBaseProductArray())
                .forEach(e -> reactantList.add(new ReactantWrapper(e, modelW.getAliasWrapperFor(e.getAlias()))));

        if(reaction.getAnnotation().getCelldesignerListOfReactantLinks() != null) {
            int i=0;
            for(CelldesignerReactantLink rlink: Arrays.asList(
                    reaction.getAnnotation().getCelldesignerListOfReactantLinks().getCelldesignerReactantLinkArray())) {
                reactantList.add(new ReactantWrapper(rlink, modelW.getAliasWrapperFor(rlink.getAlias()), i));
                i++;
            }
        }

        if(reaction.getAnnotation().getCelldesignerListOfProductLinks() != null) {
            int i=0;
            for(CelldesignerProductLink plink: Arrays.asList(
                    reaction.getAnnotation().getCelldesignerListOfProductLinks().getCelldesignerProductLinkArray())) {
                reactantList.add(new ReactantWrapper(plink, modelW.getAliasWrapperFor(plink.getAlias()), i));
                i++;
            }
        }

        if(reaction.getAnnotation().getCelldesignerListOfModification() != null) {
            SimpleEntry<List<ReactantWrapper>, List<LogicGateWrapper>> result =
                    ReactantWrapper.fromListOfModifications(reaction.getAnnotation().getCelldesignerListOfModification(), modelW);
            reactantList.addAll(result.getKey());
            logicGateWrapperList.addAll(result.getValue());
        }

        return new SimpleEntry<>(reactantList, logicGateWrapperList);
    }

    public static SimpleEntry<List<ReactantWrapper>, List<LogicGateWrapper>> fromListOfModifications(CelldesignerListOfModification listOfModification, ModelWrapper modelW) {
        List<ReactantWrapper> reactantList = new ArrayList<>();
        List<LogicGateWrapper> logicGateWrapperList = new ArrayList<>();

        /*
            If we encounter a logic gate, store a reference so we can link subsequent reactant to it.
            If several logic gate for the same reaction, they will be reached sequentially after their linked
            reactants are processed, so we only need to keep 1 reference that can be overwritten.
         */
        LogicGateWrapper logicGateRef = null;

        int i=0;
        for(CelldesignerModification modif: listOfModification.getCelldesignerModificationArray()) {
            logger.debug("Parsing modification "+modif.getModifiers()+" "+modif.getType());
            if(isLogicGate(modif)){ // logic gate case
                logicGateRef = new LogicGateWrapper(modif, i);
                logicGateWrapperList.add(logicGateRef);

            }
            // we assume that the logic gate will always be defined before it's linked reactants
            // targetLineIndex may not be present, when additional glyphs are added to an already existing logic gate
            // TODO make this clean using the ids defined in the logic gate
            else if(logicGateRef != null && ( !modif.isSetTargetLineIndex() ||
                    modif.getTargetLineIndex().getStringValue().endsWith("0"))){ // glyph connected to logic gate case
                reactantList.add(new ReactantWrapper(modif, modelW.getAliasWrapperFor(modif.getAliases()), i, logicGateRef));
            }
            else {
                reactantList.add(new ReactantWrapper(modif, modelW.getAliasWrapperFor(modif.getAliases()), i));
            }
            i++;
        }

        return new SimpleEntry<>(reactantList, logicGateWrapperList);
    }



    public Point2D.Float getCenterPoint() {
        return this.aliasW.getCenterPoint();
    }

    public static int getProcessAnchorIndex(CelldesignerModification modif) {
        // in some cases targetLineIndex may not be present (see logic gates modifiers)
        if(!modif.isSetTargetLineIndex()) {
            return 0;
        }
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

    public int getPositionIndex() {
        return positionIndex;
    }

    public LogicGateWrapper getLogicGate() {
        return logicGate;
    }
}
