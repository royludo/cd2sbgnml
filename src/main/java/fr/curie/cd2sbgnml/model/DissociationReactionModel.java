package fr.curie.cd2sbgnml.model;

import fr.curie.cd2sbgnml.graphics.AnchorPoint;
import fr.curie.cd2sbgnml.graphics.GeometryUtils;
import fr.curie.cd2sbgnml.LinkWrapper;
import fr.curie.cd2sbgnml.graphics.Link;
import fr.curie.cd2sbgnml.xmlcdwrappers.ReactantWrapper;
import fr.curie.cd2sbgnml.xmlcdwrappers.ReactionWrapper;
import org.sbml.x2001.ns.celldesigner.CelldesignerLineDirectionDocument;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.*;

public class DissociationReactionModel extends GenericReactionModel{

    public DissociationReactionModel(ReactionWrapper reactionW) {
        super(reactionW);

        ReactantWrapper startR = reactionW.getBaseReactants().get(0);
        ReactantWrapper endR1 = reactionW.getBaseProducts().get(0);
        ReactantWrapper endR2 = reactionW.getBaseProducts().get(1);

        ReactantModel startModel = new ReactantModel(this, startR);
        ReactantModel endModel1 = new ReactantModel(this, endR1);
        ReactantModel endModel2 = new ReactantModel(this, endR2);

        // list edit points
        List<Point2D.Float> editPoints = ReactionWrapper.getBaseEditPoints(reactionW.getReaction());

        // process association point
        Point2D.Float assocGlyphLocalCoords = editPoints.get(editPoints.size() - 1); // last point listed in xml
        Point2D.Float assocGlyphGlobalCoords = getAbsolutePoint(
                startR.getCenterPoint(),
                endR1.getCenterPoint(),
                endR2.getCenterPoint(), assocGlyphLocalCoords);
        System.out.println("result: " + assocGlyphLocalCoords + " -> " + assocGlyphGlobalCoords);

        String dissocId = "dissoc_"+startModel.getId()+"_"+endModel1.getId()+"_"+endModel2.getId();
        AssocDissoc dissociation = new AssocDissoc(this, assocGlyphGlobalCoords, dissocId);

        // get the relevant points
        Point2D.Float startRcoordPoint = startModel.getAbsoluteAnchorCoordinate(startR.getAnchorPoint());
        Point2D.Float endR1coordPoint = endModel1.getAbsoluteAnchorCoordinate(endR1.getAnchorPoint());
        Point2D.Float endR2coordPoint = endModel2.getAbsoluteAnchorCoordinate(endR2.getAnchorPoint());

        // branch 0
        List<Point2D.Float> absoluteEditPoints0 = this.getBranchPoints(dissociation.getGlyph().getCenter(), startRcoordPoint, 0);
        Collections.reverse(absoluteEditPoints0);
        absoluteEditPoints0 = GeometryUtils.getNormalizedEndPoints(absoluteEditPoints0,
                startModel.getGlyph(),
                dissociation.getGlyph(),
                startModel.getReactantW().getAnchorPoint(),
                AnchorPoint.CENTER);
        //LinkModel link0 = new LinkModel(startModel, dissociation, new Link(absoluteEditPoints0));
        /*link0.setSbgnSpacePointList(
                link0.getNormalizedEndPoints(
                        startR1.getAnchorPoint(), GeometryUtils.AnchorPoint.CENTER
                ));*/

        List<Point2D.Float> absoluteEditPoints1 = this.getBranchPoints(dissociation.getGlyph().getCenter(), endR1coordPoint, 1);
        System.out.println("dissoc BUUUUG: "+absoluteEditPoints1);
        System.out.println("dissoc BUUUUG: "+dissociation.getGlyph()+" "+endModel1.getGlyph()+" "+endModel1.getReactantW().getAnchorPoint());
        absoluteEditPoints1 = GeometryUtils.getNormalizedEndPoints(absoluteEditPoints1,
                dissociation.getGlyph(),
                endModel1.getGlyph(),
                AnchorPoint.CENTER,
                endModel1.getReactantW().getAnchorPoint());
        System.out.println("dissoc BUUUUG: "+absoluteEditPoints1);
        LinkModel link1 = new LinkModel(dissociation, endModel1, new Link(absoluteEditPoints1),
                "prod_"+dissociation.getId()+"_"+endModel1.getId(), "production");
        /*link1.setSbgnSpacePointList(
                link1.getNormalizedEndPoints(
                        startR2.getAnchorPoint(), GeometryUtils.AnchorPoint.CENTER
                ));*/

        List<Point2D.Float> absoluteEditPoints2 = this.getBranchPoints(dissociation.getGlyph().getCenter(), endR2coordPoint, 2);
        absoluteEditPoints2 = GeometryUtils.getNormalizedEndPoints(absoluteEditPoints2,
                dissociation.getGlyph(),
                endModel2.getGlyph(),
                AnchorPoint.CENTER,
                endModel2.getReactantW().getAnchorPoint());
        LinkModel link2 = new LinkModel(dissociation, endModel2, new Link(absoluteEditPoints2),
                "prod_"+dissociation.getId()+"_"+endModel2.getId(), "production");
        /*link2.setSbgnSpacePointList(
                link2.getNormalizedEndPoints(
                        GeometryUtils.AnchorPoint.CENTER, endR.getAnchorPoint()
                ));*/

        if(this.hasProcess()) {

            System.out.println("association process segment "+reactionW.getProcessSegmentIndex());
            System.out.println("absolutepoints "+absoluteEditPoints0);

            boolean isPolyline = absoluteEditPoints0.size() > 2 ||
                    absoluteEditPoints1.size() > 2 ||absoluteEditPoints2.size() > 2;
            Line2D.Float processAxis = new Line2D.Float(absoluteEditPoints0.get(0),
                    absoluteEditPoints0.get(absoluteEditPoints0.size() - 1));
            /*
            !!!!!! process coords must be computed AFTER normalization of arrows !!!!!
            else, if the link is pointing to the center and not the border of the glyph, process will get shifted
            as the link is longer than what it appears.

            also here the segment indexes are reversed, as the number starts from dissociation glyph
             */
            Process process = new Process(
                    this,
                    GeometryUtils.getMiddleOfPolylineSegment(absoluteEditPoints0,
                            absoluteEditPoints0.size() - 2 - reactionW.getProcessSegmentIndex()),
                    UUID.randomUUID().toString(),
                    processAxis,
                    isPolyline);

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
                    startModel.getReactantW().getAnchorPoint(),
                    AnchorPoint.CENTER);
            List<Point2D.Float> normalizedSubLinesTuple2 = GeometryUtils.getNormalizedEndPoints(subLinesTuple.getValue(),
                    process.getGlyph(),
                    dissociation.getGlyph(),
                    AnchorPoint.CENTER,
                    AnchorPoint.CENTER);

            LinkModel l21 = new LinkModel(startModel, process, new Link(normalizedSubLinesTuple1),
                    "cons_"+startModel.getId()+"_"+process.getId(), "consumption");
            LinkModel l22 = new LinkModel(process, dissociation, new Link(normalizedSubLinesTuple2),
                    "cons_"+process.getId()+"_"+dissociation.getId(), "consumption");
            System.out.println("link edit points: "+l21.getLink().getStart()+" "+l21.getLink().getEditPoints());

            // add everything to the reaction lists
            this.getReactantModels().add(startModel);
            this.getReactantModels().add(endModel1);
            this.getReactantModels().add(endModel2);
            this.getReactionNodeModels().add(process);
            this.getReactionNodeModels().add(dissociation);
            this.getLinkModels().add(l21);
            this.getLinkModels().add(l22);
            this.getLinkModels().add(link1);
            this.getLinkModels().add(link2);

            this.addModifiers(process);
            this.addAdditionalReactants(process);
            this.addAdditionalProducts(process);

        }
        else {
            throw new RuntimeException("Association has no process ! How is it even possible. Reaction id: "+reactionW.getId());
        }


