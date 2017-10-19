package fr.curie.cd2sbgnml;

import fr.curie.cd2sbgnml.graphics.GeometryUtils;
import fr.curie.cd2sbgnml.graphics.Link;
import fr.curie.cd2sbgnml.model.*;
import fr.curie.cd2sbgnml.model.Process;
import fr.curie.cd2sbgnml.xmlcdwrappers.*;
import org.apache.xmlbeans.XmlObject;
import org.sbfc.converter.GeneralConverter;
import org.sbfc.converter.exceptions.ConversionException;
import org.sbfc.converter.exceptions.ReadModelException;
import org.sbfc.converter.models.GeneralModel;
import org.sbfc.converter.models.SBGNModel;
import org.sbgn.bindings.*;
import org.sbgn.bindings.Glyph.State;
import org.sbgn.bindings.Map;
import org.sbml.x2001.ns.celldesigner.CelldesignerBoundsDocument.CelldesignerBounds;
import org.sbml.x2001.ns.celldesigner.CelldesignerCompartmentAliasDocument.CelldesignerCompartmentAlias;
import org.sbml.x2001.ns.celldesigner.*;
import org.sbml.x2001.ns.celldesigner.CelldesignerPointDocument.CelldesignerPoint;
import org.sbml.x2001.ns.celldesigner.CompartmentDocument.Compartment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.UTFDataFormatException;
import java.util.*;

import static fr.curie.cd2sbgnml.model.ReactantModel.getSbgnClass;

public class CD2SBGNML extends GeneralConverter {

    final Logger logger = LoggerFactory.getLogger(CD2SBGNML.class);

    List<Glyph> glyphList;
    HashMap<String, Glyph> glyphMap;

