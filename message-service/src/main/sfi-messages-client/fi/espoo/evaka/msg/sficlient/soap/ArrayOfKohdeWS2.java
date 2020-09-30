// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.msg.sficlient.soap;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Generated;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for ArrayOfKohde_WS2 complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ArrayOfKohde_WS2"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="Kohde" type="{http://www.suomi.fi/asiointitili}Kohde_WS2" maxOccurs="unbounded" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ArrayOfKohde_WS2", propOrder = {
    "kohde"
})
@Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
public class ArrayOfKohdeWS2 {

    @XmlElement(name = "Kohde", nillable = true)
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    protected List<KohdeWS2> kohde;

    /**
     * Gets the value of the kohde property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the kohde property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getKohde().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link KohdeWS2 }
     * 
     * 
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public List<KohdeWS2> getKohde() {
        if (kohde == null) {
            kohde = new ArrayList<KohdeWS2>();
        }
        return this.kohde;
    }

}
