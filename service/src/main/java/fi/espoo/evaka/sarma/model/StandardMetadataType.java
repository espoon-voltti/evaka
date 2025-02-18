
package fi.espoo.evaka.sarma.model;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlSchemaType;
import jakarta.xml.bind.annotation.XmlType;
import javax.xml.datatype.XMLGregorianCalendar;


/**
 * <p>Java class for StandardMetadataType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="StandardMetadataType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="metadataMasterVersion" type="{http://www.avaintec.com/2005/x-archive/record-metadata-instance/2.0}metadataMasterVersionType"/&gt;
 *         &lt;element name="virtualArchiveId" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *         &lt;element name="recordIdentifiers"&gt;
 *           &lt;complexType&gt;
 *             &lt;complexContent&gt;
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                 &lt;sequence&gt;
 *                   &lt;element name="CaseIdentifier" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *                   &lt;element name="RecordIdentifier" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *                 &lt;/sequence&gt;
 *               &lt;/restriction&gt;
 *             &lt;/complexContent&gt;
 *           &lt;/complexType&gt;
 *         &lt;/element&gt;
 *         &lt;element name="documentDescription"&gt;
 *           &lt;complexType&gt;
 *             &lt;complexContent&gt;
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                 &lt;sequence&gt;
 *                   &lt;element name="title" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *                   &lt;element name="otherId" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *                   &lt;element name="documentType" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *                   &lt;element name="documentTypeSpecifier" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *                   &lt;element name="registerName" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *                   &lt;element name="description" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *                   &lt;element name="firstName" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *                   &lt;element name="lastName" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *                   &lt;element name="socialSecurityNumber" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *                   &lt;element name="keywords" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *                   &lt;element name="personalData" type="{http://www.avaintec.com/2005/x-archive/record-metadata-instance/2.0}personalDataType" minOccurs="0"/&gt;
 *                   &lt;element name="personalDataCollectionReason" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *                   &lt;element name="relatedMaterial" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *                   &lt;element name="transferMethod" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *                   &lt;element name="language" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *                   &lt;element name="dataManagement" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *                   &lt;element name="dataSource" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *                   &lt;element name="owner" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *                   &lt;element name="signed" type="{http://www.w3.org/2001/XMLSchema}date" minOccurs="0"/&gt;
 *                   &lt;element name="signer" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *                   &lt;element name="SignatureDescription" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *                   &lt;element name="accessRight" type="{http://www.avaintec.com/2005/x-archive/record-metadata-instance/2.0}accessRightType" minOccurs="0"/&gt;
 *                   &lt;element name="Agents" minOccurs="0"&gt;
 *                     &lt;complexType&gt;
 *                       &lt;complexContent&gt;
 *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                           &lt;sequence&gt;
 *                             &lt;element name="Agent" type="{http://www.avaintec.com/2005/x-archive/record-metadata-instance/2.0}AgentType" maxOccurs="unbounded" minOccurs="0"/&gt;
 *                           &lt;/sequence&gt;
 *                         &lt;/restriction&gt;
 *                       &lt;/complexContent&gt;
 *                     &lt;/complexType&gt;
 *                   &lt;/element&gt;
 *                 &lt;/sequence&gt;
 *               &lt;/restriction&gt;
 *             &lt;/complexContent&gt;
 *           &lt;/complexType&gt;
 *         &lt;/element&gt;
 *         &lt;element name="format"&gt;
 *           &lt;complexType&gt;
 *             &lt;complexContent&gt;
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                 &lt;sequence&gt;
 *                   &lt;element name="recordType" type="{http://www.avaintec.com/2005/x-archive/record-metadata-instance/2.0}resourceTypeType"/&gt;
 *                   &lt;element name="mimeType" type="{http://www.avaintec.com/2005/x-archive/record-metadata-instance/2.0}AcceptedMimeTypeType" minOccurs="0"/&gt;
 *                   &lt;element name="characterSet" type="{http://www.avaintec.com/2005/x-archive/record-metadata-instance/2.0}CharacterSetType" minOccurs="0"/&gt;
 *                   &lt;element name="fileFormat" type="{http://www.avaintec.com/2005/x-archive/record-metadata-instance/2.0}AcceptedFileFormatType" minOccurs="0"/&gt;
 *                   &lt;element name="formatVersion" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *                   &lt;element name="payloadSize" type="{http://www.w3.org/2001/XMLSchema}integer" minOccurs="0"/&gt;
 *                   &lt;element name="fileName" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *                 &lt;/sequence&gt;
 *               &lt;/restriction&gt;
 *             &lt;/complexContent&gt;
 *           &lt;/complexType&gt;
 *         &lt;/element&gt;
 *         &lt;element name="status" minOccurs="0"&gt;
 *           &lt;complexType&gt;
 *             &lt;complexContent&gt;
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                 &lt;sequence&gt;
 *                   &lt;element name="informationSecurityStatus" type="{http://www.avaintec.com/2004/records-schedule-fi/1.0}InformationSecurityLevelType" minOccurs="0"/&gt;
 *                   &lt;element name="accessStatus" type="{http://www.avaintec.com/2004/records-schedule-fi/1.0}DisclosureLevelType" minOccurs="0"/&gt;
 *                   &lt;element name="protectionStatus" type="{http://www.avaintec.com/2004/records-schedule-fi/1.0}ProtectionLevelType" minOccurs="0"/&gt;
 *                 &lt;/sequence&gt;
 *               &lt;/restriction&gt;
 *             &lt;/complexContent&gt;
 *           &lt;/complexType&gt;
 *         &lt;/element&gt;
 *         &lt;element name="creation" minOccurs="0"&gt;
 *           &lt;complexType&gt;
 *             &lt;complexContent&gt;
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                 &lt;sequence&gt;
 *                   &lt;element name="originatingSystem" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *                   &lt;element name="created" type="{http://www.w3.org/2001/XMLSchema}date" minOccurs="0"/&gt;
 *                   &lt;element name="dateReceived" type="{http://www.w3.org/2001/XMLSchema}date" minOccurs="0"/&gt;
 *                   &lt;element name="lastModified" type="{http://www.w3.org/2001/XMLSchema}dateTime" minOccurs="0"/&gt;
 *                 &lt;/sequence&gt;
 *               &lt;/restriction&gt;
 *             &lt;/complexContent&gt;
 *           &lt;/complexType&gt;
 *         &lt;/element&gt;
 *         &lt;element name="disposition" minOccurs="0"&gt;
 *           &lt;complexType&gt;
 *             &lt;complexContent&gt;
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                 &lt;sequence&gt;
 *                   &lt;element name="retentionPeriodEnd" type="{http://www.w3.org/2001/XMLSchema}date" minOccurs="0"/&gt;
 *                   &lt;element name="disposalListRef" type="{http://www.w3.org/2001/XMLSchema}integer" minOccurs="0"/&gt;
 *                 &lt;/sequence&gt;
 *               &lt;/restriction&gt;
 *             &lt;/complexContent&gt;
 *           &lt;/complexType&gt;
 *         &lt;/element&gt;
 *         &lt;element name="policies" minOccurs="0"&gt;
 *           &lt;complexType&gt;
 *             &lt;complexContent&gt;
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                 &lt;sequence&gt;
 *                   &lt;element name="retentionPolicy" type="{http://www.avaintec.com/2004/records-schedule-fi/1.0}RetentionPolicyType" minOccurs="0"/&gt;
 *                   &lt;element name="disclosurePolicy" type="{http://www.avaintec.com/2004/records-schedule-fi/1.0}DisclosurePolicyType" minOccurs="0"/&gt;
 *                   &lt;element name="informationSecurityPolicy" type="{http://www.avaintec.com/2004/records-schedule-fi/1.0}InformationSecurityPolicyType" minOccurs="0"/&gt;
 *                   &lt;element name="protectionPolicy" type="{http://www.avaintec.com/2004/records-schedule-fi/1.0}ProtectionPolicyType" minOccurs="0"/&gt;
 *                 &lt;/sequence&gt;
 *               &lt;/restriction&gt;
 *             &lt;/complexContent&gt;
 *           &lt;/complexType&gt;
 *         &lt;/element&gt;
 *         &lt;element name="relations" minOccurs="0"&gt;
 *           &lt;complexType&gt;
 *             &lt;complexContent&gt;
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                 &lt;sequence&gt;
 *                   &lt;element name="relation" maxOccurs="unbounded" minOccurs="0"&gt;
 *                     &lt;complexType&gt;
 *                       &lt;complexContent&gt;
 *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                           &lt;sequence&gt;
 *                             &lt;element name="relationType" type="{http://www.avaintec.com/2005/x-archive/record-metadata-instance/2.0}relationTypeType"/&gt;
 *                             &lt;element name="target" type="{http://www.w3.org/2001/XMLSchema}anyURI"/&gt;
 *                           &lt;/sequence&gt;
 *                         &lt;/restriction&gt;
 *                       &lt;/complexContent&gt;
 *                     &lt;/complexType&gt;
 *                   &lt;/element&gt;
 *                 &lt;/sequence&gt;
 *               &lt;/restriction&gt;
 *             &lt;/complexContent&gt;
 *           &lt;/complexType&gt;
 *         &lt;/element&gt;
 *         &lt;element name="history" minOccurs="0"&gt;
 *           &lt;complexType&gt;
 *             &lt;complexContent&gt;
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                 &lt;sequence&gt;
 *                   &lt;element name="event" type="{http://www.avaintec.com/2005/x-archive/record-metadata-instance/2.0}historyEventType" maxOccurs="unbounded" minOccurs="0"/&gt;
 *                 &lt;/sequence&gt;
 *               &lt;/restriction&gt;
 *             &lt;/complexContent&gt;
 *           &lt;/complexType&gt;
 *         &lt;/element&gt;
 *         &lt;element name="CaseFile" type="{http://www.avaintec.com/2005/x-archive/record-metadata-instance/2.0}caseFileType" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "StandardMetadataType", propOrder = {
    "metadataMasterVersion",
    "virtualArchiveId",
    "recordIdentifiers",
    "documentDescription",
    "format",
    "status",
    "creation",
    "disposition",
    "policies",
    "relations",
    "history",
    "caseFile"
})
public class StandardMetadataType {

    @XmlElement(required = true)
    protected MetadataMasterVersionType metadataMasterVersion;
    @XmlElement(required = true)
    protected String virtualArchiveId;
    @XmlElement(required = true)
    protected StandardMetadataType.RecordIdentifiers recordIdentifiers;
    @XmlElement(required = true)
    protected StandardMetadataType.DocumentDescription documentDescription;
    @XmlElement(required = true)
    protected StandardMetadataType.Format format;
    protected StandardMetadataType.Status status;
    protected StandardMetadataType.Creation creation;
    protected StandardMetadataType.Disposition disposition;
    protected StandardMetadataType.Policies policies;
    protected StandardMetadataType.Relations relations;
    protected StandardMetadataType.History history;
    @XmlElement(name = "CaseFile")
    protected CaseFileType caseFile;

    /**
     * Gets the value of the metadataMasterVersion property.
     * 
     * @return
     *     possible object is
     *     {@link MetadataMasterVersionType }
     *     
     */
    public MetadataMasterVersionType getMetadataMasterVersion() {
        return metadataMasterVersion;
    }

    /**
     * Sets the value of the metadataMasterVersion property.
     * 
     * @param value
     *     allowed object is
     *     {@link MetadataMasterVersionType }
     *     
     */
    public void setMetadataMasterVersion(MetadataMasterVersionType value) {
        this.metadataMasterVersion = value;
    }

    /**
     * Gets the value of the virtualArchiveId property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getVirtualArchiveId() {
        return virtualArchiveId;
    }

    /**
     * Sets the value of the virtualArchiveId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setVirtualArchiveId(String value) {
        this.virtualArchiveId = value;
    }

    /**
     * Gets the value of the recordIdentifiers property.
     * 
     * @return
     *     possible object is
     *     {@link StandardMetadataType.RecordIdentifiers }
     *     
     */
    public StandardMetadataType.RecordIdentifiers getRecordIdentifiers() {
        return recordIdentifiers;
    }

    /**
     * Sets the value of the recordIdentifiers property.
     * 
     * @param value
     *     allowed object is
     *     {@link StandardMetadataType.RecordIdentifiers }
     *     
     */
    public void setRecordIdentifiers(StandardMetadataType.RecordIdentifiers value) {
        this.recordIdentifiers = value;
    }

    /**
     * Gets the value of the documentDescription property.
     * 
     * @return
     *     possible object is
     *     {@link StandardMetadataType.DocumentDescription }
     *     
     */
    public StandardMetadataType.DocumentDescription getDocumentDescription() {
        return documentDescription;
    }

    /**
     * Sets the value of the documentDescription property.
     * 
     * @param value
     *     allowed object is
     *     {@link StandardMetadataType.DocumentDescription }
     *     
     */
    public void setDocumentDescription(StandardMetadataType.DocumentDescription value) {
        this.documentDescription = value;
    }

    /**
     * Gets the value of the format property.
     * 
     * @return
     *     possible object is
     *     {@link StandardMetadataType.Format }
     *     
     */
    public StandardMetadataType.Format getFormat() {
        return format;
    }

    /**
     * Sets the value of the format property.
     * 
     * @param value
     *     allowed object is
     *     {@link StandardMetadataType.Format }
     *     
     */
    public void setFormat(StandardMetadataType.Format value) {
        this.format = value;
    }

    /**
     * Gets the value of the status property.
     * 
     * @return
     *     possible object is
     *     {@link StandardMetadataType.Status }
     *     
     */
    public StandardMetadataType.Status getStatus() {
        return status;
    }

    /**
     * Sets the value of the status property.
     * 
     * @param value
     *     allowed object is
     *     {@link StandardMetadataType.Status }
     *     
     */
    public void setStatus(StandardMetadataType.Status value) {
        this.status = value;
    }

    /**
     * Gets the value of the creation property.
     * 
     * @return
     *     possible object is
     *     {@link StandardMetadataType.Creation }
     *     
     */
    public StandardMetadataType.Creation getCreation() {
        return creation;
    }

    /**
     * Sets the value of the creation property.
     * 
     * @param value
     *     allowed object is
     *     {@link StandardMetadataType.Creation }
     *     
     */
    public void setCreation(StandardMetadataType.Creation value) {
        this.creation = value;
    }

    /**
     * Gets the value of the disposition property.
     * 
     * @return
     *     possible object is
     *     {@link StandardMetadataType.Disposition }
     *     
     */
    public StandardMetadataType.Disposition getDisposition() {
        return disposition;
    }

    /**
     * Sets the value of the disposition property.
     * 
     * @param value
     *     allowed object is
     *     {@link StandardMetadataType.Disposition }
     *     
     */
    public void setDisposition(StandardMetadataType.Disposition value) {
        this.disposition = value;
    }

    /**
     * Gets the value of the policies property.
     * 
     * @return
     *     possible object is
     *     {@link StandardMetadataType.Policies }
     *     
     */
    public StandardMetadataType.Policies getPolicies() {
        return policies;
    }

    /**
     * Sets the value of the policies property.
     * 
     * @param value
     *     allowed object is
     *     {@link StandardMetadataType.Policies }
     *     
     */
    public void setPolicies(StandardMetadataType.Policies value) {
        this.policies = value;
    }

    /**
     * Gets the value of the relations property.
     * 
     * @return
     *     possible object is
     *     {@link StandardMetadataType.Relations }
     *     
     */
    public StandardMetadataType.Relations getRelations() {
        return relations;
    }

    /**
     * Sets the value of the relations property.
     * 
     * @param value
     *     allowed object is
     *     {@link StandardMetadataType.Relations }
     *     
     */
    public void setRelations(StandardMetadataType.Relations value) {
        this.relations = value;
    }

    /**
     * Gets the value of the history property.
     * 
     * @return
     *     possible object is
     *     {@link StandardMetadataType.History }
     *     
     */
    public StandardMetadataType.History getHistory() {
        return history;
    }

    /**
     * Sets the value of the history property.
     * 
     * @param value
     *     allowed object is
     *     {@link StandardMetadataType.History }
     *     
     */
    public void setHistory(StandardMetadataType.History value) {
        this.history = value;
    }

    /**
     * Gets the value of the caseFile property.
     * 
     * @return
     *     possible object is
     *     {@link CaseFileType }
     *     
     */
    public CaseFileType getCaseFile() {
        return caseFile;
    }

    /**
     * Sets the value of the caseFile property.
     * 
     * @param value
     *     allowed object is
     *     {@link CaseFileType }
     *     
     */
    public void setCaseFile(CaseFileType value) {
        this.caseFile = value;
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
     *         &lt;element name="originatingSystem" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
     *         &lt;element name="created" type="{http://www.w3.org/2001/XMLSchema}date" minOccurs="0"/&gt;
     *         &lt;element name="dateReceived" type="{http://www.w3.org/2001/XMLSchema}date" minOccurs="0"/&gt;
     *         &lt;element name="lastModified" type="{http://www.w3.org/2001/XMLSchema}dateTime" minOccurs="0"/&gt;
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
        "originatingSystem",
        "created",
        "dateReceived",
        "lastModified"
    })
    public static class Creation {

        protected String originatingSystem;
        @XmlSchemaType(name = "date")
        protected XMLGregorianCalendar created;
        @XmlSchemaType(name = "date")
        protected XMLGregorianCalendar dateReceived;
        @XmlSchemaType(name = "dateTime")
        protected XMLGregorianCalendar lastModified;

        /**
         * Gets the value of the originatingSystem property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getOriginatingSystem() {
            return originatingSystem;
        }

        /**
         * Sets the value of the originatingSystem property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setOriginatingSystem(String value) {
            this.originatingSystem = value;
        }

        /**
         * Gets the value of the created property.
         * 
         * @return
         *     possible object is
         *     {@link XMLGregorianCalendar }
         *     
         */
        public XMLGregorianCalendar getCreated() {
            return created;
        }

        /**
         * Sets the value of the created property.
         * 
         * @param value
         *     allowed object is
         *     {@link XMLGregorianCalendar }
         *     
         */
        public void setCreated(XMLGregorianCalendar value) {
            this.created = value;
        }

        /**
         * Gets the value of the dateReceived property.
         * 
         * @return
         *     possible object is
         *     {@link XMLGregorianCalendar }
         *     
         */
        public XMLGregorianCalendar getDateReceived() {
            return dateReceived;
        }

        /**
         * Sets the value of the dateReceived property.
         * 
         * @param value
         *     allowed object is
         *     {@link XMLGregorianCalendar }
         *     
         */
        public void setDateReceived(XMLGregorianCalendar value) {
            this.dateReceived = value;
        }

        /**
         * Gets the value of the lastModified property.
         * 
         * @return
         *     possible object is
         *     {@link XMLGregorianCalendar }
         *     
         */
        public XMLGregorianCalendar getLastModified() {
            return lastModified;
        }

        /**
         * Sets the value of the lastModified property.
         * 
         * @param value
         *     allowed object is
         *     {@link XMLGregorianCalendar }
         *     
         */
        public void setLastModified(XMLGregorianCalendar value) {
            this.lastModified = value;
        }

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
     *         &lt;element name="retentionPeriodEnd" type="{http://www.w3.org/2001/XMLSchema}date" minOccurs="0"/&gt;
     *         &lt;element name="disposalListRef" type="{http://www.w3.org/2001/XMLSchema}integer" minOccurs="0"/&gt;
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
        "retentionPeriodEnd",
        "disposalListRef"
    })
    public static class Disposition {

        @XmlSchemaType(name = "date")
        protected XMLGregorianCalendar retentionPeriodEnd;
        protected BigInteger disposalListRef;

        /**
         * Gets the value of the retentionPeriodEnd property.
         * 
         * @return
         *     possible object is
         *     {@link XMLGregorianCalendar }
         *     
         */
        public XMLGregorianCalendar getRetentionPeriodEnd() {
            return retentionPeriodEnd;
        }

        /**
         * Sets the value of the retentionPeriodEnd property.
         * 
         * @param value
         *     allowed object is
         *     {@link XMLGregorianCalendar }
         *     
         */
        public void setRetentionPeriodEnd(XMLGregorianCalendar value) {
            this.retentionPeriodEnd = value;
        }

        /**
         * Gets the value of the disposalListRef property.
         * 
         * @return
         *     possible object is
         *     {@link BigInteger }
         *     
         */
        public BigInteger getDisposalListRef() {
            return disposalListRef;
        }

        /**
         * Sets the value of the disposalListRef property.
         * 
         * @param value
         *     allowed object is
         *     {@link BigInteger }
         *     
         */
        public void setDisposalListRef(BigInteger value) {
            this.disposalListRef = value;
        }

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
     *         &lt;element name="title" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
     *         &lt;element name="otherId" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
     *         &lt;element name="documentType" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
     *         &lt;element name="documentTypeSpecifier" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
     *         &lt;element name="registerName" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
     *         &lt;element name="description" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
     *         &lt;element name="firstName" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
     *         &lt;element name="lastName" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
     *         &lt;element name="socialSecurityNumber" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
     *         &lt;element name="keywords" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
     *         &lt;element name="personalData" type="{http://www.avaintec.com/2005/x-archive/record-metadata-instance/2.0}personalDataType" minOccurs="0"/&gt;
     *         &lt;element name="personalDataCollectionReason" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
     *         &lt;element name="relatedMaterial" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
     *         &lt;element name="transferMethod" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
     *         &lt;element name="language" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
     *         &lt;element name="dataManagement" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
     *         &lt;element name="dataSource" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
     *         &lt;element name="owner" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
     *         &lt;element name="signed" type="{http://www.w3.org/2001/XMLSchema}date" minOccurs="0"/&gt;
     *         &lt;element name="signer" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
     *         &lt;element name="SignatureDescription" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
     *         &lt;element name="accessRight" type="{http://www.avaintec.com/2005/x-archive/record-metadata-instance/2.0}accessRightType" minOccurs="0"/&gt;
     *         &lt;element name="Agents" minOccurs="0"&gt;
     *           &lt;complexType&gt;
     *             &lt;complexContent&gt;
     *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
     *                 &lt;sequence&gt;
     *                   &lt;element name="Agent" type="{http://www.avaintec.com/2005/x-archive/record-metadata-instance/2.0}AgentType" maxOccurs="unbounded" minOccurs="0"/&gt;
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
    @XmlType(name = "", propOrder = {
        "title",
        "otherId",
        "documentType",
        "documentTypeSpecifier",
        "registerName",
        "description",
        "firstName",
        "lastName",
        "socialSecurityNumber",
        "keywords",
        "personalData",
        "personalDataCollectionReason",
        "relatedMaterial",
        "transferMethod",
        "language",
        "dataManagement",
        "dataSource",
        "owner",
        "signed",
        "signer",
        "signatureDescription",
        "accessRight",
        "agents"
    })
    public static class DocumentDescription {

        @XmlElement(required = true)
        protected String title;
        protected String otherId;
        protected String documentType;
        protected String documentTypeSpecifier;
        protected String registerName;
        protected String description;
        protected String firstName;
        protected String lastName;
        protected String socialSecurityNumber;
        protected String keywords;
        @XmlSchemaType(name = "string")
        protected PersonalDataType personalData;
        protected String personalDataCollectionReason;
        protected String relatedMaterial;
        protected String transferMethod;
        protected String language;
        protected String dataManagement;
        protected String dataSource;
        protected String owner;
        @XmlSchemaType(name = "date")
        protected XMLGregorianCalendar signed;
        protected String signer;
        @XmlElement(name = "SignatureDescription")
        protected String signatureDescription;
        protected AccessRightType accessRight;
        @XmlElement(name = "Agents")
        protected StandardMetadataType.DocumentDescription.Agents agents;

        /**
         * Gets the value of the title property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getTitle() {
            return title;
        }

        /**
         * Sets the value of the title property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setTitle(String value) {
            this.title = value;
        }

        /**
         * Gets the value of the otherId property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getOtherId() {
            return otherId;
        }

        /**
         * Sets the value of the otherId property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setOtherId(String value) {
            this.otherId = value;
        }

        /**
         * Gets the value of the documentType property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getDocumentType() {
            return documentType;
        }

        /**
         * Sets the value of the documentType property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setDocumentType(String value) {
            this.documentType = value;
        }

        /**
         * Gets the value of the documentTypeSpecifier property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getDocumentTypeSpecifier() {
            return documentTypeSpecifier;
        }

        /**
         * Sets the value of the documentTypeSpecifier property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setDocumentTypeSpecifier(String value) {
            this.documentTypeSpecifier = value;
        }

        /**
         * Gets the value of the registerName property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getRegisterName() {
            return registerName;
        }

        /**
         * Sets the value of the registerName property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setRegisterName(String value) {
            this.registerName = value;
        }

        /**
         * Gets the value of the description property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getDescription() {
            return description;
        }

        /**
         * Sets the value of the description property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setDescription(String value) {
            this.description = value;
        }

        /**
         * Gets the value of the firstName property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getFirstName() {
            return firstName;
        }

        /**
         * Sets the value of the firstName property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setFirstName(String value) {
            this.firstName = value;
        }

        /**
         * Gets the value of the lastName property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getLastName() {
            return lastName;
        }

        /**
         * Sets the value of the lastName property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setLastName(String value) {
            this.lastName = value;
        }

        /**
         * Gets the value of the socialSecurityNumber property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getSocialSecurityNumber() {
            return socialSecurityNumber;
        }

        /**
         * Sets the value of the socialSecurityNumber property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setSocialSecurityNumber(String value) {
            this.socialSecurityNumber = value;
        }

        /**
         * Gets the value of the keywords property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getKeywords() {
            return keywords;
        }

        /**
         * Sets the value of the keywords property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setKeywords(String value) {
            this.keywords = value;
        }

        /**
         * Gets the value of the personalData property.
         * 
         * @return
         *     possible object is
         *     {@link PersonalDataType }
         *     
         */
        public PersonalDataType getPersonalData() {
            return personalData;
        }

        /**
         * Sets the value of the personalData property.
         * 
         * @param value
         *     allowed object is
         *     {@link PersonalDataType }
         *     
         */
        public void setPersonalData(PersonalDataType value) {
            this.personalData = value;
        }

        /**
         * Gets the value of the personalDataCollectionReason property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getPersonalDataCollectionReason() {
            return personalDataCollectionReason;
        }

        /**
         * Sets the value of the personalDataCollectionReason property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setPersonalDataCollectionReason(String value) {
            this.personalDataCollectionReason = value;
        }

        /**
         * Gets the value of the relatedMaterial property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getRelatedMaterial() {
            return relatedMaterial;
        }

        /**
         * Sets the value of the relatedMaterial property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setRelatedMaterial(String value) {
            this.relatedMaterial = value;
        }

        /**
         * Gets the value of the transferMethod property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getTransferMethod() {
            return transferMethod;
        }

        /**
         * Sets the value of the transferMethod property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setTransferMethod(String value) {
            this.transferMethod = value;
        }

        /**
         * Gets the value of the language property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getLanguage() {
            return language;
        }

        /**
         * Sets the value of the language property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setLanguage(String value) {
            this.language = value;
        }

        /**
         * Gets the value of the dataManagement property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getDataManagement() {
            return dataManagement;
        }

        /**
         * Sets the value of the dataManagement property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setDataManagement(String value) {
            this.dataManagement = value;
        }

        /**
         * Gets the value of the dataSource property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getDataSource() {
            return dataSource;
        }

        /**
         * Sets the value of the dataSource property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setDataSource(String value) {
            this.dataSource = value;
        }

        /**
         * Gets the value of the owner property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getOwner() {
            return owner;
        }

        /**
         * Sets the value of the owner property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setOwner(String value) {
            this.owner = value;
        }

        /**
         * Gets the value of the signed property.
         * 
         * @return
         *     possible object is
         *     {@link XMLGregorianCalendar }
         *     
         */
        public XMLGregorianCalendar getSigned() {
            return signed;
        }

        /**
         * Sets the value of the signed property.
         * 
         * @param value
         *     allowed object is
         *     {@link XMLGregorianCalendar }
         *     
         */
        public void setSigned(XMLGregorianCalendar value) {
            this.signed = value;
        }

        /**
         * Gets the value of the signer property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getSigner() {
            return signer;
        }

        /**
         * Sets the value of the signer property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setSigner(String value) {
            this.signer = value;
        }

        /**
         * Gets the value of the signatureDescription property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getSignatureDescription() {
            return signatureDescription;
        }

        /**
         * Sets the value of the signatureDescription property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setSignatureDescription(String value) {
            this.signatureDescription = value;
        }

        /**
         * Gets the value of the accessRight property.
         * 
         * @return
         *     possible object is
         *     {@link AccessRightType }
         *     
         */
        public AccessRightType getAccessRight() {
            return accessRight;
        }

        /**
         * Sets the value of the accessRight property.
         * 
         * @param value
         *     allowed object is
         *     {@link AccessRightType }
         *     
         */
        public void setAccessRight(AccessRightType value) {
            this.accessRight = value;
        }

        /**
         * Gets the value of the agents property.
         * 
         * @return
         *     possible object is
         *     {@link StandardMetadataType.DocumentDescription.Agents }
         *     
         */
        public StandardMetadataType.DocumentDescription.Agents getAgents() {
            return agents;
        }

        /**
         * Sets the value of the agents property.
         * 
         * @param value
         *     allowed object is
         *     {@link StandardMetadataType.DocumentDescription.Agents }
         *     
         */
        public void setAgents(StandardMetadataType.DocumentDescription.Agents value) {
            this.agents = value;
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
         *         &lt;element name="Agent" type="{http://www.avaintec.com/2005/x-archive/record-metadata-instance/2.0}AgentType" maxOccurs="unbounded" minOccurs="0"/&gt;
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
            "agent"
        })
        public static class Agents {

            @XmlElement(name = "Agent")
            protected List<AgentType> agent;

            /**
             * Gets the value of the agent property.
             * 
             * <p>
             * This accessor method returns a reference to the live list,
             * not a snapshot. Therefore any modification you make to the
             * returned list will be present inside the JAXB object.
             * This is why there is not a <CODE>set</CODE> method for the agent property.
             * 
             * <p>
             * For example, to add a new item, do as follows:
             * <pre>
             *    getAgent().add(newItem);
             * </pre>
             * 
             * 
             * <p>
             * Objects of the following type(s) are allowed in the list
             * {@link AgentType }
             * 
             * 
             */
            public List<AgentType> getAgent() {
                if (agent == null) {
                    agent = new ArrayList<AgentType>();
                }
                return this.agent;
            }

        }

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
     *         &lt;element name="recordType" type="{http://www.avaintec.com/2005/x-archive/record-metadata-instance/2.0}resourceTypeType"/&gt;
     *         &lt;element name="mimeType" type="{http://www.avaintec.com/2005/x-archive/record-metadata-instance/2.0}AcceptedMimeTypeType" minOccurs="0"/&gt;
     *         &lt;element name="characterSet" type="{http://www.avaintec.com/2005/x-archive/record-metadata-instance/2.0}CharacterSetType" minOccurs="0"/&gt;
     *         &lt;element name="fileFormat" type="{http://www.avaintec.com/2005/x-archive/record-metadata-instance/2.0}AcceptedFileFormatType" minOccurs="0"/&gt;
     *         &lt;element name="formatVersion" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
     *         &lt;element name="payloadSize" type="{http://www.w3.org/2001/XMLSchema}integer" minOccurs="0"/&gt;
     *         &lt;element name="fileName" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
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
        "recordType",
        "mimeType",
        "characterSet",
        "fileFormat",
        "formatVersion",
        "payloadSize",
        "fileName"
    })
    public static class Format {

        @XmlElement(required = true)
        @XmlSchemaType(name = "string")
        protected ResourceTypeType recordType;
        @XmlSchemaType(name = "string")
        protected AcceptedMimeTypeType mimeType;
        @XmlSchemaType(name = "string")
        protected CharacterSetType characterSet;
        @XmlSchemaType(name = "string")
        protected AcceptedFileFormatType fileFormat;
        protected String formatVersion;
        protected BigInteger payloadSize;
        protected String fileName;

        /**
         * Gets the value of the recordType property.
         * 
         * @return
         *     possible object is
         *     {@link ResourceTypeType }
         *     
         */
        public ResourceTypeType getRecordType() {
            return recordType;
        }

        /**
         * Sets the value of the recordType property.
         * 
         * @param value
         *     allowed object is
         *     {@link ResourceTypeType }
         *     
         */
        public void setRecordType(ResourceTypeType value) {
            this.recordType = value;
        }

        /**
         * Gets the value of the mimeType property.
         * 
         * @return
         *     possible object is
         *     {@link AcceptedMimeTypeType }
         *     
         */
        public AcceptedMimeTypeType getMimeType() {
            return mimeType;
        }

        /**
         * Sets the value of the mimeType property.
         * 
         * @param value
         *     allowed object is
         *     {@link AcceptedMimeTypeType }
         *     
         */
        public void setMimeType(AcceptedMimeTypeType value) {
            this.mimeType = value;
        }

        /**
         * Gets the value of the characterSet property.
         * 
         * @return
         *     possible object is
         *     {@link CharacterSetType }
         *     
         */
        public CharacterSetType getCharacterSet() {
            return characterSet;
        }

        /**
         * Sets the value of the characterSet property.
         * 
         * @param value
         *     allowed object is
         *     {@link CharacterSetType }
         *     
         */
        public void setCharacterSet(CharacterSetType value) {
            this.characterSet = value;
        }

        /**
         * Gets the value of the fileFormat property.
         * 
         * @return
         *     possible object is
         *     {@link AcceptedFileFormatType }
         *     
         */
        public AcceptedFileFormatType getFileFormat() {
            return fileFormat;
        }

        /**
         * Sets the value of the fileFormat property.
         * 
         * @param value
         *     allowed object is
         *     {@link AcceptedFileFormatType }
         *     
         */
        public void setFileFormat(AcceptedFileFormatType value) {
            this.fileFormat = value;
        }

        /**
         * Gets the value of the formatVersion property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getFormatVersion() {
            return formatVersion;
        }

        /**
         * Sets the value of the formatVersion property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setFormatVersion(String value) {
            this.formatVersion = value;
        }

        /**
         * Gets the value of the payloadSize property.
         * 
         * @return
         *     possible object is
         *     {@link BigInteger }
         *     
         */
        public BigInteger getPayloadSize() {
            return payloadSize;
        }

        /**
         * Sets the value of the payloadSize property.
         * 
         * @param value
         *     allowed object is
         *     {@link BigInteger }
         *     
         */
        public void setPayloadSize(BigInteger value) {
            this.payloadSize = value;
        }

        /**
         * Gets the value of the fileName property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getFileName() {
            return fileName;
        }

        /**
         * Sets the value of the fileName property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setFileName(String value) {
            this.fileName = value;
        }

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
     *         &lt;element name="event" type="{http://www.avaintec.com/2005/x-archive/record-metadata-instance/2.0}historyEventType" maxOccurs="unbounded" minOccurs="0"/&gt;
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
        "event"
    })
    public static class History {

        protected List<HistoryEventType> event;

        /**
         * Gets the value of the event property.
         * 
         * <p>
         * This accessor method returns a reference to the live list,
         * not a snapshot. Therefore any modification you make to the
         * returned list will be present inside the JAXB object.
         * This is why there is not a <CODE>set</CODE> method for the event property.
         * 
         * <p>
         * For example, to add a new item, do as follows:
         * <pre>
         *    getEvent().add(newItem);
         * </pre>
         * 
         * 
         * <p>
         * Objects of the following type(s) are allowed in the list
         * {@link HistoryEventType }
         * 
         * 
         */
        public List<HistoryEventType> getEvent() {
            if (event == null) {
                event = new ArrayList<HistoryEventType>();
            }
            return this.event;
        }

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
     *         &lt;element name="retentionPolicy" type="{http://www.avaintec.com/2004/records-schedule-fi/1.0}RetentionPolicyType" minOccurs="0"/&gt;
     *         &lt;element name="disclosurePolicy" type="{http://www.avaintec.com/2004/records-schedule-fi/1.0}DisclosurePolicyType" minOccurs="0"/&gt;
     *         &lt;element name="informationSecurityPolicy" type="{http://www.avaintec.com/2004/records-schedule-fi/1.0}InformationSecurityPolicyType" minOccurs="0"/&gt;
     *         &lt;element name="protectionPolicy" type="{http://www.avaintec.com/2004/records-schedule-fi/1.0}ProtectionPolicyType" minOccurs="0"/&gt;
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
        "retentionPolicy",
        "disclosurePolicy",
        "informationSecurityPolicy",
        "protectionPolicy"
    })
    public static class Policies {

        protected RetentionPolicyType retentionPolicy;
        protected DisclosurePolicyType disclosurePolicy;
        protected InformationSecurityPolicyType informationSecurityPolicy;
        protected ProtectionPolicyType protectionPolicy;

        /**
         * Gets the value of the retentionPolicy property.
         * 
         * @return
         *     possible object is
         *     {@link RetentionPolicyType }
         *     
         */
        public RetentionPolicyType getRetentionPolicy() {
            return retentionPolicy;
        }

        /**
         * Sets the value of the retentionPolicy property.
         * 
         * @param value
         *     allowed object is
         *     {@link RetentionPolicyType }
         *     
         */
        public void setRetentionPolicy(RetentionPolicyType value) {
            this.retentionPolicy = value;
        }

        /**
         * Gets the value of the disclosurePolicy property.
         * 
         * @return
         *     possible object is
         *     {@link DisclosurePolicyType }
         *     
         */
        public DisclosurePolicyType getDisclosurePolicy() {
            return disclosurePolicy;
        }

        /**
         * Sets the value of the disclosurePolicy property.
         * 
         * @param value
         *     allowed object is
         *     {@link DisclosurePolicyType }
         *     
         */
        public void setDisclosurePolicy(DisclosurePolicyType value) {
            this.disclosurePolicy = value;
        }

        /**
         * Gets the value of the informationSecurityPolicy property.
         * 
         * @return
         *     possible object is
         *     {@link InformationSecurityPolicyType }
         *     
         */
        public InformationSecurityPolicyType getInformationSecurityPolicy() {
            return informationSecurityPolicy;
        }

        /**
         * Sets the value of the informationSecurityPolicy property.
         * 
         * @param value
         *     allowed object is
         *     {@link InformationSecurityPolicyType }
         *     
         */
        public void setInformationSecurityPolicy(InformationSecurityPolicyType value) {
            this.informationSecurityPolicy = value;
        }

        /**
         * Gets the value of the protectionPolicy property.
         * 
         * @return
         *     possible object is
         *     {@link ProtectionPolicyType }
         *     
         */
        public ProtectionPolicyType getProtectionPolicy() {
            return protectionPolicy;
        }

        /**
         * Sets the value of the protectionPolicy property.
         * 
         * @param value
         *     allowed object is
         *     {@link ProtectionPolicyType }
         *     
         */
        public void setProtectionPolicy(ProtectionPolicyType value) {
            this.protectionPolicy = value;
        }

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
     *         &lt;element name="CaseIdentifier" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
     *         &lt;element name="RecordIdentifier" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
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
        "caseIdentifier",
        "recordIdentifier"
    })
    public static class RecordIdentifiers {

        @XmlElement(name = "CaseIdentifier")
        protected String caseIdentifier;
        @XmlElement(name = "RecordIdentifier", required = true)
        protected String recordIdentifier;

        /**
         * Gets the value of the caseIdentifier property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getCaseIdentifier() {
            return caseIdentifier;
        }

        /**
         * Sets the value of the caseIdentifier property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setCaseIdentifier(String value) {
            this.caseIdentifier = value;
        }

        /**
         * Gets the value of the recordIdentifier property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getRecordIdentifier() {
            return recordIdentifier;
        }

        /**
         * Sets the value of the recordIdentifier property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setRecordIdentifier(String value) {
            this.recordIdentifier = value;
        }

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
     *         &lt;element name="relation" maxOccurs="unbounded" minOccurs="0"&gt;
     *           &lt;complexType&gt;
     *             &lt;complexContent&gt;
     *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
     *                 &lt;sequence&gt;
     *                   &lt;element name="relationType" type="{http://www.avaintec.com/2005/x-archive/record-metadata-instance/2.0}relationTypeType"/&gt;
     *                   &lt;element name="target" type="{http://www.w3.org/2001/XMLSchema}anyURI"/&gt;
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
    @XmlType(name = "", propOrder = {
        "relation"
    })
    public static class Relations {

        protected List<StandardMetadataType.Relations.Relation> relation;

        /**
         * Gets the value of the relation property.
         * 
         * <p>
         * This accessor method returns a reference to the live list,
         * not a snapshot. Therefore any modification you make to the
         * returned list will be present inside the JAXB object.
         * This is why there is not a <CODE>set</CODE> method for the relation property.
         * 
         * <p>
         * For example, to add a new item, do as follows:
         * <pre>
         *    getRelation().add(newItem);
         * </pre>
         * 
         * 
         * <p>
         * Objects of the following type(s) are allowed in the list
         * {@link StandardMetadataType.Relations.Relation }
         * 
         * 
         */
        public List<StandardMetadataType.Relations.Relation> getRelation() {
            if (relation == null) {
                relation = new ArrayList<StandardMetadataType.Relations.Relation>();
            }
            return this.relation;
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
         *         &lt;element name="relationType" type="{http://www.avaintec.com/2005/x-archive/record-metadata-instance/2.0}relationTypeType"/&gt;
         *         &lt;element name="target" type="{http://www.w3.org/2001/XMLSchema}anyURI"/&gt;
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
            "relationType",
            "target"
        })
        public static class Relation {

            @XmlElement(required = true)
            @XmlSchemaType(name = "string")
            protected RelationTypeType relationType;
            @XmlElement(required = true)
            @XmlSchemaType(name = "anyURI")
            protected String target;

            /**
             * Gets the value of the relationType property.
             * 
             * @return
             *     possible object is
             *     {@link RelationTypeType }
             *     
             */
            public RelationTypeType getRelationType() {
                return relationType;
            }

            /**
             * Sets the value of the relationType property.
             * 
             * @param value
             *     allowed object is
             *     {@link RelationTypeType }
             *     
             */
            public void setRelationType(RelationTypeType value) {
                this.relationType = value;
            }

            /**
             * Gets the value of the target property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            public String getTarget() {
                return target;
            }

            /**
             * Sets the value of the target property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            public void setTarget(String value) {
                this.target = value;
            }

        }

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
     *         &lt;element name="informationSecurityStatus" type="{http://www.avaintec.com/2004/records-schedule-fi/1.0}InformationSecurityLevelType" minOccurs="0"/&gt;
     *         &lt;element name="accessStatus" type="{http://www.avaintec.com/2004/records-schedule-fi/1.0}DisclosureLevelType" minOccurs="0"/&gt;
     *         &lt;element name="protectionStatus" type="{http://www.avaintec.com/2004/records-schedule-fi/1.0}ProtectionLevelType" minOccurs="0"/&gt;
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
        "informationSecurityStatus",
        "accessStatus",
        "protectionStatus"
    })
    public static class Status {

        @XmlSchemaType(name = "string")
        protected InformationSecurityLevelType informationSecurityStatus;
        @XmlSchemaType(name = "string")
        protected DisclosureLevelType accessStatus;
        @XmlSchemaType(name = "string")
        protected ProtectionLevelType protectionStatus;

        /**
         * Gets the value of the informationSecurityStatus property.
         * 
         * @return
         *     possible object is
         *     {@link InformationSecurityLevelType }
         *     
         */
        public InformationSecurityLevelType getInformationSecurityStatus() {
            return informationSecurityStatus;
        }

        /**
         * Sets the value of the informationSecurityStatus property.
         * 
         * @param value
         *     allowed object is
         *     {@link InformationSecurityLevelType }
         *     
         */
        public void setInformationSecurityStatus(InformationSecurityLevelType value) {
            this.informationSecurityStatus = value;
        }

        /**
         * Gets the value of the accessStatus property.
         * 
         * @return
         *     possible object is
         *     {@link DisclosureLevelType }
         *     
         */
        public DisclosureLevelType getAccessStatus() {
            return accessStatus;
        }

        /**
         * Sets the value of the accessStatus property.
         * 
         * @param value
         *     allowed object is
         *     {@link DisclosureLevelType }
         *     
         */
        public void setAccessStatus(DisclosureLevelType value) {
            this.accessStatus = value;
        }

        /**
         * Gets the value of the protectionStatus property.
         * 
         * @return
         *     possible object is
         *     {@link ProtectionLevelType }
         *     
         */
        public ProtectionLevelType getProtectionStatus() {
            return protectionStatus;
        }

        /**
         * Sets the value of the protectionStatus property.
         * 
         * @param value
         *     allowed object is
         *     {@link ProtectionLevelType }
         *     
         */
        public void setProtectionStatus(ProtectionLevelType value) {
            this.protectionStatus = value;
        }

    }

}