    public Sbgn toSbgn( SbmlDocument sbmlDoc) {
        Sbgn sbgn = new Sbgn();
        Map map = new Map();
        map.setId("mapID");
        sbgn.getMap().add(map);

        ModelWrapper modelW = ModelWrapper.create(sbmlDoc);

        // put model notes into map notes
        if(modelW.getModel().getNotes() != null) {
            map.setNotes(getSBGNNotes(Utils.getNotes(modelW.getModel())));
        }
        //System.exit(1);

        System.out.println(modelW.getListOfSpecies().size());
        System.out.println(modelW.getListOfIncludedSpecies().size());
        System.out.println(modelW.getListOfCompartments().size());
        System.out.println("compartment aliases count: "+modelW.getListOfCompartmentAliases().size());


        this.glyphList = new ArrayList<>();
        this.glyphMap = new HashMap<>();

        // compartment section
        for(Compartment compartment: modelW.getListOfCompartments()) {
            processCompartment(compartment, modelW, map);
        }

        // species section
        for(SpeciesWrapper speciesW: modelW.getListOfSpeciesWrapper()) {
            //SpeciesWrapper speciesW = new SpeciesWrapper(species, modelW);
            processSpecies(speciesW, modelW, map);
        }

        // reactions
        for(ReactionWrapper reactionW: modelW.getListOfReactionWrapper()) {
            //ReactionWrapper reactionW = modelW.getReactionWrapperFor(reaction.getId());
            GenericReactionModel genericReactionModel = ReactionModelFactory.create(reactionW);


            // PROCESS

            System.out.println(reactionW.getId()+" "+reactionW.getReactantList().size());
            //System.out.println("branch ? "+reactionW.isBranchType()+" right: "+reactionW.isBranchTypeRight()+" left: "+reactionW.isBranchTypeLeft());

            String processId = null;
            if(reactionW.hasProcess()) {
                Point2D processCoord = genericReactionModel.getProcess().getGlyph().getCenter();
                System.out.println("process coord " + processCoord);


                Glyph processGlyph = new Glyph();
                processGlyph.setClazz(Process.getSbgnClass(genericReactionModel.getCdReactionType()));
                processId = genericReactionModel.getProcess().getId();
                processGlyph.setId(processId);

                Bbox processBbox = new Bbox();
                processBbox.setX((float) processCoord.getX() - Process.PROCESS_SIZE / 2);
                processBbox.setY((float) processCoord.getY() - Process.PROCESS_SIZE / 2);
                processBbox.setH(Process.PROCESS_SIZE);
                processBbox.setW(Process.PROCESS_SIZE);
                processGlyph.setBbox(processBbox);

                // put reaction into process glyph
                // TODO if no process, add notes into the arc
                processGlyph.setNotes(getSBGNNotes(Utils.getNotes(reactionW.getReaction())));

                glyphList.add(processGlyph);
                glyphMap.put(processId, processGlyph);
                map.getGlyph().add(processGlyph);
            }

            // Possible logic gates
            for(ReactionNodeModel nodeModel: genericReactionModel.getReactionNodeModels()) {
                if(nodeModel instanceof LogicGate) {
                    LogicGate logicGate = (LogicGate) nodeModel;

                    Point2D logicCoord = logicGate.getGlyph().getCenter();
                    System.out.println("process coord " + logicCoord);


                    Glyph logicGlyph = new Glyph();
                    try {
                        logicGlyph.setClazz(LogicGate.getSbgnClass(logicGate.getType()));
                    } catch(RuntimeException e) {
                        // we need to remove links pointing to this gate
                        System.out.println("DELETE link to gate");
                        logger.error("BOOLEAN_LOGIC_GATE_UNKNOWN was found in reaction " + reactionW.getId() +
                                " and cannot be translated properly. It will be removed.");
                        for (Iterator<LinkModel> iter = genericReactionModel.getLinkModels().listIterator(); iter.hasNext(); ) {
                            LinkModel lm = iter.next();
                            if(lm.getEnd().equals(logicGate) || lm.getStart().equals(logicGate)) {
                                logger.error("Removing "+lm.getSbgnClass()+" link with id "+lm.getId()+" to or from unknown " +
                                        "logic gate");
                                System.out.println("FOUND LINK TO DELETE");
                                iter.remove();
                                System.out.println(genericReactionModel.getLinkModels().size());
                            }

                        }

                        continue;
                    }
                    String logicId = logicGate.getId();
                    logicGlyph.setId(logicId);

                    Bbox logicBbox = new Bbox();
                    logicBbox.setX((float) logicCoord.getX() - LogicGate.LOGICGATE_SIZE / 2);
                    logicBbox.setY((float) logicCoord.getY() - LogicGate.LOGICGATE_SIZE / 2);
                    logicBbox.setH(LogicGate.LOGICGATE_SIZE);
                    logicBbox.setW(LogicGate.LOGICGATE_SIZE);
                    logicGlyph.setBbox(logicBbox);

                    glyphList.add(logicGlyph);
                    glyphMap.put(logicId, logicGlyph);
                    map.getGlyph().add(logicGlyph);

                }
            }


            ReactantWrapper baseReactant = reactionW.getBaseReactants().get(0);
            ReactantWrapper baseProduct = reactionW.getBaseProducts().get(0);
            // ARCS

            if(!reactionW.isBranchType()){
                for(LinkModel ln: genericReactionModel.getLinkModels()) {
                    map.getArc().add(getArc(ln));
                }
            }
            else if(reactionW.isBranchTypeLeft()) {
                System.out.println("REACTION: "+reactionW.getId());

                // add association glyph
                Glyph assocGlyph = new Glyph();
                assocGlyph.setClazz("association");
                String assocId = genericReactionModel.getAssocDissoc().getId();
                assocGlyph.setId(assocId);

                Point2D assocCoord = genericReactionModel.getAssocDissoc().getGlyph().getCenter();
                Bbox assocBbox = new Bbox();
                assocBbox.setX((float) assocCoord.getX() - AssocDissoc.ASSOCDISSOC_SIZE / 2);
                assocBbox.setY((float) assocCoord.getY() - AssocDissoc.ASSOCDISSOC_SIZE / 2);
                assocBbox.setH(AssocDissoc.ASSOCDISSOC_SIZE);
                assocBbox.setW(AssocDissoc.ASSOCDISSOC_SIZE);
                assocGlyph.setBbox(assocBbox);

                glyphList.add(assocGlyph);
                glyphMap.put(assocId, assocGlyph);
                map.getGlyph().add(assocGlyph);

                for(LinkModel ln: genericReactionModel.getLinkModels()) {
                    map.getArc().add(getArc(ln));
                }
            }
            else {
                System.out.println("REACTION: "+reactionW.getId());

                // add association glyph
                Glyph dissocGlyph = new Glyph();
                dissocGlyph.setClazz("dissociation");
                String dissocId = genericReactionModel.getAssocDissoc().getId();
                dissocGlyph.setId(dissocId);

                Point2D dissocCoord = genericReactionModel.getAssocDissoc().getGlyph().getCenter();
                Bbox dissocBbox = new Bbox();
                dissocBbox.setX((float) dissocCoord.getX() - AssocDissoc.ASSOCDISSOC_SIZE / 2);
                dissocBbox.setY((float) dissocCoord.getY() - AssocDissoc.ASSOCDISSOC_SIZE / 2);
                dissocBbox.setH(AssocDissoc.ASSOCDISSOC_SIZE);
                dissocBbox.setW(AssocDissoc.ASSOCDISSOC_SIZE);
                dissocGlyph.setBbox(dissocBbox);

                glyphList.add(dissocGlyph);
                glyphMap.put(dissocId, dissocGlyph);
                map.getGlyph().add(dissocGlyph);

                for(LinkModel ln: genericReactionModel.getLinkModels()) {
                    map.getArc().add(getArc(ln));
                }

            }
        }

        // text notes on the map
        for(TextWrapper textW: modelW.getListofTextWrapper()) {
            if(!textW.isVisible()) {
                continue;
            }

            Glyph textGlyph = new Glyph();

            Bbox textBbox = new Bbox();
            textBbox.setX((float) textW.getBbox().getX());
            textBbox.setY((float) textW.getBbox().getY());
            textBbox.setW((float) textW.getBbox().getWidth());
            textBbox.setH((float) textW.getBbox().getHeight());
            textGlyph.setBbox(textBbox);

            Label textLabel = new Label();
            textLabel.setText(textW.getText());
            textGlyph.setLabel(textLabel);

            textGlyph.setClazz("annotation");
            textGlyph.setId("text_"+UUID.randomUUID());

            // set reference point
            Glyph.Callout callout = new Glyph.Callout();
            Point calloutPoint = new Point();
            calloutPoint.setX((float) textW.getRefPoint().getX());
            calloutPoint.setY((float) textW.getRefPoint().getY());
            callout.setPoint(calloutPoint);
            textGlyph.setCallout(callout);

            map.getGlyph().add(textGlyph);
        }

        return sbgn;
    }

