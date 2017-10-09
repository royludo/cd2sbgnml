package fr.curie.cd2sbgnml.xmlcdwrappers;

import fr.curie.cd2sbgnml.graphics.AnchorPoint;
import org.sbml.x2001.ns.celldesigner.CelldesignerConnectSchemeDocument.CelldesignerConnectScheme;
import org.sbml.x2001.ns.celldesigner.CelldesignerModificationDocument;
import org.sbml.x2001.ns.celldesigner.CelldesignerModificationDocument.CelldesignerModification;
import org.sbml.x2001.ns.celldesigner.CelldesignerProductLinkDocument;
import org.sbml.x2001.ns.celldesigner.CelldesignerProductLinkDocument.CelldesignerProductLink;
import org.sbml.x2001.ns.celldesigner.CelldesignerReactantLinkDocument;
import org.sbml.x2001.ns.celldesigner.CelldesignerReactantLinkDocument.CelldesignerReactantLink;
import org.sbml.x2001.ns.celldesigner.ReactionDocument.Reaction;
import org.w3c.dom.Node;

import java.awt.geom.Point2D;
import java.util.*;

public class ReactionWrapper {

    private String id;
    private List<ReactantWrapper> baseReactants;
    private List<ReactantWrapper> baseProducts;
    private List<ReactantWrapper> additionalReactants;
    private List<ReactantWrapper> additionalProducts;
    private List<ReactantWrapper> modifiers;
    private Reaction reaction;
    private int processSegmentIndex;
    private String reactionType;
    private boolean hasProcess;

    public ReactionWrapper (Reaction reaction, ModelWrapper modelW) {
        this.id = reaction.getId();
        this.reaction = reaction;

        this.baseReactants = new ArrayList<>();
        this.baseProducts = new ArrayList<>();
        this.additionalReactants = new ArrayList<>();
        this.additionalProducts = new ArrayList<>();
        this.modifiers = new ArrayList<>();
        this.processSegmentIndex = getProcessSegment(reaction);
        this.reactionType = reaction.getAnnotation().getCelldesignerReactionType().
                getDomNode().getChildNodes().item(0).getNodeValue();
        this.hasProcess = hasProcess(reaction);

        // fill the corresponding lists
        for(ReactantWrapper reactW: ReactantWrapper.fromReaction(reaction, modelW)) {
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
        CelldesignerConnectScheme connectScheme = reaction.getAnnotation().getCelldesignerConnectScheme();
        if(connectScheme.getDomNode().
                getAttributes().getNamedItem("rectangleIndex") != null) {
            return Integer.parseInt(connectScheme.getDomNode().
                    getAttributes().getNamedItem("rectangleIndex").getNodeValue());
        }
        else {
            if(reaction.getAnnotation().isSetCelldesignerEditPoints()
                    && reaction.getAnnotation().getCelldesignerEditPoints().isSetTShapeIndex()) {
                return Integer.parseInt(reaction.getAnnotation().getCelldesignerEditPoints().getTShapeIndex());
            }
            else {
                // default to 1st segment, if nothing is specified
                return 0;
            }
        }
    }

    public static boolean hasProcess(Reaction reaction) {
        CelldesignerConnectScheme connectScheme = reaction.getAnnotation().getCelldesignerConnectScheme();
        if(connectScheme.getDomNode().
                getAttributes().getNamedItem("rectangleIndex") != null) {
            return true;
        }
        else {
            if(reaction.getAnnotation().isSetCelldesignerEditPoints()
                    && reaction.getAnnotation().getCelldesignerEditPoints().isSetTShapeIndex()) {
                return true;
            }
            else {
                return false;
            }
        }
    }

    /**
     * Parse a string of the form "x,y x2,y2 ..." to a list of Point2D
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

    /**
     * Get the list of edit points for the base reaction
     * @return
     */
    public static List<Point2D.Float> getBaseEditPoints (Reaction reaction){
        if(!reaction.getAnnotation().isSetCelldesignerEditPoints()) {
            return new ArrayList<>();
        }

        String editPointString = reaction.getAnnotation().getCelldesignerEditPoints().
                getDomNode().getFirstChild().getNodeValue();
        return parseEditPointsString(editPointString);
    }

    public static List<Point2D.Float> getEditPointsForBranch(Reaction reaction, int b) {
        List<Point2D.Float> editPoints = getBaseEditPoints(reaction);
        int num0 = Integer.parseInt(reaction.getAnnotation().getCelldesignerEditPoints().getNum0());
        int num1 = Integer.parseInt(reaction.getAnnotation().getCelldesignerEditPoints().getNum1());
        int num2 = Integer.parseInt(reaction.getAnnotation().getCelldesignerEditPoints().getNum2());

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
        CelldesignerModification modif = reaction.getAnnotation().
                getCelldesignerListOfModification().getCelldesignerModificationArray(index);

        if(!modif.isSetEditPoints()) {
            return new ArrayList<>();
        }

        String editPointString = modif.getEditPoints().getStringValue();
        return parseEditPointsString(editPointString);
    }

    /**
     * get edit points for the additional reactant located at position: index in the xml list of additional reactant
     * @param reaction
     * @param index
     * @return
     */
    public static List<Point2D.Float> getEditPointsForAdditionalReactant(Reaction reaction, int index) {
        CelldesignerReactantLink reactLink = reaction.getAnnotation().
                getCelldesignerListOfReactantLinks().getCelldesignerReactantLinkArray(index);

        for(int i=0; i < reactLink.getDomNode().getChildNodes().getLength(); i++) {
            Node n = reactLink.getDomNode().getChildNodes().item(i);
            if(n.getNodeName().equals("celldesigner_editPoints")) {
                return parseEditPointsString(n.getChildNodes().item(0).getNodeValue());
            }
        }

        return new ArrayList<>();
    }

    public static List<Point2D.Float> getEditPointsForAdditionalProduct(Reaction reaction, int index) {
        CelldesignerProductLink reactLink = reaction.getAnnotation().
                getCelldesignerListOfProductLinks().getCelldesignerProductLinkArray(index);

        for(int i=0; i < reactLink.getDomNode().getChildNodes().getLength(); i++) {
            Node n = reactLink.getDomNode().getChildNodes().item(i);
            if(n.getNodeName().equals("celldesigner_editPoints")) {
                return parseEditPointsString(n.getChildNodes().item(0).getNodeValue());
            }
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

    public CelldesignerConnectScheme getBaseConnectScheme() {
        return this.getReaction().getAnnotation().getCelldesignerConnectScheme();
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





}
