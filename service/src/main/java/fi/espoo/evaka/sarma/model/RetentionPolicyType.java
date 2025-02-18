
package fi.espoo.evaka.sarma.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;


/**
 * <p>Java class for RetentionPolicyType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="RetentionPolicyType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="retentionPeriod" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *         &lt;element name="retentionTrigger" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="retentionReason" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "RetentionPolicyType", namespace = "http://www.avaintec.com/2004/records-schedule-fi/1.0", propOrder = {
    "retentionPeriod",
    "retentionTrigger",
    "retentionReason"
})
public class RetentionPolicyType {

    @XmlElement(required = true)
    protected String retentionPeriod;
    protected String retentionTrigger;
    protected String retentionReason;

    /**
     * Gets the value of the retentionPeriod property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getRetentionPeriod() {
        return retentionPeriod;
    }

    /**
     * Sets the value of the retentionPeriod property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setRetentionPeriod(String value) {
        this.retentionPeriod = value;
    }

    /**
     * Gets the value of the retentionTrigger property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getRetentionTrigger() {
        return retentionTrigger;
    }

    /**
     * Sets the value of the retentionTrigger property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setRetentionTrigger(String value) {
        this.retentionTrigger = value;
    }

    /**
     * Gets the value of the retentionReason property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getRetentionReason() {
        return retentionReason;
    }

    /**
     * Sets the value of the retentionReason property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setRetentionReason(String value) {
        this.retentionReason = value;
    }

}
