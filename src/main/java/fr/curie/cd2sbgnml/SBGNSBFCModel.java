package fr.curie.cd2sbgnml;

import org.sbfc.converter.models.SBGNModel;
import org.sbgn.bindings.Sbgn;

public class SBGNSBFCModel extends SBGNModel {

    private Sbgn model;

    public SBGNSBFCModel(Sbgn model) {
        super(model);
        this.model = model;
    }


    public Sbgn getModel() {
        return model;
    }



}
