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
 * Used for the construction of an dissociation reaction. It is a branch type reaction in CellDesigner, which is
 * fundamentally different from a normal reaction.
 */
public class DissociationReactionModel extends GenericReactionModel{

    private final Logger logger = LoggerFactory.getLogger(DissociationReactionModel.class);

    public DissociationReactionModel(ReactionWrapper reactionW) {
        super(reactionW);

        ReactantWrapper startR = reactionW.getBaseReactants().get(0);
        ReactantWrapper endR1 = reactionW.getBaseProducts().get(0);
        ReactantWrapper endR2 = reactionW.getBaseProducts().get(1);

        ReactantModel startModel = new ReactantModel(startR);
        ReactantModel endModel1 = new ReactantModel(endR1);
        ReactantModel endModel2 = new ReactantModel(endR2);

        LineWrapper lineW = reactionW.getLineWrapper();

        // list edit points
        List<Point2D.Float> editPoints = reactionW.getLineWrapper().getEditPoints();

        // process association point
        Point2D.Float assocGlyphLocalCoords = editPoints.get(editPoints.size() - 1); // last point listed in xml
        Point2D.Float assocGlyphGlobalCoords = getAbsolutePoint(
                startR.getCenterPoint(),
                endR1.getCenterPoint(),
                endR2.getCenterPoint(), assocGlyphLocalCoords);
        logger.trace("result: " + assocGlyphLocalCoords + " -> " + assocGlyphGlobalCoords);

        String dissocId = "dissoc_" + UUID.randomUUID();
        AssocDissoc dissociation = new AssocDissoc(assocGlyphGlobalCoords, dissocId, new StyleInfo(dissocId));

        // get the relevant points
        Point2D.Float startRcoordPoint = startModel.getAbsoluteAnchorCoordinate(startR.getAnchorPoint());
        Point2D.Float endR1coordPoint = endModel1.getAbsoluteAnchorCoordinate(endR1.getAnchorPoint());
        Point2D.Float endR2coordPoint = endModel2.getAbsoluteAnchorCoordinate(endR2.getAnchorPoint());

        // branch 0
        List<Point2D.Float> absoluteEditPoints0 = getBranchPoints(reactionW, dissociation.getGlyph().getCenter(), startRcoordPoint, 0);
        Collections.reverse(absoluteEditPoints0);
        absoluteEditPoints0 = GeometryUtils.getNormalizedEndPoints(absoluteEditPoints0,
                startModel.getGlyph(),
                dissociation.getGlyph(),
                startModel.getAnchorPoint(),
                AnchorPoint.CENTER);

        List<Point2D.Float> absoluteEditPoints1 = getBranchPoints(reactionW, dissociation.getGlyph().getCenter(), endR1coordPoint, 1);
        absoluteEditPoints1 = GeometryUtils.getNormalizedEndPoints(absoluteEditPoints1,
                dissociation.getGlyph(),
                endModel1.getGlyph(),
                AnchorPoint.CENTER,
                endModel1.getAnchorPoint());

        String link1Id = "prod_" + UUID.randomUUID();
        LinkModel link1 = new LinkModel(dissociation, endModel1, new Link(absoluteEditPoints1),
                link1Id, "production", new StyleInfo(lineW.getLineWidth(), lineW.getLineColor(), link1Id));

        List<Point2D.Float> absoluteEditPoints2 = getBranchPoints(reactionW, dissociation.getGlyph().getCenter(), endR2coordPoint, 2);
        absoluteEditPoints2 = GeometryUtils.getNormalizedEndPoints(absoluteEditPoints2,
                dissociation.getGlyph(),
                endModel2.getGlyph(),
                AnchorPoint.CENTER,
                endModel2.getAnchorPoint());

        String link2Id = "prod_" + UUID.randomUUID();
        LinkModel link2 = new LinkModel(dissociation, endModel2, new Link(absoluteEditPoints2),
                link2Id, "production", new StyleInfo(lineW.getLineWidth(), lineW.getLineColor(), link2Id));

        if(this.hasProcess()) {

            Line2D.Float processAxis = new Line2D.Float(absoluteEditPoints0.get(absoluteEditPoints0.size() - 2 - reactionW.getProcessSegmentIndex()),
                    absoluteEditPoints0.get(absoluteEditPoints0.size() - 1 - reactionW.getProcessSegmentIndex()));
            /*
                !!!!!! process coords must be computed AFTER normalization of arrows !!!!!
                else, if the link is pointing to the center and not the border of the glyph, process will get shifted
                as the link is longer than what it appears.

                also here the segment indexes are reversed, as the number starts from dissociation glyph
             */
            String prId = "pr_"+UUID.randomUUID();
            Process process = new Process(
                    GeometryUtils.getMiddleOfPolylineSegment(absoluteEditPoints0,
                            absoluteEditPoints0.size() - 2 - reactionW.getProcessSegmentIndex()),
                    prId,
                    processAxis,
                    new StyleInfo(lineW.getLineWidth(), lineW.getLineColor(), prId));

            AbstractMap.SimpleEntry<List<Point2D.Float>, List<Point2D.Float>> subLinesTuple =
                    GeometryUtils.splitPolylineAtSegment(absoluteEditPoints0,
                            absoluteEditPoints0.size() - 2 - reactionW.getProcessSegmentIndex());

            /*
                Here we need to normalize also the link between association and process glyph.
                As it is not a valid SBGN thing, it is weirdly drawn by visualization tool.
                Better if the link does not overlap the process here.
             */
            List<Point2D.Float> normalizedSubLinesTuple1 = GeometryUtils.getNormalizedEndPoints(subLinesTuple.getKey(),
                    startModel.getGlyph(),
                    process.getGlyph(),
                    startModel.getAnchorPoint(),
                    AnchorPoint.CENTER);
            List<Point2D.Float> normalizedSubLinesTuple2 = GeometryUtils.getNormalizedEndPoints(subLinesTuple.getValue(),
                    process.getGlyph(),
                    dissociation.getGlyph(),
                    AnchorPoint.CENTER,
                    AnchorPoint.CENTER);

            // port management
            Point2D.Float pIn = normalizedSubLinesTuple1.get(normalizedSubLinesTuple1.size() - 1);
            Point2D.Float pOut = normalizedSubLinesTuple2.get(0);
            process.setPorts(pIn, pOut);

            // replace the end and start points of the sublines by corresponding ports
            normalizedSubLinesTuple1.set(normalizedSubLinesTuple1.size() - 1, process.getPortIn());
            normalizedSubLinesTuple2.set(0, process.getPortOut());

            String l21Id = "cons_" + UUID.randomUUID();
            LinkModel l21 = new LinkModel(startModel, process, new Link(normalizedSubLinesTuple1),
                    l21Id, "consumption", new StyleInfo(lineW.getLineWidth(), lineW.getLineColor(), l21Id));

            String l22Id = "cons_" + UUID.randomUUID();
            LinkModel l22 = new LinkModel(process, dissociation, new Link(normalizedSubLinesTuple2),
                    l22Id, "consumption", new StyleInfo(lineW.getLineWidth(), lineW.getLineColor(), l22Id));
            logger.trace("link edit points: "+l21.getLink().getStart()+" "+l21.getLink().getEditPoints());

            // merge links to get rid of association glyph
            LinkModel mergedLink1 = l22.mergeWith(link1, "production", link1.getId());
            LinkModel mergedLink2 = l22.mergeWith(link2, "production", link2.getId());


            if(reactionW.isReversible()) {
                l21.reverse();
            }

            // add everything to the reaction lists
            this.getReactantModels().add(startModel);
            this.getReactantModels().add(endModel1);
            this.getReactantModels().add(endModel2);
            this.getReactionNodeModels().add(process);
            //this.getReactionNodeModels().add(dissociation);
            this.getLinkModels().add(l21);
            //this.getLinkModels().add(l22);
            this.getLinkModels().add(mergedLink1);
            this.getLinkModels().add(mergedLink2);

            this.addModifiers(reactionW, process);
            this.addAdditionalReactants(reactionW, process);
            this.addAdditionalProducts(reactionW, process);

        }
        else {
            throw new RuntimeException("Association has no process ! How is it even possible. Reaction id: "+reactionW.getId());
        }

    }
}
