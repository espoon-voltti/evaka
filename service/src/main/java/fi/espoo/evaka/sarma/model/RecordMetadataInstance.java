
package fi.espoo.evaka.sarma.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="StandardMetadata" type="{http://www.avaintec.com/2005/x-archive/record-metadata-instance/2.0}StandardMetadataType"/&gt;
 *         &lt;element name="ExtendedMetadata" type="{http://www.avaintec.com/2005/x-archive/record-metadata-instance/2.0}ExtendedMetadataType" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "standardMetadata",
    "extendedMetadata"
})
@XmlRootElement(name = "RecordMetadataInstance")
public class RecordMetadataInstance {

    @XmlElement(name = "StandardMetadata", required = true)
    protected StandardMetadataType standardMetadata;
    @XmlElement(name = "ExtendedMetadata")
    protected ExtendedMetadataType extendedMetadata;

    /**
     * Gets the value of the standardMetadata property.
     * 
     * @return
     *     possible object is
     *     {@link StandardMetadataType }
     *     
     */
    public StandardMetadataType getStandardMetadata() {
        return standardMetadata;
    }

    /**
     * Sets the value of the standardMetadata property.
     * 
     * @param value
     *     allowed object is
     *     {@link StandardMetadataType }
     *     
     */
    public void setStandardMetadata(StandardMetadataType value) {
        this.standardMetadata = value;
    }

    /**
     * Gets the value of the extendedMetadata property.
     * 
     * @return
     *     possible object is
     *     {@link ExtendedMetadataType }
     *     
     */
    public ExtendedMetadataType getExtendedMetadata() {
        return extendedMetadata;
    }

    /**
     * Sets the value of the extendedMetadata property.
     * 
     * @param value
     *     allowed object is
     *     {@link ExtendedMetadataType }
     *     
     */
    public void setExtendedMetadata(ExtendedMetadataType value) {
        this.extendedMetadata = value;
    }

}
