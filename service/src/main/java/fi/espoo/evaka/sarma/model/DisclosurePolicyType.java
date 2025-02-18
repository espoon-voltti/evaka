
package fi.espoo.evaka.sarma.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlSchemaType;
import jakarta.xml.bind.annotation.XmlType;


/**
 * <p>Java class for DisclosurePolicyType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="DisclosurePolicyType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="disclosureLevel" type="{http://www.avaintec.com/2004/records-schedule-fi/1.0}DisclosureLevelType"/&gt;
 *         &lt;element name="disclosureReason" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="disclosurePeriod" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DisclosurePolicyType", namespace = "http://www.avaintec.com/2004/records-schedule-fi/1.0", propOrder = {
    "disclosureLevel",
    "disclosureReason",
    "disclosurePeriod"
})
public class DisclosurePolicyType {

    @XmlElement(required = true)
    @XmlSchemaType(name = "string")
    protected DisclosureLevelType disclosureLevel;
    protected String disclosureReason;
    protected String disclosurePeriod;

    /**
     * Gets the value of the disclosureLevel property.
     * 
     * @return
     *     possible object is
     *     {@link DisclosureLevelType }
     *     
     */
    public DisclosureLevelType getDisclosureLevel() {
        return disclosureLevel;
    }

    /**
     * Sets the value of the disclosureLevel property.
     * 
     * @param value
     *     allowed object is
     *     {@link DisclosureLevelType }
     *     
     */
    public void setDisclosureLevel(DisclosureLevelType value) {
        this.disclosureLevel = value;
    }

    /**
     * Gets the value of the disclosureReason property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDisclosureReason() {
        return disclosureReason;
    }

    /**
     * Sets the value of the disclosureReason property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDisclosureReason(String value) {
        this.disclosureReason = value;
    }

    /**
     * Gets the value of the disclosurePeriod property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDisclosurePeriod() {
        return disclosurePeriod;
    }

    /**
     * Sets the value of the disclosurePeriod property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDisclosurePeriod(String value) {
        this.disclosurePeriod = value;
    }

}