    public void processCompartment(Compartment compartment, ModelWrapper modelW, Map map) {
        if(! compartment.getId().equals("default")) {
            //System.out.println(compartment.getId()+" "+compartment.getName().getStringValue() +" "+compartment.getOutside());
            for(CelldesignerCompartmentAlias alias : modelW.getCompartmentAliasFor(compartment.getId())) {
                //System.out.println(alias.getCelldesignerBounds());
                CelldesignerBounds cdBounds = alias.getCelldesignerBounds();
                CelldesignerPoint cdPoint = alias.getCelldesignerPoint();

                Glyph compGlyph = new Glyph();

                // basic info
                compGlyph.setClazz("compartment");
                compGlyph.setId(compartment.getId()+"_"+alias.getId());
                if (!compartment.getOutside().equals("default")) {
                    compGlyph.setCompartmentRef(glyphMap.get(compartment.getOutside()));
                }

                // label
                Label compLabel = new Label();
                compLabel.setText(compartment.getName().getStringValue());
                compGlyph.setLabel(compLabel);

                // bbox
                Bbox compBbox = new Bbox();
                if (cdBounds == null) {
                    // bounds may not be here, point instead
                    // assume that the compartment takes all the remaining map space
                    float pointX = Float.parseFloat(cdPoint.getX());
                    float pointY = Float.parseFloat(cdPoint.getY());
                    CelldesignerModelDisplayDocument.CelldesignerModelDisplay display =
                            modelW.getModel().getAnnotation().getCelldesignerModelDisplay();
                    float mapSizeX = Float.parseFloat(display.getSizeX());
                    float mapSizeY = Float.parseFloat(display.getSizeY());

                    Rectangle2D.Float bbox = GeometryUtils.getCompartmentBbox(
                            alias.getCelldesignerClass().getDomNode().getChildNodes().item(0).getNodeValue(),
                            pointX, pointY,
                            Float.parseFloat(alias.getCelldesignerDoubleLine().getThickness()),
                            mapSizeX, mapSizeY);

                    compBbox.setX((float) bbox.getX());
                    compBbox.setY((float) bbox.getY());
                    compBbox.setW((float) bbox.getWidth());
                    compBbox.setH((float) bbox.getHeight());
                } else {
                    compBbox.setX(Float.parseFloat(cdBounds.getX()));
                    compBbox.setY(Float.parseFloat(cdBounds.getY()));
                    compBbox.setH(Float.parseFloat(cdBounds.getH()));
                    compBbox.setW(Float.parseFloat(cdBounds.getW()));
                }
                compGlyph.setBbox(compBbox);

                // keep notes
                compGlyph.setNotes(getSBGNNotes(Utils.getNotes(compartment)));

                // keep references
                glyphList.add(compGlyph);
                glyphMap.put(compartment.getId(), compGlyph);

                // add to output
                map.getGlyph().add(compGlyph);
            }
        }
    }

