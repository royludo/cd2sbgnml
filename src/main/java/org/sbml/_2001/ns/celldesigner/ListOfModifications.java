//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4-2 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package org.sbml._2001.ns.celldesigner;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * List of residue modification.
 * 
 * <p>Java class for listOfModifications complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="listOfModifications">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence maxOccurs="unbounded">
 *         &lt;element name="modification">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;attribute name="residue" use="required" type="{http://www.sbml.org/2001/ns/celldesigner}SId" />
 *                 &lt;attribute name="state" use="required">
 *                   &lt;simpleType>
 *                     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *                       &lt;enumeration value="phosphorylated"/>
 *                       &lt;enumeration value="acetylated"/>
 *                       &lt;enumeration value="ubiquitinated"/>
 *                       &lt;enumeration value="methylated"/>
 *                       &lt;enumeration value="hydroxylated"/>
 *                       &lt;enumeration value="don't care"/>
 *                       &lt;enumeration value="unknown"/>
 *                       &lt;enumeration value="glycosylated"/>
 *                       &lt;enumeration value="myristoylated"/>
 *                       &lt;enumeration value="palmytoylated"/>
 *                       &lt;enumeration value="prenylated"/>
 *                       &lt;enumeration value="protonated"/>
 *                       &lt;enumeration value="sulfated"/>
 *                     &lt;/restriction>
 *                   &lt;/simpleType>
 *                 &lt;/attribute>
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "listOfModifications", propOrder = {
    "modification"
})
public class ListOfModifications {

    @XmlElement(required = true)
    protected List<ListOfModifications.Modification> modification;

    /**
     * Gets the value of the modification property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the modification property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getModification().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link ListOfModifications.Modification }
     * 
     * 
     */
    public List<ListOfModifications.Modification> getModification() {
        if (modification == null) {
            modification = new ArrayList<ListOfModifications.Modification>();
        }
        return this.modification;
    }


    /**
     * <p>Java class for anonymous complex type.
     * 
     * <p>The following schema fragment specifies the expected content contained within this class.
     * 
     * <pre>
     * &lt;complexType>
     *   &lt;complexContent>
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *       &lt;attribute name="residue" use="required" type="{http://www.sbml.org/2001/ns/celldesigner}SId" />
     *       &lt;attribute name="state" use="required">
     *         &lt;simpleType>
     *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
     *             &lt;enumeration value="phosphorylated"/>
     *             &lt;enumeration value="acetylated"/>
     *             &lt;enumeration value="ubiquitinated"/>
     *             &lt;enumeration value="methylated"/>
     *             &lt;enumeration value="hydroxylated"/>
     *             &lt;enumeration value="don't care"/>
     *             &lt;enumeration value="unknown"/>
     *             &lt;enumeration value="glycosylated"/>
     *             &lt;enumeration value="myristoylated"/>
     *             &lt;enumeration value="palmytoylated"/>
     *             &lt;enumeration value="prenylated"/>
     *             &lt;enumeration value="protonated"/>
     *             &lt;enumeration value="sulfated"/>
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
    @XmlType(name = "")
    public static class Modification {

        @XmlAttribute(name = "residue", required = true)
        protected String residue;
        @XmlAttribute(name = "state", required = true)
        protected String state;

        /**
         * Gets the value of the residue property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getResidue() {
            return residue;
        }

        /**
         * Sets the value of the residue property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setResidue(String value) {
            this.residue = value;
        }

        /**
         * Gets the value of the state property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getState() {
            return state;
        }

        /**
         * Sets the value of the state property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setState(String value) {
            this.state = value;
        }

    }

}
