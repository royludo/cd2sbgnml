package fr.curie.cd2sbgnml;

import fr.curie.cd2sbgnml.graphics.AnchorPoint;
import fr.curie.cd2sbgnml.graphics.CdShape;
import fr.curie.cd2sbgnml.graphics.GeometryUtils;
import fr.curie.cd2sbgnml.graphics.Link;
import fr.curie.cd2sbgnml.model.Process;
import fr.curie.cd2sbgnml.xmlcdwrappers.*;
import fr.curie.cd2sbgnml.model.ReactantModel;
import fr.curie.cd2sbgnml.xmlcdwrappers.ReactantWrapper.ReactantType;
import fr.curie.cd2sbgnml.xmlcdwrappers.ReactionWrapper.ReactionType;
import org.sbfc.converter.GeneralConverter;
import org.sbfc.converter.exceptions.ConversionException;
import org.sbfc.converter.exceptions.ReadModelException;
import org.sbfc.converter.models.GeneralModel;
import org.sbgn.bindings.*;
import org.sbgn.GlyphClazz;
import org.sbgn.bindings.Map;
import org.sbgn.bindings.Point;
import org.sbml._2001.ns.celldesigner.*;
import org.sbml.sbml.level2.version4.*;
import org.sbml.sbml.level2.version4.OriginalModel.ListOfCompartments;
import org.sbml.sbml.level2.version4.OriginalModel.ListOfReactions;
import org.sbml.sbml.level2.version4.OriginalModel.ListOfSpecies;
import org.sbml.sbml.level2.version4.Species;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

import static org.sbgn.GlyphClazz.*;


public class SBGNML2CD extends GeneralConverter {

    final Logger logger = LoggerFactory.getLogger(SBGNML2CD.class);

    /**
     * Global translation factors that are to be applied to all elements
     */
    Rectangle2D mapBounds;
    Sbml sbml;
    boolean mapHasStyle;
    java.util.Map<String, StyleInfo> styleMap;
    /**
     * Keep track of created aliasWrappers to be referred to.
     */
    java.util.Map<String, AliasWrapper> aliasWrapperMap;

    /**
     * This map indexes all the arcs connected to each process node.
     */
    java.util.Map<String, List<Arc>> processToArcs;

    /**
     * Those 2 maps index the source and target glyph attached to each link.
     */
    java.util.Map<String, Glyph> arcToSource;
    java.util.Map<String, Glyph> arcToTarget;

    java.util.Map<String, Glyph> idToGlyph;

    java.util.Map<String, Glyph> portToGlyph;


    public Sbml toCD(Sbgn sbgn) {

        sbgn = SBGNUtils.sanitizeIds(sbgn);

        // consider only the first map
        Map sbgnMap = sbgn.getMap().get(0);

        // init celldesigner file
        sbml = this.initFile(sbgnMap);

        // init the index maps
        this.buildMaps(sbgnMap);


        /*
            Order of the glyphs and processes isn't guaranteed. We need to make a first pass to convert and
            layout all the glyph entities first. And then process the reaction through the process glyphs.
         */

        // first pass for EPNs
        for(Glyph glyph: sbgnMap.getGlyph()){
            String clazz = glyph.getClazz();
            switch (GlyphClazz.fromClazz(clazz)) {
                case COMPARTMENT:
                    processCompartment(glyph);
                    break;
                case MACROMOLECULE:
                case MACROMOLECULE_MULTIMER:
                case NUCLEIC_ACID_FEATURE:
                case NUCLEIC_ACID_FEATURE_MULTIMER:
                case SIMPLE_CHEMICAL:
                case SIMPLE_CHEMICAL_MULTIMER:
                case UNSPECIFIED_ENTITY:
                case PHENOTYPE:
                case SOURCE_AND_SINK:
                    processSpecies(glyph, false, false, null, null);
                    break;
                case COMPLEX:
                case COMPLEX_MULTIMER:
                    processSpecies(glyph, false, true, null, null);
                    break;
            }
        }

        // 2nd pass for process/reactions
        for(Glyph glyph: sbgnMap.getGlyph()){
            String clazz = glyph.getClazz();
            switch (GlyphClazz.fromClazz(clazz)) {
                case PROCESS:
                case OMITTED_PROCESS:
                case UNCERTAIN_PROCESS:
                case ASSOCIATION:
                case DISSOCIATION:
                    processReaction(glyph);
                    break;
            }
        }



        return sbml;
    }

