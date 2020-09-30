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
 * <p>Java class for Vastaus_WS1 complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="Vastaus_WS1"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="TilaKoodi" type="{http://www.suomi.fi/asiointitili}TilaKoodi_WS" minOccurs="0"/&gt;
 *         &lt;element name="Asiakkaat" type="{http://www.suomi.fi/asiointitili}ArrayOfAsiakasJaTila_WS1" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Vastaus_WS1", propOrder = {
    "tilaKoodi",
    "asiakkaat"
})
@Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
public class VastausWS1 {

    @XmlElement(name = "TilaKoodi")
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    protected TilaKoodiWS tilaKoodi;
    @XmlElement(name = "Asiakkaat")
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    protected ArrayOfAsiakasJaTilaWS1 asiakkaat;

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
     * Gets the value of the asiakkaat property.
     * 
     * @return
     *     possible object is
     *     {@link ArrayOfAsiakasJaTilaWS1 }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public ArrayOfAsiakasJaTilaWS1 getAsiakkaat() {
        return asiakkaat;
    }

    /**
     * Sets the value of the asiakkaat property.
     * 
     * @param value
     *     allowed object is
     *     {@link ArrayOfAsiakasJaTilaWS1 }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public void setAsiakkaat(ArrayOfAsiakasJaTilaWS1 value) {
        this.asiakkaat = value;
    }

}
