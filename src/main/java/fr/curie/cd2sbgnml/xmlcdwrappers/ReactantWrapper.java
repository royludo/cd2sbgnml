package fr.curie.cd2sbgnml.xmlcdwrappers;

import fr.curie.cd2sbgnml.graphics.AnchorPoint;
import org.sbml._2001.ns.celldesigner.*;
import org.sbml.sbml.level2.version4.Reaction;
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
    public enum ModificationLinkType {
        CATALYSIS, UNKNOWN_CATALYSIS,
        INHIBITION, UNKNOWN_INHIBITION,
        TRANSPORT,
        HETERODIMER_ASSOCIATION, DISSOCIATION,
        TRANSCRIPTIONAL_ACTIVATION, TRANSCRIPTIONAL_INHIBITION,
        TRANSLATIONAL_ACTIVATION, TRANSLATIONAL_INHIBITION,
        PHYSICAL_STIMULATION, MODULATION, TRIGGER,
        REDUCED_TRIGGER, NEGATIVE_INFLUENCE,
        BOOLEAN_LOGIC_GATE_OR,
        BOOLEAN_LOGIC_GATE_AND,
        BOOLEAN_LOGIC_GATE_NOT,
        BOOLEAN_LOGIC_GATE_UNKNOWN
    }


    private ReactantType reactantType;
    private AliasWrapper aliasW;
    private AnchorPoint anchorPoint;
    private ModificationLinkType modificationLinkType;
    private String targetLineIndex;


    /**
     * For reactantsWrapper that are NOT baseReactants or baseProducts. Those 2 have their lineWrapper defined
     * in the reactionWrapper.
     */
    private LineWrapper lineWrapper;

    /**
     * The index of the reactant in the corresponding list (modification,  additional product...) in the
     * xml file
     */
    private int positionIndex;

    /**
     * If the reactantWrapper is connected to a logic gate, store the reference of the gate.
     */
    private LogicGateWrapper logicGate;

    private ReactantWrapper (BaseReactant baseReactant, AliasWrapper aliasW) {
        this.reactantType = ReactantType.BASE_REACTANT;
        this.aliasW = aliasW;

        // get reactanct's link anchor point
        if(baseReactant.getLinkAnchor() != null &&
                ! baseReactant.getLinkAnchor().getPosition().equals("INACTIVE")) {
            this.anchorPoint = AnchorPoint.valueOf(baseReactant.getLinkAnchor().getPosition());
        }
        else {
            this.anchorPoint = AnchorPoint.CENTER;
        }

    }

    private ReactantWrapper (BaseProduct baseProduct, AliasWrapper aliasW) {
        this.reactantType = ReactantType.BASE_PRODUCT;
        this.aliasW = aliasW;

        // get reactanct's link anchor point
        // value can be INACTIVE which shouldn't be possible
        // need to fetch it manually to avoid API exception
        if(baseProduct.getLinkAnchor() != null &&
                ! baseProduct.getLinkAnchor().getPosition().equals("INACTIVE")) {
                this.anchorPoint = AnchorPoint.valueOf(baseProduct.getLinkAnchor().getPosition());
        }
        else {
            this.anchorPoint = AnchorPoint.CENTER;
        }
    }

    private ReactantWrapper (ReactantLink reactantLink, AliasWrapper aliasW, int index) {
        this.reactantType = ReactantType.ADDITIONAL_REACTANT;
        this.aliasW = aliasW;
        this.positionIndex = index;
        this.targetLineIndex = reactantLink.getTargetLineIndex();

        // get reactanct's link anchor point
        this.anchorPoint = AnchorPoint.CENTER; // default to center
        if(reactantLink.getLinkAnchor() != null) {
            // !!!!!!! limitation of the API here, getCelldesignerLinkAnchor() crashes !!!

            this.anchorPoint = AnchorPoint.valueOf(reactantLink.getLinkAnchor().getPosition());

            /*for(int i=0; i < reactantLink.getDomNode().getChildNodes().getLength(); i++) {
                Node n = reactantLink.getDomNode().getChildNodes().item(i);
                if(n.getNodeName().equals("celldesigner_linkAnchor") &&
                        ! n.getAttributes().getNamedItem("position").getNodeValue().equals("INACTIVE")) {
                    this.anchorPoint = AnchorPoint.valueOf(n.getAttributes().getNamedItem("position").getNodeValue());
                    break;
                }
            }*/
        }

        this.lineWrapper = new LineWrapper(reactantLink.getConnectScheme(),
                reactantLink.getEditPoints(),
                reactantLink.getLine());

    }

    private ReactantWrapper (ProductLink productLink, AliasWrapper aliasW, int index) {
        this.reactantType = ReactantType.ADDITIONAL_PRODUCT;
        this.aliasW = aliasW;
        this.positionIndex = index;
        this.targetLineIndex = productLink.getTargetLineIndex();

        // get reactanct's link anchor point
        this.anchorPoint = AnchorPoint.CENTER; // default to center
        if(productLink.getLinkAnchor() != null) {
            // !!!!!!! limitation of the API here, getCelldesignerLinkAnchor() crashes !!!
            this.anchorPoint = AnchorPoint.valueOf(productLink.getLinkAnchor().getPosition());
            /*for(int i=0; i < productLink.getDomNode().getChildNodes().getLength(); i++) {
                Node n = productLink.getDomNode().getChildNodes().item(i);
                if(n.getNodeName().equals("celldesigner_linkAnchor") &&
                        ! n.getAttributes().getNamedItem("position").getNodeValue().equals("INACTIVE")) {
                    this.anchorPoint = AnchorPoint.valueOf(n.getAttributes().getNamedItem("position").getNodeValue());
                    break;
                }
            }*/
        }

        this.lineWrapper = new LineWrapper(productLink.getConnectScheme(),
                productLink.getEditPoints(),
                productLink.getLine());
    }

    // used when guaranteed that modifier is single entity (= no logic gate)
    protected ReactantWrapper (Modification modification, AliasWrapper aliasW, int index) {
        this.reactantType = ReactantType.MODIFICATION;
        this.aliasW = aliasW;
        this.positionIndex = index;
        this.targetLineIndex = modification.getTargetLineIndex();
        this.modificationLinkType = ModificationLinkType.valueOf(modification.getType());

        // get reactanct's link anchor point
        if(modification.getLinkTarget().size() > 0
                && modification.getLinkTarget().get(0).getLinkAnchor() != null) {

                String position = modification.getLinkTarget().get(0).getLinkAnchor().getPosition();
                /*
                    In ACSN, it is possible to get an empty string for some modifications linkAnchor
                 */
                try {
                    this.anchorPoint = AnchorPoint.valueOf(position);
                } catch(IllegalArgumentException e) {
                    logger.warn("Illegal linkAnchor position found: "+position+". CENTER will be used instead");
                    this.anchorPoint = AnchorPoint.CENTER;
                }
        }
        else {
            this.anchorPoint = AnchorPoint.CENTER;
        }

        this.lineWrapper = new LineWrapper(modification.getConnectScheme(),
                modification.getEditPoints(),
                modification.getLine());
    }

    // used for modifiers linked to a logic gate
    private ReactantWrapper (Modification modification, AliasWrapper aliasW, int index, LogicGateWrapper gateRef) {
        this(modification, aliasW, index);
        this.logicGate = gateRef;
    }

    public static SimpleEntry<List<ReactantWrapper>, List<LogicGateWrapper>> fromReaction (Reaction reaction, ModelWrapper modelW) {
        List<ReactantWrapper> reactantList = new ArrayList<>();
        List<LogicGateWrapper> logicGateWrapperList = new ArrayList<>();

        // add all types of reactant species/alias
        reaction.getAnnotation().getExtension().getBaseReactants().getBaseReactant()
                .forEach(e -> reactantList.add(new ReactantWrapper(e, modelW.getAliasWrapperFor(e.getAlias()))));

        reaction.getAnnotation().getExtension().getBaseProducts().getBaseProduct()
                .forEach(e -> reactantList.add(new ReactantWrapper(e, modelW.getAliasWrapperFor(e.getAlias()))));

        if(reaction.getAnnotation().getExtension().getListOfReactantLinks() != null) {
            int i=0;
            for(ReactantLink rlink: reaction.getAnnotation().getExtension().getListOfReactantLinks().getReactantLink()){
                reactantList.add(new ReactantWrapper(rlink, modelW.getAliasWrapperFor(rlink.getAlias()), i));
                i++;
            }
        }

        if(reaction.getAnnotation().getExtension().getListOfProductLinks() != null) {
            int i=0;
            for(ProductLink plink: reaction.getAnnotation().getExtension().getListOfProductLinks().getProductLink()) {
                reactantList.add(new ReactantWrapper(plink, modelW.getAliasWrapperFor(plink.getAlias()), i));
                i++;
            }
        }

        if(reaction.getAnnotation().getExtension().getListOfModification() != null) {
            SimpleEntry<List<ReactantWrapper>, List<LogicGateWrapper>> result =
                    ReactantWrapper.fromListOfModifications(reaction.getAnnotation().getExtension().getListOfModification(), modelW);
            reactantList.addAll(result.getKey());
            logicGateWrapperList.addAll(result.getValue());
        }

        return new SimpleEntry<>(reactantList, logicGateWrapperList);
    }

    public static SimpleEntry<List<ReactantWrapper>, List<LogicGateWrapper>> fromListOfModifications(
            ListOfModification listOfModification, ModelWrapper modelW) {

        List<ReactantWrapper> reactantList = new ArrayList<>();
        List<LogicGateWrapper> logicGateWrapperList = new ArrayList<>();

        /*
            If we encounter a logic gate, store a reference so we can link subsequent reactant to it.
            If several logic gate for the same reaction, they will be reached sequentially after their linked
            reactants are processed, so we only need to keep 1 reference that can be overwritten.
         */
        LogicGateWrapper logicGateRef = null;

        int i=0;
        for(Modification modif: listOfModification.getModification()) {
            logger.debug("Parsing modification "+modif.getModifiers()+" "+modif.getType());
            if(isLogicGate(modif)){ // logic gate case
                logicGateRef = new LogicGateWrapper(modif, i);
                logicGateWrapperList.add(logicGateRef);

            }
            // we assume that the logic gate will always be defined before it's linked reactants
            // targetLineIndex may not be present, when additional glyphs are added to an already existing logic gate
            // TODO make this clean using the ids defined in the logic gate
            else if(logicGateRef != null && ( modif.getTargetLineIndex() != null ||
                    modif.getTargetLineIndex().endsWith("0"))){ // glyph connected to logic gate case
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

    public static int getProcessAnchorIndex(Modification modif) {
        // in some cases targetLineIndex may not be present (see logic gates modifiers)
        if(modif.getTargetLineIndex() == null) {
            return 0;
        }
        String targetLineIndex = modif.getTargetLineIndex();
        return Integer.parseInt(targetLineIndex.split(",")[1]);
    }

    public int getProcessAnchorIndex() {
        // in some cases targetLineIndex may not be present (see logic gates modifiers)
        if(this.getTargetLineIndex() == null) {
            return 0;
        }
        String targetLineIndex = this.getTargetLineIndex();
        return Integer.parseInt(targetLineIndex.split(",")[1]);
    }

    public List<Point2D.Float> getEditPointsForBranch(int b) {
        List<Point2D.Float> editPoints = this.getLineWrapper().getEditPoints();
        int num0 = this.getLineWrapper().getNum0();
        int num1 = this.getLineWrapper().getNum1();
        int num2 = this.getLineWrapper().getNum2();

        List<Point2D.Float> finalEditPoints = new ArrayList<>();
        switch(b) {
            case 0:
                for(int i=0; i < num0; i++) {
                    finalEditPoints.add(editPoints.get(i));
                }
                break;
            case 1:
                for(int i=num0; i < num0 + num1; i++) {
                    finalEditPoints.add(editPoints.get(i));
                }
                break;
            case 2:
                // don't go to the end of edit points list, last one may be
                // for association/dissociation point or for logic gate
                for(int i=num0 + num1; i < num0 + num1 + num2; i++) {
                    finalEditPoints.add(editPoints.get(i));
                }
                break;
            default:
                throw new RuntimeException("Value: "+b+" not allowed for branch index. Authorized values: 0, 1, 2.");
        }
        return finalEditPoints;
    }

    public static boolean isLogicGate(Modification modif) {
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
        return this.aliasW.getBounds().getH().floatValue();
    }

    public float getWidth() {
        return this.aliasW.getBounds().getW().floatValue();
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

    public LineWrapper getLineWrapper() {
        return lineWrapper;
    }

    public void setLineWrapper(LineWrapper lineWrapper) {
        this.lineWrapper = lineWrapper;
    }

    public ModificationLinkType getModificationLinkType() {
        return modificationLinkType;
    }

    public void setModificationLinkType(ModificationLinkType modificationLinkType) {
        this.modificationLinkType = modificationLinkType;
    }

    public String getTargetLineIndex() {
        return targetLineIndex;
    }

    public void setTargetLineIndex(String targetLineIndex) {
        this.targetLineIndex = targetLineIndex;
    }

    public void setReactantType(ReactantType reactantType) {
        this.reactantType = reactantType;
    }

    public void setAliasW(AliasWrapper aliasW) {
        this.aliasW = aliasW;
    }

    public void setAnchorPoint(AnchorPoint anchorPoint) {
        this.anchorPoint = anchorPoint;
    }

    public void setPositionIndex(int positionIndex) {
        this.positionIndex = positionIndex;
    }

    public void setLogicGate(LogicGateWrapper logicGate) {
        this.logicGate = logicGate;
    }
}
