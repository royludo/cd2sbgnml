package fr.curie.cd2sbgnml.model;

import fr.curie.cd2sbgnml.graphics.AnchorPoint;
import fr.curie.cd2sbgnml.graphics.GeometryUtils;
import fr.curie.cd2sbgnml.graphics.Link;
import fr.curie.cd2sbgnml.xmlcdwrappers.LineWrapper;
import fr.curie.cd2sbgnml.xmlcdwrappers.ReactantWrapper;
import fr.curie.cd2sbgnml.xmlcdwrappers.ReactionWrapper;
import fr.curie.cd2sbgnml.xmlcdwrappers.StyleInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;

import java.util.*;

/**
 * Used for the construction of an association reaction. It is a branch type reaction in CellDesigner, which is
 * fundamentally different from a normal reaction.
 */
public class AssociationReactionModel extends GenericReactionModel {

    private final Logger logger = LoggerFactory.getLogger(AssociationReactionModel.class);

    public AssociationReactionModel(ReactionWrapper reactionW) {
        super(reactionW);

        ReactantWrapper startR1 = reactionW.getBaseReactants().get(0);
        ReactantWrapper startR2 = reactionW.getBaseReactants().get(1);
        ReactantWrapper endR = reactionW.getBaseProducts().get(0);

        ReactantModel startModel0 = new ReactantModel(startR1);
        ReactantModel startModel1 = new ReactantModel(startR2);
        ReactantModel endModel = new ReactantModel(endR);

        LineWrapper lineW = reactionW.getLineWrapper();

        // list edit points
        List<Point2D.Float> editPoints = reactionW.getLineWrapper().getEditPoints();

        // process association point
        Point2D.Float assocGlyphLocalCoords = editPoints.get(editPoints.size() - 1); // last point listed in xml
        Point2D.Float assocGlyphGlobalCoords = getAbsolutePoint(
                startR1.getCenterPoint(),
                startR2.getCenterPoint(),
                endR.getCenterPoint(), assocGlyphLocalCoords);
        logger.trace("result: " + assocGlyphLocalCoords + " -> " + assocGlyphGlobalCoords);

        String assocId = "assoc_" + UUID.randomUUID();
        AssocDissoc association = new AssocDissoc(assocGlyphGlobalCoords, assocId, new StyleInfo(assocId));

        // get the relevant points
        Point2D.Float startR1coordPoint = startModel0.getAbsoluteAnchorCoordinate(startR1.getAnchorPoint());
        Point2D.Float startR2coordPoint = startModel1.getAbsoluteAnchorCoordinate(startR2.getAnchorPoint());
        Point2D.Float endRcoordPoint = endModel.getAbsoluteAnchorCoordinate(endR.getAnchorPoint());

        // careful here, edit points goes from origin (process glyph) to reactant
        // but we want the opposite, as a production arc it goes from reactant to process

        // branch 0
        List<Point2D.Float> absoluteEditPoints0 = getBranchPoints(reactionW, association.getGlyph().getCenter(), startR1coordPoint, 0);
        Collections.reverse(absoluteEditPoints0);
        String link0Id = "cons_" + UUID.randomUUID();
        LinkModel link0 = new LinkModel(startModel0, association, new Link(absoluteEditPoints0),
                link0Id, "consumption", new StyleInfo(lineW.getLineWidth(), lineW.getLineColor(), link0Id));

        List<Point2D.Float> absoluteEditPoints1 = getBranchPoints(reactionW, association.getGlyph().getCenter(), startR2coordPoint, 1);
        Collections.reverse(absoluteEditPoints1);
        String link1Id = "cons_" + UUID.randomUUID();
        LinkModel link1 = new LinkModel(startModel1, association, new Link(absoluteEditPoints1),
                link1Id, "consumption", new StyleInfo(lineW.getLineWidth(), lineW.getLineColor(), link1Id));

        List<Point2D.Float> absoluteEditPoints2 = getBranchPoints(reactionW, association.getGlyph().getCenter(), endRcoordPoint, 2);
        absoluteEditPoints2 = GeometryUtils.getNormalizedEndPoints(absoluteEditPoints2,
                association.getGlyph(),
                endModel.getGlyph(),
                AnchorPoint.CENTER,
                endModel.getAnchorPoint());

        if(this.hasProcess()) {

            Line2D.Float processAxis = new Line2D.Float(absoluteEditPoints2.get(reactionW.getProcessSegmentIndex()),
                    absoluteEditPoints2.get(reactionW.getProcessSegmentIndex() + 1));

            /*
                !!!!!! process coords must be computed AFTER normalization of arrows !!!!!
                else, if the link is pointing to the center and not the border of the glyph, process will get shifted
                as the link is longer than what it appears.
             */
            String prId = "pr_"+UUID.randomUUID();
            Process process = new Process(
                    GeometryUtils.getMiddleOfPolylineSegment(absoluteEditPoints2, reactionW.getProcessSegmentIndex()),
                    prId,
                    processAxis,
                    new StyleInfo(lineW.getLineWidth(),
                            lineW.getLineColor(), prId));

            AbstractMap.SimpleEntry<List<Point2D.Float>, List<Point2D.Float>> subLinesTuple =
                    GeometryUtils.splitPolylineAtSegment(absoluteEditPoints2, reactionW.getProcessSegmentIndex());

            /*
                Here we need to normalize also the link between association and process glyph.
                As it is not a valid SBGN thing, it is weirdly drawn by visualization tool.
                Better if the link does not overlap the process here.
             */
            List<Point2D.Float> normalizedSubLinesTuple1 = GeometryUtils.getNormalizedEndPoints(subLinesTuple.getKey(),
                    association.getGlyph(),
                    process.getGlyph(),
                    AnchorPoint.CENTER,
                    AnchorPoint.CENTER);

            List<Point2D.Float> normalizedSubLinesTuple2 = GeometryUtils.getNormalizedEndPoints(subLinesTuple.getValue(),
                    process.getGlyph(),
                    endModel.getGlyph(),
                    AnchorPoint.CENTER,
                    AnchorPoint.CENTER);

            // PROCESS port management
            Point2D.Float pIn = normalizedSubLinesTuple1.get(normalizedSubLinesTuple1.size() - 1);
            Point2D.Float pOut = normalizedSubLinesTuple2.get(0);
            process.setPorts(pIn, pOut);

            // replace the end and start points of the sublines by corresponding ports
            normalizedSubLinesTuple1.set(normalizedSubLinesTuple1.size() - 1, process.getPortIn());
            normalizedSubLinesTuple2.set(0, process.getPortOut());

            String l21Id = "cons_" + UUID.randomUUID();
            LinkModel l21 = new LinkModel(association, process, new Link(normalizedSubLinesTuple1),
                    l21Id, "consumption", new StyleInfo(lineW.getLineWidth(),
                    lineW.getLineColor(), l21Id));

            String l22Id = "prod_" + UUID.randomUUID();
            LinkModel l22 = new LinkModel(process, endModel, new Link(normalizedSubLinesTuple2),
                    l22Id, "production", new StyleInfo(lineW.getLineWidth(),
                    lineW.getLineColor(), l22Id));

            // merge links to get rid of association glyph
            LinkModel mergedLink0 = link0.mergeWith(l21, "consumption", link0.getId());
            LinkModel mergedLink1 = link1.mergeWith(l21, "consumption", link1.getId());


            if(reactionW.isReversible()) {
                mergedLink0.reverse();
                mergedLink1.reverse();
            }

            // add everything to the reaction lists
            this.getReactantModels().add(startModel0);
            this.getReactantModels().add(startModel1);
            this.getReactantModels().add(endModel);
            this.getReactionNodeModels().add(process);
            //this.getReactionNodeModels().add(association);
            this.getLinkModels().add(mergedLink0);
            this.getLinkModels().add(mergedLink1);
            //this.getLinkModels().add(l21);
            this.getLinkModels().add(l22);

            this.addModifiers(reactionW, process);
            this.addAdditionalReactants(reactionW, process);
            this.addAdditionalProducts(reactionW, process);
        }
        else {
            throw new RuntimeException("Association has no process ! How is it even possible. Reaction id: "+reactionW.getId());
        }
    }
}
