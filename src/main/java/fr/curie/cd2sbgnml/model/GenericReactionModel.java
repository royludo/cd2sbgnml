package fr.curie.cd2sbgnml.model;

import fr.curie.cd2sbgnml.graphics.AnchorPoint;
import fr.curie.cd2sbgnml.graphics.GeometryUtils;
import fr.curie.cd2sbgnml.graphics.Link;
import fr.curie.cd2sbgnml.xmlcdwrappers.LogicGateWrapper;
import fr.curie.cd2sbgnml.xmlcdwrappers.ReactantWrapper;
import fr.curie.cd2sbgnml.xmlcdwrappers.ReactionWrapper;
import fr.curie.cd2sbgnml.xmlcdwrappers.StyleInfo;
import org.sbml.x2001.ns.celldesigner.CelldesignerModificationDocument.CelldesignerModification;
import org.sbml.x2001.ns.celldesigner.ReactionDocument.Reaction;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.*;


public class GenericReactionModel {

    private List<ReactionNodeModel> reactionNodeModels;
    private List<ReactantModel> reactantModels;
    private List<LinkModel> linkModels;

    private String cdReactionType;
    private boolean hasProcess;
    private String id;

    public GenericReactionModel(ReactionWrapper reactionW) {
        this.reactionNodeModels = new ArrayList<>();
        this.reactantModels = new ArrayList<>();
        this.linkModels = new ArrayList<>();
        this.cdReactionType = reactionW.getReactionType();
        this.hasProcess = reactionW.hasProcess();
        this.id = reactionW.getId();

    }

    /**
     * return a map of reactants to a logic gate, can be used to reassign reactants to correct gate.
     * @param reactionW
     * @param process
     * @return
     */
    private HashMap<ReactantWrapper, String> addLogicGates(ReactionWrapper reactionW, Process process) {
        HashMap<ReactantWrapper, String> reactantToLogicGateMap = new HashMap<>();

        for(LogicGateWrapper logicW: reactionW.getLogicGates()) {
            System.out.println("logic gate: "+logicW.getModificationType()+" "+logicW.getType());


            CelldesignerModification modif = reactionW.getReaction().getAnnotation().
                    getCelldesignerListOfModification().getCelldesignerModificationArray(logicW.getPositionIndex());
            Point2D.Float processAnchorPoint = process.getAbsoluteAnchorCoords(ReactantWrapper.getProcessAnchorIndex(modif));
            // list edit points
            List<Point2D.Float> editPoints = ReactionWrapper.getEditPointsForModifier(reactionW.getReaction(), logicW.getPositionIndex());
            System.out.println("gate edit points "+editPoints);

            // process logic gate point
            Point2D.Float logicGateGlobalCoord = editPoints.get(editPoints.size() - 1); // last point listed in xml
            editPoints = editPoints.subList(0, editPoints.size() - 1);
            System.out.println("Rest of edit points: "+editPoints);


            String logicId = "logicglyph_" + UUID.randomUUID();
            LogicGate logicGate = new LogicGate(logicGateGlobalCoord, logicId,
                    logicW.getType(),
                    // logic gate inherits the style of its link pointing to the process
                    new StyleInfo(logicW.getModification(), logicId));
            for(ReactantWrapper reactantW: reactionW.getModifiers()) {
                if(reactantW.getLogicGate() != null && reactantW.getLogicGate().equals(logicW)) {
                    reactantToLogicGateMap.put(reactantW, logicId);
                }
            }

            List<AffineTransform> transformList =
                    GeometryUtils.getTransformsToGlobalCoords(
                            logicGateGlobalCoord,
                            processAnchorPoint);
            List<Point2D.Float> absoluteEditPoints = new ArrayList<>();
            absoluteEditPoints.add(logicGateGlobalCoord);
            absoluteEditPoints.addAll(GeometryUtils.convertPoints(editPoints, transformList));
            absoluteEditPoints.add(processAnchorPoint);

            absoluteEditPoints = GeometryUtils.getNormalizedEndPoints(absoluteEditPoints,
                    logicGate.getGlyph(),
                    process.getGlyph(),
                    AnchorPoint.E,
                    AnchorPoint.E);

            System.out.println("FINAL logic gate edit points "+absoluteEditPoints);

            // port management
            Point2D.Float pIn = logicGate.getGlyph().getCenter();
            Point2D.Float pOut = absoluteEditPoints.get(1);
            logicGate.setPorts(pIn, pOut);

            // replace the end and start points of the sublines by corresponding ports
            absoluteEditPoints.set(0, logicGate.getPortOut());

            String logicArcId = "logicarc_" + UUID.randomUUID();
            LinkModel logicLink = new LinkModel(logicGate, process, new Link(absoluteEditPoints),
                    logicArcId,
                    LinkModel.getSbgnClass(logicW.getModificationType()),
                    new StyleInfo(logicW.getModification(), logicArcId));
            this.getReactionNodeModels().add(logicGate);
            this.getLinkModels().add(logicLink);
        }

        return reactantToLogicGateMap;
    }

