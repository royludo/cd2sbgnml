package fr.curie.cd2sbgnml;

import fr.curie.cd2sbgnml.xmlcdwrappers.Utils;
import org.sbfc.converter.exceptions.ReadModelException;
import org.sbfc.converter.exceptions.WriteModelException;
import org.sbfc.converter.models.GeneralModel;
import org.sbml.sbml.level2.version4.Sbml;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.File;

public class CellDesignerSBFCModel implements GeneralModel {

    private Sbml sbml;

    public CellDesignerSBFCModel() {
        super();
    }



    public CellDesignerSBFCModel(Sbml sbml) {
        super();
        this.sbml = sbml;
    }


    public Sbml getSbml() {
        return sbml;
    }

    public void setModelFromFile(String s) throws ReadModelException {
        File file = new File(s);
        JAXBContext jaxbContext = null;
        try {
            jaxbContext = JAXBContext.newInstance(Sbml.class);
            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            this.sbml = (Sbml) jaxbUnmarshaller.unmarshal(file);

        } catch (JAXBException e) {
            throw new ReadModelException(e.getCause());
        }
    }

    public void setModelFromString(String s) throws ReadModelException {
        //this.sbml = CellDesigner.loadCellDesignerFromText(s);
    }

    public void modelToFile(String s) throws WriteModelException {
        File file = new File(s);
        Marshaller marshaller = null;
        try {
            marshaller = JAXBContext.newInstance(Sbml.class).createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.setProperty("com.sun.xml.internal.bind.namespacePrefixMapper", new Utils.DefaultNamespacePrefixMapper());
            marshaller.marshal(this.sbml, file);
        } catch (JAXBException e) {
            throw new WriteModelException(e.getCause());
        }

    }

    public String modelToString() throws WriteModelException {
        return this.sbml.toString();
    }

    public String[] getExtensions() {
        return new String[0];
    }

    public boolean isCorrectType(File file) {
        return false;
    }

    public String getURI() {
        return null;
    }
}
