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
 * <p>Java class for Vastaus_WS2 complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="Vastaus_WS2"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="TilaKoodi" type="{http://www.suomi.fi/asiointitili}TilaKoodi_WS" minOccurs="0"/&gt;
 *         &lt;element name="KohdeMaara" type="{http://www.w3.org/2001/XMLSchema}int" minOccurs="0"/&gt;
 *         &lt;element name="Kohteet" type="{http://www.suomi.fi/asiointitili}ArrayOfKohdeJaAsiakasTila_WS2_V" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Vastaus_WS2", propOrder = {
    "tilaKoodi",
    "kohdeMaara",
    "kohteet"
})
@Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
public class VastausWS2 {

    @XmlElement(name = "TilaKoodi")
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    protected TilaKoodiWS tilaKoodi;
    @XmlElement(name = "KohdeMaara")
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    protected Integer kohdeMaara;
    @XmlElement(name = "Kohteet")
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    protected ArrayOfKohdeJaAsiakasTilaWS2V kohteet;

    /**
     * Gets the value of the tilaKoodi property.
     * 
     * @return
     *     possible object is
     *     {@link TilaKoodiWS }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public TilaKoodiWS getTilaKoodi() {
        return tilaKoodi;
    }

    /**
     * Sets the value of the tilaKoodi property.
     * 
     * @param value
     *     allowed object is
     *     {@link TilaKoodiWS }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public void setTilaKoodi(TilaKoodiWS value) {
        this.tilaKoodi = value;
    }

    /**
     * Gets the value of the kohdeMaara property.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public Integer getKohdeMaara() {
        return kohdeMaara;
    }

    /**
     * Sets the value of the kohdeMaara property.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public void setKohdeMaara(Integer value) {
        this.kohdeMaara = value;
    }

    /**
     * Gets the value of the kohteet property.
     * 
     * @return
     *     possible object is
     *     {@link ArrayOfKohdeJaAsiakasTilaWS2V }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public ArrayOfKohdeJaAsiakasTilaWS2V getKohteet() {
        return kohteet;
    }

    /**
     * Sets the value of the kohteet property.
     * 
     * @param value
     *     allowed object is
     *     {@link ArrayOfKohdeJaAsiakasTilaWS2V }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public void setKohteet(ArrayOfKohdeJaAsiakasTilaWS2V value) {
        this.kohteet = value;
    }

}
