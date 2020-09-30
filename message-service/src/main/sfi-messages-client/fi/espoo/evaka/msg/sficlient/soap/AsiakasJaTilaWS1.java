// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.msg.sficlient.soap;

import javax.annotation.Generated;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.datatype.XMLGregorianCalendar;


/**
 * <p>Java class for AsiakasJaTila_WS1 complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="AsiakasJaTila_WS1"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="Tila" type="{http://www.w3.org/2001/XMLSchema}int"/&gt;
 *         &lt;element name="TilaPvm" type="{http://www.w3.org/2001/XMLSchema}dateTime"/&gt;
 *         &lt;element name="TiliPassivoitu" type="{http://www.w3.org/2001/XMLSchema}int"/&gt;
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
@XmlType(name = "AsiakasJaTila_WS1", propOrder = {
    "tila",
    "tilaPvm",
    "tiliPassivoitu"
})
@Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
public class AsiakasJaTilaWS1 {

    @XmlElement(name = "Tila")
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    protected int tila;
    @XmlElement(name = "TilaPvm", required = true)
    @XmlSchemaType(name = "dateTime")
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    protected XMLGregorianCalendar tilaPvm;
    @XmlElement(name = "TiliPassivoitu")
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    protected int tiliPassivoitu;
    @XmlAttribute(name = "AsiakasTunnus")
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    protected String asiakasTunnus;
    @XmlAttribute(name = "TunnusTyyppi")
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    protected String tunnusTyyppi;

    /**
     * Gets the value of the tila property.
     * 
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public int getTila() {
        return tila;
    }

    /**
     * Sets the value of the tila property.
     * 
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public void setTila(int value) {
        this.tila = value;
    }

    /**
     * Gets the value of the tilaPvm property.
     * 
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public XMLGregorianCalendar getTilaPvm() {
        return tilaPvm;
    }

    /**
     * Sets the value of the tilaPvm property.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public void setTilaPvm(XMLGregorianCalendar value) {
        this.tilaPvm = value;
    }

    /**
     * Gets the value of the tiliPassivoitu property.
     * 
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public int getTiliPassivoitu() {
        return tiliPassivoitu;
    }

    /**
     * Sets the value of the tiliPassivoitu property.
     * 
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public void setTiliPassivoitu(int value) {
        this.tiliPassivoitu = value;
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
