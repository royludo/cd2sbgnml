package fr.curie.cd2sbgnml.model;

import fr.curie.cd2sbgnml.graphics.*;
import fr.curie.cd2sbgnml.LinkWrapper;
import fr.curie.cd2sbgnml.xmlcdwrappers.ReactantWrapper;
import fr.curie.cd2sbgnml.xmlcdwrappers.ReactionWrapper;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;


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

        absoluteEditPoints = LinkModel.getNormalizedEndPoints(absoluteEditPoints,
                startModel,
                endModel,
                startModel.getReactantW().getAnchorPoint(),
                endModel.getReactantW().getAnchorPoint());

        if(this.hasProcess()) {

            Process process = new Process(
                    this,
                    GeometryUtils.getMiddleOfPolylineSegment(absoluteEditPoints, reactionW.getProcessSegmentIndex()));

            AbstractMap.SimpleEntry<List<Point2D.Float>, List<Point2D.Float>> subLinesTuple =
                    GeometryUtils.splitPolylineAtSegment(absoluteEditPoints, reactionW.getProcessSegmentIndex());
            LinkModel l1 = new LinkModel(startModel, process, new Link(subLinesTuple.getKey()));
            LinkModel l2 = new LinkModel(process, endModel, new Link(subLinesTuple.getValue()));
            /*System.out.println("process coords "+process.getGlyph().getCenter());
            System.out.println("original edit points "+absoluteEditPoints);
            System.out.println("subline1 "+subLinesTuple.getKey());
            System.out.println("subline2 "+subLinesTuple.getValue());
            System.out.println("in simpel reaction "+l1.getLink().getStart()+" "+l1.getLink().getEditPoints()+" "+l1.getLink().getEnd());*/

            // add everything to the reaction lists
            this.getReactantModels().add(startModel);
            this.getReactantModels().add(endModel);
            this.getReactionNodeModels().add(process);
            this.getLinkModels().add(l1);
            this.getLinkModels().add(l2);
        }
        else {
            // TODO do other cases
        }

        /*this.baseLinks.get(0).setSbgnSpacePointList(
                this.baseLinks.get(0).getNormalizedEndPoints(
                        startR.getAnchorPoint(), GeometryUtils.AnchorPoint.CENTER
                ));
        this.baseLinks.get(1).setSbgnSpacePointList(
                this.baseLinks.get(1).getNormalizedEndPoints(
                        GeometryUtils.AnchorPoint.CENTER, endR.getAnchorPoint()
                ));*/
    }

}
