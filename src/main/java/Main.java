import fr.curie.BiNoM.pathways.wrappers.CellDesigner;
import org.apache.xmlbeans.XmlException;
import org.sbfc.converter.exceptions.ConversionException;
import org.sbfc.converter.exceptions.ReadModelException;
import org.sbfc.converter.exceptions.WriteModelException;
import org.sbgn.SbgnUtil;
import org.sbgn.bindings.Sbgn;
import org.sbml.x2001.ns.celldesigner.CelldesignerListOfSpeciesAliasesDocument;
import org.sbml.x2001.ns.celldesigner.ListOfSpeciesDocument;
import org.sbml.x2001.ns.celldesigner.ModelDocument;
import org.sbml.x2001.ns.celldesigner.SbmlDocument;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class Main {
    public static void main(String args[]) {
        System.out.println("test");
        /*ModelWrapper model = null;
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
        List<ReactionWrapper> rList = model.getListOfReactionWrapper();
        for(ReactionWrapper r : rList){
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
            cdModel.setModelFromFile("src/main/resources/mtor.xml");
            //System.out.println(cdModel.modelToString());
        } catch (ReadModelException e) {
            e.printStackTrace();
        }

        try {
            SbgnUtil.writeToFile(new CD2SBGNML().toSbgn(cdModel.getModel()), new File("src/main/resources/out.sbgnml"));
        } catch (JAXBException e) {
            e.printStackTrace();
        }

        //SbmlDocument doc = CellDesigner.loadCellDesigner("src/main/resources/components44.xml");
        //System.out.println(doc);

    }
}
