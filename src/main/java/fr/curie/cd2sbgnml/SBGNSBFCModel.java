package fr.curie.cd2sbgnml;

import org.sbfc.converter.exceptions.ReadModelException;
import org.sbfc.converter.exceptions.WriteModelException;
import org.sbfc.converter.models.SBGNModel;
import org.sbgn.bindings.Sbgn;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.*;
import java.util.stream.Collectors;

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

        try {
            BufferedReader reader = new BufferedReader(
                    new FileReader(fileName));
            BOMskip(reader);
            String content = reader.lines().collect(Collectors.joining());

            // set given sbgn to always be 0.2 to avoid compatibility problems
            content = content.replaceFirst("http://sbgn\\.org/libsbgn/0\\.3", "http://sbgn.org/libsbgn/0.2");

            JAXBContext context = JAXBContext.newInstance("org.sbgn.bindings");
            Unmarshaller unmarshaller = context.createUnmarshaller();
            this.model = (Sbgn)unmarshaller.unmarshal(new StringReader(content));
        } catch (IOException | JAXBException e) {
            e.printStackTrace();
        }

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
            // following line is when using 0.3 and processing 0.2
            //content = content.replaceFirst("http://sbgn\\.org/libsbgn/0\\.3", "http://sbgn.org/libsbgn/0.2");

            PrintWriter out = new PrintWriter(fileName);
            out.println(content);
            out.close();
        } catch (JAXBException | FileNotFoundException e) {
            e.printStackTrace();
        }

    }

    /**
     * Skip BOM char. BOM is present in output of Newt.
     * See https://stackoverflow.com/a/18275066
     * @param reader
     * @throws IOException
     */
    public static void BOMskip(Reader reader) throws IOException {
        reader.mark(1);
        char[] possibleBOM = new char[1];
        reader.read(possibleBOM);
        // on windows, starts with 00ef
        if (possibleBOM[0] != '\ufeff' && possibleBOM[0] != '\u00ef') {
            reader.reset();
        }
        else if(possibleBOM[0] == '\u00ef') {
            reader.read(new char[2]);
        }
    }
}
