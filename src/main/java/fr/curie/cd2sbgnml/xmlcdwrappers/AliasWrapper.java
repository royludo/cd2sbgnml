package fr.curie.cd2sbgnml.xmlcdwrappers;

import org.sbml.x2001.ns.celldesigner.CelldesignerBoundsDocument.CelldesignerBounds;
import org.sbml.x2001.ns.celldesigner.CelldesignerCompartmentAliasDocument.CelldesignerCompartmentAlias;
import org.sbml.x2001.ns.celldesigner.CelldesignerComplexSpeciesAliasDocument.CelldesignerComplexSpeciesAlias;
import org.sbml.x2001.ns.celldesigner.CelldesignerSpeciesAliasDocument.CelldesignerSpeciesAlias;
import org.w3c.dom.Node;

import java.awt.geom.Point2D;

/**
 * wraps speciesAlias and complexSpeciesAlias as they have a lot in common
 */
public class AliasWrapper {

    public enum AliasType {COMPLEX, COMPARTMENT, BASIC}

    private CelldesignerBounds bounds;
    private AliasType aliasType;
    private String id;
    private String complexAlias;
    private String compartmentAlias;
    private String speciesId;
    private SpeciesWrapper speciesW;
    private boolean isActive;
    private AliasInfoWrapper info;
    private StyleInfo styleInfo;

    public AliasWrapper(CelldesignerSpeciesAlias alias, SpeciesWrapper speciesW) {
        this.aliasType = AliasType.BASIC;
        this.bounds = alias.getCelldesignerBounds();
        this.id = alias.getId();
        this.complexAlias = alias.getComplexSpeciesAlias();
        this.speciesId = alias.getSpecies();
        this.speciesW = speciesW;
        // I don't understand the exception that can be thrown here
        // maybe a change in the celldesigner spec since the code was done
        try {
            this.compartmentAlias = alias.getCompartmentAlias();
        } catch(Exception e) {
            //e.printStackTrace();
            this.compartmentAlias = null;
        }
        this.isActive = alias.getCelldesignerActivity().getDomNode().getChildNodes().item(0).getNodeValue().equals("active");
        this.addAliasInfo(alias.getDomNode());
        this.styleInfo = new StyleInfo(alias, speciesW.getId()+"_"+this.id);

    }

    public AliasWrapper(CelldesignerComplexSpeciesAlias alias, SpeciesWrapper speciesW) {
        this.aliasType = AliasType.COMPLEX;
        this.bounds = alias.getCelldesignerBounds();
        this.id = alias.getId();
        this.speciesId = alias.getSpecies();
        this.speciesW = speciesW;

        // complexSpeciesAlias cannot be accessed through standard object structure
        // probably because of outdated api of binom
        // we need to fetch it manually in the xml, if present
        if(alias.getDomNode().getAttributes().getNamedItem("complexSpeciesAlias") != null) {
            this.complexAlias = alias.getDomNode().getAttributes().getNamedItem("complexSpeciesAlias").getNodeValue();
        }
        else {
            this.complexAlias = null;
        }
         //alias.getDomNode().getAttributes().getNamedItem("complexSpeciesAlias").getNodeValue();
        this.compartmentAlias = alias.getCompartmentAlias();
        this.isActive = alias.getCelldesignerActivity().getDomNode().getChildNodes().item(0).getNodeValue().equals("active");
        this.addAliasInfo(alias.getDomNode());
        this.styleInfo = new StyleInfo(alias, speciesW.getId()+"_"+this.id);
    }

    public AliasWrapper(CelldesignerCompartmentAlias alias, SpeciesWrapper speciesW) {
        this.aliasType = AliasType.COMPARTMENT;
        this.bounds = alias.getCelldesignerBounds();
        this.id = alias.getId();
        this.complexAlias = null;
        this.compartmentAlias = null;
        this.speciesW = speciesW;
    }

    private void addAliasInfo(Node aliasDomNode) {
        for(int i=0; i < aliasDomNode.getChildNodes().getLength(); i++) {
            Node child = aliasDomNode.getChildNodes().item(i);
            if(child.getNodeName().equals("celldesigner_info")) {
                String state = child.getAttributes().getNamedItem("state").getNodeValue();
                if(!state.equals("empty")) {
                    // interesting info to keep
                    String prefix = child.getAttributes().getNamedItem("prefix").getNodeValue();
                    String label = child.getAttributes().getNamedItem("label").getNodeValue();
                    Float angle = Float.parseFloat(child.getAttributes().getNamedItem("angle").getNodeValue());
                    this.info = new AliasInfoWrapper(angle, prefix, label);
                }
            }
        }
    }

    public Point2D.Float getCenterPoint() {
        // TODO check if coords are center or corner (corner it seems)
        return new Point2D.Float(
                Float.parseFloat(this.bounds.getX()) + Float.parseFloat(this.bounds.getW())/2,
                Float.parseFloat(this.bounds.getY()) + Float.parseFloat(this.bounds.getH())/2);
    }

    public CelldesignerBounds getBounds() {
        return bounds;
    }

    public AliasType getAliasType() {
        return aliasType;
    }

    public String getId() {
        return id;
    }

    public String getComplexAlias() {
        return complexAlias;
    }

    public String getCompartmentAlias() {
        return compartmentAlias;
    }

    public String getSpeciesId() {
        return speciesId;
    }

    public SpeciesWrapper getSpeciesW() {
        return speciesW;
    }

    public boolean isActive() {
        return isActive;
    }

    public AliasInfoWrapper getInfo() {
        return info;
    }

    public StyleInfo getStyleInfo() {
        return styleInfo;
    }
}