    //public void addModifiers(Reaction reaction, Process process, int modifIndex, AnchorPoint anchorPoint ) {}

    public void addModifiers(ReactionWrapper reactionW, Process process) {
        // start with logic gates
        HashMap<ReactantWrapper, String> reactantToLogicGateMap = this.addLogicGates(reactionW, process);

        for(ReactantWrapper reactantW: reactionW.getModifiers()) {
            // simple case, no logic gate
            System.out.println("modifier: "+reactantW.getAliasW().getId());

            ReactantModel modifModel = new ReactantModel(this, reactantW);

            Reaction reaction = reactionW.getReaction();
            int modifIndex = reactantW.getPositionIndex();
            List<Point2D.Float> editPoints = ReactionWrapper.getEditPointsForModifier(reaction, modifIndex);
            CelldesignerModification modif = reaction.getAnnotation().
                    getCelldesignerListOfModification().getCelldesignerModificationArray(modifIndex);

            // treat modifier as linked to a reactionNodeModel, either a process or a logic gate
            ReactionNodeModel genericNode = null;
            Point2D.Float genericNodeAnchorPoint;
            String linkType;
            if(reactantW.getLogicGate() != null) { // linked to logic gate
                String logicId = reactantToLogicGateMap.get(reactantW) ;
                for(ReactionNodeModel nodeModel: this.getReactionNodeModels()) {
                    if(nodeModel.getId().equals(logicId)) {
                        genericNode = nodeModel;
                    }
                }
                genericNodeAnchorPoint = genericNode.getPortIn();
                linkType = "logic arc";
            }
            else { // modifier is linked to process
                genericNode = process;
                genericNodeAnchorPoint = process.getAbsoluteAnchorCoords(ReactantWrapper.getProcessAnchorIndex(modif));
                linkType = LinkModel.getSbgnClass(modif.getType());

            }


            System.out.println("edit points: "+editPoints);

            List<AffineTransform> transformList =
                    GeometryUtils.getTransformsToGlobalCoords(
                            modifModel.getAbsoluteAnchorCoordinate(
                                    reactantW.getAnchorPoint()),
                            genericNodeAnchorPoint);
            List<Point2D.Float> absoluteEditPoints = new ArrayList<>();
            absoluteEditPoints.add(modifModel.getAbsoluteAnchorCoordinate(reactantW.getAnchorPoint()));
            absoluteEditPoints.addAll(GeometryUtils.convertPoints(editPoints, transformList));
            absoluteEditPoints.add(genericNodeAnchorPoint);

            absoluteEditPoints = GeometryUtils.getNormalizedEndPoints(absoluteEditPoints,
                    modifModel.getGlyph(),
                    genericNode.getGlyph(),
                    modifModel.getAnchorPoint(),
                    AnchorPoint.E);

            String linkCdClass = reaction.getAnnotation().getCelldesignerListOfModification().
                    getCelldesignerModificationArray(modifIndex).getType();

            String modifId = "modif_" + UUID.randomUUID();
            LinkModel modifLink = new LinkModel(modifModel, genericNode, new Link(absoluteEditPoints),
                    modifId,
                    linkType,
                    new StyleInfo(modif, modifId));

            /*LinkWrapper link = new LinkWrapper(reactantW, process, absoluteEditPoints,
                    modifIndex, linkCdClass);
            link.setSbgnSpacePointList(
                    link.getNormalizedEndPoints(
                            reactantW.getAnchorPoint(), GeometryUtils.AnchorPoint.CENTER
                    ));*/

            // add everything to the reaction lists
            this.getReactantModels().add(modifModel);
            this.getLinkModels().add(modifLink);
        }
    }

