// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.vtjclient.soap;

import javax.annotation.Generated;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType&gt;
 *   &lt;simpleContent&gt;
 *     &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema&gt;string"&gt;
 *       &lt;attribute ref="{http://www.w3.org/XML/1998/namespace}lang default="en""/&gt;
 *     &lt;/extension&gt;
 *   &lt;/simpleContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "value"
})
@XmlRootElement(name = "title", namespace = "http://x-road.eu/xsd/xroad.xsd")
@Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
public class Title {

    @XmlValue
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
    protected String value;
    @XmlAttribute(name = "lang", namespace = "http://www.w3.org/XML/1998/namespace")
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
    protected String lang;

    /**
     * Gets the value of the value property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
    public String getValue() {
        return value;
    }

    /**
     * Sets the value of the value property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Gets the value of the lang property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
    public String getLang() {
        if (lang == null) {
            return "en";
        } else {
            return lang;
        }
    }

    /**
     * Sets the value of the lang property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
    public void setLang(String value) {
        this.lang = value;
    }

}
