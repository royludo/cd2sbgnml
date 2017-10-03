import org.sbfc.converter.GeneralConverter;
import org.sbfc.converter.exceptions.ConversionException;
import org.sbfc.converter.exceptions.ReadModelException;
import org.sbfc.converter.models.GeneralModel;
import org.sbfc.converter.models.SBGNModel;
import org.sbgn.bindings.*;
import org.sbml.x2001.ns.celldesigner.CelldesignerBoundsDocument.CelldesignerBounds;
import org.sbml.x2001.ns.celldesigner.CelldesignerCompartmentAliasDocument.CelldesignerCompartmentAlias;
import org.sbml.x2001.ns.celldesigner.*;
import org.sbml.x2001.ns.celldesigner.CelldesignerPointDocument.CelldesignerPoint;
import org.sbml.x2001.ns.celldesigner.CompartmentDocument.Compartment;
import org.sbml.x2001.ns.celldesigner.ReactionDocument.Reaction;
import org.sbml.x2001.ns.celldesigner.SpeciesDocument.Species;
import sun.awt.image.ImageWatched;

import javax.xml.bind.Element;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class CD2SBGNML extends GeneralConverter {

    List<Glyph> glyphList;
    HashMap<String, Glyph> glyphMap;

    public Sbgn toSbgn( SbmlDocument sbmlDoc) {
        Sbgn sbgn = new Sbgn();
        Map map = new Map();
        sbgn.getMap().add(map);

        ModelWrapper modelW = ModelWrapper.create(sbmlDoc);

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
        for(Species species: modelW.getListOfSpecies()) {
            SpeciesWrapper speciesW = new SpeciesWrapper(species, modelW);
            processSpecies(speciesW, modelW, map);
        }

        // included species section
        /*for(CelldesignerSpecies includedSpecies: modelW.getListOfIncludedSpecies()) {
            System.out.println("included");
            System.out.println(includedSpecies.getId()+" "+includedSpecies.getName().getStringValue());
            System.out.println("is in: "+includedSpecies.getCelldesignerAnnotation().getCelldesignerComplexSpecies().getDomNode().getChildNodes().item(0).getNodeValue());

            SpeciesWrapper includedW = new SpeciesWrapper(includedSpecies, modelW);
            processSpecies(includedW, modelW, map);

        }*/

        // reactions
        for(Reaction reaction: modelW.getListOfReactions()) {
            ReactionWrapper reactionW = modelW.getReactionWrapperFor(reaction.getId());


            // PROCESS

            System.out.println(reactionW.getId()+" "+reactionW.getReactantList().size());
            //System.out.println("branch ? "+reactionW.isBranchType()+" right: "+reactionW.isBranchTypeRight()+" left: "+reactionW.isBranchTypeLeft());
            Point2D processCoord = reactionW.getProcess().getCoords();
            //System.out.println(processCoord);


            Glyph processGlyph = new Glyph();
            processGlyph.setClazz("process");
            String processId = "pr"+
                    reactionW.getBaseReactants().get(0).getAliasW().getId()+"_"+
                    reactionW.getBaseProducts().get(0).getAliasW().getId();
            processGlyph.setId(processId);

            Bbox processBbox = new Bbox();
            processBbox.setX((float) processCoord.getX() - Process.PROCESS_SIZE / 2);
            processBbox.setY((float) processCoord.getY() - Process.PROCESS_SIZE / 2);
            processBbox.setH(Process.PROCESS_SIZE);
            processBbox.setW(Process.PROCESS_SIZE);
            processGlyph.setBbox(processBbox);

            glyphList.add(processGlyph);
            glyphMap.put(processId, processGlyph);
            map.getGlyph().add(processGlyph);



            ReactantWrapper baseReactant = reactionW.getBaseReactants().get(0);
            ReactantWrapper baseProduct = reactionW.getBaseProducts().get(0);
            // ARCS

            if(!reactionW.isBranchType()){
                Glyph source1 = glyphMap.get(baseReactant.getAliasW().getSpeciesId()+"_"+baseReactant.getAliasW().getId());
                Glyph target1 = glyphMap.get(processId);

                map.getArc().add(getArc(reactionW.getBaseLink().get(0), source1, target1, "consumption"));

                Glyph source2 = glyphMap.get(processId);
                Glyph target2 = glyphMap.get(baseProduct.getAliasW().getSpeciesId()+"_"+baseProduct.getAliasW().getId());

                map.getArc().add(getArc(reactionW.getBaseLink().get(1), source2, target2, "production"));
            }
            else if(reactionW.isBranchTypeLeft()) {
                System.out.println("REACTION: "+reactionW.getId()+" "+reactionW.getBaseLink());

                Glyph source1 = glyphMap.get(baseReactant.getAliasW().getSpeciesId()+"_"+baseReactant.getAliasW().getId());
                Glyph target1 = glyphMap.get(processId);

                map.getArc().add(getArc(reactionW.getBaseLink().get(0), source1, target1, "consumption"));

                ReactantWrapper baseReactant2 = reactionW.getBaseReactants().get(1);
                Glyph source2 = glyphMap.get(baseReactant2.getAliasW().getSpeciesId()+"_"+baseReactant2.getAliasW().getId());
                Glyph target2 = glyphMap.get(processId);

                map.getArc().add(getArc(reactionW.getBaseLink().get(1), source2, target2, "consumption"));


                Glyph source3 = glyphMap.get(processId);
                Glyph target3 = glyphMap.get(baseProduct.getAliasW().getSpeciesId()+"_"+baseProduct.getAliasW().getId());

                map.getArc().add(getArc(reactionW.getBaseLink().get(2), source3, target3, "production"));
            }
            else {
                System.out.println("REACTION: "+reactionW.getId()+" "+reactionW.getBaseLink());

                Glyph source1 = glyphMap.get(baseReactant.getAliasW().getSpeciesId()+"_"+baseReactant.getAliasW().getId());
                Glyph target1 = glyphMap.get(processId);

                map.getArc().add(getArc(reactionW.getBaseLink().get(0), source1, target1, "consumption"));


                Glyph source2 = glyphMap.get(processId);
                Glyph target2 = glyphMap.get(baseProduct.getAliasW().getSpeciesId()+"_"+baseProduct.getAliasW().getId());

                map.getArc().add(getArc(reactionW.getBaseLink().get(1), source2, target2, "production"));

                ReactantWrapper baseProduct2 = reactionW.getBaseProducts().get(1);
                Glyph source3 = glyphMap.get(processId);
                Glyph target3 = glyphMap.get(baseProduct2.getAliasW().getSpeciesId()+"_"+baseProduct2.getAliasW().getId());

                map.getArc().add(getArc(reactionW.getBaseLink().get(2), source3, target3, "production"));

            }

            for(ReactantWrapper reactantW: reactionW.getModifiers()) {
                System.out.println("MODIFIER "+reactantW.getAliasW().getId());

                Glyph source = glyphMap.get(reactantW.getAliasW().getSpeciesId()+"_"+reactantW.getAliasW().getId());
                Glyph target = glyphMap.get(processId);

                map.getArc().add(getArc(reactantW.getLink(), source, target, reactantW.getLink().getSbgnClass()));
            }
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
                    compBbox.setX(pointX);
                    compBbox.setY(pointY);
                    compBbox.setW(mapSizeX - pointX);
                    compBbox.setH(mapSizeY - pointY);
                } else {
                    compBbox.setX(Float.parseFloat(cdBounds.getX()));
                    compBbox.setY(Float.parseFloat(cdBounds.getY()));
                    compBbox.setH(Float.parseFloat(cdBounds.getH()));
                    compBbox.setW(Float.parseFloat(cdBounds.getW()));
                }
                compGlyph.setBbox(compBbox);

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
        Glyph glyph = getGlyph(species, bounds, species.getId()+"_"+alias.getId());

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
                    SpeciesWrapper includedSpecies = new SpeciesWrapper(modelW.getIncludedSpecies(includedAlias.getSpeciesId()), modelW);
                    Glyph includedGlyph = processSpeciesAlias(includedSpecies, includedAlias, modelW, map);
                    glyph.getGlyph().add(includedGlyph);
                }
            }
        }

        return glyph;
    }

    public void processSpecies(SpeciesWrapper species, ModelWrapper modelW, Map map) {
        for(AliasWrapper alias : species.getAliases()) {
            Glyph glyph = processSpeciesAlias(species, alias, modelW, map);

            // keep references
            glyphList.add(glyph);
            glyphMap.put(glyph.getId(), glyph);
            // add to map
            map.getGlyph().add(glyph);
        }
    }

    public Glyph getGlyph(SpeciesWrapper species, CelldesignerBounds bounds, String id) {
        System.out.println(species.getId()+" "+species.getName()+" "+species.getCompartment());
        //System.out.println(bounds);

        Glyph glyph = new Glyph();

        // basic info
        glyph.setClazz(species.getSbgnClass());
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
        bbox.setX(Float.parseFloat(bounds.getX()));
        bbox.setY(Float.parseFloat(bounds.getY()));
        bbox.setH(Float.parseFloat(bounds.getH()));
        bbox.setW(Float.parseFloat(bounds.getW()));
        glyph.setBbox(bbox);

        return glyph;
    }

    public Arc getArc(LinkWrapper linkW, Glyph source, Glyph target, String clazz) {
        Arc arc1 = new Arc();
        arc1.setSource(source);
        arc1.setTarget(target);
        arc1.setClazz(clazz);

        int pointCounts = linkW.getSbgnSpacePointList().size();
        Point2D startPoint = linkW.getSbgnSpacePointList().get(0);
        Point2D endPoint = linkW.getSbgnSpacePointList().get(pointCounts - 1);

        // start end
        Arc.Start s1 = new Arc.Start();
        s1.setX((float) startPoint.getX());
        s1.setY((float) startPoint.getY());
        arc1.setStart(s1);

        for(int i=1; i<pointCounts - 1; i++) {
            Point2D p = linkW.getSbgnSpacePointList().get(i);
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
