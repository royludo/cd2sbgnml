package fr.curie.cd2sbgnml.xmlcdwrappers;

import org.apache.xmlbeans.SchemaType;
import org.apache.xmlbeans.impl.schema.SchemaTypeImpl;
import org.sbgn.bindings.Glyph;
import org.sbml._2001.ns.celldesigner.*;
import org.sbml.sbml.level2.version4.Species;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * wraps species and includedSpecies as they have a lot in common
 */
public class SpeciesWrapper {

    private final Logger logger = LoggerFactory.getLogger(SpeciesWrapper.class);

    public enum ReferenceType {
        // protein types
        GENERIC, ION_CHANNEL, RECEPTOR, TRUNCATED,
        // for the 3 others, just 1 type possible
        GENE, RNA, ANTISENSE_RNA}

    private boolean isIncludedSpecies;
    private boolean isComplex;

    private String id; // caution, celldesigner doesn't like - in ids
    private String name;
    private String compartment;
    private String complex;
    private String cdClass;
    private int multimer;
    private String structuralState;
    private ReferenceType type;
    private Element notes;
    private Element referenceNotes;
    private Element annotations;

    private List<AliasWrapper> aliases;
    private List<ResidueWrapper> residues;

    public SpeciesWrapper(String id, String name, ReferenceType type) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.aliases = new ArrayList<>();
        this.residues = new ArrayList<>();
    }

    public SpeciesWrapper(Species species, ModelWrapper modelW) {
        this.isIncludedSpecies = false;
        this.cdClass = species.getAnnotation().getExtension().getSpeciesIdentity().getClazz();
        this.id = species.getId();
        this.name = species.getName();
        this.compartment = species.getCompartment();
        this.complex = null;
        this.notes = Utils.getNotes(species.getNotes());
        this.annotations = Utils.getRDFAnnotations(species.getAnnotation().getAny());
        System.out.println("annotation for species: "+this.id+" "+this.annotations);

        this.commonConstructor(species.getAnnotation().getExtension().getSpeciesIdentity(), modelW);

    }

    public SpeciesWrapper(org.sbml._2001.ns.celldesigner.Species species, ModelWrapper modelW) {
        this.isIncludedSpecies = true;
        this.cdClass = species.getAnnotation().getSpeciesIdentity().getClazz();
        this.id = species.getId();
        this.name = species.getName();
        this.complex = species.getAnnotation().getComplexSpecies();
        this.compartment = null;
        this.notes = Utils.getNotes(species.getNotes());
        //this.annotations = Utils.getRDFAnnotations(species.getAnnotation().); TODO not applicable ?

        this.commonConstructor(species.getAnnotation().getSpeciesIdentity(), modelW);
    }


    public SpeciesWrapper(Glyph sbgnGlyph) {
        this(sbgnGlyph, null);
    }

    public SpeciesWrapper(Glyph sbgnGlyph, Glyph parentGlyph) {
        this.id = "s"+sbgnGlyph.getId();

        if(sbgnGlyph.getLabel() != null) {
            this.name = sbgnGlyph.getLabel().getText();
        }
        else {
            this.name = this.id;
        }

        if(sbgnGlyph.getCompartmentRef() != null) {
            this.compartment = ((Glyph) sbgnGlyph.getCompartmentRef()).getId();
        }
        else {
            this.compartment = "default";
        }

        if(parentGlyph != null) {
            this.isIncludedSpecies = true;
            this.complex = "cs"+parentGlyph.getId();
        }
        else {
            this.isIncludedSpecies = false;
            this.complex = null;
        }



    }

    private void commonConstructor(SpeciesIdentity identity, ModelWrapper modelW) {
        this.multimer = 1; // default to 1 if nothing else found
        this.residues = new ArrayList<>();
        this.aliases = new ArrayList<>();
        this.isComplex = this.cdClass.equals("COMPLEX");


        if(this.isComplex) {
            logger.debug("Species has "+modelW.getComplexSpeciesAliasFor(this.id).size()+" complexSpeciesAliases");
            for(ComplexSpeciesAlias complexAlias : modelW.getComplexSpeciesAliasFor(this.id)) {
                if (complexAlias == null) {
                    continue;
                }
                logger.debug("Parse complex alias: " + complexAlias.getId());
                this.aliases.add(new AliasWrapper(complexAlias, this));
            }
        }
        //else {
        /*
         * here we shouldn't have to check normal aliases after complex aliases.
         * normally complex species should only have complex aliases, and not additional normal aliases
         * this only happens in ACSN
         */
        if(modelW.getSpeciesAliasFor(this.id) != null) {
            if(this.isComplex) {
                logger.warn("Complex species: "+this.id+" shouldn't have non-complex aliases");
            }

            logger.debug("Species has "+modelW.getSpeciesAliasFor(this.id).size()+" speciesAliases");
            for(SpeciesAlias alias : modelW.getSpeciesAliasFor(this.id)) {
                if (alias == null) {
                    continue;
                }
                logger.debug("Parse alias: " + alias.getId());
                this.aliases.add(new AliasWrapper(alias, this));
            }
        }

        // get information from the species' reference
        List<ModificationResidue> listOfReferenceModif;
        HashMap<String, ResidueWrapper> mapOfReferenceModif = new HashMap<>();
        if(identity.getProteinReference() != null) {
            String protId = identity.getProteinReference();
            Protein prot = modelW.getProtein(protId);

            if(prot.getListOfModificationResidues() != null) {
                // loop through reference residues
                listOfReferenceModif = prot.getListOfModificationResidues().getModificationResidue();

                mapOfReferenceModif = new HashMap<>();
                for (ModificationResidue modif : listOfReferenceModif) {
                    System.out.println("Residue found for "+prot.getId()+" resid "+modif.getId()+" angle "+modif.getAngle());
                    ResidueWrapper residueWrapper = new ResidueWrapper(modif.getId());
                    residueWrapper.angle = modif.getAngle().floatValue();
                    if(modif.getName() != null) {
                        residueWrapper.name = modif.getName();
                    }
                    mapOfReferenceModif.put(residueWrapper.id, residueWrapper);
                }
                System.out.println(mapOfReferenceModif.size()+" res for protein "+protId);
                logger.debug(mapOfReferenceModif.size()+" res for protein "+protId);
            }

            this.type = getTypeFromString(prot.getType());

            // manage protein reference notes
            this.referenceNotes = Utils.getNotes(prot.getNotes());
        }
        else if(identity.getRnaReference() != null) {
            String rnaId = identity.getRnaReference();
            RNA rna = modelW.getRNA(rnaId);
            this.type = getTypeFromString(rna.getType());

            if(rna.getListOfRegions() != null) {
                mapOfReferenceModif = mapOfRegion(rna.getListOfRegions());
            }

            // manage reference notes
            this.referenceNotes = Utils.getNotes(rna.getNotes());

        }
        else if(identity.getAntisensernaReference() != null) {
            String asrnaId = identity.getAntisensernaReference();
            AntisenseRNA asrna = modelW.getAntisenseRNA(asrnaId);
            this.type = getTypeFromString(asrna.getType());

            if(asrna.getListOfRegions() != null) {
                // loop through reference list of regions
                mapOfReferenceModif = mapOfRegion(asrna.getListOfRegions());
            }

            // manage reference notes
            this.referenceNotes = Utils.getNotes(asrna.getNotes());

        }
        else if(identity.getGeneReference() != null) {
            String geneId = identity.getGeneReference();
            Gene gene = modelW.getGene(geneId);
            this.type = getTypeFromString(gene.getType());

            if(gene.getListOfRegions() != null) {
                // loop through reference list of regions
                mapOfReferenceModif = mapOfRegion(gene.getListOfRegions());
                System.out.println("GENE MODIF COUNT: "+mapOfReferenceModif.size()+" "+gene.getListOfRegions().getRegion().size());

            }


            // manage reference notes
            this.referenceNotes = Utils.getNotes(gene.getNotes());
        }


        if(identity.getState() != null) {
            State state = identity.getState();

            // parse multimer and infounit
            if(state.getHomodimer() != null) {
                this.multimer = state.getHomodimer().intValue();
            }

            if(state.getListOfStructuralStates() != null) {
                // assume that there is only 1 state per species
                this.structuralState = state.getListOfStructuralStates().getStructuralState().getStructuralState();
            }

            // parse state variable/residues
            if(state.getListOfModifications() != null) {

                // list and map this species' residues
                List<ListOfModifications.Modification> listOfModif =
                        state.getListOfModifications().getModification();


                // loop through the species' residues
                for (ListOfModifications.Modification modif : listOfModif) {
                    System.out.println("adding state: "+modif.getState()+" for res "+modif.getResidue());
                    String residueId = modif.getResidue();
                    ResidueWrapper residueWrapper = mapOfReferenceModif.get(residueId);
                    /*
                    In ACSN, residueWrapper might produce null result, because some residue listed in the species
                    does not exist in the referenced protein ex: e_p47 doesn't list residue d_rs1, but its species
                    has a residue d_rs1.
                     */
                    if(residueWrapper != null) {
                        residueWrapper.state = modif.getState();
                    }
                    else {
                        logger.error("Residue "+residueId+" doesn't exist in referenced protein.");
                    }
                }

            }

        }
        // finally set this species' residue wrapper list
        this.residues.addAll(mapOfReferenceModif.values());
        System.out.println("final residue size for species "+this.getId()+" : "+this.residues.size());

    }

    /**
     * Getting a list of region is impossible because of broken Binom api, so we use raw NodeList
     * @deprecated
     * @param listOfNode NodeList of children of the RNA/gene/ASRNA! (not of the listOfRegions)
     * @return
     */
    private HashMap<String, ResidueWrapper> mapOfRegion(NodeList listOfNode) {
        HashMap<String, ResidueWrapper> mapOfReferenceModif = new HashMap<>();


        for(int i=0; i < listOfNode.getLength(); i++) {
            Node n = listOfNode.item(i);
            if(n.getNodeName().equals("celldesigner_listOfRegions")) {
                Node listOfRegions = n;
                for(int j=0; j < listOfRegions.getChildNodes().getLength(); j++) {
                    Node n2 = listOfRegions.getChildNodes().item(j);
                    if(n2.getNodeName().equals("celldesigner_region")) {
                        String type = n2.getAttributes().getNamedItem("type").getNodeValue();
                        String id = n2.getAttributes().getNamedItem("id").getNodeValue();
                        String pos = n2.getAttributes().getNamedItem("pos").getNodeValue();
                        if(type.equals("Modification Site")) {
                            ResidueWrapper residueWrapper = new ResidueWrapper(id);
                            residueWrapper.relativePos = Float.parseFloat(pos);
                            residueWrapper.useAngle = false;
                            if(n2.getAttributes().getNamedItem("name") != null) {
                                residueWrapper.name = n2.getAttributes().getNamedItem("name").getNodeValue();
                            }
                            mapOfReferenceModif.put(residueWrapper.id, residueWrapper);
                        }
                    }
                }
            }
        }

        return mapOfReferenceModif;
    }

    private HashMap<String, ResidueWrapper> mapOfRegion(ListOfRegions listOfRegions) {
        HashMap<String, ResidueWrapper> mapOfReferenceModif = new HashMap<>();

        for(Region region: listOfRegions.getRegion()) {
            String type = region.getType();
            String id = region.getId();
            float pos = region.getPos().floatValue();
            if(type.equals("Modification Site")) {
                ResidueWrapper residueWrapper = new ResidueWrapper(id);
                residueWrapper.relativePos = pos;
                residueWrapper.useAngle = false;
                if(region.getName() != null) {
                    residueWrapper.name = region.getName();
                }
                mapOfReferenceModif.put(residueWrapper.id, residueWrapper);
            }
        }
        return mapOfReferenceModif;
    }

    public Object getCDSpecies(String referenceId) {
        if(this.isIncludedSpecies()) {
            return this.getCDIncludedSpecies(referenceId);
        }
        else {
            return this.getCDNormalSpecies(referenceId);
        }
    }

    public Species getCDNormalSpecies(String referenceId) {
        Species species = new Species();
        species.setId(this.getId());
        species.setMetaid(this.getId());
        // CellDesigner always wants a name in the element even if the name is empty (example: degraded)
        species.setName(this.getName().isEmpty() ? this.getId() : this.getName());
        species.setCompartment(this.getCompartment());
        species.setInitialAmount(0d);

        SpeciesAnnotationType speciesAnnot = new SpeciesAnnotationType();
        species.setAnnotation(speciesAnnot);

        SpeciesAnnotationType.Extension ext = new SpeciesAnnotationType.Extension();
        speciesAnnot.setExtension(ext);
        ext.setPositionToCompartment("inside");
        ext.setSpeciesIdentity(getIdentity(referenceId));

        return species;
    }

    public org.sbml._2001.ns.celldesigner.Species getCDIncludedSpecies(String referenceId) {
        org.sbml._2001.ns.celldesigner.Species species = new org.sbml._2001.ns.celldesigner.Species();
        species.setId(this.getId());
        species.setName(this.getName().isEmpty() ? this.getId() : this.getName());

        org.sbml._2001.ns.celldesigner.Species.Annotation speciesAnnot = new org.sbml._2001.ns.celldesigner.Species.Annotation();
        species.setAnnotation(speciesAnnot);

        speciesAnnot.setComplexSpecies(this.getComplex());
        speciesAnnot.setSpeciesIdentity(getIdentity(referenceId));

        return species;
    }

    private SpeciesIdentity getIdentity(String referenceId) {
        SpeciesIdentity ident = new SpeciesIdentity();
        ident.setClazz(this.getCdClass());

        if(this.getType() == null) {
            ident.setName(this.getName().isEmpty() ? this.getId() : this.getName());
        }
        else {
            switch (this.getType()) {
                case GENERIC:
                case RECEPTOR:
                case TRUNCATED:
                case ION_CHANNEL:
                    ident.setProteinReference(referenceId);
                    break;
                case RNA:
                    ident.setRnaReference(referenceId);
                    break;
                case GENE:
                    ident.setGeneReference(referenceId);
                    break;
                case ANTISENSE_RNA:
                    ident.setAntisensernaReference(referenceId);
                    break;
            }
        }

        if(this.getMultimer() > 0) {
            State state = new State();
            ident.setState(state);

            state.setHomodimer(BigInteger.valueOf(this.getMultimer()));
        }

        return ident;
    }

    public boolean isIncludedSpecies() {
        return isIncludedSpecies;
    }

    public boolean isComplex() {
        return isComplex;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCompartment() {
        return compartment;
    }

    public String getComplex() {
        return complex;
    }

    public String getCdClass() {
        return cdClass;
    }

    public List<AliasWrapper> getAliases() {
        return aliases;
    }

    public int getMultimer() {
        return multimer;
    }

    public String getStructuralState() {
        return structuralState;
    }

    public List<ResidueWrapper> getResidues() {
        return residues;
    }

    public static ReferenceType getTypeFromString(String t){
        switch(t) {
            case "GENERIC": return ReferenceType.GENERIC;
            case "ION_CHANNEL": return ReferenceType.ION_CHANNEL;
            case "RECEPTOR": return ReferenceType.RECEPTOR;
            case "TRUNCATED": return ReferenceType.TRUNCATED;
            case "GENE": return ReferenceType.GENE;
            case "RNA": return ReferenceType.RNA;
            case "ANTISENSE_RNA": return ReferenceType.ANTISENSE_RNA;
            default: throw new IllegalArgumentException("Value: "+t+" is not a valid type. Should be one of" +
                    Arrays.toString(ReferenceType.values()));
        }
    }

    public ReferenceType getType() {
        return type;
    }


    public Element getNotes() {
        return notes;
    }

    public Element getReferenceNotes() {
        return referenceNotes;
    }

    public Element getAnnotations() {
        return annotations;
    }

    public void setIncludedSpecies(boolean includedSpecies) {
        isIncludedSpecies = includedSpecies;
    }

    public void setComplex(boolean complex) {
        isComplex = complex;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setCompartment(String compartment) {
        this.compartment = compartment;
    }

    public void setComplex(String complex) {
        this.complex = complex;
    }

    public void setCdClass(String cdClass) {
        this.cdClass = cdClass;
    }

    public void setMultimer(int multimer) {
        this.multimer = multimer;
    }

    public void setStructuralState(String structuralState) {
        this.structuralState = structuralState;
    }

    public void setType(ReferenceType type) {
        this.type = type;
    }

    public void setNotes(Element notes) {
        this.notes = notes;
    }

    public void setReferenceNotes(Element referenceNotes) {
        this.referenceNotes = referenceNotes;
    }

    public void setAnnotations(Element annotations) {
        this.annotations = annotations;
    }

    public void setAliases(List<AliasWrapper> aliases) {
        this.aliases = aliases;
    }

    public void setResidues(List<ResidueWrapper> residues) {
        this.residues = residues;
    }
}
