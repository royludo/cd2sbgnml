package fr.curie.cd2sbgnml.model;

import fr.curie.cd2sbgnml.graphics.*;
import fr.curie.cd2sbgnml.xmlcdwrappers.ReactantWrapper;
import fr.curie.cd2sbgnml.xmlcdwrappers.SpeciesWrapper.ReferenceType;

import java.awt.geom.Point2D;

/**
 *
 */
public class ReactantModel extends GenericReactionElement{

    private AnchorPoint anchorPoint;

    public ReactantModel(ReactantWrapper reactantW) {
        super(new Glyph(
                reactantW.getCenterPoint(),
                reactantW.getWidth(),
                reactantW.getHeight(),
                getCdShape(reactantW.getAliasW().getSpeciesW().getCdClass(),
                        reactantW.getAliasW().getSpeciesW().getType()),
                getSbgnShape(getSbgnClass(reactantW.getAliasW().getSpeciesW().getCdClass()))),
                reactantW.getAliasW().getSpeciesId()+"_"+reactantW.getAliasW().getId()
        );
        this.anchorPoint = reactantW.getAnchorPoint();

    }

    public static CdShape getCdShape(String cdClass, ReferenceType type) {
        switch(cdClass) {
            case "PROTEIN":
                switch(type) {
                    case GENERIC: return CdShape.RECTANGLE;
                    case RECEPTOR: return CdShape.RECEPTOR;
                    case TRUNCATED: return CdShape.TRUNCATED;
                    case ION_CHANNEL: return CdShape.RECTANGLE;
                }
                break;

            case "GENE": return CdShape.RECTANGLE;
            case "RNA": return CdShape.RIGHT_PARALLELOGRAM;
            case "ANTISENSE_RNA": return CdShape.LEFT_PARALLELOGRAM;
            case "PHENOTYPE": return CdShape.PHENOTYPE;
            case "ION": return CdShape.CIRCLE;
            case "SIMPLE_MOLECULE": return CdShape.ELLIPSE;
            case "DRUG": return CdShape.RECTANGLE; // TODO approximative
            case "UNKNOWN": return CdShape.ELLIPSE;
            case "COMPLEX": return CdShape.RECTANGLE;

            // these are for compartment shapes
            /*case "SQUARE": return "";
            case "OVAL": return "";
            case "SQUARE_CLOSEUP_NORTHWEST": return "";
            case "SQUARE_CLOSEUP_NORTHEAST": return "";
            case "SQUARE_CLOSEUP_SOUTHWEST": return "";
            case "SQUARE_CLOSEUP_NORTH": return "";
            case "SQUARE_CLOSEUP_EAST": return "";
            case "SQUARE_CLOSEUP_WEST": return "";
            case "SQUARE_CLOSEUP_SOUTH": return "";*/

            case "DEGRADED": return CdShape.CIRCLE;
        }
        throw new IllegalArgumentException("Invalid celldesigner class given: "+cdClass);
    }

    public static SbgnShape getSbgnShape(String sbgnClass) {
        switch(sbgnClass) {
            case "macromolecule": return SbgnShape.RECTANGLE;
            case "nucleic acid feature": return SbgnShape.RECTANGLE;
            case "phenotype": return SbgnShape.PHENOTYPE;
            case "simple chemical": return SbgnShape.ELLIPSE;
            case "unspecified entity": return SbgnShape.ELLIPSE;
            case "complex": return SbgnShape.RECTANGLE;
            case "source and sink": return SbgnShape.CIRCLE;
            case "process": return SbgnShape.RECTANGLE;
        }
        throw new IllegalArgumentException("Invalid sbgn class given: "+sbgnClass);
    }

    public static String getSbgnClass(String cdClass) {
        switch(cdClass) {
            case "PROTEIN": return "macromolecule";
            case "GENE": return "nucleic acid feature";
            case "RNA": return "nucleic acid feature";
            case "ANTISENSE_RNA": return "nucleic acid feature";
            case "PHENOTYPE": return "phenotype";
            case "ION": return "simple chemical";
            case "SIMPLE_MOLECULE": return "simple chemical";
            case "DRUG": return "simple chemical";
            case "UNKNOWN": return "unspecified entity";
            case "COMPLEX": return "complex";

            case "DEGRADED": return "source and sink";
        }
        throw new IllegalArgumentException("Could not infer SBGN class from species class: "+cdClass);
    }

    public static String getCdClass(String sbgnClass, ReferenceType type) {
        switch(sbgnClass) {
            case "macromolecule":
            case "macromolecule multimer":
                return "PROTEIN";
            case "nucleic acid feature":
            case "nucleic acid feature multimer":
                switch(type) {
                    case RNA: return "RNA";
                    case ANTISENSE_RNA: return "ANTISENSE_RNA";
                    case GENE:
                    default:
                            return "GENE";
                }
            case "phenotype":
            case "perturbing agent":
            case "submap":
                return "PHENOTYPE";
            case "simple chemical":
            case "simple chemical multimer":
                return "SIMPLE_MOLECULE";
            case "unspecified entity": return "UNKNOWN";
            case "complex":
            case "complex multimer":
                return "COMPLEX";
            case "source and sink": return "DEGRADED";
        }
        throw new IllegalArgumentException("Could not infer CellDesigner class from: "+sbgnClass+" "+type+" . Valid " +
                "glyph class must be provided.");
    }

    /**
     * Get relative coordinate of an anchor point from center of the shape
     * @param anchorPoint
     * @return
     */
    public Point2D.Float getRelativeAnchorCoordinate(AnchorPoint anchorPoint) {
        return GeometryUtils.getRelativeAnchorCoordinate(
                this.getGlyph().getCdShape(),
                this.getGlyph().getWidth(),
                this.getGlyph().getHeight(),
                anchorPoint
        );
    }

    public Point2D.Float getAbsoluteAnchorCoordinate(AnchorPoint anchorPoint) {
        Point2D relativePoint = this.getRelativeAnchorCoordinate(anchorPoint);
        return new Point2D.Float(
                (float) (relativePoint.getX() + this.getGlyph().getCenter().getX()),
                (float) (relativePoint.getY() + this.getGlyph().getCenter().getY()));
    }


    public AnchorPoint getAnchorPoint() {
        return anchorPoint;
    }

}