    private void processReaction(Glyph processGlyph) {
        List<Arc> connectedArcs = processToArcs.get(processGlyph.getId());
        Point2D.Float processCoords = new Point2D.Float(
                processGlyph.getBbox().getX(),
                processGlyph.getBbox().getY()
        );
        System.out.println(">>>>process: "+processGlyph.getId());
        for(Arc arc: connectedArcs) {
            System.out.println(arc.getId()+" "+arc.getClazz());
        }

        boolean isReversible = SBGNUtils.isReactionReversible(connectedArcs);
        ReactionType reactionCDClass = ReactionType.STATE_TRANSITION; // default to basic reaction type
        System.out.println("reversible "+isReversible);

        /*
            We need to determine the base class of the reaction here, wether it's an association or dissociation.
            What happens when 2 complexes are associated together in a super complex ?
            Rely on the name of the glyphs being the same after they are inside the produced complex.
            For now, only basic checks in place.
         */
        List<List<Arc>> tmp = SBGNUtils.getReactantTypes(connectedArcs, isReversible);
        List<Arc> reactants = tmp.get(0);
        List<Arc> products = tmp.get(1);
        List<Arc> modifiers = tmp.get(2);
        System.out.println("connected glyphs: "+reactants.size()+" "+products.size()+" "+modifiers.size());

        // assign correct CellDesigner reaction type
        if(SBGNUtils.isReactionAssociation(processGlyph, reactants, products)) {
            reactionCDClass = ReactionType.HETERODIMER_ASSOCIATION;
        }
        else if(SBGNUtils.isReactionDissociation(processGlyph, reactants, products)) {
            reactionCDClass = ReactionType.DISSOCIATION;
        }
        else {
            switch(GlyphClazz.fromClazz(processGlyph.getClazz())) {
                case OMITTED_PROCESS:
                    reactionCDClass = ReactionType.KNOWN_TRANSITION_OMITTED;
                    break;
                case UNCERTAIN_PROCESS:
                    reactionCDClass = ReactionType.UNKNOWN_TRANSITION;
                    break;
            }
        }

        /* process and assign arcs to base reactants and products and additional reactants and products.
         * Base reactants and products are arbitrarily chosen among the reactants/products.
         */
        List<ReactantWrapper> baseReactantsW = new ArrayList<>();
        List<Glyph> baseReactantGlyphs = new ArrayList<>();
        List<Arc> baseReactantArcs = new ArrayList<>();
        List<ReactantWrapper> additionallReactantsW = new ArrayList<>();
        List<Glyph> additionalReactantGlyphs = new ArrayList<>();
        List<Arc> additionalReactantArcs = new ArrayList<>();
        List<ReactantWrapper> baseProductsW = new ArrayList<>();
        List<Glyph> baseProductGlyphs = new ArrayList<>();
        List<Arc> baseProductArcs = new ArrayList<>();
        List<ReactantWrapper> additionalProductsW = new ArrayList<>();
        List<Glyph> additionalProductGlyphs = new ArrayList<>();
        List<Arc> additionalProductArcs = new ArrayList<>();
        int i = 0;
        for(Arc arc: reactants) {
            Glyph g;
            if(isReversible) { // what is considered reactant was previously a product
                g = arcToTarget.get(arc.getId());
            }
            else {
                g = arcToSource.get(arc.getId());
            }
            AliasWrapper aliasW = aliasWrapperMap.get(g.getId()+"_alias1");
            System.out.println("Reactant: "+g.getId()+" "+g.getClazz());

            // set the first 2 as basereactants for association, if dissociation or normal reaction only the 1st
            if((reactionCDClass == ReactionType.HETERODIMER_ASSOCIATION &&  i==1)
                    || i == 0) {
                ReactantWrapper baseWrapper = new ReactantWrapper(aliasW, ReactantType.BASE_REACTANT);
                //baseWrapper.setAnchorPoint(AnchorPoint.CENTER); // set to CENTER for now, but better computed after

                baseReactantsW.add(baseWrapper);
                baseReactantGlyphs.add(g);
                baseReactantArcs.add(arc);
            }
            else {
                ReactantWrapper additionalWrapper = new ReactantWrapper(aliasW, ReactantType.ADDITIONAL_REACTANT);
                //additionalWrapper.setAnchorPoint(AnchorPoint.CENTER); // set to CENTER for now, but better computed after

                additionallReactantsW.add(additionalWrapper);
                additionalReactantGlyphs.add(g);
                additionalReactantArcs.add(arc);
            }
            i++;
        }

        i = 0;
        for(Arc arc: products) {
            Glyph g = arcToTarget.get(arc.getId());
            AliasWrapper aliasW = aliasWrapperMap.get(g.getId()+"_alias1");
            System.out.println("Product: "+g.getId()+" "+g.getClazz());

            // for dissociation consider first 2 as base, for association and normal only the 1st
            if(i == 0 || (reactionCDClass == ReactionType.DISSOCIATION && i == 1)) {
                ReactantWrapper baseWrapper = new ReactantWrapper(aliasW, ReactantType.BASE_PRODUCT);
                //baseWrapper.setAnchorPoint(AnchorPoint.CENTER); // set to CENTER for now, but better computed after

                baseProductsW.add(baseWrapper);
                baseProductGlyphs.add(g);
                baseProductArcs.add(arc);
            }
            else {
                ReactantWrapper additionalWrapper = new ReactantWrapper(aliasW, ReactantType.ADDITIONAL_PRODUCT);
                //additionalWrapper.setAnchorPoint(AnchorPoint.CENTER); // set to CENTER for now, but better computed after

                additionalProductsW.add(additionalWrapper);
                additionalProductGlyphs.add(g);
                additionalProductArcs.add(arc);
            }
            i++;
        }

        ReactionWrapper reactionW = new ReactionWrapper(processGlyph.getId().replaceAll("-","_"),
                reactionCDClass, baseReactantsW, baseProductsW);
        reactionW.setReversible(isReversible);
        Line2D.Float processLine;
        boolean isProcessOnPolyline;

        // geometry operations to get corrects reaction links
        if(reactionCDClass == ReactionType.HETERODIMER_ASSOCIATION) {
            System.out.println("ASSOCIATION REACTION");

            // set basic variables
            ReactantWrapper baseReactantW0 = baseReactantsW.get(0);
            Glyph baseReactantGlyph0 = baseReactantGlyphs.get(0);

            ReactantWrapper baseReactantW1 = baseReactantsW.get(1);
            Glyph baseReactantGlyph1 = baseReactantGlyphs.get(1);

            ReactantWrapper baseProductW = baseProductsW.get(0);
            Glyph baseProductGlyph = baseProductGlyphs.get(0);

            // get point lists in correct order
            // apply the mapBounds correction to each point of the arc to get consistent coords
            List<Point2D.Float> reactantPoints0 = applyCorrection(SBGNUtils.getPoints(baseReactantArcs.get(0)),
                    (float) mapBounds.getX(),(float) mapBounds.getY());
            List<Point2D.Float> reactantPoints1 = applyCorrection(SBGNUtils.getPoints(baseReactantArcs.get(1)),
                    (float) mapBounds.getX(),(float) mapBounds.getY());
            if(isReversible) {
                Collections.reverse(reactantPoints0);
                Collections.reverse(reactantPoints1);
            }
            Link reactantLink0 = new Link(reactantPoints0);
            Link reactantLink1 = new Link(reactantPoints1);
            Link productLink = new Link(applyCorrection(SBGNUtils.getPoints(baseProductArcs.get(0)),
                    (float) mapBounds.getX(),(float) mapBounds.getY()));

            // infer best anchorpoints possible
            Rectangle2D.Float baseRect0 = SBGNUtils.getRectangleFromGlyph(baseReactantGlyph0);
            baseRect0.setRect(
                    baseRect0.getX() - mapBounds.getX(),
                    baseRect0.getY() - mapBounds.getY(),
                    baseRect0.getWidth(),
                    baseRect0.getHeight());
            AnchorPoint startAnchor0 = inferAnchorPoint(reactantLink0.getStart(), baseReactantW0, baseRect0);
            baseReactantW0.setAnchorPoint(startAnchor0);

            Rectangle2D.Float baseRect1 = SBGNUtils.getRectangleFromGlyph(baseReactantGlyph1);
            baseRect1.setRect(
                    baseRect1.getX() - mapBounds.getX(),
                    baseRect1.getY() - mapBounds.getY(),
                    baseRect1.getWidth(),
                    baseRect1.getHeight());
            AnchorPoint startAnchor1 = inferAnchorPoint(reactantLink1.getStart(), baseReactantW1, baseRect1);
            baseReactantW1.setAnchorPoint(startAnchor1);

            Rectangle2D.Float productRect = SBGNUtils.getRectangleFromGlyph(baseProductGlyph);
            productRect.setRect(
                    productRect.getX() - mapBounds.getX(),
                    productRect.getY() - mapBounds.getY(),
                    productRect.getWidth(),
                    productRect.getHeight());
            AnchorPoint endAnchor = inferAnchorPoint(productLink.getEnd(), baseProductW, productRect);
            baseProductW.setAnchorPoint(endAnchor);

            // compute exact final points from the inferred anchor
            Point2D.Float finalStartPoint0 = getFinalpoint(
                    baseReactantW0.getAnchorPoint(),
                    baseReactantW0,
                    baseRect0);
            Point2D.Float finalStartPoint1 = getFinalpoint(
                    baseReactantW1.getAnchorPoint(),
                    baseReactantW1,
                    baseRect1);
            Point2D.Float finalEndPoint = getFinalpoint(
                    baseProductW.getAnchorPoint(),
                    baseProductW,
                    productRect);

            // define association glyph absolute point
            /*
                If there are ports, consider association to be on the first port, that way all the links are
                already pointing to it.
                If no ports, consider the Celldesigner association glyph to be on the process, and move the process
                a bit. Not too far because it will shift all the additional links and modification links.
              */
            Point2D.Float absAssocPoint = null;
            if(processGlyph.getPort().size() > 0) {
                // we need to get the port that is on the opposite side of the product
                Port oppositePort = (Port) baseProductArcs.get(0).getSource();
                Port consumptionPort = null;
                for(Port p: processGlyph.getPort()){
                    if(p != oppositePort) {
                        consumptionPort = p;
                        break;
                    }
                }
                absAssocPoint = new Point2D.Float(
                        consumptionPort.getX() - (float) mapBounds.getX(),
                        consumptionPort.getY() - (float) mapBounds.getY());

            }
            else {
                absAssocPoint = new Point2D.Float(
                        (float) (processCoords.getX() - mapBounds.getX()),
                        (float) (processCoords.getY() - mapBounds.getY())
                );
            }

            // compute association glyph relative point
            System.out.println("Coordinate system: "+baseReactantW0.getCenterPoint()+" "+
                    baseReactantW1.getCenterPoint()+" "+baseProductW.getCenterPoint());
            Point2D.Float localAssocPoint = GeometryUtils.convertPoints(
                    Collections.singletonList(absAssocPoint),
                    GeometryUtils.getTransformsToLocalCoords(
                            baseReactantW0.getCenterPoint(),
                            baseReactantW1.getCenterPoint(),
                            baseProductW.getCenterPoint()
                    )).get(0);
            System.out.println("Assoc coords: "+absAssocPoint+" -> "+localAssocPoint);

            /*  compute branch edit points
                reverse points for consumption because CellDesigner consider all branches to start from association
                glyph. Don't reverse points for reversible reactions, as the production arcs already point to the right
                direction.
            */
            System.out.println("Original branch 0: "+reactantLink0.getAllPoints());
            List<Point2D.Float> editpoints0 = reactantLink0.getEditPoints();
            if(!isReversible)
                Collections.reverse(editpoints0);
            System.out.println("Edit points 0 from assoc: "+editpoints0);
            List<Point2D.Float> localEditPoints0 = GeometryUtils.convertPoints(
                    editpoints0,
                    GeometryUtils.getTransformsToLocalCoords(
                            absAssocPoint,
                            finalStartPoint0
                    ));
            System.out.println("Branch 0: "+reactantLink0.getEditPoints()+" -> "+localEditPoints0);

            List<Point2D.Float> editpoints1 = reactantLink1.getEditPoints();
            if(!isReversible)
                Collections.reverse(editpoints1);
            List<Point2D.Float> localEditPoints1 = GeometryUtils.convertPoints(
                    editpoints1,
                    GeometryUtils.getTransformsToLocalCoords(
                            absAssocPoint,
                            finalStartPoint1
                    ));
            System.out.println("Branch 1: "+reactantLink1.getEditPoints()+" -> "+localEditPoints1);

            List<Point2D.Float> localEditPoints2 = GeometryUtils.convertPoints(
                    productLink.getEditPoints(),
                    GeometryUtils.getTransformsToLocalCoords(
                            absAssocPoint,
                            finalEndPoint
                    ));
            System.out.println("Branch 2: "+productLink.getEditPoints()+" -> "+localEditPoints2);

            if(productLink.getEditPoints().size() > 0) {
                processLine = new Line2D.Float(
                        absAssocPoint,
                        productLink.getEditPoints().get(0)
                );
                isProcessOnPolyline = true;
            }
            else {
                processLine = new Line2D.Float(
                        absAssocPoint,
                        finalEndPoint
                );
                isProcessOnPolyline = false;
            }

            // finally set up the xml elements and add to reactions
            int num0 = localEditPoints0.size();
            int num1 = localEditPoints1.size();
            int num2 = localEditPoints2.size();
            List<Integer> segmentCountList = Arrays.asList(num0+1, num1+1, num2+1);

            ConnectScheme connectScheme = getBranchConnectScheme(segmentCountList);

            Line line = new Line();
            line.setWidth(BigDecimal.valueOf(1));
            line.setColor("ff000000");

            List<String> editPointString = new ArrayList<>();
            List<Point2D.Float> mergedList = new ArrayList<>();
            mergedList.addAll(localEditPoints0);
            mergedList.addAll(localEditPoints1);
            mergedList.addAll(localEditPoints2);
            mergedList.add(localAssocPoint);
            for(Point2D.Float p: mergedList) {
                editPointString.add(p.getX()+","+p.getY());
            }

            LineWrapper lineWrapper = new LineWrapper(connectScheme, editPointString, line);
            lineWrapper.setNum0(num0);
            lineWrapper.setNum1(num1);
            lineWrapper.setNum2(num2);
            lineWrapper.settShapeIndex(0);

            reactionW.setLineWrapper(lineWrapper);

        }
        else if(reactionCDClass == ReactionType.DISSOCIATION) {
            System.out.println("DISSOCIATION REACTION");

            // set basic variables
            ReactantWrapper baseReactantW = baseReactantsW.get(0);
            Glyph baseReactantGlyph = baseReactantGlyphs.get(0);

            ReactantWrapper baseProductW1 = baseProductsW.get(0);
            Glyph baseProductGlyph1 = baseProductGlyphs.get(0);

            ReactantWrapper baseProductW2 = baseProductsW.get(1);
            Glyph baseProductGlyph2 = baseProductGlyphs.get(1);


            // get point lists in correct order
            // apply the mapBounds correction to each point of the arc to get consistent coords
            List<Point2D.Float> reactantPoints = applyCorrection(SBGNUtils.getPoints(baseReactantArcs.get(0)),
                    (float) mapBounds.getX(),(float) mapBounds.getY());
            if(isReversible) {
                Collections.reverse(reactantPoints);
            }
            Link reactantLink = new Link(reactantPoints);
            Link productLink1 = new Link(applyCorrection(SBGNUtils.getPoints(baseProductArcs.get(0)),
                    (float) mapBounds.getX(),(float) mapBounds.getY()));
            Link productLink2 = new Link(applyCorrection(SBGNUtils.getPoints(baseProductArcs.get(1)),
                    (float) mapBounds.getX(),(float) mapBounds.getY()));


            // infer best anchorpoints possible
            Rectangle2D.Float baseRect = SBGNUtils.getRectangleFromGlyph(baseReactantGlyph);
            baseRect.setRect(
                    baseRect.getX() - mapBounds.getX(),
                    baseRect.getY() - mapBounds.getY(),
                    baseRect.getWidth(),
                    baseRect.getHeight());
            AnchorPoint startAnchor = inferAnchorPoint(reactantLink.getStart(), baseReactantW, baseRect);
            baseReactantW.setAnchorPoint(startAnchor);

            Rectangle2D.Float productRect1 = SBGNUtils.getRectangleFromGlyph(baseProductGlyph1);
            productRect1.setRect(
                    productRect1.getX() - mapBounds.getX(),
                    productRect1.getY() - mapBounds.getY(),
                    productRect1.getWidth(),
                    productRect1.getHeight());
            AnchorPoint startAnchor1 = inferAnchorPoint(productLink1.getStart(), baseProductW1, productRect1);
            baseProductW1.setAnchorPoint(startAnchor1);

            Rectangle2D.Float productRect2 = SBGNUtils.getRectangleFromGlyph(baseProductGlyph2);
            productRect2.setRect(
                    productRect2.getX() - mapBounds.getX(),
                    productRect2.getY() - mapBounds.getY(),
                    productRect2.getWidth(),
                    productRect2.getHeight());
            AnchorPoint endAnchor = inferAnchorPoint(productLink2.getEnd(), baseProductW2, productRect2);
            baseProductW2.setAnchorPoint(endAnchor);


            // compute exact final points from the inferred anchor
            Point2D.Float finalStartPoint = getFinalpoint(
                    baseReactantW.getAnchorPoint(),
                    baseReactantW,
                    baseRect);
            Point2D.Float finalEndPoint1 = getFinalpoint(
                    baseProductW1.getAnchorPoint(),
                    baseProductW1,
                    productRect1);
            Point2D.Float finalEndPoint2 = getFinalpoint(
                    baseProductW2.getAnchorPoint(),
                    baseProductW2,
                    productRect2);


            // define association glyph absolute point
            /*
                If there are ports, consider dissociation to be on the 2nd port, that way all the links are
                already pointing to it.
                If no ports, consider the Celldesigner dissociation glyph to be on the process, and move the process
                a bit. Not too far because it will shift all the additional links and modification links.
              */
            Point2D.Float absDissocPoint = null;
            if(processGlyph.getPort().size() > 0) {
                // we need to get the port that is on the opposite side of the reactant
                Port oppositePort = (Port) baseReactantArcs.get(0).getTarget();
                Port productionPort = null;
                for(Port p: processGlyph.getPort()){
                    if(p != oppositePort) {
                        productionPort = p;
                        break;
                    }
                }
                absDissocPoint = new Point2D.Float(
                        productionPort.getX() - (float) mapBounds.getX(),
                        productionPort.getY() - (float) mapBounds.getY());

            }
            else {
                absDissocPoint = new Point2D.Float(
                        (float) (processCoords.getX() - mapBounds.getX()),
                        (float) (processCoords.getY() - mapBounds.getY())
                );
            }

            // compute dissoc glyph relative point
            System.out.println("Coordinate system: "+baseReactantW.getCenterPoint()+" "+
                    baseReactantW.getCenterPoint()+" "+baseProductW1.getCenterPoint());
            Point2D.Float localDissocPoint = GeometryUtils.convertPoints(
                    Collections.singletonList(absDissocPoint),
                    GeometryUtils.getTransformsToLocalCoords(
                            baseReactantW.getCenterPoint(),
                            baseProductW1.getCenterPoint(),
                            baseProductW2.getCenterPoint()
                    )).get(0);
            System.out.println("Dissoc coords: "+absDissocPoint+" -> "+localDissocPoint);


            /*  compute branch edit points
                reverse points for consumption because CellDesigner consider all branches to start from association
                glyph. Don't reverse points for reversible reactions, as the production arcs already point to the right
                direction.
            */
            System.out.println("Original branch 0: "+reactantLink.getAllPoints());
            List<Point2D.Float> editpoints0 = reactantLink.getEditPoints();
            if(!isReversible)
                Collections.reverse(editpoints0);
            System.out.println("Edit points 0 from assoc: "+reactantLink.getEditPoints());
            List<Point2D.Float> localEditPoints0 = GeometryUtils.convertPoints(
                    editpoints0,
                    GeometryUtils.getTransformsToLocalCoords(
                            absDissocPoint,
                            finalStartPoint
                    ));
            System.out.println("Branch 0: "+reactantLink.getEditPoints()+" -> "+localEditPoints0);

            List<Point2D.Float> localEditPoints1 = GeometryUtils.convertPoints(
                    productLink1.getEditPoints(),
                    GeometryUtils.getTransformsToLocalCoords(
                            absDissocPoint,
                            finalEndPoint1
                    ));
            System.out.println("Branch 1: "+productLink1.getEditPoints()+" -> "+localEditPoints1);

            List<Point2D.Float> localEditPoints2 = GeometryUtils.convertPoints(
                    productLink2.getEditPoints(),
                    GeometryUtils.getTransformsToLocalCoords(
                            absDissocPoint,
                            finalEndPoint2
                    ));
            System.out.println("Branch 2: "+productLink2.getEditPoints()+" -> "+localEditPoints2);

            // get segment on which CellDesigner will put process
            if(editpoints0.size() > 0) {
                processLine = new Line2D.Float(
                        editpoints0.get(0),
                        absDissocPoint
                );
                isProcessOnPolyline = true;
            }
            else {
                processLine = new Line2D.Float(
                        finalStartPoint,
                        absDissocPoint
                );
                isProcessOnPolyline = false;
            }


            // finally set up the xml elements and add to reactions
            int num0 = localEditPoints0.size();
            int num1 = localEditPoints1.size();
            int num2 = localEditPoints2.size();
            List<Integer> segmentCountList = Arrays.asList(num0+1, num1+1, num2+1);

            ConnectScheme connectScheme = getBranchConnectScheme(segmentCountList);

            Line line = new Line();
            line.setWidth(BigDecimal.valueOf(1));
            line.setColor("ff000000");

            List<String> editPointString = new ArrayList<>();
            List<Point2D.Float> mergedList = new ArrayList<>();
            mergedList.addAll(localEditPoints0);
            mergedList.addAll(localEditPoints1);
            mergedList.addAll(localEditPoints2);
            mergedList.add(localDissocPoint);
            for(Point2D.Float p: mergedList) {
                editPointString.add(p.getX()+","+p.getY());
            }

            LineWrapper lineWrapper = new LineWrapper(connectScheme, editPointString, line);
            lineWrapper.setNum0(num0);
            lineWrapper.setNum1(num1);
            lineWrapper.setNum2(num2);
            lineWrapper.settShapeIndex(0);

            reactionW.setLineWrapper(lineWrapper);

        }
        else {
            ReactantWrapper baseReactantW = baseReactantsW.get(0);
            Glyph baseReactantGlyph = baseReactantGlyphs.get(0);
            ReactantWrapper baseProductW = baseProductsW.get(0);
            Glyph baseProductGlyph = baseProductGlyphs.get(0);

            // get the consumption and production arc's points and merge them together
            List<Point2D.Float> reactantPoints = SBGNUtils.getPoints(baseReactantArcs.get(0));
            if(isReversible)
                Collections.reverse(reactantPoints);
            List<Point2D.Float> productPoints = SBGNUtils.getPoints(baseProductArcs.get(0));
            // TODO what if both go the center of the process ?
            List<Point2D.Float> completeLinkPoints = new ArrayList<>(reactantPoints);
            completeLinkPoints.addAll(productPoints);
            System.out.println("Base link points: "+completeLinkPoints);

            // gather only edit points, as they are the one who will undergo transformations into local
            // coordinate system
            List<Point2D.Float> editPointsOnly;
            if(completeLinkPoints.size() > 2) {
                editPointsOnly = completeLinkPoints.subList(1, completeLinkPoints.size() - 1);

            }
            else {
                editPointsOnly = new ArrayList<>();
            }
            Point2D.Float startPoint = completeLinkPoints.get(0);
            Point2D.Float endPoint = completeLinkPoints.get(completeLinkPoints.size() - 1);

            // infer best anchorpoints possible
            AnchorPoint startAnchor = inferAnchorPoint(startPoint, baseReactantW,
                    SBGNUtils.getRectangleFromGlyph(baseReactantGlyphs.get(0)));
            baseReactantW.setAnchorPoint(startAnchor);

            AnchorPoint endAnchor = inferAnchorPoint(endPoint, baseProductW,
                    SBGNUtils.getRectangleFromGlyph(baseProductGlyphs.get(0)));
            baseProductW.setAnchorPoint(endAnchor);

            /*
                Here we need to compute the exact start and end points first = the 2 points where CellDesigner will
                put our endpoints in the end = the points located precisely and the previously computed anchor points.
                if not, the links will be distorted.
             */
            Point2D.Float finalStartPoint = getFinalpoint(
                    baseReactantW.getAnchorPoint(),
                    baseReactantW,
                    SBGNUtils.getRectangleFromGlyph(baseReactantGlyph));
            Point2D.Float finalEndPoint = getFinalpoint(
                    baseProductW.getAnchorPoint(),
                    baseProductW,
                    SBGNUtils.getRectangleFromGlyph(baseProductGlyph));

            List<Point2D.Float> localEditPoints = GeometryUtils.convertPoints(
                    editPointsOnly,
                    GeometryUtils.getTransformsToLocalCoords(
                            finalStartPoint,
                            finalEndPoint
                    ));
            System.out.println("Local edit points "+localEditPoints);

            // get segment on which CellDesigner will put process
            if(editPointsOnly.size() > 0) {
                processLine = new Line2D.Float(
                        completeLinkPoints.get(reactantPoints.size() - 1),
                        completeLinkPoints.get(reactantPoints.size())
                );
                isProcessOnPolyline = true;
            }
            else {
                processLine = new Line2D.Float(
                        finalStartPoint,
                        finalEndPoint
                );
                isProcessOnPolyline = false;
            }


            /*
                Finally build the appropriate xml elements and pass it to the reaction
             */

            int segmentCount = completeLinkPoints.size() - 1;
            int processSegmentIndex = reactantPoints.size() - 1;

            ConnectScheme connectScheme = getSimpleConnectScheme(segmentCount, processSegmentIndex);

            Line line = new Line();
            line.setWidth(BigDecimal.valueOf(1));
            line.setColor("ff000000");

            List<String> editPointString = new ArrayList<>();
            for(Point2D.Float p: localEditPoints) {
                editPointString.add(p.getX()+","+p.getY());
            }

            LineWrapper lineWrapper = new LineWrapper(connectScheme, editPointString, line);

            reactionW.setLineWrapper(lineWrapper);

        }

        // now that base reaction link is established, build the process model
        System.out.println("Process is on: "+processLine.getP1()+" "+processLine.getP2());
        Process pr = new Process(
                GeometryUtils.getMiddle((Point2D.Float) processLine.getP1(),(Point2D.Float) processLine.getP2()),
                processGlyph.getId(),
                processLine,
                isProcessOnPolyline,
                new StyleInfo(processGlyph.getId())
        );
        System.out.println("anchors 0 and 1: "+pr.getAbsoluteAnchorCoords(0)+" "+pr.getAbsoluteAnchorCoords(1));


        // process additional reactants and products
        for(i=0; i < additionalReactantArcs.size(); i++) {
            Arc additionalArc = additionalReactantArcs.get(i);
            ReactantWrapper additionalW = additionallReactantsW.get(i);
            Glyph additionalGlyph = additionalReactantGlyphs.get(i);

            additionalW.setTargetLineIndex("-1,0");

            List<Point2D.Float> additionalReactPoints = SBGNUtils.getPoints(additionalArc);
            if(isReversible)
                Collections.reverse(additionalReactPoints);

            // gather only edit points, as they are the one who will undergo transformations into local
            // coordinate system
            List<Point2D.Float> editPointsOnly;
            if(additionalReactPoints.size() > 2) {
                editPointsOnly = additionalReactPoints.subList(1, additionalReactPoints.size() - 1);

            }
            else {
                editPointsOnly = new ArrayList<>();
            }
            Point2D.Float startPoint = additionalReactPoints.get(0);

            // infer best anchorpoints possible
            AnchorPoint startAnchor = inferAnchorPoint(startPoint, additionalW,
                    SBGNUtils.getRectangleFromGlyph(additionalGlyph));
            additionalW.setAnchorPoint(startAnchor);

            Point2D.Float finalStartPoint = getFinalpoint(
                    additionalW.getAnchorPoint(),
                    additionalW,
                    SBGNUtils.getRectangleFromGlyph(additionalGlyph));

            // infer coordinates for the process' anchor point 0
            Point2D.Float anchor0 = pr.getAbsoluteAnchorCoords(0);

            List<Point2D.Float> localEditPoints = GeometryUtils.convertPoints(
                    editPointsOnly,
                    GeometryUtils.getTransformsToLocalCoords(
                            finalStartPoint,
                            anchor0
                    ));
            System.out.println("Local edit points "+localEditPoints);

            int segmentCount = localEditPoints.size() + 1;

            ConnectScheme connectScheme = getSimpleConnectScheme(segmentCount, -1);

            LineType2 line = new LineType2();
            line.setWidth(BigDecimal.valueOf(1));
            line.setColor("ff000000");
            line.setType("Straight");

            List<String> editPointString = new ArrayList<>();
            for(Point2D.Float p: localEditPoints) {
                editPointString.add(p.getX()+","+p.getY());
            }

            LineWrapper lineWrapper = new LineWrapper(connectScheme, editPointString, line);

            additionalW.setLineWrapper(lineWrapper);
            reactionW.getAdditionalReactants().add(additionalW);

        }

        sbml.getModel().getListOfReactions().getReaction().add(reactionW.getCDReaction());

    }

