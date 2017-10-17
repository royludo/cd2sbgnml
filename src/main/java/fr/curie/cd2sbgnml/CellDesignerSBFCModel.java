package fr.curie.cd2sbgnml;

import fr.curie.BiNoM.pathways.wrappers.CellDesigner;
import org.sbfc.converter.exceptions.ReadModelException;
import org.sbfc.converter.exceptions.WriteModelException;
import org.sbfc.converter.models.GeneralModel;
import org.sbml.x2001.ns.celldesigner.SbmlDocument;

import java.io.File;

public class CellDesignerSBFCModel implements GeneralModel {

    private SbmlDocument sbml;

    public CellDesignerSBFCModel(SbmlDocument sbml) {
        this.sbml = sbml;
    }


    public SbmlDocument getModel() {
        return sbml;
    }

    public void setModelFromFile(String s) throws ReadModelException {
        this.sbml = CellDesigner.loadCellDesigner(s);
    }

    public void setModelFromString(String s) throws ReadModelException {
        this.sbml = CellDesigner.loadCellDesignerFromText(s);
    }

    public void modelToFile(String s) throws WriteModelException {

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
