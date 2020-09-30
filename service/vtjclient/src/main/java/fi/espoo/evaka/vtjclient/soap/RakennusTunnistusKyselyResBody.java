// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.vtjclient.soap;

import javax.annotation.Generated;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for RakennusTunnistusKyselyResBody complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="RakennusTunnistusKyselyResBody"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="request" type="{http://xml.vrk.fi/ws/vtj/vtjkysely/1}RakennusTunnistusKyselyReqBodyTiedot" minOccurs="0"/&gt;
 *         &lt;element name="response" type="{http://xml.vrk.fi/ws/vtj/vtjkysely/1}RakennusTunnistusKyselyResType" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "RakennusTunnistusKyselyResBody", propOrder = {
    "request",
    "response"
})
@Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
public class RakennusTunnistusKyselyResBody {

    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
    protected RakennusTunnistusKyselyReqBodyTiedot request;
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
    protected RakennusTunnistusKyselyResType response;

    /**
     * Gets the value of the request property.
     * 
     * @return
     *     possible object is
     *     {@link RakennusTunnistusKyselyReqBodyTiedot }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
    public RakennusTunnistusKyselyReqBodyTiedot getRequest() {
        return request;
    }

    /**
     * Sets the value of the request property.
     * 
     * @param value
     *     allowed object is
     *     {@link RakennusTunnistusKyselyReqBodyTiedot }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
    public void setRequest(RakennusTunnistusKyselyReqBodyTiedot value) {
        this.request = value;
    }

    /**
     * Gets the value of the response property.
     * 
     * @return
     *     possible object is
     *     {@link RakennusTunnistusKyselyResType }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
    public RakennusTunnistusKyselyResType getResponse() {
        return response;
    }

    /**
     * Sets the value of the response property.
     * 
     * @param value
     *     allowed object is
     *     {@link RakennusTunnistusKyselyResType }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
    public void setResponse(RakennusTunnistusKyselyResType value) {
        this.response = value;
    }

}
