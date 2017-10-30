package fr.curie.cd2sbgnml;

import fr.curie.cd2sbgnml.xmlcdwrappers.Utils;
import org.sbfc.converter.exceptions.ReadModelException;
import org.sbfc.converter.exceptions.WriteModelException;
import org.sbfc.converter.models.GeneralModel;
import org.sbml.sbml.level2.version4.Sbml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class CellDesignerSBFCModel implements GeneralModel {

    private final Logger logger = LoggerFactory.getLogger(CellDesignerSBFCModel.class);

    private Sbml sbml;

    public CellDesignerSBFCModel() {
        super();
    }



    public CellDesignerSBFCModel(Sbml sbml) {
        super();
        this.sbml = sbml;
    }


    public Sbml getSbml() {
        return this.sbml;
    }

    public void setModelFromFile(String s) throws ReadModelException {
        byte[] encoded = new byte[0];
        try {
            encoded = Files.readAllBytes(Paths.get(s));
        } catch (IOException e) {
            e.printStackTrace();
        }
        String sbmlString = new String(encoded, StandardCharsets.UTF_8);
        this.setModelFromString(sbmlString);
    }

    public void setModelFromString(String s) throws ReadModelException {
        /*
            ACSN and other maps don't have proper namespace: xmlns="http://www.sbml.org/sbml/level2"
            we need to put a level here.
         */
        if(! s.matches("xmlns=\"http://www\\.sbml\\.org/sbml/level2\"")) {
            logger.warn("Namespace definition was messed up, it has been set to: " +
                    "xmlns=\"http://www.sbml.org/sbml/level2/version4\"");
            s = s.replaceFirst("xmlns=\"http://www\\.sbml\\.org/sbml/level2\"",
                    "xmlns=\"http://www.sbml.org/sbml/level2/version4\"");
        }

        /*
            ACSN and other maps have their <celldesigner:extension> element removed, everywhere.
         */
        // if one extension is present, then consider the rest valid. If none, we need to add them.
        if(! s.matches("<annotation>[\\n\\s]*<celldesigner:extension>")) {
            logger.warn("<celldesigner:extension> elements are missing, they were added automatically.");
            s = s.replaceAll("<annotation>", "<annotation><celldesigner:extension>");
            s = s.replaceAll("</annotation>", "</celldesigner:extension></annotation>");
        }

        JAXBContext jaxbContext = null;
        try {
            jaxbContext = JAXBContext.newInstance(Sbml.class);
            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            this.sbml = (Sbml) jaxbUnmarshaller.unmarshal(new StringReader(s));

        } catch (JAXBException e) {
            e.printStackTrace();
            throw new ReadModelException(e.getCause());
        }

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
