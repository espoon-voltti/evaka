// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.vtjclient.soap;

import javax.annotation.Generated;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for TuotteenSkeemaResBody complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="TuotteenSkeemaResBody"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="reguest" type="{http://xml.vrk.fi/ws/vtj/vtjkysely/1}TuotteenSkeemaReqBodyTiedot" minOccurs="0"/&gt;
 *         &lt;element name="response" type="{http://xml.vrk.fi/ws/vtj/vtjkysely/1}TuotteenSkeemaResType" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "TuotteenSkeemaResBody", propOrder = {
    "reguest",
    "response"
})
@Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
public class TuotteenSkeemaResBody {

    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
    protected TuotteenSkeemaReqBodyTiedot reguest;
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
    protected TuotteenSkeemaResType response;

    /**
     * Gets the value of the reguest property.
     * 
     * @return
     *     possible object is
     *     {@link TuotteenSkeemaReqBodyTiedot }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
    public TuotteenSkeemaReqBodyTiedot getReguest() {
        return reguest;
    }

    /**
     * Sets the value of the reguest property.
     * 
     * @param value
     *     allowed object is
     *     {@link TuotteenSkeemaReqBodyTiedot }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
    public void setReguest(TuotteenSkeemaReqBodyTiedot value) {
        this.reguest = value;
    }

    /**
     * Gets the value of the response property.
     * 
     * @return
     *     possible object is
     *     {@link TuotteenSkeemaResType }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
    public TuotteenSkeemaResType getResponse() {
        return response;
    }

    /**
     * Sets the value of the response property.
     * 
     * @param value
     *     allowed object is
     *     {@link TuotteenSkeemaResType }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
    public void setResponse(TuotteenSkeemaResType value) {
        this.response = value;
    }

}
