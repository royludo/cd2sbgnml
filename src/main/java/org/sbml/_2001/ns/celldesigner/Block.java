//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package org.sbml._2001.ns.celldesigner;

import java.math.BigDecimal;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;


/**
 * Block(protein) on canvas.
 * 
 * <p>Java class for block complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="block">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;attribute name="width" use="required">
 *         &lt;simpleType>
 *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}short">
 *             &lt;minInclusive value="0"/>
 *           &lt;/restriction>
 *         &lt;/simpleType>
 *       &lt;/attribute>
 *       &lt;attribute name="height" use="required">
 *         &lt;simpleType>
 *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}short">
 *             &lt;minInclusive value="0"/>
 *           &lt;/restriction>
 *         &lt;/simpleType>
 *       &lt;/attribute>
 *       &lt;attribute name="x" use="required">
 *         &lt;simpleType>
 *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}short">
 *             &lt;minInclusive value="0"/>
 *           &lt;/restriction>
 *         &lt;/simpleType>
 *       &lt;/attribute>
 *       &lt;attribute name="y" use="required">
 *         &lt;simpleType>
 *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}short">
 *             &lt;minInclusive value="0"/>
 *           &lt;/restriction>
 *         &lt;/simpleType>
 *       &lt;/attribute>
 *       &lt;attribute name="nameOffsetX" use="required">
 *         &lt;simpleType>
 *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}decimal">
 *           &lt;/restriction>
 *         &lt;/simpleType>
 *       &lt;/attribute>
 *       &lt;attribute name="nameOffsetY" use="required">
 *         &lt;simpleType>
 *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}decimal">
 *           &lt;/restriction>
 *         &lt;/simpleType>
 *       &lt;/attribute>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "block")
public class Block {

    @XmlAttribute(name = "width", required = true)
    protected short width;
    @XmlAttribute(name = "height", required = true)
    protected short height;
    @XmlAttribute(name = "x", required = true)
    protected short x;
    @XmlAttribute(name = "y", required = true)
    protected short y;
    @XmlAttribute(name = "nameOffsetX", required = true)
    protected BigDecimal nameOffsetX;
    @XmlAttribute(name = "nameOffsetY", required = true)
    protected BigDecimal nameOffsetY;

    /**
     * Gets the value of the width property.
     * 
     */
    public short getWidth() {
        return width;
    }

    /**
     * Sets the value of the width property.
     * 
     */
    public void setWidth(short value) {
        this.width = value;
    }

    /**
     * Gets the value of the height property.
     * 
     */
    public short getHeight() {
        return height;
    }

    /**
     * Sets the value of the height property.
     * 
     */
    public void setHeight(short value) {
        this.height = value;
    }

    /**
     * Gets the value of the x property.
     * 
     */
    public short getX() {
        return x;
    }

    /**
     * Sets the value of the x property.
     * 
     */
    public void setX(short value) {
        this.x = value;
    }

    /**
     * Gets the value of the y property.
     * 
     */
    public short getY() {
        return y;
    }

    /**
     * Sets the value of the y property.
     * 
     */
    public void setY(short value) {
        this.y = value;
    }

    /**
     * Gets the value of the nameOffsetX property.
     * 
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *     
     */
    public BigDecimal getNameOffsetX() {
        return nameOffsetX;
    }

    /**
     * Sets the value of the nameOffsetX property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *     
     */
    public void setNameOffsetX(BigDecimal value) {
        this.nameOffsetX = value;
    }

    /**
     * Gets the value of the nameOffsetY property.
     * 
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *     
     */
    public BigDecimal getNameOffsetY() {
        return nameOffsetY;
    }

    /**
     * Sets the value of the nameOffsetY property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *     
     */
    public void setNameOffsetY(BigDecimal value) {
        this.nameOffsetY = value;
    }

}