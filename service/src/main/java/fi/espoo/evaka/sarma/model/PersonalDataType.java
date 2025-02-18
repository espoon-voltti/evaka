
package fi.espoo.evaka.sarma.model;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlEnumValue;
import jakarta.xml.bind.annotation.XmlType;


/**
 * <p>Java class for personalDataType.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="personalDataType"&gt;
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string"&gt;
 *     &lt;enumeration value="noPersonalInformation"/&gt;
 *     &lt;enumeration value="containsPersonalInformation"/&gt;
 *     &lt;enumeration value="containsSensitivePersonalInformation"/&gt;
 *     &lt;enumeration value="containsInformationOnCriminalConvictionsAndOffenses"/&gt;
 *   &lt;/restriction&gt;
 * &lt;/simpleType&gt;
 * </pre>
 * 
 */
@XmlType(name = "personalDataType")
@XmlEnum
public enum PersonalDataType {

    @XmlEnumValue("noPersonalInformation")
    NO_PERSONAL_INFORMATION("noPersonalInformation"),
    @XmlEnumValue("containsPersonalInformation")
    CONTAINS_PERSONAL_INFORMATION("containsPersonalInformation"),
    @XmlEnumValue("containsSensitivePersonalInformation")
    CONTAINS_SENSITIVE_PERSONAL_INFORMATION("containsSensitivePersonalInformation"),
    @XmlEnumValue("containsInformationOnCriminalConvictionsAndOffenses")
    CONTAINS_INFORMATION_ON_CRIMINAL_CONVICTIONS_AND_OFFENSES("containsInformationOnCriminalConvictionsAndOffenses");
    private final String value;

    PersonalDataType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static PersonalDataType fromValue(String v) {
        for (PersonalDataType c: PersonalDataType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