    /**
     *
     * @param segmentCount
     * @param processSegmentIndex -1 or positive integer. If -1, discard the value.
     * @return
     */
    private ConnectScheme getSimpleConnectScheme(int segmentCount, int processSegmentIndex) {
        ConnectScheme connectScheme = new ConnectScheme();
        connectScheme.setConnectPolicy("direct");
        if(processSegmentIndex != -1) {
            connectScheme.setRectangleIndex(String.valueOf(processSegmentIndex));
        }

        ListOfLineDirection listOfLineDirection = new ListOfLineDirection();
        connectScheme.setListOfLineDirection(listOfLineDirection);

        for(int i=0; i < segmentCount; i++) {
            LineDirection lineDirection = new LineDirection();
            lineDirection.setIndex((short) i);
            lineDirection.setValue("unknown");
            listOfLineDirection.getLineDirection().add(lineDirection);
        }

        return connectScheme;
    }

    private ConnectScheme getBranchConnectScheme(List<Integer> segmentCountlist) {
        ConnectScheme connectScheme = new ConnectScheme();
        connectScheme.setConnectPolicy("direct");

        ListOfLineDirection listOfLineDirection = new ListOfLineDirection();
        connectScheme.setListOfLineDirection(listOfLineDirection);

        for(int i=0; i < segmentCountlist.size(); i++) {
            for(int j=0; j < segmentCountlist.get(i); j++) {
                LineDirection lineDirection = new LineDirection();
                lineDirection.setArm((short) i);
                lineDirection.setIndex((short) j);
                lineDirection.setValue("unknown");
                listOfLineDirection.getLineDirection().add(lineDirection);
            }
        }

        return connectScheme;
    }

