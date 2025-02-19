
package fi.espoo.evaka.sarma.model;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlEnumValue;
import jakarta.xml.bind.annotation.XmlType;


/**
 * <p>Java class for DisclosureLevelType.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="DisclosureLevelType"&gt;
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string"&gt;
 *     &lt;enumeration value="Public"/&gt;
 *     &lt;enumeration value="Internal"/&gt;
 *     &lt;enumeration value="Confidential"/&gt;
 *     &lt;enumeration value="Secret"/&gt;
 *   &lt;/restriction&gt;
 * &lt;/simpleType&gt;
 * </pre>
 * 
 */
@XmlType(name = "DisclosureLevelType", namespace = "http://www.avaintec.com/2004/records-schedule-fi/1.0")
@XmlEnum
public enum DisclosureLevelType {

    @XmlEnumValue("Public")
    PUBLIC("Public"),
    @XmlEnumValue("Internal")
    INTERNAL("Internal"),
    @XmlEnumValue("Confidential")
    CONFIDENTIAL("Confidential"),
    @XmlEnumValue("Secret")
    SECRET("Secret");
    private final String value;

    DisclosureLevelType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static DisclosureLevelType fromValue(String v) {
        for (DisclosureLevelType c: DisclosureLevelType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
