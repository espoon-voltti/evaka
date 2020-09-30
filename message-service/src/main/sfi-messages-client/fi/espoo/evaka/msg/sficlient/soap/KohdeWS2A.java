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
 * <p>Java class for Kohde_WS2A complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="Kohde_WS2A"&gt;
 *   &lt;complexContent&gt;
 *     &lt;extension base="{http://www.suomi.fi/asiointitili}Kohde_WS2"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="Sivut" type="{http://www.suomi.fi/asiointitili}Sivut" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/extension&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Kohde_WS2A", propOrder = {
    "sivut"
})
@Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
public class KohdeWS2A
    extends KohdeWS2
{

    @XmlElement(name = "Sivut")
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    protected Sivut sivut;

    /**
     * Gets the value of the sivut property.
     * 
     * @return
     *     possible object is
     *     {@link Sivut }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public Sivut getSivut() {
        return sivut;
    }

    /**
     * Sets the value of the sivut property.
     * 
     * @param value
     *     allowed object is
     *     {@link Sivut }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public void setSivut(Sivut value) {
        this.sivut = value;
    }

}
