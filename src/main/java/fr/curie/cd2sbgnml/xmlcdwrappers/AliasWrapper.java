package fr.curie.cd2sbgnml.xmlcdwrappers;

import org.sbml._2001.ns.celldesigner.*;
import org.sbml._2001.ns.celldesigner.ComplexSpeciesAlias.BackupSize;
import org.w3c.dom.Node;

import java.awt.geom.Point2D;
import java.math.BigDecimal;

/**
 * wraps speciesAlias and complexSpeciesAlias as they have a lot in common
 */
public class AliasWrapper {

    public enum AliasType {COMPLEX, COMPARTMENT, BASIC}

    private Bounds bounds;
    private AliasType aliasType;
    private String id;
    private String complexAlias;
    private String compartmentAlias;
    private String speciesId;
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
        this.speciesId = speciesW.getId();
        // TODO if time, use this for proper ion channel state
        this.isActive = false; // no notion of activity in SBGN
    }

    public AliasWrapper(SpeciesAlias alias, SpeciesWrapper speciesW) {
        this.aliasType = AliasType.BASIC;
        this.bounds = alias.getBounds();
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
        this.isActive = alias.getActivity().equals("active");
        //this.addAliasInfo(alias.getDomNode());
        if(!alias.getInfo().getState().equals("empty")) {
            this.info = new AliasInfoWrapper(
                    alias.getInfo().getAngle().floatValue(),
                    alias.getInfo().getPrefix(),
                    alias.getInfo().getLabel());
        }
        this.styleInfo = new StyleInfo(alias, speciesW.getId()+"_"+this.id);

    }

    public AliasWrapper(ComplexSpeciesAlias alias, SpeciesWrapper speciesW) {
        this.aliasType = AliasType.COMPLEX;
        this.bounds = alias.getBounds();
        this.id = alias.getId();
        this.speciesId = alias.getSpecies();
        this.speciesW = speciesW;

        // TODO is this ok ?
        ListOfComplexSpeciesAliases.ComplexSpeciesAlias a = (ListOfComplexSpeciesAliases.ComplexSpeciesAlias) alias;
        this.complexAlias = a.getComplexSpeciesAlias();
        // complexSpeciesAlias cannot be accessed through standard object structure
        // probably because of outdated api of binom
        // we need to fetch it manually in the xml, if present
        /*if(alias.getDomNode().getAttributes().getNamedItem("complexSpeciesAlias") != null) {
            this.complexAlias = alias.getDomNode().getAttributes().getNamedItem("complexSpeciesAlias").getNodeValue();
        }
        else {
            this.complexAlias = null;
        }*/

         //alias.getDomNode().getAttributes().getNamedItem("complexSpeciesAlias").getNodeValue();
        this.compartmentAlias = alias.getCompartmentAlias();
        this.isActive = alias.getActivity ().equals("active");
        //this.addAliasInfo(alias.getDomNode());
        if(!alias.getInfo().getState().equals("empty")) {
            this.info = new AliasInfoWrapper(
                    alias.getInfo().getAngle().floatValue(),
                    alias.getInfo().getPrefix(),
                    alias.getInfo().getLabel());
        }
        this.styleInfo = new StyleInfo(alias, speciesW.getId()+"_"+this.id);
    }

    public AliasWrapper(CompartmentAlias alias, SpeciesWrapper speciesW) {
        this.aliasType = AliasType.COMPARTMENT;
        this.bounds = alias.getBounds();
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
                this.bounds.getX().floatValue() + this.bounds.getW().floatValue() / 2,
                this.bounds.getY().floatValue() + this.bounds.getH().floatValue() / 2);
    }

    /*
        the following 2 functions are huge duplicated chunks, TODO find a way to change that
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

        Bounds bounds = new Bounds();
        alias.setBounds(bounds);
        bounds.setX(this.getBounds().getX());
        bounds.setY(this.getBounds().getY());
        bounds.setW(this.getBounds().getW());
        bounds.setH(this.getBounds().getH());

        SpeciesAlias.Font font = new SpeciesAlias.Font();
        font.setSize((int) this.getStyleInfo().getFontSize());
        alias.setFont(font);

        View view = new View();
        view.setState("usual");
        alias.setView(view);

        // the 2 views components (will be the same)
        InnerPosition innerPosition = new InnerPosition();
        innerPosition.setX(BigDecimal.valueOf(0));
        innerPosition.setY(BigDecimal.valueOf(0));

        BoxSize boxSize = new BoxSize();
        boxSize.setWidth(this.getBounds().getW());
        boxSize.setHeight(this.getBounds().getH());

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
        alias.setUsualView(usualView);

        BriefView briefView = new BriefView();
        briefView.setBoxSize(boxSize);
        briefView.setInnerPosition(innerPosition);
        briefView.setPaint(paint);
        briefView.setSingleLine(singleLine);
        alias.setBriefView(briefView);

        Info info = new Info();
        System.out.println("in cd of alias: "+this.getInfo());
        if(this.getInfo() != null) {
            info.setState("open");
            info.setAngle(BigDecimal.valueOf(this.getInfo().angle));
            info.setPrefix(this.getInfo().prefix);
            info.setLabel(this.getInfo().label);
            System.out.println("get CD of info: "+info);
        }
        else {
            info.setState("empty");
            info.setAngle(BigDecimal.valueOf(-1.57));
        }
        alias.setInfo(info);

        return alias;
    }

    public ListOfComplexSpeciesAliases.ComplexSpeciesAlias getCDComplexSpeciesAlias() {
        ListOfComplexSpeciesAliases.ComplexSpeciesAlias alias = new ListOfComplexSpeciesAliases.ComplexSpeciesAlias();
        alias.setId(this.getId());
        alias.setSpecies(this.getSpeciesId());
        alias.setCompartmentAlias(this.getCompartmentAlias());
        alias.setActivity("inactive");
        if(this.getComplexAlias() != null) {
            alias.setComplexSpeciesAlias(this.getComplexAlias());
        }

        Bounds bounds = new Bounds();
        alias.setBounds(bounds);
        bounds.setX(this.getBounds().getX());
        bounds.setY(this.getBounds().getY());
        bounds.setW(this.getBounds().getW());
        bounds.setH(this.getBounds().getH());

        ComplexSpeciesAlias.Font font = new ComplexSpeciesAlias.Font();
        font.setSize((int) this.getStyleInfo().getFontSize());
        alias.setFont(font);

        View view = new View();
        view.setState("usual");
        alias.setView(view);

        BackupSize backupSize = new BackupSize();
        alias.setBackupSize(backupSize);
        backupSize.setW(0d);
        backupSize.setH(0d);

        View backupView = new View();
        backupView.setState("none");
        alias.setBackupView(backupView);

        // the 2 views components (will be the same)
        InnerPosition innerPosition = new InnerPosition();
        innerPosition.setX(BigDecimal.valueOf(0));
        innerPosition.setY(BigDecimal.valueOf(0));

        BoxSize boxSize = new BoxSize();
        boxSize.setWidth(this.getBounds().getW());
        boxSize.setHeight(this.getBounds().getH());

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
        alias.setUsualView(usualView);

        BriefView briefView = new BriefView();
        briefView.setBoxSize(boxSize);
        briefView.setInnerPosition(innerPosition);
        briefView.setPaint(paint);
        briefView.setSingleLine(singleLine);
        alias.setBriefView(briefView);

        Info info = new Info();
        System.out.println("in cd of alias: "+this.getInfo());
        if(this.getInfo() != null) {
            info.setState("open");
            info.setAngle(BigDecimal.valueOf(this.getInfo().angle));
            info.setPrefix(this.getInfo().prefix);
            info.setLabel(this.getInfo().label);
            System.out.println("get CD of info: "+info);
        }
        else {
            info.setState("empty");
            info.setAngle(BigDecimal.valueOf(-1.57));
        }
        alias.setInfo(info);

        return alias;
    }


    public Bounds getBounds() {
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

    public void setBounds(Bounds bounds) {
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

    public void setSpeciesId(String speciesId) {
        this.speciesId = speciesId;
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
