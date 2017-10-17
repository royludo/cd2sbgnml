package fr.curie.cd2sbgnml;

import org.sbfc.converter.GeneralConverter;
import org.sbfc.converter.exceptions.ConversionException;
import org.sbfc.converter.exceptions.ReadModelException;
import org.sbfc.converter.models.GeneralModel;
import org.sbgn.bindings.Sbgn;
import org.sbml.x2001.ns.celldesigner.CelldesignerModificationDocument;
import org.sbml.x2001.ns.celldesigner.SbmlDocument;

public class SBGNML2CD extends GeneralConverter {


    public SbmlDocument toCD(Sbgn sbgn) {

        return SbmlDocument.Factory.newInstance();
    }


    @Override
    public GeneralModel convert(GeneralModel generalModel) throws ConversionException, ReadModelException {
        SBGNSBFCModel sbgnModel = (SBGNSBFCModel) generalModel;
        return new CellDesignerSBFCModel(this.toCD(sbgnModel.getModel()));
    }

    @Override
    public String getResultExtension() {
        return null;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public String getHtmlDescription() {
        return null;
    }
}
