// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.vtjclient.soap;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Generated;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;choice maxOccurs="unbounded" minOccurs="0"&gt;
 *         &lt;element name="Asiakasinfo" minOccurs="0"&gt;
 *           &lt;complexType&gt;
 *             &lt;complexContent&gt;
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                 &lt;sequence&gt;
 *                   &lt;element name="InfoS" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}AsiakasinfoTyyppi"/&gt;
 *                   &lt;element name="InfoR" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}AsiakasinfoTyyppi"/&gt;
 *                   &lt;element name="InfoE" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}AsiakasinfoTyyppi"/&gt;
 *                 &lt;/sequence&gt;
 *               &lt;/restriction&gt;
 *             &lt;/complexContent&gt;
 *           &lt;/complexType&gt;
 *         &lt;/element&gt;
 *         &lt;element name="Paluukoodi"&gt;
 *           &lt;complexType&gt;
 *             &lt;simpleContent&gt;
 *               &lt;extension base="&lt;http://xml.vrk.fi/schema/vtj/henkilotiedot/1&gt;PaluukoodiTekstiTyyppi"&gt;
 *                 &lt;attribute name="koodi" use="required" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaluukoodiTyyppi" /&gt;
 *               &lt;/extension&gt;
 *             &lt;/simpleContent&gt;
 *           &lt;/complexType&gt;
 *         &lt;/element&gt;
 *         &lt;element name="Hakuperusteet"&gt;
 *           &lt;complexType&gt;
 *             &lt;complexContent&gt;
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                 &lt;sequence&gt;
 *                   &lt;element name="Henkilotunnus"&gt;
 *                     &lt;complexType&gt;
 *                       &lt;simpleContent&gt;
 *                         &lt;extension base="&lt;http://xml.vrk.fi/schema/vtj/henkilotiedot/1&gt;HenkilotunnusTyyppi"&gt;
 *                           &lt;attribute name="hakuperustePaluukoodi" use="required" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}HakuperustePaluukoodiTyyppi" /&gt;
 *                           &lt;attribute name="hakuperusteTekstiS" use="required" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}HakuperustePaluuTekstiTyyppi" /&gt;
 *                           &lt;attribute name="hakuperusteTekstiR" use="required" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}HakuperustePaluuTekstiTyyppi" /&gt;
 *                           &lt;attribute name="hakuperusteTekstiE" use="required" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}HakuperustePaluuTekstiTyyppi" /&gt;
 *                         &lt;/extension&gt;
 *                       &lt;/simpleContent&gt;
 *                     &lt;/complexType&gt;
 *                   &lt;/element&gt;
 *                 &lt;/sequence&gt;
 *               &lt;/restriction&gt;
 *             &lt;/complexContent&gt;
 *           &lt;/complexType&gt;
 *         &lt;/element&gt;
 *         &lt;element name="Henkilo" minOccurs="0"&gt;
 *           &lt;complexType&gt;
 *             &lt;complexContent&gt;
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                 &lt;sequence&gt;
 *                   &lt;element name="Henkilotunnus"&gt;
 *                     &lt;complexType&gt;
 *                       &lt;simpleContent&gt;
 *                         &lt;extension base="&lt;http://xml.vrk.fi/schema/vtj/henkilotiedot/1&gt;HenkilotunnusTyyppi"&gt;
 *                           &lt;attribute name="voimassaolokoodi" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}VoimassaolokoodiTyyppi" /&gt;
 *                         &lt;/extension&gt;
 *                       &lt;/simpleContent&gt;
 *                     &lt;/complexType&gt;
 *                   &lt;/element&gt;
 *                   &lt;element name="NykyinenSukunimi"&gt;
 *                     &lt;complexType&gt;
 *                       &lt;complexContent&gt;
 *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                           &lt;sequence&gt;
 *                             &lt;element name="Sukunimi" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}Nimi100Tyyppi"/&gt;
 *                           &lt;/sequence&gt;
 *                         &lt;/restriction&gt;
 *                       &lt;/complexContent&gt;
 *                     &lt;/complexType&gt;
 *                   &lt;/element&gt;
 *                   &lt;element name="NykyisetEtunimet"&gt;
 *                     &lt;complexType&gt;
 *                       &lt;complexContent&gt;
 *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                           &lt;sequence&gt;
 *                             &lt;element name="Etunimet" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}Nimi100Tyyppi"/&gt;
 *                           &lt;/sequence&gt;
 *                         &lt;/restriction&gt;
 *                       &lt;/complexContent&gt;
 *                     &lt;/complexType&gt;
 *                   &lt;/element&gt;
 *                   &lt;element name="EntinenNimi" maxOccurs="3" minOccurs="0"&gt;
 *                     &lt;complexType&gt;
 *                       &lt;complexContent&gt;
 *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                           &lt;sequence&gt;
 *                             &lt;element name="Nimi" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}Nimi100Tyyppi"/&gt;
 *                             &lt;element name="Nimilajikoodi" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}EntinenNimiLajikoodiTyyppi"/&gt;
 *                             &lt;element name="Alkupvm" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
 *                             &lt;element name="Loppupvm" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
 *                             &lt;element name="Info8S" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}Teksti100Tyyppi" minOccurs="0"/&gt;
 *                             &lt;element name="Info8R" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}Teksti100Tyyppi" minOccurs="0"/&gt;
 *                           &lt;/sequence&gt;
 *                         &lt;/restriction&gt;
 *                       &lt;/complexContent&gt;
 *                     &lt;/complexType&gt;
 *                   &lt;/element&gt;
 *                   &lt;element name="VakinainenKotimainenLahiosoite"&gt;
 *                     &lt;complexType&gt;
 *                       &lt;complexContent&gt;
 *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                           &lt;sequence&gt;
 *                             &lt;element name="LahiosoiteS" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}KotimainenLahiosoiteTyyppi"/&gt;
 *                             &lt;element name="LahiosoiteR" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}KotimainenLahiosoiteTyyppi"/&gt;
 *                             &lt;element name="Postinumero" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PostinumeroTyyppi"/&gt;
 *                             &lt;element name="PostitoimipaikkaS" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PostitoimipaikkaTyyppi"/&gt;
 *                             &lt;element name="PostitoimipaikkaR" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PostitoimipaikkaTyyppi"/&gt;
 *                             &lt;element name="AsuminenAlkupvm" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
 *                             &lt;element name="AsuminenLoppupvm" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
 *                           &lt;/sequence&gt;
 *                         &lt;/restriction&gt;
 *                       &lt;/complexContent&gt;
 *                     &lt;/complexType&gt;
 *                   &lt;/element&gt;
 *                   &lt;element name="VakinainenUlkomainenLahiosoite" minOccurs="0"&gt;
 *                     &lt;complexType&gt;
 *                       &lt;complexContent&gt;
 *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                           &lt;sequence&gt;
 *                             &lt;element name="UlkomainenLahiosoite" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}UlkomainenLahiosoiteTyyppi"/&gt;
 *                             &lt;element name="UlkomainenPaikkakuntaJaValtioS" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}UlkomainenPaikkakuntaJaValtioTyyppi"/&gt;
 *                             &lt;element name="UlkomainenPaikkakuntaJaValtioR" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}UlkomainenPaikkakuntaJaValtioTyyppi"/&gt;
 *                             &lt;element name="UlkomainenPaikkakuntaJaValtioSelvakielinen" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}UlkomainenPaikkakuntaJaValtioTyyppi"/&gt;
 *                             &lt;element name="Valtiokoodi3" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}Valtiokoodi3Tyyppi"/&gt;
 *                             &lt;element name="AsuminenAlkupvm" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
 *                             &lt;element name="AsuminenLoppupvm" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
 *                           &lt;/sequence&gt;
 *                         &lt;/restriction&gt;
 *                       &lt;/complexContent&gt;
 *                     &lt;/complexType&gt;
 *                   &lt;/element&gt;
 *                   &lt;element name="VakinainenAsuinpaikka"&gt;
 *                     &lt;complexType&gt;
 *                       &lt;complexContent&gt;
 *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                           &lt;sequence&gt;
 *                             &lt;element name="Asuinpaikantunnus" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}AsuinpaikkatunnusTyyppi"/&gt;
 *                           &lt;/sequence&gt;
 *                         &lt;/restriction&gt;
 *                       &lt;/complexContent&gt;
 *                     &lt;/complexType&gt;
 *                   &lt;/element&gt;
 *                   &lt;element name="TilapainenKotimainenLahiosoite" maxOccurs="unbounded"&gt;
 *                     &lt;complexType&gt;
 *                       &lt;complexContent&gt;
 *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                           &lt;sequence&gt;
 *                             &lt;element name="LahiosoiteS" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}KotimainenLahiosoiteTyyppi"/&gt;
 *                             &lt;element name="LahiosoiteR" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}KotimainenLahiosoiteTyyppi"/&gt;
 *                             &lt;element name="Postinumero" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PostinumeroTyyppi"/&gt;
 *                             &lt;element name="PostitoimipaikkaS" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PostitoimipaikkaTyyppi"/&gt;
 *                             &lt;element name="PostitoimipaikkaR" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PostitoimipaikkaTyyppi"/&gt;
 *                             &lt;element name="AsuminenAlkupvm" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
 *                             &lt;element name="AsuminenLoppupvm" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
 *                           &lt;/sequence&gt;
 *                         &lt;/restriction&gt;
 *                       &lt;/complexContent&gt;
 *                     &lt;/complexType&gt;
 *                   &lt;/element&gt;
 *                   &lt;element name="TilapainenUlkomainenLahiosoite" maxOccurs="unbounded" minOccurs="0"&gt;
 *                     &lt;complexType&gt;
 *                       &lt;complexContent&gt;
 *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                           &lt;sequence&gt;
 *                             &lt;element name="UlkomainenLahiosoite" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}UlkomainenLahiosoiteTyyppi"/&gt;
 *                             &lt;element name="UlkomainenPaikkakuntaJaValtioS" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}UlkomainenPaikkakuntaJaValtioTyyppi"/&gt;
 *                             &lt;element name="UlkomainenPaikkakuntaJaValtioR" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}UlkomainenPaikkakuntaJaValtioTyyppi"/&gt;
 *                             &lt;element name="UlkomainenPaikkakuntaJaValtioSelvakielinen" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}UlkomainenPaikkakuntaJaValtioTyyppi"/&gt;
 *                             &lt;element name="Valtiokoodi3" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}Valtiokoodi3Tyyppi"/&gt;
 *                             &lt;element name="AsuminenAlkupvm" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
 *                             &lt;element name="AsuminenLoppupvm" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
 *                           &lt;/sequence&gt;
 *                         &lt;/restriction&gt;
 *                       &lt;/complexContent&gt;
 *                     &lt;/complexType&gt;
 *                   &lt;/element&gt;
 *                   &lt;element name="KotimainenPostiosoite" maxOccurs="unbounded"&gt;
 *                     &lt;complexType&gt;
 *                       &lt;complexContent&gt;
 *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                           &lt;sequence&gt;
 *                             &lt;element name="PostiosoiteS" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PostiosoiteTyyppi"/&gt;
 *                             &lt;element name="PostiosoiteR" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PostiosoiteTyyppi"/&gt;
 *                             &lt;element name="Postinumero" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PostinumeroTyyppi"/&gt;
 *                             &lt;element name="PostitoimipaikkaS" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PostitoimipaikkaTyyppi"/&gt;
 *                             &lt;element name="PostitoimipaikkaR" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PostitoimipaikkaTyyppi"/&gt;
 *                             &lt;element name="PostiosoiteAlkupvm" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
 *                             &lt;element name="PostiosoiteLoppupvm" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
 *                           &lt;/sequence&gt;
 *                         &lt;/restriction&gt;
 *                       &lt;/complexContent&gt;
 *                     &lt;/complexType&gt;
 *                   &lt;/element&gt;
 *                   &lt;element name="UlkomainenPostiosoite" maxOccurs="unbounded" minOccurs="0"&gt;
 *                     &lt;complexType&gt;
 *                       &lt;complexContent&gt;
 *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                           &lt;sequence&gt;
 *                             &lt;element name="UlkomainenLahiosoite" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}UlkomainenLahiosoiteTyyppi"/&gt;
 *                             &lt;element name="UlkomainenPaikkakunta" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}UlkomainenPaikkakuntaTyyppi"/&gt;
 *                             &lt;element name="Valtiokoodi3" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}Valtiokoodi3Tyyppi"/&gt;
 *                             &lt;element name="ValtioS" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}ValtioNimiTyyppi"/&gt;
 *                             &lt;element name="ValtioR" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}ValtioNimiTyyppi"/&gt;
 *                             &lt;element name="ValtioSelvakielinen" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}ValtioNimi30Tyyppi"/&gt;
 *                             &lt;element name="PostiosoiteAlkupvm" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
 *                             &lt;element name="PostiosoiteLoppupvm" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
 *                           &lt;/sequence&gt;
 *                         &lt;/restriction&gt;
 *                       &lt;/complexContent&gt;
 *                     &lt;/complexType&gt;
 *                   &lt;/element&gt;
 *                   &lt;element name="Kotikunta"&gt;
 *                     &lt;complexType&gt;
 *                       &lt;complexContent&gt;
 *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                           &lt;sequence&gt;
 *                             &lt;element name="Kuntanumero" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}KuntanumeroTyyppi"/&gt;
 *                             &lt;element name="KuntaS" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}KuntaNimiTyyppi"/&gt;
 *                             &lt;element name="KuntaR" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}KuntaNimiTyyppi"/&gt;
 *                             &lt;element name="KuntasuhdeAlkupvm" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
 *                           &lt;/sequence&gt;
 *                         &lt;/restriction&gt;
 *                       &lt;/complexContent&gt;
 *                     &lt;/complexType&gt;
 *                   &lt;/element&gt;
 *                   &lt;element name="Kuolintiedot"&gt;
 *                     &lt;complexType&gt;
 *                       &lt;complexContent&gt;
 *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                           &lt;sequence&gt;
 *                             &lt;element name="Kuollut" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}KuollutTyyppi"/&gt;
 *                             &lt;element name="Kuolinpvm" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
 *                           &lt;/sequence&gt;
 *                         &lt;/restriction&gt;
 *                       &lt;/complexContent&gt;
 *                     &lt;/complexType&gt;
 *                   &lt;/element&gt;
 *                   &lt;element name="Kuolleeksijulistamistiedot"&gt;
 *                     &lt;complexType&gt;
 *                       &lt;complexContent&gt;
 *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                           &lt;sequence&gt;
 *                             &lt;element name="Kuolleeksijulistamispvm" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
 *                           &lt;/sequence&gt;
 *                         &lt;/restriction&gt;
 *                       &lt;/complexContent&gt;
 *                     &lt;/complexType&gt;
 *                   &lt;/element&gt;
 *                   &lt;element name="Aidinkieli"&gt;
 *                     &lt;complexType&gt;
 *                       &lt;complexContent&gt;
 *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                           &lt;sequence&gt;
 *                             &lt;element name="Kielikoodi" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}KielikoodiTyyppi"/&gt;
 *                             &lt;element name="KieliS" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}KieliNimiTyyppi"/&gt;
 *                             &lt;element name="KieliR" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}KieliNimiTyyppi"/&gt;
 *                             &lt;element name="KieliSelvakielinen" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}KieliNimiTyyppi"/&gt;
 *                           &lt;/sequence&gt;
 *                         &lt;/restriction&gt;
 *                       &lt;/complexContent&gt;
 *                     &lt;/complexType&gt;
 *                   &lt;/element&gt;
 *                   &lt;element name="Turvakielto"&gt;
 *                     &lt;complexType&gt;
 *                       &lt;complexContent&gt;
 *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                           &lt;sequence&gt;
 *                             &lt;element name="TurvakieltoTieto" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}KieltoTietoTyyppi"/&gt;
 *                             &lt;element name="TurvakieltoPaattymispvm" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
 *                           &lt;/sequence&gt;
 *                         &lt;/restriction&gt;
 *                       &lt;/complexContent&gt;
 *                     &lt;/complexType&gt;
 *                   &lt;/element&gt;
 *                   &lt;element name="Sukupuoli"&gt;
 *                     &lt;complexType&gt;
 *                       &lt;complexContent&gt;
 *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                           &lt;sequence&gt;
 *                             &lt;element name="Sukupuolikoodi" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}SukupuolikoodiTyyppi"/&gt;
 *                             &lt;element name="SukupuoliS" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}SukupuoliTekstiTyyppi"/&gt;
 *                             &lt;element name="SukupuoliR" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}SukupuoliTekstiTyyppi"/&gt;
 *                           &lt;/sequence&gt;
 *                         &lt;/restriction&gt;
 *                       &lt;/complexContent&gt;
 *                     &lt;/complexType&gt;
 *                   &lt;/element&gt;
 *                   &lt;element name="Huoltaja" maxOccurs="unbounded" minOccurs="0"&gt;
 *                     &lt;complexType&gt;
 *                       &lt;complexContent&gt;
 *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                           &lt;sequence&gt;
 *                             &lt;element name="Henkilotunnus" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}HenkilotunnusTyyppi"/&gt;
 *                             &lt;element name="Syntymaaika" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
 *                             &lt;element name="NykyinenSukunimi"&gt;
 *                               &lt;complexType&gt;
 *                                 &lt;complexContent&gt;
 *                                   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                                     &lt;sequence&gt;
 *                                       &lt;element name="Sukunimi" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}Nimi100Tyyppi"/&gt;
 *                                     &lt;/sequence&gt;
 *                                   &lt;/restriction&gt;
 *                                 &lt;/complexContent&gt;
 *                               &lt;/complexType&gt;
 *                             &lt;/element&gt;
 *                             &lt;element name="NykyisetEtunimet"&gt;
 *                               &lt;complexType&gt;
 *                                 &lt;complexContent&gt;
 *                                   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                                     &lt;sequence&gt;
 *                                       &lt;element name="Etunimet" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}Nimi100Tyyppi"/&gt;
 *                                     &lt;/sequence&gt;
 *                                   &lt;/restriction&gt;
 *                                 &lt;/complexContent&gt;
 *                               &lt;/complexType&gt;
 *                             &lt;/element&gt;
 *                           &lt;/sequence&gt;
 *                         &lt;/restriction&gt;
 *                       &lt;/complexContent&gt;
 *                     &lt;/complexType&gt;
 *                   &lt;/element&gt;
 *                   &lt;element name="Huollettava" maxOccurs="unbounded" minOccurs="0"&gt;
 *                     &lt;complexType&gt;
 *                       &lt;complexContent&gt;
 *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                           &lt;sequence&gt;
 *                             &lt;element name="Henkilotunnus" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}HenkilotunnusTyyppi"/&gt;
 *                             &lt;element name="Syntymaaika" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
 *                             &lt;element name="NykyinenSukunimi"&gt;
 *                               &lt;complexType&gt;
 *                                 &lt;complexContent&gt;
 *                                   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                                     &lt;sequence&gt;
 *                                       &lt;element name="Sukunimi" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}Nimi100Tyyppi"/&gt;
 *                                     &lt;/sequence&gt;
 *                                   &lt;/restriction&gt;
 *                                 &lt;/complexContent&gt;
 *                               &lt;/complexType&gt;
 *                             &lt;/element&gt;
 *                             &lt;element name="NykyisetEtunimet"&gt;
 *                               &lt;complexType&gt;
 *                                 &lt;complexContent&gt;
 *                                   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                                     &lt;sequence&gt;
 *                                       &lt;element name="Etunimet" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}Nimi100Tyyppi"/&gt;
 *                                     &lt;/sequence&gt;
 *                                   &lt;/restriction&gt;
 *                                 &lt;/complexContent&gt;
 *                               &lt;/complexType&gt;
 *                             &lt;/element&gt;
 *                           &lt;/sequence&gt;
 *                         &lt;/restriction&gt;
 *                       &lt;/complexContent&gt;
 *                     &lt;/complexType&gt;
 *                   &lt;/element&gt;
 *                   &lt;element name="Lapsi" maxOccurs="unbounded" minOccurs="0"&gt;
 *                     &lt;complexType&gt;
 *                       &lt;complexContent&gt;
 *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                           &lt;sequence&gt;
 *                             &lt;element name="Henkilotunnus" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}HenkilotunnusTyyppi"/&gt;
 *                             &lt;element name="Syntymaaika" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
 *                             &lt;element name="NykyinenSukunimi"&gt;
 *                               &lt;complexType&gt;
 *                                 &lt;complexContent&gt;
 *                                   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                                     &lt;sequence&gt;
 *                                       &lt;element name="Sukunimi" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}Nimi100Tyyppi"/&gt;
 *                                     &lt;/sequence&gt;
 *                                   &lt;/restriction&gt;
 *                                 &lt;/complexContent&gt;
 *                               &lt;/complexType&gt;
 *                             &lt;/element&gt;
 *                             &lt;element name="NykyisetEtunimet"&gt;
 *                               &lt;complexType&gt;
 *                                 &lt;complexContent&gt;
 *                                   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                                     &lt;sequence&gt;
 *                                       &lt;element name="Etunimet" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}Nimi100Tyyppi"/&gt;
 *                                     &lt;/sequence&gt;
 *                                   &lt;/restriction&gt;
 *                                 &lt;/complexContent&gt;
 *                               &lt;/complexType&gt;
 *                             &lt;/element&gt;
 *                           &lt;/sequence&gt;
 *                         &lt;/restriction&gt;
 *                       &lt;/complexContent&gt;
 *                     &lt;/complexType&gt;
 *                   &lt;/element&gt;
 *                   &lt;element name="Kansalaisuus" maxOccurs="unbounded" minOccurs="0"&gt;
 *                     &lt;complexType&gt;
 *                       &lt;complexContent&gt;
 *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                           &lt;sequence&gt;
 *                             &lt;element name="Kansalaisuuskoodi3" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}Valtiokoodi3Tyyppi"/&gt;
 *                             &lt;element name="KansalaisuusS" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}ValtioNimiTyyppi"/&gt;
 *                             &lt;element name="KansalaisuusR" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}ValtioNimiTyyppi"/&gt;
 *                             &lt;element name="KansalaisuusSelvakielinen" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}ValtioNimi30Tyyppi"/&gt;
 *                             &lt;element name="Saamispvm" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
 *                           &lt;/sequence&gt;
 *                         &lt;/restriction&gt;
 *                       &lt;/complexContent&gt;
 *                     &lt;/complexType&gt;
 *                   &lt;/element&gt;
 *                   &lt;element name="AsukasLkm" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}AsukasLkmTyyppi" minOccurs="0"/&gt;
 *                   &lt;element name="AsukkaatAlle18v" minOccurs="0"&gt;
 *                     &lt;complexType&gt;
 *                       &lt;complexContent&gt;
 *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                           &lt;sequence&gt;
 *                             &lt;element name="AsukasLkm" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}AsukasLkmTyyppi"/&gt;
 *                           &lt;/sequence&gt;
 *                         &lt;/restriction&gt;
 *                       &lt;/complexContent&gt;
 *                     &lt;/complexType&gt;
 *                   &lt;/element&gt;
 *                   &lt;element name="AsukasAlle18v" maxOccurs="unbounded" minOccurs="0"&gt;
 *                     &lt;complexType&gt;
 *                       &lt;complexContent&gt;
 *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                           &lt;sequence&gt;
 *                             &lt;element name="Henkilotunnus" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}HenkilotunnusTyyppi"/&gt;
 *                           &lt;/sequence&gt;
 *                         &lt;/restriction&gt;
 *                       &lt;/complexContent&gt;
 *                     &lt;/complexType&gt;
 *                   &lt;/element&gt;
 *                   &lt;element name="Syntymamaa"&gt;
 *                     &lt;complexType&gt;
 *                       &lt;complexContent&gt;
 *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                           &lt;sequence&gt;
 *                             &lt;element name="Valtiokoodi3" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}Valtiokoodi3Tyyppi"/&gt;
 *                             &lt;element name="ValtioS" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}ValtioNimiTyyppi"/&gt;
 *                             &lt;element name="ValtioR" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}ValtioNimiTyyppi"/&gt;
 *                             &lt;element name="ValtioSelvakielinen" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}ValtioNimi30Tyyppi"/&gt;
 *                           &lt;/sequence&gt;
 *                         &lt;/restriction&gt;
 *                       &lt;/complexContent&gt;
 *                     &lt;/complexType&gt;
 *                   &lt;/element&gt;
 *                   &lt;element name="Edunvalvonta"&gt;
 *                     &lt;complexType&gt;
 *                       &lt;complexContent&gt;
 *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                           &lt;sequence&gt;
 *                             &lt;element name="Alkupvm" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
 *                             &lt;element name="Paattymispvm" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
 *                             &lt;element name="Rajoituskoodi" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}RajoituskoodiTyyppi"/&gt;
 *                             &lt;element name="RajoitustekstiS" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}Teksti100Tyyppi"/&gt;
 *                             &lt;element name="RajoitustekstiR" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}Teksti100Tyyppi"/&gt;
 *                             &lt;element name="Tehtavienjakokoodi" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}TehtavienjakokoodiTyyppi"/&gt;
 *                             &lt;element name="TehtavienjakoS" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}Teksti100Tyyppi"/&gt;
 *                             &lt;element name="TehtavienjakoR" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}Teksti100Tyyppi"/&gt;
 *                             &lt;element name="HenkiloEdunvalvoja" maxOccurs="unbounded"&gt;
 *                               &lt;complexType&gt;
 *                                 &lt;complexContent&gt;
 *                                   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                                     &lt;sequence&gt;
 *                                       &lt;element name="Henkilotunnus" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}HenkilotunnusTyyppi"/&gt;
 *                                       &lt;element name="Syntymaaika" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
 *                                       &lt;element name="NykyinenSukunimi"&gt;
 *                                         &lt;complexType&gt;
 *                                           &lt;complexContent&gt;
 *                                             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                                               &lt;sequence&gt;
 *                                                 &lt;element name="Sukunimi" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}Nimi100Tyyppi"/&gt;
 *                                               &lt;/sequence&gt;
 *                                             &lt;/restriction&gt;
 *                                           &lt;/complexContent&gt;
 *                                         &lt;/complexType&gt;
 *                                       &lt;/element&gt;
 *                                       &lt;element name="NykyisetEtunimet"&gt;
 *                                         &lt;complexType&gt;
 *                                           &lt;complexContent&gt;
 *                                             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                                               &lt;sequence&gt;
 *                                                 &lt;element name="Etunimet" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}Nimi100Tyyppi"/&gt;
 *                                               &lt;/sequence&gt;
 *                                             &lt;/restriction&gt;
 *                                           &lt;/complexContent&gt;
 *                                         &lt;/complexType&gt;
 *                                       &lt;/element&gt;
 *                                       &lt;element name="TehtavaAlkupvm" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
 *                                       &lt;element name="TehtavaLoppupvm" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
 *                                     &lt;/sequence&gt;
 *                                   &lt;/restriction&gt;
 *                                 &lt;/complexContent&gt;
 *                               &lt;/complexType&gt;
 *                             &lt;/element&gt;
 *                             &lt;element name="YritysJaYhteisoEdunvalvoja" maxOccurs="unbounded"&gt;
 *                               &lt;complexType&gt;
 *                                 &lt;complexContent&gt;
 *                                   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                                     &lt;sequence&gt;
 *                                       &lt;element name="Ytunnus" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}YtunnusTyyppi"/&gt;
 *                                       &lt;element name="Nimi" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}YritysNimi80Tyyppi"/&gt;
 *                                       &lt;element name="TehtavaLoppupvm" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
 *                                       &lt;element name="TehtavaAlkupvm" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
 *                                     &lt;/sequence&gt;
 *                                   &lt;/restriction&gt;
 *                                 &lt;/complexContent&gt;
 *                               &lt;/complexType&gt;
 *                             &lt;/element&gt;
 *                             &lt;element name="OikeusaputoimistoEdunvalvoja" maxOccurs="unbounded"&gt;
 *                               &lt;complexType&gt;
 *                                 &lt;complexContent&gt;
 *                                   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                                     &lt;sequence&gt;
 *                                       &lt;element name="Viranomaisnumero" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}ViranomaiskoodiTyyppi"/&gt;
 *                                       &lt;element name="ViranomainenS" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}ViranomainenNimiTyyppi"/&gt;
 *                                       &lt;element name="ViranomainenR" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}ViranomainenNimiTyyppi"/&gt;
 *                                       &lt;element name="TehtavaAlkupvm" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
 *                                       &lt;element name="TehtavaLoppupvm" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
 *                                     &lt;/sequence&gt;
 *                                   &lt;/restriction&gt;
 *                                 &lt;/complexContent&gt;
 *                               &lt;/complexType&gt;
 *                             &lt;/element&gt;
 *                           &lt;/sequence&gt;
 *                         &lt;/restriction&gt;
 *                       &lt;/complexContent&gt;
 *                     &lt;/complexType&gt;
 *                   &lt;/element&gt;
 *                   &lt;element name="Edunvalvontavaltuutus"&gt;
 *                     &lt;complexType&gt;
 *                       &lt;complexContent&gt;
 *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                           &lt;sequence&gt;
 *                             &lt;element name="Alkupvm" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
 *                             &lt;element name="Paattymispvm" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
 *                             &lt;element name="Tehtavienjakokoodi" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}TehtavienjakokoodiTyyppi"/&gt;
 *                             &lt;element name="TehtavienjakoS" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}Teksti100Tyyppi"/&gt;
 *                             &lt;element name="TehtavienjakoR" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}Teksti100Tyyppi"/&gt;
 *                             &lt;element name="HenkiloEdunvalvontavaltuutettu" maxOccurs="unbounded"&gt;
 *                               &lt;complexType&gt;
 *                                 &lt;complexContent&gt;
 *                                   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                                     &lt;sequence&gt;
 *                                       &lt;element name="Henkilotunnus" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}HenkilotunnusTyyppi"/&gt;
 *                                       &lt;element name="Syntymaaika" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
 *                                       &lt;element name="NykyinenSukunimi"&gt;
 *                                         &lt;complexType&gt;
 *                                           &lt;complexContent&gt;
 *                                             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                                               &lt;sequence&gt;
 *                                                 &lt;element name="Sukunimi" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}Nimi100Tyyppi"/&gt;
 *                                               &lt;/sequence&gt;
 *                                             &lt;/restriction&gt;
 *                                           &lt;/complexContent&gt;
 *                                         &lt;/complexType&gt;
 *                                       &lt;/element&gt;
 *                                       &lt;element name="NykyisetEtunimet"&gt;
 *                                         &lt;complexType&gt;
 *                                           &lt;complexContent&gt;
 *                                             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                                               &lt;sequence&gt;
 *                                                 &lt;element name="Etunimet" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}Nimi100Tyyppi"/&gt;
 *                                               &lt;/sequence&gt;
 *                                             &lt;/restriction&gt;
 *                                           &lt;/complexContent&gt;
 *                                         &lt;/complexType&gt;
 *                                       &lt;/element&gt;
 *                                       &lt;element name="ValtuutusAlkupvm" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
 *                                       &lt;element name="ValtuutusLoppupvm" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
 *                                     &lt;/sequence&gt;
 *                                   &lt;/restriction&gt;
 *                                 &lt;/complexContent&gt;
 *                               &lt;/complexType&gt;
 *                             &lt;/element&gt;
 *                           &lt;/sequence&gt;
 *                         &lt;/restriction&gt;
 *                       &lt;/complexContent&gt;
 *                     &lt;/complexType&gt;
 *                   &lt;/element&gt;
 *                 &lt;/sequence&gt;
 *               &lt;/restriction&gt;
 *             &lt;/complexContent&gt;
 *           &lt;/complexType&gt;
 *         &lt;/element&gt;
 *       &lt;/choice&gt;
 *       &lt;attribute name="tietojenPoimintaaika" use="required" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}TietojenPoimintaaikaTyyppi" /&gt;
 *       &lt;attribute name="sanomatunnus" use="required" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}SanomatunnusTyyppi" /&gt;
 *       &lt;attribute name="versio" use="required" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}VersioTyyppi" /&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "asiakasinfoOrPaluukoodiOrHakuperusteet"
})
@XmlRootElement(name = "VTJHenkiloVastaussanoma", namespace = "http://xml.vrk.fi/schema/vtjkysely")
@Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
public class VTJHenkiloVastaussanoma {

