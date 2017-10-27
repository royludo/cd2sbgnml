package fr.curie.cd2sbgnml;

import com.sun.org.apache.xerces.internal.dom.ElementDefinitionImpl;
import fr.curie.cd2sbgnml.model.CompartmentModel;
import fr.curie.cd2sbgnml.xmlcdwrappers.StyleInfo;
import org.apache.xmlbeans.SchemaType;
import org.apache.xmlbeans.XmlAnySimpleType;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.apache.xmlbeans.impl.schema.SchemaTypeImpl;
import org.apache.xmlbeans.impl.values.XmlObjectBase;
import org.sbfc.converter.GeneralConverter;
import org.sbfc.converter.exceptions.ConversionException;
import org.sbfc.converter.exceptions.ReadModelException;
import org.sbfc.converter.models.GeneralModel;
import org.sbgn.bindings.Bbox;
import org.sbgn.bindings.Glyph;
import org.sbgn.bindings.Map;
import org.sbgn.bindings.Sbgn;
import org.sbml._2001.ns.celldesigner.*;
import org.sbml.sbml.level2.version4.*;
import org.sbml.sbml.level2.version4.ObjectFactory;
import org.sbml.sbml.level2.version4.OriginalModel.ListOfCompartments;
import org.sbml.x2001.ns.celldesigner.*;
import org.sbml.x2001.ns.celldesigner.CelldesignerModelVersionDocument.CelldesignerModelVersion;
import org.sbml.x2001.ns.celldesigner.impl.CelldesignerModelVersionDocumentImpl;
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


public class SBGNML2CD extends GeneralConverter {

    /**
     * Global translation factors that are to be applied to all elements
     */
    Rectangle2D mapBounds;


    public Sbml toCD(Sbgn sbgn) {

        // consider only the first map
        Map sbgnMap = sbgn.getMap().get(0);

        // init celldesigner file
        Sbml sbml = this.initFile(sbgnMap);

        // parse all the style info
        java.util.Map<String, StyleInfo> styleMap = new HashMap<>();
        boolean mapHasStyle = false;
        for(Element e: sbgnMap.getExtension().getAny()) {
            if(e.getTagName().equals("renderInformation")) {
                styleMap = SBGNUtils.mapStyleinfo(e);
                mapHasStyle = true;
            }
        }



        for(Glyph glyph: sbgnMap.getGlyph()){
            if(glyph.getClazz().equals("compartment")) {

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
                if(glyph.getNotes() != null) {
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



                SimpleEntry<Compartment, CompartmentAlias> cdElements = compM.getCDElements();
                Compartment cdComp = cdElements.getKey();
                CompartmentAlias cdCompAlias = cdElements.getValue();
                sbml.getModel().getListOfCompartments().getCompartment().add(cdComp);
                sbml.getModel().getAnnotation().getExtension().getListOfCompartmentAliases().getCompartmentAlias().add(cdCompAlias);

            }
        }



        return sbml;
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
