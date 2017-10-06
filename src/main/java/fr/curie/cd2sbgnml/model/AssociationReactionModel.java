package fr.curie.cd2sbgnml.model;

import fr.curie.cd2sbgnml.graphics.AnchorPoint;
import fr.curie.cd2sbgnml.graphics.GeometryUtils;
import fr.curie.cd2sbgnml.LinkWrapper;
import fr.curie.cd2sbgnml.graphics.Link;
import fr.curie.cd2sbgnml.xmlcdwrappers.ReactantWrapper;
import fr.curie.cd2sbgnml.xmlcdwrappers.ReactionWrapper;
import org.sbml.x2001.ns.celldesigner.CelldesignerLineDirectionDocument;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.*;

public class AssociationReactionModel extends GenericReactionModel {

    public AssociationReactionModel(ReactionWrapper reactionW) {
        super(reactionW);

        ReactantWrapper startR1 = reactionW.getBaseReactants().get(0);
        ReactantWrapper startR2 = reactionW.getBaseReactants().get(1);
        ReactantWrapper endR = reactionW.getBaseProducts().get(0);

        ReactantModel startModel0 = new ReactantModel(this, startR1);
        ReactantModel startModel1 = new ReactantModel(this, startR2);
        ReactantModel endModel = new ReactantModel(this, endR);

        // list edit points
        List<Point2D.Float> editPoints = ReactionWrapper.getBaseEditPoints(reactionW.getReaction());

        // process association point
        Point2D.Float assocGlyphLocalCoords = editPoints.get(editPoints.size() - 1); // last point listed in xml
        Point2D.Float assocGlyphGlobalCoords = getAbsolutePoint(
                startR1.getCenterPoint(),
                startR2.getCenterPoint(),
                endR.getCenterPoint(), assocGlyphLocalCoords);
        System.out.println("result: " + assocGlyphLocalCoords + " -> " + assocGlyphGlobalCoords);

        AssocDissoc association = new AssocDissoc(this, assocGlyphGlobalCoords);

        // get the relevant points
        Point2D.Float startR1coordPoint = startModel0.getAbsoluteAnchorCoordinate(startR1.getAnchorPoint());
        Point2D.Float startR2coordPoint = startModel1.getAbsoluteAnchorCoordinate(startR2.getAnchorPoint());
        Point2D.Float endRcoordPoint = endModel.getAbsoluteAnchorCoordinate(endR.getAnchorPoint());

        // careful here, edit points goes from origin (process glyph) to reactant
        // but we want the opposite, as a production arc it goes from reactant to process

        // branch 0
        List<Point2D.Float> absoluteEditPoints0 = this.getBranchPoints(association.getGlyph().getCenter(), startR1coordPoint, 0);
        Collections.reverse(absoluteEditPoints0);
        LinkModel link0 = new LinkModel(startModel0, association, new Link(absoluteEditPoints0));
        /*link0.setSbgnSpacePointList(
                link0.getNormalizedEndPoints(
                        startR1.getAnchorPoint(), GeometryUtils.AnchorPoint.CENTER
                ));*/

        List<Point2D.Float> absoluteEditPoints1 = this.getBranchPoints(association.getGlyph().getCenter(), startR2coordPoint, 1);
        Collections.reverse(absoluteEditPoints1);
        LinkModel link1 = new LinkModel(startModel1, association, new Link(absoluteEditPoints1));
        /*link1.setSbgnSpacePointList(
                link1.getNormalizedEndPoints(
                        startR2.getAnchorPoint(), GeometryUtils.AnchorPoint.CENTER
                ));*/

        List<Point2D.Float> absoluteEditPoints2 = this.getBranchPoints(association.getGlyph().getCenter(), endRcoordPoint, 2);
        absoluteEditPoints2 = LinkModel.getNormalizedEndPoints(absoluteEditPoints2,
                association,
                endModel,
                AnchorPoint.CENTER,
                endModel.getReactantW().getAnchorPoint());
        //LinkModel link2 = new LinkModel(association, endModel, new Link(absolutePoints2));
        /*link2.setSbgnSpacePointList(
                link2.getNormalizedEndPoints(
                        GeometryUtils.AnchorPoint.CENTER, endR.getAnchorPoint()
                ));*/

        if(this.hasProcess()) {

            System.out.println("association process segment "+reactionW.getProcessSegmentIndex());
            System.out.println("absolutepoints2 "+absoluteEditPoints2);
            /*
            !!!!!! process coords must be computed AFTER normalization of arrows !!!!!
            else, if the link is pointing to the center and not the border of the glyph, process will get shifted
            as the link is longer than what it appears.
             */
            Process process = new Process(
                    this,
                    GeometryUtils.getMiddleOfPolylineSegment(absoluteEditPoints2, reactionW.getProcessSegmentIndex()));

            AbstractMap.SimpleEntry<List<Point2D.Float>, List<Point2D.Float>> subLinesTuple =
                    GeometryUtils.splitPolylineAtSegment(absoluteEditPoints2, reactionW.getProcessSegmentIndex());

            /*
            Here we need to normalize also the link between association and process glyph.
            As it is not a valid SBGN thing, it is weirdly drawn by visualization tool.
            Better if the link does not overlap the process here.
             */
            List<Point2D.Float> normalizedSubLinesTuple1 = LinkModel.getNormalizedEndPoints(subLinesTuple.getKey(),
                    association,
                    process,
                    AnchorPoint.CENTER,
                    AnchorPoint.CENTER);

            LinkModel l21 = new LinkModel(association, process, new Link(normalizedSubLinesTuple1));
            LinkModel l22 = new LinkModel(process, endModel, new Link(subLinesTuple.getValue()));
            System.out.println("link edit points: "+link0.getLink().getEditPoints()+" "+l21.getLink().getStart()+" "+l21.getLink().getEditPoints());

            // add everything to the reaction lists
            this.getReactantModels().add(startModel0);
            this.getReactantModels().add(startModel1);
            this.getReactantModels().add(endModel);
            this.getReactionNodeModels().add(process);
            this.getReactionNodeModels().add(association);
            this.getLinkModels().add(link0);
            this.getLinkModels().add(link1);
            this.getLinkModels().add(l21);
            this.getLinkModels().add(l22);
        }
        else {
            // TODO do other cases
            throw new RuntimeException("Association has no process ! How is it even possible. Reaction id: "+reactionW.getId());
        }
    }
}
