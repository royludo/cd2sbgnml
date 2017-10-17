package fr.curie.cd2sbgnml;

import com.sun.org.apache.xerces.internal.dom.ElementDefinitionImpl;
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
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.xml.XMLNode;
import org.sbml.sbml.level2.version4.Model;
import org.sbml.sbml.level2.version4.ObjectFactory;
import org.sbml.sbml.level2.version4.Sbml;
import org.sbml.wrapper.ModelWrapper;
import org.sbml.x2001.ns.celldesigner.*;
import org.sbml.x2001.ns.celldesigner.CelldesignerModelVersionDocument.CelldesignerModelVersion;
import org.sbml.x2001.ns.celldesigner.impl.CelldesignerModelVersionDocumentImpl;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.tidy.DOMAttrImpl;
import org.w3c.tidy.DOMElementImpl;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;
import java.lang.annotation.Annotation;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.AbstractMap;
import java.util.AbstractMap.SimpleEntry;


public class SBGNML2CD extends GeneralConverter {


    public Sbml toCD(Sbgn sbgn) {

        // consider only the first map
        Map sbgnMap = sbgn.getMap().get(0);

        // init celldesigner file
        Sbml sbml = this.initFile(sbgnMap);


        for(Glyph glyph: sbgnMap.getGlyph()){

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
        ext.setModelVersion(BigDecimal.valueOf(4));

        SimpleEntry<Integer, Integer> bounds = getMapBounds(map);
        ModelDisplay modelDisplay = new ModelDisplay();
        modelDisplay.setSizeX(bounds.getKey().shortValue());
        modelDisplay.setSizeY(bounds.getValue().shortValue());
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

    /**
     * returns the size of a map
     * @param map
     * @return
     */
    public SimpleEntry<Integer, Integer> getMapBounds(Map map) {
        if(map.getGlyph().size() == 0) {
            return new SimpleEntry<>(0,0);
        }

        float minX, maxX, minY, maxY;
        Bbox firstBox = map.getGlyph().get(0).getBbox();
        minX = firstBox.getX();
        maxX = minX + firstBox.getW();
        minY = firstBox.getY();
        maxY = minY + firstBox.getH();

        for(Glyph glyph: map.getGlyph()){
            Bbox b = glyph.getBbox();
            float currentX = b.getX();
            float currentMaxX = b.getX() + b.getW();
            float currentY = b.getY();
            float currentMaxY = b.getY() + b.getH();


            minX = Float.min(currentX, minX);
            maxX = Float.max(currentMaxX, maxX);
            minY = Float.min(currentY, minY);
            maxY = Float.max(currentMaxY, maxY);
        }

        int sizeX = Math.round(maxX - minX) + 100; // add some margin
        int sizeY = Math.round(maxY - minY) + 100;
        return new SimpleEntry<>(sizeX, sizeY);
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
