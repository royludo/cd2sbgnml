package fr.curie.cd2sbgnml.xmlcdwrappers;


import org.sbml._2001.ns.celldesigner.Modification;

public class LogicGateWrapper extends ReactantWrapper {

    public enum LogicGateType {AND, OR, NOT, UNKNOWN}

    private LogicGateType type;
    private String modifiers;
    private String aliases;
    private ReactantWrapper.ModificationLinkType logicGateModificationType;

    public LogicGateWrapper(Modification modif, int i) {
        super(modif, null, i);

        this.setPositionIndex(i);
        this.modifiers = modif.getModifiers();
        this.aliases = modif.getAliases();
        System.out.println("LOGIC MODIFICAITON "+modif.getModificationType());
        this.logicGateModificationType = ModificationLinkType.valueOf(modif.getModificationType());
        switch (ModificationLinkType.valueOf(modif.getType())) {
            case BOOLEAN_LOGIC_GATE_OR: this.type = LogicGateType.OR; break;
            case BOOLEAN_LOGIC_GATE_AND: this.type = LogicGateType.AND; break;
            case BOOLEAN_LOGIC_GATE_NOT: this.type = LogicGateType.NOT; break;
            case BOOLEAN_LOGIC_GATE_UNKNOWN: this.type = LogicGateType.UNKNOWN; break;
            default:
                throw new IllegalArgumentException("Modification type: "+modif.getType()+" is not a logic gate type.");
        }
    }

    public LogicGateType getType() {
        return type;
    }

    public String getModificationType() {
        return this.logicGateModificationType.toString();
    }

    public void setType(LogicGateType type) {
        this.type = type;
    }

    public String getModifiers() {
        return modifiers;
    }

    public void setModifiers(String modifiers) {
        this.modifiers = modifiers;
    }

    public String getAliases() {
        return aliases;
    }

    public void setAliases(String aliases) {
        this.aliases = aliases;
    }

    public void setLogicGateModificationType(ModificationLinkType logicGateModificationType) {
        this.logicGateModificationType = logicGateModificationType;
    }
}