    @XmlElements({
        @XmlElement(name = "Asiakasinfo", namespace = "http://xml.vrk.fi/schema/vtjkysely", type = VTJHenkiloVastaussanoma.Asiakasinfo.class),
        @XmlElement(name = "Paluukoodi", namespace = "http://xml.vrk.fi/schema/vtjkysely", type = VTJHenkiloVastaussanoma.Paluukoodi.class),
        @XmlElement(name = "Hakuperusteet", namespace = "http://xml.vrk.fi/schema/vtjkysely", type = VTJHenkiloVastaussanoma.Hakuperusteet.class),
        @XmlElement(name = "Henkilo", namespace = "http://xml.vrk.fi/schema/vtjkysely", type = VTJHenkiloVastaussanoma.Henkilo.class)
    })
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
    protected List<Object> asiakasinfoOrPaluukoodiOrHakuperusteet;
    @XmlAttribute(name = "tietojenPoimintaaika", required = true)
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
    protected String tietojenPoimintaaika;
    @XmlAttribute(name = "sanomatunnus", required = true)
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
    protected String sanomatunnus;
    @XmlAttribute(name = "versio", required = true)
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
    protected String versio;

    /**
     * Gets the value of the asiakasinfoOrPaluukoodiOrHakuperusteet property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the asiakasinfoOrPaluukoodiOrHakuperusteet property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getAsiakasinfoOrPaluukoodiOrHakuperusteet().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link VTJHenkiloVastaussanoma.Asiakasinfo }
     * {@link VTJHenkiloVastaussanoma.Paluukoodi }
     * {@link VTJHenkiloVastaussanoma.Hakuperusteet }
     * {@link VTJHenkiloVastaussanoma.Henkilo }
     * 
     * 
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
    public List<Object> getAsiakasinfoOrPaluukoodiOrHakuperusteet() {
        if (asiakasinfoOrPaluukoodiOrHakuperusteet == null) {
            asiakasinfoOrPaluukoodiOrHakuperusteet = new ArrayList<Object>();
        }
        return this.asiakasinfoOrPaluukoodiOrHakuperusteet;
    }

    /**
     * Gets the value of the tietojenPoimintaaika property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
    public String getTietojenPoimintaaika() {
        return tietojenPoimintaaika;
    }

    /**
     * Sets the value of the tietojenPoimintaaika property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
    public void setTietojenPoimintaaika(String value) {
        this.tietojenPoimintaaika = value;
    }

    /**
     * Gets the value of the sanomatunnus property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
    public String getSanomatunnus() {
        return sanomatunnus;
    }

    /**
     * Sets the value of the sanomatunnus property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
    public void setSanomatunnus(String value) {
        this.sanomatunnus = value;
    }

    /**
     * Gets the value of the versio property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
    public String getVersio() {
        return versio;
    }

    /**
     * Sets the value of the versio property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
    public void setVersio(String value) {
        this.versio = value;
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
     *         &lt;element name="InfoS" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}AsiakasinfoTyyppi"/&gt;
     *         &lt;element name="InfoR" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}AsiakasinfoTyyppi"/&gt;
     *         &lt;element name="InfoE" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}AsiakasinfoTyyppi"/&gt;
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
        "infoS",
        "infoR",
        "infoE"
    })
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
    public static class Asiakasinfo {

        @XmlElement(name = "InfoS", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        protected String infoS;
        @XmlElement(name = "InfoR", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        protected String infoR;
        @XmlElement(name = "InfoE", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        protected String infoE;

        /**
         * Gets the value of the infoS property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        public String getInfoS() {
            return infoS;
        }

        /**
         * Sets the value of the infoS property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        public void setInfoS(String value) {
            this.infoS = value;
        }

        /**
         * Gets the value of the infoR property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        public String getInfoR() {
            return infoR;
        }

        /**
         * Sets the value of the infoR property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        public void setInfoR(String value) {
            this.infoR = value;
        }

        /**
         * Gets the value of the infoE property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        public String getInfoE() {
            return infoE;
        }

        /**
         * Sets the value of the infoE property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        public void setInfoE(String value) {
            this.infoE = value;
        }

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
     *         &lt;element name="Henkilotunnus"&gt;
     *           &lt;complexType&gt;
     *             &lt;simpleContent&gt;
     *               &lt;extension base="&lt;http://xml.vrk.fi/schema/vtj/henkilotiedot/1&gt;HenkilotunnusTyyppi"&gt;
     *                 &lt;attribute name="hakuperustePaluukoodi" use="required" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}HakuperustePaluukoodiTyyppi" /&gt;
     *                 &lt;attribute name="hakuperusteTekstiS" use="required" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}HakuperustePaluuTekstiTyyppi" /&gt;
     *                 &lt;attribute name="hakuperusteTekstiR" use="required" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}HakuperustePaluuTekstiTyyppi" /&gt;
     *                 &lt;attribute name="hakuperusteTekstiE" use="required" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}HakuperustePaluuTekstiTyyppi" /&gt;
     *               &lt;/extension&gt;
     *             &lt;/simpleContent&gt;
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
    @XmlType(name = "", propOrder = {
        "henkilotunnus"
    })
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
    public static class Hakuperusteet {

        @XmlElement(name = "Henkilotunnus", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        protected VTJHenkiloVastaussanoma.Hakuperusteet.Henkilotunnus henkilotunnus;

        /**
         * Gets the value of the henkilotunnus property.
         * 
         * @return
         *     possible object is
         *     {@link VTJHenkiloVastaussanoma.Hakuperusteet.Henkilotunnus }
         *     
         */
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        public VTJHenkiloVastaussanoma.Hakuperusteet.Henkilotunnus getHenkilotunnus() {
            return henkilotunnus;
        }

        /**
         * Sets the value of the henkilotunnus property.
         * 
         * @param value
         *     allowed object is
         *     {@link VTJHenkiloVastaussanoma.Hakuperusteet.Henkilotunnus }
         *     
         */
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        public void setHenkilotunnus(VTJHenkiloVastaussanoma.Hakuperusteet.Henkilotunnus value) {
            this.henkilotunnus = value;
        }


