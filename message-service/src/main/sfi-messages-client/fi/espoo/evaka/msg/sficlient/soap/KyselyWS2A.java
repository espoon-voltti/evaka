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
 * <p>Java class for Kysely_WS2A complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="Kysely_WS2A"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="Tiedostot" type="{http://www.suomi.fi/asiointitili}Tiedosto" maxOccurs="2" minOccurs="0"/&gt;
 *         &lt;element name="Paperi" type="{http://www.w3.org/2001/XMLSchema}boolean"/&gt;
 *         &lt;element name="Kohteet" type="{http://www.suomi.fi/asiointitili}ArrayOfKohde_WS2A" minOccurs="0"/&gt;
 *         &lt;element name="Tulostustoimittaja" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *         &lt;element name="LahetaTulostukseen" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/&gt;
 *         &lt;element name="Laskutus" minOccurs="0"&gt;
 *           &lt;complexType&gt;
 *             &lt;complexContent&gt;
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                 &lt;sequence&gt;
 *                   &lt;element name="Tunniste"&gt;
 *                     &lt;simpleType&gt;
 *                       &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string"&gt;
 *                         &lt;minLength value="6"/&gt;
 *                         &lt;maxLength value="6"/&gt;
 *                       &lt;/restriction&gt;
 *                     &lt;/simpleType&gt;
 *                   &lt;/element&gt;
 *                   &lt;element name="Salasana" minOccurs="0"&gt;
 *                     &lt;simpleType&gt;
 *                       &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string"&gt;
 *                         &lt;minLength value="4"/&gt;
 *                         &lt;maxLength value="4"/&gt;
 *                       &lt;/restriction&gt;
 *                     &lt;/simpleType&gt;
 *                   &lt;/element&gt;
 *                 &lt;/sequence&gt;
 *               &lt;/restriction&gt;
 *             &lt;/complexContent&gt;
 *           &lt;/complexType&gt;
 *         &lt;/element&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Kysely_WS2A", propOrder = {
    "tiedostot",
    "paperi",
    "kohteet",
    "tulostustoimittaja",
    "lahetaTulostukseen",
    "laskutus"
})
@Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
public class KyselyWS2A {

    @XmlElement(name = "Tiedostot")
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    protected List<Tiedosto> tiedostot;
    @XmlElement(name = "Paperi")
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    protected boolean paperi;
    @XmlElement(name = "Kohteet")
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    protected ArrayOfKohdeWS2A kohteet;
    @XmlElement(name = "Tulostustoimittaja", required = true)
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    protected String tulostustoimittaja;
    @XmlElement(name = "LahetaTulostukseen", defaultValue = "true")
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    protected Boolean lahetaTulostukseen;
    @XmlElement(name = "Laskutus")
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    protected KyselyWS2A.Laskutus laskutus;

    /**
     * Gets the value of the tiedostot property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the tiedostot property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getTiedostot().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Tiedosto }
     * 
     * 
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public List<Tiedosto> getTiedostot() {
        if (tiedostot == null) {
            tiedostot = new ArrayList<Tiedosto>();
        }
        return this.tiedostot;
    }

    /**
     * Gets the value of the paperi property.
     * 
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public boolean isPaperi() {
        return paperi;
    }

    /**
     * Sets the value of the paperi property.
     * 
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public void setPaperi(boolean value) {
        this.paperi = value;
    }

    /**
     * Gets the value of the kohteet property.
     * 
     * @return
     *     possible object is
     *     {@link ArrayOfKohdeWS2A }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public ArrayOfKohdeWS2A getKohteet() {
        return kohteet;
    }

    /**
     * Sets the value of the kohteet property.
     * 
     * @param value
     *     allowed object is
     *     {@link ArrayOfKohdeWS2A }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public void setKohteet(ArrayOfKohdeWS2A value) {
        this.kohteet = value;
    }

    /**
     * Gets the value of the tulostustoimittaja property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public String getTulostustoimittaja() {
        return tulostustoimittaja;
    }

    /**
     * Sets the value of the tulostustoimittaja property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public void setTulostustoimittaja(String value) {
        this.tulostustoimittaja = value;
    }

    /**
     * Gets the value of the lahetaTulostukseen property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public Boolean isLahetaTulostukseen() {
        return lahetaTulostukseen;
    }

    /**
     * Sets the value of the lahetaTulostukseen property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public void setLahetaTulostukseen(Boolean value) {
        this.lahetaTulostukseen = value;
    }

    /**
     * Gets the value of the laskutus property.
     * 
     * @return
     *     possible object is
     *     {@link KyselyWS2A.Laskutus }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public KyselyWS2A.Laskutus getLaskutus() {
        return laskutus;
    }

    /**
     * Sets the value of the laskutus property.
     * 
     * @param value
     *     allowed object is
     *     {@link KyselyWS2A.Laskutus }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public void setLaskutus(KyselyWS2A.Laskutus value) {
        this.laskutus = value;
    }


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
     *         &lt;element name="Tunniste"&gt;
     *           &lt;simpleType&gt;
     *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string"&gt;
     *               &lt;minLength value="6"/&gt;
     *               &lt;maxLength value="6"/&gt;
     *             &lt;/restriction&gt;
     *           &lt;/simpleType&gt;
     *         &lt;/element&gt;
     *         &lt;element name="Salasana" minOccurs="0"&gt;
     *           &lt;simpleType&gt;
     *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string"&gt;
     *               &lt;minLength value="4"/&gt;
     *               &lt;maxLength value="4"/&gt;
     *             &lt;/restriction&gt;
     *           &lt;/simpleType&gt;
     *         &lt;/element&gt;
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
        "tunniste",
        "salasana"
    })
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
    public static class Laskutus {

        @XmlElement(name = "Tunniste", required = true)
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
        protected String tunniste;
        @XmlElement(name = "Salasana")
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
        protected String salasana;

        /**
         * Gets the value of the tunniste property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
        public String getTunniste() {
            return tunniste;
        }

        /**
         * Sets the value of the tunniste property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
        public void setTunniste(String value) {
            this.tunniste = value;
        }

        /**
         * Gets the value of the salasana property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
        public String getSalasana() {
            return salasana;
        }

        /**
         * Sets the value of the salasana property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-03-11T04:08:38+02:00", comments = "JAXB RI v2.3.1")
        public void setSalasana(String value) {
            this.salasana = value;
        }

    }

}
