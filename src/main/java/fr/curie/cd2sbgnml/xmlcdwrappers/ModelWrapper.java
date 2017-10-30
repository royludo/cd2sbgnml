package fr.curie.cd2sbgnml.xmlcdwrappers;

import org.sbml._2001.ns.celldesigner.*;
import org.sbml.sbml.level2.version4.*;
import org.sbml.sbml.level2.version4.Species;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ModelWrapper {

    private final Logger logger = LoggerFactory.getLogger(ModelWrapper.class);


    private Model model;

    // basic lists
    private List<Species> listOfSpecies;
    private List<Compartment> listOfCompartments;
    private List<org.sbml._2001.ns.celldesigner.Species> listOfIncludedSpecies;
    private List<CompartmentAlias> listOfCompartmentAliases;
    private List<SpeciesAlias> listOfSpeciesAliases;
    private List<ListOfComplexSpeciesAliases.ComplexSpeciesAlias> listOfComplexSpeciesAliases;
    private List<Reaction> listOfReactions;
    private List<Protein> listOfProtein;
    private List<RNA> listOfRna;
    private List<AntisenseRNA> listOfAntisenseRna;
    private List<Gene> listOfGene;
    private List<Layer> listOfLayers;

    private HashMap<String, Species> mapOfSpecies;
    private HashMap<String, org.sbml._2001.ns.celldesigner.Species> mapOfIncludedSpecies;
    private HashMap<String, Protein> mapOfProtein;
    private HashMap<String, RNA> mapOfRna;
    private HashMap<String, AntisenseRNA> mapOfAntisenseRna;
    private HashMap<String, Gene> mapOfGene;

    private HashMap<String, List<CompartmentAlias>> compartment2aliasMap;
    private HashMap<String, List<SpeciesAlias>> species2aliasMap;
    private HashMap<String, List<ComplexSpeciesAlias>> complexSpecies2aliasMap;

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

    public static ModelWrapper create(Sbml sbmlDoc) {
        ModelWrapper modelW = new ModelWrapper();
        modelW.model = sbmlDoc.getModel();
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
        this.listOfSpecies = model.getListOfSpecies().getSpecies();
        this.listOfCompartments = model.getListOfCompartments().getCompartment();
        this.listOfCompartmentAliases =
                model.getAnnotation().getExtension().getListOfCompartmentAliases().getCompartmentAlias();
        this.listOfSpeciesAliases =
                model.getAnnotation().getExtension().getListOfSpeciesAliases().getSpeciesAlias();
        this.listOfComplexSpeciesAliases =
                model.getAnnotation().getExtension().getListOfComplexSpeciesAliases().getComplexSpeciesAlias();

        // unlike the others which are there but empty, listOfIncludedSpecies may not be there at all
        if(model.getAnnotation().getExtension().getListOfIncludedSpecies() != null) {
            this.listOfIncludedSpecies = model.getAnnotation().getExtension().getListOfIncludedSpecies().getSpecies();
        }
        else {
            this.listOfIncludedSpecies = new ArrayList<>();
        }

        // list reactions if present
        if(model.getListOfReactions() == null || model.getListOfReactions().getReaction().size()  == 0) {
            logger.warn("No reaction found.");
            this.listOfReactions = new ArrayList<>();
        }
        else {
            this.listOfReactions = model.getListOfReactions().getReaction();
        }

        this.listOfProtein = model.getAnnotation().getExtension().getListOfProteins().getProtein();
        this.listOfRna = model.getAnnotation().getExtension().getListOfRNAs().getRNA();
        this.listOfAntisenseRna = model.getAnnotation().getExtension().getListOfAntisenseRNAs().getAntisenseRNA();
        this.listOfGene = model.getAnnotation().getExtension().getListOfGenes().getGene();
        this.listOfLayers = model.getAnnotation().getExtension().getListOfLayers().getLayer();

        this.listofTextWrapper = new ArrayList<>();
        for(Layer layer: this.listOfLayers) {
            // loop through the texts of the layer, if texts are defined
            if(layer.getListOfTexts() != null) {
                for(LayerSpeciesAlias text: layer.getListOfTexts().getLayerSpeciesAlias()) {
                    this.listofTextWrapper.add(new TextWrapper(text, layer.isVisible()));
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
        for(org.sbml._2001.ns.celldesigner.Species species: this.listOfIncludedSpecies) {
            this.mapOfIncludedSpecies.put(species.getId(), species);
        }

        this.mapOfProtein = new HashMap<>();
        for(Protein pr: this.listOfProtein) {
            this.mapOfProtein.put(pr.getId(), pr);
        }

        this.mapOfRna = new HashMap<>();
        for(RNA pr: this.listOfRna) {
            this.mapOfRna.put(pr.getId(), pr);
        }

        this.mapOfAntisenseRna = new HashMap<>();
        for(AntisenseRNA pr: this.listOfAntisenseRna) {
            this.mapOfAntisenseRna.put(pr.getId(), pr);
        }

        this.mapOfGene = new HashMap<>();
        for(Gene pr: this.listOfGene) {
            this.mapOfGene.put(pr.getId(), pr);
        }


        this.compartment2aliasMap = new HashMap<>();
        for(CompartmentAlias alias: this.listOfCompartmentAliases) {
            if(!this.compartment2aliasMap.containsKey(alias.getCompartment())) {
                this.compartment2aliasMap.put(alias.getCompartment(), new ArrayList<>());
            }
            this.compartment2aliasMap.get(alias.getCompartment()).add(alias);
        }

        this.species2aliasMap = new HashMap<>();
        for(SpeciesAlias alias: this.listOfSpeciesAliases) {
            if(!this.species2aliasMap.containsKey(alias.getSpecies())) {
                this.species2aliasMap.put(alias.getSpecies(), new ArrayList<>());
            }
            this.species2aliasMap.get(alias.getSpecies()).add(alias);
        }

        this.complexSpecies2aliasMap = new HashMap<>();
        for(ComplexSpeciesAlias alias: this.listOfComplexSpeciesAliases) {
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
        for(org.sbml._2001.ns.celldesigner.Species species: this.listOfIncludedSpecies) {
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
        for(ComplexSpeciesAlias complexAlias: this.listOfComplexSpeciesAliases) {
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
        for(SpeciesAlias alias: this.listOfSpeciesAliases) {
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

    public List<org.sbml._2001.ns.celldesigner.Species> getListOfIncludedSpecies() {
        return listOfIncludedSpecies;
    }

    public org.sbml._2001.ns.celldesigner.Species getIncludedSpecies(String id) {
        return this.mapOfIncludedSpecies.get(id);
    }

    public List<CompartmentAlias> getListOfCompartmentAliases() {
        return listOfCompartmentAliases;
    }

    public List<SpeciesAlias> getListOfSpeciesAliases() {
        return listOfSpeciesAliases;
    }

    public List<ListOfComplexSpeciesAliases.ComplexSpeciesAlias> getListOfComplexSpeciesAliases() {
        return listOfComplexSpeciesAliases;
    }

    public List<Reaction> getListOfReactions() {
        return listOfReactions;
    }

    public List<Protein> getListOfProtein() {
        return listOfProtein;
    }

    public List<RNA> getListOfRna() {
        return listOfRna;
    }

    public List<AntisenseRNA> getListOfAntisenseRna() {
        return listOfAntisenseRna;
    }

    public List<Gene> getListOfGene() {
        return listOfGene;
    }

    public Protein getProtein(String id) {
        return this.mapOfProtein.get(id);
    }

    public RNA getRNA(String id) {
        return this.mapOfRna.get(id);
    }

    public AntisenseRNA getAntisenseRNA(String id) {
        return this.mapOfAntisenseRna.get(id);
    }

    public Gene getGene(String id) {
        return this.mapOfGene.get(id);
    }

    public List<CompartmentAlias> getCompartmentAliasFor(String compartmentId) {
        return this.compartment2aliasMap.get(compartmentId);
    }

    public List<SpeciesAlias> getSpeciesAliasFor(String speciesId) {
        return this.species2aliasMap.get(speciesId);
    }

    public List<ComplexSpeciesAlias> getComplexSpeciesAliasFor(String speciesId) {
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

    public List<Layer> getListOfLayers() {
        return listOfLayers;
    }

    public List<TextWrapper> getListofTextWrapper() {
        return listofTextWrapper;
    }


}