    private void processSpecies(Glyph glyph, boolean isIncluded, boolean isComplex,
                                String parentSpeciesId, String parentAliasId) {
        String label = glyph.getLabel() == null ? "": glyph.getLabel().getText();

        System.out.println(glyph.getClazz()+" "+isIncluded+" "+isComplex);
        // first determine specific subtypes
        SpeciesWrapper.ReferenceType subType = null;
        boolean ionFlag = false;
        boolean drugFlag = false;
        if(!isComplex) {
            switch(GlyphClazz.fromClazz(glyph.getClazz())) {
                case MACROMOLECULE:
                case MACROMOLECULE_MULTIMER:
                    subType = SpeciesWrapper.ReferenceType.GENERIC; // default
                    if(SBGNUtils.hasUnitOfInfo(glyph, "receptor")) {
                        subType = SpeciesWrapper.ReferenceType.RECEPTOR;
                    }
                    if(SBGNUtils.hasUnitOfInfo(glyph, "ion channel")) {
                        subType = SpeciesWrapper.ReferenceType.ION_CHANNEL;
                    }
                    if(SBGNUtils.hasUnitOfInfo(glyph, "truncated")) {
                        subType = SpeciesWrapper.ReferenceType.TRUNCATED;
                    }
                    break;
                case NUCLEIC_ACID_FEATURE:
                case NUCLEIC_ACID_FEATURE_MULTIMER:
                    subType = SpeciesWrapper.ReferenceType.GENE; // default
                    if(SBGNUtils.hasUnitOfInfo(glyph, "rna")) {
                        subType = SpeciesWrapper.ReferenceType.RNA;
                    }
                    if(SBGNUtils.hasUnitOfInfo(glyph, "asrna")) {
                        subType = SpeciesWrapper.ReferenceType.ANTISENSE_RNA;
                    }
                    break;
                case SIMPLE_CHEMICAL:
                case SIMPLE_CHEMICAL_MULTIMER:
                    if(SBGNUtils.hasUnitOfInfo(glyph, "ion")) {
                        ionFlag = true;
                    }
                    if(SBGNUtils.hasUnitOfInfo(glyph, "drug")) {
                        drugFlag = true;
                    }
                    break;
            }
        }

        // create associated reference type (protein, gene...)
        // if no particular type, default to own id (for complex, simple molecules...)
        String referenceId = glyph.getId();
        if(subType != null) {
            switch(subType) {
                case GENERIC:
                case RECEPTOR:
                case ION_CHANNEL:
                case TRUNCATED:
                    Protein prot = new Protein();
                    prot.setType(subType.toString());
                    referenceId = "prot_"+glyph.getId();
                    prot.setId(referenceId);
                    prot.setName(label);
                    sbml.getModel().getAnnotation().getExtension().getListOfProteins().getProtein().add(prot);
                    break;
                case GENE:
                    Gene gene = new Gene();
                    referenceId = "gene_"+glyph.getId();
                    gene.setId(referenceId);
                    gene.setName(label);
                    gene.setType("GENE");
                    sbml.getModel().getAnnotation().getExtension().getListOfGenes().getGene().add(gene);
                    break;
                case RNA:
                    RNA rna = new RNA();
                    referenceId = "rna_"+glyph.getId();
                    rna.setId(referenceId);
                    rna.setName(label);
                    rna.setType("RNA");
                    sbml.getModel().getAnnotation().getExtension().getListOfRNAs().getRNA().add(rna);
                    break;
                case ANTISENSE_RNA:
                    AntisenseRNA asrna = new AntisenseRNA();
                    referenceId = "asrna_"+glyph.getId();
                    asrna.setId(referenceId);
                    asrna.setName(label);
                    asrna.setType("ANTISENSE_RNA");
                    sbml.getModel().getAnnotation().getExtension().getListOfAntisenseRNAs().getAntisenseRNA().add(asrna);
                    break;
            }
        }

        // create the species + alias couple
        SpeciesWrapper speciesW;
        String aliasId = glyph.getId()+"_alias1";
        AliasWrapper aliasW;
        if(isComplex) {
            speciesW = new SpeciesWrapper(glyph.getId(), label, null);
            aliasW = new AliasWrapper(aliasId, AliasWrapper.AliasType.COMPLEX, speciesW);
        }
        else {
            speciesW = new SpeciesWrapper(glyph.getId(), label, subType);
            aliasW = new AliasWrapper(aliasId, AliasWrapper.AliasType.BASIC, speciesW);
        }
        speciesW.getAliases().add(aliasW);


        // PROCESS SPECIES
        // class
        if(ionFlag) {
            speciesW.setCdClass("ION");
        }
        else if(drugFlag) {
            speciesW.setCdClass("DRUG");
        }
        else {
            speciesW.setCdClass(ReactantModel.getCdClass(glyph.getClazz(), subType));
        }
        System.out.println("cd clazz: "+speciesW.getCdClass());

        // compartmentRef
        if(glyph.getCompartmentRef() != null) {
            speciesW.setCompartment(((Glyph) glyph.getCompartmentRef()).getId());
        }
        else {
            speciesW.setCompartment("default");
        }

        // add species to correct list
        if(isIncluded) {
            speciesW.setComplex(parentSpeciesId);
            org.sbml._2001.ns.celldesigner.Species species = speciesW.getCDIncludedSpecies(referenceId);
            sbml.getModel().getAnnotation().getExtension().getListOfIncludedSpecies().getSpecies().add(species);
        }
        else {
            Species species = speciesW.getCDNormalSpecies(referenceId);
            sbml.getModel().getListOfSpecies().getSpecies().add(species);
        }

        // PROCESS ALIAS
        // compartmentRef
        if(glyph.getCompartmentRef() != null) {
            aliasW.setCompartmentAlias(((Glyph) glyph.getCompartmentRef()).getId()+"_alias1"); // TODO get alias id properly
        }

        Bounds bounds = new Bounds();
        bounds.setX(BigDecimal.valueOf(glyph.getBbox().getX()- (float) mapBounds.getX()));
        bounds.setY(BigDecimal.valueOf(glyph.getBbox().getY()- (float) mapBounds.getY()));
        bounds.setW(BigDecimal.valueOf(glyph.getBbox().getW()));
        bounds.setH(BigDecimal.valueOf(glyph.getBbox().getH()));
        aliasW.setBounds(bounds);

        // style
        aliasW.setStyleInfo(styleMap.get(glyph.getId()));

        if(isIncluded) {
            aliasW.setComplexAlias(parentAliasId);
        }

        // add alias to correct list
        if(isComplex) {
            ListOfComplexSpeciesAliases.ComplexSpeciesAlias complexSpeciesAlias = aliasW.getCDComplexSpeciesAlias();
            sbml.getModel().getAnnotation().getExtension().getListOfComplexSpeciesAliases()
                    .getComplexSpeciesAlias().add(complexSpeciesAlias);
        }
        else {
            SpeciesAlias speciesAlias = aliasW.getCDSpeciesAlias();
            sbml.getModel().getAnnotation().getExtension().getListOfSpeciesAliases().getSpeciesAlias().add(speciesAlias);
        }
        aliasWrapperMap.put(aliasW.getId(), aliasW);

        // recursively process included glyphs
        for(Glyph subglyph: glyph.getGlyph()) {
            // only concerned about EPN subunits
            if(! subglyph.getClazz().equals("unit of information")
                    && ! subglyph.getClazz().equals("state variable")
                    && ! subglyph.getClazz().equals("entity")) {
                GlyphClazz subClazz = GlyphClazz.fromClazz(subglyph.getClazz());
                boolean isSubGlyphComplex = false;
                if(subClazz == COMPLEX || subClazz == COMPLEX_MULTIMER) {
                    isSubGlyphComplex = true;
                }
                processSpecies(subglyph, true, isSubGlyphComplex, glyph.getId(), aliasId);
            }
        }


    }

