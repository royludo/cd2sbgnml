package fr.curie.cd2sbgnml;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.sbfc.converter.exceptions.ConversionException;
import org.sbfc.converter.exceptions.ReadModelException;
import org.sbfc.converter.exceptions.WriteModelException;
import org.sbgn.SbgnUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.IOException;

public class Cd2SbgnmlScript {
    @Parameter(names = { "-i", "--input"}, required = true)
    private String inputFileName;

    @Parameter(names = { "-o", "--output" }, required = true)
    private String outputFileName;

    public static void main(String[] args) {
        Logger logger = LoggerFactory.getLogger(Cd2SbgnmlScript.class);

        Cd2SbgnmlScript app = new Cd2SbgnmlScript();
        JCommander.newBuilder()
                .addObject(app)
                .build()
                .parse(args);

        convert(app.inputFileName, app.outputFileName);
    }

    public static void convert(String inputFileName, String outputFileName) {
        CD2SBGNML toSBGNConverter = new CD2SBGNML();


        CellDesignerSBFCModel cdModel = new CellDesignerSBFCModel();
        try {
            cdModel.setModelFromFile(inputFileName);
        } catch (ReadModelException e) {
            e.printStackTrace();
        }

        try {
            SBGNSBFCModel sbgnModel = (SBGNSBFCModel) toSBGNConverter.convert(cdModel);
            sbgnModel.modelToFile(outputFileName);
        } catch (ConversionException | ReadModelException | WriteModelException e) {
            e.printStackTrace();
        }

        // VALIDATION
        try {
            SbgnUtil.isValid(new File(outputFileName));
        } catch (JAXBException | SAXException | IOException e) {
            e.printStackTrace();
        }
    }
}
