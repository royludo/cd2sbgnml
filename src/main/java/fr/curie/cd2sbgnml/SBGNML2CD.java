package fr.curie.cd2sbgnml;

import fr.curie.cd2sbgnml.graphics.AnchorPoint;
import fr.curie.cd2sbgnml.graphics.CdShape;
import fr.curie.cd2sbgnml.graphics.GeometryUtils;
import fr.curie.cd2sbgnml.graphics.Link;
import fr.curie.cd2sbgnml.model.LinkModel;
import fr.curie.cd2sbgnml.model.Process;
import fr.curie.cd2sbgnml.xmlcdwrappers.*;
import fr.curie.cd2sbgnml.model.ReactantModel;
import fr.curie.cd2sbgnml.xmlcdwrappers.ReactantWrapper.ReactantType;
import fr.curie.cd2sbgnml.xmlcdwrappers.ReactionWrapper.ReactionType;
import org.sbfc.converter.GeneralConverter;
import org.sbfc.converter.exceptions.ConversionException;
import org.sbfc.converter.exceptions.ReadModelException;
import org.sbfc.converter.models.GeneralModel;
import org.sbgn.ArcClazz;
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
    java.util.Map<String, SpeciesWrapper> speciesWrapperMap;
    java.util.Map<String, Protein> protMap;
    java.util.Map<String, RNA> rnaMap;
    java.util.Map<String, Gene> geneMap;
    java.util.Map<String, AntisenseRNA> asrnaMap;


    /**
     * This map indexes all the arcs connected to each process node.
     */
    java.util.Map<String, List<Arc>> processToArcs;

    /**
     * This list will contain all arcs that are not linked to any process node.
     * For example, phenotype arcs.
     * Or AF map arcs.
     */
    List<Arc> orphanArcs;

    /**
     * Those 2 maps index the source and target glyph attached to each link.
     */
    java.util.Map<String, Glyph> arcToSource;
    java.util.Map<String, Glyph> arcToTarget;

    java.util.Map<String, Glyph> idToGlyph;

    java.util.Map<String, Glyph> portToGlyph;

    java.util.Map<String, List<Arc>> glyphToArc;


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
                case PERTURBING_AGENT:
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

        // now process the remaining orphan arcs
        for(Arc orphanArc: orphanArcs) {
            processOrphanArc(orphanArc);
        }

        processEnd();



        return sbml;
    }

    private void processEnd() {
        for(SpeciesWrapper speciesW: speciesWrapperMap.values()){
            // add species to correct list
            if(speciesW.isIncludedSpecies()) {
                ListOfIncludedSpecies listOfIncludedSpecies =
                        sbml.getModel().getAnnotation().getExtension().getListOfIncludedSpecies();

                // create listofincluded if not already there
                if(listOfIncludedSpecies == null) {
                    listOfIncludedSpecies = new ListOfIncludedSpecies();
                    sbml.getModel().getAnnotation().getExtension().setListOfIncludedSpecies(listOfIncludedSpecies);
                }
                org.sbml._2001.ns.celldesigner.Species species = speciesW.getCDIncludedSpecies();
                listOfIncludedSpecies.getSpecies().add(species);
            }
            else {
                Species species = speciesW.getCDNormalSpecies();
                sbml.getModel().getListOfSpecies().getSpecies().add(species);
            }
        }

        for(Protein p: protMap.values()) {
            sbml.getModel().getAnnotation().getExtension().getListOfProteins().getProtein().add(p);
        }
        for(Gene p: geneMap.values()) {
            sbml.getModel().getAnnotation().getExtension().getListOfGenes().getGene().add(p);
        }
        for(RNA p: rnaMap.values()) {
            sbml.getModel().getAnnotation().getExtension().getListOfRNAs().getRNA().add(p);
        }
        for(AntisenseRNA p: asrnaMap.values()) {
            sbml.getModel().getAnnotation().getExtension().getListOfAntisenseRNAs().getAntisenseRNA().add(p);
        }
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
        List<ReactantWrapper> modificationsW = new ArrayList<>();
        List<Glyph> modificationsGlyphs = new ArrayList<>();
        List<Arc> modificationsArcs = new ArrayList<>();
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

        i = 0;
        for(Arc arc: modifiers) {
            Glyph g = arcToSource.get(arc.getId());
            AliasWrapper aliasW = aliasWrapperMap.get(g.getId()+"_alias1");
            System.out.println("Modification: "+g.getId()+" "+g.getClazz());

            ReactantWrapper modifWrapper = new ReactantWrapper(aliasW, ReactantType.MODIFICATION);
            //baseWrapper.setAnchorPoint(AnchorPoint.CENTER); // set to CENTER for now, but better computed after

            modifWrapper.setModificationLinkType(
                    ReactantWrapper.ModificationLinkType.valueOf(
                            LinkModel.getCdClass(
                                    ArcClazz.fromClazz(arc.getClazz()))));
            modificationsW.add(modifWrapper);
            modificationsGlyphs.add(g);
            modificationsArcs.add(arc);

            i++;
        }

        ReactionWrapper reactionW = new ReactionWrapper(processGlyph.getId().replaceAll("-","_"),
                reactionCDClass, baseReactantsW, baseProductsW);
        reactionW.setReversible(isReversible);
        Line2D.Float processLine;

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
            }
            else {
                processLine = new Line2D.Float(
                        absAssocPoint,
                        finalEndPoint
                );
            }

            // finally set up the xml elements and add to reactions
            int num0 = localEditPoints0.size();
            int num1 = localEditPoints1.size();
            int num2 = localEditPoints2.size();
            List<Integer> segmentCountList = Arrays.asList(num0+1, num1+1, num2+1);

            ConnectScheme connectScheme = getBranchConnectScheme(segmentCountList);

            String lineColor = "ff000000";
            float lineWidth = 1;
            /*
                Set a style only if all components' style are the same
             */
            if(mapHasStyle) {
                StyleInfo baseReactantArcStyle1 = styleMap.get(baseReactantArcs.get(0).getId());
                StyleInfo baseReactantArcStyle2 = styleMap.get(baseReactantArcs.get(1).getId());
                StyleInfo baseProductArcStyle = styleMap.get(baseProductArcs.get(0).getId());
                StyleInfo processGlyphStyle = styleMap.get(processGlyph.getId());
                if(baseReactantArcStyle1.getLineWidth() == baseReactantArcStyle2.getLineWidth()
                        && baseReactantArcStyle1.getLineWidth() == processGlyphStyle.getLineWidth()
                        && baseReactantArcStyle1.getLineWidth() == baseProductArcStyle.getLineWidth()

                        && baseReactantArcStyle1.getLineColor().equals(baseReactantArcStyle2.getLineColor())
                        && baseReactantArcStyle1.getLineColor().equals(baseProductArcStyle.getLineColor())
                        && baseReactantArcStyle1.getLineColor().equals(processGlyphStyle.getLineColor())) {
                    lineWidth = baseReactantArcStyle1.getLineWidth();
                    lineColor = baseReactantArcStyle1.getLineColor();
                }
            }

            Line line = new Line();
            line.setWidth(BigDecimal.valueOf(lineWidth));
            line.setColor(lineColor);

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
            }
            else {
                processLine = new Line2D.Float(
                        finalStartPoint,
                        absDissocPoint
                );
            }


            // finally set up the xml elements and add to reactions
            int num0 = localEditPoints0.size();
            int num1 = localEditPoints1.size();
            int num2 = localEditPoints2.size();
            List<Integer> segmentCountList = Arrays.asList(num0+1, num1+1, num2+1);

            ConnectScheme connectScheme = getBranchConnectScheme(segmentCountList);

            String lineColor = "ff000000";
            float lineWidth = 1;
            /*
                Set a style only if all components' style are the same
             */
            if(mapHasStyle) {
                StyleInfo baseReactantArcStyle = styleMap.get(baseReactantArcs.get(0).getId());
                StyleInfo baseProductArcStyle1 = styleMap.get(baseProductArcs.get(0).getId());
                StyleInfo baseProductArcStyle2 = styleMap.get(baseProductArcs.get(1).getId());
                StyleInfo processGlyphStyle = styleMap.get(processGlyph.getId());
                if(baseReactantArcStyle.getLineWidth() == baseProductArcStyle1.getLineWidth()
                        && baseProductArcStyle1.getLineWidth() == processGlyphStyle.getLineWidth()
                        && baseProductArcStyle1.getLineWidth() == baseProductArcStyle2.getLineWidth()

                        && baseReactantArcStyle.getLineColor().equals(baseProductArcStyle1.getLineColor())
                        && baseReactantArcStyle.getLineColor().equals(baseProductArcStyle2.getLineColor())
                        && baseReactantArcStyle.getLineColor().equals(processGlyphStyle.getLineColor())) {
                    lineWidth = baseReactantArcStyle.getLineWidth();
                    lineColor = baseReactantArcStyle.getLineColor();
                }
            }

            Line line = new Line();
            line.setWidth(BigDecimal.valueOf(lineWidth));
            line.setColor(lineColor);

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
            }
            else {
                processLine = new Line2D.Float(
                        finalStartPoint,
                        finalEndPoint
                );
            }


            /*
                Finally build the appropriate xml elements and pass it to the reaction
             */

            int segmentCount = completeLinkPoints.size() - 1;
            int processSegmentIndex = reactantPoints.size() - 1;

            ConnectScheme connectScheme = getSimpleConnectScheme(segmentCount, processSegmentIndex);

            String lineColor = "ff000000";
            float lineWidth = 1;
            /*
                Set a style only if all components' style are the same
             */
            if(mapHasStyle) {
                StyleInfo baseReactantArcStyle = styleMap.get(baseReactantArcs.get(0).getId());
                StyleInfo baseProductArcStyle = styleMap.get(baseProductArcs.get(0).getId());
                StyleInfo processGlyphStyle = styleMap.get(processGlyph.getId());
                if(baseReactantArcStyle.getLineWidth() == baseProductArcStyle.getLineWidth()
                        && baseProductArcStyle.getLineWidth() == processGlyphStyle.getLineWidth()

                        && baseReactantArcStyle.getLineColor().equals(baseProductArcStyle.getLineColor())
                        && baseReactantArcStyle.getLineColor().equals(processGlyphStyle.getLineColor())) {
                    lineWidth = baseReactantArcStyle.getLineWidth();
                    lineColor = baseReactantArcStyle.getLineColor();
                }
            }


            Line line = new Line();
            line.setWidth(BigDecimal.valueOf(lineWidth));
            line.setColor(lineColor);

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
            if(processGlyph.getPort().size() > 0) {
                additionalReactPoints.add(new Point2D.Float(
                        processGlyph.getBbox().getX(),
                        processGlyph.getBbox().getY())
                );
            }
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

            String lineColor = "ff000000";
            float lineWidth = 1;
            if(mapHasStyle) {
                System.out.println(additionalArc.getId());
                StyleInfo styleInfo = styleMap.get(additionalArc.getId());
                lineWidth = styleInfo.getLineWidth();
                lineColor = styleInfo.getLineColor();
            }

            int segmentCount = localEditPoints.size() + 1;

            ConnectScheme connectScheme = getSimpleConnectScheme(segmentCount, -1);

            LineType2 line = new LineType2();
            line.setWidth(BigDecimal.valueOf(lineWidth));
            line.setColor(lineColor);
            line.setType("Straight");

            List<String> editPointString = new ArrayList<>();
            for(Point2D.Float p: localEditPoints) {
                editPointString.add(p.getX()+","+p.getY());
            }

            LineWrapper lineWrapper = new LineWrapper(connectScheme, editPointString, line);

            additionalW.setLineWrapper(lineWrapper);
            reactionW.getAdditionalReactants().add(additionalW);

        }


        for(i=0; i < additionalProductArcs.size(); i++) {
            Arc additionalArc = additionalProductArcs.get(i);
            ReactantWrapper additionalW = additionalProductsW.get(i);
            Glyph additionalGlyph = additionalProductGlyphs.get(i);

            additionalW.setTargetLineIndex("-1,1");

            List<Point2D.Float> additionalProdPoints = new ArrayList<>();
            if(processGlyph.getPort().size() > 0) {
                additionalProdPoints.add(new Point2D.Float(
                        processGlyph.getBbox().getX(),
                        processGlyph.getBbox().getY())
                );
            }
            additionalProdPoints.addAll(SBGNUtils.getPoints(additionalArc));

            // gather only edit points, as they are the one who will undergo transformations into local
            // coordinate system
            List<Point2D.Float> editPointsOnly;
            if(additionalProdPoints.size() > 2) {
                editPointsOnly = additionalProdPoints.subList(1, additionalProdPoints.size() - 1);
            }
            else {
                editPointsOnly = new ArrayList<>();
            }
            Point2D.Float endPoint = additionalProdPoints.get(additionalProdPoints.size() - 1);

            // infer best anchorpoints possible
            AnchorPoint endAnchor = inferAnchorPoint(endPoint, additionalW,
                    SBGNUtils.getRectangleFromGlyph(additionalGlyph));
            additionalW.setAnchorPoint(endAnchor);

            Point2D.Float finalEndPoint = getFinalpoint(
                    additionalW.getAnchorPoint(),
                    additionalW,
                    SBGNUtils.getRectangleFromGlyph(additionalGlyph));

            // infer coordinates for the process' anchor point 0
            Point2D.Float anchor1 = pr.getAbsoluteAnchorCoords(1);

            List<Point2D.Float> localEditPoints = GeometryUtils.convertPoints(
                    editPointsOnly,
                    GeometryUtils.getTransformsToLocalCoords(
                            anchor1,
                            finalEndPoint
                    ));
            System.out.println("Local edit points "+localEditPoints);

            int segmentCount = localEditPoints.size() + 1;

            ConnectScheme connectScheme = getSimpleConnectScheme(segmentCount, -1);

            String lineColor = "ff000000";
            float lineWidth = 1;
            if(mapHasStyle) {
                System.out.println(additionalArc.getId());
                StyleInfo styleInfo = styleMap.get(additionalArc.getId());
                lineWidth = styleInfo.getLineWidth();
                lineColor = styleInfo.getLineColor();
            }

            LineType2 line = new LineType2();
            line.setWidth(BigDecimal.valueOf(lineWidth));
            line.setColor(lineColor);
            line.setType("Straight");

            List<String> editPointString = new ArrayList<>();
            for(Point2D.Float p: localEditPoints) {
                editPointString.add(p.getX()+","+p.getY());
            }

            LineWrapper lineWrapper = new LineWrapper(connectScheme, editPointString, line);

            additionalW.setLineWrapper(lineWrapper);
            reactionW.getAdditionalProducts().add(additionalW);

        }

        for(i=0; i < modificationsArcs.size(); i++) {
            Arc modificationArc = modificationsArcs.get(i);
            ReactantWrapper modificationW = modificationsW.get(i);
            Glyph modificationGlyph = modificationsGlyphs.get(i);

            /*
                If logic gate encountered, get all direct links to it. Exclude other logic gates as a complete
                tree of logic gates is impossible in CellDesigner.
                So get only direct links to EPNs and connect them to newly created logicWrapper.
             */
            if(SBGNUtils.isLogicGate(modificationGlyph)) {
                System.out.println("PROCESS LOGIC GATE");
                System.out.println(modificationW.getModificationLinkType());

                List<Arc> arcsConnectedToLogic = glyphToArc.get(modificationGlyph.getId());
                System.out.println("Arcs connected to logic gate: "+arcsConnectedToLogic.size());

                List<ReactantWrapper> connectedReactantsW = new ArrayList<>();
                List<String> logicModifiers = new ArrayList<>();
                List<String> logicAliases = new ArrayList<>();
                for(Arc logicArc: arcsConnectedToLogic) {
                    Glyph sourceGlyhp = arcToSource.get(logicArc.getId());
                    System.out.println("Glyph "+sourceGlyhp.getClazz()+" connected to logic");
                    // discard other connected logic gates, and the arc coming from the current gate itself
                    if(SBGNUtils.isLogicGate(sourceGlyhp)) {
                        System.out.println("skip logic gate");
                        continue;
                    }

                    AliasWrapper aliasW = aliasWrapperMap.get(sourceGlyhp.getId()+"_alias1");
                    System.out.println("modif aliasw "+aliasW);
                    System.out.println("Modification: "+sourceGlyhp.getId()+" "+sourceGlyhp.getClazz());
                    System.out.println("isaliasincluded ? "+aliasW.getSpeciesW().isIncludedSpecies());

                    ReactantWrapper modifWrapper = new ReactantWrapper(aliasW, ReactantType.MODIFICATION);

                    // modifications linked to logic gates inherit their link type
                    modifWrapper.setModificationLinkType(
                            ReactantWrapper.ModificationLinkType.valueOf(
                                    LinkModel.getCdClass(
                                            ArcClazz.fromClazz(modificationArc.getClazz()))));

                    ReactantWrapper processedLogicModifW = processModifierToLogic(logicArc, sourceGlyhp,
                            modifWrapper, modificationGlyph);

                    // we can't directly add the connected reactants, need the logic gate first.
                    //reactionW.getModifiers().add(processedLogicModifW);
                    connectedReactantsW.add(processedLogicModifW);
                    logicModifiers.add(processedLogicModifW.getAliasW().getSpeciesW().getId());
                    logicAliases.add(processedLogicModifW.getAliasW().getId());
                }

                // process the logic gate itself
                ReactantWrapper processedlogicW = processLogicGate(modificationArc,
                        modificationGlyph, modificationW, pr);

                LogicGateWrapper finalLogicW = new LogicGateWrapper(
                        processedlogicW,
                        LogicGateWrapper.LogicGateType.valueOf(modificationGlyph.getClazz().toUpperCase()),
                        logicModifiers,
                        logicAliases,
                        processedlogicW.getModificationLinkType()
                );

                /*SpeciesWrapper logicSpW = new SpeciesWrapper(modificationGlyph.getId(),
                        modificationGlyph.getId(), null);
                AliasWrapper logicAliasW = new AliasWrapper(modificationGlyph.getId()+"_alias1",
                        AliasWrapper.AliasType.BASIC, logicSpW);
                finalLogicW.setAliasW(logicAliasW);*/
                finalLogicW.setAnchorPoint(AnchorPoint.CENTER);

                System.out.println("Test presence of aliasw "+finalLogicW.getAliasW());
                System.out.println(finalLogicW.getModificationType()+" "+finalLogicW.getModificationLinkType());

                reactionW.getModifiers().add(finalLogicW);
                for(ReactantWrapper connectedToLogic: connectedReactantsW) {
                    reactionW.getModifiers().add(connectedToLogic);
                }


            }
            // non logic gates modifiers
            else {
                ReactantWrapper processedModifW = processModifier(modificationArc,
                        modificationGlyph, modificationW, pr);
                System.out.println("add modifier -> "+processedModifW);
                reactionW.getModifiers().add(processedModifW);
            }

            // get all speciesReference and add the reaction to those species as catalyzedReaction
            for(ModifierSpeciesReference speciesReference:
                    reactionW.getCDReaction().getListOfModifiers().getModifierSpeciesReference()) {
                String speciesId = speciesReference.getSpecies();
                SpeciesWrapper speciesW = speciesWrapperMap.get(speciesId);

                System.out.println("ADD CATALYYYYZED");
                speciesW.getCatalyzedReactions().add(reactionW.getId());
            }

        }

        sbml.getModel().getListOfReactions().getReaction().add(reactionW.getCDReaction());

    }

    private void processOrphanArc(Arc orphanArc) {
        // process orphan arcs

        System.out.println(orphanArc.getClazz());

        Glyph targetGlyph = arcToTarget.get(orphanArc.getId());
        Glyph sourceGlyph = arcToSource.get(orphanArc.getId());

        AliasWrapper sourceAliasW = aliasWrapperMap.get(sourceGlyph.getId()+"_alias1");
        AliasWrapper targetAliasW = aliasWrapperMap.get(targetGlyph.getId()+"_alias1");

        // case where one of the glyphs could not be translated (ex: submaps)
        if(sourceAliasW == null || targetAliasW == null) {
            logger.warn("Discarding arc because its source or target could not be translated"); // TODO more details
            return;
        }

        ReactantWrapper sourceWrapper = new ReactantWrapper(sourceAliasW, ReactantType.BASE_REACTANT);
        ReactantWrapper targetWrapper = new ReactantWrapper(targetAliasW, ReactantType.BASE_PRODUCT);

        List<Point2D.Float> arcPoints = SBGNUtils.getPoints(orphanArc);

        // gather only edit points, as they are the one who will undergo transformations into local
        // coordinate system
        List<Point2D.Float> editPointsOnly;
        if(arcPoints.size() > 2) {
            editPointsOnly = arcPoints.subList(1, arcPoints.size() - 1);
        }
        else {
            editPointsOnly = new ArrayList<>();
        }
        Point2D.Float startPoint = arcPoints.get(0);
        Point2D.Float endPoint = arcPoints.get(arcPoints.size() - 1);

        // infer best anchorpoints possible
        AnchorPoint startAnchor = inferAnchorPoint(startPoint, sourceWrapper,
                SBGNUtils.getRectangleFromGlyph(sourceGlyph));
        sourceWrapper.setAnchorPoint(startAnchor);

        AnchorPoint endAnchor = inferAnchorPoint(endPoint, targetWrapper,
                SBGNUtils.getRectangleFromGlyph(targetGlyph));
        targetWrapper.setAnchorPoint(endAnchor);

        Point2D.Float finalStartPoint = getFinalpoint(
                sourceWrapper.getAnchorPoint(),
                sourceWrapper,
                SBGNUtils.getRectangleFromGlyph(sourceGlyph));
        Point2D.Float finalEndPoint = getFinalpoint(
                targetWrapper.getAnchorPoint(),
                targetWrapper,
                SBGNUtils.getRectangleFromGlyph(targetGlyph));


        List<Point2D.Float> localEditPoints = GeometryUtils.convertPoints(
                editPointsOnly,
                GeometryUtils.getTransformsToLocalCoords(
                        finalStartPoint,
                        finalEndPoint
                ));
        System.out.println("Local edit points "+localEditPoints);


        int segmentCount = localEditPoints.size() + 1;

        ConnectScheme connectScheme = getSimpleConnectScheme(segmentCount, -1);

        String lineColor = "ff000000";
        float lineWidth = 1;
        if(mapHasStyle) {
            System.out.println(orphanArc.getId());
            StyleInfo styleInfo = styleMap.get(orphanArc.getId());
            lineWidth = styleInfo.getLineWidth();
            lineColor = styleInfo.getLineColor();
        }

        LineType2 line = new LineType2();
        line.setWidth(BigDecimal.valueOf(lineWidth));
        line.setColor(lineColor);
        line.setType("Straight");

        List<String> editPointString = new ArrayList<>();
        for(Point2D.Float p: localEditPoints) {
            editPointString.add(p.getX()+","+p.getY());
        }

        LineWrapper lineWrapper = new LineWrapper(connectScheme, editPointString, line);

        ReactionWrapper reactionW = new ReactionWrapper(
                orphanArc.getId(),
                ReactionType.valueOf(LinkModel.getReducedCdClass(ArcClazz.fromClazz(orphanArc.getClazz()))),
                Collections.singletonList(sourceWrapper),
                Collections.singletonList(targetWrapper));

        reactionW.setLineWrapper(lineWrapper);
        reactionW.setHasProcess(false);
        System.out.println("Add direct link: "+reactionW.getId());

        sbml.getModel().getListOfReactions().getReaction().add(reactionW.getCDReaction());

        /*additionalW.setLineWrapper(lineWrapper);
        reactionW.getAdditionalProducts().add(additionalW);*/
    }

    private ReactantWrapper processModifierToLogic(Arc modificationArc, Glyph modificationGlyph,
                                            ReactantWrapper modificationW, Glyph logicGateGlyph) {
        List<Point2D.Float> modificationPoints = SBGNUtils.getPoints(modificationArc);
        if(logicGateGlyph.getPort().size() > 0) {
            modificationPoints.add(new Point2D.Float(
                    //modificationGlyph.getBbox().getX() - (float) mapBounds.getX() + modificationGlyph.getBbox().getW() / 2,
                    //modificationGlyph.getBbox().getY() - (float) mapBounds.getY() + modificationGlyph.getBbox().getH() / 2
                    logicGateGlyph.getBbox().getX() + logicGateGlyph.getBbox().getW() / 2,
                    logicGateGlyph.getBbox().getY() + logicGateGlyph.getBbox().getH() / 2
            ));
        }

        // gather only edit points, as they are the one who will undergo transformations into local
        // coordinate system
        List<Point2D.Float> editPointsOnly;
        if(modificationPoints.size() > 2) {
            editPointsOnly = modificationPoints.subList(1, modificationPoints.size() - 1);
        }
        else {
            editPointsOnly = new ArrayList<>();
        }
        Point2D.Float startPoint = modificationPoints.get(0);
        Point2D.Float endPoint = modificationPoints.get(modificationPoints.size() - 1);

        // infer best anchorpoints possible
        System.out.println(modificationArc.getId());
        if(!SBGNUtils.isLogicGate(modificationGlyph)) {
            AnchorPoint startAnchor = inferAnchorPoint(startPoint, modificationW,
                    SBGNUtils.getRectangleFromGlyph(modificationGlyph));
            modificationW.setAnchorPoint(startAnchor);
        }

        Point2D.Float finalEndPoint = endPoint;

        modificationW.setTargetLineIndex("-1,0");

        Point2D.Float finalStartPoint = getFinalpoint(
                modificationW.getAnchorPoint(),
                modificationW,
                SBGNUtils.getRectangleFromGlyph(modificationGlyph));


        List<Point2D.Float> localEditPoints = GeometryUtils.convertPoints(
                editPointsOnly,
                GeometryUtils.getTransformsToLocalCoords(
                        finalStartPoint,
                        finalEndPoint
                ));
        System.out.println("Local edit points "+localEditPoints);


        int segmentCount = localEditPoints.size() + 1;

        ConnectScheme connectScheme = getSimpleConnectScheme(segmentCount, -1);

        String lineColor = "ff000000";
        float lineWidth = 1;
        if(mapHasStyle) {
            StyleInfo styleInfo = styleMap.get(modificationArc.getId());
            lineWidth = styleInfo.getLineWidth();
            lineColor = styleInfo.getLineColor();
        }

        LineType2 line = new LineType2();
        line.setWidth(BigDecimal.valueOf(lineWidth));
        line.setColor(lineColor);
        line.setType("Straight");

        List<String> editPointString = new ArrayList<>();
        for(Point2D.Float p: localEditPoints) {
            editPointString.add(p.getX()+","+p.getY());
        }

        LineWrapper lineWrapper = new LineWrapper(connectScheme, editPointString, line);

        modificationW.setLineWrapper(lineWrapper);
        return modificationW;
    }

    private ReactantWrapper processLogicGate(Arc modificationArc, Glyph modificationGlyph,
                                            ReactantWrapper modificationW, Process pr) {

        List<Point2D.Float> modificationPoints = new ArrayList<>();
        if(modificationGlyph.getPort().size() > 0) {
            // join the arc coming out of the port to the center of the logic gate
            modificationPoints.add(new Point2D.Float(
                    //modificationGlyph.getBbox().getX() - (float) mapBounds.getX() + modificationGlyph.getBbox().getW() / 2,
                    //modificationGlyph.getBbox().getY() - (float) mapBounds.getY() + modificationGlyph.getBbox().getH() / 2
                    modificationGlyph.getBbox().getX() + modificationGlyph.getBbox().getW() / 2,
                    modificationGlyph.getBbox().getY() + modificationGlyph.getBbox().getH() / 2
            ));
        }
        modificationPoints.addAll(SBGNUtils.getPoints(modificationArc));

        // gather only edit points, as they are the one who will undergo transformations into local
        // coordinate system
        List<Point2D.Float> editPointsOnly;
        if(modificationPoints.size() > 2) {
            editPointsOnly = modificationPoints.subList(1, modificationPoints.size() - 1);
        }
        else {
            editPointsOnly = new ArrayList<>();
        }
        Point2D.Float startPoint = modificationPoints.get(0);
        Point2D.Float endPoint = modificationPoints.get(modificationPoints.size() - 1);

        // infer best anchorpoints possible
        System.out.println(modificationArc.getId());

        int processAnchor = inferProcessAnchorPoint(endPoint, pr);
        modificationW.setTargetLineIndex("-1," + processAnchor);

        Point2D.Float finalEndPoint = pr.getAbsoluteAnchorCoords(processAnchor);
        System.out.println("process coords: "+pr.getGlyph().getCenter());


        // for logic gates, just take the center of the glyph
        Point2D.Float finalStartPoint = startPoint;
        /*
            here we work with a process that is already placed on final coordinates (translated with mapbounds)
            so we need to harmonize everything, including the coordinates of edit points
         */
        /*List<Point2D.Float> newEditPointsOnly = new ArrayList<>();
        for(Point2D.Float p: editPointsOnly) {
            newEditPointsOnly.add(new Point2D.Float(
                    (float) (p.getX() - mapBounds.getX()),
                    (float) (p.getY() - mapBounds.getY())
            ));
        }
        editPointsOnly = newEditPointsOnly;*/

        System.out.println("LOGIIIIC: "+ finalStartPoint+" "+editPointsOnly+" "+finalEndPoint);
        System.out.println(modificationPoints.size()+" "+modificationPoints);



        List<Point2D.Float> localEditPoints = GeometryUtils.convertPoints(
                editPointsOnly,
                GeometryUtils.getTransformsToLocalCoords(
                        finalStartPoint,
                        finalEndPoint
                ));
        System.out.println("Local edit points "+localEditPoints);


        int segmentCount = localEditPoints.size() + 1;

        ConnectScheme connectScheme = getSimpleConnectScheme(segmentCount, -1);

        String lineColor = "ff000000";
        float lineWidth = 1;
        if(mapHasStyle) {
            StyleInfo styleInfo = styleMap.get(modificationArc.getId());
            lineWidth = styleInfo.getLineWidth();
            lineColor = styleInfo.getLineColor();
        }

        LineType2 line = new LineType2();
        line.setWidth(BigDecimal.valueOf(lineWidth));
        line.setColor(lineColor);
        line.setType("Straight");

        List<String> editPointString = new ArrayList<>();
        for(Point2D.Float p: localEditPoints) {
            editPointString.add(p.getX()+","+p.getY());
        }

        // logic gates have their own coordinate added to the edit point, in global coord system
        // we need to adjust to map translation factor
        editPointString.add(
                (finalStartPoint.getX() - mapBounds.getX())
                        +","+
                        (finalStartPoint.getY() - mapBounds.getY()));

        LineWrapper lineWrapper = new LineWrapper(connectScheme, editPointString, line);

        modificationW.setLineWrapper(lineWrapper);
        return modificationW;
    }

    /**
     * Pass a Point2D for logic gates, process otherwise
     * @param modificationArc
     * @param modificationGlyph
     * @param modificationW
     * @param prOrLogic
     * @return
     */
    private ReactantWrapper processModifier(Arc modificationArc, Glyph modificationGlyph,
                                 ReactantWrapper modificationW, Object prOrLogic) {
        List<Point2D.Float> modificationPoints = SBGNUtils.getPoints(modificationArc);

        // gather only edit points, as they are the one who will undergo transformations into local
        // coordinate system
        List<Point2D.Float> editPointsOnly;
        if(modificationPoints.size() > 2) {
            editPointsOnly = modificationPoints.subList(1, modificationPoints.size() - 1);
        }
        else {
            editPointsOnly = new ArrayList<>();
        }
        Point2D.Float startPoint = modificationPoints.get(0);
        Point2D.Float endPoint = modificationPoints.get(modificationPoints.size() - 1);

        // infer best anchorpoints possible
        System.out.println(modificationArc.getId());
        if(!SBGNUtils.isLogicGate(modificationGlyph)) {
            AnchorPoint startAnchor = inferAnchorPoint(startPoint, modificationW,
                    SBGNUtils.getRectangleFromGlyph(modificationGlyph));
            modificationW.setAnchorPoint(startAnchor);
        }

        Point2D.Float finalEndPoint = null;
        if(prOrLogic instanceof Process) {
            Process pr = (Process) prOrLogic;
            int processAnchor = inferProcessAnchorPoint(endPoint, pr);
            modificationW.setTargetLineIndex("-1," + processAnchor);

            finalEndPoint = pr.getAbsoluteAnchorCoords(processAnchor);
            System.out.println("process coords: "+pr.getGlyph().getCenter());
        }
        else if(prOrLogic instanceof Point2D) {
            finalEndPoint = (Point2D.Float) prOrLogic;
            modificationW.setTargetLineIndex("-1,0");
        }

        Point2D.Float finalStartPoint  = null;
        if(SBGNUtils.isLogicGate(modificationGlyph)) {
            // for logic gates, just take the center of the glyph
            finalStartPoint = new Point2D.Float(
                    (float) (modificationGlyph.getBbox().getX() - mapBounds.getX() + modificationGlyph.getBbox().getW() / 2),
                    (float) (modificationGlyph.getBbox().getY() - mapBounds.getY() + modificationGlyph.getBbox().getH() / 2)
            );
            /*
                here we work with a process that is already placed on final coordinates (translated with mapbounds)
                so we need to harmonize everything, including the coordinates of edit points
             */
            /*List<Point2D.Float> newEditPointsOnly = new ArrayList<>();
            for(Point2D.Float p: editPointsOnly) {
                newEditPointsOnly.add(new Point2D.Float(
                        (float) (p.getX() - mapBounds.getX()),
                        (float) (p.getY() - mapBounds.getY())
                ));
            }
            editPointsOnly = newEditPointsOnly;*/

            System.out.println("LOGIIIIC: "+ finalStartPoint+" "+editPointsOnly+" "+finalEndPoint);
            System.out.println(modificationPoints.size()+" "+modificationPoints);
        }
        else {
            finalStartPoint = getFinalpoint(
                    modificationW.getAnchorPoint(),
                    modificationW,
                    SBGNUtils.getRectangleFromGlyph(modificationGlyph));
        }


        List<Point2D.Float> localEditPoints = GeometryUtils.convertPoints(
                editPointsOnly,
                GeometryUtils.getTransformsToLocalCoords(
                        finalStartPoint,
                        finalEndPoint
                ));
        System.out.println("Local edit points "+localEditPoints);


        int segmentCount = localEditPoints.size() + 1;

        ConnectScheme connectScheme = getSimpleConnectScheme(segmentCount, -1);

        String lineColor = "ff000000";
        float lineWidth = 1;
        if(mapHasStyle) {
            StyleInfo styleInfo = styleMap.get(modificationArc.getId());
            lineWidth = styleInfo.getLineWidth();
            lineColor = styleInfo.getLineColor();
        }

        LineType2 line = new LineType2();
        line.setWidth(BigDecimal.valueOf(lineWidth));
        line.setColor(lineColor);
        line.setType("Straight");

        List<String> editPointString = new ArrayList<>();
        for(Point2D.Float p: localEditPoints) {
            editPointString.add(p.getX()+","+p.getY());
        }

        // logic gates have their own coordinate added to the edit point, in global coord system
        if(SBGNUtils.isLogicGate(modificationGlyph)) {
            editPointString.add(finalStartPoint.getX()+","+finalStartPoint.getY());
        }

        LineWrapper lineWrapper = new LineWrapper(connectScheme, editPointString, line);

        modificationW.setLineWrapper(lineWrapper);
        return modificationW;
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
        label = Utils.UTF8charsToCD(label);

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

        // process state vars
        List<ResidueWrapper> residueList = new ArrayList<>();
        int i=0;
        for(Glyph stateVar: SBGNUtils.getStateVariables(glyph)) {
            double angle = GeometryUtils.getAngleOfAuxUnit(
                    SBGNUtils.getRectangleFromGlyph(glyph),
                    SBGNUtils.getRectangleFromGlyph(stateVar));
            double topRatio = GeometryUtils.getTopRatioOfAuxUnit(
                    SBGNUtils.getRectangleFromGlyph(glyph),
                    SBGNUtils.getRectangleFromGlyph(stateVar));

            System.out.println("State var: "+angle+" "+stateVar.getState().getVariable()+" "+
                    stateVar.getState().getValue()+" "+topRatio);

            String variable = stateVar.getState().getVariable();
            String value = stateVar.getState().getValue();

            ResidueWrapper resW = new ResidueWrapper("rs"+i);
            //resW.useAngle = true;
            resW.name = variable;
            resW.state = ResidueWrapper.getLongState(value);
            resW.angle = (float) angle;
            resW.relativePos = (float) topRatio;
            residueList.add(resW);

            i++;

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

                    if(residueList.size() > 0) {
                        ListOfModificationResidues listOfModificationResidues = new ListOfModificationResidues();
                        prot.setListOfModificationResidues(listOfModificationResidues);

                        for(ResidueWrapper resW: residueList) {
                            ModificationResidue modificationResidue = new ModificationResidue();
                            listOfModificationResidues.getModificationResidue().add(modificationResidue);

                            modificationResidue.setSide("none");
                            modificationResidue.setAngle(BigDecimal.valueOf(resW.angle));
                            modificationResidue.setId(resW.id);
                            if(resW.name != null && !resW.name.isEmpty()) {
                                modificationResidue.setName(resW.name);
                            }
                        }
                    }

                    protMap.put(referenceId, prot);
                    //sbml.getModel().getAnnotation().getExtension().getListOfProteins().getProtein().add(prot);
                    break;
                case GENE:
                    Gene gene = new Gene();
                    referenceId = "gene_"+glyph.getId();
                    gene.setId(referenceId);
                    gene.setName(label);
                    gene.setType("GENE");

                    if(residueList.size() > 0) {
                        ListOfRegions listOfRegions = new ListOfRegions();
                        gene.setListOfRegions(listOfRegions);

                        for(ResidueWrapper resW: residueList) {
                            ListOfRegions.Region region = new ListOfRegions.Region();
                            listOfRegions.getRegion().add(region);

                            region.setId(resW.id);
                            region.setActive(false);
                            region.setSize(BigDecimal.valueOf(0));
                            region.setType("Modification Site");
                            region.setPos(BigDecimal.valueOf(resW.relativePos));

                            if(resW.name != null && !resW.name.isEmpty()) {
                                region.setName(resW.name);
                            }
                        }
                    }

                    geneMap.put(referenceId, gene);
                    //sbml.getModel().getAnnotation().getExtension().getListOfGenes().getGene().add(gene);
                    break;
                case RNA:
                    RNA rna = new RNA();
                    referenceId = "rna_"+glyph.getId();
                    rna.setId(referenceId);
                    rna.setName(label);
                    rna.setType("RNA");

                    if(residueList.size() > 0) {
                        ListOfRegions listOfRegions = new ListOfRegions();
                        rna.setListOfRegions(listOfRegions);

                        for(ResidueWrapper resW: residueList) {
                            ListOfRegions.Region region = new ListOfRegions.Region();
                            listOfRegions.getRegion().add(region);

                            region.setId(resW.id);
                            // region.setActive(false); // <--- same as genes except no activity
                            region.setSize(BigDecimal.valueOf(0));
                            region.setType("Modification Site");
                            region.setPos(BigDecimal.valueOf(resW.relativePos));

                            if(resW.name != null && !resW.name.isEmpty()) {
                                region.setName(resW.name);
                            }
                        }
                    }


                    rnaMap.put(referenceId, rna);
                    //sbml.getModel().getAnnotation().getExtension().getListOfRNAs().getRNA().add(rna);
                    break;
                case ANTISENSE_RNA:
                    AntisenseRNA asrna = new AntisenseRNA();
                    referenceId = "asrna_"+glyph.getId();
                    asrna.setId(referenceId);
                    asrna.setName(label);
                    asrna.setType("ANTISENSE_RNA");

                    if(residueList.size() > 0) {
                        ListOfRegions listOfRegions = new ListOfRegions();
                        asrna.setListOfRegions(listOfRegions);

                        for(ResidueWrapper resW: residueList) {
                            ListOfRegions.Region region = new ListOfRegions.Region();
                            listOfRegions.getRegion().add(region);

                            region.setId(resW.id);
                            // region.setActive(false); // <--- same as genes except no activity
                            region.setSize(BigDecimal.valueOf(0));
                            region.setType("Modification Site");
                            region.setPos(BigDecimal.valueOf(resW.relativePos));

                            if(resW.name != null && !resW.name.isEmpty()) {
                                region.setName(resW.name);
                            }
                        }
                    }

                    asrnaMap.put(referenceId, asrna);
                    //sbml.getModel().getAnnotation().getExtension().getListOfAntisenseRNAs().getAntisenseRNA().add(asrna);
                    break;
            }
        }

        // create the species + alias couple
        SpeciesWrapper speciesW;
        String aliasId = glyph.getId()+"_alias1";
        AliasWrapper aliasW;
        if(isComplex) {
            speciesW = new SpeciesWrapper(glyph.getId(), label, null, referenceId);
            aliasW = new AliasWrapper(aliasId, AliasWrapper.AliasType.COMPLEX, speciesW);
        }
        else {
            speciesW = new SpeciesWrapper(glyph.getId(), label, subType, referenceId);
            aliasW = new AliasWrapper(aliasId, AliasWrapper.AliasType.BASIC, speciesW);
        }
        speciesW.getAliases().add(aliasW);

        // find and set the toplevel complex parent
        if(isIncluded) {
            AliasWrapper parentComplexAliasW = aliasWrapperMap.get(parentAliasId);
            // we need to go up the chain in case of multiple inclusion levels
            while(parentComplexAliasW.getTopLevelParent() != null) {
                parentComplexAliasW = parentComplexAliasW.getTopLevelParent();
            }
            aliasW.setTopLevelParent(parentComplexAliasW);
            System.out.println("TOP level parent: "+parentComplexAliasW.getSpeciesW().getId()+" "+parentComplexAliasW.getId());
        }


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

        // multimer
        if(glyph.getClazz().endsWith("multimer")) {
            speciesW.setMultimer(2); // default to 2, if more information is present, count will be more precise
        }

        // we allow elements to not be multimer, but still have a N:\d unit of info
        int multimerCount = SBGNUtils.getMultimerFromInfo(glyph);
        if(multimerCount > 0) {
            speciesW.setMultimer(multimerCount);
        }

        // add state variables
        for(ResidueWrapper resW: residueList) {
            speciesW.getResidues().add(resW);
        }

        // add species to correct list
        if(isIncluded) {
            speciesW.setComplex(parentSpeciesId);
            speciesW.setIncludedSpecies(true);
            /*org.sbml._2001.ns.celldesigner.Species species = speciesW.getCDIncludedSpecies(referenceId);
            sbml.getModel().getAnnotation().getExtension().getListOfIncludedSpecies().getSpecies().add(species);*/
        }
        else {
            /*Species species = speciesW.getCDNormalSpecies(referenceId);
            sbml.getModel().getListOfSpecies().getSpecies().add(species);*/
        }
        speciesWrapperMap.put(speciesW.getId(), speciesW);

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
        if(mapHasStyle)
            aliasW.setStyleInfo(styleMap.get(glyph.getId()));
        else
            aliasW.setStyleInfo(new StyleInfo(aliasW.getId()));

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
        //ext.setListOfLayers(new ListOfLayers()); // TODO only when a layer is there
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
        speciesWrapperMap = new HashMap<>();
        glyphToArc = new HashMap<>();
        orphanArcs = new ArrayList<>();
        protMap = new HashMap<>();
        rnaMap = new HashMap<>();
        geneMap = new HashMap<>();
        asrnaMap = new HashMap<>();

        // parse all the style info
        styleMap = new HashMap<>();
        mapHasStyle = false;
        if(map.getExtension() != null) {
            for (Element e : map.getExtension().getAny()) {
                if (e.getTagName().equals("renderInformation")) {
                    styleMap = SBGNUtils.mapStyleinfo(e);
                    mapHasStyle = true;
                }
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
            glyphToArc.put(g.getId(), new ArrayList<>());
        }

        for(Arc arc: map.getArc()) {
            Glyph sourceGlyph;
            Glyph targetGlyph;
            boolean isConnectedToProcess = false;

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
                isConnectedToProcess = true;
            }
            if(processToArcs.containsKey(targetGlyph.getId())){
                processToArcs.get(targetGlyph.getId()).add(arc);
                isConnectedToProcess = true;
            }

            if(glyphToArc.containsKey(sourceGlyph.getId())){
                glyphToArc.get(sourceGlyph.getId()).add(arc);
            }
            if(glyphToArc.containsKey(targetGlyph.getId())){
                glyphToArc.get(targetGlyph.getId()).add(arc);
            }

            if(!SBGNUtils.isLogicGate(sourceGlyph)
                && !SBGNUtils.isLogicGate(targetGlyph)
                && !isConnectedToProcess) {
                orphanArcs.add(arc);
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

    // TODO the 6 process anchor coords could be cached
    public static int inferProcessAnchorPoint(Point2D.Float p, Process process) {
        double minDist = Double.MAX_VALUE;
        int result = -1;
        // process anchors indexes go from 2 to 7
        for(int i=2; i<8; i++) {
            Point2D.Float anchorCoord = process.getAbsoluteAnchorCoords(i);
            double distance = p.distance(anchorCoord);
            if(distance < minDist) {
                minDist = distance;
                result = i;
            }
        }

        return result;
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
