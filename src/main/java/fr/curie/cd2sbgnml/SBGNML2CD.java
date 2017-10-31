package fr.curie.cd2sbgnml;

import fr.curie.cd2sbgnml.model.CompartmentModel;
import fr.curie.cd2sbgnml.model.ReactantModel;
import fr.curie.cd2sbgnml.xmlcdwrappers.AliasWrapper;
import fr.curie.cd2sbgnml.xmlcdwrappers.SpeciesWrapper;
import fr.curie.cd2sbgnml.xmlcdwrappers.StyleInfo;
import org.sbfc.converter.GeneralConverter;
import org.sbfc.converter.exceptions.ConversionException;
import org.sbfc.converter.exceptions.ReadModelException;
import org.sbfc.converter.models.GeneralModel;
import org.sbgn.bindings.Glyph;
import org.sbgn.bindings.Map;
import org.sbgn.bindings.Sbgn;
import org.sbgn.GlyphClazz;
import org.sbml._2001.ns.celldesigner.*;
import org.sbml.sbml.level2.version4.*;
import org.sbml.sbml.level2.version4.ObjectFactory;
import org.sbml.sbml.level2.version4.OriginalModel.ListOfCompartments;
import org.sbml.sbml.level2.version4.OriginalModel.ListOfReactions;
import org.sbml.sbml.level2.version4.OriginalModel.ListOfSpecies;
import org.sbml.sbml.level2.version4.Species;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import sun.java2d.pipe.SpanShapeRenderer;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.lang.annotation.Annotation;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.AbstractMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.HashMap;

import static org.sbgn.GlyphClazz.COMPARTMENT;
import static org.sbgn.GlyphClazz.MACROMOLECULE;


public class SBGNML2CD extends GeneralConverter {

    /**
     * Global translation factors that are to be applied to all elements
     */
    Rectangle2D mapBounds;
    Sbml sbml;
    boolean mapHasStyle;
    java.util.Map<String, StyleInfo> styleMap;


    public Sbml toCD(Sbgn sbgn) {

        // consider only the first map
        Map sbgnMap = sbgn.getMap().get(0);

        // init celldesigner file
        sbml = this.initFile(sbgnMap);

        // parse all the style info
        styleMap = new HashMap<>();
        mapHasStyle = false;
        for(Element e: sbgnMap.getExtension().getAny()) {
            if(e.getTagName().equals("renderInformation")) {
                styleMap = SBGNUtils.mapStyleinfo(e);
                mapHasStyle = true;
            }
        }



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
                    processSpecies(glyph, false, false);
                    break;
                case COMPLEX:
                case COMPLEX_MULTIMER:
                    processSpecies(glyph, false, true);
                    break;
            }
        }



        return sbml;
    }

    private void processSpecies(Glyph glyph, boolean isIncluded, boolean isComplex) {
        String label = glyph.getLabel() == null ? "": glyph.getLabel().getText();

        // first determine specific subtypes
        SpeciesWrapper.ReferenceType subType = null;
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
        speciesW.setCdClass(ReactantModel.getCdClass(glyph.getClazz(), subType));
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






    }

    private void processCompartment(Glyph glyph) {

        String label = glyph.getLabel() == null ? "": glyph.getLabel().getText();
        CompartmentModel compM = new CompartmentModel(
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