    private void processCompartment(Glyph glyph) {

        String label = glyph.getLabel() == null ? "": glyph.getLabel().getText();
        CompartmentWrapper compM = new CompartmentWrapper(
                glyph.getId(),
                label,
                new Rectangle2D.Float(
                        glyph.getBbox().getX() - (float) mapBounds.getX(),
                        glyph.getBbox().getY() - (float) mapBounds.getY(),
                        glyph.getBbox().getW(),
                        glyph.getBbox().getH())
        );

        if(glyph.getCompartmentRef() != null) {
            compM.setOutside(((Glyph) glyph.getCompartmentRef()).getId());
        }

        if(mapHasStyle) {
            compM.setStyleInfo(styleMap.get(glyph.getId()));
        }

        // label is precisely placed
        if(glyph.getLabel() != null
                && glyph.getLabel().getBbox() != null) {
            Point2D namePoint = new Point2D.Float(
                    glyph.getLabel().getBbox().getX(),
                    glyph.getLabel().getBbox().getY()
            );
            compM.setNamePoint(namePoint);
        }

        // notes
        if(glyph.getNotes() != null
                && glyph.getNotes().getAny().size() > 0) {
            Element notes = glyph.getNotes().getAny().get(0);
            compM.setNotes(notes);
        }

        // rdf annotations
        if(glyph.getExtension() != null) {
            for(Element e: glyph.getExtension().getAny()){
                if(e.getTagName().equals("annotation")) {
                    // TODO urn:miriam:CHEBI:12 doesn't seem to be loaded by CD
                    // TODO find a way to resolve uri ?
                    Element rdf = SBGNUtils.sanitizeRdfURNs((Element) e.getElementsByTagName("rdf:RDF").item(0));
                    compM.setAnnotations(rdf);
                }
            }
        }

        sbml.getModel().getListOfCompartments().getCompartment()
                .add(compM.getCDCompartment());
        sbml.getModel().getAnnotation().getExtension().getListOfCompartmentAliases().getCompartmentAlias()
                .add(compM.getCDCompartmentAlias());

    }


