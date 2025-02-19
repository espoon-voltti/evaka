
package fi.espoo.evaka.sarma.model;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlEnumValue;
import jakarta.xml.bind.annotation.XmlType;


/**
 * <p>Java class for resourceTypeType.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="resourceTypeType"&gt;
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string"&gt;
 *     &lt;enumeration value="digital"/&gt;
 *     &lt;enumeration value="physical"/&gt;
 *   &lt;/restriction&gt;
 * &lt;/simpleType&gt;
 * </pre>
 * 
 */
@XmlType(name = "resourceTypeType")
@XmlEnum
public enum ResourceTypeType {

    @XmlEnumValue("digital")
    DIGITAL("digital"),
    @XmlEnumValue("physical")
    PHYSICAL("physical");
    private final String value;

    ResourceTypeType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static ResourceTypeType fromValue(String v) {
        for (ResourceTypeType c: ResourceTypeType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
