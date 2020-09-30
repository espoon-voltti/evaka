// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.msg.sficlient.soap;

import javax.annotation.Generated;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="Viranomainen" type="{http://www.suomi.fi/asiointitili}Viranomainen"/&gt;
 *         &lt;element name="Kysely" type="{http://www.suomi.fi/asiointitili}Kysely_WS1"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "viranomainen",
    "kysely"
})
@XmlRootElement(name = "HaeAsiakkaita")
@Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
public class HaeAsiakkaita {

    @XmlElement(name = "Viranomainen", required = true)
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    protected Viranomainen viranomainen;
    @XmlElement(name = "Kysely", required = true)
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    protected KyselyWS1 kysely;

    /**
     * Gets the value of the viranomainen property.
     * 
     * @return
     *     possible object is
     *     {@link Viranomainen }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public Viranomainen getViranomainen() {
        return viranomainen;
    }

    /**
     * Sets the value of the viranomainen property.
     * 
     * @param value
     *     allowed object is
     *     {@link Viranomainen }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public void setViranomainen(Viranomainen value) {
        this.viranomainen = value;
    }

    /**
     * Gets the value of the kysely property.
     * 
     * @return
     *     possible object is
     *     {@link KyselyWS1 }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public KyselyWS1 getKysely() {
        return kysely;
    }

    /**
     * Sets the value of the kysely property.
     * 
     * @param value
     *     allowed object is
     *     {@link KyselyWS1 }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public void setKysely(KyselyWS1 value) {
        this.kysely = value;
    }

}
