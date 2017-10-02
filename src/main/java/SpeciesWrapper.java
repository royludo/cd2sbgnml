import org.sbml.x2001.ns.celldesigner.CelldesignerClassDocument.CelldesignerClass;
import org.sbml.x2001.ns.celldesigner.CelldesignerComplexSpeciesAliasDocument.CelldesignerComplexSpeciesAlias;
import org.sbml.x2001.ns.celldesigner.CelldesignerComplexSpeciesDocument.CelldesignerComplexSpecies;
import org.sbml.x2001.ns.celldesigner.CelldesignerSpeciesAliasDocument.CelldesignerSpeciesAlias;
import org.sbml.x2001.ns.celldesigner.CelldesignerSpeciesDocument.CelldesignerSpecies;
import org.sbml.x2001.ns.celldesigner.SpeciesDocument.Species;

import java.util.ArrayList;
import java.util.List;

/**
 * wraps species and includedSpecies as they have a lot in common
 */
public class SpeciesWrapper {

    public enum CdShape {RECTANGLE, ELLIPSE, PHENOTYPE, LEFT_PARALLELOGRAM, RIGHT_PARALLELOGRAM, TRUNCATED}

    private boolean isIncludedSpecies;
    private boolean isComplex;

    private String id;
    private String name;
    private String compartment;
    private String complex;
    private String cdClass;
    private String sbgnClass;
    private CdShape cdShape;

    private List<AliasWrapper> aliases;

    public SpeciesWrapper(Species species, ModelWrapper modelW) {
        this.isIncludedSpecies = false;
        CelldesignerClass cdClassClass = species.getAnnotation().getCelldesignerSpeciesIdentity().getCelldesignerClass();
        this.cdClass = cdClassClass.getDomNode().getChildNodes().item(0).getNodeValue();
        this.isComplex = this.cdClass.equals("COMPLEX");
        this.id = species.getId();
        this.name = species.getName().getStringValue();
        this.compartment = species.getCompartment();
        this.complex = null;
        this.aliases = new ArrayList<>();

        if(this.isComplex) {
            for(CelldesignerComplexSpeciesAlias complexAlias : modelW.getComplexSpeciesAliasFor(this.id)) {
                if (complexAlias == null) {
                    continue;
                }
                this.aliases.add(new AliasWrapper(complexAlias, this));
            }
        }
        else {
            for(CelldesignerSpeciesAlias alias : modelW.getSpeciesAliasFor(this.id)) {
                if (alias == null) {
                    continue;
                }
                this.aliases.add(new AliasWrapper(alias, this));
            }
        }

        this.sbgnClass = getSbgnClass(cdClass);
        this.cdShape = getSbgnShape(sbgnClass);
    }

    public SpeciesWrapper(CelldesignerSpecies species, ModelWrapper modelW) {
        this.isIncludedSpecies = true;
        CelldesignerClass cdClassClass = species.getCelldesignerAnnotation().getCelldesignerSpeciesIdentity().getCelldesignerClass();
        this.cdClass = cdClassClass.getDomNode().getChildNodes().item(0).getNodeValue();
        this.isComplex = this.cdClass.equals("COMPLEX");
        this.id = species.getId();
        this.name = species.getName().getStringValue();
        CelldesignerComplexSpecies complexSpecies = species.getCelldesignerAnnotation().getCelldesignerComplexSpecies();
        this.complex = complexSpecies.getDomNode().getChildNodes().item(0).getNodeValue();
        this.compartment = null;
        this.aliases = new ArrayList<>();

        if(this.isComplex) {
            for(CelldesignerComplexSpeciesAlias complexAlias : modelW.getComplexSpeciesAliasFor(this.id)) {
                if (complexAlias == null) {
                    continue;
                }
                this.aliases.add(new AliasWrapper(complexAlias, this));
            }
        }
        else {
            for(CelldesignerSpeciesAlias alias : modelW.getSpeciesAliasFor(this.id)) {
                if (alias == null) {
                    continue;
                }
                this.aliases.add(new AliasWrapper(alias, this));
            }
        }

        this.sbgnClass = getSbgnClass(cdClass);
        this.cdShape = getSbgnShape(sbgnClass);
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

            case "DEGRADED": return "source and sink"; // TODO check if it is ok
        }
        throw new IllegalArgumentException("Could not infer SBGN class from species class: "+cdClass);
    }

    public String getSbgnClass() {
        return sbgnClass;
    }

    public static CdShape getSbgnShape(String sbgnClass) {
        switch(sbgnClass) {
            case "macromolecule":
            case "nucleic acid feature":
            case "complex": return CdShape.RECTANGLE;
            case "phenotype": return CdShape.PHENOTYPE;
            case "simple chemical":
            case "unspecified entity":
            case "source and sink": return CdShape.ELLIPSE;
        }
        throw new IllegalArgumentException("Invalid SBGN class given: "+sbgnClass);
    }

    public CdShape getCdShape() {
        return cdShape;
    }

    public boolean isIncludedSpecies() {
        return isIncludedSpecies;
    }

    public boolean isComplex() {
        return isComplex;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCompartment() {
        return compartment;
    }

    public String getComplex() {
        return complex;
    }

    public String getCdClass() {
        return cdClass;
    }

    public List<AliasWrapper> getAliases() {
        return aliases;
    }

}
