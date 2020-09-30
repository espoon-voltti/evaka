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
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;
import javax.xml.datatype.XMLGregorianCalendar;


/**
 * <p>Java class for Kohde_WS2 complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="Kohde_WS2"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="Asiakas" type="{http://www.suomi.fi/asiointitili}Asiakas" maxOccurs="unbounded"/&gt;
 *         &lt;element name="ViranomaisTunniste" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="Viittaus" type="{http://www.suomi.fi/asiointitili}Viittaus_WS" minOccurs="0"/&gt;
 *         &lt;element name="VahvistusVaatimus" type="{http://www.w3.org/2001/XMLSchema}int" minOccurs="0"/&gt;
 *         &lt;element name="VaadiLukukuittaus" type="{http://www.w3.org/2001/XMLSchema}int" minOccurs="0"/&gt;
 *         &lt;element name="AsiaNumero" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="Nimeke" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="LahetysPvm" type="{http://www.w3.org/2001/XMLSchema}dateTime"/&gt;
 *         &lt;element name="LahettajaNimi" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="KuvausTeksti" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="Maksullisuus" type="{http://www.w3.org/2001/XMLSchema}int" minOccurs="0"/&gt;
 *         &lt;element name="MaksamisKuvausTeksti" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="Tila" type="{http://www.suomi.fi/asiointitili}Tila_WS2" minOccurs="0"/&gt;
 *         &lt;element name="Tiedostot" type="{http://www.suomi.fi/asiointitili}ArrayOfTiedosto" minOccurs="0"/&gt;
 *         &lt;element name="ViranomaisenEmail" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="SmsLisatieto" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="EmailLisatietoOtsikko" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="EmailLisatietoSisalto" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="TavoitettavuusTietoSMS" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="TavoitettavuusTietoEmail" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Kohde_WS2", propOrder = {
    "asiakas",
    "viranomaisTunniste",
    "viittaus",
    "vahvistusVaatimus",
    "vaadiLukukuittaus",
    "asiaNumero",
    "nimeke",
    "lahetysPvm",
    "lahettajaNimi",
    "kuvausTeksti",
    "maksullisuus",
    "maksamisKuvausTeksti",
    "tila",
    "tiedostot",
    "viranomaisenEmail",
    "smsLisatieto",
    "emailLisatietoOtsikko",
    "emailLisatietoSisalto",
    "tavoitettavuusTietoSMS",
    "tavoitettavuusTietoEmail"
})
@XmlSeeAlso({
    KohdeWS2A.class
})
@Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
public class KohdeWS2 {

    @XmlElement(name = "Asiakas", required = true)
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    protected List<Asiakas> asiakas;
    @XmlElement(name = "ViranomaisTunniste")
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    protected String viranomaisTunniste;
    @XmlElement(name = "Viittaus")
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    protected ViittausWS viittaus;
    @XmlElement(name = "VahvistusVaatimus")
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    protected Integer vahvistusVaatimus;
    @XmlElement(name = "VaadiLukukuittaus")
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    protected Integer vaadiLukukuittaus;
    @XmlElement(name = "AsiaNumero")
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    protected String asiaNumero;
    @XmlElement(name = "Nimeke")
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    protected String nimeke;
    @XmlElement(name = "LahetysPvm", required = true)
    @XmlSchemaType(name = "dateTime")
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    protected XMLGregorianCalendar lahetysPvm;
    @XmlElement(name = "LahettajaNimi")
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    protected String lahettajaNimi;
    @XmlElement(name = "KuvausTeksti")
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    protected String kuvausTeksti;
    @XmlElement(name = "Maksullisuus")
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    protected Integer maksullisuus;
    @XmlElement(name = "MaksamisKuvausTeksti")
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    protected String maksamisKuvausTeksti;
    @XmlElement(name = "Tila")
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    protected TilaWS2 tila;
    @XmlElement(name = "Tiedostot")
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    protected ArrayOfTiedosto tiedostot;
    @XmlElement(name = "ViranomaisenEmail")
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    protected String viranomaisenEmail;
    @XmlElement(name = "SmsLisatieto")
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    protected String smsLisatieto;
    @XmlElement(name = "EmailLisatietoOtsikko")
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    protected String emailLisatietoOtsikko;
    @XmlElement(name = "EmailLisatietoSisalto")
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    protected String emailLisatietoSisalto;
    @XmlElement(name = "TavoitettavuusTietoSMS")
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    protected String tavoitettavuusTietoSMS;
    @XmlElement(name = "TavoitettavuusTietoEmail")
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    protected String tavoitettavuusTietoEmail;

    /**
     * Gets the value of the asiakas property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the asiakas property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getAsiakas().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Asiakas }
     * 
     * 
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public List<Asiakas> getAsiakas() {
        if (asiakas == null) {
            asiakas = new ArrayList<Asiakas>();
        }
        return this.asiakas;
    }

    /**
     * Gets the value of the viranomaisTunniste property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public String getViranomaisTunniste() {
        return viranomaisTunniste;
    }

    /**
     * Sets the value of the viranomaisTunniste property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public void setViranomaisTunniste(String value) {
        this.viranomaisTunniste = value;
    }

    /**
     * Gets the value of the viittaus property.
     * 
     * @return
     *     possible object is
     *     {@link ViittausWS }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public ViittausWS getViittaus() {
        return viittaus;
    }

    /**
     * Sets the value of the viittaus property.
     * 
     * @param value
     *     allowed object is
     *     {@link ViittausWS }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public void setViittaus(ViittausWS value) {
        this.viittaus = value;
    }

    /**
     * Gets the value of the vahvistusVaatimus property.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public Integer getVahvistusVaatimus() {
        return vahvistusVaatimus;
    }

    /**
     * Sets the value of the vahvistusVaatimus property.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public void setVahvistusVaatimus(Integer value) {
        this.vahvistusVaatimus = value;
    }

    /**
     * Gets the value of the vaadiLukukuittaus property.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public Integer getVaadiLukukuittaus() {
        return vaadiLukukuittaus;
    }

    /**
     * Sets the value of the vaadiLukukuittaus property.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public void setVaadiLukukuittaus(Integer value) {
        this.vaadiLukukuittaus = value;
    }

    /**
     * Gets the value of the asiaNumero property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public String getAsiaNumero() {
        return asiaNumero;
    }

    /**
     * Sets the value of the asiaNumero property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public void setAsiaNumero(String value) {
        this.asiaNumero = value;
    }

    /**
     * Gets the value of the nimeke property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public String getNimeke() {
        return nimeke;
    }

    /**
     * Sets the value of the nimeke property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public void setNimeke(String value) {
        this.nimeke = value;
    }

    /**
     * Gets the value of the lahetysPvm property.
     * 
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public XMLGregorianCalendar getLahetysPvm() {
        return lahetysPvm;
    }

    /**
     * Sets the value of the lahetysPvm property.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public void setLahetysPvm(XMLGregorianCalendar value) {
        this.lahetysPvm = value;
    }

    /**
     * Gets the value of the lahettajaNimi property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public String getLahettajaNimi() {
        return lahettajaNimi;
    }

    /**
     * Sets the value of the lahettajaNimi property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public void setLahettajaNimi(String value) {
        this.lahettajaNimi = value;
    }

    /**
     * Gets the value of the kuvausTeksti property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public String getKuvausTeksti() {
        return kuvausTeksti;
    }

    /**
     * Sets the value of the kuvausTeksti property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public void setKuvausTeksti(String value) {
        this.kuvausTeksti = value;
    }

    /**
     * Gets the value of the maksullisuus property.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public Integer getMaksullisuus() {
        return maksullisuus;
    }

    /**
     * Sets the value of the maksullisuus property.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public void setMaksullisuus(Integer value) {
        this.maksullisuus = value;
    }

    /**
     * Gets the value of the maksamisKuvausTeksti property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public String getMaksamisKuvausTeksti() {
        return maksamisKuvausTeksti;
    }

    /**
     * Sets the value of the maksamisKuvausTeksti property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public void setMaksamisKuvausTeksti(String value) {
        this.maksamisKuvausTeksti = value;
    }

    /**
     * Gets the value of the tila property.
     * 
     * @return
     *     possible object is
     *     {@link TilaWS2 }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public TilaWS2 getTila() {
        return tila;
    }

    /**
     * Sets the value of the tila property.
     * 
     * @param value
     *     allowed object is
     *     {@link TilaWS2 }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public void setTila(TilaWS2 value) {
        this.tila = value;
    }

    /**
     * Gets the value of the tiedostot property.
     * 
     * @return
     *     possible object is
     *     {@link ArrayOfTiedosto }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public ArrayOfTiedosto getTiedostot() {
        return tiedostot;
    }

    /**
     * Sets the value of the tiedostot property.
     * 
     * @param value
     *     allowed object is
     *     {@link ArrayOfTiedosto }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public void setTiedostot(ArrayOfTiedosto value) {
        this.tiedostot = value;
    }

    /**
     * Gets the value of the viranomaisenEmail property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public String getViranomaisenEmail() {
        return viranomaisenEmail;
    }

    /**
     * Sets the value of the viranomaisenEmail property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public void setViranomaisenEmail(String value) {
        this.viranomaisenEmail = value;
    }

    /**
     * Gets the value of the smsLisatieto property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public String getSmsLisatieto() {
        return smsLisatieto;
    }

    /**
     * Sets the value of the smsLisatieto property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public void setSmsLisatieto(String value) {
        this.smsLisatieto = value;
    }

    /**
     * Gets the value of the emailLisatietoOtsikko property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public String getEmailLisatietoOtsikko() {
        return emailLisatietoOtsikko;
    }

    /**
     * Sets the value of the emailLisatietoOtsikko property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public void setEmailLisatietoOtsikko(String value) {
        this.emailLisatietoOtsikko = value;
    }

    /**
     * Gets the value of the emailLisatietoSisalto property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public String getEmailLisatietoSisalto() {
        return emailLisatietoSisalto;
    }

    /**
     * Sets the value of the emailLisatietoSisalto property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public void setEmailLisatietoSisalto(String value) {
        this.emailLisatietoSisalto = value;
    }

    /**
     * Gets the value of the tavoitettavuusTietoSMS property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public String getTavoitettavuusTietoSMS() {
        return tavoitettavuusTietoSMS;
    }

    /**
     * Sets the value of the tavoitettavuusTietoSMS property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public void setTavoitettavuusTietoSMS(String value) {
        this.tavoitettavuusTietoSMS = value;
    }

    /**
     * Gets the value of the tavoitettavuusTietoEmail property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public String getTavoitettavuusTietoEmail() {
        return tavoitettavuusTietoEmail;
    }

    /**
     * Sets the value of the tavoitettavuusTietoEmail property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public void setTavoitettavuusTietoEmail(String value) {
        this.tavoitettavuusTietoEmail = value;
    }

}
