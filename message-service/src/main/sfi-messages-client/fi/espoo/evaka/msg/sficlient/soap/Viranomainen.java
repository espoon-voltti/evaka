// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.msg.sficlient.soap;

import javax.annotation.Generated;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for Viranomainen complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="Viranomainen"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="ViranomaisTunnus" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="PalveluTunnus" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="KayttajaTunnus" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="Yhteyshenkilo" type="{http://www.suomi.fi/asiointitili}Yhteyshenkilo" minOccurs="0"/&gt;
 *         &lt;element name="Osoite" type="{http://www.suomi.fi/asiointitili}Osoite" minOccurs="0"/&gt;
 *         &lt;element name="SanomaTunniste" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="SanomaVersio" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="SanomaVarmenneNimi" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Viranomainen", propOrder = {
    "viranomaisTunnus",
    "palveluTunnus",
    "kayttajaTunnus",
    "yhteyshenkilo",
    "osoite",
    "sanomaTunniste",
    "sanomaVersio",
    "sanomaVarmenneNimi"
})
@Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
public class Viranomainen {

    @XmlElement(name = "ViranomaisTunnus")
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    protected String viranomaisTunnus;
    @XmlElement(name = "PalveluTunnus")
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    protected String palveluTunnus;
    @XmlElement(name = "KayttajaTunnus")
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    protected String kayttajaTunnus;
    @XmlElement(name = "Yhteyshenkilo")
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    protected Yhteyshenkilo yhteyshenkilo;
    @XmlElement(name = "Osoite")
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    protected Osoite osoite;
    @XmlElement(name = "SanomaTunniste")
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    protected String sanomaTunniste;
    @XmlElement(name = "SanomaVersio")
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    protected String sanomaVersio;
    @XmlElement(name = "SanomaVarmenneNimi")
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    protected String sanomaVarmenneNimi;

    /**
     * Gets the value of the viranomaisTunnus property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public String getViranomaisTunnus() {
        return viranomaisTunnus;
    }

    /**
     * Sets the value of the viranomaisTunnus property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public void setViranomaisTunnus(String value) {
        this.viranomaisTunnus = value;
    }

    /**
     * Gets the value of the palveluTunnus property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public String getPalveluTunnus() {
        return palveluTunnus;
    }

    /**
     * Sets the value of the palveluTunnus property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public void setPalveluTunnus(String value) {
        this.palveluTunnus = value;
    }

    /**
     * Gets the value of the kayttajaTunnus property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public String getKayttajaTunnus() {
        return kayttajaTunnus;
    }

    /**
     * Sets the value of the kayttajaTunnus property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public void setKayttajaTunnus(String value) {
        this.kayttajaTunnus = value;
    }

    /**
     * Gets the value of the yhteyshenkilo property.
     * 
     * @return
     *     possible object is
     *     {@link Yhteyshenkilo }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public Yhteyshenkilo getYhteyshenkilo() {
        return yhteyshenkilo;
    }

    /**
     * Sets the value of the yhteyshenkilo property.
     * 
     * @param value
     *     allowed object is
     *     {@link Yhteyshenkilo }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public void setYhteyshenkilo(Yhteyshenkilo value) {
        this.yhteyshenkilo = value;
    }

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
     * Gets the value of the sanomaTunniste property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public String getSanomaTunniste() {
        return sanomaTunniste;
    }

    /**
     * Sets the value of the sanomaTunniste property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public void setSanomaTunniste(String value) {
        this.sanomaTunniste = value;
    }

    /**
     * Gets the value of the sanomaVersio property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public String getSanomaVersio() {
        return sanomaVersio;
    }

    /**
     * Sets the value of the sanomaVersio property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public void setSanomaVersio(String value) {
        this.sanomaVersio = value;
    }

    /**
     * Gets the value of the sanomaVarmenneNimi property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public String getSanomaVarmenneNimi() {
        return sanomaVarmenneNimi;
    }

    /**
     * Sets the value of the sanomaVarmenneNimi property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public void setSanomaVarmenneNimi(String value) {
        this.sanomaVarmenneNimi = value;
    }

}
