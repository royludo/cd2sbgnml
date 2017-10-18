package fr.curie.cd2sbgnml.model;

import fr.curie.cd2sbgnml.graphics.*;
import fr.curie.cd2sbgnml.xmlcdwrappers.ReactantWrapper;
import fr.curie.cd2sbgnml.xmlcdwrappers.SpeciesWrapper;
import fr.curie.cd2sbgnml.xmlcdwrappers.SpeciesWrapper.ReferenceType;

import java.awt.geom.Point2D;
import java.util.List;

public class ReactantModel extends GenericReactionElement{

    private GenericReactionModel reactionModel;
    private AnchorPoint anchorPoint;

    public ReactantModel(GenericReactionModel genericReactionModel, ReactantWrapper reactantW) {
        super(new Glyph(
                reactantW.getCenterPoint(),
                reactantW.getWidth(),
                reactantW.getHeight(),
                getCdShape(reactantW.getAliasW().getSpeciesW().getCdClass(),
                        reactantW.getAliasW().getSpeciesW().getType()),
                getSbgnShape(getSbgnClass(reactantW.getAliasW().getSpeciesW().getCdClass()))),
                reactantW.getAliasW().getSpeciesId()+"_"+reactantW.getAliasW().getId()
        );
        this.reactionModel = genericReactionModel;
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

    /**
     * Get relative coordinate of an anchor point from center of the shape
     * @param anchorPoint
     * @return
     */
    public Point2D.Float getRelativeAnchorCoordinate(AnchorPoint anchorPoint) {

        Point2D.Float relativeAnchorPoint;
        if(anchorPoint != AnchorPoint.CENTER) {
            float angle = GeometryUtils.perimeterAnchorPointToAngle(anchorPoint);
            // ellipse shapes
            switch(this.getGlyph().getCdShape()) {
                case ELLIPSE:
                case CIRCLE:
                    relativeAnchorPoint = GeometryUtils.ellipsePerimeterPointFromAngle(
                            this.getGlyph().getWidth(), this.getGlyph().getHeight(), angle);
                    break;
                case PHENOTYPE:
                    relativeAnchorPoint = GeometryUtils.getRelativePhenotypeAnchorPosition(
                            anchorPoint, this.getGlyph().getWidth(), this.getGlyph().getHeight());
                    System.out.println("PHENOTYPE "+this.getGlyph().getWidth()+" "+this.getGlyph().getHeight()+" "+relativeAnchorPoint);
                    break;
                case LEFT_PARALLELOGRAM:
                    relativeAnchorPoint = GeometryUtils.getRelativeLeftParallelogramAnchorPosition(
                            anchorPoint, this.getGlyph().getWidth(), this.getGlyph().getHeight());
                    break;
                case RIGHT_PARALLELOGRAM:
                    relativeAnchorPoint = GeometryUtils.getRelativeRightParallelogramAnchorPosition(
                            anchorPoint, this.getGlyph().getWidth(), this.getGlyph().getHeight());
                    break;
                case RECEPTOR:
                    relativeAnchorPoint = GeometryUtils.getRelativeReceptorAnchorPosition(
                            anchorPoint, this.getGlyph().getWidth(), this.getGlyph().getHeight());
                    break;
                case TRUNCATED:
                default: // RECTANGLE as default
                    relativeAnchorPoint = GeometryUtils.getRelativeRectangleAnchorPosition(
                            anchorPoint, this.getGlyph().getWidth(), this.getGlyph().getHeight());
            }
        }
        else {
            relativeAnchorPoint = new Point2D.Float(0,0);
        }
        return relativeAnchorPoint;
    }

    public Point2D.Float getAbsoluteAnchorCoordinate(AnchorPoint anchorPoint) {
        Point2D relativePoint = this.getRelativeAnchorCoordinate(anchorPoint);
        return new Point2D.Float(
                (float) (relativePoint.getX() + this.getGlyph().getCenter().getX()),
                (float) (relativePoint.getY() + this.getGlyph().getCenter().getY()));
    }


    public GenericReactionModel getReactionModel() {
        return reactionModel;
    }

    public AnchorPoint getAnchorPoint() {
        return anchorPoint;
    }

}
