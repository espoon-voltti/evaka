// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.msg.sficlient.soap;

import javax.annotation.Generated;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for Asiakas complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="Asiakas"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="Osoite" type="{http://www.suomi.fi/asiointitili}Osoite" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *       &lt;attribute name="AsiakasTunnus" type="{http://www.w3.org/2001/XMLSchema}string" /&gt;
 *       &lt;attribute name="Sahkoposti" type="{http://www.w3.org/2001/XMLSchema}string" /&gt;
 *       &lt;attribute name="Matkapuhelin" type="{http://www.w3.org/2001/XMLSchema}string" /&gt;
 *       &lt;attribute name="TunnusTyyppi" type="{http://www.w3.org/2001/XMLSchema}string" default="SSN" /&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Asiakas", propOrder = {
    "osoite"
})
@Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
public class Asiakas {

    @XmlElement(name = "Osoite")
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    protected Osoite osoite;
    @XmlAttribute(name = "AsiakasTunnus")
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    protected String asiakasTunnus;
    @XmlAttribute(name = "Sahkoposti")
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    protected String sahkoposti;
    @XmlAttribute(name = "Matkapuhelin")
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    protected String matkapuhelin;
    @XmlAttribute(name = "TunnusTyyppi")
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    protected String tunnusTyyppi;

    /**
     * Gets the value of the osoite property.
     * 
     * @return
     *     possible object is
     *     {@link Osoite }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public Osoite getOsoite() {
        return osoite;
    }

    /**
     * Sets the value of the osoite property.
     * 
     * @param value
     *     allowed object is
     *     {@link Osoite }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public void setOsoite(Osoite value) {
        this.osoite = value;
    }

    /**
     * Gets the value of the asiakasTunnus property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public String getAsiakasTunnus() {
        return asiakasTunnus;
    }

    /**
     * Sets the value of the asiakasTunnus property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public void setAsiakasTunnus(String value) {
        this.asiakasTunnus = value;
    }

    /**
     * Gets the value of the sahkoposti property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public String getSahkoposti() {
        return sahkoposti;
    }

    /**
     * Sets the value of the sahkoposti property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public void setSahkoposti(String value) {
        this.sahkoposti = value;
    }

    /**
     * Gets the value of the matkapuhelin property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public String getMatkapuhelin() {
        return matkapuhelin;
    }

    /**
     * Sets the value of the matkapuhelin property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public void setMatkapuhelin(String value) {
        this.matkapuhelin = value;
    }

    /**
     * Gets the value of the tunnusTyyppi property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public String getTunnusTyyppi() {
        if (tunnusTyyppi == null) {
            return "SSN";
        } else {
            return tunnusTyyppi;
        }
    }

    /**
     * Sets the value of the tunnusTyyppi property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public void setTunnusTyyppi(String value) {
        this.tunnusTyyppi = value;
    }

}
