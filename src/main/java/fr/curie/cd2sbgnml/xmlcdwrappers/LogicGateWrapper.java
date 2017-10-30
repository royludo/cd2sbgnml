package fr.curie.cd2sbgnml.xmlcdwrappers;


import org.sbml._2001.ns.celldesigner.Modification;

public class LogicGateWrapper {

    public enum LogicGateType {AND, OR, NOT, UNKNOWN}

    private LogicGateType type;
    private String modificationType;
    private Modification modification;

    /**
     * The index of the reactant in the corresponding list (modification,  additional product...) in the
     * xml file
     */
    private int positionIndex;

    public LogicGateWrapper(Modification modif, int i) {
        this.positionIndex = i;
        System.out.println("LOGIC MODIFICAITON "+modif.getModificationType());
        this.modificationType = modif.getModificationType();
        switch (modif.getType()) {
            case "BOOLEAN_LOGIC_GATE_OR": this.type = LogicGateType.OR; break;
            case "BOOLEAN_LOGIC_GATE_AND": this.type = LogicGateType.AND; break;
            case "BOOLEAN_LOGIC_GATE_NOT": this.type = LogicGateType.NOT; break;
            case "BOOLEAN_LOGIC_GATE_UNKNOWN": this.type = LogicGateType.UNKNOWN; break;
            default:
                throw new IllegalArgumentException("Modification type: "+modif.getType()+" is not a logic gate type.");
        }
        this.modification = modif;
    }

    public LogicGateType getType() {
        return type;
    }

    public String getModificationType() {
        return modificationType;
    }

    public int getPositionIndex() {
        return positionIndex;
    }

    public Modification getModification() {
        return modification;
    }
}
