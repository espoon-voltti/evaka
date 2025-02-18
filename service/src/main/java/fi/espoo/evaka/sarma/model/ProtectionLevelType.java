
package fi.espoo.evaka.sarma.model;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlEnumValue;
import jakarta.xml.bind.annotation.XmlType;


/**
 * <p>Java class for ProtectionLevelType.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="ProtectionLevelType"&gt;
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string"&gt;
 *     &lt;enumeration value="Basic"/&gt;
 *     &lt;enumeration value="Enhanced"/&gt;
 *     &lt;enumeration value="High"/&gt;
 *   &lt;/restriction&gt;
 * &lt;/simpleType&gt;
 * </pre>
 * 
 */
@XmlType(name = "ProtectionLevelType", namespace = "http://www.avaintec.com/2004/records-schedule-fi/1.0")
@XmlEnum
public enum ProtectionLevelType {

    @XmlEnumValue("Basic")
    BASIC("Basic"),
    @XmlEnumValue("Enhanced")
    ENHANCED("Enhanced"),
    @XmlEnumValue("High")
    HIGH("High");
    private final String value;

    ProtectionLevelType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static ProtectionLevelType fromValue(String v) {
        for (ProtectionLevelType c: ProtectionLevelType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
