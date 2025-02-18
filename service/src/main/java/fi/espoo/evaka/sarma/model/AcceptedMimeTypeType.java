
package fi.espoo.evaka.sarma.model;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlEnumValue;
import jakarta.xml.bind.annotation.XmlType;


/**
 * <p>Java class for AcceptedMimeTypeType.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="AcceptedMimeTypeType"&gt;
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string"&gt;
 *     &lt;enumeration value="application/pdf"/&gt;
 *     &lt;enumeration value="text/plain"/&gt;
 *     &lt;enumeration value="text/xml"/&gt;
 *     &lt;enumeration value="application/msword"/&gt;
 *     &lt;enumeration value="application/vnd.openxmlformats-officedocument.wordprocessingml.document"/&gt;
 *     &lt;enumeration value="image/jpeg"/&gt;
 *     &lt;enumeration value="image/png"/&gt;
 *     &lt;enumeration value="image/tiff"/&gt;
 *   &lt;/restriction&gt;
 * &lt;/simpleType&gt;
 * </pre>
 * 
 */
@XmlType(name = "AcceptedMimeTypeType")
@XmlEnum
public enum AcceptedMimeTypeType {

    @XmlEnumValue("application/pdf")
    APPLICATION_PDF("application/pdf"),
    @XmlEnumValue("text/plain")
    TEXT_PLAIN("text/plain"),
    @XmlEnumValue("text/xml")
    TEXT_XML("text/xml"),
    @XmlEnumValue("application/msword")
    APPLICATION_MSWORD("application/msword"),
    @XmlEnumValue("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
    APPLICATION_VND_OPENXMLFORMATS_OFFICEDOCUMENT_WORDPROCESSINGML_DOCUMENT("application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
    @XmlEnumValue("image/jpeg")
    IMAGE_JPEG("image/jpeg"),
    @XmlEnumValue("image/png")
    IMAGE_PNG("image/png"),
    @XmlEnumValue("image/tiff")
    IMAGE_TIFF("image/tiff");
    private final String value;

    AcceptedMimeTypeType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static AcceptedMimeTypeType fromValue(String v) {
        for (AcceptedMimeTypeType c: AcceptedMimeTypeType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
