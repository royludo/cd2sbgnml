/**
 * Represents things that can be linked, even without a concrete existence in CellDesigner
 * like processes and logic gate. Also represents an abstraction of species as glyphs on
 * the map.
 * Everything that can be linked.
 */
public class AbstractLinkableCDEntity {

    public enum AbstractLinkableCDEntityType {SPECIES, LOGIC_GATE, PROCESS}
    private AbstractLinkableCDEntityType type;

    public AbstractLinkableCDEntity(AbstractLinkableCDEntityType type) {
        this.type = type;
    }

    public AbstractLinkableCDEntityType getType() {
        return type;
    }


}
