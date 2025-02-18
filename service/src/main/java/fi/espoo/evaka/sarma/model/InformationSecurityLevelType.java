
package fi.espoo.evaka.sarma.model;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlEnumValue;
import jakarta.xml.bind.annotation.XmlType;


/**
 * <p>Java class for InformationSecurityLevelType.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="InformationSecurityLevelType"&gt;
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string"&gt;
 *     &lt;enumeration value="ST I"/&gt;
 *     &lt;enumeration value="ST II"/&gt;
 *     &lt;enumeration value="ST III"/&gt;
 *     &lt;enumeration value="ST IV"/&gt;
 *     &lt;enumeration value="Unclassified"/&gt;
 *   &lt;/restriction&gt;
 * &lt;/simpleType&gt;
 * </pre>
 * 
 */
@XmlType(name = "InformationSecurityLevelType", namespace = "http://www.avaintec.com/2004/records-schedule-fi/1.0")
@XmlEnum
public enum InformationSecurityLevelType {

    @XmlEnumValue("ST I")
    ST_I("ST I"),
    @XmlEnumValue("ST II")
    ST_II("ST II"),
    @XmlEnumValue("ST III")
    ST_III("ST III"),
    @XmlEnumValue("ST IV")
    ST_IV("ST IV"),
    @XmlEnumValue("Unclassified")
    UNCLASSIFIED("Unclassified");
    private final String value;

    InformationSecurityLevelType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static InformationSecurityLevelType fromValue(String v) {
        for (InformationSecurityLevelType c: InformationSecurityLevelType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