        /**
         * <p>Java class for anonymous complex type.
         * 
         * <p>The following schema fragment specifies the expected content contained within this class.
         * 
         * <pre>
         * &lt;complexType&gt;
         *   &lt;simpleContent&gt;
         *     &lt;extension base="&lt;http://xml.vrk.fi/schema/vtj/henkilotiedot/1&gt;HenkilotunnusTyyppi"&gt;
         *       &lt;attribute name="hakuperustePaluukoodi" use="required" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}HakuperustePaluukoodiTyyppi" /&gt;
         *       &lt;attribute name="hakuperusteTekstiS" use="required" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}HakuperustePaluuTekstiTyyppi" /&gt;
         *       &lt;attribute name="hakuperusteTekstiR" use="required" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}HakuperustePaluuTekstiTyyppi" /&gt;
         *       &lt;attribute name="hakuperusteTekstiE" use="required" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}HakuperustePaluuTekstiTyyppi" /&gt;
         *     &lt;/extension&gt;
         *   &lt;/simpleContent&gt;
         * &lt;/complexType&gt;
         * </pre>
         * 
         * 
         */
        @XmlAccessorType(XmlAccessType.FIELD)
        @XmlType(name = "", propOrder = {
            "value"
        })
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        public static class Henkilotunnus {

            @XmlValue
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected String value;
            @XmlAttribute(name = "hakuperustePaluukoodi", required = true)
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected String hakuperustePaluukoodi;
            @XmlAttribute(name = "hakuperusteTekstiS", required = true)
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected String hakuperusteTekstiS;
            @XmlAttribute(name = "hakuperusteTekstiR", required = true)
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected String hakuperusteTekstiR;
            @XmlAttribute(name = "hakuperusteTekstiE", required = true)
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected String hakuperusteTekstiE;

            /**
             * Muoto 11 merkkia, syntymaaika ppkkvv, syntymavuosisadan ilmaiseva valimerkki [- tai + tai A], yksilonumero (3 numeroa), tarkistusmerkki (ABCDEFHJKLMNPRSTUVWXY tai numero)= pp paiva arvo valilta 01-31, kk kuukausi arvo valilta 01-12, vv vuosi 2 numeroa eli 00-99, yhden kerran - tai + tai A, 3 numeroa, 1 iso kirjain joukosta ABCDEFHJKLMNPRSTUVWXY tai numero. Voi olla myos tyhja.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public String getValue() {
                return value;
            }

            /**
             * Sets the value of the value property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public void setValue(String value) {
                this.value = value;
            }

            /**
             * Gets the value of the hakuperustePaluukoodi property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public String getHakuperustePaluukoodi() {
                return hakuperustePaluukoodi;
            }

            /**
             * Sets the value of the hakuperustePaluukoodi property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public void setHakuperustePaluukoodi(String value) {
                this.hakuperustePaluukoodi = value;
            }

            /**
             * Gets the value of the hakuperusteTekstiS property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public String getHakuperusteTekstiS() {
                return hakuperusteTekstiS;
            }

            /**
             * Sets the value of the hakuperusteTekstiS property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public void setHakuperusteTekstiS(String value) {
                this.hakuperusteTekstiS = value;
            }

            /**
             * Gets the value of the hakuperusteTekstiR property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public String getHakuperusteTekstiR() {
                return hakuperusteTekstiR;
            }

            /**
             * Sets the value of the hakuperusteTekstiR property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public void setHakuperusteTekstiR(String value) {
                this.hakuperusteTekstiR = value;
            }

            /**
             * Gets the value of the hakuperusteTekstiE property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public String getHakuperusteTekstiE() {
                return hakuperusteTekstiE;
            }

            /**
             * Sets the value of the hakuperusteTekstiE property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public void setHakuperusteTekstiE(String value) {
                this.hakuperusteTekstiE = value;
            }

        }

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
     *         &lt;element name="Henkilotunnus"&gt;
     *           &lt;complexType&gt;
     *             &lt;simpleContent&gt;
     *               &lt;extension base="&lt;http://xml.vrk.fi/schema/vtj/henkilotiedot/1&gt;HenkilotunnusTyyppi"&gt;
     *                 &lt;attribute name="voimassaolokoodi" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}VoimassaolokoodiTyyppi" /&gt;
     *               &lt;/extension&gt;
     *             &lt;/simpleContent&gt;
     *           &lt;/complexType&gt;
     *         &lt;/element&gt;
     *         &lt;element name="NykyinenSukunimi"&gt;
     *           &lt;complexType&gt;
     *             &lt;complexContent&gt;
     *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
     *                 &lt;sequence&gt;
     *                   &lt;element name="Sukunimi" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}Nimi100Tyyppi"/&gt;
     *                 &lt;/sequence&gt;
     *               &lt;/restriction&gt;
     *             &lt;/complexContent&gt;
     *           &lt;/complexType&gt;
     *         &lt;/element&gt;
     *         &lt;element name="NykyisetEtunimet"&gt;
     *           &lt;complexType&gt;
     *             &lt;complexContent&gt;
     *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
     *                 &lt;sequence&gt;
     *                   &lt;element name="Etunimet" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}Nimi100Tyyppi"/&gt;
     *                 &lt;/sequence&gt;
     *               &lt;/restriction&gt;
     *             &lt;/complexContent&gt;
     *           &lt;/complexType&gt;
     *         &lt;/element&gt;
     *         &lt;element name="EntinenNimi" maxOccurs="3" minOccurs="0"&gt;
     *           &lt;complexType&gt;
     *             &lt;complexContent&gt;
     *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
     *                 &lt;sequence&gt;
     *                   &lt;element name="Nimi" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}Nimi100Tyyppi"/&gt;
     *                   &lt;element name="Nimilajikoodi" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}EntinenNimiLajikoodiTyyppi"/&gt;
     *                   &lt;element name="Alkupvm" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
     *                   &lt;element name="Loppupvm" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
     *                   &lt;element name="Info8S" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}Teksti100Tyyppi" minOccurs="0"/&gt;
     *                   &lt;element name="Info8R" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}Teksti100Tyyppi" minOccurs="0"/&gt;
     *                 &lt;/sequence&gt;
     *               &lt;/restriction&gt;
     *             &lt;/complexContent&gt;
     *           &lt;/complexType&gt;
     *         &lt;/element&gt;
     *         &lt;element name="VakinainenKotimainenLahiosoite"&gt;
     *           &lt;complexType&gt;
     *             &lt;complexContent&gt;
     *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
     *                 &lt;sequence&gt;
     *                   &lt;element name="LahiosoiteS" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}KotimainenLahiosoiteTyyppi"/&gt;
     *                   &lt;element name="LahiosoiteR" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}KotimainenLahiosoiteTyyppi"/&gt;
     *                   &lt;element name="Postinumero" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PostinumeroTyyppi"/&gt;
     *                   &lt;element name="PostitoimipaikkaS" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PostitoimipaikkaTyyppi"/&gt;
     *                   &lt;element name="PostitoimipaikkaR" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PostitoimipaikkaTyyppi"/&gt;
     *                   &lt;element name="AsuminenAlkupvm" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
     *                   &lt;element name="AsuminenLoppupvm" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
     *                 &lt;/sequence&gt;
     *               &lt;/restriction&gt;
     *             &lt;/complexContent&gt;
     *           &lt;/complexType&gt;
     *         &lt;/element&gt;
     *         &lt;element name="VakinainenUlkomainenLahiosoite" minOccurs="0"&gt;
     *           &lt;complexType&gt;
     *             &lt;complexContent&gt;
     *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
     *                 &lt;sequence&gt;
     *                   &lt;element name="UlkomainenLahiosoite" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}UlkomainenLahiosoiteTyyppi"/&gt;
     *                   &lt;element name="UlkomainenPaikkakuntaJaValtioS" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}UlkomainenPaikkakuntaJaValtioTyyppi"/&gt;
     *                   &lt;element name="UlkomainenPaikkakuntaJaValtioR" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}UlkomainenPaikkakuntaJaValtioTyyppi"/&gt;
     *                   &lt;element name="UlkomainenPaikkakuntaJaValtioSelvakielinen" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}UlkomainenPaikkakuntaJaValtioTyyppi"/&gt;
     *                   &lt;element name="Valtiokoodi3" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}Valtiokoodi3Tyyppi"/&gt;
     *                   &lt;element name="AsuminenAlkupvm" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
     *                   &lt;element name="AsuminenLoppupvm" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
     *                 &lt;/sequence&gt;
     *               &lt;/restriction&gt;
     *             &lt;/complexContent&gt;
     *           &lt;/complexType&gt;
     *         &lt;/element&gt;
     *         &lt;element name="VakinainenAsuinpaikka"&gt;
     *           &lt;complexType&gt;
     *             &lt;complexContent&gt;
     *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
     *                 &lt;sequence&gt;
     *                   &lt;element name="Asuinpaikantunnus" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}AsuinpaikkatunnusTyyppi"/&gt;
     *                 &lt;/sequence&gt;
     *               &lt;/restriction&gt;
     *             &lt;/complexContent&gt;
     *           &lt;/complexType&gt;
     *         &lt;/element&gt;
     *         &lt;element name="TilapainenKotimainenLahiosoite" maxOccurs="unbounded"&gt;
     *           &lt;complexType&gt;
     *             &lt;complexContent&gt;
     *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
     *                 &lt;sequence&gt;
     *                   &lt;element name="LahiosoiteS" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}KotimainenLahiosoiteTyyppi"/&gt;
     *                   &lt;element name="LahiosoiteR" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}KotimainenLahiosoiteTyyppi"/&gt;
     *                   &lt;element name="Postinumero" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PostinumeroTyyppi"/&gt;
     *                   &lt;element name="PostitoimipaikkaS" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PostitoimipaikkaTyyppi"/&gt;
     *                   &lt;element name="PostitoimipaikkaR" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PostitoimipaikkaTyyppi"/&gt;
     *                   &lt;element name="AsuminenAlkupvm" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
     *                   &lt;element name="AsuminenLoppupvm" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
     *                 &lt;/sequence&gt;
     *               &lt;/restriction&gt;
     *             &lt;/complexContent&gt;
     *           &lt;/complexType&gt;
     *         &lt;/element&gt;
     *         &lt;element name="TilapainenUlkomainenLahiosoite" maxOccurs="unbounded" minOccurs="0"&gt;
     *           &lt;complexType&gt;
     *             &lt;complexContent&gt;
     *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
     *                 &lt;sequence&gt;
     *                   &lt;element name="UlkomainenLahiosoite" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}UlkomainenLahiosoiteTyyppi"/&gt;
     *                   &lt;element name="UlkomainenPaikkakuntaJaValtioS" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}UlkomainenPaikkakuntaJaValtioTyyppi"/&gt;
     *                   &lt;element name="UlkomainenPaikkakuntaJaValtioR" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}UlkomainenPaikkakuntaJaValtioTyyppi"/&gt;
     *                   &lt;element name="UlkomainenPaikkakuntaJaValtioSelvakielinen" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}UlkomainenPaikkakuntaJaValtioTyyppi"/&gt;
     *                   &lt;element name="Valtiokoodi3" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}Valtiokoodi3Tyyppi"/&gt;
     *                   &lt;element name="AsuminenAlkupvm" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
     *                   &lt;element name="AsuminenLoppupvm" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
     *                 &lt;/sequence&gt;
     *               &lt;/restriction&gt;
     *             &lt;/complexContent&gt;
     *           &lt;/complexType&gt;
     *         &lt;/element&gt;
     *         &lt;element name="KotimainenPostiosoite" maxOccurs="unbounded"&gt;
     *           &lt;complexType&gt;
     *             &lt;complexContent&gt;
     *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
     *                 &lt;sequence&gt;
     *                   &lt;element name="PostiosoiteS" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PostiosoiteTyyppi"/&gt;
     *                   &lt;element name="PostiosoiteR" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PostiosoiteTyyppi"/&gt;
     *                   &lt;element name="Postinumero" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PostinumeroTyyppi"/&gt;
     *                   &lt;element name="PostitoimipaikkaS" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PostitoimipaikkaTyyppi"/&gt;
     *                   &lt;element name="PostitoimipaikkaR" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PostitoimipaikkaTyyppi"/&gt;
     *                   &lt;element name="PostiosoiteAlkupvm" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
     *                   &lt;element name="PostiosoiteLoppupvm" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
     *                 &lt;/sequence&gt;
     *               &lt;/restriction&gt;
     *             &lt;/complexContent&gt;
     *           &lt;/complexType&gt;
     *         &lt;/element&gt;
     *         &lt;element name="UlkomainenPostiosoite" maxOccurs="unbounded" minOccurs="0"&gt;
     *           &lt;complexType&gt;
     *             &lt;complexContent&gt;
     *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
     *                 &lt;sequence&gt;
     *                   &lt;element name="UlkomainenLahiosoite" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}UlkomainenLahiosoiteTyyppi"/&gt;
     *                   &lt;element name="UlkomainenPaikkakunta" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}UlkomainenPaikkakuntaTyyppi"/&gt;
     *                   &lt;element name="Valtiokoodi3" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}Valtiokoodi3Tyyppi"/&gt;
     *                   &lt;element name="ValtioS" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}ValtioNimiTyyppi"/&gt;
     *                   &lt;element name="ValtioR" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}ValtioNimiTyyppi"/&gt;
     *                   &lt;element name="ValtioSelvakielinen" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}ValtioNimi30Tyyppi"/&gt;
     *                   &lt;element name="PostiosoiteAlkupvm" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
     *                   &lt;element name="PostiosoiteLoppupvm" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
     *                 &lt;/sequence&gt;
     *               &lt;/restriction&gt;
     *             &lt;/complexContent&gt;
     *           &lt;/complexType&gt;
     *         &lt;/element&gt;
     *         &lt;element name="Kotikunta"&gt;
     *           &lt;complexType&gt;
     *             &lt;complexContent&gt;
     *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
     *                 &lt;sequence&gt;
     *                   &lt;element name="Kuntanumero" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}KuntanumeroTyyppi"/&gt;
     *                   &lt;element name="KuntaS" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}KuntaNimiTyyppi"/&gt;
     *                   &lt;element name="KuntaR" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}KuntaNimiTyyppi"/&gt;
     *                   &lt;element name="KuntasuhdeAlkupvm" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
     *                 &lt;/sequence&gt;
     *               &lt;/restriction&gt;
     *             &lt;/complexContent&gt;
     *           &lt;/complexType&gt;
     *         &lt;/element&gt;
     *         &lt;element name="Kuolintiedot"&gt;
     *           &lt;complexType&gt;
     *             &lt;complexContent&gt;
     *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
     *                 &lt;sequence&gt;
     *                   &lt;element name="Kuollut" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}KuollutTyyppi"/&gt;
     *                   &lt;element name="Kuolinpvm" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
     *                 &lt;/sequence&gt;
     *               &lt;/restriction&gt;
     *             &lt;/complexContent&gt;
     *           &lt;/complexType&gt;
     *         &lt;/element&gt;
     *         &lt;element name="Kuolleeksijulistamistiedot"&gt;
     *           &lt;complexType&gt;
     *             &lt;complexContent&gt;
     *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
     *                 &lt;sequence&gt;
     *                   &lt;element name="Kuolleeksijulistamispvm" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
     *                 &lt;/sequence&gt;
     *               &lt;/restriction&gt;
     *             &lt;/complexContent&gt;
     *           &lt;/complexType&gt;
     *         &lt;/element&gt;
     *         &lt;element name="Aidinkieli"&gt;
     *           &lt;complexType&gt;
     *             &lt;complexContent&gt;
     *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
     *                 &lt;sequence&gt;
     *                   &lt;element name="Kielikoodi" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}KielikoodiTyyppi"/&gt;
     *                   &lt;element name="KieliS" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}KieliNimiTyyppi"/&gt;
     *                   &lt;element name="KieliR" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}KieliNimiTyyppi"/&gt;
     *                   &lt;element name="KieliSelvakielinen" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}KieliNimiTyyppi"/&gt;
     *                 &lt;/sequence&gt;
     *               &lt;/restriction&gt;
     *             &lt;/complexContent&gt;
     *           &lt;/complexType&gt;
     *         &lt;/element&gt;
     *         &lt;element name="Turvakielto"&gt;
     *           &lt;complexType&gt;
     *             &lt;complexContent&gt;
     *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
     *                 &lt;sequence&gt;
     *                   &lt;element name="TurvakieltoTieto" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}KieltoTietoTyyppi"/&gt;
     *                   &lt;element name="TurvakieltoPaattymispvm" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
     *                 &lt;/sequence&gt;
     *               &lt;/restriction&gt;
     *             &lt;/complexContent&gt;
     *           &lt;/complexType&gt;
     *         &lt;/element&gt;
     *         &lt;element name="Sukupuoli"&gt;
     *           &lt;complexType&gt;
     *             &lt;complexContent&gt;
     *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
     *                 &lt;sequence&gt;
     *                   &lt;element name="Sukupuolikoodi" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}SukupuolikoodiTyyppi"/&gt;
     *                   &lt;element name="SukupuoliS" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}SukupuoliTekstiTyyppi"/&gt;
     *                   &lt;element name="SukupuoliR" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}SukupuoliTekstiTyyppi"/&gt;
     *                 &lt;/sequence&gt;
     *               &lt;/restriction&gt;
     *             &lt;/complexContent&gt;
     *           &lt;/complexType&gt;
     *         &lt;/element&gt;
     *         &lt;element name="Huoltaja" maxOccurs="unbounded" minOccurs="0"&gt;
     *           &lt;complexType&gt;
     *             &lt;complexContent&gt;
     *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
     *                 &lt;sequence&gt;
     *                   &lt;element name="Henkilotunnus" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}HenkilotunnusTyyppi"/&gt;
     *                   &lt;element name="Syntymaaika" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
     *                   &lt;element name="NykyinenSukunimi"&gt;
     *                     &lt;complexType&gt;
     *                       &lt;complexContent&gt;
     *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
     *                           &lt;sequence&gt;
     *                             &lt;element name="Sukunimi" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}Nimi100Tyyppi"/&gt;
     *                           &lt;/sequence&gt;
     *                         &lt;/restriction&gt;
     *                       &lt;/complexContent&gt;
     *                     &lt;/complexType&gt;
     *                   &lt;/element&gt;
     *                   &lt;element name="NykyisetEtunimet"&gt;
     *                     &lt;complexType&gt;
     *                       &lt;complexContent&gt;
     *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
     *                           &lt;sequence&gt;
     *                             &lt;element name="Etunimet" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}Nimi100Tyyppi"/&gt;
     *                           &lt;/sequence&gt;
     *                         &lt;/restriction&gt;
     *                       &lt;/complexContent&gt;
     *                     &lt;/complexType&gt;
     *                   &lt;/element&gt;
     *                 &lt;/sequence&gt;
     *               &lt;/restriction&gt;
     *             &lt;/complexContent&gt;
     *           &lt;/complexType&gt;
     *         &lt;/element&gt;
     *         &lt;element name="Huollettava" maxOccurs="unbounded" minOccurs="0"&gt;
     *           &lt;complexType&gt;
     *             &lt;complexContent&gt;
     *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
     *                 &lt;sequence&gt;
     *                   &lt;element name="Henkilotunnus" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}HenkilotunnusTyyppi"/&gt;
     *                   &lt;element name="Syntymaaika" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
     *                   &lt;element name="NykyinenSukunimi"&gt;
     *                     &lt;complexType&gt;
     *                       &lt;complexContent&gt;
     *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
     *                           &lt;sequence&gt;
     *                             &lt;element name="Sukunimi" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}Nimi100Tyyppi"/&gt;
     *                           &lt;/sequence&gt;
     *                         &lt;/restriction&gt;
     *                       &lt;/complexContent&gt;
     *                     &lt;/complexType&gt;
     *                   &lt;/element&gt;
     *                   &lt;element name="NykyisetEtunimet"&gt;
     *                     &lt;complexType&gt;
     *                       &lt;complexContent&gt;
     *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
     *                           &lt;sequence&gt;
     *                             &lt;element name="Etunimet" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}Nimi100Tyyppi"/&gt;
     *                           &lt;/sequence&gt;
     *                         &lt;/restriction&gt;
     *                       &lt;/complexContent&gt;
     *                     &lt;/complexType&gt;
     *                   &lt;/element&gt;
     *                 &lt;/sequence&gt;
     *               &lt;/restriction&gt;
     *             &lt;/complexContent&gt;
     *           &lt;/complexType&gt;
     *         &lt;/element&gt;
     *         &lt;element name="Lapsi" maxOccurs="unbounded" minOccurs="0"&gt;
     *           &lt;complexType&gt;
     *             &lt;complexContent&gt;
     *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
     *                 &lt;sequence&gt;
     *                   &lt;element name="Henkilotunnus" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}HenkilotunnusTyyppi"/&gt;
     *                   &lt;element name="Syntymaaika" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
     *                   &lt;element name="NykyinenSukunimi"&gt;
     *                     &lt;complexType&gt;
     *                       &lt;complexContent&gt;
     *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
     *                           &lt;sequence&gt;
     *                             &lt;element name="Sukunimi" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}Nimi100Tyyppi"/&gt;
     *                           &lt;/sequence&gt;
     *                         &lt;/restriction&gt;
     *                       &lt;/complexContent&gt;
     *                     &lt;/complexType&gt;
     *                   &lt;/element&gt;
     *                   &lt;element name="NykyisetEtunimet"&gt;
     *                     &lt;complexType&gt;
     *                       &lt;complexContent&gt;
     *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
     *                           &lt;sequence&gt;
     *                             &lt;element name="Etunimet" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}Nimi100Tyyppi"/&gt;
     *                           &lt;/sequence&gt;
     *                         &lt;/restriction&gt;
     *                       &lt;/complexContent&gt;
     *                     &lt;/complexType&gt;
     *                   &lt;/element&gt;
     *                 &lt;/sequence&gt;
     *               &lt;/restriction&gt;
     *             &lt;/complexContent&gt;
     *           &lt;/complexType&gt;
     *         &lt;/element&gt;
     *         &lt;element name="Kansalaisuus" maxOccurs="unbounded" minOccurs="0"&gt;
     *           &lt;complexType&gt;
     *             &lt;complexContent&gt;
     *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
     *                 &lt;sequence&gt;
     *                   &lt;element name="Kansalaisuuskoodi3" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}Valtiokoodi3Tyyppi"/&gt;
     *                   &lt;element name="KansalaisuusS" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}ValtioNimiTyyppi"/&gt;
     *                   &lt;element name="KansalaisuusR" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}ValtioNimiTyyppi"/&gt;
     *                   &lt;element name="KansalaisuusSelvakielinen" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}ValtioNimi30Tyyppi"/&gt;
     *                   &lt;element name="Saamispvm" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
     *                 &lt;/sequence&gt;
     *               &lt;/restriction&gt;
     *             &lt;/complexContent&gt;
     *           &lt;/complexType&gt;
     *         &lt;/element&gt;
     *         &lt;element name="AsukasLkm" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}AsukasLkmTyyppi" minOccurs="0"/&gt;
     *         &lt;element name="AsukkaatAlle18v" minOccurs="0"&gt;
     *           &lt;complexType&gt;
     *             &lt;complexContent&gt;
     *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
     *                 &lt;sequence&gt;
     *                   &lt;element name="AsukasLkm" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}AsukasLkmTyyppi"/&gt;
     *                 &lt;/sequence&gt;
     *               &lt;/restriction&gt;
     *             &lt;/complexContent&gt;
     *           &lt;/complexType&gt;
     *         &lt;/element&gt;
     *         &lt;element name="AsukasAlle18v" maxOccurs="unbounded" minOccurs="0"&gt;
     *           &lt;complexType&gt;
     *             &lt;complexContent&gt;
     *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
     *                 &lt;sequence&gt;
     *                   &lt;element name="Henkilotunnus" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}HenkilotunnusTyyppi"/&gt;
     *                 &lt;/sequence&gt;
     *               &lt;/restriction&gt;
     *             &lt;/complexContent&gt;
     *           &lt;/complexType&gt;
     *         &lt;/element&gt;
     *         &lt;element name="Syntymamaa"&gt;
     *           &lt;complexType&gt;
     *             &lt;complexContent&gt;
     *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
     *                 &lt;sequence&gt;
     *                   &lt;element name="Valtiokoodi3" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}Valtiokoodi3Tyyppi"/&gt;
     *                   &lt;element name="ValtioS" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}ValtioNimiTyyppi"/&gt;
     *                   &lt;element name="ValtioR" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}ValtioNimiTyyppi"/&gt;
     *                   &lt;element name="ValtioSelvakielinen" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}ValtioNimi30Tyyppi"/&gt;
     *                 &lt;/sequence&gt;
     *               &lt;/restriction&gt;
     *             &lt;/complexContent&gt;
     *           &lt;/complexType&gt;
     *         &lt;/element&gt;
     *         &lt;element name="Edunvalvonta"&gt;
     *           &lt;complexType&gt;
     *             &lt;complexContent&gt;
     *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
     *                 &lt;sequence&gt;
     *                   &lt;element name="Alkupvm" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
     *                   &lt;element name="Paattymispvm" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
     *                   &lt;element name="Rajoituskoodi" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}RajoituskoodiTyyppi"/&gt;
     *                   &lt;element name="RajoitustekstiS" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}Teksti100Tyyppi"/&gt;
     *                   &lt;element name="RajoitustekstiR" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}Teksti100Tyyppi"/&gt;
     *                   &lt;element name="Tehtavienjakokoodi" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}TehtavienjakokoodiTyyppi"/&gt;
     *                   &lt;element name="TehtavienjakoS" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}Teksti100Tyyppi"/&gt;
     *                   &lt;element name="TehtavienjakoR" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}Teksti100Tyyppi"/&gt;
     *                   &lt;element name="HenkiloEdunvalvoja" maxOccurs="unbounded"&gt;
     *                     &lt;complexType&gt;
     *                       &lt;complexContent&gt;
     *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
     *                           &lt;sequence&gt;
     *                             &lt;element name="Henkilotunnus" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}HenkilotunnusTyyppi"/&gt;
     *                             &lt;element name="Syntymaaika" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
     *                             &lt;element name="NykyinenSukunimi"&gt;
     *                               &lt;complexType&gt;
     *                                 &lt;complexContent&gt;
     *                                   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
     *                                     &lt;sequence&gt;
     *                                       &lt;element name="Sukunimi" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}Nimi100Tyyppi"/&gt;
     *                                     &lt;/sequence&gt;
     *                                   &lt;/restriction&gt;
     *                                 &lt;/complexContent&gt;
     *                               &lt;/complexType&gt;
     *                             &lt;/element&gt;
     *                             &lt;element name="NykyisetEtunimet"&gt;
     *                               &lt;complexType&gt;
     *                                 &lt;complexContent&gt;
     *                                   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
     *                                     &lt;sequence&gt;
     *                                       &lt;element name="Etunimet" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}Nimi100Tyyppi"/&gt;
     *                                     &lt;/sequence&gt;
     *                                   &lt;/restriction&gt;
     *                                 &lt;/complexContent&gt;
     *                               &lt;/complexType&gt;
     *                             &lt;/element&gt;
     *                             &lt;element name="TehtavaAlkupvm" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
     *                             &lt;element name="TehtavaLoppupvm" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
     *                           &lt;/sequence&gt;
     *                         &lt;/restriction&gt;
     *                       &lt;/complexContent&gt;
     *                     &lt;/complexType&gt;
     *                   &lt;/element&gt;
     *                   &lt;element name="YritysJaYhteisoEdunvalvoja" maxOccurs="unbounded"&gt;
     *                     &lt;complexType&gt;
     *                       &lt;complexContent&gt;
     *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
     *                           &lt;sequence&gt;
     *                             &lt;element name="Ytunnus" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}YtunnusTyyppi"/&gt;
     *                             &lt;element name="Nimi" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}YritysNimi80Tyyppi"/&gt;
     *                             &lt;element name="TehtavaLoppupvm" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
     *                             &lt;element name="TehtavaAlkupvm" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
     *                           &lt;/sequence&gt;
     *                         &lt;/restriction&gt;
     *                       &lt;/complexContent&gt;
     *                     &lt;/complexType&gt;
     *                   &lt;/element&gt;
     *                   &lt;element name="OikeusaputoimistoEdunvalvoja" maxOccurs="unbounded"&gt;
     *                     &lt;complexType&gt;
     *                       &lt;complexContent&gt;
     *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
     *                           &lt;sequence&gt;
     *                             &lt;element name="Viranomaisnumero" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}ViranomaiskoodiTyyppi"/&gt;
     *                             &lt;element name="ViranomainenS" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}ViranomainenNimiTyyppi"/&gt;
     *                             &lt;element name="ViranomainenR" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}ViranomainenNimiTyyppi"/&gt;
     *                             &lt;element name="TehtavaAlkupvm" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
     *                             &lt;element name="TehtavaLoppupvm" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
     *                           &lt;/sequence&gt;
     *                         &lt;/restriction&gt;
     *                       &lt;/complexContent&gt;
     *                     &lt;/complexType&gt;
     *                   &lt;/element&gt;
     *                 &lt;/sequence&gt;
     *               &lt;/restriction&gt;
     *             &lt;/complexContent&gt;
     *           &lt;/complexType&gt;
     *         &lt;/element&gt;
     *         &lt;element name="Edunvalvontavaltuutus"&gt;
     *           &lt;complexType&gt;
     *             &lt;complexContent&gt;
     *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
     *                 &lt;sequence&gt;
     *                   &lt;element name="Alkupvm" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
     *                   &lt;element name="Paattymispvm" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
     *                   &lt;element name="Tehtavienjakokoodi" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}TehtavienjakokoodiTyyppi"/&gt;
     *                   &lt;element name="TehtavienjakoS" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}Teksti100Tyyppi"/&gt;
     *                   &lt;element name="TehtavienjakoR" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}Teksti100Tyyppi"/&gt;
     *                   &lt;element name="HenkiloEdunvalvontavaltuutettu" maxOccurs="unbounded"&gt;
     *                     &lt;complexType&gt;
     *                       &lt;complexContent&gt;
     *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
     *                           &lt;sequence&gt;
     *                             &lt;element name="Henkilotunnus" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}HenkilotunnusTyyppi"/&gt;
     *                             &lt;element name="Syntymaaika" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
     *                             &lt;element name="NykyinenSukunimi"&gt;
     *                               &lt;complexType&gt;
     *                                 &lt;complexContent&gt;
     *                                   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
     *                                     &lt;sequence&gt;
     *                                       &lt;element name="Sukunimi" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}Nimi100Tyyppi"/&gt;
     *                                     &lt;/sequence&gt;
     *                                   &lt;/restriction&gt;
     *                                 &lt;/complexContent&gt;
     *                               &lt;/complexType&gt;
     *                             &lt;/element&gt;
     *                             &lt;element name="NykyisetEtunimet"&gt;
     *                               &lt;complexType&gt;
     *                                 &lt;complexContent&gt;
     *                                   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
     *                                     &lt;sequence&gt;
     *                                       &lt;element name="Etunimet" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}Nimi100Tyyppi"/&gt;
     *                                     &lt;/sequence&gt;
     *                                   &lt;/restriction&gt;
     *                                 &lt;/complexContent&gt;
     *                               &lt;/complexType&gt;
     *                             &lt;/element&gt;
     *                             &lt;element name="ValtuutusAlkupvm" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
     *                             &lt;element name="ValtuutusLoppupvm" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
     *                           &lt;/sequence&gt;
     *                         &lt;/restriction&gt;
     *                       &lt;/complexContent&gt;
     *                     &lt;/complexType&gt;
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
    @XmlType(name = "", propOrder = {
        "henkilotunnus",
        "nykyinenSukunimi",
        "nykyisetEtunimet",
        "entinenNimi",
        "vakinainenKotimainenLahiosoite",
        "vakinainenUlkomainenLahiosoite",
        "vakinainenAsuinpaikka",
        "tilapainenKotimainenLahiosoite",
        "tilapainenUlkomainenLahiosoite",
        "kotimainenPostiosoite",
        "ulkomainenPostiosoite",
        "kotikunta",
        "kuolintiedot",
        "kuolleeksijulistamistiedot",
        "aidinkieli",
        "turvakielto",
        "sukupuoli",
        "huoltaja",
        "huollettava",
        "lapsi",
        "kansalaisuus",
        "asukasLkm",
        "asukkaatAlle18V",
        "asukasAlle18V",
        "syntymamaa",
        "edunvalvonta",
        "edunvalvontavaltuutus"
    })
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
    public static class Henkilo {

        @XmlElement(name = "Henkilotunnus", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        protected VTJHenkiloVastaussanoma.Henkilo.Henkilotunnus henkilotunnus;
        @XmlElement(name = "NykyinenSukunimi", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        protected VTJHenkiloVastaussanoma.Henkilo.NykyinenSukunimi nykyinenSukunimi;
        @XmlElement(name = "NykyisetEtunimet", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        protected VTJHenkiloVastaussanoma.Henkilo.NykyisetEtunimet nykyisetEtunimet;
        @XmlElement(name = "EntinenNimi", namespace = "http://xml.vrk.fi/schema/vtjkysely")
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        protected List<VTJHenkiloVastaussanoma.Henkilo.EntinenNimi> entinenNimi;
        @XmlElement(name = "VakinainenKotimainenLahiosoite", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        protected VTJHenkiloVastaussanoma.Henkilo.VakinainenKotimainenLahiosoite vakinainenKotimainenLahiosoite;
        @XmlElement(name = "VakinainenUlkomainenLahiosoite", namespace = "http://xml.vrk.fi/schema/vtjkysely")
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        protected VTJHenkiloVastaussanoma.Henkilo.VakinainenUlkomainenLahiosoite vakinainenUlkomainenLahiosoite;
        @XmlElement(name = "VakinainenAsuinpaikka", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        protected VTJHenkiloVastaussanoma.Henkilo.VakinainenAsuinpaikka vakinainenAsuinpaikka;
        @XmlElement(name = "TilapainenKotimainenLahiosoite", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        protected List<VTJHenkiloVastaussanoma.Henkilo.TilapainenKotimainenLahiosoite> tilapainenKotimainenLahiosoite;
        @XmlElement(name = "TilapainenUlkomainenLahiosoite", namespace = "http://xml.vrk.fi/schema/vtjkysely")
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        protected List<VTJHenkiloVastaussanoma.Henkilo.TilapainenUlkomainenLahiosoite> tilapainenUlkomainenLahiosoite;
        @XmlElement(name = "KotimainenPostiosoite", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        protected List<VTJHenkiloVastaussanoma.Henkilo.KotimainenPostiosoite> kotimainenPostiosoite;
        @XmlElement(name = "UlkomainenPostiosoite", namespace = "http://xml.vrk.fi/schema/vtjkysely")
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        protected List<VTJHenkiloVastaussanoma.Henkilo.UlkomainenPostiosoite> ulkomainenPostiosoite;
        @XmlElement(name = "Kotikunta", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        protected VTJHenkiloVastaussanoma.Henkilo.Kotikunta kotikunta;
        @XmlElement(name = "Kuolintiedot", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        protected VTJHenkiloVastaussanoma.Henkilo.Kuolintiedot kuolintiedot;
        @XmlElement(name = "Kuolleeksijulistamistiedot", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        protected VTJHenkiloVastaussanoma.Henkilo.Kuolleeksijulistamistiedot kuolleeksijulistamistiedot;
        @XmlElement(name = "Aidinkieli", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        protected VTJHenkiloVastaussanoma.Henkilo.Aidinkieli aidinkieli;
        @XmlElement(name = "Turvakielto", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        protected VTJHenkiloVastaussanoma.Henkilo.Turvakielto turvakielto;
        @XmlElement(name = "Sukupuoli", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        protected VTJHenkiloVastaussanoma.Henkilo.Sukupuoli sukupuoli;
        @XmlElement(name = "Huoltaja", namespace = "http://xml.vrk.fi/schema/vtjkysely")
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        protected List<VTJHenkiloVastaussanoma.Henkilo.Huoltaja> huoltaja;
        @XmlElement(name = "Huollettava", namespace = "http://xml.vrk.fi/schema/vtjkysely")
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        protected List<VTJHenkiloVastaussanoma.Henkilo.Huollettava> huollettava;
        @XmlElement(name = "Lapsi", namespace = "http://xml.vrk.fi/schema/vtjkysely")
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        protected List<VTJHenkiloVastaussanoma.Henkilo.Lapsi> lapsi;
        @XmlElement(name = "Kansalaisuus", namespace = "http://xml.vrk.fi/schema/vtjkysely")
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        protected List<VTJHenkiloVastaussanoma.Henkilo.Kansalaisuus> kansalaisuus;
        @XmlElement(name = "AsukasLkm", namespace = "http://xml.vrk.fi/schema/vtjkysely")
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        protected String asukasLkm;
        @XmlElement(name = "AsukkaatAlle18v", namespace = "http://xml.vrk.fi/schema/vtjkysely")
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        protected VTJHenkiloVastaussanoma.Henkilo.AsukkaatAlle18V asukkaatAlle18V;
        @XmlElement(name = "AsukasAlle18v", namespace = "http://xml.vrk.fi/schema/vtjkysely")
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        protected List<VTJHenkiloVastaussanoma.Henkilo.AsukasAlle18V> asukasAlle18V;
        @XmlElement(name = "Syntymamaa", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        protected VTJHenkiloVastaussanoma.Henkilo.Syntymamaa syntymamaa;
        @XmlElement(name = "Edunvalvonta", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        protected VTJHenkiloVastaussanoma.Henkilo.Edunvalvonta edunvalvonta;
        @XmlElement(name = "Edunvalvontavaltuutus", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        protected VTJHenkiloVastaussanoma.Henkilo.Edunvalvontavaltuutus edunvalvontavaltuutus;

        /**
         * Gets the value of the henkilotunnus property.
         * 
         * @return
         *     possible object is
         *     {@link VTJHenkiloVastaussanoma.Henkilo.Henkilotunnus }
         *     
         */
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        public VTJHenkiloVastaussanoma.Henkilo.Henkilotunnus getHenkilotunnus() {
            return henkilotunnus;
        }

        /**
         * Sets the value of the henkilotunnus property.
         * 
         * @param value
         *     allowed object is
         *     {@link VTJHenkiloVastaussanoma.Henkilo.Henkilotunnus }
         *     
         */
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        public void setHenkilotunnus(VTJHenkiloVastaussanoma.Henkilo.Henkilotunnus value) {
            this.henkilotunnus = value;
        }

        /**
         * Gets the value of the nykyinenSukunimi property.
         * 
         * @return
         *     possible object is
         *     {@link VTJHenkiloVastaussanoma.Henkilo.NykyinenSukunimi }
         *     
         */
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        public VTJHenkiloVastaussanoma.Henkilo.NykyinenSukunimi getNykyinenSukunimi() {
            return nykyinenSukunimi;
        }

        /**
         * Sets the value of the nykyinenSukunimi property.
         * 
         * @param value
         *     allowed object is
         *     {@link VTJHenkiloVastaussanoma.Henkilo.NykyinenSukunimi }
         *     
         */
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        public void setNykyinenSukunimi(VTJHenkiloVastaussanoma.Henkilo.NykyinenSukunimi value) {
            this.nykyinenSukunimi = value;
        }

        /**
         * Gets the value of the nykyisetEtunimet property.
         * 
         * @return
         *     possible object is
         *     {@link VTJHenkiloVastaussanoma.Henkilo.NykyisetEtunimet }
         *     
         */
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        public VTJHenkiloVastaussanoma.Henkilo.NykyisetEtunimet getNykyisetEtunimet() {
            return nykyisetEtunimet;
        }

        /**
         * Sets the value of the nykyisetEtunimet property.
         * 
         * @param value
         *     allowed object is
         *     {@link VTJHenkiloVastaussanoma.Henkilo.NykyisetEtunimet }
         *     
         */
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        public void setNykyisetEtunimet(VTJHenkiloVastaussanoma.Henkilo.NykyisetEtunimet value) {
            this.nykyisetEtunimet = value;
        }

        /**
         * Gets the value of the entinenNimi property.
         * 
         * <p>
         * This accessor method returns a reference to the live list,
         * not a snapshot. Therefore any modification you make to the
         * returned list will be present inside the JAXB object.
         * This is why there is not a <CODE>set</CODE> method for the entinenNimi property.
         * 
         * <p>
         * For example, to add a new item, do as follows:
         * <pre>
         *    getEntinenNimi().add(newItem);
         * </pre>
         * 
         * 
         * <p>
         * Objects of the following type(s) are allowed in the list
         * {@link VTJHenkiloVastaussanoma.Henkilo.EntinenNimi }
         * 
         * 
         */
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        public List<VTJHenkiloVastaussanoma.Henkilo.EntinenNimi> getEntinenNimi() {
            if (entinenNimi == null) {
                entinenNimi = new ArrayList<VTJHenkiloVastaussanoma.Henkilo.EntinenNimi>();
            }
            return this.entinenNimi;
        }

        /**
         * Gets the value of the vakinainenKotimainenLahiosoite property.
         * 
         * @return
         *     possible object is
         *     {@link VTJHenkiloVastaussanoma.Henkilo.VakinainenKotimainenLahiosoite }
         *     
         */
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        public VTJHenkiloVastaussanoma.Henkilo.VakinainenKotimainenLahiosoite getVakinainenKotimainenLahiosoite() {
            return vakinainenKotimainenLahiosoite;
        }

        /**
         * Sets the value of the vakinainenKotimainenLahiosoite property.
         * 
         * @param value
         *     allowed object is
         *     {@link VTJHenkiloVastaussanoma.Henkilo.VakinainenKotimainenLahiosoite }
         *     
         */
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        public void setVakinainenKotimainenLahiosoite(VTJHenkiloVastaussanoma.Henkilo.VakinainenKotimainenLahiosoite value) {
            this.vakinainenKotimainenLahiosoite = value;
        }

        /**
         * Gets the value of the vakinainenUlkomainenLahiosoite property.
         * 
         * @return
         *     possible object is
         *     {@link VTJHenkiloVastaussanoma.Henkilo.VakinainenUlkomainenLahiosoite }
         *     
         */
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        public VTJHenkiloVastaussanoma.Henkilo.VakinainenUlkomainenLahiosoite getVakinainenUlkomainenLahiosoite() {
            return vakinainenUlkomainenLahiosoite;
        }

        /**
         * Sets the value of the vakinainenUlkomainenLahiosoite property.
         * 
         * @param value
         *     allowed object is
         *     {@link VTJHenkiloVastaussanoma.Henkilo.VakinainenUlkomainenLahiosoite }
         *     
         */
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        public void setVakinainenUlkomainenLahiosoite(VTJHenkiloVastaussanoma.Henkilo.VakinainenUlkomainenLahiosoite value) {
            this.vakinainenUlkomainenLahiosoite = value;
        }

        /**
         * Gets the value of the vakinainenAsuinpaikka property.
         * 
         * @return
         *     possible object is
         *     {@link VTJHenkiloVastaussanoma.Henkilo.VakinainenAsuinpaikka }
         *     
         */
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        public VTJHenkiloVastaussanoma.Henkilo.VakinainenAsuinpaikka getVakinainenAsuinpaikka() {
            return vakinainenAsuinpaikka;
        }

        /**
         * Sets the value of the vakinainenAsuinpaikka property.
         * 
         * @param value
         *     allowed object is
         *     {@link VTJHenkiloVastaussanoma.Henkilo.VakinainenAsuinpaikka }
         *     
         */
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        public void setVakinainenAsuinpaikka(VTJHenkiloVastaussanoma.Henkilo.VakinainenAsuinpaikka value) {
            this.vakinainenAsuinpaikka = value;
        }

        /**
         * Gets the value of the tilapainenKotimainenLahiosoite property.
         * 
         * <p>
         * This accessor method returns a reference to the live list,
         * not a snapshot. Therefore any modification you make to the
         * returned list will be present inside the JAXB object.
         * This is why there is not a <CODE>set</CODE> method for the tilapainenKotimainenLahiosoite property.
         * 
         * <p>
         * For example, to add a new item, do as follows:
         * <pre>
         *    getTilapainenKotimainenLahiosoite().add(newItem);
         * </pre>
         * 
         * 
         * <p>
         * Objects of the following type(s) are allowed in the list
         * {@link VTJHenkiloVastaussanoma.Henkilo.TilapainenKotimainenLahiosoite }
         * 
         * 
         */
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        public List<VTJHenkiloVastaussanoma.Henkilo.TilapainenKotimainenLahiosoite> getTilapainenKotimainenLahiosoite() {
            if (tilapainenKotimainenLahiosoite == null) {
                tilapainenKotimainenLahiosoite = new ArrayList<VTJHenkiloVastaussanoma.Henkilo.TilapainenKotimainenLahiosoite>();
            }
            return this.tilapainenKotimainenLahiosoite;
        }

        /**
         * Gets the value of the tilapainenUlkomainenLahiosoite property.
         * 
         * <p>
         * This accessor method returns a reference to the live list,
         * not a snapshot. Therefore any modification you make to the
         * returned list will be present inside the JAXB object.
         * This is why there is not a <CODE>set</CODE> method for the tilapainenUlkomainenLahiosoite property.
         * 
         * <p>
         * For example, to add a new item, do as follows:
         * <pre>
         *    getTilapainenUlkomainenLahiosoite().add(newItem);
         * </pre>
         * 
         * 
         * <p>
         * Objects of the following type(s) are allowed in the list
         * {@link VTJHenkiloVastaussanoma.Henkilo.TilapainenUlkomainenLahiosoite }
         * 
         * 
         */
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        public List<VTJHenkiloVastaussanoma.Henkilo.TilapainenUlkomainenLahiosoite> getTilapainenUlkomainenLahiosoite() {
            if (tilapainenUlkomainenLahiosoite == null) {
                tilapainenUlkomainenLahiosoite = new ArrayList<VTJHenkiloVastaussanoma.Henkilo.TilapainenUlkomainenLahiosoite>();
            }
            return this.tilapainenUlkomainenLahiosoite;
        }

        /**
         * Gets the value of the kotimainenPostiosoite property.
         * 
         * <p>
         * This accessor method returns a reference to the live list,
         * not a snapshot. Therefore any modification you make to the
         * returned list will be present inside the JAXB object.
         * This is why there is not a <CODE>set</CODE> method for the kotimainenPostiosoite property.
         * 
         * <p>
         * For example, to add a new item, do as follows:
         * <pre>
         *    getKotimainenPostiosoite().add(newItem);
         * </pre>
         * 
         * 
         * <p>
         * Objects of the following type(s) are allowed in the list
         * {@link VTJHenkiloVastaussanoma.Henkilo.KotimainenPostiosoite }
         * 
         * 
         */
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        public List<VTJHenkiloVastaussanoma.Henkilo.KotimainenPostiosoite> getKotimainenPostiosoite() {
            if (kotimainenPostiosoite == null) {
                kotimainenPostiosoite = new ArrayList<VTJHenkiloVastaussanoma.Henkilo.KotimainenPostiosoite>();
            }
            return this.kotimainenPostiosoite;
        }

        /**
         * Gets the value of the ulkomainenPostiosoite property.
         * 
         * <p>
         * This accessor method returns a reference to the live list,
         * not a snapshot. Therefore any modification you make to the
         * returned list will be present inside the JAXB object.
         * This is why there is not a <CODE>set</CODE> method for the ulkomainenPostiosoite property.
         * 
         * <p>
         * For example, to add a new item, do as follows:
         * <pre>
         *    getUlkomainenPostiosoite().add(newItem);
         * </pre>
         * 
         * 
         * <p>
         * Objects of the following type(s) are allowed in the list
         * {@link VTJHenkiloVastaussanoma.Henkilo.UlkomainenPostiosoite }
         * 
         * 
         */
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        public List<VTJHenkiloVastaussanoma.Henkilo.UlkomainenPostiosoite> getUlkomainenPostiosoite() {
            if (ulkomainenPostiosoite == null) {
                ulkomainenPostiosoite = new ArrayList<VTJHenkiloVastaussanoma.Henkilo.UlkomainenPostiosoite>();
            }
            return this.ulkomainenPostiosoite;
        }

        /**
         * Gets the value of the kotikunta property.
         * 
         * @return
         *     possible object is
         *     {@link VTJHenkiloVastaussanoma.Henkilo.Kotikunta }
         *     
         */
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        public VTJHenkiloVastaussanoma.Henkilo.Kotikunta getKotikunta() {
            return kotikunta;
        }

        /**
         * Sets the value of the kotikunta property.
         * 
         * @param value
         *     allowed object is
         *     {@link VTJHenkiloVastaussanoma.Henkilo.Kotikunta }
         *     
         */
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        public void setKotikunta(VTJHenkiloVastaussanoma.Henkilo.Kotikunta value) {
            this.kotikunta = value;
        }

        /**
         * Gets the value of the kuolintiedot property.
         * 
         * @return
         *     possible object is
         *     {@link VTJHenkiloVastaussanoma.Henkilo.Kuolintiedot }
         *     
         */
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        public VTJHenkiloVastaussanoma.Henkilo.Kuolintiedot getKuolintiedot() {
            return kuolintiedot;
        }

        /**
         * Sets the value of the kuolintiedot property.
         * 
         * @param value
         *     allowed object is
         *     {@link VTJHenkiloVastaussanoma.Henkilo.Kuolintiedot }
         *     
         */
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        public void setKuolintiedot(VTJHenkiloVastaussanoma.Henkilo.Kuolintiedot value) {
            this.kuolintiedot = value;
        }

        /**
         * Gets the value of the kuolleeksijulistamistiedot property.
         * 
         * @return
         *     possible object is
         *     {@link VTJHenkiloVastaussanoma.Henkilo.Kuolleeksijulistamistiedot }
         *     
         */
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        public VTJHenkiloVastaussanoma.Henkilo.Kuolleeksijulistamistiedot getKuolleeksijulistamistiedot() {
            return kuolleeksijulistamistiedot;
        }

        /**
         * Sets the value of the kuolleeksijulistamistiedot property.
         * 
         * @param value
         *     allowed object is
         *     {@link VTJHenkiloVastaussanoma.Henkilo.Kuolleeksijulistamistiedot }
         *     
         */
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        public void setKuolleeksijulistamistiedot(VTJHenkiloVastaussanoma.Henkilo.Kuolleeksijulistamistiedot value) {
            this.kuolleeksijulistamistiedot = value;
        }

        /**
         * Gets the value of the aidinkieli property.
         * 
         * @return
         *     possible object is
         *     {@link VTJHenkiloVastaussanoma.Henkilo.Aidinkieli }
         *     
         */
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        public VTJHenkiloVastaussanoma.Henkilo.Aidinkieli getAidinkieli() {
            return aidinkieli;
        }

        /**
         * Sets the value of the aidinkieli property.
         * 
         * @param value
         *     allowed object is
         *     {@link VTJHenkiloVastaussanoma.Henkilo.Aidinkieli }
         *     
         */
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        public void setAidinkieli(VTJHenkiloVastaussanoma.Henkilo.Aidinkieli value) {
            this.aidinkieli = value;
        }

        /**
         * Gets the value of the turvakielto property.
         * 
         * @return
         *     possible object is
         *     {@link VTJHenkiloVastaussanoma.Henkilo.Turvakielto }
         *     
         */
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        public VTJHenkiloVastaussanoma.Henkilo.Turvakielto getTurvakielto() {
            return turvakielto;
        }

        /**
         * Sets the value of the turvakielto property.
         * 
         * @param value
         *     allowed object is
         *     {@link VTJHenkiloVastaussanoma.Henkilo.Turvakielto }
         *     
         */
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        public void setTurvakielto(VTJHenkiloVastaussanoma.Henkilo.Turvakielto value) {
            this.turvakielto = value;
        }

        /**
         * Gets the value of the sukupuoli property.
         * 
         * @return
         *     possible object is
         *     {@link VTJHenkiloVastaussanoma.Henkilo.Sukupuoli }
         *     
         */
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        public VTJHenkiloVastaussanoma.Henkilo.Sukupuoli getSukupuoli() {
            return sukupuoli;
        }

        /**
         * Sets the value of the sukupuoli property.
         * 
         * @param value
         *     allowed object is
         *     {@link VTJHenkiloVastaussanoma.Henkilo.Sukupuoli }
         *     
         */
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        public void setSukupuoli(VTJHenkiloVastaussanoma.Henkilo.Sukupuoli value) {
            this.sukupuoli = value;
        }

        /**
         * Gets the value of the huoltaja property.
         * 
         * <p>
         * This accessor method returns a reference to the live list,
         * not a snapshot. Therefore any modification you make to the
         * returned list will be present inside the JAXB object.
         * This is why there is not a <CODE>set</CODE> method for the huoltaja property.
         * 
         * <p>
         * For example, to add a new item, do as follows:
         * <pre>
         *    getHuoltaja().add(newItem);
         * </pre>
         * 
         * 
         * <p>
         * Objects of the following type(s) are allowed in the list
         * {@link VTJHenkiloVastaussanoma.Henkilo.Huoltaja }
         * 
         * 
         */
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        public List<VTJHenkiloVastaussanoma.Henkilo.Huoltaja> getHuoltaja() {
            if (huoltaja == null) {
                huoltaja = new ArrayList<VTJHenkiloVastaussanoma.Henkilo.Huoltaja>();
            }
            return this.huoltaja;
        }

        /**
         * Gets the value of the huollettava property.
         * 
         * <p>
         * This accessor method returns a reference to the live list,
         * not a snapshot. Therefore any modification you make to the
         * returned list will be present inside the JAXB object.
         * This is why there is not a <CODE>set</CODE> method for the huollettava property.
         * 
         * <p>
         * For example, to add a new item, do as follows:
         * <pre>
         *    getHuollettava().add(newItem);
         * </pre>
         * 
         * 
         * <p>
         * Objects of the following type(s) are allowed in the list
         * {@link VTJHenkiloVastaussanoma.Henkilo.Huollettava }
         * 
         * 
         */
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        public List<VTJHenkiloVastaussanoma.Henkilo.Huollettava> getHuollettava() {
            if (huollettava == null) {
                huollettava = new ArrayList<VTJHenkiloVastaussanoma.Henkilo.Huollettava>();
            }
            return this.huollettava;
        }

        /**
         * Gets the value of the lapsi property.
         * 
         * <p>
         * This accessor method returns a reference to the live list,
         * not a snapshot. Therefore any modification you make to the
         * returned list will be present inside the JAXB object.
         * This is why there is not a <CODE>set</CODE> method for the lapsi property.
         * 
         * <p>
         * For example, to add a new item, do as follows:
         * <pre>
         *    getLapsi().add(newItem);
         * </pre>
         * 
         * 
         * <p>
         * Objects of the following type(s) are allowed in the list
         * {@link VTJHenkiloVastaussanoma.Henkilo.Lapsi }
         * 
         * 
         */
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        public List<VTJHenkiloVastaussanoma.Henkilo.Lapsi> getLapsi() {
            if (lapsi == null) {
                lapsi = new ArrayList<VTJHenkiloVastaussanoma.Henkilo.Lapsi>();
            }
            return this.lapsi;
        }

        /**
         * Gets the value of the kansalaisuus property.
         * 
         * <p>
         * This accessor method returns a reference to the live list,
         * not a snapshot. Therefore any modification you make to the
         * returned list will be present inside the JAXB object.
         * This is why there is not a <CODE>set</CODE> method for the kansalaisuus property.
         * 
         * <p>
         * For example, to add a new item, do as follows:
         * <pre>
         *    getKansalaisuus().add(newItem);
         * </pre>
         * 
         * 
         * <p>
         * Objects of the following type(s) are allowed in the list
         * {@link VTJHenkiloVastaussanoma.Henkilo.Kansalaisuus }
         * 
         * 
         */
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        public List<VTJHenkiloVastaussanoma.Henkilo.Kansalaisuus> getKansalaisuus() {
            if (kansalaisuus == null) {
                kansalaisuus = new ArrayList<VTJHenkiloVastaussanoma.Henkilo.Kansalaisuus>();
            }
            return this.kansalaisuus;
        }

        /**
         * Gets the value of the asukasLkm property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        public String getAsukasLkm() {
            return asukasLkm;
        }

        /**
         * Sets the value of the asukasLkm property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        public void setAsukasLkm(String value) {
            this.asukasLkm = value;
        }

        /**
         * Gets the value of the asukkaatAlle18V property.
         * 
         * @return
         *     possible object is
         *     {@link VTJHenkiloVastaussanoma.Henkilo.AsukkaatAlle18V }
         *     
         */
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        public VTJHenkiloVastaussanoma.Henkilo.AsukkaatAlle18V getAsukkaatAlle18V() {
            return asukkaatAlle18V;
        }

        /**
         * Sets the value of the asukkaatAlle18V property.
         * 
         * @param value
         *     allowed object is
         *     {@link VTJHenkiloVastaussanoma.Henkilo.AsukkaatAlle18V }
         *     
         */
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        public void setAsukkaatAlle18V(VTJHenkiloVastaussanoma.Henkilo.AsukkaatAlle18V value) {
            this.asukkaatAlle18V = value;
        }

        /**
         * Gets the value of the asukasAlle18V property.
         * 
         * <p>
         * This accessor method returns a reference to the live list,
         * not a snapshot. Therefore any modification you make to the
         * returned list will be present inside the JAXB object.
         * This is why there is not a <CODE>set</CODE> method for the asukasAlle18V property.
         * 
         * <p>
         * For example, to add a new item, do as follows:
         * <pre>
         *    getAsukasAlle18V().add(newItem);
         * </pre>
         * 
         * 
         * <p>
         * Objects of the following type(s) are allowed in the list
         * {@link VTJHenkiloVastaussanoma.Henkilo.AsukasAlle18V }
         * 
         * 
         */
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        public List<VTJHenkiloVastaussanoma.Henkilo.AsukasAlle18V> getAsukasAlle18V() {
            if (asukasAlle18V == null) {
                asukasAlle18V = new ArrayList<VTJHenkiloVastaussanoma.Henkilo.AsukasAlle18V>();
            }
            return this.asukasAlle18V;
        }

        /**
         * Gets the value of the syntymamaa property.
         * 
         * @return
         *     possible object is
         *     {@link VTJHenkiloVastaussanoma.Henkilo.Syntymamaa }
         *     
         */
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        public VTJHenkiloVastaussanoma.Henkilo.Syntymamaa getSyntymamaa() {
            return syntymamaa;
        }

        /**
         * Sets the value of the syntymamaa property.
         * 
         * @param value
         *     allowed object is
         *     {@link VTJHenkiloVastaussanoma.Henkilo.Syntymamaa }
         *     
         */
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        public void setSyntymamaa(VTJHenkiloVastaussanoma.Henkilo.Syntymamaa value) {
            this.syntymamaa = value;
        }

        /**
         * Gets the value of the edunvalvonta property.
         * 
         * @return
         *     possible object is
         *     {@link VTJHenkiloVastaussanoma.Henkilo.Edunvalvonta }
         *     
         */
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        public VTJHenkiloVastaussanoma.Henkilo.Edunvalvonta getEdunvalvonta() {
            return edunvalvonta;
        }

        /**
         * Sets the value of the edunvalvonta property.
         * 
         * @param value
         *     allowed object is
         *     {@link VTJHenkiloVastaussanoma.Henkilo.Edunvalvonta }
         *     
         */
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        public void setEdunvalvonta(VTJHenkiloVastaussanoma.Henkilo.Edunvalvonta value) {
            this.edunvalvonta = value;
        }

        /**
         * Gets the value of the edunvalvontavaltuutus property.
         * 
         * @return
         *     possible object is
         *     {@link VTJHenkiloVastaussanoma.Henkilo.Edunvalvontavaltuutus }
         *     
         */
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        public VTJHenkiloVastaussanoma.Henkilo.Edunvalvontavaltuutus getEdunvalvontavaltuutus() {
            return edunvalvontavaltuutus;
        }

        /**
         * Sets the value of the edunvalvontavaltuutus property.
         * 
         * @param value
         *     allowed object is
         *     {@link VTJHenkiloVastaussanoma.Henkilo.Edunvalvontavaltuutus }
         *     
         */
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        public void setEdunvalvontavaltuutus(VTJHenkiloVastaussanoma.Henkilo.Edunvalvontavaltuutus value) {
            this.edunvalvontavaltuutus = value;
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
         *         &lt;element name="Kielikoodi" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}KielikoodiTyyppi"/&gt;
         *         &lt;element name="KieliS" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}KieliNimiTyyppi"/&gt;
         *         &lt;element name="KieliR" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}KieliNimiTyyppi"/&gt;
         *         &lt;element name="KieliSelvakielinen" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}KieliNimiTyyppi"/&gt;
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
            "kielikoodi",
            "kieliS",
            "kieliR",
            "kieliSelvakielinen"
        })
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        public static class Aidinkieli {

            @XmlElement(name = "Kielikoodi", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected String kielikoodi;
            @XmlElement(name = "KieliS", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected String kieliS;
            @XmlElement(name = "KieliR", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected String kieliR;
            @XmlElement(name = "KieliSelvakielinen", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected String kieliSelvakielinen;

            /**
             * Gets the value of the kielikoodi property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public String getKielikoodi() {
                return kielikoodi;
            }

            /**
             * Sets the value of the kielikoodi property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public void setKielikoodi(String value) {
                this.kielikoodi = value;
            }

            /**
             * Gets the value of the kieliS property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public String getKieliS() {
                return kieliS;
            }

            /**
             * Sets the value of the kieliS property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public void setKieliS(String value) {
                this.kieliS = value;
            }

            /**
             * Gets the value of the kieliR property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public String getKieliR() {
                return kieliR;
            }

            /**
             * Sets the value of the kieliR property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public void setKieliR(String value) {
                this.kieliR = value;
            }

            /**
             * Gets the value of the kieliSelvakielinen property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public String getKieliSelvakielinen() {
                return kieliSelvakielinen;
            }

            /**
             * Sets the value of the kieliSelvakielinen property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public void setKieliSelvakielinen(String value) {
                this.kieliSelvakielinen = value;
            }

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
         *         &lt;element name="Henkilotunnus" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}HenkilotunnusTyyppi"/&gt;
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
            "henkilotunnus"
        })
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        public static class AsukasAlle18V {

            @XmlElement(name = "Henkilotunnus", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected String henkilotunnus;

            /**
             * Gets the value of the henkilotunnus property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public String getHenkilotunnus() {
                return henkilotunnus;
            }

            /**
             * Sets the value of the henkilotunnus property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public void setHenkilotunnus(String value) {
                this.henkilotunnus = value;
            }

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
         *         &lt;element name="AsukasLkm" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}AsukasLkmTyyppi"/&gt;
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
            "asukasLkm"
        })
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        public static class AsukkaatAlle18V {

            @XmlElement(name = "AsukasLkm", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected String asukasLkm;

            /**
             * Gets the value of the asukasLkm property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public String getAsukasLkm() {
                return asukasLkm;
            }

            /**
             * Sets the value of the asukasLkm property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public void setAsukasLkm(String value) {
                this.asukasLkm = value;
            }

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
         *         &lt;element name="Alkupvm" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
         *         &lt;element name="Paattymispvm" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
         *         &lt;element name="Rajoituskoodi" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}RajoituskoodiTyyppi"/&gt;
         *         &lt;element name="RajoitustekstiS" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}Teksti100Tyyppi"/&gt;
         *         &lt;element name="RajoitustekstiR" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}Teksti100Tyyppi"/&gt;
         *         &lt;element name="Tehtavienjakokoodi" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}TehtavienjakokoodiTyyppi"/&gt;
         *         &lt;element name="TehtavienjakoS" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}Teksti100Tyyppi"/&gt;
         *         &lt;element name="TehtavienjakoR" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}Teksti100Tyyppi"/&gt;
         *         &lt;element name="HenkiloEdunvalvoja" maxOccurs="unbounded"&gt;
         *           &lt;complexType&gt;
         *             &lt;complexContent&gt;
         *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
         *                 &lt;sequence&gt;
         *                   &lt;element name="Henkilotunnus" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}HenkilotunnusTyyppi"/&gt;
         *                   &lt;element name="Syntymaaika" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
         *                   &lt;element name="NykyinenSukunimi"&gt;
         *                     &lt;complexType&gt;
         *                       &lt;complexContent&gt;
         *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
         *                           &lt;sequence&gt;
         *                             &lt;element name="Sukunimi" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}Nimi100Tyyppi"/&gt;
         *                           &lt;/sequence&gt;
         *                         &lt;/restriction&gt;
         *                       &lt;/complexContent&gt;
         *                     &lt;/complexType&gt;
         *                   &lt;/element&gt;
         *                   &lt;element name="NykyisetEtunimet"&gt;
         *                     &lt;complexType&gt;
         *                       &lt;complexContent&gt;
         *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
         *                           &lt;sequence&gt;
         *                             &lt;element name="Etunimet" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}Nimi100Tyyppi"/&gt;
         *                           &lt;/sequence&gt;
         *                         &lt;/restriction&gt;
         *                       &lt;/complexContent&gt;
         *                     &lt;/complexType&gt;
         *                   &lt;/element&gt;
         *                   &lt;element name="TehtavaAlkupvm" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
         *                   &lt;element name="TehtavaLoppupvm" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
         *                 &lt;/sequence&gt;
         *               &lt;/restriction&gt;
         *             &lt;/complexContent&gt;
         *           &lt;/complexType&gt;
         *         &lt;/element&gt;
         *         &lt;element name="YritysJaYhteisoEdunvalvoja" maxOccurs="unbounded"&gt;
         *           &lt;complexType&gt;
         *             &lt;complexContent&gt;
         *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
         *                 &lt;sequence&gt;
         *                   &lt;element name="Ytunnus" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}YtunnusTyyppi"/&gt;
         *                   &lt;element name="Nimi" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}YritysNimi80Tyyppi"/&gt;
         *                   &lt;element name="TehtavaLoppupvm" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
         *                   &lt;element name="TehtavaAlkupvm" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
         *                 &lt;/sequence&gt;
         *               &lt;/restriction&gt;
         *             &lt;/complexContent&gt;
         *           &lt;/complexType&gt;
         *         &lt;/element&gt;
         *         &lt;element name="OikeusaputoimistoEdunvalvoja" maxOccurs="unbounded"&gt;
         *           &lt;complexType&gt;
         *             &lt;complexContent&gt;
         *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
         *                 &lt;sequence&gt;
         *                   &lt;element name="Viranomaisnumero" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}ViranomaiskoodiTyyppi"/&gt;
         *                   &lt;element name="ViranomainenS" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}ViranomainenNimiTyyppi"/&gt;
         *                   &lt;element name="ViranomainenR" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}ViranomainenNimiTyyppi"/&gt;
         *                   &lt;element name="TehtavaAlkupvm" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
         *                   &lt;element name="TehtavaLoppupvm" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
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
        @XmlType(name = "", propOrder = {
            "alkupvm",
            "paattymispvm",
            "rajoituskoodi",
            "rajoitustekstiS",
            "rajoitustekstiR",
            "tehtavienjakokoodi",
            "tehtavienjakoS",
            "tehtavienjakoR",
            "henkiloEdunvalvoja",
            "yritysJaYhteisoEdunvalvoja",
            "oikeusaputoimistoEdunvalvoja"
        })
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        public static class Edunvalvonta {

            @XmlElement(name = "Alkupvm", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected String alkupvm;
            @XmlElement(name = "Paattymispvm", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected String paattymispvm;
            @XmlElement(name = "Rajoituskoodi", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected String rajoituskoodi;
            @XmlElement(name = "RajoitustekstiS", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected String rajoitustekstiS;
            @XmlElement(name = "RajoitustekstiR", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected String rajoitustekstiR;
            @XmlElement(name = "Tehtavienjakokoodi", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected String tehtavienjakokoodi;
            @XmlElement(name = "TehtavienjakoS", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected String tehtavienjakoS;
            @XmlElement(name = "TehtavienjakoR", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected String tehtavienjakoR;
            @XmlElement(name = "HenkiloEdunvalvoja", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected List<VTJHenkiloVastaussanoma.Henkilo.Edunvalvonta.HenkiloEdunvalvoja> henkiloEdunvalvoja;
            @XmlElement(name = "YritysJaYhteisoEdunvalvoja", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected List<VTJHenkiloVastaussanoma.Henkilo.Edunvalvonta.YritysJaYhteisoEdunvalvoja> yritysJaYhteisoEdunvalvoja;
            @XmlElement(name = "OikeusaputoimistoEdunvalvoja", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected List<VTJHenkiloVastaussanoma.Henkilo.Edunvalvonta.OikeusaputoimistoEdunvalvoja> oikeusaputoimistoEdunvalvoja;

            /**
             * Gets the value of the alkupvm property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public String getAlkupvm() {
                return alkupvm;
            }

            /**
             * Sets the value of the alkupvm property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public void setAlkupvm(String value) {
                this.alkupvm = value;
            }

            /**
             * Gets the value of the paattymispvm property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public String getPaattymispvm() {
                return paattymispvm;
            }

            /**
             * Sets the value of the paattymispvm property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public void setPaattymispvm(String value) {
                this.paattymispvm = value;
            }

            /**
             * Gets the value of the rajoituskoodi property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public String getRajoituskoodi() {
                return rajoituskoodi;
            }

            /**
             * Sets the value of the rajoituskoodi property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public void setRajoituskoodi(String value) {
                this.rajoituskoodi = value;
            }

            /**
             * Gets the value of the rajoitustekstiS property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public String getRajoitustekstiS() {
                return rajoitustekstiS;
            }

            /**
             * Sets the value of the rajoitustekstiS property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public void setRajoitustekstiS(String value) {
                this.rajoitustekstiS = value;
            }

            /**
             * Gets the value of the rajoitustekstiR property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public String getRajoitustekstiR() {
                return rajoitustekstiR;
            }

            /**
             * Sets the value of the rajoitustekstiR property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public void setRajoitustekstiR(String value) {
                this.rajoitustekstiR = value;
            }

            /**
             * Gets the value of the tehtavienjakokoodi property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public String getTehtavienjakokoodi() {
                return tehtavienjakokoodi;
            }

            /**
             * Sets the value of the tehtavienjakokoodi property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public void setTehtavienjakokoodi(String value) {
                this.tehtavienjakokoodi = value;
            }

            /**
             * Gets the value of the tehtavienjakoS property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public String getTehtavienjakoS() {
                return tehtavienjakoS;
            }

            /**
             * Sets the value of the tehtavienjakoS property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public void setTehtavienjakoS(String value) {
                this.tehtavienjakoS = value;
            }

            /**
             * Gets the value of the tehtavienjakoR property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public String getTehtavienjakoR() {
                return tehtavienjakoR;
            }

            /**
             * Sets the value of the tehtavienjakoR property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public void setTehtavienjakoR(String value) {
                this.tehtavienjakoR = value;
            }

            /**
             * Gets the value of the henkiloEdunvalvoja property.
             * 
             * <p>
             * This accessor method returns a reference to the live list,
             * not a snapshot. Therefore any modification you make to the
             * returned list will be present inside the JAXB object.
             * This is why there is not a <CODE>set</CODE> method for the henkiloEdunvalvoja property.
             * 
             * <p>
             * For example, to add a new item, do as follows:
             * <pre>
             *    getHenkiloEdunvalvoja().add(newItem);
             * </pre>
             * 
             * 
             * <p>
             * Objects of the following type(s) are allowed in the list
             * {@link VTJHenkiloVastaussanoma.Henkilo.Edunvalvonta.HenkiloEdunvalvoja }
             * 
             * 
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public List<VTJHenkiloVastaussanoma.Henkilo.Edunvalvonta.HenkiloEdunvalvoja> getHenkiloEdunvalvoja() {
                if (henkiloEdunvalvoja == null) {
                    henkiloEdunvalvoja = new ArrayList<VTJHenkiloVastaussanoma.Henkilo.Edunvalvonta.HenkiloEdunvalvoja>();
                }
                return this.henkiloEdunvalvoja;
            }

            /**
             * Gets the value of the yritysJaYhteisoEdunvalvoja property.
             * 
             * <p>
             * This accessor method returns a reference to the live list,
             * not a snapshot. Therefore any modification you make to the
             * returned list will be present inside the JAXB object.
             * This is why there is not a <CODE>set</CODE> method for the yritysJaYhteisoEdunvalvoja property.
             * 
             * <p>
             * For example, to add a new item, do as follows:
             * <pre>
             *    getYritysJaYhteisoEdunvalvoja().add(newItem);
             * </pre>
             * 
             * 
             * <p>
             * Objects of the following type(s) are allowed in the list
             * {@link VTJHenkiloVastaussanoma.Henkilo.Edunvalvonta.YritysJaYhteisoEdunvalvoja }
             * 
             * 
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public List<VTJHenkiloVastaussanoma.Henkilo.Edunvalvonta.YritysJaYhteisoEdunvalvoja> getYritysJaYhteisoEdunvalvoja() {
                if (yritysJaYhteisoEdunvalvoja == null) {
                    yritysJaYhteisoEdunvalvoja = new ArrayList<VTJHenkiloVastaussanoma.Henkilo.Edunvalvonta.YritysJaYhteisoEdunvalvoja>();
                }
                return this.yritysJaYhteisoEdunvalvoja;
            }

            /**
             * Gets the value of the oikeusaputoimistoEdunvalvoja property.
             * 
             * <p>
             * This accessor method returns a reference to the live list,
             * not a snapshot. Therefore any modification you make to the
             * returned list will be present inside the JAXB object.
             * This is why there is not a <CODE>set</CODE> method for the oikeusaputoimistoEdunvalvoja property.
             * 
             * <p>
             * For example, to add a new item, do as follows:
             * <pre>
             *    getOikeusaputoimistoEdunvalvoja().add(newItem);
             * </pre>
             * 
             * 
             * <p>
             * Objects of the following type(s) are allowed in the list
             * {@link VTJHenkiloVastaussanoma.Henkilo.Edunvalvonta.OikeusaputoimistoEdunvalvoja }
             * 
             * 
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public List<VTJHenkiloVastaussanoma.Henkilo.Edunvalvonta.OikeusaputoimistoEdunvalvoja> getOikeusaputoimistoEdunvalvoja() {
                if (oikeusaputoimistoEdunvalvoja == null) {
                    oikeusaputoimistoEdunvalvoja = new ArrayList<VTJHenkiloVastaussanoma.Henkilo.Edunvalvonta.OikeusaputoimistoEdunvalvoja>();
                }
                return this.oikeusaputoimistoEdunvalvoja;
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
             *         &lt;element name="Henkilotunnus" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}HenkilotunnusTyyppi"/&gt;
             *         &lt;element name="Syntymaaika" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
             *         &lt;element name="NykyinenSukunimi"&gt;
             *           &lt;complexType&gt;
             *             &lt;complexContent&gt;
             *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
             *                 &lt;sequence&gt;
             *                   &lt;element name="Sukunimi" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}Nimi100Tyyppi"/&gt;
             *                 &lt;/sequence&gt;
             *               &lt;/restriction&gt;
             *             &lt;/complexContent&gt;
             *           &lt;/complexType&gt;
             *         &lt;/element&gt;
             *         &lt;element name="NykyisetEtunimet"&gt;
             *           &lt;complexType&gt;
             *             &lt;complexContent&gt;
             *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
             *                 &lt;sequence&gt;
             *                   &lt;element name="Etunimet" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}Nimi100Tyyppi"/&gt;
             *                 &lt;/sequence&gt;
             *               &lt;/restriction&gt;
             *             &lt;/complexContent&gt;
             *           &lt;/complexType&gt;
             *         &lt;/element&gt;
             *         &lt;element name="TehtavaAlkupvm" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
             *         &lt;element name="TehtavaLoppupvm" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
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
                "henkilotunnus",
                "syntymaaika",
                "nykyinenSukunimi",
                "nykyisetEtunimet",
                "tehtavaAlkupvm",
                "tehtavaLoppupvm"
            })
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public static class HenkiloEdunvalvoja {

                @XmlElement(name = "Henkilotunnus", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
                @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
                protected String henkilotunnus;
                @XmlElement(name = "Syntymaaika", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
                @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
                protected String syntymaaika;
                @XmlElement(name = "NykyinenSukunimi", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
                @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
                protected VTJHenkiloVastaussanoma.Henkilo.Edunvalvonta.HenkiloEdunvalvoja.NykyinenSukunimi nykyinenSukunimi;
                @XmlElement(name = "NykyisetEtunimet", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
                @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
                protected VTJHenkiloVastaussanoma.Henkilo.Edunvalvonta.HenkiloEdunvalvoja.NykyisetEtunimet nykyisetEtunimet;
                @XmlElement(name = "TehtavaAlkupvm", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
                @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
                protected String tehtavaAlkupvm;
                @XmlElement(name = "TehtavaLoppupvm", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
                @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
                protected String tehtavaLoppupvm;

                /**
                 * Gets the value of the henkilotunnus property.
                 * 
                 * @return
                 *     possible object is
                 *     {@link String }
                 *     
                 */
                @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
                public String getHenkilotunnus() {
                    return henkilotunnus;
                }

                /**
                 * Sets the value of the henkilotunnus property.
                 * 
                 * @param value
                 *     allowed object is
                 *     {@link String }
                 *     
                 */
                @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
                public void setHenkilotunnus(String value) {
                    this.henkilotunnus = value;
                }

                /**
                 * Gets the value of the syntymaaika property.
                 * 
                 * @return
                 *     possible object is
                 *     {@link String }
                 *     
                 */
                @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
                public String getSyntymaaika() {
                    return syntymaaika;
                }

                /**
                 * Sets the value of the syntymaaika property.
                 * 
                 * @param value
                 *     allowed object is
                 *     {@link String }
                 *     
                 */
                @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
                public void setSyntymaaika(String value) {
                    this.syntymaaika = value;
                }

                /**
                 * Gets the value of the nykyinenSukunimi property.
                 * 
                 * @return
                 *     possible object is
                 *     {@link VTJHenkiloVastaussanoma.Henkilo.Edunvalvonta.HenkiloEdunvalvoja.NykyinenSukunimi }
                 *     
                 */
                @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
                public VTJHenkiloVastaussanoma.Henkilo.Edunvalvonta.HenkiloEdunvalvoja.NykyinenSukunimi getNykyinenSukunimi() {
                    return nykyinenSukunimi;
                }

                /**
                 * Sets the value of the nykyinenSukunimi property.
                 * 
                 * @param value
                 *     allowed object is
                 *     {@link VTJHenkiloVastaussanoma.Henkilo.Edunvalvonta.HenkiloEdunvalvoja.NykyinenSukunimi }
                 *     
                 */
                @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
                public void setNykyinenSukunimi(VTJHenkiloVastaussanoma.Henkilo.Edunvalvonta.HenkiloEdunvalvoja.NykyinenSukunimi value) {
                    this.nykyinenSukunimi = value;
                }

                /**
                 * Gets the value of the nykyisetEtunimet property.
                 * 
                 * @return
                 *     possible object is
                 *     {@link VTJHenkiloVastaussanoma.Henkilo.Edunvalvonta.HenkiloEdunvalvoja.NykyisetEtunimet }
                 *     
                 */
                @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
                public VTJHenkiloVastaussanoma.Henkilo.Edunvalvonta.HenkiloEdunvalvoja.NykyisetEtunimet getNykyisetEtunimet() {
                    return nykyisetEtunimet;
                }

                /**
                 * Sets the value of the nykyisetEtunimet property.
                 * 
                 * @param value
                 *     allowed object is
                 *     {@link VTJHenkiloVastaussanoma.Henkilo.Edunvalvonta.HenkiloEdunvalvoja.NykyisetEtunimet }
                 *     
                 */
                @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
                public void setNykyisetEtunimet(VTJHenkiloVastaussanoma.Henkilo.Edunvalvonta.HenkiloEdunvalvoja.NykyisetEtunimet value) {
                    this.nykyisetEtunimet = value;
                }

                /**
                 * Gets the value of the tehtavaAlkupvm property.
                 * 
                 * @return
                 *     possible object is
                 *     {@link String }
                 *     
                 */
                @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
                public String getTehtavaAlkupvm() {
                    return tehtavaAlkupvm;
                }

                /**
                 * Sets the value of the tehtavaAlkupvm property.
                 * 
                 * @param value
                 *     allowed object is
                 *     {@link String }
                 *     
                 */
                @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
                public void setTehtavaAlkupvm(String value) {
                    this.tehtavaAlkupvm = value;
                }

                /**
                 * Gets the value of the tehtavaLoppupvm property.
                 * 
                 * @return
                 *     possible object is
                 *     {@link String }
                 *     
                 */
                @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
                public String getTehtavaLoppupvm() {
                    return tehtavaLoppupvm;
                }

                /**
                 * Sets the value of the tehtavaLoppupvm property.
                 * 
                 * @param value
                 *     allowed object is
                 *     {@link String }
                 *     
                 */
                @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
                public void setTehtavaLoppupvm(String value) {
                    this.tehtavaLoppupvm = value;
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
                 *         &lt;element name="Sukunimi" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}Nimi100Tyyppi"/&gt;
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
                    "sukunimi"
                })
                @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
                public static class NykyinenSukunimi {

                    @XmlElement(name = "Sukunimi", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
                    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
                    protected String sukunimi;

                    /**
                     * Gets the value of the sukunimi property.
                     * 
                     * @return
                     *     possible object is
                     *     {@link String }
                     *     
                     */
                    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
                    public String getSukunimi() {
                        return sukunimi;
                    }

                    /**
                     * Sets the value of the sukunimi property.
                     * 
                     * @param value
                     *     allowed object is
                     *     {@link String }
                     *     
                     */
                    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
                    public void setSukunimi(String value) {
                        this.sukunimi = value;
                    }

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
                 *         &lt;element name="Etunimet" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}Nimi100Tyyppi"/&gt;
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
                    "etunimet"
                })
                @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
                public static class NykyisetEtunimet {

                    @XmlElement(name = "Etunimet", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
                    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
                    protected String etunimet;

                    /**
                     * Gets the value of the etunimet property.
                     * 
                     * @return
                     *     possible object is
                     *     {@link String }
                     *     
                     */
                    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
                    public String getEtunimet() {
                        return etunimet;
                    }

                    /**
                     * Sets the value of the etunimet property.
                     * 
                     * @param value
                     *     allowed object is
                     *     {@link String }
                     *     
                     */
                    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
                    public void setEtunimet(String value) {
                        this.etunimet = value;
                    }

                }

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
             *         &lt;element name="Viranomaisnumero" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}ViranomaiskoodiTyyppi"/&gt;
             *         &lt;element name="ViranomainenS" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}ViranomainenNimiTyyppi"/&gt;
             *         &lt;element name="ViranomainenR" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}ViranomainenNimiTyyppi"/&gt;
             *         &lt;element name="TehtavaAlkupvm" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
             *         &lt;element name="TehtavaLoppupvm" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
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
                "viranomaisnumero",
                "viranomainenS",
                "viranomainenR",
                "tehtavaAlkupvm",
                "tehtavaLoppupvm"
            })
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public static class OikeusaputoimistoEdunvalvoja {

                @XmlElement(name = "Viranomaisnumero", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
                @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
                protected String viranomaisnumero;
                @XmlElement(name = "ViranomainenS", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
                @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
                protected String viranomainenS;
                @XmlElement(name = "ViranomainenR", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
                @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
                protected String viranomainenR;
                @XmlElement(name = "TehtavaAlkupvm", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
                @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
                protected String tehtavaAlkupvm;
                @XmlElement(name = "TehtavaLoppupvm", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
                @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
                protected String tehtavaLoppupvm;

                /**
                 * Gets the value of the viranomaisnumero property.
                 * 
                 * @return
                 *     possible object is
                 *     {@link String }
                 *     
                 */
                @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
                public String getViranomaisnumero() {
                    return viranomaisnumero;
                }

                /**
                 * Sets the value of the viranomaisnumero property.
                 * 
                 * @param value
                 *     allowed object is
                 *     {@link String }
                 *     
                 */
                @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
                public void setViranomaisnumero(String value) {
                    this.viranomaisnumero = value;
                }

                /**
                 * Gets the value of the viranomainenS property.
                 * 
                 * @return
                 *     possible object is
                 *     {@link String }
                 *     
                 */
                @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
                public String getViranomainenS() {
                    return viranomainenS;
                }

                /**
                 * Sets the value of the viranomainenS property.
                 * 
                 * @param value
                 *     allowed object is
                 *     {@link String }
                 *     
                 */
                @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
                public void setViranomainenS(String value) {
                    this.viranomainenS = value;
                }

                /**
                 * Gets the value of the viranomainenR property.
                 * 
                 * @return
                 *     possible object is
                 *     {@link String }
                 *     
                 */
                @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
                public String getViranomainenR() {
                    return viranomainenR;
                }

                /**
                 * Sets the value of the viranomainenR property.
                 * 
                 * @param value
                 *     allowed object is
                 *     {@link String }
                 *     
                 */
                @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
                public void setViranomainenR(String value) {
                    this.viranomainenR = value;
                }

                /**
                 * Gets the value of the tehtavaAlkupvm property.
                 * 
                 * @return
                 *     possible object is
                 *     {@link String }
                 *     
                 */
                @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
                public String getTehtavaAlkupvm() {
                    return tehtavaAlkupvm;
                }

                /**
                 * Sets the value of the tehtavaAlkupvm property.
                 * 
                 * @param value
                 *     allowed object is
                 *     {@link String }
                 *     
                 */
                @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
                public void setTehtavaAlkupvm(String value) {
                    this.tehtavaAlkupvm = value;
                }

                /**
                 * Gets the value of the tehtavaLoppupvm property.
                 * 
                 * @return
                 *     possible object is
                 *     {@link String }
                 *     
                 */
                @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
                public String getTehtavaLoppupvm() {
                    return tehtavaLoppupvm;
                }

                /**
                 * Sets the value of the tehtavaLoppupvm property.
                 * 
                 * @param value
                 *     allowed object is
                 *     {@link String }
                 *     
                 */
                @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
                public void setTehtavaLoppupvm(String value) {
                    this.tehtavaLoppupvm = value;
                }

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
             *         &lt;element name="Ytunnus" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}YtunnusTyyppi"/&gt;
             *         &lt;element name="Nimi" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}YritysNimi80Tyyppi"/&gt;
             *         &lt;element name="TehtavaLoppupvm" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
             *         &lt;element name="TehtavaAlkupvm" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
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
                "ytunnus",
                "nimi",
                "tehtavaLoppupvm",
                "tehtavaAlkupvm"
            })
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public static class YritysJaYhteisoEdunvalvoja {

                @XmlElement(name = "Ytunnus", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
                @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
                protected String ytunnus;
                @XmlElement(name = "Nimi", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
                @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
                protected String nimi;
                @XmlElement(name = "TehtavaLoppupvm", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
                @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
                protected String tehtavaLoppupvm;
                @XmlElement(name = "TehtavaAlkupvm", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
                @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
                protected String tehtavaAlkupvm;

                /**
                 * Gets the value of the ytunnus property.
                 * 
                 * @return
                 *     possible object is
                 *     {@link String }
                 *     
                 */
                @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
                public String getYtunnus() {
                    return ytunnus;
                }

                /**
                 * Sets the value of the ytunnus property.
                 * 
                 * @param value
                 *     allowed object is
                 *     {@link String }
                 *     
                 */
                @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
                public void setYtunnus(String value) {
                    this.ytunnus = value;
                }

                /**
                 * Gets the value of the nimi property.
                 * 
                 * @return
                 *     possible object is
                 *     {@link String }
                 *     
                 */
                @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
                public String getNimi() {
                    return nimi;
                }

                /**
                 * Sets the value of the nimi property.
                 * 
                 * @param value
                 *     allowed object is
                 *     {@link String }
                 *     
                 */
                @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
                public void setNimi(String value) {
                    this.nimi = value;
                }

                /**
                 * Gets the value of the tehtavaLoppupvm property.
                 * 
                 * @return
                 *     possible object is
                 *     {@link String }
                 *     
                 */
                @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
                public String getTehtavaLoppupvm() {
                    return tehtavaLoppupvm;
                }

                /**
                 * Sets the value of the tehtavaLoppupvm property.
                 * 
                 * @param value
                 *     allowed object is
                 *     {@link String }
                 *     
                 */
                @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
                public void setTehtavaLoppupvm(String value) {
                    this.tehtavaLoppupvm = value;
                }

                /**
                 * Gets the value of the tehtavaAlkupvm property.
                 * 
                 * @return
                 *     possible object is
                 *     {@link String }
                 *     
                 */
                @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
                public String getTehtavaAlkupvm() {
                    return tehtavaAlkupvm;
                }

                /**
                 * Sets the value of the tehtavaAlkupvm property.
                 * 
                 * @param value
                 *     allowed object is
                 *     {@link String }
                 *     
                 */
                @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
                public void setTehtavaAlkupvm(String value) {
                    this.tehtavaAlkupvm = value;
                }

            }

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
         *         &lt;element name="Alkupvm" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
         *         &lt;element name="Paattymispvm" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
         *         &lt;element name="Tehtavienjakokoodi" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}TehtavienjakokoodiTyyppi"/&gt;
         *         &lt;element name="TehtavienjakoS" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}Teksti100Tyyppi"/&gt;
         *         &lt;element name="TehtavienjakoR" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}Teksti100Tyyppi"/&gt;
         *         &lt;element name="HenkiloEdunvalvontavaltuutettu" maxOccurs="unbounded"&gt;
         *           &lt;complexType&gt;
         *             &lt;complexContent&gt;
         *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
         *                 &lt;sequence&gt;
         *                   &lt;element name="Henkilotunnus" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}HenkilotunnusTyyppi"/&gt;
         *                   &lt;element name="Syntymaaika" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
         *                   &lt;element name="NykyinenSukunimi"&gt;
         *                     &lt;complexType&gt;
         *                       &lt;complexContent&gt;
         *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
         *                           &lt;sequence&gt;
         *                             &lt;element name="Sukunimi" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}Nimi100Tyyppi"/&gt;
         *                           &lt;/sequence&gt;
         *                         &lt;/restriction&gt;
         *                       &lt;/complexContent&gt;
         *                     &lt;/complexType&gt;
         *                   &lt;/element&gt;
         *                   &lt;element name="NykyisetEtunimet"&gt;
         *                     &lt;complexType&gt;
         *                       &lt;complexContent&gt;
         *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
         *                           &lt;sequence&gt;
         *                             &lt;element name="Etunimet" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}Nimi100Tyyppi"/&gt;
         *                           &lt;/sequence&gt;
         *                         &lt;/restriction&gt;
         *                       &lt;/complexContent&gt;
         *                     &lt;/complexType&gt;
         *                   &lt;/element&gt;
         *                   &lt;element name="ValtuutusAlkupvm" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
         *                   &lt;element name="ValtuutusLoppupvm" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
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
        @XmlType(name = "", propOrder = {
            "alkupvm",
            "paattymispvm",
            "tehtavienjakokoodi",
            "tehtavienjakoS",
            "tehtavienjakoR",
            "henkiloEdunvalvontavaltuutettu"
        })
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        public static class Edunvalvontavaltuutus {

            @XmlElement(name = "Alkupvm", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected String alkupvm;
            @XmlElement(name = "Paattymispvm", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected String paattymispvm;
            @XmlElement(name = "Tehtavienjakokoodi", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected String tehtavienjakokoodi;
            @XmlElement(name = "TehtavienjakoS", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected String tehtavienjakoS;
            @XmlElement(name = "TehtavienjakoR", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected String tehtavienjakoR;
            @XmlElement(name = "HenkiloEdunvalvontavaltuutettu", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected List<VTJHenkiloVastaussanoma.Henkilo.Edunvalvontavaltuutus.HenkiloEdunvalvontavaltuutettu> henkiloEdunvalvontavaltuutettu;

            /**
             * Gets the value of the alkupvm property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public String getAlkupvm() {
                return alkupvm;
            }

            /**
             * Sets the value of the alkupvm property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public void setAlkupvm(String value) {
                this.alkupvm = value;
            }

            /**
             * Gets the value of the paattymispvm property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public String getPaattymispvm() {
                return paattymispvm;
            }

            /**
             * Sets the value of the paattymispvm property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public void setPaattymispvm(String value) {
                this.paattymispvm = value;
            }

            /**
             * Gets the value of the tehtavienjakokoodi property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public String getTehtavienjakokoodi() {
                return tehtavienjakokoodi;
            }

            /**
             * Sets the value of the tehtavienjakokoodi property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public void setTehtavienjakokoodi(String value) {
                this.tehtavienjakokoodi = value;
            }

            /**
             * Gets the value of the tehtavienjakoS property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public String getTehtavienjakoS() {
                return tehtavienjakoS;
            }

            /**
             * Sets the value of the tehtavienjakoS property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public void setTehtavienjakoS(String value) {
                this.tehtavienjakoS = value;
            }

            /**
             * Gets the value of the tehtavienjakoR property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public String getTehtavienjakoR() {
                return tehtavienjakoR;
            }

            /**
             * Sets the value of the tehtavienjakoR property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public void setTehtavienjakoR(String value) {
                this.tehtavienjakoR = value;
            }

            /**
             * Gets the value of the henkiloEdunvalvontavaltuutettu property.
             * 
             * <p>
             * This accessor method returns a reference to the live list,
             * not a snapshot. Therefore any modification you make to the
             * returned list will be present inside the JAXB object.
             * This is why there is not a <CODE>set</CODE> method for the henkiloEdunvalvontavaltuutettu property.
             * 
             * <p>
             * For example, to add a new item, do as follows:
             * <pre>
             *    getHenkiloEdunvalvontavaltuutettu().add(newItem);
             * </pre>
             * 
             * 
             * <p>
             * Objects of the following type(s) are allowed in the list
             * {@link VTJHenkiloVastaussanoma.Henkilo.Edunvalvontavaltuutus.HenkiloEdunvalvontavaltuutettu }
             * 
             * 
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public List<VTJHenkiloVastaussanoma.Henkilo.Edunvalvontavaltuutus.HenkiloEdunvalvontavaltuutettu> getHenkiloEdunvalvontavaltuutettu() {
                if (henkiloEdunvalvontavaltuutettu == null) {
                    henkiloEdunvalvontavaltuutettu = new ArrayList<VTJHenkiloVastaussanoma.Henkilo.Edunvalvontavaltuutus.HenkiloEdunvalvontavaltuutettu>();
                }
                return this.henkiloEdunvalvontavaltuutettu;
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
             *         &lt;element name="Henkilotunnus" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}HenkilotunnusTyyppi"/&gt;
             *         &lt;element name="Syntymaaika" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
             *         &lt;element name="NykyinenSukunimi"&gt;
             *           &lt;complexType&gt;
             *             &lt;complexContent&gt;
             *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
             *                 &lt;sequence&gt;
             *                   &lt;element name="Sukunimi" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}Nimi100Tyyppi"/&gt;
             *                 &lt;/sequence&gt;
             *               &lt;/restriction&gt;
             *             &lt;/complexContent&gt;
             *           &lt;/complexType&gt;
             *         &lt;/element&gt;
             *         &lt;element name="NykyisetEtunimet"&gt;
             *           &lt;complexType&gt;
             *             &lt;complexContent&gt;
             *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
             *                 &lt;sequence&gt;
             *                   &lt;element name="Etunimet" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}Nimi100Tyyppi"/&gt;
             *                 &lt;/sequence&gt;
             *               &lt;/restriction&gt;
             *             &lt;/complexContent&gt;
             *           &lt;/complexType&gt;
             *         &lt;/element&gt;
             *         &lt;element name="ValtuutusAlkupvm" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
             *         &lt;element name="ValtuutusLoppupvm" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
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
                "henkilotunnus",
                "syntymaaika",
                "nykyinenSukunimi",
                "nykyisetEtunimet",
                "valtuutusAlkupvm",
                "valtuutusLoppupvm"
            })
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public static class HenkiloEdunvalvontavaltuutettu {

                @XmlElement(name = "Henkilotunnus", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
                @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
                protected String henkilotunnus;
                @XmlElement(name = "Syntymaaika", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
                @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
                protected String syntymaaika;
                @XmlElement(name = "NykyinenSukunimi", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
                @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
                protected VTJHenkiloVastaussanoma.Henkilo.Edunvalvontavaltuutus.HenkiloEdunvalvontavaltuutettu.NykyinenSukunimi nykyinenSukunimi;
                @XmlElement(name = "NykyisetEtunimet", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
                @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
                protected VTJHenkiloVastaussanoma.Henkilo.Edunvalvontavaltuutus.HenkiloEdunvalvontavaltuutettu.NykyisetEtunimet nykyisetEtunimet;
                @XmlElement(name = "ValtuutusAlkupvm", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
                @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
                protected String valtuutusAlkupvm;
                @XmlElement(name = "ValtuutusLoppupvm", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
                @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
                protected String valtuutusLoppupvm;

                /**
                 * Gets the value of the henkilotunnus property.
                 * 
                 * @return
                 *     possible object is
                 *     {@link String }
                 *     
                 */
                @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
                public String getHenkilotunnus() {
                    return henkilotunnus;
                }

                /**
                 * Sets the value of the henkilotunnus property.
                 * 
                 * @param value
                 *     allowed object is
                 *     {@link String }
                 *     
                 */
                @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
                public void setHenkilotunnus(String value) {
                    this.henkilotunnus = value;
                }

                /**
                 * Gets the value of the syntymaaika property.
                 * 
                 * @return
                 *     possible object is
                 *     {@link String }
                 *     
                 */
                @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
                public String getSyntymaaika() {
                    return syntymaaika;
                }

                /**
                 * Sets the value of the syntymaaika property.
                 * 
                 * @param value
                 *     allowed object is
                 *     {@link String }
                 *     
                 */
                @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
                public void setSyntymaaika(String value) {
                    this.syntymaaika = value;
                }

                /**
                 * Gets the value of the nykyinenSukunimi property.
                 * 
                 * @return
                 *     possible object is
                 *     {@link VTJHenkiloVastaussanoma.Henkilo.Edunvalvontavaltuutus.HenkiloEdunvalvontavaltuutettu.NykyinenSukunimi }
                 *     
                 */
                @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
                public VTJHenkiloVastaussanoma.Henkilo.Edunvalvontavaltuutus.HenkiloEdunvalvontavaltuutettu.NykyinenSukunimi getNykyinenSukunimi() {
                    return nykyinenSukunimi;
                }

                /**
                 * Sets the value of the nykyinenSukunimi property.
                 * 
                 * @param value
                 *     allowed object is
                 *     {@link VTJHenkiloVastaussanoma.Henkilo.Edunvalvontavaltuutus.HenkiloEdunvalvontavaltuutettu.NykyinenSukunimi }
                 *     
                 */
                @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
                public void setNykyinenSukunimi(VTJHenkiloVastaussanoma.Henkilo.Edunvalvontavaltuutus.HenkiloEdunvalvontavaltuutettu.NykyinenSukunimi value) {
                    this.nykyinenSukunimi = value;
                }

                /**
                 * Gets the value of the nykyisetEtunimet property.
                 * 
                 * @return
                 *     possible object is
                 *     {@link VTJHenkiloVastaussanoma.Henkilo.Edunvalvontavaltuutus.HenkiloEdunvalvontavaltuutettu.NykyisetEtunimet }
                 *     
                 */
                @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
                public VTJHenkiloVastaussanoma.Henkilo.Edunvalvontavaltuutus.HenkiloEdunvalvontavaltuutettu.NykyisetEtunimet getNykyisetEtunimet() {
                    return nykyisetEtunimet;
                }

                /**
                 * Sets the value of the nykyisetEtunimet property.
                 * 
                 * @param value
                 *     allowed object is
                 *     {@link VTJHenkiloVastaussanoma.Henkilo.Edunvalvontavaltuutus.HenkiloEdunvalvontavaltuutettu.NykyisetEtunimet }
                 *     
                 */
                @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
                public void setNykyisetEtunimet(VTJHenkiloVastaussanoma.Henkilo.Edunvalvontavaltuutus.HenkiloEdunvalvontavaltuutettu.NykyisetEtunimet value) {
                    this.nykyisetEtunimet = value;
                }

                /**
                 * Gets the value of the valtuutusAlkupvm property.
                 * 
                 * @return
                 *     possible object is
                 *     {@link String }
                 *     
                 */
                @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
                public String getValtuutusAlkupvm() {
                    return valtuutusAlkupvm;
                }

                /**
                 * Sets the value of the valtuutusAlkupvm property.
                 * 
                 * @param value
                 *     allowed object is
                 *     {@link String }
                 *     
                 */
                @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
                public void setValtuutusAlkupvm(String value) {
                    this.valtuutusAlkupvm = value;
                }

                /**
                 * Gets the value of the valtuutusLoppupvm property.
                 * 
                 * @return
                 *     possible object is
                 *     {@link String }
                 *     
                 */
                @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
                public String getValtuutusLoppupvm() {
                    return valtuutusLoppupvm;
                }

                /**
                 * Sets the value of the valtuutusLoppupvm property.
                 * 
                 * @param value
                 *     allowed object is
                 *     {@link String }
                 *     
                 */
                @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
                public void setValtuutusLoppupvm(String value) {
                    this.valtuutusLoppupvm = value;
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
                 *         &lt;element name="Sukunimi" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}Nimi100Tyyppi"/&gt;
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
                    "sukunimi"
                })
                @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
                public static class NykyinenSukunimi {

                    @XmlElement(name = "Sukunimi", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
                    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
                    protected String sukunimi;

                    /**
                     * Gets the value of the sukunimi property.
                     * 
                     * @return
                     *     possible object is
                     *     {@link String }
                     *     
                     */
                    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
                    public String getSukunimi() {
                        return sukunimi;
                    }

                    /**
                     * Sets the value of the sukunimi property.
                     * 
                     * @param value
                     *     allowed object is
                     *     {@link String }
                     *     
                     */
                    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
                    public void setSukunimi(String value) {
                        this.sukunimi = value;
                    }

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
                 *         &lt;element name="Etunimet" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}Nimi100Tyyppi"/&gt;
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
                    "etunimet"
                })
                @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
                public static class NykyisetEtunimet {

                    @XmlElement(name = "Etunimet", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
                    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
                    protected String etunimet;

                    /**
                     * Gets the value of the etunimet property.
                     * 
                     * @return
                     *     possible object is
                     *     {@link String }
                     *     
                     */
                    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
                    public String getEtunimet() {
                        return etunimet;
                    }

                    /**
                     * Sets the value of the etunimet property.
                     * 
                     * @param value
                     *     allowed object is
                     *     {@link String }
                     *     
                     */
                    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
                    public void setEtunimet(String value) {
                        this.etunimet = value;
                    }

                }

            }

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
         *         &lt;element name="Nimi" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}Nimi100Tyyppi"/&gt;
         *         &lt;element name="Nimilajikoodi" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}EntinenNimiLajikoodiTyyppi"/&gt;
         *         &lt;element name="Alkupvm" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
         *         &lt;element name="Loppupvm" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
         *         &lt;element name="Info8S" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}Teksti100Tyyppi" minOccurs="0"/&gt;
         *         &lt;element name="Info8R" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}Teksti100Tyyppi" minOccurs="0"/&gt;
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
            "nimi",
            "nimilajikoodi",
            "alkupvm",
            "loppupvm",
            "info8S",
            "info8R"
        })
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        public static class EntinenNimi {

            @XmlElement(name = "Nimi", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected String nimi;
            @XmlElement(name = "Nimilajikoodi", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected String nimilajikoodi;
            @XmlElement(name = "Alkupvm", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected String alkupvm;
            @XmlElement(name = "Loppupvm", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected String loppupvm;
            @XmlElement(name = "Info8S", namespace = "http://xml.vrk.fi/schema/vtjkysely")
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected String info8S;
            @XmlElement(name = "Info8R", namespace = "http://xml.vrk.fi/schema/vtjkysely")
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected String info8R;

            /**
             * Gets the value of the nimi property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public String getNimi() {
                return nimi;
            }

            /**
             * Sets the value of the nimi property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public void setNimi(String value) {
                this.nimi = value;
            }

            /**
             * Gets the value of the nimilajikoodi property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public String getNimilajikoodi() {
                return nimilajikoodi;
            }

            /**
             * Sets the value of the nimilajikoodi property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public void setNimilajikoodi(String value) {
                this.nimilajikoodi = value;
            }

            /**
             * Gets the value of the alkupvm property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public String getAlkupvm() {
                return alkupvm;
            }

            /**
             * Sets the value of the alkupvm property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public void setAlkupvm(String value) {
                this.alkupvm = value;
            }

            /**
             * Gets the value of the loppupvm property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public String getLoppupvm() {
                return loppupvm;
            }

            /**
             * Sets the value of the loppupvm property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public void setLoppupvm(String value) {
                this.loppupvm = value;
            }

            /**
             * Gets the value of the info8S property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public String getInfo8S() {
                return info8S;
            }

            /**
             * Sets the value of the info8S property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public void setInfo8S(String value) {
                this.info8S = value;
            }

            /**
             * Gets the value of the info8R property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public String getInfo8R() {
                return info8R;
            }

            /**
             * Sets the value of the info8R property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public void setInfo8R(String value) {
                this.info8R = value;
            }

        }


        /**
         * <p>Java class for anonymous complex type.
         * 
         * <p>The following schema fragment specifies the expected content contained within this class.
         * 
         * <pre>
         * &lt;complexType&gt;
         *   &lt;simpleContent&gt;
         *     &lt;extension base="&lt;http://xml.vrk.fi/schema/vtj/henkilotiedot/1&gt;HenkilotunnusTyyppi"&gt;
         *       &lt;attribute name="voimassaolokoodi" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}VoimassaolokoodiTyyppi" /&gt;
         *     &lt;/extension&gt;
         *   &lt;/simpleContent&gt;
         * &lt;/complexType&gt;
         * </pre>
         * 
         * 
         */
        @XmlAccessorType(XmlAccessType.FIELD)
        @XmlType(name = "", propOrder = {
            "value"
        })
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        public static class Henkilotunnus {

            @XmlValue
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected String value;
            @XmlAttribute(name = "voimassaolokoodi")
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected String voimassaolokoodi;

            /**
             * Muoto 11 merkkia, syntymaaika ppkkvv, syntymavuosisadan ilmaiseva valimerkki [- tai + tai A], yksilonumero (3 numeroa), tarkistusmerkki (ABCDEFHJKLMNPRSTUVWXY tai numero)= pp paiva arvo valilta 01-31, kk kuukausi arvo valilta 01-12, vv vuosi 2 numeroa eli 00-99, yhden kerran - tai + tai A, 3 numeroa, 1 iso kirjain joukosta ABCDEFHJKLMNPRSTUVWXY tai numero. Voi olla myos tyhja.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public String getValue() {
                return value;
            }

            /**
             * Sets the value of the value property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public void setValue(String value) {
                this.value = value;
            }

            /**
             * Gets the value of the voimassaolokoodi property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public String getVoimassaolokoodi() {
                return voimassaolokoodi;
            }

            /**
             * Sets the value of the voimassaolokoodi property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public void setVoimassaolokoodi(String value) {
                this.voimassaolokoodi = value;
            }

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
         *         &lt;element name="Henkilotunnus" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}HenkilotunnusTyyppi"/&gt;
         *         &lt;element name="Syntymaaika" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
         *         &lt;element name="NykyinenSukunimi"&gt;
         *           &lt;complexType&gt;
         *             &lt;complexContent&gt;
         *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
         *                 &lt;sequence&gt;
         *                   &lt;element name="Sukunimi" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}Nimi100Tyyppi"/&gt;
         *                 &lt;/sequence&gt;
         *               &lt;/restriction&gt;
         *             &lt;/complexContent&gt;
         *           &lt;/complexType&gt;
         *         &lt;/element&gt;
         *         &lt;element name="NykyisetEtunimet"&gt;
         *           &lt;complexType&gt;
         *             &lt;complexContent&gt;
         *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
         *                 &lt;sequence&gt;
         *                   &lt;element name="Etunimet" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}Nimi100Tyyppi"/&gt;
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
        @XmlType(name = "", propOrder = {
            "henkilotunnus",
            "syntymaaika",
            "nykyinenSukunimi",
            "nykyisetEtunimet"
        })
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        public static class Huollettava {

            @XmlElement(name = "Henkilotunnus", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected String henkilotunnus;
            @XmlElement(name = "Syntymaaika", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected String syntymaaika;
            @XmlElement(name = "NykyinenSukunimi", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected VTJHenkiloVastaussanoma.Henkilo.Huollettava.NykyinenSukunimi nykyinenSukunimi;
            @XmlElement(name = "NykyisetEtunimet", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected VTJHenkiloVastaussanoma.Henkilo.Huollettava.NykyisetEtunimet nykyisetEtunimet;

            /**
             * Gets the value of the henkilotunnus property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public String getHenkilotunnus() {
                return henkilotunnus;
            }

            /**
             * Sets the value of the henkilotunnus property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public void setHenkilotunnus(String value) {
                this.henkilotunnus = value;
            }

            /**
             * Gets the value of the syntymaaika property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public String getSyntymaaika() {
                return syntymaaika;
            }

            /**
             * Sets the value of the syntymaaika property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public void setSyntymaaika(String value) {
                this.syntymaaika = value;
            }

            /**
             * Gets the value of the nykyinenSukunimi property.
             * 
             * @return
             *     possible object is
             *     {@link VTJHenkiloVastaussanoma.Henkilo.Huollettava.NykyinenSukunimi }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public VTJHenkiloVastaussanoma.Henkilo.Huollettava.NykyinenSukunimi getNykyinenSukunimi() {
                return nykyinenSukunimi;
            }

            /**
             * Sets the value of the nykyinenSukunimi property.
             * 
             * @param value
             *     allowed object is
             *     {@link VTJHenkiloVastaussanoma.Henkilo.Huollettava.NykyinenSukunimi }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public void setNykyinenSukunimi(VTJHenkiloVastaussanoma.Henkilo.Huollettava.NykyinenSukunimi value) {
                this.nykyinenSukunimi = value;
            }

            /**
             * Gets the value of the nykyisetEtunimet property.
             * 
             * @return
             *     possible object is
             *     {@link VTJHenkiloVastaussanoma.Henkilo.Huollettava.NykyisetEtunimet }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public VTJHenkiloVastaussanoma.Henkilo.Huollettava.NykyisetEtunimet getNykyisetEtunimet() {
                return nykyisetEtunimet;
            }

            /**
             * Sets the value of the nykyisetEtunimet property.
             * 
             * @param value
             *     allowed object is
             *     {@link VTJHenkiloVastaussanoma.Henkilo.Huollettava.NykyisetEtunimet }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public void setNykyisetEtunimet(VTJHenkiloVastaussanoma.Henkilo.Huollettava.NykyisetEtunimet value) {
                this.nykyisetEtunimet = value;
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
             *         &lt;element name="Sukunimi" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}Nimi100Tyyppi"/&gt;
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
                "sukunimi"
            })
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public static class NykyinenSukunimi {

                @XmlElement(name = "Sukunimi", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
                @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
                protected String sukunimi;

                /**
                 * Gets the value of the sukunimi property.
                 * 
                 * @return
                 *     possible object is
                 *     {@link String }
                 *     
                 */
                @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
                public String getSukunimi() {
                    return sukunimi;
                }

                /**
                 * Sets the value of the sukunimi property.
                 * 
                 * @param value
                 *     allowed object is
                 *     {@link String }
                 *     
                 */
                @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
                public void setSukunimi(String value) {
                    this.sukunimi = value;
                }

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
             *         &lt;element name="Etunimet" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}Nimi100Tyyppi"/&gt;
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
                "etunimet"
            })
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public static class NykyisetEtunimet {

                @XmlElement(name = "Etunimet", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
                @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
                protected String etunimet;

                /**
                 * Gets the value of the etunimet property.
                 * 
                 * @return
                 *     possible object is
                 *     {@link String }
                 *     
                 */
                @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
                public String getEtunimet() {
                    return etunimet;
                }

                /**
                 * Sets the value of the etunimet property.
                 * 
                 * @param value
                 *     allowed object is
                 *     {@link String }
                 *     
                 */
                @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
                public void setEtunimet(String value) {
                    this.etunimet = value;
                }

            }

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
         *         &lt;element name="Henkilotunnus" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}HenkilotunnusTyyppi"/&gt;
         *         &lt;element name="Syntymaaika" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
         *         &lt;element name="NykyinenSukunimi"&gt;
         *           &lt;complexType&gt;
         *             &lt;complexContent&gt;
         *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
         *                 &lt;sequence&gt;
         *                   &lt;element name="Sukunimi" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}Nimi100Tyyppi"/&gt;
         *                 &lt;/sequence&gt;
         *               &lt;/restriction&gt;
         *             &lt;/complexContent&gt;
         *           &lt;/complexType&gt;
         *         &lt;/element&gt;
         *         &lt;element name="NykyisetEtunimet"&gt;
         *           &lt;complexType&gt;
         *             &lt;complexContent&gt;
         *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
         *                 &lt;sequence&gt;
         *                   &lt;element name="Etunimet" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}Nimi100Tyyppi"/&gt;
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
        @XmlType(name = "", propOrder = {
            "henkilotunnus",
            "syntymaaika",
            "nykyinenSukunimi",
            "nykyisetEtunimet"
        })
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        public static class Huoltaja {

            @XmlElement(name = "Henkilotunnus", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected String henkilotunnus;
            @XmlElement(name = "Syntymaaika", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected String syntymaaika;
            @XmlElement(name = "NykyinenSukunimi", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected VTJHenkiloVastaussanoma.Henkilo.Huoltaja.NykyinenSukunimi nykyinenSukunimi;
            @XmlElement(name = "NykyisetEtunimet", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected VTJHenkiloVastaussanoma.Henkilo.Huoltaja.NykyisetEtunimet nykyisetEtunimet;

            /**
             * Gets the value of the henkilotunnus property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public String getHenkilotunnus() {
                return henkilotunnus;
            }

            /**
             * Sets the value of the henkilotunnus property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public void setHenkilotunnus(String value) {
                this.henkilotunnus = value;
            }

            /**
             * Gets the value of the syntymaaika property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public String getSyntymaaika() {
                return syntymaaika;
            }

            /**
             * Sets the value of the syntymaaika property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public void setSyntymaaika(String value) {
                this.syntymaaika = value;
            }

            /**
             * Gets the value of the nykyinenSukunimi property.
             * 
             * @return
             *     possible object is
             *     {@link VTJHenkiloVastaussanoma.Henkilo.Huoltaja.NykyinenSukunimi }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public VTJHenkiloVastaussanoma.Henkilo.Huoltaja.NykyinenSukunimi getNykyinenSukunimi() {
                return nykyinenSukunimi;
            }

            /**
             * Sets the value of the nykyinenSukunimi property.
             * 
             * @param value
             *     allowed object is
             *     {@link VTJHenkiloVastaussanoma.Henkilo.Huoltaja.NykyinenSukunimi }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public void setNykyinenSukunimi(VTJHenkiloVastaussanoma.Henkilo.Huoltaja.NykyinenSukunimi value) {
                this.nykyinenSukunimi = value;
            }

            /**
             * Gets the value of the nykyisetEtunimet property.
             * 
             * @return
             *     possible object is
             *     {@link VTJHenkiloVastaussanoma.Henkilo.Huoltaja.NykyisetEtunimet }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public VTJHenkiloVastaussanoma.Henkilo.Huoltaja.NykyisetEtunimet getNykyisetEtunimet() {
                return nykyisetEtunimet;
            }

            /**
             * Sets the value of the nykyisetEtunimet property.
             * 
             * @param value
             *     allowed object is
             *     {@link VTJHenkiloVastaussanoma.Henkilo.Huoltaja.NykyisetEtunimet }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public void setNykyisetEtunimet(VTJHenkiloVastaussanoma.Henkilo.Huoltaja.NykyisetEtunimet value) {
                this.nykyisetEtunimet = value;
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
             *         &lt;element name="Sukunimi" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}Nimi100Tyyppi"/&gt;
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
                "sukunimi"
            })
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public static class NykyinenSukunimi {

                @XmlElement(name = "Sukunimi", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
                @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
                protected String sukunimi;

                /**
                 * Gets the value of the sukunimi property.
                 * 
                 * @return
                 *     possible object is
                 *     {@link String }
                 *     
                 */
                @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
                public String getSukunimi() {
                    return sukunimi;
                }

                /**
                 * Sets the value of the sukunimi property.
                 * 
                 * @param value
                 *     allowed object is
                 *     {@link String }
                 *     
                 */
                @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
                public void setSukunimi(String value) {
                    this.sukunimi = value;
                }

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
             *         &lt;element name="Etunimet" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}Nimi100Tyyppi"/&gt;
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
                "etunimet"
            })
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public static class NykyisetEtunimet {

                @XmlElement(name = "Etunimet", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
                @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
                protected String etunimet;

                /**
                 * Gets the value of the etunimet property.
                 * 
                 * @return
                 *     possible object is
                 *     {@link String }
                 *     
                 */
                @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
                public String getEtunimet() {
                    return etunimet;
                }

                /**
                 * Sets the value of the etunimet property.
                 * 
                 * @param value
                 *     allowed object is
                 *     {@link String }
                 *     
                 */
                @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
                public void setEtunimet(String value) {
                    this.etunimet = value;
                }

            }

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
         *         &lt;element name="Kansalaisuuskoodi3" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}Valtiokoodi3Tyyppi"/&gt;
         *         &lt;element name="KansalaisuusS" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}ValtioNimiTyyppi"/&gt;
         *         &lt;element name="KansalaisuusR" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}ValtioNimiTyyppi"/&gt;
         *         &lt;element name="KansalaisuusSelvakielinen" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}ValtioNimi30Tyyppi"/&gt;
         *         &lt;element name="Saamispvm" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
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
            "kansalaisuuskoodi3",
            "kansalaisuusS",
            "kansalaisuusR",
            "kansalaisuusSelvakielinen",
            "saamispvm"
        })
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        public static class Kansalaisuus {

            @XmlElement(name = "Kansalaisuuskoodi3", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected String kansalaisuuskoodi3;
            @XmlElement(name = "KansalaisuusS", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected String kansalaisuusS;
            @XmlElement(name = "KansalaisuusR", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected String kansalaisuusR;
            @XmlElement(name = "KansalaisuusSelvakielinen", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected String kansalaisuusSelvakielinen;
            @XmlElement(name = "Saamispvm", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected String saamispvm;

            /**
             * Gets the value of the kansalaisuuskoodi3 property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public String getKansalaisuuskoodi3() {
                return kansalaisuuskoodi3;
            }

            /**
             * Sets the value of the kansalaisuuskoodi3 property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public void setKansalaisuuskoodi3(String value) {
                this.kansalaisuuskoodi3 = value;
            }

            /**
             * Gets the value of the kansalaisuusS property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public String getKansalaisuusS() {
                return kansalaisuusS;
            }

            /**
             * Sets the value of the kansalaisuusS property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public void setKansalaisuusS(String value) {
                this.kansalaisuusS = value;
            }

            /**
             * Gets the value of the kansalaisuusR property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public String getKansalaisuusR() {
                return kansalaisuusR;
            }

            /**
             * Sets the value of the kansalaisuusR property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public void setKansalaisuusR(String value) {
                this.kansalaisuusR = value;
            }

            /**
             * Gets the value of the kansalaisuusSelvakielinen property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public String getKansalaisuusSelvakielinen() {
                return kansalaisuusSelvakielinen;
            }

            /**
             * Sets the value of the kansalaisuusSelvakielinen property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public void setKansalaisuusSelvakielinen(String value) {
                this.kansalaisuusSelvakielinen = value;
            }

            /**
             * Gets the value of the saamispvm property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public String getSaamispvm() {
                return saamispvm;
            }

            /**
             * Sets the value of the saamispvm property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public void setSaamispvm(String value) {
                this.saamispvm = value;
            }

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
         *         &lt;element name="Kuntanumero" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}KuntanumeroTyyppi"/&gt;
         *         &lt;element name="KuntaS" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}KuntaNimiTyyppi"/&gt;
         *         &lt;element name="KuntaR" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}KuntaNimiTyyppi"/&gt;
         *         &lt;element name="KuntasuhdeAlkupvm" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
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
            "kuntanumero",
            "kuntaS",
            "kuntaR",
            "kuntasuhdeAlkupvm"
        })
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        public static class Kotikunta {

            @XmlElement(name = "Kuntanumero", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected String kuntanumero;
            @XmlElement(name = "KuntaS", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected String kuntaS;
            @XmlElement(name = "KuntaR", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected String kuntaR;
            @XmlElement(name = "KuntasuhdeAlkupvm", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected String kuntasuhdeAlkupvm;

            /**
             * Gets the value of the kuntanumero property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public String getKuntanumero() {
                return kuntanumero;
            }

            /**
             * Sets the value of the kuntanumero property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public void setKuntanumero(String value) {
                this.kuntanumero = value;
            }

            /**
             * Gets the value of the kuntaS property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public String getKuntaS() {
                return kuntaS;
            }

            /**
             * Sets the value of the kuntaS property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public void setKuntaS(String value) {
                this.kuntaS = value;
            }

            /**
             * Gets the value of the kuntaR property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public String getKuntaR() {
                return kuntaR;
            }

            /**
             * Sets the value of the kuntaR property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public void setKuntaR(String value) {
                this.kuntaR = value;
            }

            /**
             * Gets the value of the kuntasuhdeAlkupvm property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public String getKuntasuhdeAlkupvm() {
                return kuntasuhdeAlkupvm;
            }

            /**
             * Sets the value of the kuntasuhdeAlkupvm property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public void setKuntasuhdeAlkupvm(String value) {
                this.kuntasuhdeAlkupvm = value;
            }

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
         *         &lt;element name="PostiosoiteS" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PostiosoiteTyyppi"/&gt;
         *         &lt;element name="PostiosoiteR" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PostiosoiteTyyppi"/&gt;
         *         &lt;element name="Postinumero" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PostinumeroTyyppi"/&gt;
         *         &lt;element name="PostitoimipaikkaS" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PostitoimipaikkaTyyppi"/&gt;
         *         &lt;element name="PostitoimipaikkaR" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PostitoimipaikkaTyyppi"/&gt;
         *         &lt;element name="PostiosoiteAlkupvm" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
         *         &lt;element name="PostiosoiteLoppupvm" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
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
            "postiosoiteS",
            "postiosoiteR",
            "postinumero",
            "postitoimipaikkaS",
            "postitoimipaikkaR",
            "postiosoiteAlkupvm",
            "postiosoiteLoppupvm"
        })
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        public static class KotimainenPostiosoite {

            @XmlElement(name = "PostiosoiteS", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected String postiosoiteS;
            @XmlElement(name = "PostiosoiteR", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected String postiosoiteR;
            @XmlElement(name = "Postinumero", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected String postinumero;
            @XmlElement(name = "PostitoimipaikkaS", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected String postitoimipaikkaS;
            @XmlElement(name = "PostitoimipaikkaR", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected String postitoimipaikkaR;
            @XmlElement(name = "PostiosoiteAlkupvm", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected String postiosoiteAlkupvm;
            @XmlElement(name = "PostiosoiteLoppupvm", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected String postiosoiteLoppupvm;

            /**
             * Gets the value of the postiosoiteS property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public String getPostiosoiteS() {
                return postiosoiteS;
            }

            /**
             * Sets the value of the postiosoiteS property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public void setPostiosoiteS(String value) {
                this.postiosoiteS = value;
            }

            /**
             * Gets the value of the postiosoiteR property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public String getPostiosoiteR() {
                return postiosoiteR;
            }

            /**
             * Sets the value of the postiosoiteR property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public void setPostiosoiteR(String value) {
                this.postiosoiteR = value;
            }

            /**
             * Gets the value of the postinumero property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public String getPostinumero() {
                return postinumero;
            }

            /**
             * Sets the value of the postinumero property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public void setPostinumero(String value) {
                this.postinumero = value;
            }

            /**
             * Gets the value of the postitoimipaikkaS property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public String getPostitoimipaikkaS() {
                return postitoimipaikkaS;
            }

            /**
             * Sets the value of the postitoimipaikkaS property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public void setPostitoimipaikkaS(String value) {
                this.postitoimipaikkaS = value;
            }

            /**
             * Gets the value of the postitoimipaikkaR property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public String getPostitoimipaikkaR() {
                return postitoimipaikkaR;
            }

            /**
             * Sets the value of the postitoimipaikkaR property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public void setPostitoimipaikkaR(String value) {
                this.postitoimipaikkaR = value;
            }

            /**
             * Gets the value of the postiosoiteAlkupvm property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public String getPostiosoiteAlkupvm() {
                return postiosoiteAlkupvm;
            }

            /**
             * Sets the value of the postiosoiteAlkupvm property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public void setPostiosoiteAlkupvm(String value) {
                this.postiosoiteAlkupvm = value;
            }

            /**
             * Gets the value of the postiosoiteLoppupvm property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public String getPostiosoiteLoppupvm() {
                return postiosoiteLoppupvm;
            }

            /**
             * Sets the value of the postiosoiteLoppupvm property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public void setPostiosoiteLoppupvm(String value) {
                this.postiosoiteLoppupvm = value;
            }

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
         *         &lt;element name="Kuollut" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}KuollutTyyppi"/&gt;
         *         &lt;element name="Kuolinpvm" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
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
            "kuollut",
            "kuolinpvm"
        })
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        public static class Kuolintiedot {

            @XmlElement(name = "Kuollut", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected String kuollut;
            @XmlElement(name = "Kuolinpvm", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected String kuolinpvm;

            /**
             * Gets the value of the kuollut property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public String getKuollut() {
                return kuollut;
            }

            /**
             * Sets the value of the kuollut property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public void setKuollut(String value) {
                this.kuollut = value;
            }

            /**
             * Gets the value of the kuolinpvm property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public String getKuolinpvm() {
                return kuolinpvm;
            }

            /**
             * Sets the value of the kuolinpvm property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public void setKuolinpvm(String value) {
                this.kuolinpvm = value;
            }

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
         *         &lt;element name="Kuolleeksijulistamispvm" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
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
            "kuolleeksijulistamispvm"
        })
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        public static class Kuolleeksijulistamistiedot {

            @XmlElement(name = "Kuolleeksijulistamispvm", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected String kuolleeksijulistamispvm;

            /**
             * Gets the value of the kuolleeksijulistamispvm property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public String getKuolleeksijulistamispvm() {
                return kuolleeksijulistamispvm;
            }

            /**
             * Sets the value of the kuolleeksijulistamispvm property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public void setKuolleeksijulistamispvm(String value) {
                this.kuolleeksijulistamispvm = value;
            }

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
         *         &lt;element name="Henkilotunnus" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}HenkilotunnusTyyppi"/&gt;
         *         &lt;element name="Syntymaaika" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
         *         &lt;element name="NykyinenSukunimi"&gt;
         *           &lt;complexType&gt;
         *             &lt;complexContent&gt;
         *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
         *                 &lt;sequence&gt;
         *                   &lt;element name="Sukunimi" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}Nimi100Tyyppi"/&gt;
         *                 &lt;/sequence&gt;
         *               &lt;/restriction&gt;
         *             &lt;/complexContent&gt;
         *           &lt;/complexType&gt;
         *         &lt;/element&gt;
         *         &lt;element name="NykyisetEtunimet"&gt;
         *           &lt;complexType&gt;
         *             &lt;complexContent&gt;
         *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
         *                 &lt;sequence&gt;
         *                   &lt;element name="Etunimet" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}Nimi100Tyyppi"/&gt;
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
        @XmlType(name = "", propOrder = {
            "henkilotunnus",
            "syntymaaika",
            "nykyinenSukunimi",
            "nykyisetEtunimet"
        })
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        public static class Lapsi {

            @XmlElement(name = "Henkilotunnus", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected String henkilotunnus;
            @XmlElement(name = "Syntymaaika", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected String syntymaaika;
            @XmlElement(name = "NykyinenSukunimi", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected VTJHenkiloVastaussanoma.Henkilo.Lapsi.NykyinenSukunimi nykyinenSukunimi;
            @XmlElement(name = "NykyisetEtunimet", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected VTJHenkiloVastaussanoma.Henkilo.Lapsi.NykyisetEtunimet nykyisetEtunimet;

            /**
             * Gets the value of the henkilotunnus property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public String getHenkilotunnus() {
                return henkilotunnus;
            }

            /**
             * Sets the value of the henkilotunnus property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public void setHenkilotunnus(String value) {
                this.henkilotunnus = value;
            }

            /**
             * Gets the value of the syntymaaika property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public String getSyntymaaika() {
                return syntymaaika;
            }

            /**
             * Sets the value of the syntymaaika property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public void setSyntymaaika(String value) {
                this.syntymaaika = value;
            }

            /**
             * Gets the value of the nykyinenSukunimi property.
             * 
             * @return
             *     possible object is
             *     {@link VTJHenkiloVastaussanoma.Henkilo.Lapsi.NykyinenSukunimi }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public VTJHenkiloVastaussanoma.Henkilo.Lapsi.NykyinenSukunimi getNykyinenSukunimi() {
                return nykyinenSukunimi;
            }

            /**
             * Sets the value of the nykyinenSukunimi property.
             * 
             * @param value
             *     allowed object is
             *     {@link VTJHenkiloVastaussanoma.Henkilo.Lapsi.NykyinenSukunimi }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public void setNykyinenSukunimi(VTJHenkiloVastaussanoma.Henkilo.Lapsi.NykyinenSukunimi value) {
                this.nykyinenSukunimi = value;
            }

            /**
             * Gets the value of the nykyisetEtunimet property.
             * 
             * @return
             *     possible object is
             *     {@link VTJHenkiloVastaussanoma.Henkilo.Lapsi.NykyisetEtunimet }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public VTJHenkiloVastaussanoma.Henkilo.Lapsi.NykyisetEtunimet getNykyisetEtunimet() {
                return nykyisetEtunimet;
            }

            /**
             * Sets the value of the nykyisetEtunimet property.
             * 
             * @param value
             *     allowed object is
             *     {@link VTJHenkiloVastaussanoma.Henkilo.Lapsi.NykyisetEtunimet }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public void setNykyisetEtunimet(VTJHenkiloVastaussanoma.Henkilo.Lapsi.NykyisetEtunimet value) {
                this.nykyisetEtunimet = value;
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
             *         &lt;element name="Sukunimi" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}Nimi100Tyyppi"/&gt;
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
                "sukunimi"
            })
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public static class NykyinenSukunimi {

                @XmlElement(name = "Sukunimi", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
                @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
                protected String sukunimi;

                /**
                 * Gets the value of the sukunimi property.
                 * 
                 * @return
                 *     possible object is
                 *     {@link String }
                 *     
                 */
                @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
                public String getSukunimi() {
                    return sukunimi;
                }

                /**
                 * Sets the value of the sukunimi property.
                 * 
                 * @param value
                 *     allowed object is
                 *     {@link String }
                 *     
                 */
                @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
                public void setSukunimi(String value) {
                    this.sukunimi = value;
                }

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
             *         &lt;element name="Etunimet" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}Nimi100Tyyppi"/&gt;
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
                "etunimet"
            })
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public static class NykyisetEtunimet {

                @XmlElement(name = "Etunimet", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
                @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
                protected String etunimet;

                /**
                 * Gets the value of the etunimet property.
                 * 
                 * @return
                 *     possible object is
                 *     {@link String }
                 *     
                 */
                @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
                public String getEtunimet() {
                    return etunimet;
                }

                /**
                 * Sets the value of the etunimet property.
                 * 
                 * @param value
                 *     allowed object is
                 *     {@link String }
                 *     
                 */
                @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
                public void setEtunimet(String value) {
                    this.etunimet = value;
                }

            }

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
         *         &lt;element name="Sukunimi" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}Nimi100Tyyppi"/&gt;
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
            "sukunimi"
        })
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        public static class NykyinenSukunimi {

            @XmlElement(name = "Sukunimi", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected String sukunimi;

            /**
             * Gets the value of the sukunimi property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public String getSukunimi() {
                return sukunimi;
            }

            /**
             * Sets the value of the sukunimi property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public void setSukunimi(String value) {
                this.sukunimi = value;
            }

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
         *         &lt;element name="Etunimet" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}Nimi100Tyyppi"/&gt;
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
            "etunimet"
        })
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        public static class NykyisetEtunimet {

            @XmlElement(name = "Etunimet", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected String etunimet;

            /**
             * Gets the value of the etunimet property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public String getEtunimet() {
                return etunimet;
            }

            /**
             * Sets the value of the etunimet property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public void setEtunimet(String value) {
                this.etunimet = value;
            }

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
         *         &lt;element name="Sukupuolikoodi" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}SukupuolikoodiTyyppi"/&gt;
         *         &lt;element name="SukupuoliS" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}SukupuoliTekstiTyyppi"/&gt;
         *         &lt;element name="SukupuoliR" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}SukupuoliTekstiTyyppi"/&gt;
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
            "sukupuolikoodi",
            "sukupuoliS",
            "sukupuoliR"
        })
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        public static class Sukupuoli {

            @XmlElement(name = "Sukupuolikoodi", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected String sukupuolikoodi;
            @XmlElement(name = "SukupuoliS", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected String sukupuoliS;
            @XmlElement(name = "SukupuoliR", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected String sukupuoliR;

            /**
             * Gets the value of the sukupuolikoodi property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public String getSukupuolikoodi() {
                return sukupuolikoodi;
            }

            /**
             * Sets the value of the sukupuolikoodi property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public void setSukupuolikoodi(String value) {
                this.sukupuolikoodi = value;
            }

            /**
             * Gets the value of the sukupuoliS property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public String getSukupuoliS() {
                return sukupuoliS;
            }

            /**
             * Sets the value of the sukupuoliS property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public void setSukupuoliS(String value) {
                this.sukupuoliS = value;
            }

            /**
             * Gets the value of the sukupuoliR property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public String getSukupuoliR() {
                return sukupuoliR;
            }

            /**
             * Sets the value of the sukupuoliR property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public void setSukupuoliR(String value) {
                this.sukupuoliR = value;
            }

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
         *         &lt;element name="Valtiokoodi3" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}Valtiokoodi3Tyyppi"/&gt;
         *         &lt;element name="ValtioS" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}ValtioNimiTyyppi"/&gt;
         *         &lt;element name="ValtioR" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}ValtioNimiTyyppi"/&gt;
         *         &lt;element name="ValtioSelvakielinen" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}ValtioNimi30Tyyppi"/&gt;
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
            "valtiokoodi3",
            "valtioS",
            "valtioR",
            "valtioSelvakielinen"
        })
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        public static class Syntymamaa {

            @XmlElement(name = "Valtiokoodi3", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected String valtiokoodi3;
            @XmlElement(name = "ValtioS", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected String valtioS;
            @XmlElement(name = "ValtioR", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected String valtioR;
            @XmlElement(name = "ValtioSelvakielinen", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected String valtioSelvakielinen;

            /**
             * Gets the value of the valtiokoodi3 property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public String getValtiokoodi3() {
                return valtiokoodi3;
            }

            /**
             * Sets the value of the valtiokoodi3 property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public void setValtiokoodi3(String value) {
                this.valtiokoodi3 = value;
            }

            /**
             * Gets the value of the valtioS property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public String getValtioS() {
                return valtioS;
            }

            /**
             * Sets the value of the valtioS property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public void setValtioS(String value) {
                this.valtioS = value;
            }

            /**
             * Gets the value of the valtioR property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public String getValtioR() {
                return valtioR;
            }

            /**
             * Sets the value of the valtioR property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public void setValtioR(String value) {
                this.valtioR = value;
            }

            /**
             * Gets the value of the valtioSelvakielinen property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public String getValtioSelvakielinen() {
                return valtioSelvakielinen;
            }

            /**
             * Sets the value of the valtioSelvakielinen property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public void setValtioSelvakielinen(String value) {
                this.valtioSelvakielinen = value;
            }

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
         *         &lt;element name="LahiosoiteS" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}KotimainenLahiosoiteTyyppi"/&gt;
         *         &lt;element name="LahiosoiteR" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}KotimainenLahiosoiteTyyppi"/&gt;
         *         &lt;element name="Postinumero" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PostinumeroTyyppi"/&gt;
         *         &lt;element name="PostitoimipaikkaS" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PostitoimipaikkaTyyppi"/&gt;
         *         &lt;element name="PostitoimipaikkaR" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PostitoimipaikkaTyyppi"/&gt;
         *         &lt;element name="AsuminenAlkupvm" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
         *         &lt;element name="AsuminenLoppupvm" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
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
            "lahiosoiteS",
            "lahiosoiteR",
            "postinumero",
            "postitoimipaikkaS",
            "postitoimipaikkaR",
            "asuminenAlkupvm",
            "asuminenLoppupvm"
        })
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        public static class TilapainenKotimainenLahiosoite {

            @XmlElement(name = "LahiosoiteS", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected String lahiosoiteS;
            @XmlElement(name = "LahiosoiteR", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected String lahiosoiteR;
            @XmlElement(name = "Postinumero", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected String postinumero;
            @XmlElement(name = "PostitoimipaikkaS", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected String postitoimipaikkaS;
            @XmlElement(name = "PostitoimipaikkaR", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected String postitoimipaikkaR;
            @XmlElement(name = "AsuminenAlkupvm", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected String asuminenAlkupvm;
            @XmlElement(name = "AsuminenLoppupvm", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected String asuminenLoppupvm;

            /**
             * Gets the value of the lahiosoiteS property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public String getLahiosoiteS() {
                return lahiosoiteS;
            }

            /**
             * Sets the value of the lahiosoiteS property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public void setLahiosoiteS(String value) {
                this.lahiosoiteS = value;
            }

            /**
             * Gets the value of the lahiosoiteR property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public String getLahiosoiteR() {
                return lahiosoiteR;
            }

            /**
             * Sets the value of the lahiosoiteR property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public void setLahiosoiteR(String value) {
                this.lahiosoiteR = value;
            }

            /**
             * Gets the value of the postinumero property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public String getPostinumero() {
                return postinumero;
            }

            /**
             * Sets the value of the postinumero property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public void setPostinumero(String value) {
                this.postinumero = value;
            }

            /**
             * Gets the value of the postitoimipaikkaS property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public String getPostitoimipaikkaS() {
                return postitoimipaikkaS;
            }

            /**
             * Sets the value of the postitoimipaikkaS property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public void setPostitoimipaikkaS(String value) {
                this.postitoimipaikkaS = value;
            }

            /**
             * Gets the value of the postitoimipaikkaR property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public String getPostitoimipaikkaR() {
                return postitoimipaikkaR;
            }

            /**
             * Sets the value of the postitoimipaikkaR property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public void setPostitoimipaikkaR(String value) {
                this.postitoimipaikkaR = value;
            }

            /**
             * Gets the value of the asuminenAlkupvm property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public String getAsuminenAlkupvm() {
                return asuminenAlkupvm;
            }

            /**
             * Sets the value of the asuminenAlkupvm property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public void setAsuminenAlkupvm(String value) {
                this.asuminenAlkupvm = value;
            }

            /**
             * Gets the value of the asuminenLoppupvm property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public String getAsuminenLoppupvm() {
                return asuminenLoppupvm;
            }

            /**
             * Sets the value of the asuminenLoppupvm property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public void setAsuminenLoppupvm(String value) {
                this.asuminenLoppupvm = value;
            }

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
         *         &lt;element name="UlkomainenLahiosoite" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}UlkomainenLahiosoiteTyyppi"/&gt;
         *         &lt;element name="UlkomainenPaikkakuntaJaValtioS" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}UlkomainenPaikkakuntaJaValtioTyyppi"/&gt;
         *         &lt;element name="UlkomainenPaikkakuntaJaValtioR" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}UlkomainenPaikkakuntaJaValtioTyyppi"/&gt;
         *         &lt;element name="UlkomainenPaikkakuntaJaValtioSelvakielinen" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}UlkomainenPaikkakuntaJaValtioTyyppi"/&gt;
         *         &lt;element name="Valtiokoodi3" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}Valtiokoodi3Tyyppi"/&gt;
         *         &lt;element name="AsuminenAlkupvm" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
         *         &lt;element name="AsuminenLoppupvm" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
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
            "ulkomainenLahiosoite",
            "ulkomainenPaikkakuntaJaValtioS",
            "ulkomainenPaikkakuntaJaValtioR",
            "ulkomainenPaikkakuntaJaValtioSelvakielinen",
            "valtiokoodi3",
            "asuminenAlkupvm",
            "asuminenLoppupvm"
        })
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        public static class TilapainenUlkomainenLahiosoite {

            @XmlElement(name = "UlkomainenLahiosoite", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected String ulkomainenLahiosoite;
            @XmlElement(name = "UlkomainenPaikkakuntaJaValtioS", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected String ulkomainenPaikkakuntaJaValtioS;
            @XmlElement(name = "UlkomainenPaikkakuntaJaValtioR", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected String ulkomainenPaikkakuntaJaValtioR;
            @XmlElement(name = "UlkomainenPaikkakuntaJaValtioSelvakielinen", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected String ulkomainenPaikkakuntaJaValtioSelvakielinen;
            @XmlElement(name = "Valtiokoodi3", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected String valtiokoodi3;
            @XmlElement(name = "AsuminenAlkupvm", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected String asuminenAlkupvm;
            @XmlElement(name = "AsuminenLoppupvm", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected String asuminenLoppupvm;

            /**
             * Gets the value of the ulkomainenLahiosoite property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public String getUlkomainenLahiosoite() {
                return ulkomainenLahiosoite;
            }

            /**
             * Sets the value of the ulkomainenLahiosoite property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public void setUlkomainenLahiosoite(String value) {
                this.ulkomainenLahiosoite = value;
            }

            /**
             * Gets the value of the ulkomainenPaikkakuntaJaValtioS property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public String getUlkomainenPaikkakuntaJaValtioS() {
                return ulkomainenPaikkakuntaJaValtioS;
            }

            /**
             * Sets the value of the ulkomainenPaikkakuntaJaValtioS property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public void setUlkomainenPaikkakuntaJaValtioS(String value) {
                this.ulkomainenPaikkakuntaJaValtioS = value;
            }

            /**
             * Gets the value of the ulkomainenPaikkakuntaJaValtioR property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public String getUlkomainenPaikkakuntaJaValtioR() {
                return ulkomainenPaikkakuntaJaValtioR;
            }

            /**
             * Sets the value of the ulkomainenPaikkakuntaJaValtioR property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public void setUlkomainenPaikkakuntaJaValtioR(String value) {
                this.ulkomainenPaikkakuntaJaValtioR = value;
            }

            /**
             * Gets the value of the ulkomainenPaikkakuntaJaValtioSelvakielinen property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public String getUlkomainenPaikkakuntaJaValtioSelvakielinen() {
                return ulkomainenPaikkakuntaJaValtioSelvakielinen;
            }

            /**
             * Sets the value of the ulkomainenPaikkakuntaJaValtioSelvakielinen property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public void setUlkomainenPaikkakuntaJaValtioSelvakielinen(String value) {
                this.ulkomainenPaikkakuntaJaValtioSelvakielinen = value;
            }

            /**
             * Gets the value of the valtiokoodi3 property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public String getValtiokoodi3() {
                return valtiokoodi3;
            }

            /**
             * Sets the value of the valtiokoodi3 property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public void setValtiokoodi3(String value) {
                this.valtiokoodi3 = value;
            }

            /**
             * Gets the value of the asuminenAlkupvm property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public String getAsuminenAlkupvm() {
                return asuminenAlkupvm;
            }

            /**
             * Sets the value of the asuminenAlkupvm property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public void setAsuminenAlkupvm(String value) {
                this.asuminenAlkupvm = value;
            }

            /**
             * Gets the value of the asuminenLoppupvm property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public String getAsuminenLoppupvm() {
                return asuminenLoppupvm;
            }

            /**
             * Sets the value of the asuminenLoppupvm property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public void setAsuminenLoppupvm(String value) {
                this.asuminenLoppupvm = value;
            }

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
         *         &lt;element name="TurvakieltoTieto" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}KieltoTietoTyyppi"/&gt;
         *         &lt;element name="TurvakieltoPaattymispvm" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
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
            "turvakieltoTieto",
            "turvakieltoPaattymispvm"
        })
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        public static class Turvakielto {

            @XmlElement(name = "TurvakieltoTieto", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected String turvakieltoTieto;
            @XmlElement(name = "TurvakieltoPaattymispvm", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected String turvakieltoPaattymispvm;

            /**
             * Gets the value of the turvakieltoTieto property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public String getTurvakieltoTieto() {
                return turvakieltoTieto;
            }

            /**
             * Sets the value of the turvakieltoTieto property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public void setTurvakieltoTieto(String value) {
                this.turvakieltoTieto = value;
            }

            /**
             * Gets the value of the turvakieltoPaattymispvm property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public String getTurvakieltoPaattymispvm() {
                return turvakieltoPaattymispvm;
            }

            /**
             * Sets the value of the turvakieltoPaattymispvm property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public void setTurvakieltoPaattymispvm(String value) {
                this.turvakieltoPaattymispvm = value;
            }

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
         *         &lt;element name="UlkomainenLahiosoite" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}UlkomainenLahiosoiteTyyppi"/&gt;
         *         &lt;element name="UlkomainenPaikkakunta" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}UlkomainenPaikkakuntaTyyppi"/&gt;
         *         &lt;element name="Valtiokoodi3" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}Valtiokoodi3Tyyppi"/&gt;
         *         &lt;element name="ValtioS" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}ValtioNimiTyyppi"/&gt;
         *         &lt;element name="ValtioR" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}ValtioNimiTyyppi"/&gt;
         *         &lt;element name="ValtioSelvakielinen" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}ValtioNimi30Tyyppi"/&gt;
         *         &lt;element name="PostiosoiteAlkupvm" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
         *         &lt;element name="PostiosoiteLoppupvm" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
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
            "ulkomainenLahiosoite",
            "ulkomainenPaikkakunta",
            "valtiokoodi3",
            "valtioS",
            "valtioR",
            "valtioSelvakielinen",
            "postiosoiteAlkupvm",
            "postiosoiteLoppupvm"
        })
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        public static class UlkomainenPostiosoite {

            @XmlElement(name = "UlkomainenLahiosoite", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected String ulkomainenLahiosoite;
            @XmlElement(name = "UlkomainenPaikkakunta", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected String ulkomainenPaikkakunta;
            @XmlElement(name = "Valtiokoodi3", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected String valtiokoodi3;
            @XmlElement(name = "ValtioS", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected String valtioS;
            @XmlElement(name = "ValtioR", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected String valtioR;
            @XmlElement(name = "ValtioSelvakielinen", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected String valtioSelvakielinen;
            @XmlElement(name = "PostiosoiteAlkupvm", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected String postiosoiteAlkupvm;
            @XmlElement(name = "PostiosoiteLoppupvm", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected String postiosoiteLoppupvm;

            /**
             * Gets the value of the ulkomainenLahiosoite property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public String getUlkomainenLahiosoite() {
                return ulkomainenLahiosoite;
            }

            /**
             * Sets the value of the ulkomainenLahiosoite property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public void setUlkomainenLahiosoite(String value) {
                this.ulkomainenLahiosoite = value;
            }

            /**
             * Gets the value of the ulkomainenPaikkakunta property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public String getUlkomainenPaikkakunta() {
                return ulkomainenPaikkakunta;
            }

            /**
             * Sets the value of the ulkomainenPaikkakunta property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public void setUlkomainenPaikkakunta(String value) {
                this.ulkomainenPaikkakunta = value;
            }

            /**
             * Gets the value of the valtiokoodi3 property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public String getValtiokoodi3() {
                return valtiokoodi3;
            }

            /**
             * Sets the value of the valtiokoodi3 property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public void setValtiokoodi3(String value) {
                this.valtiokoodi3 = value;
            }

            /**
             * Gets the value of the valtioS property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public String getValtioS() {
                return valtioS;
            }

            /**
             * Sets the value of the valtioS property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public void setValtioS(String value) {
                this.valtioS = value;
            }

            /**
             * Gets the value of the valtioR property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public String getValtioR() {
                return valtioR;
            }

            /**
             * Sets the value of the valtioR property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public void setValtioR(String value) {
                this.valtioR = value;
            }

            /**
             * Gets the value of the valtioSelvakielinen property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public String getValtioSelvakielinen() {
                return valtioSelvakielinen;
            }

            /**
             * Sets the value of the valtioSelvakielinen property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public void setValtioSelvakielinen(String value) {
                this.valtioSelvakielinen = value;
            }

            /**
             * Gets the value of the postiosoiteAlkupvm property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public String getPostiosoiteAlkupvm() {
                return postiosoiteAlkupvm;
            }

            /**
             * Sets the value of the postiosoiteAlkupvm property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public void setPostiosoiteAlkupvm(String value) {
                this.postiosoiteAlkupvm = value;
            }

            /**
             * Gets the value of the postiosoiteLoppupvm property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public String getPostiosoiteLoppupvm() {
                return postiosoiteLoppupvm;
            }

            /**
             * Sets the value of the postiosoiteLoppupvm property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public void setPostiosoiteLoppupvm(String value) {
                this.postiosoiteLoppupvm = value;
            }

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
         *         &lt;element name="Asuinpaikantunnus" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}AsuinpaikkatunnusTyyppi"/&gt;
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
            "asuinpaikantunnus"
        })
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        public static class VakinainenAsuinpaikka {

            @XmlElement(name = "Asuinpaikantunnus", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected String asuinpaikantunnus;

            /**
             * Gets the value of the asuinpaikantunnus property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public String getAsuinpaikantunnus() {
                return asuinpaikantunnus;
            }

            /**
             * Sets the value of the asuinpaikantunnus property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public void setAsuinpaikantunnus(String value) {
                this.asuinpaikantunnus = value;
            }

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
         *         &lt;element name="LahiosoiteS" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}KotimainenLahiosoiteTyyppi"/&gt;
         *         &lt;element name="LahiosoiteR" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}KotimainenLahiosoiteTyyppi"/&gt;
         *         &lt;element name="Postinumero" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PostinumeroTyyppi"/&gt;
         *         &lt;element name="PostitoimipaikkaS" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PostitoimipaikkaTyyppi"/&gt;
         *         &lt;element name="PostitoimipaikkaR" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PostitoimipaikkaTyyppi"/&gt;
         *         &lt;element name="AsuminenAlkupvm" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
         *         &lt;element name="AsuminenLoppupvm" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
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
            "lahiosoiteS",
            "lahiosoiteR",
            "postinumero",
            "postitoimipaikkaS",
            "postitoimipaikkaR",
            "asuminenAlkupvm",
            "asuminenLoppupvm"
        })
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        public static class VakinainenKotimainenLahiosoite {

            @XmlElement(name = "LahiosoiteS", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected String lahiosoiteS;
            @XmlElement(name = "LahiosoiteR", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected String lahiosoiteR;
            @XmlElement(name = "Postinumero", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected String postinumero;
            @XmlElement(name = "PostitoimipaikkaS", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected String postitoimipaikkaS;
            @XmlElement(name = "PostitoimipaikkaR", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected String postitoimipaikkaR;
            @XmlElement(name = "AsuminenAlkupvm", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected String asuminenAlkupvm;
            @XmlElement(name = "AsuminenLoppupvm", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected String asuminenLoppupvm;

            /**
             * Gets the value of the lahiosoiteS property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public String getLahiosoiteS() {
                return lahiosoiteS;
            }

            /**
             * Sets the value of the lahiosoiteS property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public void setLahiosoiteS(String value) {
                this.lahiosoiteS = value;
            }

            /**
             * Gets the value of the lahiosoiteR property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public String getLahiosoiteR() {
                return lahiosoiteR;
            }

            /**
             * Sets the value of the lahiosoiteR property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public void setLahiosoiteR(String value) {
                this.lahiosoiteR = value;
            }

            /**
             * Gets the value of the postinumero property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public String getPostinumero() {
                return postinumero;
            }

            /**
             * Sets the value of the postinumero property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public void setPostinumero(String value) {
                this.postinumero = value;
            }

            /**
             * Gets the value of the postitoimipaikkaS property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public String getPostitoimipaikkaS() {
                return postitoimipaikkaS;
            }

            /**
             * Sets the value of the postitoimipaikkaS property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public void setPostitoimipaikkaS(String value) {
                this.postitoimipaikkaS = value;
            }

            /**
             * Gets the value of the postitoimipaikkaR property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public String getPostitoimipaikkaR() {
                return postitoimipaikkaR;
            }

            /**
             * Sets the value of the postitoimipaikkaR property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public void setPostitoimipaikkaR(String value) {
                this.postitoimipaikkaR = value;
            }

            /**
             * Gets the value of the asuminenAlkupvm property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public String getAsuminenAlkupvm() {
                return asuminenAlkupvm;
            }

            /**
             * Sets the value of the asuminenAlkupvm property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public void setAsuminenAlkupvm(String value) {
                this.asuminenAlkupvm = value;
            }

            /**
             * Gets the value of the asuminenLoppupvm property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public String getAsuminenLoppupvm() {
                return asuminenLoppupvm;
            }

            /**
             * Sets the value of the asuminenLoppupvm property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public void setAsuminenLoppupvm(String value) {
                this.asuminenLoppupvm = value;
            }

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
         *         &lt;element name="UlkomainenLahiosoite" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}UlkomainenLahiosoiteTyyppi"/&gt;
         *         &lt;element name="UlkomainenPaikkakuntaJaValtioS" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}UlkomainenPaikkakuntaJaValtioTyyppi"/&gt;
         *         &lt;element name="UlkomainenPaikkakuntaJaValtioR" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}UlkomainenPaikkakuntaJaValtioTyyppi"/&gt;
         *         &lt;element name="UlkomainenPaikkakuntaJaValtioSelvakielinen" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}UlkomainenPaikkakuntaJaValtioTyyppi"/&gt;
         *         &lt;element name="Valtiokoodi3" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}Valtiokoodi3Tyyppi"/&gt;
         *         &lt;element name="AsuminenAlkupvm" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
         *         &lt;element name="AsuminenLoppupvm" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaivamaaraTyyppi"/&gt;
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
            "ulkomainenLahiosoite",
            "ulkomainenPaikkakuntaJaValtioS",
            "ulkomainenPaikkakuntaJaValtioR",
            "ulkomainenPaikkakuntaJaValtioSelvakielinen",
            "valtiokoodi3",
            "asuminenAlkupvm",
            "asuminenLoppupvm"
        })
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        public static class VakinainenUlkomainenLahiosoite {

            @XmlElement(name = "UlkomainenLahiosoite", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected String ulkomainenLahiosoite;
            @XmlElement(name = "UlkomainenPaikkakuntaJaValtioS", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected String ulkomainenPaikkakuntaJaValtioS;
            @XmlElement(name = "UlkomainenPaikkakuntaJaValtioR", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected String ulkomainenPaikkakuntaJaValtioR;
            @XmlElement(name = "UlkomainenPaikkakuntaJaValtioSelvakielinen", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected String ulkomainenPaikkakuntaJaValtioSelvakielinen;
            @XmlElement(name = "Valtiokoodi3", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected String valtiokoodi3;
            @XmlElement(name = "AsuminenAlkupvm", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected String asuminenAlkupvm;
            @XmlElement(name = "AsuminenLoppupvm", namespace = "http://xml.vrk.fi/schema/vtjkysely", required = true)
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            protected String asuminenLoppupvm;

            /**
             * Gets the value of the ulkomainenLahiosoite property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public String getUlkomainenLahiosoite() {
                return ulkomainenLahiosoite;
            }

            /**
             * Sets the value of the ulkomainenLahiosoite property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public void setUlkomainenLahiosoite(String value) {
                this.ulkomainenLahiosoite = value;
            }

            /**
             * Gets the value of the ulkomainenPaikkakuntaJaValtioS property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public String getUlkomainenPaikkakuntaJaValtioS() {
                return ulkomainenPaikkakuntaJaValtioS;
            }

            /**
             * Sets the value of the ulkomainenPaikkakuntaJaValtioS property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public void setUlkomainenPaikkakuntaJaValtioS(String value) {
                this.ulkomainenPaikkakuntaJaValtioS = value;
            }

            /**
             * Gets the value of the ulkomainenPaikkakuntaJaValtioR property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public String getUlkomainenPaikkakuntaJaValtioR() {
                return ulkomainenPaikkakuntaJaValtioR;
            }

            /**
             * Sets the value of the ulkomainenPaikkakuntaJaValtioR property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public void setUlkomainenPaikkakuntaJaValtioR(String value) {
                this.ulkomainenPaikkakuntaJaValtioR = value;
            }

            /**
             * Gets the value of the ulkomainenPaikkakuntaJaValtioSelvakielinen property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public String getUlkomainenPaikkakuntaJaValtioSelvakielinen() {
                return ulkomainenPaikkakuntaJaValtioSelvakielinen;
            }

            /**
             * Sets the value of the ulkomainenPaikkakuntaJaValtioSelvakielinen property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public void setUlkomainenPaikkakuntaJaValtioSelvakielinen(String value) {
                this.ulkomainenPaikkakuntaJaValtioSelvakielinen = value;
            }

            /**
             * Gets the value of the valtiokoodi3 property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public String getValtiokoodi3() {
                return valtiokoodi3;
            }

            /**
             * Sets the value of the valtiokoodi3 property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public void setValtiokoodi3(String value) {
                this.valtiokoodi3 = value;
            }

            /**
             * Gets the value of the asuminenAlkupvm property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public String getAsuminenAlkupvm() {
                return asuminenAlkupvm;
            }

            /**
             * Sets the value of the asuminenAlkupvm property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public void setAsuminenAlkupvm(String value) {
                this.asuminenAlkupvm = value;
            }

            /**
             * Gets the value of the asuminenLoppupvm property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public String getAsuminenLoppupvm() {
                return asuminenLoppupvm;
            }

            /**
             * Sets the value of the asuminenLoppupvm property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
            public void setAsuminenLoppupvm(String value) {
                this.asuminenLoppupvm = value;
            }

        }

    }


    /**
     * <p>Java class for anonymous complex type.
     * 
     * <p>The following schema fragment specifies the expected content contained within this class.
     * 
     * <pre>
     * &lt;complexType&gt;
     *   &lt;simpleContent&gt;
     *     &lt;extension base="&lt;http://xml.vrk.fi/schema/vtj/henkilotiedot/1&gt;PaluukoodiTekstiTyyppi"&gt;
     *       &lt;attribute name="koodi" use="required" type="{http://xml.vrk.fi/schema/vtj/henkilotiedot/1}PaluukoodiTyyppi" /&gt;
     *     &lt;/extension&gt;
     *   &lt;/simpleContent&gt;
     * &lt;/complexType&gt;
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
        "value"
    })
    @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
    public static class Paluukoodi {

        @XmlValue
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        protected String value;
        @XmlAttribute(name = "koodi", required = true)
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        protected String koodi;

        /**
         * Jarjestelman virhetilanteissa kaytettavaa paluukoodia vastaava teksti selvakielisena suomeksi. Muoto 0-200 merkkia.
         *                 0000 Haku onnistui
         *                 0001 Hakuperusteella (henkilotunnus, sahkoinen asiointitunnus, .. ) ei loydy tietoja vtj:sta.
         *                 0002 Hakuperusteena kaytetty (tai sovelluksen sahkoisella asiointitunnuksella etsima) henkilotunnus on passivoitu.
         *                 0003 Kysyvan sovelluksen lahettama tunnussana puuttuu, on virheellinen tai vanhentunut.
         *                 0004 Kysyvan sovelluksen lahettama kayttajatunnus puuttuu, on virheellinen tai vanhentunut.
         *                 0005 ILMOITUS -tiedossa oleva selvakielinen teksti ilmoittaa virheen tai muun ilmoitettavan asian. Selvakielisia teksteja ovat:
         *                 Sovellus tulostaa seuraavat paluukoodilla 0005:
         *                 Seuranta ei onnistu / Uppfoljningen misslyckades
         *                 Sukunimi on pakollinen annettava / Slaktnamn ar obligatoriskt
         *                 Etunimet on pakollinen annettava / Fornamn ar obligatoriskt
         *                 Syntymaaika on pakollinen annettava / Fodelsetid ar obligatoriskt
         *                 1. etunimi tuntematon nimihakemistolle / 1. fornamn okant for registret
         *                 2. etunimi tuntematon nimihakemistolle / 2. fornamn okant for registret
         *                 3. etunimi tuntematon nimihakemistolle / 3. fornamn okant for registret
         *                 Loytyi useampi kuin 1. / Hittades flera an 1.
         *                 Pakollinen hakuehto puuttuu. / Obligatoriskt sokkriteriet fattas.
         *                 0006 Hakuperusteella (henkilotunnus, sahkoinen asiointitunnus, .. ) ei loydy tietoja vtj:sta.
         *                 0007 Haettava henkilo on kuollut, joten tietoja ei voida palauttaa, ellei muuta ole sovittu.
         *                 0008 Kyselysanomassa on pyynto varmenteen sulkulistatarkistuksesta (SULKULISTATARKISTUS="YES"), mutta sita ei toistaiseksi suoriteta.
         *                 0009 Kyselysanomassa on pyynto varmenteen voimassaolotarkistuksesta (VOIMASSAOLOTARKISTUS="YES"), mutta sita ei toistaiseksi suoriteta.
         *                 0010 Kysyvan sovelluksen on sovittu kayttavan tunnistukseen sahkoista asiointitunnusta (Finuid, Satu). Sahkoisen asiointitunnuksen sisaltava varmenne kuitenkin puuttuu.
         *                 0011 Kysyvan sovelluksen on sovittu kayttavan tunnistukseen 'perinteista' kayttajatunnusta. Kayttajatunnusta ei kuitenkaan laheteta, vaan sen tilalla tule sahkoisen asiointitunnuksen (Finuid, Satu) sisaltama varmenne.
         *                 0012 Varmenteelle suoritetussa voimassaolotarkistuksessa on todettu varmenteen voimassaolon paattyneen.
         *                 0013 Varmenteelle suoritetussa sulkulistatarkistuksessa on todettu varmenteen olevan sulkulistalla.
         *                 0014 Varmenne ei ole varmenne ollenkaan tai se ei ole vrk:n hyvaksyma.
         *                 0015 Varmenne ei ole vrk:n hyvaksyma.
         *                 0016 Kysely- ja vastaussanomien vertailussa on todettu niiden tunnistetietojen eroavan.
         *                 0017 Hakuperusteella (henkilotunnus) ei loydy tietoja hollesta (Holhousasiain rekisteri).
         *                 0018 Henkilo on alle 15-vuotias
         *                 0019 Tietoja ei voida luovuttaa
         *                 -1500 Virheellinen tunnus/salasana pari
         *                 -1505 laskutustiedoissa jokin virhe
         *                 -1800 WebServicen sisainen virhe
         *                 -1805 Tunnistuksessa kaytettavan kyselysanoman validointivirhe, eli sanoma ei ole skeeman mukainen.
         *                 -1900 Tuotetta ei loydy
         *                 -1901 Tuotetta ei loydy
         *                 -1902 Sanoma ei ole validi (merkit)
         *                 - 1903 Sanoma ei ole validi (tunnistuksen hakuxml merkit)
         *                 - 1904 Sanoma ei ole validi (loppukayttaja puuttuu)
         *             
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        public String getValue() {
            return value;
        }

        /**
         * Sets the value of the value property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        public void setValue(String value) {
            this.value = value;
        }

        /**
         * Gets the value of the koodi property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        public String getKoodi() {
            return koodi;
        }

        /**
         * Sets the value of the koodi property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        @Generated(value = "com.sun.tools.xjc.Driver", date = "2019-07-17T01:59:02+03:00", comments = "JAXB RI v2.3.1")
        public void setKoodi(String value) {
            this.koodi = value;
        }

    }

}