    public Glyph processSpeciesAlias(SpeciesWrapper species, AliasWrapper alias, ModelWrapper modelW, Map map) {
        CelldesignerBounds bounds = alias.getBounds();
        Glyph glyph = getGlyph(alias);
        glyph.setNotes(getSBGNNotes(species.getNotes()));
        if(species.getReferenceNotes() != null) {
            // TODO is piling up <html> elements in 1 note ok ?
            glyph.getNotes().getAny().add(species.getReferenceNotes());
            System.out.println("MULTIPLE NOTES "+glyph.getNotes().getAny());
        }

        if(species.isComplex()) {
            System.out.println("COMPLEX species: "+species.getId()+" alias: "+alias.getId());
            System.out.println("include list "+modelW.getIncludedAliasWrapperFor(alias.getId()));
            if(modelW.getIncludedAliasWrapperFor(alias.getId()) == null) {
                // empty complex, should probably not happen
                //throw new IllegalStateException("empty complex for species "+species.getId()+" alias: "+alias.getId()+" name: "+species.getName());
                System.out.println("warning: empty complex for species "+species.getId()+" alias: "+alias.getId()+" name: "+species.getName());
            }
            else {
                for(AliasWrapper includedAlias: modelW.getIncludedAliasWrapperFor(alias.getId())) {
                    SpeciesWrapper includedSpecies = modelW.getSpeciesWrapperFor(includedAlias.getSpeciesId());
                    Glyph includedGlyph = processSpeciesAlias(includedSpecies, includedAlias, modelW, map);
                    glyph.getGlyph().add(includedGlyph);

                    /*
                    In ACSN, we need to keep references to included species also because some have links. Which
                    shouldn't happen.
                     */
                    glyphList.add(includedGlyph);
                    glyphMap.put(includedGlyph.getId(), includedGlyph);
                }
            }
        }

        return glyph;
    }

    public void processSpecies(SpeciesWrapper species, ModelWrapper modelW, Map map) {
        for(AliasWrapper alias : species.getAliases()) {
            // included species is already added inside its complex when complex is processed
            if(!species.isIncludedSpecies()) {
                Glyph glyph = processSpeciesAlias(species, alias, modelW, map);

                // keep references
                glyphList.add(glyph);
                glyphMap.put(glyph.getId(), glyph);
                // add to map
                map.getGlyph().add(glyph);
            }
        }
    }

