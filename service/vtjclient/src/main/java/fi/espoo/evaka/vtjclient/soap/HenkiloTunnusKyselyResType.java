// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.vtjclient.soap;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Generated;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementRefs;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for HenkiloTunnusKyselyResType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="HenkiloTunnusKyselyResType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence maxOccurs="3"&gt;
 *         &lt;element ref="{http://xml.vrk.fi/schema/vtjkysely}VTJHenkiloVastaussanoma" minOccurs="0"/&gt;
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
@XmlType(name = "HenkiloTunnusKyselyResType", propOrder = {
    "vtjHenkiloVastaussanomaAndFaultCodeAndFaultString"
})
@Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
public class HenkiloTunnusKyselyResType {

    @XmlElementRefs({
        @XmlElementRef(name = "VTJHenkiloVastaussanoma", namespace = "http://xml.vrk.fi/schema/vtjkysely", type = VTJHenkiloVastaussanoma.class, required = false),
        @XmlElementRef(name = "faultCode", namespace = "http://xml.vrk.fi/ws/vtj/vtjkysely/1", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "faultString", namespace = "http://xml.vrk.fi/ws/vtj/vtjkysely/1", type = JAXBElement.class, required = false)
    })
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
    protected List<Object> vtjHenkiloVastaussanomaAndFaultCodeAndFaultString;

    /**
     * Gets the value of the vtjHenkiloVastaussanomaAndFaultCodeAndFaultString property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the vtjHenkiloVastaussanomaAndFaultCodeAndFaultString property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getVTJHenkiloVastaussanomaAndFaultCodeAndFaultString().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link VTJHenkiloVastaussanoma }
     * {@link JAXBElement }{@code <}{@link String }{@code >}
     * {@link JAXBElement }{@code <}{@link String }{@code >}
     * 
     * 
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
    public List<Object> getVTJHenkiloVastaussanomaAndFaultCodeAndFaultString() {
        if (vtjHenkiloVastaussanomaAndFaultCodeAndFaultString == null) {
            vtjHenkiloVastaussanomaAndFaultCodeAndFaultString = new ArrayList<Object>();
        }
        return this.vtjHenkiloVastaussanomaAndFaultCodeAndFaultString;
    }

}
