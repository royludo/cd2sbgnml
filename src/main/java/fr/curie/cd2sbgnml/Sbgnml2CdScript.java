package fr.curie.cd2sbgnml;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.sbfc.converter.exceptions.ConversionException;
import org.sbfc.converter.exceptions.ReadModelException;
import org.sbfc.converter.exceptions.WriteModelException;
import org.sbml.sbml.level2.version4.Sbml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.util.ValidationEventCollector;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.File;

public class Sbgnml2CdScript {
    @Parameter(names = { "-i", "--input"}, required = true)
    private String inputFileName;

    @Parameter(names = { "-o", "--output" }, required = true)
    private String outputFileName;

    public static void main(String[] args) {
        Logger logger = LoggerFactory.getLogger(Sbgnml2CdScript.class);

        Sbgnml2CdScript app = new Sbgnml2CdScript();
        JCommander.newBuilder()
                .addObject(app)
                .build()
                .parse(args);

        convert(app.inputFileName, app.outputFileName);
    }

    public static void convert(String inputFileName, String outputFileName) {
        SBGNML2CD toCDConverter = new SBGNML2CD();


        SBGNSBFCModel sbgnModel = new SBGNSBFCModel();
        try {
            sbgnModel.setModelFromFile(inputFileName);
        } catch (ReadModelException e) {
            e.printStackTrace();
        }

        try {
            CellDesignerSBFCModel cellDesignerSBFCModel = (CellDesignerSBFCModel) toCDConverter.convert(sbgnModel);
            cellDesignerSBFCModel.modelToFile(outputFileName);
        } catch (ConversionException | ReadModelException | WriteModelException e) {
            e.printStackTrace();
        }

        // VALIDATION

        SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        try {
            Schema schema = sf.newSchema(
                            Sbgnml2CdScript.class.getResource("/schema/CellDesigner.xsd"));
            JAXBContext jc = JAXBContext.newInstance(Sbml.class);

            Unmarshaller unmarshaller = jc.createUnmarshaller();
            unmarshaller.setSchema(schema);
            unmarshaller.setEventHandler(new ValidationEventCollector());
            unmarshaller.unmarshal(new File(outputFileName));
        } catch (SAXException | JAXBException e) {
            e.printStackTrace();
        }
    }
}
