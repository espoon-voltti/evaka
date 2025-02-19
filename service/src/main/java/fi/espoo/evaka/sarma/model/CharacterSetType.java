
package fi.espoo.evaka.sarma.model;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlEnumValue;
import jakarta.xml.bind.annotation.XmlType;


/**
 * <p>Java class for CharacterSetType.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="CharacterSetType"&gt;
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string"&gt;
 *     &lt;enumeration value="UTF-8"/&gt;
 *     &lt;enumeration value="ISO-8859-1"/&gt;
 *   &lt;/restriction&gt;
 * &lt;/simpleType&gt;
 * </pre>
 * 
 */
@XmlType(name = "CharacterSetType")
@XmlEnum
public enum CharacterSetType {

    @XmlEnumValue("UTF-8")
    UTF_8("UTF-8"),
    @XmlEnumValue("ISO-8859-1")
    ISO_8859_1("ISO-8859-1");
    private final String value;

    CharacterSetType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static CharacterSetType fromValue(String v) {
        for (CharacterSetType c: CharacterSetType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
