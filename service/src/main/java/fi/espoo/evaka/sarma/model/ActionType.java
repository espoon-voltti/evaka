
package fi.espoo.evaka.sarma.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlSchemaType;
import jakarta.xml.bind.annotation.XmlType;
import javax.xml.datatype.XMLGregorianCalendar;


/**
 * <p>Java class for actionType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="actionType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="Action_Title" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="Action_Type" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="Action_Created" type="{http://www.w3.org/2001/XMLSchema}date" minOccurs="0"/&gt;
 *         &lt;element name="Action_Accepted" type="{http://www.w3.org/2001/XMLSchema}date" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "actionType", propOrder = {
    "actionTitle",
    "actionType",
    "actionCreated",
    "actionAccepted"
})
public class ActionType {

    @XmlElement(name = "Action_Title")
    protected String actionTitle;
    @XmlElement(name = "Action_Type")
    protected String actionType;
    @XmlElement(name = "Action_Created")
    @XmlSchemaType(name = "date")
    protected XMLGregorianCalendar actionCreated;
    @XmlElement(name = "Action_Accepted")
    @XmlSchemaType(name = "date")
    protected XMLGregorianCalendar actionAccepted;

    /**
     * Gets the value of the actionTitle property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getActionTitle() {
        return actionTitle;
    }

    /**
     * Sets the value of the actionTitle property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setActionTitle(String value) {
        this.actionTitle = value;
    }

    /**
     * Gets the value of the actionType property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getActionType() {
        return actionType;
    }

    /**
     * Sets the value of the actionType property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setActionType(String value) {
        this.actionType = value;
    }

    /**
     * Gets the value of the actionCreated property.
     * 
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public XMLGregorianCalendar getActionCreated() {
        return actionCreated;
    }

    /**
     * Sets the value of the actionCreated property.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public void setActionCreated(XMLGregorianCalendar value) {
        this.actionCreated = value;
    }

    /**
     * Gets the value of the actionAccepted property.
     * 
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public XMLGregorianCalendar getActionAccepted() {
        return actionAccepted;
    }

    /**
     * Sets the value of the actionAccepted property.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public void setActionAccepted(XMLGregorianCalendar value) {
        this.actionAccepted = value;
    }

}
