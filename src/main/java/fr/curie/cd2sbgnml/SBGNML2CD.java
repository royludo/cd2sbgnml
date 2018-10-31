package fr.curie.cd2sbgnml;

import fr.curie.cd2sbgnml.graphics.AnchorPoint;
import fr.curie.cd2sbgnml.graphics.CdShape;
import fr.curie.cd2sbgnml.graphics.GeometryUtils;
import fr.curie.cd2sbgnml.graphics.Link;
import fr.curie.cd2sbgnml.model.LinkModel;
import fr.curie.cd2sbgnml.model.LogicGate;
import fr.curie.cd2sbgnml.model.Process;
import fr.curie.cd2sbgnml.xmlcdwrappers.*;
import fr.curie.cd2sbgnml.model.ReactantModel;
import fr.curie.cd2sbgnml.xmlcdwrappers.ReactantWrapper.ReactantType;
import fr.curie.cd2sbgnml.xmlcdwrappers.ReactionType;
import org.sbfc.converter.GeneralConverter;
import org.sbfc.converter.exceptions.ConversionException;
import org.sbfc.converter.exceptions.ReadModelException;
import org.sbfc.converter.models.GeneralModel;
import org.sbgn.ArcClazz;
import org.sbgn.bindings.*;
import org.sbgn.GlyphClazz;
import org.sbgn.bindings.Map;
import org.sbml._2001.ns.celldesigner.*;
import org.sbml.sbml.level2.version4.*;
import org.sbml.sbml.level2.version4.OriginalModel.ListOfCompartments;
import org.sbml.sbml.level2.version4.OriginalModel.ListOfReactions;
import org.sbml.sbml.level2.version4.OriginalModel.ListOfSpecies;
import org.sbml.sbml.level2.version4.Species;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.AbstractMap.SimpleEntry;
import java.util.stream.Collectors;

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
     * HashSet containing all logic gate glyphs that aren't associated to any process.
     * The set is first built with all logic gates in the map, and they are removed as reactions are processed, if
     * they are connected to a process.
     * After all reactions are processed, only the orphan logic gates will remain in the set.
     */
    HashSet<Glyph> orphanLogicGates;

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
        Map sbgnMap = sbgn.getMap(); //.get(0);

        // init celldesigner file
        sbml = this.initFile(sbgnMap);

        // init the index maps
        this.buildMaps(sbgnMap);

        // put notes and annotations from map to model
        if(sbgnMap.getNotes() != null
                && sbgnMap.getNotes().getAny().size() > 0) {
            Element notesE = sbgnMap.getNotes().getAny().get(0);
            SBase.Notes notes = new SBase.Notes();
            notes.getAny().add(notesE);
            sbml.getModel().setNotes(notes);
        }

        if(sbgnMap.getExtension() != null) {
            for(Element e: sbgnMap.getExtension().getAny()){
                if(e.getTagName().equals("annotation")) {
                    Element rdf = SBGNUtils.sanitizeRdfURNs((Element) e.getElementsByTagName("rdf:RDF").item(0));
                    sbml.getModel().getAnnotation().getAny().add(rdf);
                }
            }
        }


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
                case SUBMAP:
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

        // process the remaining orphan logic gates
        for(Glyph orphanLogic: orphanLogicGates) {
            processLogicReaction(orphanLogic);
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

    /**
     * Logic gate reactions have different features than all other reactions in CellDesigner.
     * They can have more than 2 baseReactants. Instead of modifications, they have gateMembers listed.
     * @param logicGlyph
     */
    private void processLogicReaction(Glyph logicGlyph) {
        List<Arc> connectedArcs = glyphToArc.get(logicGlyph.getId());
        Point2D.Float logicCoords = new Point2D.Float(
                logicGlyph.getBbox().getX() + logicGlyph.getBbox().getW() / 2,
                logicGlyph.getBbox().getY() + logicGlyph.getBbox().getH() / 2
        );

        ReactionType reactionCDClass = ReactionType.BOOLEAN_LOGIC_GATE;

        List<List<Arc>> tmp = SBGNUtils.getBLGReactantTypes(connectedArcs);
        List<Arc> reactants = tmp.get(0);
        Arc productArc = tmp.get(1).get(0);

        List<ReactantWrapper> baseReactantsW = new ArrayList<>();
        List<Glyph> baseReactantGlyphs = new ArrayList<>();
        List<Arc> baseReactantArcs = new ArrayList<>();

        List<String> aliases = new ArrayList<>();
        List<String> speciesModifiers = new ArrayList<>();

        for(Arc arc: reactants) {
            Glyph g = arcToSource.get(arc.getId());

            AliasWrapper aliasW = aliasWrapperMap.get(g.getId()+"_alias1");

            ReactantWrapper baseWrapper = new ReactantWrapper(aliasW, ReactantType.BASE_REACTANT);
            //baseWrapper.setAnchorPoint(AnchorPoint.CENTER); // set to CENTER for now, but better computed after

            // incoming arcs have the type of the product arc
            baseWrapper.setModificationLinkType(
                    ModificationLinkType.valueOf(
                            LinkModel.getCdClass(
                                    ArcClazz.fromClazz(productArc.getClazz()))));

            baseReactantsW.add(baseWrapper);
            baseReactantGlyphs.add(g);
            baseReactantArcs.add(arc);
            aliases.add(aliasW.getId());
            speciesModifiers.add(aliasW.getSpeciesId());
        }

        Glyph baseProductGlyph = arcToTarget.get(productArc.getId());
        AliasWrapper aliasW = aliasWrapperMap.get(baseProductGlyph.getId()+"_alias1");
        ReactantWrapper baseProductW = new ReactantWrapper(aliasW, ReactantType.BASE_PRODUCT);
        baseProductW.setModificationLinkType(
                ModificationLinkType.valueOf(
                        LinkModel.getCdClass(
                                ArcClazz.fromClazz(productArc.getClazz()))));

        ReactionWrapper reactionW = new ReactionWrapper(logicGlyph.getId().replaceAll("-","_"),
                reactionCDClass, baseReactantsW, Collections.singletonList(baseProductW));

        // 1st product link = logic gate associated link
        SimpleEntry<Link, Link> tmpProductLink = baseLinkProcessingStep1(baseProductW,
                baseProductGlyph,
                productArc,
                logicCoords,
                false,
                new ReactionFeatures(false, true, false));
        List<Point2D.Float> localEditPointsProduct = tmpProductLink.getKey().getEditPoints();
        Point2D.Float finalEndPoint = tmpProductLink.getKey().getEnd();
        Link productLink = tmpProductLink.getValue();
        // apply translation factor on logic gate
        Point2D.Float logicPoint = new Point2D.Float(
                (float) (logicCoords.getX() - mapBounds.getX()),
                (float) (logicCoords.getY() - mapBounds.getY()));
        LineWrapper productLineWrapper = buildLineWrapper(productArc.getId(), localEditPointsProduct, logicPoint);

        LogicGateWrapper logicW = new LogicGateWrapper(
                baseProductW,
                LogicGate.getLogicGateType(GlyphClazz.fromClazz(logicGlyph.getClazz())),
                speciesModifiers,
                aliases,
                baseProductW.getModificationLinkType());
        logicW.setLineWrapper(productLineWrapper);
        logicW.setTargetLineIndex("-1,0");
        reactionW.getModifiers().add(logicW);


        int i=0;
        //java.util.Map<String, List<Point2D.Float>> arcsId2Editpoints = new LinkedHashMap<>();
        for(ReactantWrapper reactantW: baseReactantsW) {
            List<Point2D.Float> localEditPoints0 = baseLinkProcessingStep1(reactantW,
                    baseReactantGlyphs.get(i),
                    baseReactantArcs.get(i),
                    logicCoords,
                    true,
                    new ReactionFeatures(false, true, false, true)).getKey().getEditPoints();

            reactantW.setTargetLineIndex("-1,0");
            List<String> editPointStringList = new ArrayList<>();
            for(Point2D.Float p: localEditPoints0) {
                editPointStringList.add(p.getX()+","+p.getY());
            }

            LineWrapper lineWrapper = buildLineWrapper(baseReactantArcs.get(i).getId(),
                    localEditPoints0, null);
            reactantW.setLineWrapper(lineWrapper);


            //arcsId2Editpoints.put(baseReactantArcs.get(i).getId(), localEditPoints0);
            i++;
        }

        //arcsId2Editpoints.put(productArc.getId(), localEditPointsProduct);

        List<String> baseReactionEditPointString = new ArrayList<>();
        /*List<Point2D.Float> mergedList = new ArrayList<>();
        mergedList.addAll(productLink.getEditPoints());
        mergedList.add(logicCoords);*/
        for(Point2D.Float p: logicW.getLineWrapper().getEditPoints()) {
            baseReactionEditPointString.add(p.getX()+","+p.getY());
        }

        Line line = new Line();
        line.setWidth(BigDecimal.valueOf(1));
        line.setColor("ff000000");


        LineWrapper baseLineWrapper = new LineWrapper(null, baseReactionEditPointString, line);
        reactionW.setLineWrapper(baseLineWrapper);

        sbml.getModel().getListOfReactions().getReaction().add(reactionW.getCDReaction());


    }

    private void processReaction(Glyph processGlyph) {
        // TODO add arc notes and annotations to reaction
        List<Arc> connectedArcs = processToArcs.get(processGlyph.getId());
        Point2D.Float processCoords = new Point2D.Float(
                processGlyph.getBbox().getX(),
                processGlyph.getBbox().getY()
        );

        boolean isReversible = SBGNUtils.isReactionReversible(connectedArcs);
        ReactionType reactionCDClass = ReactionType.STATE_TRANSITION; // default to basic reaction type

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

            ReactantWrapper modifWrapper = new ReactantWrapper(aliasW, ReactantType.MODIFICATION);
            //baseWrapper.setAnchorPoint(AnchorPoint.CENTER); // set to CENTER for now, but better computed after

            modifWrapper.setModificationLinkType(
                    ModificationLinkType.valueOf(
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
            ReactionFeatures reactionFeatures = new ReactionFeatures(isReversible,true, true);

            ReactantWrapper baseReactantW0 = baseReactantsW.get(0);
            ReactantWrapper baseReactantW1 = baseReactantsW.get(1);
            ReactantWrapper baseProductW = baseProductsW.get(0);

            SimpleEntry<Point2D.Float, Point2D.Float> tmpResult = getAssocDissocPoints(
                    Arrays.asList(baseReactantW0, baseReactantW1, baseProductW),
                    processGlyph,
                    processCoords,
                    baseProductArcs.get(0),
                    true
            );
            Point2D.Float absAssocPoint = tmpResult.getKey();
            Point2D.Float localAssocPoint = tmpResult.getValue();


            List<Point2D.Float> localEditPoints0 = baseLinkProcessingStep1(
                    baseReactantW0, baseReactantGlyphs.get(0),
                    baseReactantArcs.get(0), absAssocPoint, true, reactionFeatures).getKey().getEditPoints();

            List<Point2D.Float> localEditPoints1 = baseLinkProcessingStep1(
                    baseReactantW1, baseReactantGlyphs.get(1),
                    baseReactantArcs.get(1), absAssocPoint, true, reactionFeatures).getKey().getEditPoints();

            SimpleEntry<Link, Link> tmpResultPoints = baseLinkProcessingStep1(
                    baseProductW, baseProductGlyphs.get(0),
                    baseProductArcs.get(0), absAssocPoint, false, reactionFeatures);
            List<Point2D.Float> localEditPoints2 = tmpResultPoints.getKey().getEditPoints();
            Point2D.Float finalEndPoint = tmpResultPoints.getKey().getEnd();
            Link productLink = tmpResultPoints.getValue();

            processLine = getProcessLine(productLink, absAssocPoint, reactionFeatures);

            java.util.Map<String, List<Point2D.Float>> arcsId2Editpoints = new LinkedHashMap<>();
            arcsId2Editpoints.put(baseReactantArcs.get(0).getId(), localEditPoints0);
            arcsId2Editpoints.put(baseReactantArcs.get(1).getId(), localEditPoints1);
            arcsId2Editpoints.put(baseProductArcs.get(0).getId(), localEditPoints2);
            LineWrapper lineWrapper = buildLineWrapperWithProcess(
                    arcsId2Editpoints,
                    processGlyph.getId(),
                    localAssocPoint
            );

            reactionW.setLineWrapper(lineWrapper);

        }
        else if(reactionCDClass == ReactionType.DISSOCIATION) {
            ReactionFeatures reactionFeatures = new ReactionFeatures(isReversible,true, false);

            ReactantWrapper baseReactantW = baseReactantsW.get(0);
            ReactantWrapper baseProductW1 = baseProductsW.get(0);
            ReactantWrapper baseProductW2 = baseProductsW.get(1);

            SimpleEntry<Point2D.Float, Point2D.Float> tmpResult = getAssocDissocPoints(
                    Arrays.asList(baseReactantW, baseProductW1, baseProductW2),
                    processGlyph,
                    processCoords,
                    baseReactantArcs.get(0),
                    false
            );
            Point2D.Float absDissocPoint = tmpResult.getKey();
            Point2D.Float localDissocPoint = tmpResult.getValue();


            SimpleEntry<Link, Link> tmpResultPoints = baseLinkProcessingStep1(
                    baseReactantW, baseReactantGlyphs.get(0),
                    baseReactantArcs.get(0), absDissocPoint, true, reactionFeatures);
            List<Point2D.Float> localEditPoints0 = tmpResultPoints.getKey().getEditPoints();
            Point2D.Float finalStartPoint = tmpResultPoints.getKey().getStart();
            Link reactantLink = tmpResultPoints.getValue();

            List<Point2D.Float> localEditPoints1 = baseLinkProcessingStep1(
                    baseProductW1, baseProductGlyphs.get(0),
                    baseProductArcs.get(0), absDissocPoint, false, reactionFeatures).getKey().getEditPoints();

            List<Point2D.Float> localEditPoints2 = baseLinkProcessingStep1(
                    baseProductW2, baseProductGlyphs.get(1),
                    baseProductArcs.get(1), absDissocPoint, false, reactionFeatures).getKey().getEditPoints();


            processLine = getProcessLine(reactantLink, absDissocPoint, reactionFeatures);

            java.util.Map<String, List<Point2D.Float>> arcsId2Editpoints = new LinkedHashMap<>();
            arcsId2Editpoints.put(baseReactantArcs.get(0).getId(), localEditPoints0);
            arcsId2Editpoints.put(baseProductArcs.get(0).getId(), localEditPoints1);
            arcsId2Editpoints.put(baseProductArcs.get(1).getId(), localEditPoints2);
            LineWrapper lineWrapper = buildLineWrapperWithProcess(
                    arcsId2Editpoints,
                    processGlyph.getId(),
                    localDissocPoint
            );

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
            int processSegmentIndex = reactantPoints.size() - 1;

            java.util.Map<String, List<Point2D.Float>> arcsId2Editpoints = new LinkedHashMap<>();
            arcsId2Editpoints.put(baseReactantArcs.get(0).getId(),
                    localEditPoints.subList(0,processSegmentIndex));
            arcsId2Editpoints.put(baseProductArcs.get(0).getId(),
                    localEditPoints.subList(processSegmentIndex, localEditPoints.size()));
            LineWrapper lineWrapper = buildLineWrapperWithProcess(
                    arcsId2Editpoints,
                    processGlyph.getId(),
                    null
            );

            reactionW.setLineWrapper(lineWrapper);

        }

        // now that base reaction link is established, build the process model
        Process pr = new Process(
                GeometryUtils.getMiddle((Point2D.Float) processLine.getP1(),(Point2D.Float) processLine.getP2()),
                processGlyph.getId(),
                processLine,
                new StyleInfo(processGlyph.getId())
        );

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

            LineWrapper lineWrapper = buildLineWrapper(additionalArc.getId(), localEditPoints, null);

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

            LineWrapper lineWrapper = buildLineWrapper(additionalArc.getId(), localEditPoints, null);

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
                List<Arc> arcsConnectedToLogic = glyphToArc.get(modificationGlyph.getId());

                List<ReactantWrapper> connectedReactantsW = new ArrayList<>();
                List<String> logicModifiers = new ArrayList<>();
                List<String> logicAliases = new ArrayList<>();
                for(Arc logicArc: arcsConnectedToLogic) {
                    Glyph sourceGlyhp = arcToSource.get(logicArc.getId());
                    // discard other connected logic gates, and the arc coming from the current gate itself
                    if(SBGNUtils.isLogicGate(sourceGlyhp)) {
                        continue;
                    }

                    AliasWrapper aliasW = aliasWrapperMap.get(sourceGlyhp.getId()+"_alias1");

                    ReactantWrapper modifWrapper = new ReactantWrapper(aliasW, ReactantType.MODIFICATION);

                    // modifications linked to logic gates inherit their link type
                    modifWrapper.setModificationLinkType(
                            ModificationLinkType.valueOf(
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

                orphanLogicGates.remove(modificationGlyph);

                /*SpeciesWrapper logicSpW = new SpeciesWrapper(modificationGlyph.getId(),
                        modificationGlyph.getId(), null);
                AliasWrapper logicAliasW = new AliasWrapper(modificationGlyph.getId()+"_alias1",
                        AliasWrapper.AliasType.SPECIES, logicSpW);
                finalLogicW.setAliasW(logicAliasW);*/
                finalLogicW.setAnchorPoint(AnchorPoint.CENTER);

                reactionW.getModifiers().add(finalLogicW);
                for(ReactantWrapper connectedToLogic: connectedReactantsW) {
                    reactionW.getModifiers().add(connectedToLogic);
                }


            }
            // non logic gates modifiers
            else {
                ReactantWrapper processedModifW = processModifier(modificationArc,
                        modificationGlyph, modificationW, pr);
                reactionW.getModifiers().add(processedModifW);
            }

            // get all speciesReference and add the reaction to those species as catalyzedReaction
            for(ModifierSpeciesReference speciesReference:
                    reactionW.getCDReaction().getListOfModifiers().getModifierSpeciesReference()) {
                String speciesId = speciesReference.getSpecies();
                SpeciesWrapper speciesW = speciesWrapperMap.get(speciesId);

                speciesW.getCatalyzedReactions().add(reactionW.getId());
            }

        }

        setNotes(reactionW, processGlyph);
        setAnnotations(reactionW, processGlyph);

        sbml.getModel().getListOfReactions().getReaction().add(reactionW.getCDReaction());

    }

    private void processOrphanArc(Arc orphanArc) {
        // process orphan arcs
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

        LineWrapper lineWrapper = buildLineWrapper(orphanArc.getId(), localEditPoints, null);

        ReactionWrapper reactionW = new ReactionWrapper(
                orphanArc.getId(),
                ReactionType.valueOf(LinkModel.getReducedCdClass(ArcClazz.fromClazz(orphanArc.getClazz()))),
                Collections.singletonList(sourceWrapper),
                Collections.singletonList(targetWrapper));

        reactionW.setLineWrapper(lineWrapper);
        reactionW.setHasProcess(false);

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

        LineWrapper lineWrapper = buildLineWrapper(modificationArc.getId(), localEditPoints, null);

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
        int processAnchor = inferProcessAnchorPoint(endPoint, pr);
        modificationW.setTargetLineIndex("-1," + processAnchor);

        Point2D.Float finalEndPoint = pr.getAbsoluteAnchorCoords(processAnchor);

        // for logic gates, just take the center of the glyph
        Point2D.Float finalStartPoint = startPoint;

        List<Point2D.Float> localEditPoints = GeometryUtils.convertPoints(
                editPointsOnly,
                GeometryUtils.getTransformsToLocalCoords(
                        finalStartPoint,
                        finalEndPoint
                ));

        // logic gates have their own coordinate added to the edit point, in global coord system
        // we need to adjust to map translation factor
        Point2D.Float logicPoint = new Point2D.Float(
                (float) (finalStartPoint.getX() - mapBounds.getX()),
                (float) (finalStartPoint.getY() - mapBounds.getY()));

        LineWrapper lineWrapper = buildLineWrapper(modificationArc.getId(), localEditPoints, logicPoint);

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

        LineWrapper lineWrapper;
        // logic gates have their own coordinate added to the edit point, in global coord system
        if(SBGNUtils.isLogicGate(modificationGlyph)) {
            Point2D.Float logicPoint = new Point2D.Float(
                    (float) finalStartPoint.getX(),
                    (float) finalStartPoint.getY());
            lineWrapper = buildLineWrapper(modificationArc.getId(), localEditPoints, logicPoint);
        }
        else {
            lineWrapper = buildLineWrapper(modificationArc.getId(), localEditPoints, null);
        }

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
        List<Glyph> unitOfInfoList = glyph.getGlyph().stream()
                .filter(g->GlyphClazz.fromClazz(g.getClazz()) == UNIT_OF_INFORMATION)
                .collect(Collectors.toList());

        // first determine specific subtypes
        SpeciesWrapper.ReferenceType subType = null;
        boolean ionFlag = false;
        boolean drugFlag = false;
        if(!isComplex) {
            switch(GlyphClazz.fromClazz(glyph.getClazz())) {
                case MACROMOLECULE:
                case MACROMOLECULE_MULTIMER:
                    subType = SpeciesWrapper.ReferenceType.GENERIC; // default

                    Optional<Glyph> g = SBGNUtils.getUnitOfInfo(glyph, "receptor");
                    if(g.isPresent()) {
                        subType = SpeciesWrapper.ReferenceType.RECEPTOR;
                        unitOfInfoList.remove(g.get());
                    }
                    g = SBGNUtils.getUnitOfInfo(glyph, "ion channel");
                    if(g.isPresent()) {
                        subType = SpeciesWrapper.ReferenceType.ION_CHANNEL;
                        unitOfInfoList.remove(g.get());
                    }
                    g = SBGNUtils.getUnitOfInfo(glyph, "truncated");
                    if(g.isPresent()) {
                        subType = SpeciesWrapper.ReferenceType.TRUNCATED;
                        unitOfInfoList.remove(g.get());
                    }
                    break;
                case NUCLEIC_ACID_FEATURE:
                case NUCLEIC_ACID_FEATURE_MULTIMER:
                    subType = SpeciesWrapper.ReferenceType.GENE; // default
                    g = SBGNUtils.getUnitOfInfo(glyph, "rna");
                    if(g.isPresent()) {
                        subType = SpeciesWrapper.ReferenceType.RNA;
                        unitOfInfoList.remove(g.get());
                    }
                    g = SBGNUtils.getUnitOfInfo(glyph, "asrna");
                    if(g.isPresent()) {
                        subType = SpeciesWrapper.ReferenceType.ANTISENSE_RNA;
                        unitOfInfoList.remove(g.get());
                    }
                    break;
                case SIMPLE_CHEMICAL:
                case SIMPLE_CHEMICAL_MULTIMER:
                    g = SBGNUtils.getUnitOfInfo(glyph, "ion");
                    if(g.isPresent()) {
                        ionFlag = true;
                        unitOfInfoList.remove(g.get());
                    }
                    g = SBGNUtils.getUnitOfInfo(glyph, "drug");
                    if(g.isPresent()) {
                        drugFlag = true;
                        unitOfInfoList.remove(g.get());
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

        // process unit of info
        // certain values are allowed by CellDesigner. Other things are considered free input.
        HashSet<String> recognizedInfo = new HashSet<>(Arrays.asList(
                "pc:T", "pc:V", "pc:pH",
                "mt:ion", "mt:rad", "mt:rna", "mt:dna", "mt:prot", "mt:psac",
                "ct:gene", "ct:tss","ct:coding","ct:grr","ct:mRNA"
                )); // also N:\d+

        List<AliasInfoWrapper> infoWrapperList = new ArrayList<>();
        for(Glyph infoUnit: unitOfInfoList) {
            // !! beware angle direction is inversed for units of info...
            double angle = -GeometryUtils.getAngleOfAuxUnit(
                    SBGNUtils.getRectangleFromGlyph(glyph),
                    SBGNUtils.getRectangleFromGlyph(infoUnit));

            String value = infoUnit.getLabel().getText();
            String prefix;
            String infoLabel;
            if(recognizedInfo.contains(value) || value.startsWith("N:")) {
                String[] tmp = value.split(":");
                prefix = tmp[0];
                infoLabel = tmp[1];
            }
            else {
                prefix = "free input";
                infoLabel = value;
            }

            AliasInfoWrapper infoWrapper = new AliasInfoWrapper(
                    (float) angle,
                    prefix,
                    infoLabel);

            infoWrapperList.add(infoWrapper);
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
            aliasW = new AliasWrapper(aliasId, AliasWrapper.AliasType.SPECIES, speciesW);
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
            try {
                speciesW.setCdClass(ReactantModel.getCdClass(glyph.getClazz(), subType));
            }
            catch (Exception e) {
                logger.error(e.getMessage()+" Glyph will be skipped and will not appear in translation.");
                return;
            }
        }

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

        setNotes(speciesW, glyph);
        setAnnotations(speciesW, glyph);

        // add species to correct list
        if(isIncluded) {
            speciesW.setComplex(parentSpeciesId);
            speciesW.setIncludedSpecies(true);
        }
        speciesWrapperMap.put(speciesW.getId(), speciesW);

        // PROCESS ALIAS
        // compartmentRef
        if(glyph.getCompartmentRef() != null) {
            aliasW.setCompartmentAlias(((Glyph) glyph.getCompartmentRef()).getId()+"_alias1"); // TODO get alias id properly
        }

        Rectangle2D.Float bounds = new Rectangle2D.Float();
        bounds.setRect(
                glyph.getBbox().getX()- (float) mapBounds.getX(),
                glyph.getBbox().getY()- (float) mapBounds.getY(),
                glyph.getBbox().getW(),
                glyph.getBbox().getH());
        aliasW.setBounds(bounds);

        // style
        if(mapHasStyle)
            aliasW.setStyleInfo(styleMap.get(glyph.getId()));
        else
            aliasW.setStyleInfo(new StyleInfo(aliasW.getId()));

        if(isIncluded) {
            aliasW.setComplexAlias(parentAliasId);
        }

        // unit of info management
        // CellDesigner only allows 1 unit of information, this can lead to loss of info
        // only consider the first remaining info unit, output errors about the others
        if(infoWrapperList.size() > 0) {
            aliasW.setInfo(infoWrapperList.get(0));

            if(infoWrapperList.size() > 1) {
                for(int j=1; i < unitOfInfoList.size(); i++) {
                    Glyph discardedUnit = unitOfInfoList.get(j);
                    logger.error("Unit of information with id: "+discardedUnit.getId()+" and content: "
                            +discardedUnit.getLabel().getText()+" on glyph with id: "+ glyph.getId()+" cannot be" +
                            "translated and will be lost.");
                }
            }
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

        setNotes(compM, glyph);
        setAnnotations(compM, glyph);

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
        orphanLogicGates = new HashSet<>();
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

        java.util.Map<String, Glyph> terminalId2Submap = new HashMap<>();
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
            if(clazz == AND || clazz == OR || clazz == NOT) {
                orphanLogicGates.add(g);
            }

            // in case of submap, go inside and index all terminals
            if(clazz == SUBMAP) {
                for(Glyph subGlyph: g.getGlyph()) {
                    if(GlyphClazz.fromClazz(subGlyph.getClazz()) == TERMINAL) {
                        terminalId2Submap.put(subGlyph.getId(), g);
                    }
                }
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
            }
            // for terminals, make links point directly at the parent submap
            else if(arc.getSource() instanceof Glyph && ((Glyph) arc.getSource()).getClazz().equals("terminal")) {
                // terminal should always be the target, not the source.
                logger.warn("The arc: "+arc.getId()+" has a source terminal (id: "+((Glyph) arc.getSource()).getId()+"). " +
                        "But Arcs should always have terminals as target.");
                sourceGlyph = terminalId2Submap.get(((Glyph) arc.getSource()).getId());
            }
            else { // glyph itself
                sourceGlyph = (Glyph) arc.getSource();
            }
            arcToSource.put(arc.getId(), sourceGlyph);

            if(arc.getTarget() instanceof Port) {
                Port p = (Port) arc.getTarget();
                targetGlyph = portToGlyph.get(p.getId());
            }
            // for terminals, make links point directly at the parent submap
            else if(arc.getTarget() instanceof Glyph && ((Glyph) arc.getTarget()).getClazz().equals("terminal")) {
                targetGlyph = terminalId2Submap.get(((Glyph) arc.getTarget()).getId());
            }
            else { // glyph itself
                targetGlyph = (Glyph) arc.getTarget();
            }
            arcToTarget.put(arc.getId(), targetGlyph);

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

            /*//
                Filter things that will be considered as orphan arcs. We want all direct arcs (that is arcs that do
                not connect a process) and arcs that are not linked to logic gates (because those are already treated
                when connected to process) except when the arc is also connected to phenotypes or submap. Phenotypes
                and submaps can have connected logic gates. Those logic gates have to be processed with the orphan arcs.
             */
            if(         !isConnectedToProcess
                    &&
                        ((!SBGNUtils.isLogicGate(sourceGlyph)
                            && !SBGNUtils.isLogicGate(targetGlyph)))) {
                orphanArcs.add(arc);
            }
        }

    }

    class ReactionFeatures {
        private boolean isReversible;
        private boolean isBranch;
        private boolean isAssociation;
        private boolean isLogicGateReaction;

        public ReactionFeatures(boolean isReversible, boolean isBranch, boolean isAssociation) {
            this.isReversible = isReversible;
            this.isBranch = isBranch;
            this.isAssociation = isAssociation;
            this.isLogicGateReaction = false;
        }

        public ReactionFeatures(boolean isReversible, boolean isBranch, boolean isAssociation, boolean isLogicGateReaction) {
            this.isReversible = isReversible;
            this.isBranch = isBranch;
            this.isAssociation = isAssociation;
            this.isLogicGateReaction = isLogicGateReaction;
        }

        public boolean isReversible() {
            return isReversible;
        }

        public boolean isAssociation() {
            return isBranch && isAssociation;
        }

        public boolean isDissociation() {
            return isBranch && !isAssociation;
        }

        public boolean isReactionSimple() {
            return !isBranch;
        }

        public boolean isLogicGateReaction() {
            return isLogicGateReaction;
        }
    }


    public SimpleEntry<Link, Link> baseLinkProcessingStep1(ReactantWrapper reactantW, Glyph glyph, Arc arc,
                                       Point2D.Float absAssocPoint,
                                       boolean isReactant, ReactionFeatures options) {
        // get point lists in correct order
        // apply the mapBounds correction to each point of the arc to get consistent coords
        List<Point2D.Float> reactantPoints0 = applyCorrection(SBGNUtils.getPoints(arc),
                (float) mapBounds.getX(),(float) mapBounds.getY());
        if(options.isReversible() && isReactant) {
            Collections.reverse(reactantPoints0);
        }
        Link reactantLink0 = new Link(reactantPoints0);


        // infer best anchorpoints possible
        Rectangle2D.Float baseRect0 = SBGNUtils.getRectangleFromGlyph(glyph);
        baseRect0.setRect(
                baseRect0.getX() - mapBounds.getX(),
                baseRect0.getY() - mapBounds.getY(),
                baseRect0.getWidth(),
                baseRect0.getHeight());
        AnchorPoint startAnchor0;
        if(isReactant) {
            startAnchor0 = inferAnchorPoint(reactantLink0.getStart(), reactantW, baseRect0);
        }
        else {
            startAnchor0 = inferAnchorPoint(reactantLink0.getEnd(), reactantW, baseRect0);
        }
        reactantW.setAnchorPoint(startAnchor0);

        // compute exact final points from the inferred anchor
        Point2D.Float finalPoint = getFinalpoint(
                reactantW.getAnchorPoint(),
                reactantW,
                baseRect0);

        /*  compute branch edit points
                reverse points for consumption because CellDesigner consider all branches to start from association
                glyph. Don't reverse points for reversible reactions, as the production arcs already point to the right
                direction.
            */
        List<Point2D.Float> editpoints0 = reactantLink0.getEditPoints();
        if(!options.isReversible && isReactant)
            Collections.reverse(editpoints0);

        AffineTransform transform;
        if(options.isLogicGateReaction) {
            transform = GeometryUtils.getTransformsToLocalCoords(finalPoint, absAssocPoint);
        }
        else {
            transform = GeometryUtils.getTransformsToLocalCoords(absAssocPoint, finalPoint);
        }

        List<Point2D.Float> localEditPoints0 = GeometryUtils.convertPoints(editpoints0, transform);
        List<Point2D.Float> finalAndLocalPoints = new ArrayList<>();
        finalAndLocalPoints.add(new Point2D.Float());
        finalAndLocalPoints.addAll(localEditPoints0);
        finalAndLocalPoints.add(finalPoint);
        return new SimpleEntry<>(new Link(finalAndLocalPoints), reactantLink0);
    }

    public SimpleEntry<Point2D.Float, Point2D.Float> getAssocDissocPoints(List<ReactantWrapper> reactants,
                                                                          Glyph processGlyph,
                                                                          Point2D.Float processCoords,
                                                                          Arc arc, boolean isAssociation) {
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
            Port oppositePort;
            if(isAssociation) {
                oppositePort = (Port) arc.getSource();
            }
            else {
                oppositePort = (Port) arc.getTarget();
            }

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
        Point2D.Float localAssocPoint = GeometryUtils.convertPoints(
                Collections.singletonList(absAssocPoint),
                GeometryUtils.getTransformsToLocalCoords(
                        reactants.get(0).getCenterPoint(),
                        reactants.get(1).getCenterPoint(),
                        reactants.get(2).getCenterPoint()
                )).get(0);
        return new SimpleEntry<>(absAssocPoint, localAssocPoint);
    }

    public Line2D.Float getProcessLine(Link link, Point2D assocPoint, ReactionFeatures options) {
        if(link.getEditPoints().size() > 0) { // there are some edit points
            if(options.isAssociation())
                return new Line2D.Float(
                        assocPoint,
                        link.getEditPoints().get(0)
                );
            else
                return new Line2D.Float(
                        link.getEditPoints().get(0),
                        assocPoint
                );
        }
        else { // straight line
            if(options.isAssociation())
                return new Line2D.Float(
                        assocPoint,
                        link.getEnd()
                );
            else
                return new Line2D.Float(
                        link.getStart(),
                        assocPoint
                );
        }
    }

    /**
     *
     * @param arcIds2LocalEditPoints at least 2 entries, 3 for branch reactions
     * @param processGLyphId
     * @param localAssocPoint
     * @return
     */
    public LineWrapper buildLineWrapperWithProcess(java.util.Map<String, List<Point2D.Float>> arcIds2LocalEditPoints,
                                                   String processGLyphId, Point2D.Float localAssocPoint) {
        // finally set up the xml elements and add to reactions
        List<String> arcsIds = new ArrayList<>();
        List<List<Point2D.Float>> editPointsList = new ArrayList<>();
        List<Integer> segmentCountList = new ArrayList<>();
        int totalSegmentCount = 1;
        int processSegmentIndex = 0;
        boolean isBranchReactionType = localAssocPoint != null;

        boolean isFirstEntry = true;
        for(java.util.Map.Entry<String, List<Point2D.Float>> entry: arcIds2LocalEditPoints.entrySet()) {
            arcsIds.add(entry.getKey());
            editPointsList.add(entry.getValue());
            segmentCountList.add(entry.getValue().size()+1);
            totalSegmentCount += entry.getValue().size();
            if(isFirstEntry) {
                isFirstEntry = false;
                processSegmentIndex = entry.getValue().size();
            }
        }

        ConnectScheme connectScheme;
        if(isBranchReactionType) {
            connectScheme = getBranchConnectScheme(segmentCountList);
        }
        else {
            connectScheme = getSimpleConnectScheme(totalSegmentCount, processSegmentIndex);
        }


        String lineColor = "ff000000";
        float lineWidth = 1;
        /*
            Set a style only if all components' style are the same
         */
        if(mapHasStyle) {
            boolean areAllStyleTheSame = true;
            StyleInfo arcStyle1 = styleMap.get(arcsIds.get(0));
            // check all styles are homogeneous by comparing all other arcs styles to arcStyle1
            for(int i=1; i < arcsIds.size(); i++) {
                StyleInfo arcStyle2 = styleMap.get(arcsIds.get(i));
                if(arcStyle1.getLineWidth() != arcStyle2.getLineWidth()
                        || !arcStyle1.getLineColor().equals(arcStyle2.getLineColor())) {
                    areAllStyleTheSame = false;
                    break;
                }
            }
            // finally check that all arc's styles are consistent with process glyph style
            StyleInfo processGlyphStyle = styleMap.get(processGLyphId);
            if(arcStyle1.getLineWidth() != processGlyphStyle.getLineWidth()
                    || !arcStyle1.getLineColor().equals(processGlyphStyle.getLineColor())) {
                areAllStyleTheSame = false;
            }

            if(areAllStyleTheSame) { // styles are all consistent
                lineWidth = arcStyle1.getLineWidth();
                lineColor = arcStyle1.getLineColor();
            }
        }

        Line line = new Line();
        line.setWidth(BigDecimal.valueOf(lineWidth));
        line.setColor(lineColor);

        List<String> editPointString = new ArrayList<>();
        List<Point2D.Float> mergedList = new ArrayList<>();
        for(List<Point2D.Float> editPoints: editPointsList) {
            mergedList.addAll(editPoints);
        }
        if(isBranchReactionType) { // the assocPoint needs to be added at the end of the string
            mergedList.add(localAssocPoint);
        }
        for(Point2D.Float p: mergedList) {
            editPointString.add(p.getX()+","+p.getY());
        }

        LineWrapper lineWrapper = new LineWrapper(connectScheme, editPointString, line);
        if(isBranchReactionType) {
            // here the number of edit points is needed
            lineWrapper.setNum0(segmentCountList.get(0) - 1);
            lineWrapper.setNum1(segmentCountList.get(1) - 1);
            lineWrapper.setNum2(segmentCountList.get(2) - 1);
            lineWrapper.settShapeIndex(0);
        }

        return lineWrapper;
    }

    /**
     *
     * @param arcId
     * @param localEditPoints
     * @param additionalPoint for logic gates, their coordinates must be added at the end of the edit points string
     * @return
     */
    public LineWrapper buildLineWrapper(String arcId, List<Point2D.Float> localEditPoints, Point2D.Float additionalPoint) {

        String lineColor = "ff000000";
        float lineWidth = 1;
        if(mapHasStyle) {
            StyleInfo styleInfo = styleMap.get(arcId);
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

        if(additionalPoint != null) {
            editPointString.add(additionalPoint.getX()+","+additionalPoint.getY());
        }

        return new LineWrapper(connectScheme, editPointString, line);
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

    /**
     * Get the notes from an SBGN entity and add it to the provided object
     * @param notesFeature
     * @param sbgnEntity
     */
    public void setNotes(INotesFeature notesFeature, SBGNBase sbgnEntity) {
        if(sbgnEntity.getNotes() != null
                && sbgnEntity.getNotes().getAny().size() > 0) {
            Element notes = sbgnEntity.getNotes().getAny().get(0);
            notesFeature.setNotes(notes);
        }
    }

    /**
     * Get the annotations from an SBGN element and add it to the provided object
     * @param annotationsFeature
     * @param sbgnEntity
     */
    public void setAnnotations(IAnnotationsFeature annotationsFeature, SBGNBase sbgnEntity) {
        if(sbgnEntity.getExtension() != null) {
            for(Element e: sbgnEntity.getExtension().getAny()){
                if(e.getTagName().equals("annotation")) {
                    // TODO urn:miriam:CHEBI:12 doesn't seem to be loaded by CD
                    // TODO find a way to resolve uri ?
                    Element rdf = SBGNUtils.sanitizeRdfURNs((Element) e.getElementsByTagName("rdf:RDF").item(0));
                    annotationsFeature.setAnnotations(rdf);
                }
            }
        }
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
