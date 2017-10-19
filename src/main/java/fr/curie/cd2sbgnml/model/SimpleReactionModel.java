package fr.curie.cd2sbgnml.model;

import fr.curie.cd2sbgnml.graphics.*;
import fr.curie.cd2sbgnml.xmlcdwrappers.ReactantWrapper;
import fr.curie.cd2sbgnml.xmlcdwrappers.ReactionWrapper;

import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


public class SimpleReactionModel extends GenericReactionModel{

    public SimpleReactionModel(ReactionWrapper reactionW) {
        super(reactionW);

        ReactantWrapper startR = reactionW.getBaseReactants().get(0);
        ReactantWrapper endR = reactionW.getBaseProducts().get(0);

        ReactantModel startModel = new ReactantModel(this, startR);
        ReactantModel endModel = new ReactantModel(this, endR);

        Point2D.Float baseLinkStartPoint = startModel.getAbsoluteAnchorCoordinate(startR.getAnchorPoint());
        Point2D.Float baseLinkEndPoint = endModel.getAbsoluteAnchorCoordinate(endR.getAnchorPoint());

        List<Point2D.Float> editPoints = ReactionWrapper.getBaseEditPoints(reactionW.getReaction());
        List<AffineTransform> transformList = GeometryUtils.getTransformsToGlobalCoords(baseLinkStartPoint, baseLinkEndPoint);

        List<Point2D.Float> absoluteEditPoints = new ArrayList<>();
        absoluteEditPoints.add(baseLinkStartPoint);
        absoluteEditPoints.addAll(GeometryUtils.convertPoints(editPoints, transformList));
        absoluteEditPoints.add(baseLinkEndPoint);

        System.out.println("BEFORE NOMRALIZE "+absoluteEditPoints);
        absoluteEditPoints = GeometryUtils.getNormalizedEndPoints(absoluteEditPoints,
                startModel.getGlyph(),
                endModel.getGlyph(),
                startModel.getAnchorPoint(),
                endModel.getAnchorPoint());
        System.out.println("AFTER NOMRALIZE "+absoluteEditPoints);

        if(this.hasProcess()) {

            boolean isPolyline = absoluteEditPoints.size() > 2;
            Line2D.Float processAxis = new Line2D.Float(absoluteEditPoints.get(reactionW.getProcessSegmentIndex()),
                    absoluteEditPoints.get(reactionW.getProcessSegmentIndex() + 1));

            Process process = new Process(
                    this,
                    GeometryUtils.getMiddleOfPolylineSegment(absoluteEditPoints, reactionW.getProcessSegmentIndex()),
                    "pr_"+UUID.randomUUID().toString(),
                    processAxis,
                    isPolyline);

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

            LinkModel l1 = new LinkModel(startModel, process, new Link(subLinesTuple1),
                    "cons_" + UUID.randomUUID(), "consumption");
            LinkModel l2 = new LinkModel(process, endModel, new Link(subLinesTuple2),
                    "prod_" + UUID.randomUUID(), LinkModel.getSbgnClass(reactionW.getReactionType()));
            /*System.out.println("process coords "+process.getGlyph().getCenter());
            System.out.println("original edit points "+absoluteEditPoints);
            System.out.println("subline1 "+subLinesTuple.getKey());
            System.out.println("subline2 "+subLinesTuple.getValue());
            System.out.println("in simpel reaction "+l1.getLink().getStart()+" "+l1.getLink().getEditPoints()+" "+l1.getLink().getEnd());*/

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
            LinkModel l1 = new LinkModel(startModel, endModel, new Link(absoluteEditPoints),
                    "prod_" + UUID.randomUUID(), LinkModel.getSbgnClass(reactionW.getReactionType()));

            // add everything to the reaction lists
            this.getReactantModels().add(startModel);
            this.getReactantModels().add(endModel);
            this.getLinkModels().add(l1);
        }

    }

}
