package fr.curie.cd2sbgnml.xmlcdwrappers;

import org.sbml._2001.ns.celldesigner.*;
import org.sbml._2001.ns.celldesigner.ReactionAnnotationType.Extension;
import org.sbml.sbml.level2.version4.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import java.awt.geom.Point2D;
import java.util.*;
import java.util.AbstractMap.SimpleEntry;

import static fr.curie.cd2sbgnml.xmlcdwrappers.ModificationLinkType.*;

public class ReactionWrapper implements INotesFeature, IAnnotationsFeature {

    private static final Logger logger = LoggerFactory.getLogger(ReactionWrapper.class);

    private String id;
    private List<ReactantWrapper> baseReactants;
    private List<ReactantWrapper> baseProducts;
    private List<ReactantWrapper> additionalReactants;
    private List<ReactantWrapper> additionalProducts;
    private List<ReactantWrapper> modifiers;
    private List<LogicGateWrapper> logicGates;
    private int processSegmentIndex;
    private ReactionType reactionType;
    private boolean hasProcess;
    private boolean isReversible;
    private LineWrapper lineWrapper;
    private Element notes;
    private Element annotations;

    public ReactionWrapper (String id, ReactionType type,
                            List<ReactantWrapper> baseReactants, List<ReactantWrapper> baseProducts) {
        this.id = id;
        this.reactionType = type;
        this.baseReactants = baseReactants;
        this.baseProducts = baseProducts;
        this.additionalProducts = new ArrayList<>();
        this.additionalReactants = new ArrayList<>();
        this.modifiers = new ArrayList<>();
        this.logicGates = new ArrayList<>();
    }

