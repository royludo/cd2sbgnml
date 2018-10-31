package fr.curie.cd2sbgnml.model;

import fr.curie.cd2sbgnml.graphics.AnchorPoint;
import fr.curie.cd2sbgnml.graphics.GeometryUtils;
import fr.curie.cd2sbgnml.graphics.Link;

import fr.curie.cd2sbgnml.xmlcdwrappers.*;
import fr.curie.cd2sbgnml.xmlcdwrappers.LogicGateWrapper.LogicGateType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

import java.util.*;

/**
 * Contains parts that are common to simple, association and dissociation reactions.
 */
public class GenericReactionModel {

    private static final Logger logger = LoggerFactory.getLogger(GenericReactionModel.class);

    private List<ReactionNodeModel> reactionNodeModels;
    private List<ReactantModel> reactantModels;
    private List<LinkModel> linkModels;

    private ReactionType cdReactionType;
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
            logger.trace("logic gate: "+logicW.getModificationType()+" "+logicW.getType());


            Point2D.Float processAnchorPoint = process.getAbsoluteAnchorCoords(logicW.getProcessAnchorIndex());
            // list edit points
            List<Point2D.Float> editPoints = logicW.getLineWrapper().getEditPoints(); // ReactionWrapper.getEditPointsForModifier(reactionW.getReaction(), logicW.getPositionIndex());
            logger.trace("gate edit points "+editPoints);

            // process logic gate point
            Point2D.Float logicGateGlobalCoord = editPoints.get(editPoints.size() - 1); // last point listed in xml
            editPoints = editPoints.subList(0, editPoints.size() - 1);
            logger.trace("Rest of edit points: "+editPoints);


            String logicId = "logicglyph_" + UUID.randomUUID();
            LogicGate logicGate = new LogicGate(logicGateGlobalCoord, logicId,
                    logicW.getType(),
                    // logic gate inherits the style of its link pointing to the process
                    new StyleInfo(logicW.getLineWrapper().getLineWidth(),
                            logicW.getLineWrapper().getLineColor(), logicId));
            for(ReactantWrapper reactantW: reactionW.getModifiers()) {
                if(reactantW.getLogicGate() != null && reactantW.getLogicGate().equals(logicW)) {
                    reactantToLogicGateMap.put(reactantW, logicId);
                }
            }

            AffineTransform transform =
                    GeometryUtils.getTransformsToGlobalCoords(
                            logicGateGlobalCoord,
                            processAnchorPoint);
            List<Point2D.Float> absoluteEditPoints = new ArrayList<>();
            absoluteEditPoints.add(logicGateGlobalCoord);
            absoluteEditPoints.addAll(GeometryUtils.convertPoints(editPoints, transform));
            absoluteEditPoints.add(processAnchorPoint);

            absoluteEditPoints = GeometryUtils.getNormalizedEndPoints(absoluteEditPoints,
                    logicGate.getGlyph(),
                    process.getGlyph(),
                    AnchorPoint.E,
                    AnchorPoint.E);