    public void addAdditionalReactants(ReactionWrapper reactionW, Process process) {
        for(ReactantWrapper reactantW: reactionW.getAdditionalReactants()) {
            ReactantModel reactantModel = new ReactantModel(this, reactantW);

            List<AffineTransform> transformList =
                    GeometryUtils.getTransformsToGlobalCoords(
                            reactantModel.getAbsoluteAnchorCoordinate(
                                    reactantW.getAnchorPoint()),
                            process.getAbsoluteAnchorCoords(0));

            int positionIndex = reactantW.getPositionIndex();
            System.out.println("POSITION INDEX "+positionIndex);
            Reaction reaction = reactionW.getReaction();
            List<Point2D.Float> editPoints = ReactionWrapper.getEditPointsForAdditionalReactant(reaction, positionIndex);
            System.out.println("ADDITIONAL REACT EDIT POINTS "+editPoints);

            List<Point2D.Float> absoluteEditPoints = new ArrayList<>();
            absoluteEditPoints.add(reactantModel.getAbsoluteAnchorCoordinate(reactantW.getAnchorPoint()));
            absoluteEditPoints.addAll(GeometryUtils.convertPoints(editPoints, transformList));
            absoluteEditPoints.add(process.getAbsoluteAnchorCoords(0));
            System.out.println("ABSOLUTE POINTS: "+absoluteEditPoints);

            Point2D.Float normalizedStart = GeometryUtils.normalizePoint(absoluteEditPoints.get(0),
                    absoluteEditPoints.get(1),
                    reactantModel.getGlyph(),
                    reactantModel.getAnchorPoint());

            List<Point2D.Float> normalizedEditPoints = new ArrayList<>();
            normalizedEditPoints.add(normalizedStart);
            normalizedEditPoints.addAll(GeometryUtils.convertPoints(editPoints, transformList));
            normalizedEditPoints.add(process.getPortIn());

            String reactLinkId = "addreact_"+ UUID.randomUUID();
            LinkModel reactLink = new LinkModel(reactantModel, process, new Link(normalizedEditPoints),
                    reactLinkId,
                    "consumption",
                    new StyleInfo(reactionW.getReaction().getAnnotation()
                            .getCelldesignerListOfReactantLinks()
                            .getCelldesignerReactantLinkArray(positionIndex), reactLinkId));

            if(reactionW.isReversible()) {
                reactLink.reverse();
            }

            // add everything to the reaction lists
            this.getReactantModels().add(reactantModel);
            this.getLinkModels().add(reactLink);
        }
    }

