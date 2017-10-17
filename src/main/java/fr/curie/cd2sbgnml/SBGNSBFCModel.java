package fr.curie.cd2sbgnml;

import org.sbfc.converter.exceptions.ReadModelException;
import org.sbfc.converter.models.SBGNModel;
import org.sbgn.SbgnUtil;
import org.sbgn.bindings.Sbgn;

import javax.xml.bind.JAXBException;
import java.io.File;

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
            this.model = SbgnUtil.readFromFile(new File(fileName));
        } catch (JAXBException e) {
            throw new ReadModelException(e.getCause());
        }
    }



}
