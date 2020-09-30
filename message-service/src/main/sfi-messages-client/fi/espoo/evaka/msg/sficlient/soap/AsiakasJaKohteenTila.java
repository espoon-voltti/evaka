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
 * <p>Java class for AsiakasJaKohteenTila complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="AsiakasJaKohteenTila"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="AsiointitiliTunniste" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="KohteenTila" type="{http://www.w3.org/2001/XMLSchema}int"/&gt;
 *         &lt;element name="KohteenTilaKuvaus" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *       &lt;attribute name="AsiakasTunnus" type="{http://www.w3.org/2001/XMLSchema}string" /&gt;
 *       &lt;attribute name="TunnusTyyppi" type="{http://www.w3.org/2001/XMLSchema}string" /&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "AsiakasJaKohteenTila", propOrder = {
    "asiointitiliTunniste",
    "kohteenTila",
    "kohteenTilaKuvaus"
})
@Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
public class AsiakasJaKohteenTila {

    @XmlElement(name = "AsiointitiliTunniste")
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    protected String asiointitiliTunniste;
    @XmlElement(name = "KohteenTila")
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    protected int kohteenTila;
    @XmlElement(name = "KohteenTilaKuvaus")
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    protected String kohteenTilaKuvaus;
    @XmlAttribute(name = "AsiakasTunnus")
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    protected String asiakasTunnus;
    @XmlAttribute(name = "TunnusTyyppi")
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    protected String tunnusTyyppi;

    /**
     * Gets the value of the asiointitiliTunniste property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public String getAsiointitiliTunniste() {
        return asiointitiliTunniste;
    }

    /**
     * Sets the value of the asiointitiliTunniste property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public void setAsiointitiliTunniste(String value) {
        this.asiointitiliTunniste = value;
    }

    /**
     * Gets the value of the kohteenTila property.
     * 
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public int getKohteenTila() {
        return kohteenTila;
    }

    /**
     * Sets the value of the kohteenTila property.
     * 
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public void setKohteenTila(int value) {
        this.kohteenTila = value;
    }

    /**
     * Gets the value of the kohteenTilaKuvaus property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public String getKohteenTilaKuvaus() {
        return kohteenTilaKuvaus;
    }

    /**
     * Sets the value of the kohteenTilaKuvaus property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public void setKohteenTilaKuvaus(String value) {
        this.kohteenTilaKuvaus = value;
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
     * Gets the value of the tunnusTyyppi property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public String getTunnusTyyppi() {
        return tunnusTyyppi;
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
