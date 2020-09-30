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
 * <p>Java class for Kysely_WS2 complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="Kysely_WS2"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="KohdeMaara" type="{http://www.w3.org/2001/XMLSchema}int"/&gt;
 *         &lt;element name="Kohteet" type="{http://www.suomi.fi/asiointitili}ArrayOfKohde_WS2" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Kysely_WS2", propOrder = {
    "kohdeMaara",
    "kohteet"
})
@Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
public class KyselyWS2 {

    @XmlElement(name = "KohdeMaara")
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    protected int kohdeMaara;
    @XmlElement(name = "Kohteet")
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    protected ArrayOfKohdeWS2 kohteet;

    /**
     * Gets the value of the kohdeMaara property.
     * 
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public int getKohdeMaara() {
        return kohdeMaara;
    }

    /**
     * Sets the value of the kohdeMaara property.
     * 
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public void setKohdeMaara(int value) {
        this.kohdeMaara = value;
    }

    /**
     * Gets the value of the kohteet property.
     * 
     * @return
     *     possible object is
     *     {@link ArrayOfKohdeWS2 }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public ArrayOfKohdeWS2 getKohteet() {
        return kohteet;
    }

    /**
     * Sets the value of the kohteet property.
     * 
     * @param value
     *     allowed object is
     *     {@link ArrayOfKohdeWS2 }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public void setKohteet(ArrayOfKohdeWS2 value) {
        this.kohteet = value;
    }

}
