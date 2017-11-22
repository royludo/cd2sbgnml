package fr.curie.cd2sbgnml.xmlcdwrappers;

import fr.curie.cd2sbgnml.graphics.GeometryUtils;
import org.sbml._2001.ns.celldesigner.*;
import org.sbml.sbml.level2.version4.Compartment;
import org.sbml.sbml.level2.version4.SBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.math.BigDecimal;

public class CompartmentWrapper implements INotesFeature, IAnnotationsFeature {

    private final Logger logger = LoggerFactory.getLogger(CompartmentWrapper.class);

    private String id;
    private String metaid;
    private String name;
    private double size = 1;
    private String units = "volume";
    private String outside = "default";
    private Element notes;
    private Element annotations;
    private Rectangle2D bbox;
    private String cdClass = "SQUARE";
    private String aliasId;
    private Point2D namePoint;
    private StyleInfo styleInfo;
    private String infoState = "empty";
    private float infoAngle = -1.57f;


    public CompartmentWrapper(String id, String name, Rectangle2D bbox) {
        this.id = id;
        this.name = name;
        this.bbox = bbox;

        this.metaid = id;
        this.aliasId = id+"_alias1";
        this.namePoint = getDefaultNamePoint(name, id, bbox);
        this.styleInfo = new StyleInfo(id);
    }

    public static Point2D getDefaultNamePoint(String name, String id, Rectangle2D bbox) {
        String s;
        if(name.isEmpty()) {
            s = id;
        }
        else {
            s = name;
        }
        // centered horizontally
        float x = (float) (bbox.getX() + bbox.getWidth() / 2 - GeometryUtils.getLengthForString(s) / 2);
        // at the bottom
        float y = (float) (bbox.getY() + bbox.getHeight() * 0.95);
        return new Point2D.Float(x, y);
    }

    public Compartment getCDCompartment() {
        Compartment compartment = new Compartment();
        compartment.setId(this.getId());
        compartment.setMetaid(this.getMetaid());
        compartment.setSize(this.getSize());
        compartment.setUnits(this.getUnits());
        compartment.setName(this.getName());
        compartment.setOutside(this.getOutside());

        CompartmentAnnotationType annotation = new CompartmentAnnotationType();
        compartment.setAnnotation(annotation);

        CompartmentAnnotationType.Extension extension = new CompartmentAnnotationType.Extension();
        annotation.setExtension(extension);

        extension.setName(this.getName());

        if(this.getNotes() != null
                && !Utils.isNoteEmpty(this.getNotes())) {
            SBase.Notes notes = new SBase.Notes();
            notes.getAny().add(this.getNotes());
            compartment.setNotes(notes);
        }

        annotation.getAny().add(this.getAnnotations());
        return compartment;
    }

    public CompartmentAlias getCDCompartmentAlias() {

        CompartmentAlias compAlias = new CompartmentAlias();
        compAlias.setId(this.getAliasId());
        compAlias.setCompartment(this.getId());
        compAlias.setClazz(this.getCdClass());

        Bounds bounds = new Bounds();
        compAlias.setBounds(bounds);
        bounds.setX(BigDecimal.valueOf(this.getBbox().getX()));
        bounds.setY(BigDecimal.valueOf(this.getBbox().getY()));
        bounds.setW(BigDecimal.valueOf(this.getBbox().getWidth()));
        bounds.setH(BigDecimal.valueOf(this.getBbox().getHeight()));

        Point namePoint = new Point();
        compAlias.setNamePoint(namePoint);
        namePoint.setX(BigDecimal.valueOf(this.namePoint.getX()));
        namePoint.setY(BigDecimal.valueOf(this.namePoint.getY()));

        Info info = new Info();
        compAlias.setInfo(info);
        info.setState(this.getInfoState());
        info.setAngle(BigDecimal.valueOf(this.getInfoAngle()));

        Paint paint = new Paint();
        compAlias.setPaint(paint);
        paint.setScheme("Color");
        paint.setColor(this.getStyleInfo().getLineColor());

        DoubleLine dbline = new DoubleLine();
        compAlias.setDoubleLine(dbline);
        /*
            There are some rules associated with thickness and outer and inner width for compartments.
         */
        float lineWidth = this.getStyleInfo().getLineWidth();
        lineWidth = lineWidth < 3 ? 3: lineWidth; // needs to be at least 3
        float outerwidth = (lineWidth - 1) / 2 + 1;
        outerwidth = outerwidth > 5 ? 5 : outerwidth; // limit max size
        float innerwidth = (lineWidth -1) / 2;
        innerwidth = innerwidth > 4 ? 4 : innerwidth; // limit max size, needs to be less than outer

        dbline.setThickness(BigDecimal.valueOf(lineWidth));
        dbline.setOuterWidth(BigDecimal.valueOf(outerwidth));
        dbline.setInnerWidth(BigDecimal.valueOf(innerwidth));


        return compAlias;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMetaid() {
        return metaid;
    }

    public void setMetaid(String metaid) {
        this.metaid = metaid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getSize() {
        return size;
    }

    public void setSize(float size) {
        this.size = size;
    }

    public String getUnits() {
        return units;
    }

    public void setUnits(String units) {
        this.units = units;
    }

    public String getOutside() {
        return outside;
    }

    public void setOutside(String outside) {
        this.outside = outside;
    }

    public Element getNotes() {
        return notes;
    }

    public void setNotes(Element notes) {
        this.notes = notes;
    }

    public Element getAnnotations() {
        return annotations;
    }

    public void setAnnotations(Element annotations) {
        this.annotations = annotations;
    }

    public Rectangle2D getBbox() {
        return bbox;
    }

    public void setBbox(Rectangle2D bbox) {
        this.bbox = bbox;
    }

    public String getCdClass() {
        return cdClass;
    }

    public void setCdClass(String cdClass) {
        this.cdClass = cdClass;
    }

    public String getAliasId() {
        return aliasId;
    }

    public void setAliasId(String aliasId) {
        this.aliasId = aliasId;
    }

    public Point2D getNamePoint() {
        return namePoint;
    }

    public void setNamePoint(Point2D namePoint) {
        this.namePoint = namePoint;
    }

    public StyleInfo getStyleInfo() {
        return styleInfo;
    }

    public void setStyleInfo(StyleInfo styleInfo) {
        this.styleInfo = styleInfo;
    }

    public String getInfoState() {
        return infoState;
    }

    public void setInfoState(String infoState) {
        this.infoState = infoState;
    }

    public float getInfoAngle() {
        return infoAngle;
    }

    public void setInfoAngle(float infoAngle) {
        this.infoAngle = infoAngle;
    }
}
