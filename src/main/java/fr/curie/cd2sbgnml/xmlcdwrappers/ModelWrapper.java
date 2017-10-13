package fr.curie.cd2sbgnml.xmlcdwrappers;

import org.sbml.x2001.ns.celldesigner.*;
import org.sbml.x2001.ns.celldesigner.CelldesignerAntisenseRNADocument.CelldesignerAntisenseRNA;
import org.sbml.x2001.ns.celldesigner.CelldesignerCompartmentAliasDocument.CelldesignerCompartmentAlias;
import org.sbml.x2001.ns.celldesigner.CelldesignerComplexSpeciesAliasDocument.CelldesignerComplexSpeciesAlias;
import org.sbml.x2001.ns.celldesigner.CelldesignerGeneDocument.CelldesignerGene;
import org.sbml.x2001.ns.celldesigner.CelldesignerLayerDocument.CelldesignerLayer;
import org.sbml.x2001.ns.celldesigner.CelldesignerLayerSpeciesAliasDocument.CelldesignerLayerSpeciesAlias;
import org.sbml.x2001.ns.celldesigner.CelldesignerProteinDocument.CelldesignerProtein;
import org.sbml.x2001.ns.celldesigner.CelldesignerRNADocument.CelldesignerRNA;
import org.sbml.x2001.ns.celldesigner.CelldesignerSpeciesAliasDocument.CelldesignerSpeciesAlias;
import org.sbml.x2001.ns.celldesigner.CelldesignerSpeciesDocument.CelldesignerSpecies;
import org.sbml.x2001.ns.celldesigner.CompartmentDocument.Compartment;
import org.sbml.x2001.ns.celldesigner.ModelDocument.Model;
import org.sbml.x2001.ns.celldesigner.ReactionDocument.Reaction;
import org.sbml.x2001.ns.celldesigner.SpeciesDocument.Species;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class ModelWrapper {

    private final Logger logger = LoggerFactory.getLogger(ModelWrapper.class);


    private Model model;

    // basic lists
    private List<Species> listOfSpecies;
    private List<Compartment> listOfCompartments;
    private List<CelldesignerSpecies> listOfIncludedSpecies;
    private List<CelldesignerCompartmentAlias> listOfCompartmentAliases;
    private List<CelldesignerSpeciesAlias> listOfSpeciesAliases;
    private List<CelldesignerComplexSpeciesAlias> listOfComplexSpeciesAliases;
    private List<Reaction> listOfReactions;
    private List<CelldesignerProtein> listOfProtein;
    private List<CelldesignerRNA> listOfRna;
    private List<CelldesignerAntisenseRNA> listOfAntisenseRna;
    private List<CelldesignerGene> listOfGene;
    private List<CelldesignerLayer> listOfLayers;

    private HashMap<String, Species> mapOfSpecies;
    private HashMap<String, CelldesignerSpecies> mapOfIncludedSpecies;
    private HashMap<String, CelldesignerProtein> mapOfProtein;
    private HashMap<String, CelldesignerRNA> mapOfRna;
    private HashMap<String, CelldesignerAntisenseRNA> mapOfAntisenseRna;
    private HashMap<String, CelldesignerGene> mapOfGene;

    private HashMap<String, List<CelldesignerCompartmentAlias>> compartment2aliasMap;
    private HashMap<String, List<CelldesignerSpeciesAlias>> species2aliasMap;
    private HashMap<String, List<CelldesignerComplexSpeciesAlias>> complexSpecies2aliasMap;

    private List<SpeciesWrapper> listOfSpeciesWrapper; // doesn't contain included species!
    private List<AliasWrapper> listofAliasWrapper;
    private List<TextWrapper> listofTextWrapper;
    private HashMap<String, SpeciesWrapper> mapOfSpeciesWrapper;
    private HashMap<String, AliasWrapper> mapOfAliasWrapper;


    private HashMap<String, List<AliasWrapper>> complexSpeciesAlias2speciesAliasWrapper;
    //private HashMap<String, fr.curie.cd2sbgnml.xmlcdwrappers.AliasWrapper> globalAliasMap;

    private List<ReactionWrapper> listOfReactionWrapper;
    private HashMap<String, ReactionWrapper> mapOfReactionWrapper;
    private HashMap<String, List<ReactantWrapper>> alias2reactantWrapper;

    public static ModelWrapper create(SbmlDocument sbmlDoc) {
        ModelWrapper modelW = new ModelWrapper();
        modelW.model = sbmlDoc.getSbml().getModel();
        modelW.addBasicLists();
        modelW.addBasicMaps();
        modelW.addWrapperListsAndMaps();
        modelW.addComplexNestingIndex();
        modelW.addReactionStructures();
        return modelW;
    }

    /**
     * change arrays returned by the basic API into lists
     * for basic celldesigner API elements
     */
    private void addBasicLists() {
        this.listOfSpecies = Arrays .asList(model.getListOfSpecies().getSpeciesArray());
        this.listOfCompartments = Arrays.asList(model.getListOfCompartments().getCompartmentArray());
        this.listOfCompartmentAliases =
                Arrays.asList(model.getAnnotation().getCelldesignerListOfCompartmentAliases().getCelldesignerCompartmentAliasArray());
        this.listOfSpeciesAliases =
                Arrays.asList(model.getAnnotation().getCelldesignerListOfSpeciesAliases().getCelldesignerSpeciesAliasArray());
        this.listOfComplexSpeciesAliases =
                Arrays.asList(model.getAnnotation().getCelldesignerListOfComplexSpeciesAliases().getCelldesignerComplexSpeciesAliasArray());

        // unlike the others which are there but empty, listOfIncludedSpecies may not be there at all
        if(model.getAnnotation().getCelldesignerListOfIncludedSpecies() != null) {
            this.listOfIncludedSpecies =
                    Arrays.asList(model.getAnnotation().getCelldesignerListOfIncludedSpecies().getCelldesignerSpeciesArray());
        }
        else {
            this.listOfIncludedSpecies = new ArrayList<>();
        }

        // list reactions if present
        if(model.getListOfReactions() == null || model.getListOfReactions().sizeOfReactionArray() == 0) {
            logger.warn("No reaction found.");
            this.listOfReactions = new ArrayList<>();
        }
        else {
            this.listOfReactions = Arrays.asList(model.getListOfReactions().getReactionArray());
        }

        this.listOfProtein =
                Arrays.asList(model.getAnnotation().getCelldesignerListOfProteins().getCelldesignerProteinArray());
        this.listOfRna =
                Arrays.asList(model.getAnnotation().getCelldesignerListOfRNAs().getCelldesignerRNAArray());
        this.listOfAntisenseRna =
                Arrays.asList(model.getAnnotation().getCelldesignerListOfAntisenseRNAs().getCelldesignerAntisenseRNAArray());
        this.listOfGene =
                Arrays.asList(model.getAnnotation().getCelldesignerListOfGenes().getCelldesignerGeneArray());
        this.listOfLayers =
                Arrays.asList(model.getAnnotation().getCelldesignerListOfLayers().getCelldesignerLayerArray());

        this.listofTextWrapper = new ArrayList<>();
        for(CelldesignerLayer layer: this.listOfLayers) {
            // loop through the texts of the layer, if texts are defined
            if(layer.isSetCelldesignerListOfTexts()) {
                for(int i=0; i < layer.getCelldesignerListOfTexts().sizeOfCelldesignerLayerSpeciesAliasArray(); i++) {
                    CelldesignerLayerSpeciesAlias text = layer.getCelldesignerListOfTexts().getCelldesignerLayerSpeciesAliasArray(i);
                    this.listofTextWrapper.add(new TextWrapper(text, Boolean.parseBoolean(layer.getVisible().getStringValue())));
                }
            }
        }
    }

    /**
     * index celldesigner api elements through maps
     */
    private void addBasicMaps() {
        this.mapOfSpecies = new HashMap<>();
        for(Species species: this.listOfSpecies) {
            this.mapOfSpecies.put(species.getId(), species);
        }

        this.mapOfIncludedSpecies = new HashMap<>();
        for(CelldesignerSpecies species: this.listOfIncludedSpecies) {
            this.mapOfIncludedSpecies.put(species.getId(), species);
        }

        this.mapOfProtein = new HashMap<>();
        for(CelldesignerProtein pr: this.listOfProtein) {
            this.mapOfProtein.put(pr.getId(), pr);
        }

        this.mapOfRna = new HashMap<>();
        for(CelldesignerRNA pr: this.listOfRna) {
            this.mapOfRna.put(pr.getId(), pr);
        }

        this.mapOfAntisenseRna = new HashMap<>();
        for(CelldesignerAntisenseRNA pr: this.listOfAntisenseRna) {
            this.mapOfAntisenseRna.put(pr.getId(), pr);
        }

        this.mapOfGene = new HashMap<>();
        for(CelldesignerGene pr: this.listOfGene) {
            this.mapOfGene.put(pr.getId(), pr);
        }


        this.compartment2aliasMap = new HashMap<>();
        for(CelldesignerCompartmentAlias alias: this.listOfCompartmentAliases) {
            if(!this.compartment2aliasMap.containsKey(alias.getCompartment())) {
                this.compartment2aliasMap.put(alias.getCompartment(), new ArrayList<>());
            }
            this.compartment2aliasMap.get(alias.getCompartment()).add(alias);
        }

        this.species2aliasMap = new HashMap<>();
        for(CelldesignerSpeciesAlias alias: this.listOfSpeciesAliases) {
            if(!this.species2aliasMap.containsKey(alias.getSpecies())) {
                this.species2aliasMap.put(alias.getSpecies(), new ArrayList<>());
            }
            this.species2aliasMap.get(alias.getSpecies()).add(alias);
        }

        this.complexSpecies2aliasMap = new HashMap<>();
        for(CelldesignerComplexSpeciesAlias alias: this.listOfComplexSpeciesAliases) {
            if(!this.complexSpecies2aliasMap.containsKey(alias.getSpecies())) {
                this.complexSpecies2aliasMap.put(alias.getSpecies(), new ArrayList<>());
            }
            this.complexSpecies2aliasMap.get(alias.getSpecies()).add(alias);
        }
    }

    /**
     * create, store and index wrappers for celldesigner api elements
     */
    private void addWrapperListsAndMaps() {
        this.listOfSpeciesWrapper = new ArrayList<>();
        this.listofAliasWrapper = new ArrayList<>();
        this.mapOfSpeciesWrapper = new HashMap<>();
        this.mapOfAliasWrapper = new HashMap<>();

        logger.info("Wrapping "+this.listOfSpecies.size()+" species");
        for(Species species: this.listOfSpecies) {
            logger.debug("Parse species: "+species.getId());
            SpeciesWrapper speciesW = new SpeciesWrapper(species, this);
            this.listOfSpeciesWrapper.add(speciesW);
            this.mapOfSpeciesWrapper.put(speciesW.getId(), speciesW);
            for(AliasWrapper aliasW : speciesW.getAliases()) {
                this.listofAliasWrapper.add(aliasW);
                this.mapOfAliasWrapper.put(aliasW.getId(), aliasW);
            }
        }
        logger.info(this.listofAliasWrapper.size()+" alias wrapper added");

        logger.info("Wrapping "+this.listOfIncludedSpecies.size()+" included species");
        for(CelldesignerSpecies species: this.listOfIncludedSpecies) {
            logger.debug("Parse included species: "+species.getId());
            SpeciesWrapper speciesW = new SpeciesWrapper(species, this);
            this.listOfSpeciesWrapper.add(speciesW);
            this.mapOfSpeciesWrapper.put(speciesW.getId(), speciesW);
            for(AliasWrapper aliasW : speciesW.getAliases()) {
                this.listofAliasWrapper.add(aliasW);
                this.mapOfAliasWrapper.put(aliasW.getId(), aliasW);
            }
        }
        logger.info(this.listOfSpeciesWrapper.size()+" species wrapper total");
        logger.info(this.listofAliasWrapper.size()+" alias wrapper total");
        //        logger.info(this.mapOfAliasWrapper.get("c_a157").toString());
    }

    private void addComplexNestingIndex() {
        // complex nesting tree
        this.complexSpeciesAlias2speciesAliasWrapper = new HashMap<>();
        //this.globalAliasMap = new HashMap<>();
        for(CelldesignerComplexSpeciesAlias complexAlias: this.listOfComplexSpeciesAliases) {
            //logger.info("Fetching wrapper for complex alias: "+complexAlias.getId());
            AliasWrapper aliasW = this.getAliasWrapperFor(complexAlias.getId());
            if(aliasW.getComplexAlias() != null) {
                if(!this.complexSpeciesAlias2speciesAliasWrapper.containsKey(aliasW.getComplexAlias())) {
                    this.complexSpeciesAlias2speciesAliasWrapper.put(aliasW.getComplexAlias(), new ArrayList<>());
                }
                this.complexSpeciesAlias2speciesAliasWrapper.get(aliasW.getComplexAlias()).add(aliasW);
            }
            //globalAliasMap.put(complexAlias.getId(), aliasW);
        }
        for(CelldesignerSpeciesAlias alias: this.listOfSpeciesAliases) {
            //logger.info("Fetching wrapper for alias: "+alias.getId());
            AliasWrapper aliasW = this.getAliasWrapperFor(alias.getId());
            if(aliasW.getComplexAlias() != null) {
                if(!this.complexSpeciesAlias2speciesAliasWrapper.containsKey(aliasW.getComplexAlias())) {
                    this.complexSpeciesAlias2speciesAliasWrapper.put(aliasW.getComplexAlias(), new ArrayList<>());
                }
                this.complexSpeciesAlias2speciesAliasWrapper.get(aliasW.getComplexAlias()).add(aliasW);
            }
            //globalAliasMap.put(alias.getId(), aliasW);
        }
    }

    private void addReactionStructures() {
        this.listOfReactionWrapper = new ArrayList<>();
        this.mapOfReactionWrapper = new HashMap<>();
        this.alias2reactantWrapper = new HashMap<>();

        for(Reaction reaction: this.listOfReactions) {
            logger.info("Parse reaction "+reaction.getId());
            ReactionWrapper reactionW = new ReactionWrapper(reaction, this);
            this.listOfReactionWrapper.add(reactionW);
            this.mapOfReactionWrapper.put(reactionW.getId(), reactionW);
            for(ReactantWrapper reactantW: reactionW.getReactantList()) {
                if(!this.alias2reactantWrapper.containsKey(reactantW.getAliasW().getId())) {
                    this.alias2reactantWrapper.put(reactantW.getAliasW().getId(), new ArrayList<>());
                }
                this.alias2reactantWrapper.get(reactantW.getAliasW().getId()).add(reactantW);
            }
        }
    }

    public Model getModel() {
        return model;
    }

    public List<Species> getListOfSpecies() {
        return listOfSpecies;
    }

    public Species getSpecies(String id) {
        return this.mapOfSpecies.get(id);
    }

    public List<Compartment> getListOfCompartments() {
        return listOfCompartments;
    }

    public List<CelldesignerSpecies> getListOfIncludedSpecies() {
        return listOfIncludedSpecies;
    }

    public CelldesignerSpecies getIncludedSpecies(String id) {
        return this.mapOfIncludedSpecies.get(id);
    }

    public List<CelldesignerCompartmentAlias> getListOfCompartmentAliases() {
        return listOfCompartmentAliases;
    }

    public List<CelldesignerSpeciesAlias> getListOfSpeciesAliases() {
        return listOfSpeciesAliases;
    }

    public List<CelldesignerComplexSpeciesAlias> getListOfComplexSpeciesAliases() {
        return listOfComplexSpeciesAliases;
    }

    public List<Reaction> getListOfReactions() {
        return listOfReactions;
    }

    public List<CelldesignerProtein> getListOfProtein() {
        return listOfProtein;
    }

    public List<CelldesignerRNA> getListOfRna() {
        return listOfRna;
    }

    public List<CelldesignerAntisenseRNA> getListOfAntisenseRna() {
        return listOfAntisenseRna;
    }

    public List<CelldesignerGene> getListOfGene() {
        return listOfGene;
    }

    public CelldesignerProtein getProtein(String id) {
        return this.mapOfProtein.get(id);
    }

    public CelldesignerRNA getRNA(String id) {
        return this.mapOfRna.get(id);
    }

    public CelldesignerAntisenseRNA getAntisenseRNA(String id) {
        return this.mapOfAntisenseRna.get(id);
    }

    public CelldesignerGene getGene(String id) {
        return this.mapOfGene.get(id);
    }

    public List<CelldesignerCompartmentAlias> getCompartmentAliasFor(String compartmentId) {
        return this.compartment2aliasMap.get(compartmentId);
    }

    public List<CelldesignerSpeciesAlias> getSpeciesAliasFor(String speciesId) {
        return this.species2aliasMap.get(speciesId);
    }

    public List<CelldesignerComplexSpeciesAlias> getComplexSpeciesAliasFor(String speciesId) {
        return this.complexSpecies2aliasMap.get(speciesId);
    }

    public List<AliasWrapper> getIncludedAliasWrapperFor(String complexAliasId) {
        return this.complexSpeciesAlias2speciesAliasWrapper.get(complexAliasId);
    }

    public AliasWrapper getAliasWrapperFor(String aliasId) {
        return this.mapOfAliasWrapper.get(aliasId);
    }

    public SpeciesWrapper getSpeciesWrapperFor(String speciesId) {
        return this.mapOfSpeciesWrapper.get(speciesId);
    }

    public List<SpeciesWrapper> getListOfSpeciesWrapper() {
        return listOfSpeciesWrapper;
    }

    public List<AliasWrapper> getListofAliasWrapper() {
        return listofAliasWrapper;
    }

    public List<ReactionWrapper> getListOfReactionWrapper() {
        return listOfReactionWrapper;
    }

    public ReactionWrapper getReactionWrapperFor(String reactionId) {
        return this.mapOfReactionWrapper.get(reactionId);
    }

    public List<ReactantWrapper> getReactionWrappersForAlias(String aliasId) {
        return this.alias2reactantWrapper.get(aliasId);
    }

    public List<CelldesignerLayer> getListOfLayers() {
        return listOfLayers;
    }

    public List<TextWrapper> getListofTextWrapper() {
        return listofTextWrapper;
    }


}
