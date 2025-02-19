
package fi.espoo.evaka.sarma.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlSchemaType;
import jakarta.xml.bind.annotation.XmlType;


/**
 * <p>Java class for ProtectionPolicyType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ProtectionPolicyType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="protectionLevel" type="{http://www.avaintec.com/2004/records-schedule-fi/1.0}ProtectionLevelType"/&gt;
 *         &lt;element name="protectionReason" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="protectionMeasures" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ProtectionPolicyType", namespace = "http://www.avaintec.com/2004/records-schedule-fi/1.0", propOrder = {
    "protectionLevel",
    "protectionReason",
    "protectionMeasures"
})
public class ProtectionPolicyType {

    @XmlElement(required = true)
    @XmlSchemaType(name = "string")
    protected ProtectionLevelType protectionLevel;
    protected String protectionReason;
    protected String protectionMeasures;

    /**
     * Gets the value of the protectionLevel property.
     * 
     * @return
     *     possible object is
     *     {@link ProtectionLevelType }
     *     
     */
    public ProtectionLevelType getProtectionLevel() {
        return protectionLevel;
    }

    /**
     * Sets the value of the protectionLevel property.
     * 
     * @param value
     *     allowed object is
     *     {@link ProtectionLevelType }
     *     
     */
    public void setProtectionLevel(ProtectionLevelType value) {
        this.protectionLevel = value;
    }

    /**
     * Gets the value of the protectionReason property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getProtectionReason() {
        return protectionReason;
    }

    /**
     * Sets the value of the protectionReason property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setProtectionReason(String value) {
        this.protectionReason = value;
    }

    /**
     * Gets the value of the protectionMeasures property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getProtectionMeasures() {
        return protectionMeasures;
    }

    /**
     * Sets the value of the protectionMeasures property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setProtectionMeasures(String value) {
        this.protectionMeasures = value;
    }

}