    public void addAdditionalProducts(ReactionWrapper reactionW, Process process) {
        for(ReactantWrapper reactantW: reactionW.getAdditionalProducts()) {
            ReactantModel reactantModel = new ReactantModel(this, reactantW);

            List<AffineTransform> transformList =
                    GeometryUtils.getTransformsToGlobalCoords(
                            process.getAbsoluteAnchorCoords(1),
                            reactantModel.getAbsoluteAnchorCoordinate(
                                    reactantW.getAnchorPoint()));

            int positionIndex = reactantW.getPositionIndex();
            System.out.println("POSITION INDEX "+positionIndex);
            Reaction reaction = reactionW.getReaction();
            List<Point2D.Float> editPoints = ReactionWrapper.getEditPointsForAdditionalProduct(reaction, positionIndex);
            System.out.println("ADDITIONAL REACT EDIT POINTS "+editPoints);

            List<Point2D.Float> absoluteEditPoints = new ArrayList<>();
            absoluteEditPoints.add(process.getAbsoluteAnchorCoords(1));
            absoluteEditPoints.addAll(GeometryUtils.convertPoints(editPoints, transformList));
            absoluteEditPoints.add(reactantModel.getAbsoluteAnchorCoordinate(reactantW.getAnchorPoint()));
            System.out.println("ABSOLUTE POINTS: "+absoluteEditPoints);

            Point2D.Float normalizedEnd = GeometryUtils.normalizePoint(absoluteEditPoints.get(absoluteEditPoints.size() - 1),
                    absoluteEditPoints.get(absoluteEditPoints.size() - 2),
                    reactantModel.getGlyph(),
                    reactantModel.getAnchorPoint());

            List<Point2D.Float> normalizedEditPoints = new ArrayList<>();
            normalizedEditPoints.add(process.getPortOut());
            normalizedEditPoints.addAll(GeometryUtils.convertPoints(editPoints, transformList));
            normalizedEditPoints.add(normalizedEnd);

            String reactLinkId = "addprod_" + UUID.randomUUID();
            LinkModel reactLink = new LinkModel(process, reactantModel, new Link(normalizedEditPoints),
                    reactLinkId, "production",
                    new StyleInfo(reactionW.getReaction().getAnnotation()
                            .getCelldesignerListOfProductLinks()
                            .getCelldesignerProductLinkArray(positionIndex), reactLinkId));

            // add everything to the reaction lists
            this.getReactantModels().add(reactantModel);
            this.getLinkModels().add(reactLink);
        }
    }

    /*
     //TODO remove, probably unused anymore
    public Point2D getProcessCoords (ModelWrapper modelW) {

        // TODO also take complexSpeciesAlias into account <- should be ok

        //System.out.println("reactant "+reactantBbox.getX()+" "+reactantBbox.getY()+" "+reactantBbox.getW()+" "+reactantBbox.getH());
        //System.out.println("product "+productBbox.getX()+" "+productBbox.getY()+" "+productBbox.getW()+" "+productBbox.getH());

        Point2D processCenter = null;
        if(!this.isBranchType()) {
            // TODO take anchor point into account
            // for now, only from center
            Point2D reactantStart = this.baseReactants.get(0).getLinkStartingPoint();
            Point2D productEnd = this.baseProducts.get(0).getLinkStartingPoint();

            LinkWrapper baseLink = this.baseLinks.get(0);

            if(baseLink.getPointList().size() == 0) {
                //System.out.println(reactantStart+" "+productEnd);

                processCenter = new Point2D.Double(
                        reactantStart.getX() + (productEnd.getX() - reactantStart.getX()) / 2,
                        reactantStart.getY() + (productEnd.getY() - reactantStart.getY()) / 2
                );
            }
            else {
                Point2D segmentStart;
                Point2D segmentEnd;
                int segmentCount = baseLink.getPointList().size() + 1;
                if(this.processSegmentIndex == 0) { // on first segment
                    segmentStart = reactantStart;
                    segmentEnd = baseLink.getPointList().get(0);
                }
                else if(this.processSegmentIndex == segmentCount - 1) { // on last segment
                    segmentStart = baseLink.getPointList().get(segmentCount - 2);
                    segmentEnd = productEnd;
                }
                else {
                    segmentStart = baseLink.getPointList().get(this.processSegmentIndex - 1);
                    segmentEnd = baseLink.getPointList().get(this.processSegmentIndex);
                }

                processCenter = new Point2D.Double(
                        segmentStart.getX() + (segmentEnd.getX() - segmentStart.getX()) / 2,
                        segmentStart.getY() + (segmentEnd.getY() - segmentStart.getY()) / 2
                );

            }
        }
        else {
            processCenter = new Point2D.Float(0,0);
        }

        return processCenter;

    }*/

