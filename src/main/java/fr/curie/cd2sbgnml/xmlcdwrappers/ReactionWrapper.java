package fr.curie.cd2sbgnml.xmlcdwrappers;

import org.sbml._2001.ns.celldesigner.ConnectScheme;
import org.sbml._2001.ns.celldesigner.Modification;
import org.sbml._2001.ns.celldesigner.ProductLink;
import org.sbml._2001.ns.celldesigner.ReactantLink;
import org.sbml.sbml.level2.version4.Reaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

import java.awt.geom.Point2D;
import java.net.ConnectException;
import java.util.*;
import java.util.AbstractMap.SimpleEntry;

public class ReactionWrapper {

    private static final Logger logger = LoggerFactory.getLogger(ReactionWrapper.class);

    private String id;
    private List<ReactantWrapper> baseReactants;
    private List<ReactantWrapper> baseProducts;
    private List<ReactantWrapper> additionalReactants;
    private List<ReactantWrapper> additionalProducts;
    private List<ReactantWrapper> modifiers;
    private List<LogicGateWrapper> logicGates;
    private Reaction reaction;
    private int processSegmentIndex;
    private String reactionType;
    private boolean hasProcess;
    private boolean isReversible;

    public ReactionWrapper (Reaction reaction, ModelWrapper modelW) {
        this.id = reaction.getId();
        this.reaction = reaction;

        this.baseReactants = new ArrayList<>();
        this.baseProducts = new ArrayList<>();
        this.additionalReactants = new ArrayList<>();
        this.additionalProducts = new ArrayList<>();
        this.modifiers = new ArrayList<>();
        this.logicGates = new ArrayList<>();
        this.processSegmentIndex = getProcessSegment(reaction);
        this.reactionType = reaction.getAnnotation().getExtension().getReactionType();
        this.hasProcess = hasProcess(reaction);
        this.isReversible = reaction.isReversible(); //  reaction.isSetReversible() ? Boolean.parseBoolean(reaction.getReversible()) : true;

        // fill the corresponding lists
        SimpleEntry<List<ReactantWrapper>, List<LogicGateWrapper>> wrapperListTuple = ReactantWrapper.fromReaction(reaction, modelW);
        List<ReactantWrapper> reactantWrapperList = wrapperListTuple.getKey();
        List<LogicGateWrapper> logicGateWrapperList = wrapperListTuple.getValue();
        this.logicGates.addAll(logicGateWrapperList);

        for(ReactantWrapper reactW: reactantWrapperList) {
            switch(reactW.getReactantType()){
                case BASE_REACTANT: this.baseReactants.add(reactW); break;
                case BASE_PRODUCT:this.baseProducts.add(reactW); break;
                case ADDITIONAL_REACTANT:this.additionalReactants.add(reactW); break;
                case ADDITIONAL_PRODUCT: this.additionalProducts.add(reactW); break;
                case MODIFICATION: this.modifiers.add(reactW); break;
            }

            //System.out.println("Reactant link start point: "+ reaction.getId()+" "+reactW.getAliasW().getId()+" "+reactW.getLinkStartingPoint());
        }

    }

    public boolean isBranchTypeLeft() {
        if(this.baseReactants.size() > 1 && this.baseProducts.size() > 1) {
            throw new RuntimeException("Multiple branches on both sides of reaction: "+this.getId()+" unforeseen case.");
        }
        return this.baseReactants.size() > 1 && this.baseProducts.size() == 1;
    }

    public boolean isBranchTypeRight() {
        if(this.baseReactants.size() > 1 && this.baseProducts.size() > 1) {
            throw new RuntimeException("Multiple branches on both sides of reaction: "+this.getId()+" unforeseen case.");
        }
        return this.baseReactants.size() == 1 && this.baseProducts.size() > 1;
    }

    // we assume there can never be multiple branches on the left AND right at the same time
    // branch amount != reactant amount
    // branching is defined by base products and reactant only
    public boolean isBranchType() {
        return this.isBranchTypeLeft() || this.isBranchTypeRight();
    }

    /**
     * Get the segment index on which the process glyph of a reaction is located.
     * If no process is present (direct connection), 0 (1st segment) is returned.
     * If reaction has branches (association/dissociation), segment index is taken from tshapeIndex
     * @param reaction sbml reaction element
     * @return index as int starting from 0
     */
    public static int getProcessSegment(Reaction reaction) {
        ConnectScheme connectScheme = reaction.getAnnotation().getExtension().getConnectScheme();

        /*
         * in ACSN, connectScheme element is missing in some places (apoptosis)
         */
        if(connectScheme == null) {
            logger.warn("ConnectScheme element missing for reaction: "+reaction.getId());
            return 0;
        }

        if(connectScheme.getRectangleIndex() != null) {
            return Integer.parseInt(connectScheme.getRectangleIndex());
        }
        else {
            if(reaction.getAnnotation().getExtension().getEditPoints() != null
                    && reaction.getAnnotation().getExtension().getEditPoints().getTShapeIndex() != null) {
                return (int) reaction.getAnnotation().getExtension ().getEditPoints().getTShapeIndex();
            }
            else {
                // default to 1st segment, if nothing is specified
                return 0;
            }
        }
    }

