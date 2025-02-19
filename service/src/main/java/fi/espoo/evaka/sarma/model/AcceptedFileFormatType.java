
package fi.espoo.evaka.sarma.model;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlEnumValue;
import jakarta.xml.bind.annotation.XmlType;


/**
 * <p>Java class for AcceptedFileFormatType.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="AcceptedFileFormatType"&gt;
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string"&gt;
 *     &lt;enumeration value="pdf"/&gt;
 *     &lt;enumeration value="txt"/&gt;
 *     &lt;enumeration value="docx"/&gt;
 *     &lt;enumeration value="doc"/&gt;
 *     &lt;enumeration value="xml"/&gt;
 *     &lt;enumeration value="jpg"/&gt;
 *     &lt;enumeration value="png"/&gt;
 *     &lt;enumeration value="tiff"/&gt;
 *     &lt;enumeration value="cda/level2"/&gt;
 *   &lt;/restriction&gt;
 * &lt;/simpleType&gt;
 * </pre>
 * 
 */
@XmlType(name = "AcceptedFileFormatType")
@XmlEnum
public enum AcceptedFileFormatType {

    @XmlEnumValue("pdf")
    PDF("pdf"),
    @XmlEnumValue("txt")
    TXT("txt"),
    @XmlEnumValue("docx")
    DOCX("docx"),
    @XmlEnumValue("doc")
    DOC("doc"),
    @XmlEnumValue("xml")
    XML("xml"),
    @XmlEnumValue("jpg")
    JPG("jpg"),
    @XmlEnumValue("png")
    PNG("png"),
    @XmlEnumValue("tiff")
    TIFF("tiff"),
    @XmlEnumValue("cda/level2")
    CDA_LEVEL_2("cda/level2");
    private final String value;

    AcceptedFileFormatType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static AcceptedFileFormatType fromValue(String v) {
        for (AcceptedFileFormatType c: AcceptedFileFormatType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
