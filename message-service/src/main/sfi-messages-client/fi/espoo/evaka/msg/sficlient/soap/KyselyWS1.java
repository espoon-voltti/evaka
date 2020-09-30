// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.msg.sficlient.soap;

import javax.annotation.Generated;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.datatype.XMLGregorianCalendar;


/**
 * <p>Java class for Kysely_WS1 complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="Kysely_WS1"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="KyselyLaji" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="KyselyAlku" type="{http://www.w3.org/2001/XMLSchema}dateTime"/&gt;
 *         &lt;element name="KyselyLoppu" type="{http://www.w3.org/2001/XMLSchema}dateTime"/&gt;
 *         &lt;element name="Asiakkaat" type="{http://www.suomi.fi/asiointitili}ArrayOfAsiakas" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Kysely_WS1", propOrder = {
    "kyselyLaji",
    "kyselyAlku",
    "kyselyLoppu",
    "asiakkaat"
})
@Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
public class KyselyWS1 {

    @XmlElement(name = "KyselyLaji")
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    protected String kyselyLaji;
    @XmlElement(name = "KyselyAlku", required = true, nillable = true)
    @XmlSchemaType(name = "dateTime")
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    protected XMLGregorianCalendar kyselyAlku;
    @XmlElement(name = "KyselyLoppu", required = true, nillable = true)
    @XmlSchemaType(name = "dateTime")
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    protected XMLGregorianCalendar kyselyLoppu;
    @XmlElement(name = "Asiakkaat")
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    protected ArrayOfAsiakas asiakkaat;

    /**
     * Gets the value of the kyselyLaji property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public String getKyselyLaji() {
        return kyselyLaji;
    }

    /**
     * Sets the value of the kyselyLaji property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public void setKyselyLaji(String value) {
        this.kyselyLaji = value;
    }

    /**
     * Gets the value of the kyselyAlku property.
     * 
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public XMLGregorianCalendar getKyselyAlku() {
        return kyselyAlku;
    }

    /**
     * Sets the value of the kyselyAlku property.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public void setKyselyAlku(XMLGregorianCalendar value) {
        this.kyselyAlku = value;
    }

    /**
     * Gets the value of the kyselyLoppu property.
     * 
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public XMLGregorianCalendar getKyselyLoppu() {
        return kyselyLoppu;
    }

    /**
     * Sets the value of the kyselyLoppu property.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public void setKyselyLoppu(XMLGregorianCalendar value) {
        this.kyselyLoppu = value;
    }

    /**
     * Gets the value of the asiakkaat property.
     * 
     * @return
     *     possible object is
     *     {@link ArrayOfAsiakas }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public ArrayOfAsiakas getAsiakkaat() {
        return asiakkaat;
    }

    /**
     * Sets the value of the asiakkaat property.
     * 
     * @param value
     *     allowed object is
     *     {@link ArrayOfAsiakas }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public void setAsiakkaat(ArrayOfAsiakas value) {
        this.asiakkaat = value;
    }

}