    private Sbml initFile(Map map) {

        Sbml sbml = new Sbml();
        sbml.setLevel(BigInteger.valueOf(2));
        sbml.setVersion(BigInteger.valueOf(4));


        Model model = new Model();
        sbml.setModel(model);
        model.setId("untitled");
        model.setMetaid("untitled");

        ModelAnnotationType annotation = new ModelAnnotationType();
        model.setAnnotation(annotation);

        ModelAnnotationType.Extension ext = new ModelAnnotationType.Extension();
        ext.setModelVersion(BigDecimal.valueOf(4.0));

        this.mapBounds = SBGNUtils.getMapBounds(map);
        ModelDisplay modelDisplay = new ModelDisplay();
        modelDisplay.setSizeX((short) mapBounds.getWidth());
        modelDisplay.setSizeY((short) mapBounds.getHeight());
        ext.setModelDisplay(modelDisplay);

        ext.setListOfSpeciesAliases(new ListOfSpeciesAliases());
        ext.setListOfAntisenseRNAs(new ListOfAntisenseRNAs());
        ext.setListOfBlockDiagrams(new ListOfBlockDiagrams());
        ext.setListOfCompartmentAliases(new ListOfCompartmentAliases());
        ext.setListOfComplexSpeciesAliases(new ListOfComplexSpeciesAliases());
        ext.setListOfGenes(new ListOfGenes());
        ext.setListOfGroups(new ListOfGroups());
        ext.setListOfIncludedSpecies(new ListOfIncludedSpecies());
        ext.setListOfLayers(new ListOfLayers());
        ext.setListOfProteins(new ListOfProteins());
        ext.setListOfRNAs(new ListOfRNAs());

        annotation.setExtension(ext);

        ListOfCompartments listOfCompartments = new ListOfCompartments();
        model.setListOfCompartments(listOfCompartments);

        // default compartment
        Compartment defaultCompartment = new Compartment();
        listOfCompartments.getCompartment().add(defaultCompartment);
        defaultCompartment.setId("default");
        defaultCompartment.setMetaid("default");
        defaultCompartment.setSize(1d);
        defaultCompartment.setUnits("volume");


        // pure sbml part
        ListOfSpecies listOfSpecies = new ListOfSpecies();
        model.setListOfSpecies(listOfSpecies);

        ListOfReactions listOfReactions = new ListOfReactions();
        model.setListOfReactions(listOfReactions);

        /*try {
            annotation.setXMLNode();
        } catch (XMLStreamException e) {
            e.printStackTrace();
        }*/


        //Node sbmlDocument = SbmlDocument.Factory.newInstance().getDomNode();

        //Node sbml = (Element) SbmlDocument.Sbml.Factory.newInstance().getDomNode();
       // sbml.set ("2");
        //sbml.setVersion("4");

        //ModelDocument.Model model = ModelDocument.Model.Factory.newInstance();
        //model.setId("untitled");
        /*
            seems impossible to customize the xml through getDOMNode
         */

        /*Attr attr = model.getDomNode().getOwnerDocument().createAttribute("metaid");
        attr.setValue("untitled");
        model.getDomNode().getAttributes().setNamedItemNS(attr);*/
        //((Element) model.getDomNode().at .setAttribute("metaid", "untitled");

        //AnnotationDocument.Annotation annotation = AnnotationDocument.Annotation.Factory.newInstance();
        //annotation.addNewCelldesignerModelVersion();


        //annotation.setCelldesignerModelVersion(modelVersion);
       /* model.setAnnotation(annotation);
        sbml.setModel(model);
        sbmlDocument.setSbml(sbml);*/

        return sbml;
    }

