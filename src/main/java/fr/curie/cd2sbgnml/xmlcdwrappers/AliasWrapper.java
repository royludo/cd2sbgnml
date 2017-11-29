package fr.curie.cd2sbgnml.xmlcdwrappers;

import org.sbml._2001.ns.celldesigner.*;
import org.sbml._2001.ns.celldesigner.ComplexSpeciesAlias.BackupSize;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.math.BigDecimal;
import java.util.AbstractMap.SimpleEntry;

import static fr.curie.cd2sbgnml.xmlcdwrappers.Utils.bounds2Rect;
import static fr.curie.cd2sbgnml.xmlcdwrappers.Utils.rect2Bounds;

/**
 * Wraps speciesAlias and complexSpeciesAlias xml entities as they have a lot in common.
 * Compartments are too different and are treated separately.
 */
public class AliasWrapper {

    /**
     * The different aliases possible. Aliases exist for complexes, species.
     */
    public enum AliasType {COMPLEX, SPECIES}

    /**
     * Retains the information about the original type of the alias, species or complexSpecies.
     */
    private AliasType aliasType;
    private Rectangle2D bounds; // TODO replace by a more generic Rect2D
    private String id;
    private String complexAlias;
    private String compartmentAlias;
    private SpeciesWrapper speciesW;

    /**
     * For ion channels, determines if open (active) or closed (inactive)
     */
    private boolean isActive;
    private AliasInfoWrapper info;
    private StyleInfo styleInfo;

    /**
     * When an included species interacts in a reaction, its speciesReference isn't listed in the sbml listOfReactants,
     * listOfProducts or listOfModifiers. Instead, it's its top level complex's speciesReference that is used.
     * So we need to keep track of the parent complex's aliasWrapper.
     */
    private AliasWrapper topLevelParent;

    public AliasWrapper(String id, AliasType type, SpeciesWrapper speciesW) {
        this.id = id;
        this.aliasType = type;
        this.speciesW = speciesW;
        // TODO if time, use this for proper ion channel state
        this.isActive = false; // no notion of activity in SBGN
    }

    /**
     * Parse speciesAlias from CellDesigner file.
     * @param alias the speciesAlias xml element
     * @param speciesW the parent species wrapper
     */
    public AliasWrapper(SpeciesAlias alias, SpeciesWrapper speciesW) {
        this.aliasType = AliasType.SPECIES;
        this.bounds = bounds2Rect(alias.getBounds());
        this.id = alias.getId();
        this.complexAlias = alias.getComplexSpeciesAlias();
        this.speciesW = speciesW;
        this.compartmentAlias = alias.getCompartmentAlias();
        this.isActive = alias.getActivity().equals("active");

        if(!alias.getInfo().getState().equals("empty")) {
            this.info = new AliasInfoWrapper(
                    alias.getInfo().getAngle().floatValue(),
                    alias.getInfo().getPrefix(),
                    alias.getInfo().getLabel());
        }
        this.styleInfo = new StyleInfo(alias, speciesW.getId()+"_"+this.id);

    }

    /**
     * Parse complexSpeciesAlias from CellDesigner file.
     * @param alias the complexSpeciesAlias xml element
     * @param speciesW the parent species wrapper
     */
    public AliasWrapper(ComplexSpeciesAlias alias, SpeciesWrapper speciesW) {
        this.aliasType = AliasType.COMPLEX;
        this.bounds = bounds2Rect(alias.getBounds());
        this.id = alias.getId();
        this.speciesW = speciesW;

        ListOfComplexSpeciesAliases.ComplexSpeciesAlias a = (ListOfComplexSpeciesAliases.ComplexSpeciesAlias) alias;
        this.complexAlias = a.getComplexSpeciesAlias();
        this.compartmentAlias = alias.getCompartmentAlias();
        this.isActive = alias.getActivity ().equals("active");

        if(!alias.getInfo().getState().equals("empty")) {
            this.info = new AliasInfoWrapper(
                    alias.getInfo().getAngle().floatValue(),
                    alias.getInfo().getPrefix(),
                    alias.getInfo().getLabel());
        }
        this.styleInfo = new StyleInfo(alias, speciesW.getId()+"_"+this.id);
    }

    /**
     * @return the center coordinates determined from the alias' bounds.
     */
    public Point2D.Float getCenterPoint() {
        return new Point2D.Float(
                (float) this.bounds.getCenterX(),
                (float) this.bounds.getCenterY());
    }

    /**
     * Generates the xml for the speciesAlias element.
     * @return a fully built SpeciesAlias object
     */
    public SpeciesAlias getCDSpeciesAlias() {
        SpeciesAlias alias = new SpeciesAlias();
        alias.setId(this.getId());
        alias.setSpecies(this.getSpeciesId());
        alias.setCompartmentAlias(this.getCompartmentAlias());
        alias.setActivity("inactive");
        if(this.getComplexAlias() != null) {
            alias.setComplexSpeciesAlias(this.getComplexAlias());
        }

        alias.setBounds(this.getBoundsElement());
        alias.setView(this.getSimpleViewElement());
        SimpleEntry<UsualView, BriefView> viewsEntry = this.getViewElements();
        alias.setUsualView(viewsEntry.getKey());
        alias.setBriefView(viewsEntry.getValue());
        alias.setInfo(this.getInfoElement());
        // end of part that is the same as to ComplexSpeciesAlias, rest varies

        SpeciesAlias.Font font = new SpeciesAlias.Font();
        font.setSize((int) this.getStyleInfo().getFontSize());
        alias.setFont(font);

        return alias;
    }

