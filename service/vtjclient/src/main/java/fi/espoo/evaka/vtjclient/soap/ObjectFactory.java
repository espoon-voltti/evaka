// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.vtjclient.soap;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the fi.espoo.evaka.vtjclient.soap package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _Client_QNAME = new QName("http://x-road.eu/xsd/xroad.xsd", "client");
    private final static QName _Service_QNAME = new QName("http://x-road.eu/xsd/xroad.xsd", "service");
    private final static QName _CentralService_QNAME = new QName("http://x-road.eu/xsd/xroad.xsd", "centralService");
    private final static QName _Id_QNAME = new QName("http://x-road.eu/xsd/xroad.xsd", "id");
    private final static QName _UserId_QNAME = new QName("http://x-road.eu/xsd/xroad.xsd", "userId");
    private final static QName _Issue_QNAME = new QName("http://x-road.eu/xsd/xroad.xsd", "issue");
    private final static QName _ProtocolVersion_QNAME = new QName("http://x-road.eu/xsd/xroad.xsd", "protocolVersion");
    private final static QName _Version_QNAME = new QName("http://x-road.eu/xsd/xroad.xsd", "version");
    private final static QName _XRoadInstance_QNAME = new QName("http://x-road.eu/xsd/identifiers", "xRoadInstance");
    private final static QName _MemberClass_QNAME = new QName("http://x-road.eu/xsd/identifiers", "memberClass");
    private final static QName _MemberCode_QNAME = new QName("http://x-road.eu/xsd/identifiers", "memberCode");
    private final static QName _SubsystemCode_QNAME = new QName("http://x-road.eu/xsd/identifiers", "subsystemCode");
    private final static QName _GroupCode_QNAME = new QName("http://x-road.eu/xsd/identifiers", "groupCode");
    private final static QName _ServiceCode_QNAME = new QName("http://x-road.eu/xsd/identifiers", "serviceCode");
    private final static QName _ServiceVersion_QNAME = new QName("http://x-road.eu/xsd/identifiers", "serviceVersion");
    private final static QName _SecurityCategoryCode_QNAME = new QName("http://x-road.eu/xsd/identifiers", "securityCategoryCode");
    private final static QName _ServerCode_QNAME = new QName("http://x-road.eu/xsd/identifiers", "serverCode");
    private final static QName _HenkilonTunnusKysely_QNAME = new QName("http://xml.vrk.fi/ws/vtj/vtjkysely/1", "HenkilonTunnusKysely");
    private final static QName _HenkilonTunnusKyselyResponse_QNAME = new QName("http://xml.vrk.fi/ws/vtj/vtjkysely/1", "HenkilonTunnusKyselyResponse");
    private final static QName _HenkilonTunnistusKysely_QNAME = new QName("http://xml.vrk.fi/ws/vtj/vtjkysely/1", "HenkilonTunnistusKysely");
    private final static QName _HenkilonTunnistusKyselyResponse_QNAME = new QName("http://xml.vrk.fi/ws/vtj/vtjkysely/1", "HenkilonTunnistusKyselyResponse");
    private final static QName _RakennuksenTunnusKysely_QNAME = new QName("http://xml.vrk.fi/ws/vtj/vtjkysely/1", "RakennuksenTunnusKysely");
    private final static QName _RakennuksenTunnusKyselyResponse_QNAME = new QName("http://xml.vrk.fi/ws/vtj/vtjkysely/1", "RakennuksenTunnusKyselyResponse");
    private final static QName _RakennuksenTunnistusKysely_QNAME = new QName("http://xml.vrk.fi/ws/vtj/vtjkysely/1", "RakennuksenTunnistusKysely");
    private final static QName _RakennuksenTunnistusKyselyResponse_QNAME = new QName("http://xml.vrk.fi/ws/vtj/vtjkysely/1", "RakennuksenTunnistusKyselyResponse");
    private final static QName _KiinteistonTunnusKysely_QNAME = new QName("http://xml.vrk.fi/ws/vtj/vtjkysely/1", "KiinteistonTunnusKysely");
    private final static QName _KiinteistonTunnusKyselyResponse_QNAME = new QName("http://xml.vrk.fi/ws/vtj/vtjkysely/1", "KiinteistonTunnusKyselyResponse");
    private final static QName _KiinteistonTunnistusKysely_QNAME = new QName("http://xml.vrk.fi/ws/vtj/vtjkysely/1", "KiinteistonTunnistusKysely");
    private final static QName _KiinteistonTunnistusKyselyResponse_QNAME = new QName("http://xml.vrk.fi/ws/vtj/vtjkysely/1", "KiinteistonTunnistusKyselyResponse");
    private final static QName _HaeTuotteenSkeema_QNAME = new QName("http://xml.vrk.fi/ws/vtj/vtjkysely/1", "HaeTuotteenSkeema");
    private final static QName _HaeTuotteenSkeemaResponse_QNAME = new QName("http://xml.vrk.fi/ws/vtj/vtjkysely/1", "HaeTuotteenSkeemaResponse");
    private final static QName _HenkiloTunnusKyselyResTypeFaultCode_QNAME = new QName("http://xml.vrk.fi/ws/vtj/vtjkysely/1", "faultCode");
    private final static QName _HenkiloTunnusKyselyResTypeFaultString_QNAME = new QName("http://xml.vrk.fi/ws/vtj/vtjkysely/1", "faultString");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: fi.espoo.evaka.vtjclient.soap
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link VTJHenkiloVastaussanoma }
     * 
     */
    public VTJHenkiloVastaussanoma createVTJHenkiloVastaussanoma() {
        return new VTJHenkiloVastaussanoma();
    }

    /**
     * Create an instance of {@link VTJHenkiloVastaussanoma.Henkilo }
     * 
     */
    public VTJHenkiloVastaussanoma.Henkilo createVTJHenkiloVastaussanomaHenkilo() {
        return new VTJHenkiloVastaussanoma.Henkilo();
    }

    /**
     * Create an instance of {@link VTJHenkiloVastaussanoma.Henkilo.Edunvalvontavaltuutus }
     * 
     */
    public VTJHenkiloVastaussanoma.Henkilo.Edunvalvontavaltuutus createVTJHenkiloVastaussanomaHenkiloEdunvalvontavaltuutus() {
        return new VTJHenkiloVastaussanoma.Henkilo.Edunvalvontavaltuutus();
    }

    /**
     * Create an instance of {@link VTJHenkiloVastaussanoma.Henkilo.Edunvalvontavaltuutus.HenkiloEdunvalvontavaltuutettu }
     * 
     */
    public VTJHenkiloVastaussanoma.Henkilo.Edunvalvontavaltuutus.HenkiloEdunvalvontavaltuutettu createVTJHenkiloVastaussanomaHenkiloEdunvalvontavaltuutusHenkiloEdunvalvontavaltuutettu() {
        return new VTJHenkiloVastaussanoma.Henkilo.Edunvalvontavaltuutus.HenkiloEdunvalvontavaltuutettu();
    }

    /**
     * Create an instance of {@link VTJHenkiloVastaussanoma.Henkilo.Edunvalvonta }
     * 
     */
    public VTJHenkiloVastaussanoma.Henkilo.Edunvalvonta createVTJHenkiloVastaussanomaHenkiloEdunvalvonta() {
        return new VTJHenkiloVastaussanoma.Henkilo.Edunvalvonta();
    }

    /**
     * Create an instance of {@link VTJHenkiloVastaussanoma.Henkilo.Edunvalvonta.HenkiloEdunvalvoja }
     * 
     */
    public VTJHenkiloVastaussanoma.Henkilo.Edunvalvonta.HenkiloEdunvalvoja createVTJHenkiloVastaussanomaHenkiloEdunvalvontaHenkiloEdunvalvoja() {
        return new VTJHenkiloVastaussanoma.Henkilo.Edunvalvonta.HenkiloEdunvalvoja();
    }

    /**
     * Create an instance of {@link VTJHenkiloVastaussanoma.Henkilo.Lapsi }
     * 
     */
    public VTJHenkiloVastaussanoma.Henkilo.Lapsi createVTJHenkiloVastaussanomaHenkiloLapsi() {
        return new VTJHenkiloVastaussanoma.Henkilo.Lapsi();
    }

    /**
     * Create an instance of {@link VTJHenkiloVastaussanoma.Henkilo.Huollettava }
     * 
     */
    public VTJHenkiloVastaussanoma.Henkilo.Huollettava createVTJHenkiloVastaussanomaHenkiloHuollettava() {
        return new VTJHenkiloVastaussanoma.Henkilo.Huollettava();
    }

    /**
     * Create an instance of {@link VTJHenkiloVastaussanoma.Henkilo.Huoltaja }
     * 
     */
    public VTJHenkiloVastaussanoma.Henkilo.Huoltaja createVTJHenkiloVastaussanomaHenkiloHuoltaja() {
        return new VTJHenkiloVastaussanoma.Henkilo.Huoltaja();
    }

    /**
     * Create an instance of {@link VTJHenkiloVastaussanoma.Hakuperusteet }
     * 
     */
    public VTJHenkiloVastaussanoma.Hakuperusteet createVTJHenkiloVastaussanomaHakuperusteet() {
        return new VTJHenkiloVastaussanoma.Hakuperusteet();
    }

    /**
     * Create an instance of {@link XRoadClientIdentifierType }
     * 
     */
    public XRoadClientIdentifierType createXRoadClientIdentifierType() {
        return new XRoadClientIdentifierType();
    }

    /**
     * Create an instance of {@link XRoadServiceIdentifierType }
     * 
     */
    public XRoadServiceIdentifierType createXRoadServiceIdentifierType() {
        return new XRoadServiceIdentifierType();
    }

    /**
     * Create an instance of {@link XRoadCentralServiceIdentifierType }
     * 
     */
    public XRoadCentralServiceIdentifierType createXRoadCentralServiceIdentifierType() {
        return new XRoadCentralServiceIdentifierType();
    }

    /**
     * Create an instance of {@link RequestHash }
     * 
     */
    public RequestHash createRequestHash() {
        return new RequestHash();
    }

    /**
     * Create an instance of {@link Title }
     * 
     */
    public Title createTitle() {
        return new Title();
    }

    /**
     * Create an instance of {@link Notes }
     * 
     */
    public Notes createNotes() {
        return new Notes();
    }

    /**
     * Create an instance of {@link TechNotes }
     * 
     */
    public TechNotes createTechNotes() {
        return new TechNotes();
    }

    /**
     * Create an instance of {@link XRoadIdentifierType }
     * 
     */
    public XRoadIdentifierType createXRoadIdentifierType() {
        return new XRoadIdentifierType();
    }

    /**
     * Create an instance of {@link XRoadSecurityCategoryIdentifierType }
     * 
     */
    public XRoadSecurityCategoryIdentifierType createXRoadSecurityCategoryIdentifierType() {
        return new XRoadSecurityCategoryIdentifierType();
    }

    /**
     * Create an instance of {@link XRoadSecurityServerIdentifierType }
     * 
     */
    public XRoadSecurityServerIdentifierType createXRoadSecurityServerIdentifierType() {
        return new XRoadSecurityServerIdentifierType();
    }

    /**
     * Create an instance of {@link XRoadGlobalGroupIdentifierType }
     * 
     */
    public XRoadGlobalGroupIdentifierType createXRoadGlobalGroupIdentifierType() {
        return new XRoadGlobalGroupIdentifierType();
    }

    /**
     * Create an instance of {@link XRoadLocalGroupIdentifierType }
     * 
     */
    public XRoadLocalGroupIdentifierType createXRoadLocalGroupIdentifierType() {
        return new XRoadLocalGroupIdentifierType();
    }

    /**
     * Create an instance of {@link HenkiloTunnusKyselyReqBody }
     * 
     */
    public HenkiloTunnusKyselyReqBody createHenkiloTunnusKyselyReqBody() {
        return new HenkiloTunnusKyselyReqBody();
    }

    /**
     * Create an instance of {@link HenkiloTunnusKyselyResBody }
     * 
     */
    public HenkiloTunnusKyselyResBody createHenkiloTunnusKyselyResBody() {
        return new HenkiloTunnusKyselyResBody();
    }

    /**
     * Create an instance of {@link HenkiloTunnistusKyselyReqBody }
     * 
     */
    public HenkiloTunnistusKyselyReqBody createHenkiloTunnistusKyselyReqBody() {
        return new HenkiloTunnistusKyselyReqBody();
    }

    /**
     * Create an instance of {@link HenkiloTunnistusKyselyResBody }
     * 
     */
    public HenkiloTunnistusKyselyResBody createHenkiloTunnistusKyselyResBody() {
        return new HenkiloTunnistusKyselyResBody();
    }

    /**
     * Create an instance of {@link RakennusTunnusKyselyReqBody }
     * 
     */
    public RakennusTunnusKyselyReqBody createRakennusTunnusKyselyReqBody() {
        return new RakennusTunnusKyselyReqBody();
    }

    /**
     * Create an instance of {@link RakennusTunnusKyselyResBody }
     * 
     */
    public RakennusTunnusKyselyResBody createRakennusTunnusKyselyResBody() {
        return new RakennusTunnusKyselyResBody();
    }

    /**
     * Create an instance of {@link RakennusTunnistusKyselyReqBody }
     * 
     */
    public RakennusTunnistusKyselyReqBody createRakennusTunnistusKyselyReqBody() {
        return new RakennusTunnistusKyselyReqBody();
    }

    /**
     * Create an instance of {@link RakennusTunnistusKyselyResBody }
     * 
     */
    public RakennusTunnistusKyselyResBody createRakennusTunnistusKyselyResBody() {
        return new RakennusTunnistusKyselyResBody();
    }

    /**
     * Create an instance of {@link KiinteistoTunnusKyselyReqBody }
     * 
     */
    public KiinteistoTunnusKyselyReqBody createKiinteistoTunnusKyselyReqBody() {
        return new KiinteistoTunnusKyselyReqBody();
    }

    /**
     * Create an instance of {@link KiinteistoTunnusKyselyResBody }
     * 
     */
    public KiinteistoTunnusKyselyResBody createKiinteistoTunnusKyselyResBody() {
        return new KiinteistoTunnusKyselyResBody();
    }

    /**
     * Create an instance of {@link KiinteistoTunnistusKyselyReqBody }
     * 
     */
    public KiinteistoTunnistusKyselyReqBody createKiinteistoTunnistusKyselyReqBody() {
        return new KiinteistoTunnistusKyselyReqBody();
    }

    /**
     * Create an instance of {@link KiinteistoTunnistusKyselyResBody }
     * 
     */
    public KiinteistoTunnistusKyselyResBody createKiinteistoTunnistusKyselyResBody() {
        return new KiinteistoTunnistusKyselyResBody();
    }

    /**
     * Create an instance of {@link TuotteenSkeemaReqBody }
     * 
     */
    public TuotteenSkeemaReqBody createTuotteenSkeemaReqBody() {
        return new TuotteenSkeemaReqBody();
    }

    /**
     * Create an instance of {@link TuotteenSkeemaResBody }
     * 
     */
    public TuotteenSkeemaResBody createTuotteenSkeemaResBody() {
        return new TuotteenSkeemaResBody();
    }

    /**
     * Create an instance of {@link HenkiloTunnusKyselyReqBodyTiedot }
     * 
     */
    public HenkiloTunnusKyselyReqBodyTiedot createHenkiloTunnusKyselyReqBodyTiedot() {
        return new HenkiloTunnusKyselyReqBodyTiedot();
    }

    /**
     * Create an instance of {@link FaultResponseType }
     * 
     */
    public FaultResponseType createFaultResponseType() {
        return new FaultResponseType();
    }

    /**
     * Create an instance of {@link HenkiloTunnusKyselyResType }
     * 
     */
    public HenkiloTunnusKyselyResType createHenkiloTunnusKyselyResType() {
        return new HenkiloTunnusKyselyResType();
    }

    /**
     * Create an instance of {@link HenkiloTunnistusKyselyReqBodyTiedot }
     * 
     */
    public HenkiloTunnistusKyselyReqBodyTiedot createHenkiloTunnistusKyselyReqBodyTiedot() {
        return new HenkiloTunnistusKyselyReqBodyTiedot();
    }

    /**
     * Create an instance of {@link HenkiloTunnistusKyselyResType }
     * 
     */
    public HenkiloTunnistusKyselyResType createHenkiloTunnistusKyselyResType() {
        return new HenkiloTunnistusKyselyResType();
    }

    /**
     * Create an instance of {@link RakennusTunnusKyselyReqBodyTiedot }
     * 
     */
    public RakennusTunnusKyselyReqBodyTiedot createRakennusTunnusKyselyReqBodyTiedot() {
        return new RakennusTunnusKyselyReqBodyTiedot();
    }

    /**
     * Create an instance of {@link RakennusTunnusKyselyResType }
     * 
     */
    public RakennusTunnusKyselyResType createRakennusTunnusKyselyResType() {
        return new RakennusTunnusKyselyResType();
    }

    /**
     * Create an instance of {@link RakennusTunnistusKyselyReqBodyTiedot }
     * 
     */
    public RakennusTunnistusKyselyReqBodyTiedot createRakennusTunnistusKyselyReqBodyTiedot() {
        return new RakennusTunnistusKyselyReqBodyTiedot();
    }

    /**
     * Create an instance of {@link RakennusTunnistusKyselyResType }
     * 
     */
    public RakennusTunnistusKyselyResType createRakennusTunnistusKyselyResType() {
        return new RakennusTunnistusKyselyResType();
    }

    /**
     * Create an instance of {@link KiinteistoTunnusKyselyReqBodyTiedot }
     * 
     */
    public KiinteistoTunnusKyselyReqBodyTiedot createKiinteistoTunnusKyselyReqBodyTiedot() {
        return new KiinteistoTunnusKyselyReqBodyTiedot();
    }

    /**
     * Create an instance of {@link KiinteistoTunnusKyselyResType }
     * 
     */
    public KiinteistoTunnusKyselyResType createKiinteistoTunnusKyselyResType() {
        return new KiinteistoTunnusKyselyResType();
    }

    /**
     * Create an instance of {@link KiinteistoTunnistusKyselyReqBodyTiedot }
     * 
     */
    public KiinteistoTunnistusKyselyReqBodyTiedot createKiinteistoTunnistusKyselyReqBodyTiedot() {
        return new KiinteistoTunnistusKyselyReqBodyTiedot();
    }

    /**
     * Create an instance of {@link KiinteistoTunnistusKyselyResType }
     * 
     */
    public KiinteistoTunnistusKyselyResType createKiinteistoTunnistusKyselyResType() {
        return new KiinteistoTunnistusKyselyResType();
    }

    /**
     * Create an instance of {@link TuotteenSkeemaReqBodyTiedot }
     * 
     */
    public TuotteenSkeemaReqBodyTiedot createTuotteenSkeemaReqBodyTiedot() {
        return new TuotteenSkeemaReqBodyTiedot();
    }

    /**
     * Create an instance of {@link TuotteenSkeemaResType }
     * 
     */
    public TuotteenSkeemaResType createTuotteenSkeemaResType() {
        return new TuotteenSkeemaResType();
    }

    /**
     * Create an instance of {@link VTJHenkiloVastaussanoma.Asiakasinfo }
     * 
     */
    public VTJHenkiloVastaussanoma.Asiakasinfo createVTJHenkiloVastaussanomaAsiakasinfo() {
        return new VTJHenkiloVastaussanoma.Asiakasinfo();
    }

    /**
     * Create an instance of {@link VTJHenkiloVastaussanoma.Paluukoodi }
     * 
     */
    public VTJHenkiloVastaussanoma.Paluukoodi createVTJHenkiloVastaussanomaPaluukoodi() {
        return new VTJHenkiloVastaussanoma.Paluukoodi();
    }

    /**
     * Create an instance of {@link VTJHenkiloVastaussanoma.Henkilo.Henkilotunnus }
     * 
     */
    public VTJHenkiloVastaussanoma.Henkilo.Henkilotunnus createVTJHenkiloVastaussanomaHenkiloHenkilotunnus() {
        return new VTJHenkiloVastaussanoma.Henkilo.Henkilotunnus();
    }

    /**
     * Create an instance of {@link VTJHenkiloVastaussanoma.Henkilo.NykyinenSukunimi }
     * 
     */
    public VTJHenkiloVastaussanoma.Henkilo.NykyinenSukunimi createVTJHenkiloVastaussanomaHenkiloNykyinenSukunimi() {
        return new VTJHenkiloVastaussanoma.Henkilo.NykyinenSukunimi();
    }

    /**
     * Create an instance of {@link VTJHenkiloVastaussanoma.Henkilo.NykyisetEtunimet }
     * 
     */
    public VTJHenkiloVastaussanoma.Henkilo.NykyisetEtunimet createVTJHenkiloVastaussanomaHenkiloNykyisetEtunimet() {
        return new VTJHenkiloVastaussanoma.Henkilo.NykyisetEtunimet();
    }

    /**
     * Create an instance of {@link VTJHenkiloVastaussanoma.Henkilo.EntinenNimi }
     * 
     */
    public VTJHenkiloVastaussanoma.Henkilo.EntinenNimi createVTJHenkiloVastaussanomaHenkiloEntinenNimi() {
        return new VTJHenkiloVastaussanoma.Henkilo.EntinenNimi();
    }

    /**
     * Create an instance of {@link VTJHenkiloVastaussanoma.Henkilo.VakinainenKotimainenLahiosoite }
     * 
     */
    public VTJHenkiloVastaussanoma.Henkilo.VakinainenKotimainenLahiosoite createVTJHenkiloVastaussanomaHenkiloVakinainenKotimainenLahiosoite() {
        return new VTJHenkiloVastaussanoma.Henkilo.VakinainenKotimainenLahiosoite();
    }

    /**
     * Create an instance of {@link VTJHenkiloVastaussanoma.Henkilo.VakinainenUlkomainenLahiosoite }
     * 
     */
    public VTJHenkiloVastaussanoma.Henkilo.VakinainenUlkomainenLahiosoite createVTJHenkiloVastaussanomaHenkiloVakinainenUlkomainenLahiosoite() {
        return new VTJHenkiloVastaussanoma.Henkilo.VakinainenUlkomainenLahiosoite();
    }

    /**
     * Create an instance of {@link VTJHenkiloVastaussanoma.Henkilo.VakinainenAsuinpaikka }
     * 
     */
    public VTJHenkiloVastaussanoma.Henkilo.VakinainenAsuinpaikka createVTJHenkiloVastaussanomaHenkiloVakinainenAsuinpaikka() {
        return new VTJHenkiloVastaussanoma.Henkilo.VakinainenAsuinpaikka();
    }

    /**
     * Create an instance of {@link VTJHenkiloVastaussanoma.Henkilo.TilapainenKotimainenLahiosoite }
     * 
     */
    public VTJHenkiloVastaussanoma.Henkilo.TilapainenKotimainenLahiosoite createVTJHenkiloVastaussanomaHenkiloTilapainenKotimainenLahiosoite() {
        return new VTJHenkiloVastaussanoma.Henkilo.TilapainenKotimainenLahiosoite();
    }

    /**
     * Create an instance of {@link VTJHenkiloVastaussanoma.Henkilo.TilapainenUlkomainenLahiosoite }
     * 
     */
    public VTJHenkiloVastaussanoma.Henkilo.TilapainenUlkomainenLahiosoite createVTJHenkiloVastaussanomaHenkiloTilapainenUlkomainenLahiosoite() {
        return new VTJHenkiloVastaussanoma.Henkilo.TilapainenUlkomainenLahiosoite();
    }

    /**
     * Create an instance of {@link VTJHenkiloVastaussanoma.Henkilo.KotimainenPostiosoite }
     * 
     */
    public VTJHenkiloVastaussanoma.Henkilo.KotimainenPostiosoite createVTJHenkiloVastaussanomaHenkiloKotimainenPostiosoite() {
        return new VTJHenkiloVastaussanoma.Henkilo.KotimainenPostiosoite();
    }

    /**
     * Create an instance of {@link VTJHenkiloVastaussanoma.Henkilo.UlkomainenPostiosoite }
     * 
     */
    public VTJHenkiloVastaussanoma.Henkilo.UlkomainenPostiosoite createVTJHenkiloVastaussanomaHenkiloUlkomainenPostiosoite() {
        return new VTJHenkiloVastaussanoma.Henkilo.UlkomainenPostiosoite();
    }

    /**
     * Create an instance of {@link VTJHenkiloVastaussanoma.Henkilo.Kotikunta }
     * 
     */
    public VTJHenkiloVastaussanoma.Henkilo.Kotikunta createVTJHenkiloVastaussanomaHenkiloKotikunta() {
        return new VTJHenkiloVastaussanoma.Henkilo.Kotikunta();
    }

    /**
     * Create an instance of {@link VTJHenkiloVastaussanoma.Henkilo.Kuolintiedot }
     * 
     */
    public VTJHenkiloVastaussanoma.Henkilo.Kuolintiedot createVTJHenkiloVastaussanomaHenkiloKuolintiedot() {
        return new VTJHenkiloVastaussanoma.Henkilo.Kuolintiedot();
    }

    /**
     * Create an instance of {@link VTJHenkiloVastaussanoma.Henkilo.Kuolleeksijulistamistiedot }
     * 
     */
    public VTJHenkiloVastaussanoma.Henkilo.Kuolleeksijulistamistiedot createVTJHenkiloVastaussanomaHenkiloKuolleeksijulistamistiedot() {
        return new VTJHenkiloVastaussanoma.Henkilo.Kuolleeksijulistamistiedot();
    }

    /**
     * Create an instance of {@link VTJHenkiloVastaussanoma.Henkilo.Aidinkieli }
     * 
     */
    public VTJHenkiloVastaussanoma.Henkilo.Aidinkieli createVTJHenkiloVastaussanomaHenkiloAidinkieli() {
        return new VTJHenkiloVastaussanoma.Henkilo.Aidinkieli();
    }

    /**
     * Create an instance of {@link VTJHenkiloVastaussanoma.Henkilo.Turvakielto }
     * 
     */
    public VTJHenkiloVastaussanoma.Henkilo.Turvakielto createVTJHenkiloVastaussanomaHenkiloTurvakielto() {
        return new VTJHenkiloVastaussanoma.Henkilo.Turvakielto();
    }

    /**
     * Create an instance of {@link VTJHenkiloVastaussanoma.Henkilo.Sukupuoli }
     * 
     */
    public VTJHenkiloVastaussanoma.Henkilo.Sukupuoli createVTJHenkiloVastaussanomaHenkiloSukupuoli() {
        return new VTJHenkiloVastaussanoma.Henkilo.Sukupuoli();
    }

    /**
     * Create an instance of {@link VTJHenkiloVastaussanoma.Henkilo.Kansalaisuus }
     * 
     */
    public VTJHenkiloVastaussanoma.Henkilo.Kansalaisuus createVTJHenkiloVastaussanomaHenkiloKansalaisuus() {
        return new VTJHenkiloVastaussanoma.Henkilo.Kansalaisuus();
    }

    /**
     * Create an instance of {@link VTJHenkiloVastaussanoma.Henkilo.AsukkaatAlle18V }
     * 
     */
    public VTJHenkiloVastaussanoma.Henkilo.AsukkaatAlle18V createVTJHenkiloVastaussanomaHenkiloAsukkaatAlle18V() {
        return new VTJHenkiloVastaussanoma.Henkilo.AsukkaatAlle18V();
    }

    /**
     * Create an instance of {@link VTJHenkiloVastaussanoma.Henkilo.AsukasAlle18V }
     * 
     */
    public VTJHenkiloVastaussanoma.Henkilo.AsukasAlle18V createVTJHenkiloVastaussanomaHenkiloAsukasAlle18V() {
        return new VTJHenkiloVastaussanoma.Henkilo.AsukasAlle18V();
    }

    /**
     * Create an instance of {@link VTJHenkiloVastaussanoma.Henkilo.Syntymamaa }
     * 
     */
    public VTJHenkiloVastaussanoma.Henkilo.Syntymamaa createVTJHenkiloVastaussanomaHenkiloSyntymamaa() {
        return new VTJHenkiloVastaussanoma.Henkilo.Syntymamaa();
    }

    /**
     * Create an instance of {@link VTJHenkiloVastaussanoma.Henkilo.Edunvalvontavaltuutus.HenkiloEdunvalvontavaltuutettu.NykyinenSukunimi }
     * 
     */
    public VTJHenkiloVastaussanoma.Henkilo.Edunvalvontavaltuutus.HenkiloEdunvalvontavaltuutettu.NykyinenSukunimi createVTJHenkiloVastaussanomaHenkiloEdunvalvontavaltuutusHenkiloEdunvalvontavaltuutettuNykyinenSukunimi() {
        return new VTJHenkiloVastaussanoma.Henkilo.Edunvalvontavaltuutus.HenkiloEdunvalvontavaltuutettu.NykyinenSukunimi();
    }

    /**
     * Create an instance of {@link VTJHenkiloVastaussanoma.Henkilo.Edunvalvontavaltuutus.HenkiloEdunvalvontavaltuutettu.NykyisetEtunimet }
     * 
     */
    public VTJHenkiloVastaussanoma.Henkilo.Edunvalvontavaltuutus.HenkiloEdunvalvontavaltuutettu.NykyisetEtunimet createVTJHenkiloVastaussanomaHenkiloEdunvalvontavaltuutusHenkiloEdunvalvontavaltuutettuNykyisetEtunimet() {
        return new VTJHenkiloVastaussanoma.Henkilo.Edunvalvontavaltuutus.HenkiloEdunvalvontavaltuutettu.NykyisetEtunimet();
    }

    /**
     * Create an instance of {@link VTJHenkiloVastaussanoma.Henkilo.Edunvalvonta.YritysJaYhteisoEdunvalvoja }
     * 
     */
    public VTJHenkiloVastaussanoma.Henkilo.Edunvalvonta.YritysJaYhteisoEdunvalvoja createVTJHenkiloVastaussanomaHenkiloEdunvalvontaYritysJaYhteisoEdunvalvoja() {
        return new VTJHenkiloVastaussanoma.Henkilo.Edunvalvonta.YritysJaYhteisoEdunvalvoja();
    }

    /**
     * Create an instance of {@link VTJHenkiloVastaussanoma.Henkilo.Edunvalvonta.OikeusaputoimistoEdunvalvoja }
     * 
     */
    public VTJHenkiloVastaussanoma.Henkilo.Edunvalvonta.OikeusaputoimistoEdunvalvoja createVTJHenkiloVastaussanomaHenkiloEdunvalvontaOikeusaputoimistoEdunvalvoja() {
        return new VTJHenkiloVastaussanoma.Henkilo.Edunvalvonta.OikeusaputoimistoEdunvalvoja();
    }

    /**
     * Create an instance of {@link VTJHenkiloVastaussanoma.Henkilo.Edunvalvonta.HenkiloEdunvalvoja.NykyinenSukunimi }
     * 
     */
    public VTJHenkiloVastaussanoma.Henkilo.Edunvalvonta.HenkiloEdunvalvoja.NykyinenSukunimi createVTJHenkiloVastaussanomaHenkiloEdunvalvontaHenkiloEdunvalvojaNykyinenSukunimi() {
        return new VTJHenkiloVastaussanoma.Henkilo.Edunvalvonta.HenkiloEdunvalvoja.NykyinenSukunimi();
    }

    /**
     * Create an instance of {@link VTJHenkiloVastaussanoma.Henkilo.Edunvalvonta.HenkiloEdunvalvoja.NykyisetEtunimet }
     * 
     */
    public VTJHenkiloVastaussanoma.Henkilo.Edunvalvonta.HenkiloEdunvalvoja.NykyisetEtunimet createVTJHenkiloVastaussanomaHenkiloEdunvalvontaHenkiloEdunvalvojaNykyisetEtunimet() {
        return new VTJHenkiloVastaussanoma.Henkilo.Edunvalvonta.HenkiloEdunvalvoja.NykyisetEtunimet();
    }

    /**
     * Create an instance of {@link VTJHenkiloVastaussanoma.Henkilo.Lapsi.NykyinenSukunimi }
     * 
     */
    public VTJHenkiloVastaussanoma.Henkilo.Lapsi.NykyinenSukunimi createVTJHenkiloVastaussanomaHenkiloLapsiNykyinenSukunimi() {
        return new VTJHenkiloVastaussanoma.Henkilo.Lapsi.NykyinenSukunimi();
    }

    /**
     * Create an instance of {@link VTJHenkiloVastaussanoma.Henkilo.Lapsi.NykyisetEtunimet }
     * 
     */
    public VTJHenkiloVastaussanoma.Henkilo.Lapsi.NykyisetEtunimet createVTJHenkiloVastaussanomaHenkiloLapsiNykyisetEtunimet() {
        return new VTJHenkiloVastaussanoma.Henkilo.Lapsi.NykyisetEtunimet();
    }

    /**
     * Create an instance of {@link VTJHenkiloVastaussanoma.Henkilo.Huollettava.NykyinenSukunimi }
     * 
     */
    public VTJHenkiloVastaussanoma.Henkilo.Huollettava.NykyinenSukunimi createVTJHenkiloVastaussanomaHenkiloHuollettavaNykyinenSukunimi() {
        return new VTJHenkiloVastaussanoma.Henkilo.Huollettava.NykyinenSukunimi();
    }

    /**
     * Create an instance of {@link VTJHenkiloVastaussanoma.Henkilo.Huollettava.NykyisetEtunimet }
     * 
     */
    public VTJHenkiloVastaussanoma.Henkilo.Huollettava.NykyisetEtunimet createVTJHenkiloVastaussanomaHenkiloHuollettavaNykyisetEtunimet() {
        return new VTJHenkiloVastaussanoma.Henkilo.Huollettava.NykyisetEtunimet();
    }

    /**
     * Create an instance of {@link VTJHenkiloVastaussanoma.Henkilo.Huoltaja.NykyinenSukunimi }
     * 
     */
    public VTJHenkiloVastaussanoma.Henkilo.Huoltaja.NykyinenSukunimi createVTJHenkiloVastaussanomaHenkiloHuoltajaNykyinenSukunimi() {
        return new VTJHenkiloVastaussanoma.Henkilo.Huoltaja.NykyinenSukunimi();
    }

    /**
     * Create an instance of {@link VTJHenkiloVastaussanoma.Henkilo.Huoltaja.NykyisetEtunimet }
     * 
     */
    public VTJHenkiloVastaussanoma.Henkilo.Huoltaja.NykyisetEtunimet createVTJHenkiloVastaussanomaHenkiloHuoltajaNykyisetEtunimet() {
        return new VTJHenkiloVastaussanoma.Henkilo.Huoltaja.NykyisetEtunimet();
    }

    /**
     * Create an instance of {@link VTJHenkiloVastaussanoma.Hakuperusteet.Henkilotunnus }
     * 
     */
    public VTJHenkiloVastaussanoma.Hakuperusteet.Henkilotunnus createVTJHenkiloVastaussanomaHakuperusteetHenkilotunnus() {
        return new VTJHenkiloVastaussanoma.Hakuperusteet.Henkilotunnus();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link XRoadClientIdentifierType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link XRoadClientIdentifierType }{@code >}
     */
    @XmlElementDecl(namespace = "http://x-road.eu/xsd/xroad.xsd", name = "client")
    public JAXBElement<XRoadClientIdentifierType> createClient(XRoadClientIdentifierType value) {
        return new JAXBElement<XRoadClientIdentifierType>(_Client_QNAME, XRoadClientIdentifierType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link XRoadServiceIdentifierType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link XRoadServiceIdentifierType }{@code >}
     */
    @XmlElementDecl(namespace = "http://x-road.eu/xsd/xroad.xsd", name = "service")
    public JAXBElement<XRoadServiceIdentifierType> createService(XRoadServiceIdentifierType value) {
        return new JAXBElement<XRoadServiceIdentifierType>(_Service_QNAME, XRoadServiceIdentifierType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link XRoadCentralServiceIdentifierType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link XRoadCentralServiceIdentifierType }{@code >}
     */
    @XmlElementDecl(namespace = "http://x-road.eu/xsd/xroad.xsd", name = "centralService")
    public JAXBElement<XRoadCentralServiceIdentifierType> createCentralService(XRoadCentralServiceIdentifierType value) {
        return new JAXBElement<XRoadCentralServiceIdentifierType>(_CentralService_QNAME, XRoadCentralServiceIdentifierType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    @XmlElementDecl(namespace = "http://x-road.eu/xsd/xroad.xsd", name = "id")
    public JAXBElement<String> createId(String value) {
        return new JAXBElement<String>(_Id_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    @XmlElementDecl(namespace = "http://x-road.eu/xsd/xroad.xsd", name = "userId")
    public JAXBElement<String> createUserId(String value) {
        return new JAXBElement<String>(_UserId_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    @XmlElementDecl(namespace = "http://x-road.eu/xsd/xroad.xsd", name = "issue")
    public JAXBElement<String> createIssue(String value) {
        return new JAXBElement<String>(_Issue_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    @XmlElementDecl(namespace = "http://x-road.eu/xsd/xroad.xsd", name = "protocolVersion")
    public JAXBElement<String> createProtocolVersion(String value) {
        return new JAXBElement<String>(_ProtocolVersion_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    @XmlElementDecl(namespace = "http://x-road.eu/xsd/xroad.xsd", name = "version")
    public JAXBElement<String> createVersion(String value) {
        return new JAXBElement<String>(_Version_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    @XmlElementDecl(namespace = "http://x-road.eu/xsd/identifiers", name = "xRoadInstance")
    public JAXBElement<String> createXRoadInstance(String value) {
        return new JAXBElement<String>(_XRoadInstance_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    @XmlElementDecl(namespace = "http://x-road.eu/xsd/identifiers", name = "memberClass")
    public JAXBElement<String> createMemberClass(String value) {
        return new JAXBElement<String>(_MemberClass_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    @XmlElementDecl(namespace = "http://x-road.eu/xsd/identifiers", name = "memberCode")
    public JAXBElement<String> createMemberCode(String value) {
        return new JAXBElement<String>(_MemberCode_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    @XmlElementDecl(namespace = "http://x-road.eu/xsd/identifiers", name = "subsystemCode")
    public JAXBElement<String> createSubsystemCode(String value) {
        return new JAXBElement<String>(_SubsystemCode_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    @XmlElementDecl(namespace = "http://x-road.eu/xsd/identifiers", name = "groupCode")
    public JAXBElement<String> createGroupCode(String value) {
        return new JAXBElement<String>(_GroupCode_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    @XmlElementDecl(namespace = "http://x-road.eu/xsd/identifiers", name = "serviceCode")
    public JAXBElement<String> createServiceCode(String value) {
        return new JAXBElement<String>(_ServiceCode_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    @XmlElementDecl(namespace = "http://x-road.eu/xsd/identifiers", name = "serviceVersion")
    public JAXBElement<String> createServiceVersion(String value) {
        return new JAXBElement<String>(_ServiceVersion_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    @XmlElementDecl(namespace = "http://x-road.eu/xsd/identifiers", name = "securityCategoryCode")
    public JAXBElement<String> createSecurityCategoryCode(String value) {
        return new JAXBElement<String>(_SecurityCategoryCode_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    @XmlElementDecl(namespace = "http://x-road.eu/xsd/identifiers", name = "serverCode")
    public JAXBElement<String> createServerCode(String value) {
        return new JAXBElement<String>(_ServerCode_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link HenkiloTunnusKyselyReqBody }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link HenkiloTunnusKyselyReqBody }{@code >}
     */
    @XmlElementDecl(namespace = "http://xml.vrk.fi/ws/vtj/vtjkysely/1", name = "HenkilonTunnusKysely")
    public JAXBElement<HenkiloTunnusKyselyReqBody> createHenkilonTunnusKysely(HenkiloTunnusKyselyReqBody value) {
        return new JAXBElement<HenkiloTunnusKyselyReqBody>(_HenkilonTunnusKysely_QNAME, HenkiloTunnusKyselyReqBody.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link HenkiloTunnusKyselyResBody }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link HenkiloTunnusKyselyResBody }{@code >}
     */
    @XmlElementDecl(namespace = "http://xml.vrk.fi/ws/vtj/vtjkysely/1", name = "HenkilonTunnusKyselyResponse")
    public JAXBElement<HenkiloTunnusKyselyResBody> createHenkilonTunnusKyselyResponse(HenkiloTunnusKyselyResBody value) {
        return new JAXBElement<HenkiloTunnusKyselyResBody>(_HenkilonTunnusKyselyResponse_QNAME, HenkiloTunnusKyselyResBody.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link HenkiloTunnistusKyselyReqBody }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link HenkiloTunnistusKyselyReqBody }{@code >}
     */
    @XmlElementDecl(namespace = "http://xml.vrk.fi/ws/vtj/vtjkysely/1", name = "HenkilonTunnistusKysely")
    public JAXBElement<HenkiloTunnistusKyselyReqBody> createHenkilonTunnistusKysely(HenkiloTunnistusKyselyReqBody value) {
        return new JAXBElement<HenkiloTunnistusKyselyReqBody>(_HenkilonTunnistusKysely_QNAME, HenkiloTunnistusKyselyReqBody.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link HenkiloTunnistusKyselyResBody }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link HenkiloTunnistusKyselyResBody }{@code >}
     */
    @XmlElementDecl(namespace = "http://xml.vrk.fi/ws/vtj/vtjkysely/1", name = "HenkilonTunnistusKyselyResponse")
    public JAXBElement<HenkiloTunnistusKyselyResBody> createHenkilonTunnistusKyselyResponse(HenkiloTunnistusKyselyResBody value) {
        return new JAXBElement<HenkiloTunnistusKyselyResBody>(_HenkilonTunnistusKyselyResponse_QNAME, HenkiloTunnistusKyselyResBody.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link RakennusTunnusKyselyReqBody }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link RakennusTunnusKyselyReqBody }{@code >}
     */
    @XmlElementDecl(namespace = "http://xml.vrk.fi/ws/vtj/vtjkysely/1", name = "RakennuksenTunnusKysely")
    public JAXBElement<RakennusTunnusKyselyReqBody> createRakennuksenTunnusKysely(RakennusTunnusKyselyReqBody value) {
        return new JAXBElement<RakennusTunnusKyselyReqBody>(_RakennuksenTunnusKysely_QNAME, RakennusTunnusKyselyReqBody.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link RakennusTunnusKyselyResBody }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link RakennusTunnusKyselyResBody }{@code >}
     */
    @XmlElementDecl(namespace = "http://xml.vrk.fi/ws/vtj/vtjkysely/1", name = "RakennuksenTunnusKyselyResponse")
    public JAXBElement<RakennusTunnusKyselyResBody> createRakennuksenTunnusKyselyResponse(RakennusTunnusKyselyResBody value) {
        return new JAXBElement<RakennusTunnusKyselyResBody>(_RakennuksenTunnusKyselyResponse_QNAME, RakennusTunnusKyselyResBody.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link RakennusTunnistusKyselyReqBody }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link RakennusTunnistusKyselyReqBody }{@code >}
     */
    @XmlElementDecl(namespace = "http://xml.vrk.fi/ws/vtj/vtjkysely/1", name = "RakennuksenTunnistusKysely")
    public JAXBElement<RakennusTunnistusKyselyReqBody> createRakennuksenTunnistusKysely(RakennusTunnistusKyselyReqBody value) {
        return new JAXBElement<RakennusTunnistusKyselyReqBody>(_RakennuksenTunnistusKysely_QNAME, RakennusTunnistusKyselyReqBody.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link RakennusTunnistusKyselyResBody }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link RakennusTunnistusKyselyResBody }{@code >}
     */
    @XmlElementDecl(namespace = "http://xml.vrk.fi/ws/vtj/vtjkysely/1", name = "RakennuksenTunnistusKyselyResponse")
    public JAXBElement<RakennusTunnistusKyselyResBody> createRakennuksenTunnistusKyselyResponse(RakennusTunnistusKyselyResBody value) {
        return new JAXBElement<RakennusTunnistusKyselyResBody>(_RakennuksenTunnistusKyselyResponse_QNAME, RakennusTunnistusKyselyResBody.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link KiinteistoTunnusKyselyReqBody }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link KiinteistoTunnusKyselyReqBody }{@code >}
     */
    @XmlElementDecl(namespace = "http://xml.vrk.fi/ws/vtj/vtjkysely/1", name = "KiinteistonTunnusKysely")
    public JAXBElement<KiinteistoTunnusKyselyReqBody> createKiinteistonTunnusKysely(KiinteistoTunnusKyselyReqBody value) {
        return new JAXBElement<KiinteistoTunnusKyselyReqBody>(_KiinteistonTunnusKysely_QNAME, KiinteistoTunnusKyselyReqBody.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link KiinteistoTunnusKyselyResBody }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link KiinteistoTunnusKyselyResBody }{@code >}
     */
    @XmlElementDecl(namespace = "http://xml.vrk.fi/ws/vtj/vtjkysely/1", name = "KiinteistonTunnusKyselyResponse")
    public JAXBElement<KiinteistoTunnusKyselyResBody> createKiinteistonTunnusKyselyResponse(KiinteistoTunnusKyselyResBody value) {
        return new JAXBElement<KiinteistoTunnusKyselyResBody>(_KiinteistonTunnusKyselyResponse_QNAME, KiinteistoTunnusKyselyResBody.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link KiinteistoTunnistusKyselyReqBody }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link KiinteistoTunnistusKyselyReqBody }{@code >}
     */
    @XmlElementDecl(namespace = "http://xml.vrk.fi/ws/vtj/vtjkysely/1", name = "KiinteistonTunnistusKysely")
    public JAXBElement<KiinteistoTunnistusKyselyReqBody> createKiinteistonTunnistusKysely(KiinteistoTunnistusKyselyReqBody value) {
        return new JAXBElement<KiinteistoTunnistusKyselyReqBody>(_KiinteistonTunnistusKysely_QNAME, KiinteistoTunnistusKyselyReqBody.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link KiinteistoTunnistusKyselyResBody }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link KiinteistoTunnistusKyselyResBody }{@code >}
     */
    @XmlElementDecl(namespace = "http://xml.vrk.fi/ws/vtj/vtjkysely/1", name = "KiinteistonTunnistusKyselyResponse")
    public JAXBElement<KiinteistoTunnistusKyselyResBody> createKiinteistonTunnistusKyselyResponse(KiinteistoTunnistusKyselyResBody value) {
        return new JAXBElement<KiinteistoTunnistusKyselyResBody>(_KiinteistonTunnistusKyselyResponse_QNAME, KiinteistoTunnistusKyselyResBody.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link TuotteenSkeemaReqBody }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link TuotteenSkeemaReqBody }{@code >}
     */
    @XmlElementDecl(namespace = "http://xml.vrk.fi/ws/vtj/vtjkysely/1", name = "HaeTuotteenSkeema")
    public JAXBElement<TuotteenSkeemaReqBody> createHaeTuotteenSkeema(TuotteenSkeemaReqBody value) {
        return new JAXBElement<TuotteenSkeemaReqBody>(_HaeTuotteenSkeema_QNAME, TuotteenSkeemaReqBody.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link TuotteenSkeemaResBody }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link TuotteenSkeemaResBody }{@code >}
     */
    @XmlElementDecl(namespace = "http://xml.vrk.fi/ws/vtj/vtjkysely/1", name = "HaeTuotteenSkeemaResponse")
    public JAXBElement<TuotteenSkeemaResBody> createHaeTuotteenSkeemaResponse(TuotteenSkeemaResBody value) {
        return new JAXBElement<TuotteenSkeemaResBody>(_HaeTuotteenSkeemaResponse_QNAME, TuotteenSkeemaResBody.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    @XmlElementDecl(namespace = "http://xml.vrk.fi/ws/vtj/vtjkysely/1", name = "faultCode", scope = HenkiloTunnusKyselyResType.class)
    public JAXBElement<String> createHenkiloTunnusKyselyResTypeFaultCode(String value) {
        return new JAXBElement<String>(_HenkiloTunnusKyselyResTypeFaultCode_QNAME, String.class, HenkiloTunnusKyselyResType.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    @XmlElementDecl(namespace = "http://xml.vrk.fi/ws/vtj/vtjkysely/1", name = "faultString", scope = HenkiloTunnusKyselyResType.class)
    public JAXBElement<String> createHenkiloTunnusKyselyResTypeFaultString(String value) {
        return new JAXBElement<String>(_HenkiloTunnusKyselyResTypeFaultString_QNAME, String.class, HenkiloTunnusKyselyResType.class, value);
    }

}