    public ReactionWrapper (Reaction reaction, ModelWrapper modelW) {
        this.id = reaction.getId();

        this.baseReactants = new ArrayList<>();
        this.baseProducts = new ArrayList<>();
        this.additionalReactants = new ArrayList<>();
        this.additionalProducts = new ArrayList<>();
        this.modifiers = new ArrayList<>();
        this.logicGates = new ArrayList<>();
        this.processSegmentIndex = getProcessSegment(reaction);
        this.reactionType = ReactionType.valueOf(reaction.getAnnotation().getExtension().getReactionType());
        this.hasProcess = hasProcess(reaction);
        this.isReversible = reaction.isReversible(); //  reaction.isSetReversible() ? Boolean.parseBoolean(reaction.getReversible()) : true;
        this.lineWrapper = new LineWrapper(reaction.getAnnotation().getExtension().getConnectScheme(),
                reaction.getAnnotation().getExtension().getEditPoints(),
                reaction.getAnnotation().getExtension().getLine());
        this.notes = Utils.getNotes(reaction.getNotes());
        this.annotations = Utils.getRDFAnnotations(reaction.getAnnotation().getAny());

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

    public List<Point2D.Float> getEditPointsForModifier() {
        //Modification modif = reaction.getAnnotation().
         //       getExtension().getListOfModification().getModification().get(index);

        if(this.getLineWrapper().getEditPoints().size() == 0) {
            return new ArrayList<>();
        }

        /*
        In ACSN some isseteditPoints can return true without having any editpoints, and then return empty string
         */
        // TODO check if ok
        if(this.getLineWrapper().getEditPoints().size() > 0) { // && !modif.getEditPoints().getStringValue().equals("")) {
            return this.getLineWrapper().getEditPoints();
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

    public Reaction getCDReaction() {
        Reaction reaction = new Reaction();
        reaction.setId(this.getId());
        reaction.setMetaid(this.getId());
        reaction.setReversible(this.isReversible());

        // init base lists
        ListOfSpeciesReferences listOfReactants = new ListOfSpeciesReferences();
        reaction.setListOfReactants(listOfReactants);
        ListOfSpeciesReferences listOfProducts = new ListOfSpeciesReferences();
        reaction.setListOfProducts(listOfProducts);

        // init annotation part
        ReactionAnnotationType annotation = new ReactionAnnotationType();
        reaction.setAnnotation(annotation);

        Extension ext = new Extension();
        annotation.setExtension(ext);

        ext.setReactionType(this.getReactionType().toString());

        ext.setConnectScheme(this.getLineWrapper().getCDConnectScheme());
        ext.setLine(this.getLineWrapper().getCDLine());
        boolean isBranchType =
                this.getReactionType() == ReactionType.HETERODIMER_ASSOCIATION
                || this.getReactionType() == ReactionType.DISSOCIATION;

        if(this.getLineWrapper().getEditPoints().size() > 0) {
            ext.setEditPoints(this.getLineWrapper().getCDEditPoints(isBranchType));
        }

        // metaids cannot be the same, so we keep a global counter for the reaction, and use the reaction
        // id in the metaid. This should ensure uniqueness in all the document.
        int metaCounter = 1;

        // base reactant list
        BaseReactants baseReactants = new BaseReactants();
        ext.setBaseReactants(baseReactants);
        for(ReactantWrapper reactantWrapper: this.getBaseReactants()) {
            BaseReactant baseReactant = (BaseReactant) reactantWrapper.getCDElement();
            baseReactants.getBaseReactant().add(baseReactant);

            listOfReactants.getSpeciesReference().add(getSpeciesReference(reactantWrapper, metaCounter));
            metaCounter++;
        }

        // base product list
        BaseProducts baseProducts = new BaseProducts();
        ext.setBaseProducts(baseProducts);
        for(ReactantWrapper reactantWrapper: this.getBaseProducts()) {
            BaseProduct baseProduct = (BaseProduct) reactantWrapper.getCDElement();
            baseProducts.getBaseProduct().add(baseProduct);

            listOfProducts.getSpeciesReference().add(getSpeciesReference(reactantWrapper, metaCounter));
            metaCounter++;
        }

        if(this.getAdditionalReactants().size() > 0) {
            ListOfReactantLinks listOfReactantLinks = new ListOfReactantLinks();
            for(ReactantWrapper w: this.getAdditionalReactants()) {
                listOfReactantLinks.getReactantLink().add((ReactantLink) w.getCDElement());

                listOfReactants.getSpeciesReference().add(getSpeciesReference(w, metaCounter));
                metaCounter++;
            }
            ext.setListOfReactantLinks(listOfReactantLinks);
        }

        if(this.getAdditionalProducts().size() > 0) {
            ListOfProductLinks listOfProductLinks = new ListOfProductLinks();
            for(ReactantWrapper w: this.getAdditionalProducts()) {
                listOfProductLinks.getProductLink().add((ProductLink) w.getCDElement());

                listOfProducts.getSpeciesReference().add(getSpeciesReference(w, metaCounter));
                metaCounter++;
            }
            ext.setListOfProductLinks(listOfProductLinks);
        }

        // if special logic gate reaction, we need to add gate members in addition to the base reactants and product
        if(this.getReactionType().equals("BOOLEAN_LOGIC_GATE")) {
            ListOfGateMember listOfGateMember = new ListOfGateMember();

            listOfGateMember.getGateMember().add(((LogicGateWrapper)this.getModifiers().get(0)).getCDElement());

            for(ReactantWrapper reactantWrapper: this.getBaseReactants()) {
                listOfGateMember.getGateMember().add(reactantWrapper.getAsModification());
            }

            ext.setListOfGateMember(listOfGateMember);
        }
        else {
            if (this.getModifiers().size() > 0) {
                ListOfModifierSpeciesReferences listOfModifiers = new ListOfModifierSpeciesReferences();
                reaction.setListOfModifiers(listOfModifiers);

                ListOfModification listOfModification = new ListOfModification();
                logger.debug("Number of modifiers to serialize: " + this.getModifiers().size());
                for (ReactantWrapper w : this.getModifiers()) {
                    listOfModification.getModification().add((Modification) w.getCDElement());

                    // create associated speciesReference for the sbml list
                    logger.debug("modif type: " + w.getModificationLinkType() + " " + w.getAliasW() + " " + this.getId() +
                            " " + this.getModifiers().size() + " " + (w instanceof LogicGateWrapper));
                    if (w.getAliasW() != null)
                        logger.debug("isincluded ? " + w.getAliasW().getSpeciesW().isIncludedSpecies());
                    /*
                        Logic gates are not listed in the species reference
                        If included species, the species reference must be the one of the topmost parent complex
                     */
                    if (!(w.getModificationLinkType() == BOOLEAN_LOGIC_GATE_UNKNOWN
                            || w.getModificationLinkType() == BOOLEAN_LOGIC_GATE_AND
                            || w.getModificationLinkType() == BOOLEAN_LOGIC_GATE_OR
                            || w.getModificationLinkType() == BOOLEAN_LOGIC_GATE_NOT)) {

                        listOfModifiers.getModifierSpeciesReference().add(getModifierSpeciesReference(w, metaCounter));
                        metaCounter++;
                    }
                }
                ext.setListOfModification(listOfModification);
            }
        }

        // notes and annotations
        if(this.getNotes() != null) {
            SBase.Notes notes = new SBase.Notes();
            reaction.setNotes(notes);
            notes.getAny().add(this.getNotes());
        }

        if(this.getAnnotations() != null) {
            annotation.getAny().add(this.getAnnotations());
        }

        return reaction;
    }

    /**
     * Metaids cannot be the same
     * @param w
     * @return
     */
    private SpeciesReference getSpeciesReference(ReactantWrapper w, int metaCount) {
        String aliasId, speciesId;
        if(w.getAliasW().getSpeciesW().isIncludedSpecies()) {
            aliasId = w.getAliasW().getTopLevelParent().getId();
            speciesId = w.getAliasW().getTopLevelParent().getSpeciesId();
        }
        else {
            aliasId = w.getAliasW().getId();
            speciesId = w.getAliasW().getSpeciesW().getId();
        }

        // create associated speciesReference for the sbml list
        SpeciesReference speciesReference = new SpeciesReference();
        speciesReference.setSpecies(speciesId);
        speciesReference.setMetaid(this.getId()+"_meta"+metaCount+"_"+speciesId);

        SpeciesReferenceAnnotationType speciesRefAnnotation = new SpeciesReferenceAnnotationType();
        speciesReference.setAnnotation(speciesRefAnnotation);

        SpeciesReferenceAnnotationType.Extension speciesRefExt = new SpeciesReferenceAnnotationType.Extension();
        speciesRefAnnotation.setExtension(speciesRefExt);

        speciesRefExt.setAlias(aliasId);

        return speciesReference;
    }

    private ModifierSpeciesReference getModifierSpeciesReference(ReactantWrapper w, int metaCount) {
        String aliasId, speciesId;
        if(w.getAliasW().getSpeciesW().isIncludedSpecies()) {
            aliasId = w.getAliasW().getTopLevelParent().getId();
            speciesId = w.getAliasW().getTopLevelParent().getSpeciesId();
        }
        else {
            aliasId = w.getAliasW().getId();
            speciesId = w.getAliasW().getSpeciesW().getId();
        }

        // create associated speciesReference for the sbml list
        ModifierSpeciesReference speciesReference = new ModifierSpeciesReference();
        speciesReference.setSpecies(speciesId);
        speciesReference.setMetaid(this.getId()+"_meta"+metaCount+"_"+speciesId);

        SpeciesReferenceAnnotationType speciesRefAnnotation = new SpeciesReferenceAnnotationType();
        speciesReference.setAnnotation(speciesRefAnnotation);

        SpeciesReferenceAnnotationType.Extension speciesRefExt = new SpeciesReferenceAnnotationType.Extension();
        speciesRefAnnotation.setExtension(speciesRefExt);

        speciesRefExt.setAlias(aliasId);

        return speciesReference;
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

    public ReactionType getReactionType() {
        return reactionType;
    }

    public List<LogicGateWrapper> getLogicGates() {
        return logicGates;
    }

    public boolean isReversible() {
        return isReversible;
    }

    public LineWrapper getLineWrapper() {
        return lineWrapper;
    }

    public void setLineWrapper(LineWrapper lineWrapper) {
        this.lineWrapper = lineWrapper;
    }

    public Element getNotes() {
        return notes;
    }

    public Element getAnnotations() {
        return annotations;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setProcessSegmentIndex(int processSegmentIndex) {
        this.processSegmentIndex = processSegmentIndex;
    }

    public void setReactionType(ReactionType reactionType) {
        this.reactionType = reactionType;
    }

    public void setHasProcess(boolean hasProcess) {
        this.hasProcess = hasProcess;
    }

    public void setReversible(boolean reversible) {
        isReversible = reversible;
    }

    public void setNotes(Element notes) {
        this.notes = notes;
    }

    public void setAnnotations(Element annotations) {
        this.annotations = annotations;
    }
}