    public Glyph getGlyph(AliasWrapper aliasW) {
        String id = aliasW.getSpeciesW().getId()+"_"+aliasW.getId();
        SpeciesWrapper species = aliasW.getSpeciesW();

        System.out.println(species.getId()+" "+species.getName()+" "+species.getCompartment());
        //System.out.println(bounds);
        Rectangle2D.Float bboxRect = new Rectangle2D.Float(
                Float.parseFloat(aliasW.getBounds().getX()),
                Float.parseFloat(aliasW.getBounds().getY()),
                Float.parseFloat(aliasW.getBounds().getW()),
                Float.parseFloat(aliasW.getBounds().getH() )
        );

        Glyph glyph = new Glyph();

        // basic info
        glyph.setId(id);
        if (! species.isIncludedSpecies() && !species.getCompartment().equals("default")) {
            glyph.setCompartmentRef(this.glyphMap.get(species.getCompartment()));
        }

        // label
        Label label = new Label();
        label.setText(species.getName());
        glyph.setLabel(label);

        // bbox
        Bbox bbox = new Bbox();
        bbox.setX((float) bboxRect.getX());
        bbox.setY((float) bboxRect.getY());
        bbox.setH((float) bboxRect.getHeight());
        bbox.setW((float) bboxRect.getWidth());
        glyph.setBbox(bbox);

        // structural state
        if(species.getStructuralState() != null) {

            Glyph statevar = getStateVariable("", species.getStructuralState(), bboxRect, 90);
            glyph.getGlyph().add(statevar);

        }

        // alias info unit
        if(aliasW.getInfo() != null) {

            Glyph unitOfInfo = getUnitOfInfo(aliasW.getInfo().getSbgnText(),
                    bboxRect,
                    // clockwork here !!!
                    - GeometryUtils.unsignedRadianToSignedDegree(aliasW.getInfo().angle));
            glyph.getGlyph().add(unitOfInfo);

        }

        // multimerize
        String sbgnClass = getSbgnClass(species.getCdClass());
        if(species.getMultimer() > 1) {
            sbgnClass += " multimer";

            // add another unit of info only if info with prefix N isn't there (from Aliasinfo),
            // else adding another unit of info would be redundant
            if(!(aliasW.getInfo() != null && aliasW.getInfo().prefix.equals("N"))) {

                float angle = 90;
                if (species.getStructuralState() != null) {
                    // if a unit of info is there already, top position is taken.
                    // put multimer info on bottom
                    angle = -90;
                }

                Glyph unitOfInfoMultimer = getUnitOfInfo("N:" + species.getMultimer(),
                        bboxRect,
                        angle);
                glyph.getGlyph().add(unitOfInfoMultimer);
            }

        }

        // state variables
        System.out.println("Create residues for "+species.getId()+" size: "+species.getResidues().size());
        for(ResidueWrapper residueW: species.getResidues()) {

            System.out.println("statevar: received: "+residueW.angle+" passed: "+GeometryUtils.unsignedRadianToSignedDegree(residueW.angle));
            Glyph residue = getStateVariable(
                    residueW.name,
                    ResidueWrapper.getShortState(residueW.state),
                    bboxRect,
                    GeometryUtils.unsignedRadianToSignedDegree(residueW.angle));

            glyph.getGlyph().add(residue);
        }

        // add additional units of info depending on the situation
        // eg for receptors, ion channels, truncated, genes...
        if(species.getType() == SpeciesWrapper.ReferenceType.RECEPTOR) {
            Glyph receptorUnitOfInfo = getUnitOfInfo("receptor", bboxRect, 90);
            glyph.getGlyph().add(receptorUnitOfInfo);
        }
        else if(species.getType() == SpeciesWrapper.ReferenceType.ION_CHANNEL) {
            Glyph receptorUnitOfInfo = getUnitOfInfo("ion channel", bboxRect, 90);
            glyph.getGlyph().add(receptorUnitOfInfo);

            Glyph activeStateVar;
            if(aliasW.isActive()) {
                activeStateVar = getStateVariable("", "open", bboxRect, -90);
            }
            else {
                activeStateVar = getStateVariable("", "closed", bboxRect, -90);
            }
            glyph.getGlyph().add(activeStateVar);
        }
        else if(species.getType() == SpeciesWrapper.ReferenceType.TRUNCATED) {
            Glyph receptorUnitOfInfo = getUnitOfInfo("truncated", bboxRect, 90);
            glyph.getGlyph().add(receptorUnitOfInfo);
        }


        // class
        glyph.setClazz(sbgnClass);


        return glyph;
    }

