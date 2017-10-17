package fr.curie.cd2sbgnml;

import org.sbfc.converter.exceptions.ConversionException;
import org.sbfc.converter.exceptions.ReadModelException;
import org.sbfc.converter.exceptions.WriteModelException;
import org.sbgn.SbgnUtil;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLReader;
import org.sbml.jsbml.SBMLWriter;
import org.sbml.jsbml.TidySBMLWriter;
import org.sbml.sbml.level2.version4.Sbml;
import org.sbml.wrapper.ModelWrapper;
import org.sbml.wrapper.ObjectFactory;
import org.slf4j.impl.SimpleLogger;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class Main {
    public static void main(String args[]) {
        System.out.println("test");
        System.setProperty(SimpleLogger.LOG_FILE_KEY, "src/main/resources/report.log");
        System.setProperty(SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "DEBUG");

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

        CellDesignerSBFCModel cdModel = new CellDesignerSBFCModel();
        try {
            cdModel.setModelFromFile("src/main/resources/reaction.xml");
            //System.out.println(cdModel.modelToString());
        } catch (ReadModelException e) {
            e.printStackTrace();
        }

        try {
            SbgnUtil.writeToFile(new CD2SBGNML().toSbgn(cdModel.getModel()), new File("src/main/resources/out.sbgnml"));
        } catch (JAXBException e) {
            e.printStackTrace();
        }

        System.out.println("VALIDATION");
        try {
            SbgnUtil.isValid(new File("src/main/resources/out.sbgnml"));
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
            sbgnModel.setModelFromFile("src/main/resources/out.sbgnml");
        } catch (ReadModelException e) {
            e.printStackTrace();
        }

        try {
            //SbgnUtil.writeToFile(new CD2SBGNML().toSbgn(cdModel.getModel()), new File("src/main/resources/out.sbgnml"));
            CellDesignerSBFCModel backCdModel = (CellDesignerSBFCModel) new SBGNML2CD().convert(sbgnModel);
            //backCdModel.modelToFile("src/main/resources/newCD.sbgnml");
            System.out.println(backCdModel.getBaseSbml());

            ModelWrapper mw = new ModelWrapper(backCdModel.getBaseSbml().getModel(), backCdModel.getBaseSbml());
            SBMLWriter sw = new SBMLWriter();


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
            }

        } catch (ConversionException e) {
            e.printStackTrace();
        } catch (ReadModelException e) {
            e.printStackTrace();
        } /*catch (WriteModelException e) {
            e.printStackTrace();
        }*/ /*catch (FileNotFoundException e) {
            e.printStackTrace();
        }*/ /*catch (XMLStreamException e) {
            e.printStackTrace();
        }*/


        //SbmlDocument doc = CellDesigner.loadCellDesigner("src/main/resources/components44.xml");
        //System.out.println(doc);

    }
}
