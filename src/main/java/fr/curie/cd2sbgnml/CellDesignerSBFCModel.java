package fr.curie.cd2sbgnml;

import fr.curie.BiNoM.pathways.wrappers.CellDesigner;
import org.sbfc.converter.exceptions.ReadModelException;
import org.sbfc.converter.exceptions.WriteModelException;
import org.sbfc.converter.models.GeneralModel;
import org.sbml.sbml.level2.version4.Sbml;
import org.sbml.x2001.ns.celldesigner.SbmlDocument;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;

public class CellDesignerSBFCModel implements GeneralModel {

    private SbmlDocument sbml;
    private Sbml baseSbml;

    public CellDesignerSBFCModel() {
        super();
    }

    public CellDesignerSBFCModel(SbmlDocument sbml) {
        super();
        this.sbml = sbml;
    }



    public CellDesignerSBFCModel(Sbml sbml) {
        super();
        this.baseSbml = sbml;
    }


    public SbmlDocument getModel() {
        return sbml;
    }
    public Sbml getBaseSbml() {
        return baseSbml;
    }

    public void setModelFromFile(String s) throws ReadModelException {
        this.sbml = CellDesigner.loadCellDesigner(s);
    }

    public void setModelFromString(String s) throws ReadModelException {
        this.sbml = CellDesigner.loadCellDesignerFromText(s);
    }

    public void modelToFile(String s) throws WriteModelException {
        CellDesigner.saveCellDesigner(this.sbml, s);
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