    private void buildMaps(Map map) {

        idToGlyph = new HashMap<>();
        arcToSource = new HashMap<>();
        arcToTarget = new HashMap<>();
        processToArcs = new HashMap<>();
        portToGlyph = new HashMap<>();
        aliasWrapperMap = new HashMap<>();

        // parse all the style info
        styleMap = new HashMap<>();
        mapHasStyle = false;
        for(Element e: map.getExtension().getAny()) {
            if(e.getTagName().equals("renderInformation")) {
                styleMap = SBGNUtils.mapStyleinfo(e);
                mapHasStyle = true;
            }
        }

        for(Glyph g: map.getGlyph()) {
            GlyphClazz clazz = GlyphClazz.fromClazz(g.getClazz());
            idToGlyph.put(g.getId(), g);
            for(Port p: g.getPort()) {
                portToGlyph.put(p.getId(), g);
            }
            if(clazz == PROCESS || clazz == UNCERTAIN_PROCESS || clazz == OMITTED_PROCESS
                    || clazz == ASSOCIATION || clazz == DISSOCIATION) {
                processToArcs.put(g.getId(), new ArrayList<>());
            }
        }

        for(Arc arc: map.getArc()) {
            Glyph sourceGlyph;
            Glyph targetGlyph;
            if(arc.getSource() instanceof Port) {
                Port p = (Port) arc.getSource();
                sourceGlyph = portToGlyph.get(p.getId());
                arcToSource.put(arc.getId(), sourceGlyph);
            }
            else { // glyph itself
                sourceGlyph = (Glyph) arc.getSource();
                arcToSource.put(arc.getId(), sourceGlyph);
            }

            if(arc.getTarget() instanceof Port) {
                Port p = (Port) arc.getTarget();
                targetGlyph = portToGlyph.get(p.getId());
                arcToTarget.put(arc.getId(), targetGlyph);
            }
            else { // glyph itself
                targetGlyph = (Glyph) arc.getTarget();
                arcToTarget.put(arc.getId(), targetGlyph);
            }

            if(processToArcs.containsKey(sourceGlyph.getId())){
                processToArcs.get(sourceGlyph.getId()).add(arc);
            }
            if(processToArcs.containsKey(targetGlyph.getId())){
                processToArcs.get(targetGlyph.getId()).add(arc);
            }
        }

    }