    public static boolean hasProcess(Reaction reaction) {
        ConnectScheme connectScheme = reaction.getAnnotation().getExtension().getConnectScheme();

        /*
         * in ACSN, connectScheme element is missing in some places (apoptosis)
         */
        if(connectScheme == null) {
            return true;
        }

        if(connectScheme.getRectangleIndex() != null) {
            return true;
        }
        else {
            if(reaction.getAnnotation().getExtension().getEditPoints() != null
                    && reaction.getAnnotation().getExtension().getEditPoints().getTShapeIndex() != null) {
                return true;
            }
            else {
                return false;
            }
        }
    }

    /**
     * Parse a string of the form "x,y x2,y2 ..." to a list of Point2D
     * @deprecated
     * @param editPointString
     * @return
     */
    public static List<Point2D.Float> parseEditPointsString(String editPointString) {
        List<Point2D.Float> editPoints = new ArrayList<>();
        Arrays.stream(editPointString.split(" ")).
                forEach(e -> {
                    String[] tmp = e.split(",");
                    editPoints.add(new Point2D.Float(Float.parseFloat(tmp[0]), Float.parseFloat(tmp[1])));
                });
        return editPoints;
    }

    public static List<Point2D.Float> parseEditPointsString(List<String> editPointString) {
        List<Point2D.Float> editPoints = new ArrayList<>();
        editPointString.stream().
                forEach(e -> {
                    String[] tmp = e.split(",");
                    editPoints.add(new Point2D.Float(Float.parseFloat(tmp[0]), Float.parseFloat(tmp[1])));
                });
        return editPoints;
    }

    /**
     * Get the list of edit points for the base reaction
     * @return
     */
    public static List<Point2D.Float> getBaseEditPoints (Reaction reaction){
        if(reaction.getAnnotation().getExtension().getEditPoints() == null) {
            return new ArrayList<>();
        }

        // TODO check if ok
        List<String> editPointString = reaction.getAnnotation().getExtension().getEditPoints().getValue();
        return parseEditPointsString(editPointString);
    }

    public static List<Point2D.Float> getEditPointsForBranch(Reaction reaction, int b) {
        List<Point2D.Float> editPoints = getBaseEditPoints(reaction);
        int num0 = (int) reaction.getAnnotation().getExtension().getEditPoints().getNum0();
        int num1 = (int) reaction.getAnnotation().getExtension().getEditPoints().getNum1();
        int num2 = (int) reaction.getAnnotation().getExtension().getEditPoints().getNum2();

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

    /**
     * get edit points for the modifier located at position: index in the xml list of modifiers
     * @param reaction
     * @param index
     * @return
     */
    public static List<Point2D.Float> getEditPointsForModifier(Reaction reaction, int index) {
        Modification modif = reaction.getAnnotation().
                getExtension().getListOfModification().getModification().get(index);

        if(modif.getEditPoints() == null) {
            return new ArrayList<>();
        }

        /*
        In ACSN some isseteditPoints can return true without having any editpoints, and then return empty string
         */
        // TODO check if ok
        if(modif.getEditPoints() != null) { // && !modif.getEditPoints().getStringValue().equals("")) {
            List<String> editPointString = modif.getEditPoints();
            return parseEditPointsString(editPointString);
        }
        else {
            return new ArrayList<>();
        }

    }

    /**
     * get edit points for the additional reactant located at position: index in the xml list of additional reactant
     * @param reaction
     * @param index
     * @return
     */
    public static List<Point2D.Float> getEditPointsForAdditionalReactant(Reaction reaction, int index) {
        ReactantLink reactLink = reaction.getAnnotation().
                getExtension().getListOfReactantLinks().getReactantLink().get(index);

        if(reactLink.getEditPoints() != null) {
            return parseEditPointsString(reactLink.getEditPoints().getValue());
        }
        return new ArrayList<>();
    }

    public static List<Point2D.Float> getEditPointsForAdditionalProduct(Reaction reaction, int index) {
        ProductLink reactLink = reaction.getAnnotation().
                getExtension().getListOfProductLinks().getProductLink().get(index);

        if(reactLink.getEditPoints() != null) {
            return parseEditPointsString(reactLink.getEditPoints().getValue());
        }
        return new ArrayList<>();

    }


    public boolean hasProcess(){
        return this.hasProcess;
    }

    public List<ReactantWrapper> getReactantList() {
        List<ReactantWrapper> reactList = new ArrayList<>();
        reactList.addAll(this.getBaseReactants());
        reactList.addAll(this.getBaseProducts());
        reactList.addAll(this.getAdditionalReactants());
        reactList.addAll(this.getAdditionalProducts());
        reactList.addAll(this.getModifiers());
        return reactList;
    }

    public String getId() {
        return id;
    }

    public ConnectScheme getBaseConnectScheme() {
        return this.getReaction().getAnnotation().getExtension().getConnectScheme();
    }

    public Reaction getReaction() {
        return reaction;
    }

    public List<ReactantWrapper> getBaseReactants() {
        return baseReactants;
    }

    public List<ReactantWrapper> getBaseProducts() {
        return baseProducts;
    }

    public List<ReactantWrapper> getAdditionalReactants() {
        return additionalReactants;
    }

    public List<ReactantWrapper> getAdditionalProducts() {
        return additionalProducts;
    }

    public List<ReactantWrapper> getModifiers() {
        return modifiers;
    }

    public int getProcessSegmentIndex() {
        return processSegmentIndex;
    }

    public String getReactionType() {
        return reactionType;
    }

    public List<LogicGateWrapper> getLogicGates() {
        return logicGates;
    }

    public boolean isReversible() {
        return isReversible;
    }
}
