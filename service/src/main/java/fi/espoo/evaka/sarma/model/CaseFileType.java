
package fi.espoo.evaka.sarma.model;

import java.util.ArrayList;
import java.util.List;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlSchemaType;
import jakarta.xml.bind.annotation.XmlType;
import javax.xml.datatype.XMLGregorianCalendar;


/**
 * <p>Java class for caseFileType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="caseFileType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="Case_NativeId" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="Case_Title" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="Case_Created" type="{http://www.w3.org/2001/XMLSchema}date" minOccurs="0"/&gt;
 *         &lt;element name="Case_PersonalData" type="{http://www.avaintec.com/2005/x-archive/record-metadata-instance/2.0}personalDataType" minOccurs="0"/&gt;
 *         &lt;element name="Case_PersonalDataCollectionReason" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="Case_RetentionPeriod" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="Case_RetentionReason" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="Case_PublicityClass" type="{http://www.avaintec.com/2004/records-schedule-fi/1.0}DisclosureLevelType" minOccurs="0"/&gt;
 *         &lt;element name="Case_SecurityPeriod" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="Case_SecurityReason" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="Case_Status" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="Case_Function" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="Case_Language" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="Case_Finished" type="{http://www.w3.org/2001/XMLSchema}date" minOccurs="0"/&gt;
 *         &lt;element name="Case_Actions" minOccurs="0"&gt;
 *           &lt;complexType&gt;
 *             &lt;complexContent&gt;
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                 &lt;sequence&gt;
 *                   &lt;element name="Action" type="{http://www.avaintec.com/2005/x-archive/record-metadata-instance/2.0}actionType" maxOccurs="unbounded" minOccurs="0"/&gt;
 *                 &lt;/sequence&gt;
 *               &lt;/restriction&gt;
 *             &lt;/complexContent&gt;
 *           &lt;/complexType&gt;
 *         &lt;/element&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "caseFileType", propOrder = {
    "caseNativeId",
    "caseTitle",
    "caseCreated",
    "casePersonalData",
    "casePersonalDataCollectionReason",
    "caseRetentionPeriod",
    "caseRetentionReason",
    "casePublicityClass",
    "caseSecurityPeriod",
    "caseSecurityReason",
    "caseStatus",
    "caseFunction",
    "caseLanguage",
    "caseFinished",
    "caseActions"
})
public class CaseFileType {

    @XmlElement(name = "Case_NativeId")
    protected String caseNativeId;
    @XmlElement(name = "Case_Title")
    protected String caseTitle;
    @XmlElement(name = "Case_Created")
    @XmlSchemaType(name = "date")
    protected XMLGregorianCalendar caseCreated;
    @XmlElement(name = "Case_PersonalData")
    @XmlSchemaType(name = "string")
    protected PersonalDataType casePersonalData;
    @XmlElement(name = "Case_PersonalDataCollectionReason")
    protected String casePersonalDataCollectionReason;
    @XmlElement(name = "Case_RetentionPeriod")
    protected String caseRetentionPeriod;
    @XmlElement(name = "Case_RetentionReason")
    protected String caseRetentionReason;
    @XmlElement(name = "Case_PublicityClass")
    @XmlSchemaType(name = "string")
    protected DisclosureLevelType casePublicityClass;
    @XmlElement(name = "Case_SecurityPeriod")
    protected String caseSecurityPeriod;
    @XmlElement(name = "Case_SecurityReason")
    protected String caseSecurityReason;
    @XmlElement(name = "Case_Status")
    protected String caseStatus;
    @XmlElement(name = "Case_Function")
    protected String caseFunction;
    @XmlElement(name = "Case_Language")
    protected String caseLanguage;
    @XmlElement(name = "Case_Finished")
    @XmlSchemaType(name = "date")
    protected XMLGregorianCalendar caseFinished;
    @XmlElement(name = "Case_Actions")
    protected CaseFileType.CaseActions caseActions;

    /**
     * Gets the value of the caseNativeId property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCaseNativeId() {
        return caseNativeId;
    }

    /**
     * Sets the value of the caseNativeId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCaseNativeId(String value) {
        this.caseNativeId = value;
    }

    /**
     * Gets the value of the caseTitle property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCaseTitle() {
        return caseTitle;
    }

    /**
     * Sets the value of the caseTitle property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCaseTitle(String value) {
        this.caseTitle = value;
    }

    /**
     * Gets the value of the caseCreated property.
     * 
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public XMLGregorianCalendar getCaseCreated() {
        return caseCreated;
    }

    /**
     * Sets the value of the caseCreated property.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public void setCaseCreated(XMLGregorianCalendar value) {
        this.caseCreated = value;
    }

    /**
     * Gets the value of the casePersonalData property.
     * 
     * @return
     *     possible object is
     *     {@link PersonalDataType }
     *     
     */
    public PersonalDataType getCasePersonalData() {
        return casePersonalData;
    }

    /**
     * Sets the value of the casePersonalData property.
     * 
     * @param value
     *     allowed object is
     *     {@link PersonalDataType }
     *     
     */
    public void setCasePersonalData(PersonalDataType value) {
        this.casePersonalData = value;
    }

    /**
     * Gets the value of the casePersonalDataCollectionReason property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCasePersonalDataCollectionReason() {
        return casePersonalDataCollectionReason;
    }

    /**
     * Sets the value of the casePersonalDataCollectionReason property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCasePersonalDataCollectionReason(String value) {
        this.casePersonalDataCollectionReason = value;
    }

    /**
     * Gets the value of the caseRetentionPeriod property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCaseRetentionPeriod() {
        return caseRetentionPeriod;
    }

    /**
     * Sets the value of the caseRetentionPeriod property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCaseRetentionPeriod(String value) {
        this.caseRetentionPeriod = value;
    }

    /**
     * Gets the value of the caseRetentionReason property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCaseRetentionReason() {
        return caseRetentionReason;
    }

    /**
     * Sets the value of the caseRetentionReason property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCaseRetentionReason(String value) {
        this.caseRetentionReason = value;
    }

    /**
     * Gets the value of the casePublicityClass property.
     * 
     * @return
     *     possible object is
     *     {@link DisclosureLevelType }
     *     
     */
    public DisclosureLevelType getCasePublicityClass() {
        return casePublicityClass;
    }

    /**
     * Sets the value of the casePublicityClass property.
     * 
     * @param value
     *     allowed object is
     *     {@link DisclosureLevelType }
     *     
     */
    public void setCasePublicityClass(DisclosureLevelType value) {
        this.casePublicityClass = value;
    }

    /**
     * Gets the value of the caseSecurityPeriod property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCaseSecurityPeriod() {
        return caseSecurityPeriod;
    }

    /**
     * Sets the value of the caseSecurityPeriod property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCaseSecurityPeriod(String value) {
        this.caseSecurityPeriod = value;
    }

    /**
     * Gets the value of the caseSecurityReason property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCaseSecurityReason() {
        return caseSecurityReason;
    }

    /**
     * Sets the value of the caseSecurityReason property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCaseSecurityReason(String value) {
        this.caseSecurityReason = value;
    }

    /**
     * Gets the value of the caseStatus property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCaseStatus() {
        return caseStatus;
    }

    /**
     * Sets the value of the caseStatus property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCaseStatus(String value) {
        this.caseStatus = value;
    }

    /**
     * Gets the value of the caseFunction property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCaseFunction() {
        return caseFunction;
    }

    /**
     * Sets the value of the caseFunction property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCaseFunction(String value) {
        this.caseFunction = value;
    }

    /**
     * Gets the value of the caseLanguage property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCaseLanguage() {
        return caseLanguage;
    }

    /**
     * Sets the value of the caseLanguage property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCaseLanguage(String value) {
        this.caseLanguage = value;
    }

    /**
     * Gets the value of the caseFinished property.
     * 
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public XMLGregorianCalendar getCaseFinished() {
        return caseFinished;
    }

    /**
     * Sets the value of the caseFinished property.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public void setCaseFinished(XMLGregorianCalendar value) {
        this.caseFinished = value;
    }

    /**
     * Gets the value of the caseActions property.
     * 
     * @return
     *     possible object is
     *     {@link CaseFileType.CaseActions }
     *     
     */
    public CaseFileType.CaseActions getCaseActions() {
        return caseActions;
    }

    /**
     * Sets the value of the caseActions property.
     * 
     * @param value
     *     allowed object is
     *     {@link CaseFileType.CaseActions }
     *     
     */
    public void setCaseActions(CaseFileType.CaseActions value) {
        this.caseActions = value;
    }


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
     *         &lt;element name="Action" type="{http://www.avaintec.com/2005/x-archive/record-metadata-instance/2.0}actionType" maxOccurs="unbounded" minOccurs="0"/&gt;
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
        "action"
    })
    public static class CaseActions {

        @XmlElement(name = "Action")
        protected List<ActionType> action;

        /**
         * Gets the value of the action property.
         * 
         * <p>
         * This accessor method returns a reference to the live list,
         * not a snapshot. Therefore any modification you make to the
         * returned list will be present inside the JAXB object.
         * This is why there is not a <CODE>set</CODE> method for the action property.
         * 
         * <p>
         * For example, to add a new item, do as follows:
         * <pre>
         *    getAction().add(newItem);
         * </pre>
         * 
         * 
         * <p>
         * Objects of the following type(s) are allowed in the list
         * {@link ActionType }
         * 
         * 
         */
        public List<ActionType> getAction() {
            if (action == null) {
                action = new ArrayList<ActionType>();
            }
            return this.action;
        }

    }

}
