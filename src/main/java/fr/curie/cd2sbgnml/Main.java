package fr.curie.cd2sbgnml;

import fr.curie.cd2sbgnml.xmlcdwrappers.Utils;
import org.sbfc.converter.exceptions.ConversionException;
import org.sbfc.converter.exceptions.ReadModelException;
import org.sbfc.converter.exceptions.WriteModelException;
import org.sbgn.SbgnUtil;
/*import org.sbml.sbml.level2.version4.Sbml;
import org.sbml.wrapper.ModelWrapper;
import org.sbml.wrapper.ObjectFactory;*/
import org.sbml.sbml.level2.version4.Sbml;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.bind.*;
import javax.xml.bind.util.ValidationEventCollector;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.File;
import java.io.IOException;

public class Main {
    public static void main(String args[]) {
        System.out.println("test");
        /*System.setProperty(SimpleLogger.LOG_FILE_KEY, "samples/report.log");
        System.setProperty(SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "DEBUG");*/

        /*fr.curie.cd2sbgnml.xmlcdwrappers.ModelWrapper model = null;
        try {
            model = ObjectFactory.unmarshalSBML("src/main/resources/components44.xml");
        } catch (JAXBException e) {
            e.printStackTrace();
        }
        // ListOfSpeciesAlias
        List<SpeciesAlias> saList = model.getListOfSpeciesAliases();
        for (SpeciesAlias sa : saList) {
            String str = sa.getId() + ":" + sa.getSpecies() + ":";
            str += "(" + sa.getBounds().getX() + "," + sa.getBounds().getY() + ") [";
            str += sa.getBounds().getW() + " x " + sa.getBounds().getH() + "]";
            System.out.println(str);
        }
        // ListOfReactions
        List<fr.curie.cd2sbgnml.xmlcdwrappers.ReactionWrapper> rList = model.getListOfReactionWrapper();
        for(fr.curie.cd2sbgnml.xmlcdwrappers.ReactionWrapper r : rList){
            System.out.println(r.getId() + ": " + r.getReactionType());
            ConnectScheme cs = r.getConnectScheme();
            System.out.println("connect policy: " + cs.getConnectPolicy());
        }

        //System.out.println(CellDesigner.loadCellDesigner("src/main/resources/components44.xml"));
        /*try {
            ModelDocument doc= ModelDocument.Factory.parse(new File("src/main/resources/master.xml"));
        } catch (XmlException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }*/


        //SbmlDocument doc = CellDesigner.loadCellDesigner("src/main/resources/master.xml");
        /*try {
            SbmlDocument doc = SbmlDocument.Factory.parse(new File("src/main/resources/master.xml"));
        } catch (XmlException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }*/

        String inputFile = "samples/reaction.xml";

        SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema = null;
        try {
            schema = sf.newSchema(new File("schema/CellDesigner.xsd"));
            JAXBContext jc = JAXBContext.newInstance(Sbml.class);

            Unmarshaller unmarshaller = jc.createUnmarshaller();
            unmarshaller.setSchema(schema);
            unmarshaller.setEventHandler(new ValidationEventCollector());
            Sbml sbml = (Sbml) unmarshaller.unmarshal(new File(inputFile));
        } catch (SAXException | JAXBException e) {
            e.printStackTrace();
        }

        if(true) {
            CellDesignerSBFCModel cdModel = new CellDesignerSBFCModel();
            try {
                cdModel.setModelFromFile(inputFile);
                //System.out.println(cdModel.modelToString());
            } catch (ReadModelException e) {
                e.printStackTrace();
            }

            try {
                CD2SBGNML cd2SBGNML = new CD2SBGNML();
                SBGNSBFCModel sbgnModel = (SBGNSBFCModel) cd2SBGNML.convert(cdModel);
                sbgnModel.modelToFile("samples/out.sbgnml");
                //SbgnUtil.writeToFile(new CD2SBGNML().toSbgn(cdModel.getSbml()), new File("samples/out.sbgnml"));
            } catch (ReadModelException | ConversionException | WriteModelException e) {
                e.printStackTrace();
            }
        }

        System.out.println("VALIDATION");
        try {
            SbgnUtil.isValid(new File("samples/out.sbgnml"));
        } catch (JAXBException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


        System.out.println("CONVERT BACK TO CD");
        SBGNSBFCModel sbgnModel = new SBGNSBFCModel();
        try {
            sbgnModel.setModelFromFile("samples/out.sbgnml");
        } catch (ReadModelException e) {
            e.printStackTrace();
        }

        try {
            //SbgnUtil.writeToFile(new CD2SBGNML().toSbgn(cdModel.getModel()), new File("src/main/resources/out.sbgnml"));
            CellDesignerSBFCModel backCdModel = (CellDesignerSBFCModel) new SBGNML2CD().convert(sbgnModel);
            //backCdModel.modelToFile("src/main/resources/newCD.sbgnml");
            System.out.println(backCdModel.getSbml());


            File file = new File("samples/newCD.xml");
            Marshaller marshaller = JAXBContext.newInstance(Sbml.class).createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.setProperty("com.sun.xml.bind.namespacePrefixMapper", new Utils.DefaultNamespacePrefixMapper());
            marshaller.marshal(backCdModel.getSbml() , file);


            /*ModelWrapper mw = new ModelWrapper(backCdModel.getSbml().getModel(), backCdModel.getSbml());



            try {
                SBMLDocument doc = SBMLReader.read(ObjectFactory.generateXMLString(mw));
                System.out.println(sw.writeSBMLToString(doc));
                sw.writeSBMLToFile(doc, "src/main/resources/newCD.xml");
            } catch (JAXBException e) {
                e.printStackTrace();
            } catch (XMLStreamException e) {
                e.printStackTrace();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }*/

        } catch (ConversionException e) {
            e.printStackTrace();
        } catch (ReadModelException e) {
            e.printStackTrace();
        } catch (PropertyException e) {
            e.printStackTrace();
        } catch (JAXBException e) {
            e.printStackTrace();
        }

        System.out.println("VALIDATION of CellDesigner final output");

        sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        schema = null;
        try {
            schema = sf.newSchema(new File("schema/CellDesigner.xsd"));
            JAXBContext jc = JAXBContext.newInstance(Sbml.class);

            Unmarshaller unmarshaller = jc.createUnmarshaller();
            unmarshaller.setSchema(schema);
            unmarshaller.setEventHandler(new ValidationEventCollector());
            Sbml sbml = (Sbml) unmarshaller.unmarshal(new File("samples/newCD.xml"));
        } catch (SAXException | JAXBException e) {
            e.printStackTrace();
        }



    }
}
