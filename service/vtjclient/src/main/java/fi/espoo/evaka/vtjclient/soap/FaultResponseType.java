// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.vtjclient.soap;

import javax.annotation.Generated;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for FaultResponseType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="FaultResponseType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="faultCode" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="faultString" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "FaultResponseType", propOrder = {
    "faultCode",
    "faultString"
})
@Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
public class FaultResponseType {

    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
    protected String faultCode;
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
    protected String faultString;

    /**
     * Gets the value of the faultCode property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
    public String getFaultCode() {
        return faultCode;
    }

    /**
     * Sets the value of the faultCode property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
    public void setFaultCode(String value) {
        this.faultCode = value;
    }

    /**
     * Gets the value of the faultString property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
    public String getFaultString() {
        return faultString;
    }

    /**
     * Sets the value of the faultString property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
    public void setFaultString(String value) {
        this.faultString = value;
    }

}
