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
 *         &lt;element name="HaeAsiakkaitaResult" type="{http://www.suomi.fi/asiointitili}Vastaus_WS1"/&gt;
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
    "haeAsiakkaitaResult"
})
@XmlRootElement(name = "HaeAsiakkaitaResponse")
@Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
public class HaeAsiakkaitaResponse {

    @XmlElement(name = "HaeAsiakkaitaResult", required = true)
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    protected VastausWS1 haeAsiakkaitaResult;

    /**
     * Gets the value of the haeAsiakkaitaResult property.
     * 
     * @return
     *     possible object is
     *     {@link VastausWS1 }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public VastausWS1 getHaeAsiakkaitaResult() {
        return haeAsiakkaitaResult;
    }

    /**
     * Sets the value of the haeAsiakkaitaResult property.
     * 
     * @param value
     *     allowed object is
     *     {@link VastausWS1 }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public void setHaeAsiakkaitaResult(VastausWS1 value) {
        this.haeAsiakkaitaResult = value;
    }

}