    /**
     * Generates the xml for the complexSpeciesAlias element.
     * @return a fully built ComplexSpeciesAlias object
     */
    public ListOfComplexSpeciesAliases.ComplexSpeciesAlias getCDComplexSpeciesAlias() {
        ListOfComplexSpeciesAliases.ComplexSpeciesAlias alias = new ListOfComplexSpeciesAliases.ComplexSpeciesAlias();
        alias.setId(this.getId());
        alias.setSpecies(this.getSpeciesId());
        alias.setCompartmentAlias(this.getCompartmentAlias());
        alias.setActivity("inactive");
        if(this.getComplexAlias() != null) {
            alias.setComplexSpeciesAlias(this.getComplexAlias());
        }

        alias.setBounds(this.getBoundsElement());
        alias.setView(this.getSimpleViewElement());
        SimpleEntry<UsualView, BriefView> viewsEntry = this.getViewElements();
        alias.setUsualView(viewsEntry.getKey());
        alias.setBriefView(viewsEntry.getValue());
        alias.setInfo(this.getInfoElement());
        // end of part that is the same as to SpeciesAlias, rest varies

        ComplexSpeciesAlias.Font font = new ComplexSpeciesAlias.Font();
        font.setSize((int) this.getStyleInfo().getFontSize());
        alias.setFont(font);

        BackupSize backupSize = new BackupSize();
        alias.setBackupSize(backupSize);
        backupSize.setW(0d);
        backupSize.setH(0d);

        View backupView = new View();
        backupView.setState("none");
        alias.setBackupView(backupView);

        return alias;
    }

    private View getSimpleViewElement() {
        View view = new View();
        view.setState("usual");
        return view;
    }

    private Bounds getBoundsElement() {
        return rect2Bounds(this.getBounds());
    }

    private Info getInfoElement() {
        Info info = new Info();
        if(this.getInfo() != null) {
            info.setState("open");
            info.setAngle(BigDecimal.valueOf(this.getInfo().angle));
            info.setPrefix(this.getInfo().prefix);
            info.setLabel(this.getInfo().label);
        }
        else {
            info.setState("empty");
            info.setAngle(BigDecimal.valueOf(-1.57));
        }
        return info;
    }

    private SimpleEntry<UsualView, BriefView> getViewElements() {
        // the 2 views components (will be the same)
        InnerPosition innerPosition = new InnerPosition();
        innerPosition.setX(BigDecimal.valueOf(0));
        innerPosition.setY(BigDecimal.valueOf(0));

        BoxSize boxSize = new BoxSize();
        boxSize.setWidth(BigDecimal.valueOf(this.getBounds().getWidth()));
        boxSize.setHeight(BigDecimal.valueOf(this.getBounds().getHeight()));

        SingleLine singleLine = new SingleLine();
        singleLine.setWidth(BigDecimal.valueOf(this.getStyleInfo().getLineWidth()));

        Paint paint = new Paint();
        paint.setColor(this.getStyleInfo().getBgColor());
        paint.setScheme("Color");

        UsualView usualView = new UsualView();
        usualView.setBoxSize(boxSize);
        usualView.setInnerPosition(innerPosition);
        usualView.setPaint(paint);
        usualView.setSingleLine(singleLine);

        BriefView briefView = new BriefView();
        briefView.setBoxSize(boxSize);
        briefView.setInnerPosition(innerPosition);
        briefView.setPaint(paint);
        briefView.setSingleLine(singleLine);

        return new SimpleEntry<>(usualView, briefView);
    }


    public Rectangle2D getBounds() {
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
        return speciesW.getId();
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

    public void setBounds(Rectangle2D bounds) {
        this.bounds = bounds;
    }

    public void setAliasType(AliasType aliasType) {
        this.aliasType = aliasType;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setComplexAlias(String complexAlias) {
        this.complexAlias = complexAlias;
    }

    public void setCompartmentAlias(String compartmentAlias) {
        this.compartmentAlias = compartmentAlias;
    }

    public void setSpeciesW(SpeciesWrapper speciesW) {
        this.speciesW = speciesW;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public void setInfo(AliasInfoWrapper info) {
        this.info = info;
    }

    public void setStyleInfo(StyleInfo styleInfo) {
        this.styleInfo = styleInfo;
    }

    public AliasWrapper getTopLevelParent() {
        return topLevelParent;
    }

    public void setTopLevelParent(AliasWrapper topLevelParent) {
        this.topLevelParent = topLevelParent;
    }
}