    public static AnchorPoint inferAnchorPoint(Point2D.Float p, ReactantWrapper reactantW, Rectangle2D.Float rect) {
        SpeciesWrapper speciesW = reactantW.getAliasW().getSpeciesW();
        CdShape baseProductShape = ReactantModel.getCdShape(
                speciesW.getCdClass(),
                speciesW.getType());
        AnchorPoint a = GeometryUtils.getNearestAnchorPoint(
                p,
                rect,
                baseProductShape
        );
        return a;
    }

    public static Point2D.Float getFinalpoint(AnchorPoint a, ReactantWrapper reactantW, Rectangle2D.Float r) {
        SpeciesWrapper speciesW = reactantW.getAliasW().getSpeciesW();
        CdShape baseProductShape = ReactantModel.getCdShape(
                speciesW.getCdClass(),
                speciesW.getType());
        return GeometryUtils.getAbsoluteAnchorPoint(
                baseProductShape,
                r,
                a);
    }

    /**
     * Apply correcting factor inplace to a the coordinates of a list of points
     * @param points
     * @param xdiff
     * @param ydiff
     * @return
     */
    public static List<Point2D.Float> applyCorrection(List<Point2D.Float> points, float xdiff, float ydiff) {
        for(Point2D.Float p: points) {
            p.setLocation(p.getX() - xdiff, p.getY() - ydiff);
        }
        return points;
    }


    @Override
    public GeneralModel convert(GeneralModel generalModel) throws ConversionException, ReadModelException {
        SBGNSBFCModel sbgnModel = (SBGNSBFCModel) generalModel;
        return new CellDesignerSBFCModel(this.toCD(sbgnModel.getModel()));
    }

    @Override
    public String getResultExtension() {
        return null;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public String getHtmlDescription() {
        return null;
    }
}