            logger.trace("FINAL logic gate edit points "+absoluteEditPoints);

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
                    new StyleInfo(logicW.getLineWrapper().getLineWidth(),
                            logicW.getLineWrapper().getLineColor(), logicArcId));
            this.getReactionNodeModels().add(logicGate);
            this.getLinkModels().add(logicLink);
        }

        return reactantToLogicGateMap;
    }

    public void addModifiers(ReactionWrapper reactionW, Process process) {
        // start with logic gates
        HashMap<ReactantWrapper, String> reactantToLogicGateMap = this.addLogicGates(reactionW, process);
        HashSet<ReactionNodeModel> logicGatesToBeRemoved = new HashSet<>();
        HashSet<LinkModel> logicLinksToBeRemoved = new HashSet<>();

        for(ReactantWrapper reactantW: reactionW.getModifiers()) {
            // simple case, no logic gate
            logger.trace("modifier: "+reactantW.getAliasW().getId());

            ReactantModel modifModel = new ReactantModel(reactantW);

            //Reaction reaction = reactionW.getReaction();
            int modifIndex = reactantW.getPositionIndex();
            List<Point2D.Float> editPoints = reactantW.getLineWrapper().getEditPoints();

            // treat modifier as linked to a reactionNodeModel, either a process or a logic gate
            ReactionNodeModel genericNode = null;
            LinkModel logicLink = null; // in case reactant is linked to logic gate
            Point2D.Float genericNodeAnchorPoint;
            String linkType;
            if(reactantW.getLogicGate() != null) { // linked to logic gate
                String logicId = reactantToLogicGateMap.get(reactantW);
                for(ReactionNodeModel nodeModel: this.getReactionNodeModels()) {
                    if(nodeModel.getId().equals(logicId)) {
                        genericNode = nodeModel;
                    }
                }

                /*
                    Case of UNKNOWN logic gates, needs to be removed
                 */
                LogicGateWrapper logicGate = reactantW.getLogicGate();
                if(logicGate.getType() == LogicGateType.UNKNOWN) {
                    logger.error("For reaction: "+this.getId()+" a glyph with ID: "+reactantW.getAliasW().getId()+
                            " and glyph name: "+reactantW.getAliasW().getSpeciesW().getName()+
                            " is linked to an UNKNOWN logic gate which cannot be translated. " +
                            "The logic gate will be removed and the modifier will point directly to the process glyph.");

                    // find the link of this logic gate
                    for(LinkModel linkModel: this.getLinkModels()) {
                        if(linkModel.getStart().getId().equals(logicId)) {
                            logicLink = linkModel;
                        }
                    }

                    // schedule for removal. Don't remove now, because other modifiers might be
                    // linked to this logic gate
                    logicLinksToBeRemoved.add(logicLink);
                    logicGatesToBeRemoved.add(genericNode);

                    // set destination of reactantWrapper link to be the process instead of the logic gate
                    genericNode = process;
                    genericNodeAnchorPoint = process.getAbsoluteAnchorCoords(logicGate.getProcessAnchorIndex());
                    linkType = logicLink.getSbgnClass();

                }
                // normal logic gate case
                else {
                    genericNodeAnchorPoint = genericNode.getPortIn();
                    linkType = "logic arc";
                }


            }
            else { // modifier is linked to process
                genericNode = process;
                genericNodeAnchorPoint = process.getAbsoluteAnchorCoords(reactantW.getProcessAnchorIndex());
                linkType = LinkModel.getSbgnClass(reactantW.getModificationLinkType().toString());

            }


            logger.trace("edit points: "+editPoints);

            AffineTransform transform =
                    GeometryUtils.getTransformsToGlobalCoords(
                            modifModel.getAbsoluteAnchorCoordinate(
                                    reactantW.getAnchorPoint()),
                            genericNodeAnchorPoint);
            List<Point2D.Float> absoluteEditPoints = new ArrayList<>();
            absoluteEditPoints.add(modifModel.getAbsoluteAnchorCoordinate(reactantW.getAnchorPoint()));
            absoluteEditPoints.addAll(GeometryUtils.convertPoints(editPoints, transform));
            absoluteEditPoints.add(genericNodeAnchorPoint);

            absoluteEditPoints = GeometryUtils.getNormalizedEndPoints(absoluteEditPoints,
                    modifModel.getGlyph(),
                    genericNode.getGlyph(),
                    modifModel.getAnchorPoint(),
                    AnchorPoint.E);

            String modifId = "modif_" + UUID.randomUUID();
            LinkModel modifLink = new LinkModel(modifModel, genericNode, new Link(absoluteEditPoints),
                    modifId,
                    linkType,
                    new StyleInfo(reactantW.getLineWrapper().getLineWidth(),
                            reactantW.getLineWrapper().getLineColor(), modifId));

            // add everything to the reaction lists
            this.getReactantModels().add(modifModel);
            this.getLinkModels().add(modifLink);
        }

        // remove unwanted logic gates and their links
        if(logicGatesToBeRemoved.size() > 0) {
            logger.error(logicGatesToBeRemoved.size()+" logic gates were removed for reaction "+this.getId());
        }
        for(ReactionNodeModel r: logicGatesToBeRemoved){
            this.getReactionNodeModels().remove(r);
        }
        for(LinkModel l: logicLinksToBeRemoved) {
            this.getLinkModels().remove(l);
        }
    }

    public void addAdditionalReactants(ReactionWrapper reactionW, Process process) {
        for(ReactantWrapper reactantW: reactionW.getAdditionalReactants()) {
            ReactantModel reactantModel = new ReactantModel(reactantW);

            AffineTransform transform =
                    GeometryUtils.getTransformsToGlobalCoords(
                            reactantModel.getAbsoluteAnchorCoordinate(
                                    reactantW.getAnchorPoint()),
                            process.getAbsoluteAnchorCoords(0));

            int positionIndex = reactantW.getPositionIndex();
            logger.trace("POSITION INDEX "+positionIndex);
            //Reaction reaction = reactionW.getReaction();
            List<Point2D.Float> editPoints = reactantW.getLineWrapper().getEditPoints();
            logger.trace("ADDITIONAL REACT EDIT POINTS "+editPoints);

            List<Point2D.Float> absoluteEditPoints = new ArrayList<>();
            absoluteEditPoints.add(reactantModel.getAbsoluteAnchorCoordinate(reactantW.getAnchorPoint()));
            absoluteEditPoints.addAll(GeometryUtils.convertPoints(editPoints, transform));
            absoluteEditPoints.add(process.getAbsoluteAnchorCoords(0));
            logger.trace("ABSOLUTE POINTS: "+absoluteEditPoints);

            Point2D.Float normalizedStart = GeometryUtils.normalizePoint(absoluteEditPoints.get(0),
                    absoluteEditPoints.get(1),
                    reactantModel.getGlyph(),
                    reactantModel.getAnchorPoint());

            List<Point2D.Float> normalizedEditPoints = new ArrayList<>();
            normalizedEditPoints.add(normalizedStart);
            normalizedEditPoints.addAll(GeometryUtils.convertPoints(editPoints, transform));
            normalizedEditPoints.add(process.getPortIn());

            String reactLinkId = "addreact_"+ UUID.randomUUID();
            LinkModel reactLink = new LinkModel(reactantModel, process, new Link(normalizedEditPoints),
                    reactLinkId,
                    "consumption",
                    new StyleInfo(reactantW.getLineWrapper().getLineWidth(),
                            reactantW.getLineWrapper().getLineColor(), reactLinkId));

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
            ReactantModel reactantModel = new ReactantModel(reactantW);

            AffineTransform transform =
                    GeometryUtils.getTransformsToGlobalCoords(
                            process.getAbsoluteAnchorCoords(1),
                            reactantModel.getAbsoluteAnchorCoordinate(
                                    reactantW.getAnchorPoint()));

            int positionIndex = reactantW.getPositionIndex();
            logger.trace("POSITION INDEX "+positionIndex);
            //Reaction reaction = reactionW.getReaction();
            List<Point2D.Float> editPoints = reactantW.getLineWrapper().getEditPoints();
            logger.trace("ADDITIONAL REACT EDIT POINTS "+editPoints);

            List<Point2D.Float> absoluteEditPoints = new ArrayList<>();
            absoluteEditPoints.add(process.getAbsoluteAnchorCoords(1));
            absoluteEditPoints.addAll(GeometryUtils.convertPoints(editPoints, transform));
            absoluteEditPoints.add(reactantModel.getAbsoluteAnchorCoordinate(reactantW.getAnchorPoint()));
            logger.trace("ABSOLUTE POINTS: "+absoluteEditPoints);

            Point2D.Float normalizedEnd = GeometryUtils.normalizePoint(absoluteEditPoints.get(absoluteEditPoints.size() - 1),
                    absoluteEditPoints.get(absoluteEditPoints.size() - 2),
                    reactantModel.getGlyph(),
                    reactantModel.getAnchorPoint());

            List<Point2D.Float> normalizedEditPoints = new ArrayList<>();
            normalizedEditPoints.add(process.getPortOut());
            normalizedEditPoints.addAll(GeometryUtils.convertPoints(editPoints, transform));
            normalizedEditPoints.add(normalizedEnd);

            String reactLinkId = "addprod_" + UUID.randomUUID();
            LinkModel reactLink = new LinkModel(process, reactantModel, new Link(normalizedEditPoints),
                    reactLinkId, "production",
                    new StyleInfo(reactantW.getLineWrapper().getLineWidth(),
                            reactantW.getLineWrapper().getLineColor(), reactLinkId));

            // add everything to the reaction lists
            this.getReactantModels().add(reactantModel);
            this.getLinkModels().add(reactLink);
        }
    }

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
        AffineTransform t = GeometryUtils.getTransformsToGlobalCoords(origin, pX, pY);
        t.transform(absolutePoint, absolutePoint);

        /*
            In ACSN we can produce NaN here
         */
        if(Double.isNaN(absolutePoint.getX()) || Double.isNaN(absolutePoint.getY())) {
            logger.error("NaN was produced as global coords for association glyph with coords: "+editPoint
                    +" Local coordinates will be used instead, but they may be incorrect.");
            absolutePoint = editPoint;
        }

        return absolutePoint;
    }


    /**
     * already comprises start and end
     */
    public static List<Point2D.Float> getBranchPoints(ReactionWrapper reactionW, Point2D.Float origin, Point2D.Float pX, int branch) {

        List<Point2D.Float> absoluteEditPoints = new ArrayList<>();
        absoluteEditPoints.add(origin);
        logger.trace("local system: "+origin+" "+pX);
        logger.trace("points for BRANCH "+branch+" "+ reactionW.getEditPointsForBranch(branch));

        for (Point2D editP : reactionW.getEditPointsForBranch(branch)) {
            Point2D p = new Point2D.Double(editP.getX(), editP.getY());

            AffineTransform t = GeometryUtils.getTransformsToGlobalCoords(origin, pX);
            t.transform(p, p);

            logger.trace("BRANCH "+branch+" result: " + editP + " -> " + p.toString());
            absoluteEditPoints.add(new Point2D.Float((float) p.getX(), (float) p.getY()));

        }
        absoluteEditPoints.add(pX);
        logger.trace("BRANCH "+branch+" stack: "+absoluteEditPoints);

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

    public ReactionType getCdReactionType() {
        return cdReactionType;
    }

    public boolean hasProcess() {
        return hasProcess;
    }

    public String getId() {
        return id;
    }
}