    /**
     * CellDesigner uses a special coordinate system to define the edit points of a link.
     * The x axis goes along a direct line passing through the center of both involved elements.
     * Origin of the x axis is the center of the start element, 1 is the center of the end element.
     * The y axis is orthogonal, its origin is the same as x, and the 1 coordinate is at the same distance as x.
     * Y axis is oriented on the right of x, following the global coordinate system of the map.
     *
     * It means that points going beyond the center of elements can have coordinates > 1 or < 0.
     *
     */
    /*public Point2f localToAbsoluteCoord(Point2f cStart, Point2f cEnd, Point2f p) {
        return new Point2f();
    }*/


    /**
     * Given a 3-point coordinate system, gets the absolute coordinates of a relative point
     * @param origin
     * @param pX
     * @param pY
     * @param editPoint
     * @return
     */
    public static Point2D.Float getAbsolutePoint(Point2D.Float origin, Point2D.Float pX, Point2D.Float pY, Point2D.Float editPoint) {

        // transform association glyph point
        Point2D.Float absolutePoint = new Point2D.Float((float) editPoint.getX(), (float) editPoint.getY());
        for(AffineTransform t: GeometryUtils.getTransformsToGlobalCoords(origin, pX, pY)) {
            t.transform(absolutePoint, absolutePoint);
        }

        return absolutePoint;
    }


    /**
     * already comprises start and end
     * @param origin
     * @param pX
     * @param branch
     * @return
     */
    public static List<Point2D.Float> getBranchPoints(ReactionWrapper reactionW, Point2D.Float origin, Point2D.Float pX, int branch) {

        List<Point2D.Float> absoluteEditPoints = new ArrayList<>();
        absoluteEditPoints.add(origin);
        System.out.println("local system: "+origin+" "+pX);
        System.out.println("points for BRANCH "+branch+" "+ ReactionWrapper.getEditPointsForBranch(reactionW.getReaction(), branch));

        for (Point2D editP : ReactionWrapper.getEditPointsForBranch(reactionW.getReaction(), branch)) {
            Point2D p = new Point2D.Double(editP.getX(), editP.getY());

            for(AffineTransform t: GeometryUtils.getTransformsToGlobalCoords(origin, pX)) {
                t.transform(p, p);
            }

            System.out.println("BRANCH "+branch+" result: " + editP + " -> " + p.toString());
            absoluteEditPoints.add(new Point2D.Float((float) p.getX(), (float) p.getY()));

        }
        absoluteEditPoints.add(pX);
        System.out.println("BRANCH "+branch+" stack: "+absoluteEditPoints);

        return absoluteEditPoints;
    }

    public Process getProcess() {
        for(ReactionNodeModel node: this.getReactionNodeModels()) {
            if(node instanceof Process) {
                return (Process) node;
            }
        }
        throw new RuntimeException("Reaction has no process "+this.getId());
    }

    public AssocDissoc getAssocDissoc() {
        for(ReactionNodeModel node: this.getReactionNodeModels()) {
            if(node instanceof AssocDissoc) {
                return (AssocDissoc) node;
            }
        }
        throw new RuntimeException("Reaction has no association or dissociation glyph "+this.getId());
    }

    public List<ReactionNodeModel> getReactionNodeModels() {
        return reactionNodeModels;
    }

    public List<ReactantModel> getReactantModels() {
        return reactantModels;
    }

    public List<LinkModel> getLinkModels() {
        return linkModels;
    }

    public String getCdReactionType() {
        return cdReactionType;
    }

    public boolean hasProcess() {
        return hasProcess;
    }

    public String getId() {
        return id;
    }
}