    public Glyph getStateVariable(String prefix, String value, Rectangle2D.Float parentBbox, float angle) {

        Glyph unitOfInfo = new Glyph();

        State state = new State();
        state.setValue(value);
        state.setVariable(prefix);
        unitOfInfo.setState(state);

        Rectangle2D.Float infoRect = GeometryUtils.getAuxUnitBbox(parentBbox, prefix+":"+value, angle);
        Bbox infoBbox = new Bbox();
        infoBbox.setX((float) infoRect.getX());
        infoBbox.setY((float) infoRect.getY());
        infoBbox.setW((float) infoRect.getWidth());
        infoBbox.setH((float) infoRect.getHeight());
        unitOfInfo.setBbox(infoBbox);

        unitOfInfo.setClazz("state variable");
        unitOfInfo.setId("_" + UUID.randomUUID());

        return unitOfInfo;
    }

    public Glyph getUnitOfInfo(String text, Rectangle2D.Float parentBbox, float angle) {

        Glyph unitOfInfo = new Glyph();
        Label infoLabel = new Label();
        infoLabel.setText(text);
        unitOfInfo.setLabel(infoLabel);

        Rectangle2D.Float infoRect = GeometryUtils.getAuxUnitBbox(parentBbox, text, angle);
        Bbox infoBbox = new Bbox();
        infoBbox.setX((float) infoRect.getX());
        infoBbox.setY((float) infoRect.getY());
        infoBbox.setW((float) infoRect.getWidth());
        infoBbox.setH((float) infoRect.getHeight());
        unitOfInfo.setBbox(infoBbox);

        unitOfInfo.setClazz("unit of information");
        unitOfInfo.setId("_" + UUID.randomUUID());

        return unitOfInfo;
    }

    public Arc getArc(LinkModel linkM) {

        /*
        In ACSN, some subunits of a complex have connections. They are not added to the global glyphMap index, so they
        cannot be referenced here.
         */
        if(!this.glyphMap.containsKey(linkM.getStart().getId())) {
            logger.error("No source for link: "+linkM.getId()+" missing glyph "+linkM.getStart().getId());
        }
        if(!this.glyphMap.containsKey(linkM.getEnd().getId())) {
            logger.error("No target for link: "+linkM.getId()+" missing glyph "+linkM.getEnd().getId());
        }

        return getArc(
                linkM.getLink(),
                this.glyphMap.get(linkM.getStart().getId()),
                this.glyphMap.get(linkM.getEnd().getId()),
                linkM.getSbgnClass(),
                linkM.getId());
    }

    public Arc getArc(Link link, Glyph source, Glyph target, String clazz, String id) {
        Arc arc1 = new Arc();
        arc1.setSource(source);
        arc1.setTarget(target);
        arc1.setClazz(clazz);
        arc1.setId(id);

        int pointCounts = link.getEditPoints().size();
        Point2D startPoint = link.getStart();
        Point2D endPoint = link.getEnd();
        System.out.println("ARC!!!! -> "+startPoint+" "+link.getEditPoints()+" "+endPoint);

        // start end
        Arc.Start s1 = new Arc.Start();
        s1.setX((float) startPoint.getX());
        s1.setY((float) startPoint.getY());
        arc1.setStart(s1);

        for(int i=0; i<link.getEditPoints().size() ; i++) {
            Point2D p = link.getEditPoints().get(i);
            Arc.Next next = new Arc.Next();
            next.setX((float) p.getX());
            next.setY((float) p.getY());
            arc1.getNext().add(next);
        }

        Arc.End e1 = new Arc.End();
        e1.setX((float) endPoint.getX());
        e1.setY((float) endPoint.getY());
        arc1.setEnd(e1);

        return arc1;
    }

    /**
     * Convert html Element into an SBGN notes object
     * @param notes
     * @return
     */
    public SBGNBase.Notes getSBGNNotes(Element notes) {
        SBGNBase.Notes newNotes = new SBGNBase.Notes();
        //System.out.println((Element) modelW.getModel().getNotes().getDomNode());
        newNotes.getAny().add(notes);
        return newNotes;
    }

    public GeneralModel convert(GeneralModel generalModel) throws ConversionException, ReadModelException {
        CellDesignerSBFCModel cdModel = (CellDesignerSBFCModel) generalModel;
        return new SBGNModel(this.toSbgn(cdModel.getModel()));
    }

    public String getResultExtension() {
        return null;
    }

    public String getName() {
        return null;
    }

    public String getDescription() {
        return null;
    }

    public String getHtmlDescription() {
        return null;
    }
}
