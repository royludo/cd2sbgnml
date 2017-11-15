package fr.curie.cd2sbgnml;

import org.sbfc.converter.exceptions.ReadModelException;
import org.sbfc.converter.exceptions.WriteModelException;
import org.sbfc.converter.models.SBGNModel;
import org.sbgn.SbgnUtil;
import org.sbgn.bindings.Sbgn;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.List;

public class SBGNSBFCModel extends SBGNModel {

    private Sbgn model;

    public SBGNSBFCModel() {
        super();
    }

    public SBGNSBFCModel(Sbgn model) {
        super(model);
        this.model = model;
    }


    public Sbgn getModel() {
        return model;
    }

    public void setModelFromFile(String fileName) throws ReadModelException {
        // set given sbgn to always be 0.2 to avoid compatibility problems

        File f = new File(fileName);
        try {
            String content = new String(Files.readAllBytes(f.toPath()));
            content = content.replaceFirst("http://sbgn\\.org/libsbgn/0\\.2", "http://sbgn.org/libsbgn/0.3");
            //System.out.println(content);
            JAXBContext context = JAXBContext.newInstance("org.sbgn.bindings");
            Unmarshaller unmarshaller = context.createUnmarshaller();
            this.model = (Sbgn)unmarshaller.unmarshal(new StringReader(content));
        } catch (IOException | JAXBException e) {
            e.printStackTrace();
        }


        /*try {
            this.model = SbgnUtil.readFromFile(new File(fileName));
        } catch (JAXBException e) {
            throw new ReadModelException(e.getCause());
        }*/
    }

    @Override
    public void modelToFile(String fileName) throws WriteModelException {
        StringWriter sw = new StringWriter();
        try {
            Marshaller marshaller = JAXBContext.newInstance("org.sbgn.bindings").createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            //marshaller.setProperty("com.sun.xml.bind.namespacePrefixMapper", new Utils.DefaultNamespacePrefixMapper());
            marshaller.marshal(this.model , sw);

            String content = sw.toString();
            content = content.replaceFirst("http://sbgn\\.org/libsbgn/0\\.3", "http://sbgn.org/libsbgn/0.2");

            PrintWriter out = new PrintWriter(fileName);
            out.println(content);
            out.close();
        } catch (JAXBException | FileNotFoundException e) {
            e.printStackTrace();
        }

    }
}
