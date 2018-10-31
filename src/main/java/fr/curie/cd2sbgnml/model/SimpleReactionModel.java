package fr.curie.cd2sbgnml.model;

import fr.curie.cd2sbgnml.graphics.*;
import fr.curie.cd2sbgnml.xmlcdwrappers.ReactantWrapper;
import fr.curie.cd2sbgnml.xmlcdwrappers.ReactionWrapper;
import fr.curie.cd2sbgnml.xmlcdwrappers.StyleInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Used for construction of simple non-branching reactions.
 */
public class SimpleReactionModel extends GenericReactionModel{

    private static final Logger logger = LoggerFactory.getLogger(SimpleReactionModel.class);

    public SimpleReactionModel(ReactionWrapper reactionW) {
        super(reactionW);

        ReactantWrapper startR = reactionW.getBaseReactants().get(0);
        ReactantWrapper endR = reactionW.getBaseProducts().get(0);

        ReactantModel startModel = new ReactantModel(startR);
        ReactantModel endModel = new ReactantModel(endR);

        Point2D.Float baseLinkStartPoint = startModel.getAbsoluteAnchorCoordinate(startR.getAnchorPoint());
        Point2D.Float baseLinkEndPoint = endModel.getAbsoluteAnchorCoordinate(endR.getAnchorPoint());

        List<Point2D.Float> editPoints = reactionW.getLineWrapper().getEditPoints();
        AffineTransform transform = GeometryUtils.getTransformsToGlobalCoords(baseLinkStartPoint, baseLinkEndPoint);

        List<Point2D.Float> absoluteEditPoints = new ArrayList<>();
        absoluteEditPoints.add(baseLinkStartPoint);
        absoluteEditPoints.addAll(GeometryUtils.convertPoints(editPoints, transform));
        absoluteEditPoints.add(baseLinkEndPoint);

        absoluteEditPoints = GeometryUtils.getNormalizedEndPoints(absoluteEditPoints,
                startModel.getGlyph(),
                endModel.getGlyph(),
                startModel.getAnchorPoint(),
                endModel.getAnchorPoint());

        if(this.hasProcess()) {

            Line2D.Float processAxis = new Line2D.Float(absoluteEditPoints.get(reactionW.getProcessSegmentIndex()),
                    absoluteEditPoints.get(reactionW.getProcessSegmentIndex() + 1));
            // corner case when both points of axis are the same, center is the same

            String prId = "pr_"+UUID.randomUUID();
            Process process = new Process(
                    GeometryUtils.getMiddleOfPolylineSegment(absoluteEditPoints, reactionW.getProcessSegmentIndex()),
                    prId,
                    processAxis,
                    // style info of the process should inherit from style of the line it's on
                    new StyleInfo(reactionW.getLineWrapper().getLineWidth(),
                            reactionW.getLineWrapper().getLineColor(), prId));

            AbstractMap.SimpleEntry<List<Point2D.Float>, List<Point2D.Float>> subLinesTuple =
                    GeometryUtils.splitPolylineAtSegment(absoluteEditPoints, reactionW.getProcessSegmentIndex());

            List<Point2D.Float> subLinesTuple1 = GeometryUtils.getNormalizedEndPoints(subLinesTuple.getKey(),
                    startModel.getGlyph(),
                    process.getGlyph(),
                    startModel.getAnchorPoint(),
                    AnchorPoint.CENTER);

            List<Point2D.Float> subLinesTuple2 = GeometryUtils.getNormalizedEndPoints(subLinesTuple.getValue(),
                    process.getGlyph(),
                    endModel.getGlyph(),
                    AnchorPoint.CENTER,
                    endModel.getAnchorPoint());

            // port management
            Point2D.Float pIn = subLinesTuple1.get(subLinesTuple1.size() - 1);
            Point2D.Float pOut = subLinesTuple2.get(0);
            process.setPorts(pIn, pOut);

            // replace the end and start points of the sublines by corresponding ports
            subLinesTuple1.set(subLinesTuple1.size() - 1, process.getPortIn());
            subLinesTuple2.set(0, process.getPortOut());

            String l1Id = "cons_" + UUID.randomUUID();
            LinkModel l1 = new LinkModel(startModel, process, new Link(subLinesTuple1),
                    l1Id, "consumption",
                    new StyleInfo(reactionW.getLineWrapper().getLineWidth(),
                            reactionW.getLineWrapper().getLineColor(), l1Id));

            String l2Id = "prod_" + UUID.randomUUID();
            LinkModel l2 = new LinkModel(process, endModel, new Link(subLinesTuple2),
                    l2Id, LinkModel.getSbgnClass(reactionW.getReactionType().toString()),
                    new StyleInfo(reactionW.getLineWrapper().getLineWidth(),
                            reactionW.getLineWrapper().getLineColor(), l2Id));

            if(reactionW.isReversible()) {
                l1.reverse();
            }

            // add everything to the reaction lists
            this.getReactantModels().add(startModel);
            this.getReactantModels().add(endModel);
            this.getReactionNodeModels().add(process);
            this.getLinkModels().add(l1);
            this.getLinkModels().add(l2);

            this.addModifiers(reactionW, process);
            this.addAdditionalReactants(reactionW, process);
            this.addAdditionalProducts(reactionW, process);
        }
        else {
            String linkid = "direct_" + UUID.randomUUID();
            LinkModel l1 = new LinkModel(startModel, endModel, new Link(absoluteEditPoints),
                    linkid, LinkModel.getSbgnClass(reactionW.getReactionType().toString()),
                    new StyleInfo(reactionW.getLineWrapper().getLineWidth(),
                            reactionW.getLineWrapper().getLineColor(), linkid));

            // add everything to the reaction lists
            this.getReactantModels().add(startModel);
            this.getReactantModels().add(endModel);
            this.getLinkModels().add(l1);

            if(reactionW.getModifiers().size() > 0) {
                logger.error("Reaction "+reactionW.getId()+" with no process has "+reactionW.getModifiers().size()
                        +" modifier links that were removed.");
            }

        }

    }

}
