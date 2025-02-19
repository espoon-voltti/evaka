
package fi.espoo.evaka.sarma.model;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlEnumValue;
import jakarta.xml.bind.annotation.XmlType;


/**
 * <p>Java class for relationTypeType.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="relationTypeType"&gt;
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string"&gt;
 *     &lt;enumeration value="isFormatOf"/&gt;
 *     &lt;enumeration value="hasFormat"/&gt;
 *     &lt;enumeration value="isPartOf"/&gt;
 *     &lt;enumeration value="hasPart"/&gt;
 *     &lt;enumeration value="isRedactionOf"/&gt;
 *     &lt;enumeration value="hasRedaction"/&gt;
 *     &lt;enumeration value="isReferencedBy"/&gt;
 *     &lt;enumeration value="references"/&gt;
 *     &lt;enumeration value="isReplacedBy"/&gt;
 *     &lt;enumeration value="replaces"/&gt;
 *     &lt;enumeration value="isRequiredBy"/&gt;
 *     &lt;enumeration value="requires"/&gt;
 *     &lt;enumeration value="isVersionOf"/&gt;
 *     &lt;enumeration value="hasVersion"/&gt;
 *     &lt;enumeration value="conformsTo"/&gt;
 *     &lt;enumeration value="hasAttachment"/&gt;
 *     &lt;enumeration value="isAttachmentOf"/&gt;
 *     &lt;enumeration value="hasSignature"/&gt;
 *     &lt;enumeration value="isSignatureOf"/&gt;
 *   &lt;/restriction&gt;
 * &lt;/simpleType&gt;
 * </pre>
 * 
 */
@XmlType(name = "relationTypeType")
@XmlEnum
public enum RelationTypeType {

    @XmlEnumValue("isFormatOf")
    IS_FORMAT_OF("isFormatOf"),
    @XmlEnumValue("hasFormat")
    HAS_FORMAT("hasFormat"),
    @XmlEnumValue("isPartOf")
    IS_PART_OF("isPartOf"),
    @XmlEnumValue("hasPart")
    HAS_PART("hasPart"),
    @XmlEnumValue("isRedactionOf")
    IS_REDACTION_OF("isRedactionOf"),
    @XmlEnumValue("hasRedaction")
    HAS_REDACTION("hasRedaction"),
    @XmlEnumValue("isReferencedBy")
    IS_REFERENCED_BY("isReferencedBy"),
    @XmlEnumValue("references")
    REFERENCES("references"),
    @XmlEnumValue("isReplacedBy")
    IS_REPLACED_BY("isReplacedBy"),
    @XmlEnumValue("replaces")
    REPLACES("replaces"),
    @XmlEnumValue("isRequiredBy")
    IS_REQUIRED_BY("isRequiredBy"),
    @XmlEnumValue("requires")
    REQUIRES("requires"),
    @XmlEnumValue("isVersionOf")
    IS_VERSION_OF("isVersionOf"),
    @XmlEnumValue("hasVersion")
    HAS_VERSION("hasVersion"),
    @XmlEnumValue("conformsTo")
    CONFORMS_TO("conformsTo"),
    @XmlEnumValue("hasAttachment")
    HAS_ATTACHMENT("hasAttachment"),
    @XmlEnumValue("isAttachmentOf")
    IS_ATTACHMENT_OF("isAttachmentOf"),
    @XmlEnumValue("hasSignature")
    HAS_SIGNATURE("hasSignature"),
    @XmlEnumValue("isSignatureOf")
    IS_SIGNATURE_OF("isSignatureOf");
    private final String value;

    RelationTypeType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static RelationTypeType fromValue(String v) {
        for (RelationTypeType c: RelationTypeType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