        /* TODO adapt
        ReactantWrapper startR = this.baseReactants.get(0);
        ReactantWrapper endR1 = this.baseProducts.get(0);
        ReactantWrapper endR2 = this.baseProducts.get(1);

        List<Integer> segmentCount = new ArrayList<>();
        segmentCount.add(0);
        segmentCount.add(0);
        segmentCount.add(0);
        List<CelldesignerLineDirectionDocument.CelldesignerLineDirection> listOfLineDirection =
                Arrays.asList(this.getBaseConnectScheme().getCelldesignerListOfLineDirection().getCelldesignerLineDirectionArray());
        for(CelldesignerLineDirectionDocument.CelldesignerLineDirection lineDirection: listOfLineDirection){
            int arm = Integer.parseInt(lineDirection.getArm());
            segmentCount.set(arm, segmentCount.get(arm) + 1);
        }

        System.out.println("branch segments count: "+segmentCount);

        // list edit points
        List<Point2D.Float> editPoints = this.getBaseEditPoints();

        // process association point
        Point2D.Float assocGlyphLocalCoords = editPoints.get(editPoints.size() - 1);
        Point2D.Float assocGlyphGlobalCoords = getAbsolutePoint(
                startR.getCenterPoint(),
                endR1.getCenterPoint(),
                endR2.getCenterPoint(), assocGlyphLocalCoords);
        System.out.println("result: " + assocGlyphLocalCoords + " -> " + assocGlyphGlobalCoords);

        this.process = new Process(assocGlyphGlobalCoords);

        List<Point2D.Float> absoluteEditPoints0 = this.getBranchPoints(process.getCoords(), startR.getLinkStartingPoint(), 0);
        Collections.reverse(absoluteEditPoints0);
        LinkWrapper link0 = new LinkWrapper(startR, process, absoluteEditPoints0);
        link0.setSbgnSpacePointList(
                link0.getNormalizedEndPoints(
                        startR.getAnchorPoint(), GeometryUtils.AnchorPoint.CENTER
                ));
        this.baseLinks.add(link0);

        List<Point2D.Float> absoluteEditPoints1 = this.getBranchPoints(process.getCoords(), endR1.getLinkStartingPoint(), 1);
        //Collections.reverse(absoluteEditPoints1);
        LinkWrapper link1 = new LinkWrapper(process, endR1, absoluteEditPoints1);
        link1.setSbgnSpacePointList(
                link1.getNormalizedEndPoints(
                        GeometryUtils.AnchorPoint.CENTER, endR1.getAnchorPoint()
                ));
        this.baseLinks.add(link1);

        List<Point2D.Float> absoluteEditPoints2 = this.getBranchPoints(process.getCoords(), endR2.getLinkStartingPoint(), 2);
        LinkWrapper link2 = new LinkWrapper(process, endR2, absoluteEditPoints2);
        link2.setSbgnSpacePointList(
                link2.getNormalizedEndPoints(
                        GeometryUtils.AnchorPoint.CENTER, endR2.getAnchorPoint()
                ));
        this.baseLinks.add(link2);*/
    }
}
