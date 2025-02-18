
package fi.espoo.evaka.sarma.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlSchemaType;
import jakarta.xml.bind.annotation.XmlType;


/**
 * <p>Java class for InformationSecurityPolicyType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="InformationSecurityPolicyType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="securityLevel" type="{http://www.avaintec.com/2004/records-schedule-fi/1.0}InformationSecurityLevelType"/&gt;
 *         &lt;element name="securityReason" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="securityPeriod" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "InformationSecurityPolicyType", namespace = "http://www.avaintec.com/2004/records-schedule-fi/1.0", propOrder = {
    "securityLevel",
    "securityReason",
    "securityPeriod"
})
public class InformationSecurityPolicyType {

    @XmlElement(required = true)
    @XmlSchemaType(name = "string")
    protected InformationSecurityLevelType securityLevel;
    protected String securityReason;
    protected String securityPeriod;

    /**
     * Gets the value of the securityLevel property.
     * 
     * @return
     *     possible object is
     *     {@link InformationSecurityLevelType }
     *     
     */
    public InformationSecurityLevelType getSecurityLevel() {
        return securityLevel;
    }

    /**
     * Sets the value of the securityLevel property.
     * 
     * @param value
     *     allowed object is
     *     {@link InformationSecurityLevelType }
     *     
     */
    public void setSecurityLevel(InformationSecurityLevelType value) {
        this.securityLevel = value;
    }

    /**
     * Gets the value of the securityReason property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSecurityReason() {
        return securityReason;
    }

    /**
     * Sets the value of the securityReason property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSecurityReason(String value) {
        this.securityReason = value;
    }

    /**
     * Gets the value of the securityPeriod property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSecurityPeriod() {
        return securityPeriod;
    }

    /**
     * Sets the value of the securityPeriod property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSecurityPeriod(String value) {
        this.securityPeriod = value;
    }

}
