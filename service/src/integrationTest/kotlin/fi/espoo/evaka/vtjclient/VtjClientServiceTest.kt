// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.vtjclient

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.identity.ExternalIdentifier.SSN
import fi.espoo.evaka.lookup
import fi.espoo.evaka.pis.Employee
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.vtjclient.dto.NativeLanguage
import fi.espoo.evaka.vtjclient.dto.PersonAddress
import fi.espoo.evaka.vtjclient.dto.RestrictedDetails
import fi.espoo.evaka.vtjclient.dto.VtjPerson
import fi.espoo.evaka.vtjclient.mapper.VtjHenkiloMapper
import fi.espoo.evaka.vtjclient.service.vtjclient.IVtjClientService
import fi.espoo.evaka.vtjclient.service.vtjclient.IVtjClientService.RequestType
import fi.espoo.evaka.vtjclient.service.vtjclient.IVtjClientService.RequestType.ASUKASMAARA
import fi.espoo.evaka.vtjclient.service.vtjclient.IVtjClientService.RequestType.HUOLLETTAVA_HUOLTAJAT
import fi.espoo.evaka.vtjclient.service.vtjclient.IVtjClientService.RequestType.HUOLTAJA_HUOLLETTAVA
import fi.espoo.evaka.vtjclient.service.vtjclient.IVtjClientService.RequestType.PERUSSANOMA3
import fi.espoo.evaka.vtjclient.service.vtjclient.IVtjClientService.VTJQuery
import fi.espoo.evaka.vtjclient.soap.VTJHenkiloVastaussanoma.Henkilo
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource
import org.springframework.ws.client.core.WebServiceTemplate
import org.springframework.ws.test.client.MockWebServiceServer
import org.springframework.ws.test.client.RequestMatcher
import org.springframework.ws.test.client.RequestMatchers.validPayload
import org.springframework.ws.test.client.RequestMatchers.xpath
import org.springframework.ws.test.client.ResponseCreators.withSoapEnvelope
import org.springframework.xml.transform.StringSource

val NIL_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000000")

class VtjClientServiceTest : FullApplicationTest(resetDbBeforeEach = false) {
    @Autowired lateinit var vtjClientService: IVtjClientService

    @Autowired lateinit var mapper: VtjHenkiloMapper

    private val schemaResource: Resource = ClassPathResource("wsdl/query.xsd")

    // If you want to run these tests against the actual VTJ, change the property
    // fi.espoo.voltti.vtj.test.use_actual_vtj
    // from application-integration-test.properties to true, and make sure there is an inbound port
    // 443 in lipa-test ec2 instance
    // To run against the queries successfully against VTJ, you need to:
    // 1. Configure the test VTJ user/pass from the SSM Parameter Store to
    // application-integration-test.properties
    //    The parameter names in the store are /test/evaka-vtj-client/username and
    // /test/evaka-vtj-client/password
    //    the properties to set are fi.espoo.voltti.vtj.client.username/password
    // 2. Set fi.espoo.voltti.vtj.xroad.trustStore.password to value from
    // /test/evaka-srv/xroad_truststore_password
    //    and replace resources/xroad/trustStore.jsk with S3/
    // evaka-deployment-test/evaka-srv/trustStore.jks
    // 3. Set fi.espoo.voltti.vtj.xroad.address to point at the local xroad connectivity server

    @Autowired protected lateinit var wsTemplate: WebServiceTemplate

    var mockServer: MockWebServiceServer? = null

    @BeforeEach
    fun beforeEach() {
        if (
            !env.lookup<Boolean>(
                "evaka.integration.vtj.test.use_actual_vtj",
                "fi.espoo.voltti.vtj.test.use_actual_vtj"
            )
        ) {
            mockServer = MockWebServiceServer.createServer(wsTemplate)
        }
    }

    @Test
    fun `VTJ should return results for perussanoma 3`() {
        mockServer
            ?.expect(validPayload(schemaResource))
            ?.andExpect(vtjRequestType(PERUSSANOMA3))
            ?.andRespond(withSoapEnvelope(perussanoma3Response))

        val query =
            VTJQuery(
                requestingUserId = requestingUser.id.raw,
                type = PERUSSANOMA3,
                ssn = "020501A999T"
            )
        val response = vtjClientService.query(query)

        val result = mapper.mapToVtjPerson(response!!)

        with(result) {
            assertThat(firstNames).isEqualTo("Albert Kristian")
            assertThat(lastName).isEqualTo("Jäppinen")
            assertThat(dependants).size().isEqualTo(0)
            with(address!!) {
                assertThat(streetAddress).isEqualTo("Kauppa Puistikko 6 B 23")
                assertThat(postOffice).isEqualTo("VAASA")
                assertThat(postalCode).isEqualTo("65100")
                assertThat(streetAddressSe).isEqualTo("Handels Esplanaden 6 B 23")
                assertThat(postOfficeSe).isEqualTo("VASA")
            }
            assertThat(residenceCode).isEqualTo("90000009871B023 ")
        }
        mockServer?.verify()
    }

    @Test
    fun `VTJ should return results for dependants`() {
        mockServer
            ?.expect(validPayload(schemaResource))
            ?.andExpect(vtjRequestType(HUOLTAJA_HUOLLETTAVA))
            ?.andRespond(withSoapEnvelope(huoltajaHuollettavatResponse))

        val query =
            VTJQuery(
                requestingUserId = requestingUser.id.raw,
                type = HUOLTAJA_HUOLLETTAVA,
                ssn = "020190-9521"
            )
        val response = vtjClientService.query(query)

        val result = mapper.mapToVtjPerson(response!!)

        with(result) {
            assertThat(firstNames).isEqualTo("Anna Alexandra")
            assertThat(lastName).isEqualTo("Popov")

            assertThat(result).isEqualTo(popovsWithBasicChildData)
        }
        mockServer?.verify()
    }

    @Test
    fun `VTJ should return results for caretakers`() {
        mockServer
            ?.expect(validPayload(schemaResource))
            ?.andExpect(vtjRequestType(HUOLLETTAVA_HUOLTAJAT))
            ?.andRespond(withSoapEnvelope(huollettavaHuoltajaResponse))
        val query =
            VTJQuery(
                requestingUserId = requestingUser.id.raw,
                type = HUOLLETTAVA_HUOLTAJAT,
                ssn = "311211A9527"
            )
        val response = vtjClientService.query(query)

        val result = mapper.mapToVtjPerson(response!!)

        with(result) {
            assertThat(firstNames).isEqualTo("Minja Zavutina")
            assertThat(lastName).isEqualTo("Popov")
            assertThat(socialSecurityNumber).isEqualTo("311211A9527")
            assertThat(guardians.any { it.socialSecurityNumber.equals("010181-9533") })
            assertThat(guardians.any { it.socialSecurityNumber.equals("020190-9521") })
        }
        mockServer?.verify()
    }

    @Test
    fun `VTJ should return results for resident count`() {
        mockServer
            ?.expect(validPayload(schemaResource))
            ?.andExpect(vtjRequestType(ASUKASMAARA))
            ?.andRespond(withSoapEnvelope(asukasMaaraResponse))
        val expectedSSN = "020501A999T"
        val expectedUnderageSSNs = listOf("090702A9996")

        val query =
            VTJQuery(
                requestingUserId = requestingUser.id.raw,
                type = ASUKASMAARA,
                ssn = expectedSSN
            )
        val response = vtjClientService.query(query)

        val result = ResidentCountExample.residentCountFromHenkilo(response!!)

        with(result) {
            assertThat(ssn.ssn).isEqualTo(expectedSSN)
            assertThat(residentCount).isEqualTo(4)
            assertThat(underageResidentCount).isEqualTo(expectedUnderageSSNs.size)
            assertThat(underageResidentSSns.map(SSN::ssn))
                .containsExactlyInAnyOrder(*expectedUnderageSSNs.toTypedArray())
        }
        mockServer?.verify()
    }

    private val requestingUser =
        Employee(
            id = EmployeeId(NIL_ID),
            preferredFirstName = null,
            firstName = "Integration",
            lastName = "Test",
            email = "integration-test@example.org",
            externalId = null,
            created = HelsinkiDateTime.now(),
            updated = null,
            temporaryInUnitId = null,
            active = true
        )

    private fun vtjRequestType(requestType: RequestType): RequestMatcher =
        xpath("//ns2:HenkilonTunnusKysely/ns2:request/ns2:SoSoNimi/text()", queryNamespaces)
            .evaluatesTo(requestType.queryName)

    private val queryNamespaces = mapOf("ns2" to "http://xml.vrk.fi/ws/vtj/vtjkysely/1")
}

private val popovsWithBasicChildData: VtjPerson by lazy {
    val address =
        PersonAddress(
            streetAddress = "Kurteninkatu 13 H 45",
            postalCode = "65100",
            postOffice = "VAASA",
            streetAddressSe = "Kurtensgatan 13 H 45",
            postOfficeSe = "VASA"
        )

    val kakTy =
        VtjPerson(
            firstNames = "Kak Ty",
            lastName = "Popov",
            socialSecurityNumber = "011014A9510",
            restrictedDetails = null
        )

    val minja = kakTy.copy(firstNames = "Minja Zavutina", socialSecurityNumber = "311211A9527")

    val mir = kakTy.copy(firstNames = "Mir", socialSecurityNumber = "010110A951P")

    val petrus = kakTy.copy(firstNames = "Petrus", socialSecurityNumber = "010108A9516")

    val pan = kakTy.copy(firstNames = "Pan Olov", socialSecurityNumber = "010506A951W")

    kakTy.copy(
        firstNames = "Anna Alexandra",
        socialSecurityNumber = "020190-9521",
        nativeLanguage = NativeLanguage("viro, eesti", "et"),
        address = address,
        residenceCode = "90000130991H045 ",
        restrictedDetails = RestrictedDetails(false),
        nationalities = emptyList(),
        dependants = listOf(kakTy, minja, mir, petrus, pan)
    )
}

// an example model for resident count. Decide actual model/useage later based on confirmed
// requirements
class ResidentCountExample(
    val ssn: SSN,
    val residentCount: Int,
    val underageResidentCount: Int,
    val underageResidentSSns: List<SSN>
) {
    companion object Factory {
        fun residentCountFromHenkilo(henkilo: Henkilo) =
            ResidentCountExample(
                ssn = SSN.getInstance(henkilo.henkilotunnus.value),
                residentCount = henkilo.asukasLkm.toInt(),
                underageResidentCount = henkilo.asukkaatAlle18V.asukasLkm.toInt(),
                underageResidentSSns =
                    henkilo.asukasAlle18V.map { SSN.getInstance(it.henkilotunnus) }
            )
    }
}

// Below are actual VTJ responses to the queries above, if they are done against the actual VTJ. The
// username has been blanked out
@Language("xml")
private val perussanoma3Response =
    """
<?xml version="1.0" encoding="UTF-8"?>
<s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/">
    <s:Header>
        <h:client xmlns="http://x-road.eu/xsd/xroad.xsd" xmlns:a="http://x-road.eu/xsd/identifiers"
                  xmlns:h="http://x-road.eu/xsd/xroad.xsd" xmlns:xsd="http://www.w3.org/2001/XMLSchema"
                  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" a:objectType="SUBSYSTEM">
            <a:xRoadInstance>FI-TEST</a:xRoadInstance>
            <a:memberClass>MUN</a:memberClass>
            <a:memberCode>0101263-6</a:memberCode>
            <a:subsystemCode>evaka-test</a:subsystemCode>
        </h:client>
        <h:id xmlns="http://x-road.eu/xsd/xroad.xsd" xmlns:h="http://x-road.eu/xsd/xroad.xsd">41fc10922728511e</h:id>
        <h:requestHash xmlns="http://x-road.eu/xsd/xroad.xsd" xmlns:h="http://x-road.eu/xsd/xroad.xsd"
                       algorithmId="http://www.w3.org/2001/04/xmlenc#sha512">
            VK9YTnb2003Ykf8FcKqRlF608RKn85CfR2tp/SAMrUdEmEForA/1MBQ3dRCLIfjehm4YqCGCp21CmHqJ4zm4JA==
        </h:requestHash>
        <h:issue xmlns="http://x-road.eu/xsd/xroad.xsd" xmlns:h="http://x-road.eu/xsd/xroad.xsd"/>
        <h:protocolVersion xmlns="http://x-road.eu/xsd/xroad.xsd" xmlns:h="http://x-road.eu/xsd/xroad.xsd">4.0
        </h:protocolVersion>
        <h:service xmlns="http://x-road.eu/xsd/xroad.xsd" xmlns:a="http://x-road.eu/xsd/identifiers"
                   xmlns:h="http://x-road.eu/xsd/xroad.xsd" a:objectType="SERVICE">
            <a:xRoadInstance>FI-TEST</a:xRoadInstance>
            <a:memberClass>GOV</a:memberClass>
            <a:memberCode>0245437-2</a:memberCode>
            <a:subsystemCode>VTJkysely</a:subsystemCode>
            <a:serviceCode>HenkilonTunnusKysely</a:serviceCode>
            <a:serviceVersion>v1</a:serviceVersion>
        </h:service>
        <h:userId xmlns="http://x-road.eu/xsd/xroad.xsd" xmlns:h="http://x-road.eu/xsd/xroad.xsd">
            00000000-0000-0000-0000-000000000000
        </h:userId>
    </s:Header>
    <s:Body xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
        <HenkilonTunnusKyselyResponse xmlns="http://xml.vrk.fi/ws/vtj/vtjkysely/1">
            <request>
                <SoSoNimi>Perussanoma 3</SoSoNimi>
                <Kayttajatunnus>*****</Kayttajatunnus>
                <Salasana/>
                <Loppukayttaja>azure-id: 00000000-0000-0000-0000-000000000000</Loppukayttaja>
                <Henkilotunnus>020501A999T</Henkilotunnus>
            </request>
            <response>
                <VTJHenkiloVastaussanoma xmlns="http://xml.vrk.fi/schema/vtjkysely"
                                         xmlns:vtj="http://xml.vrk.fi/schema/vtj/henkilotiedot/1"
                                         sanomatunnus="PERUSSANOMA 3" tietojenPoimintaaika="20190620174212" versio="1.0"
                                         xsi:schemaLocation="http://xml.vrk.fi/schema/vtjkysely PERUSSANOMA 3.xsd">
                    <Asiakasinfo>
                        <InfoS>20.06.2019 17:42</InfoS>
                        <InfoR>20.06.2019 17:42</InfoR>
                        <InfoE>20.06.2019 17:42</InfoE>
                    </Asiakasinfo>
                    <Paluukoodi koodi="0000">Haku onnistui</Paluukoodi>
                    <Hakuperusteet>
                        <Henkilotunnus hakuperustePaluukoodi="1" hakuperusteTekstiE="Found"
                                       hakuperusteTekstiR="Hittades" hakuperusteTekstiS="Löytyi">020501A999T
                        </Henkilotunnus>
                    </Hakuperusteet>
                    <Henkilo>
                        <Henkilotunnus voimassaolokoodi="1">020501A999T</Henkilotunnus>
                        <NykyinenSukunimi>
                            <Sukunimi>Jäppinen</Sukunimi>
                        </NykyinenSukunimi>
                        <NykyisetEtunimet>
                            <Etunimet>Albert Kristian</Etunimet>
                        </NykyisetEtunimet>
                        <EntinenNimi>
                            <Nimi/>
                            <Nimilajikoodi/>
                            <Alkupvm/>
                            <Loppupvm/>
                            <Info8S/>
                            <Info8R/>
                        </EntinenNimi>
                        <VakinainenKotimainenLahiosoite>
                            <LahiosoiteS>Kauppa Puistikko 6 B 23</LahiosoiteS>
                            <LahiosoiteR>Handels Esplanaden 6 B 23</LahiosoiteR>
                            <Postinumero>65100</Postinumero>
                            <PostitoimipaikkaS>VAASA</PostitoimipaikkaS>
                            <PostitoimipaikkaR>VASA</PostitoimipaikkaR>
                            <AsuminenAlkupvm>20031125</AsuminenAlkupvm>
                            <AsuminenLoppupvm/>
                        </VakinainenKotimainenLahiosoite>
                        <VakinainenAsuinpaikka>
                            <Asuinpaikantunnus>90000009871B023 </Asuinpaikantunnus>
                        </VakinainenAsuinpaikka>
                        <VakinainenUlkomainenLahiosoite>
                            <UlkomainenLahiosoite/>
                            <UlkomainenPaikkakuntaJaValtioS/>
                            <UlkomainenPaikkakuntaJaValtioR/>
                            <UlkomainenPaikkakuntaJaValtioSelvakielinen/>
                            <Valtiokoodi3/>
                            <AsuminenAlkupvm/>
                            <AsuminenLoppupvm/>
                        </VakinainenUlkomainenLahiosoite>
                        <TilapainenKotimainenLahiosoite>
                            <LahiosoiteS/>
                            <LahiosoiteR/>
                            <Postinumero/>
                            <PostitoimipaikkaS/>
                            <PostitoimipaikkaR/>
                            <AsuminenAlkupvm/>
                            <AsuminenLoppupvm/>
                        </TilapainenKotimainenLahiosoite>
                        <TilapainenUlkomainenLahiosoite>
                            <UlkomainenLahiosoite/>
                            <UlkomainenPaikkakuntaJaValtioS/>
                            <UlkomainenPaikkakuntaJaValtioR/>
                            <UlkomainenPaikkakuntaJaValtioSelvakielinen/>
                            <Valtiokoodi3/>
                            <AsuminenAlkupvm/>
                            <AsuminenLoppupvm/>
                        </TilapainenUlkomainenLahiosoite>
                        <KotimainenPostiosoite>
                            <PostiosoiteS/>
                            <PostiosoiteR/>
                            <Postinumero/>
                            <PostitoimipaikkaS/>
                            <PostitoimipaikkaR/>
                            <PostiosoiteAlkupvm/>
                            <PostiosoiteLoppupvm/>
                        </KotimainenPostiosoite>
                        <UlkomainenPostiosoite>
                            <UlkomainenLahiosoite/>
                            <UlkomainenPaikkakunta/>
                            <Valtiokoodi3/>
                            <ValtioS/>
                            <ValtioR/>
                            <ValtioSelvakielinen/>
                            <PostiosoiteAlkupvm/>
                            <PostiosoiteLoppupvm/>
                        </UlkomainenPostiosoite>
                        <Kotikunta>
                            <Kuntanumero>905</Kuntanumero>
                            <KuntaS>Vaasa</KuntaS>
                            <KuntaR>Vasa</KuntaR>
                            <KuntasuhdeAlkupvm>20031125</KuntasuhdeAlkupvm>
                        </Kotikunta>
                        <Kuolintiedot>
                            <Kuollut/>
                            <Kuolinpvm/>
                        </Kuolintiedot>
                        <Kuolleeksijulistamistiedot>
                            <Kuolleeksijulistamispvm/>
                        </Kuolleeksijulistamistiedot>
                        <Aidinkieli>
                            <Kielikoodi>fi</Kielikoodi>
                            <KieliS>suomi</KieliS>
                            <KieliR>finska</KieliR>
                            <KieliSelvakielinen/>
                        </Aidinkieli>
                        <Turvakielto>
                            <TurvakieltoTieto/>
                            <TurvakieltoPaattymispvm/>
                        </Turvakielto>
                        <Sukupuoli>
                            <Sukupuolikoodi>1</Sukupuolikoodi>
                            <SukupuoliS>Mies</SukupuoliS>
                            <SukupuoliR>Man</SukupuoliR>
                        </Sukupuoli>
                        <Kansalaisuus>
                            <Kansalaisuuskoodi3>246</Kansalaisuuskoodi3>
                            <KansalaisuusS>Suomi</KansalaisuusS>
                            <KansalaisuusR>Finland</KansalaisuusR>
                            <KansalaisuusSelvakielinen/>
                            <Saamispvm/>
                        </Kansalaisuus>
                        <Edunvalvonta>
                            <Alkupvm/>
                            <Paattymispvm/>
                            <Rajoituskoodi/>
                            <RajoitustekstiS/>
                            <RajoitustekstiR/>
                            <Tehtavienjakokoodi/>
                            <TehtavienjakoS/>
                            <TehtavienjakoR/>
                            <HenkiloEdunvalvoja>
                                <Henkilotunnus/>
                                <Syntymaaika/>
                                <NykyinenSukunimi>
                                    <Sukunimi/>
                                </NykyinenSukunimi>
                                <NykyisetEtunimet>
                                    <Etunimet/>
                                </NykyisetEtunimet>
                                <TehtavaAlkupvm/>
                                <TehtavaLoppupvm/>
                            </HenkiloEdunvalvoja>
                            <YritysJaYhteisoEdunvalvoja>
                                <Ytunnus/>
                                <Nimi/>
                                <TehtavaLoppupvm/>
                                <TehtavaAlkupvm/>
                            </YritysJaYhteisoEdunvalvoja>
                            <OikeusaputoimistoEdunvalvoja>
                                <Viranomaisnumero/>
                                <ViranomainenS/>
                                <ViranomainenR/>
                                <TehtavaAlkupvm/>
                                <TehtavaLoppupvm/>
                            </OikeusaputoimistoEdunvalvoja>
                        </Edunvalvonta>
                        <Edunvalvontavaltuutus>
                            <Alkupvm/>
                            <Paattymispvm/>
                            <Tehtavienjakokoodi/>
                            <TehtavienjakoS/>
                            <TehtavienjakoR/>
                            <HenkiloEdunvalvontavaltuutettu>
                                <Henkilotunnus/>
                                <Syntymaaika/>
                                <NykyinenSukunimi>
                                    <Sukunimi/>
                                </NykyinenSukunimi>
                                <NykyisetEtunimet>
                                    <Etunimet/>
                                </NykyisetEtunimet>
                                <ValtuutusAlkupvm/>
                                <ValtuutusLoppupvm/>
                            </HenkiloEdunvalvontavaltuutettu>
                        </Edunvalvontavaltuutus>
                    </Henkilo>
                </VTJHenkiloVastaussanoma>
            </response>
        </HenkilonTunnusKyselyResponse>
    </s:Body>
</s:Envelope>
    """.trimIndent()
        .let(::StringSource)

@Language("xml")
private val huoltajaHuollettavatResponse =
    """
<?xml version="1.0" encoding="UTF-8"?>
<s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/">
    <s:Header>
        <h:client xmlns="http://x-road.eu/xsd/xroad.xsd" xmlns:a="http://x-road.eu/xsd/identifiers"
                  xmlns:h="http://x-road.eu/xsd/xroad.xsd" xmlns:xsd="http://www.w3.org/2001/XMLSchema"
                  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" a:objectType="SUBSYSTEM">
            <a:xRoadInstance>FI-TEST</a:xRoadInstance>
            <a:memberClass>MUN</a:memberClass>
            <a:memberCode>0101263-6</a:memberCode>
            <a:subsystemCode>evaka-test</a:subsystemCode>
        </h:client>
        <h:id xmlns="http://x-road.eu/xsd/xroad.xsd" xmlns:h="http://x-road.eu/xsd/xroad.xsd">53e03e104a79b165</h:id>
        <h:requestHash xmlns="http://x-road.eu/xsd/xroad.xsd" xmlns:h="http://x-road.eu/xsd/xroad.xsd"
                       algorithmId="http://www.w3.org/2001/04/xmlenc#sha512">
            7w6e76VWTh0ZKOX4+vTvaIkC9noX2uSrxHi2jky5/JZSkpv1K4ojOs78Ri/5EWt/JgLXeOHEbeRYHLH70VWQ1w==
        </h:requestHash>
        <h:issue xmlns="http://x-road.eu/xsd/xroad.xsd" xmlns:h="http://x-road.eu/xsd/xroad.xsd"/>
        <h:protocolVersion xmlns="http://x-road.eu/xsd/xroad.xsd" xmlns:h="http://x-road.eu/xsd/xroad.xsd">4.0
        </h:protocolVersion>
        <h:service xmlns="http://x-road.eu/xsd/xroad.xsd" xmlns:a="http://x-road.eu/xsd/identifiers"
                   xmlns:h="http://x-road.eu/xsd/xroad.xsd" a:objectType="SERVICE">
            <a:xRoadInstance>FI-TEST</a:xRoadInstance>
            <a:memberClass>GOV</a:memberClass>
            <a:memberCode>0245437-2</a:memberCode>
            <a:subsystemCode>VTJkysely</a:subsystemCode>
            <a:serviceCode>HenkilonTunnusKysely</a:serviceCode>
            <a:serviceVersion>v1</a:serviceVersion>
        </h:service>
        <h:userId xmlns="http://x-road.eu/xsd/xroad.xsd" xmlns:h="http://x-road.eu/xsd/xroad.xsd">
            00000000-0000-0000-0000-000000000000
        </h:userId>
    </s:Header>
    <s:Body xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
        <HenkilonTunnusKyselyResponse xmlns="http://xml.vrk.fi/ws/vtj/vtjkysely/1">
            <request>
                <SoSoNimi>Huoltaja-Huollettavat</SoSoNimi>
                <Kayttajatunnus>*****</Kayttajatunnus>
                <Salasana/>
                <Loppukayttaja>azure-id: 00000000-0000-0000-0000-000000000000</Loppukayttaja>
                <Henkilotunnus>020190-9521</Henkilotunnus>
            </request>
            <response>
                <VTJHenkiloVastaussanoma xmlns="http://xml.vrk.fi/schema/vtjkysely"
                                         xmlns:vtj="http://xml.vrk.fi/schema/vtj/henkilotiedot/1"
                                         sanomatunnus="HUOLTAJA-HUOLLETTAVAT" tietojenPoimintaaika="20190625115255"
                                         versio="1.0"
                                         xsi:schemaLocation="http://xml.vrk.fi/schema/vtjkysely HUOLTAJA-HUOLLETTAVAT.xsd">
                    <Asiakasinfo>
                        <InfoS>25.06.2019 11:52</InfoS>
                        <InfoR>25.06.2019 11:52</InfoR>
                        <InfoE>25.06.2019 11:52</InfoE>
                    </Asiakasinfo>
                    <Paluukoodi koodi="0000">Haku onnistui</Paluukoodi>
                    <Hakuperusteet>
                        <Henkilotunnus hakuperustePaluukoodi="1" hakuperusteTekstiE="Found"
                                       hakuperusteTekstiR="Hittades" hakuperusteTekstiS="Löytyi">020190-9521
                        </Henkilotunnus>
                    </Hakuperusteet>
                    <Henkilo>
                        <Henkilotunnus voimassaolokoodi="1">020190-9521</Henkilotunnus>
                        <NykyinenSukunimi>
                            <Sukunimi>Popov</Sukunimi>
                        </NykyinenSukunimi>
                        <NykyisetEtunimet>
                            <Etunimet>Anna Alexandra</Etunimet>
                        </NykyisetEtunimet>
                        <VakinainenKotimainenLahiosoite>
                            <LahiosoiteS>Kurteninkatu 13 H 45</LahiosoiteS>
                            <LahiosoiteR>Kurtensgatan 13 H 45</LahiosoiteR>
                            <Postinumero>65100</Postinumero>
                            <PostitoimipaikkaS>VAASA</PostitoimipaikkaS>
                            <PostitoimipaikkaR>VASA</PostitoimipaikkaR>
                            <AsuminenAlkupvm>20150601</AsuminenAlkupvm>
                            <AsuminenLoppupvm/>
                        </VakinainenKotimainenLahiosoite>
                        <VakinainenUlkomainenLahiosoite>
                            <UlkomainenLahiosoite/>
                            <UlkomainenPaikkakuntaJaValtioS/>
                            <UlkomainenPaikkakuntaJaValtioR/>
                            <UlkomainenPaikkakuntaJaValtioSelvakielinen/>
                            <Valtiokoodi3/>
                            <AsuminenAlkupvm/>
                            <AsuminenLoppupvm/>
                        </VakinainenUlkomainenLahiosoite>
                        <VakinainenAsuinpaikka>
                            <Asuinpaikantunnus>90000130991H045 </Asuinpaikantunnus>
                        </VakinainenAsuinpaikka>
                        <TilapainenKotimainenLahiosoite>
                            <LahiosoiteS/>
                            <LahiosoiteR/>
                            <Postinumero/>
                            <PostitoimipaikkaS/>
                            <PostitoimipaikkaR/>
                            <AsuminenAlkupvm/>
                            <AsuminenLoppupvm/>
                        </TilapainenKotimainenLahiosoite>
                        <TilapainenUlkomainenLahiosoite>
                            <UlkomainenLahiosoite/>
                            <UlkomainenPaikkakuntaJaValtioS/>
                            <UlkomainenPaikkakuntaJaValtioR/>
                            <UlkomainenPaikkakuntaJaValtioSelvakielinen/>
                            <Valtiokoodi3/>
                            <AsuminenAlkupvm/>
                            <AsuminenLoppupvm/>
                        </TilapainenUlkomainenLahiosoite>
                        <KotimainenPostiosoite>
                            <PostiosoiteS/>
                            <PostiosoiteR/>
                            <Postinumero/>
                            <PostitoimipaikkaS/>
                            <PostitoimipaikkaR/>
                            <PostiosoiteAlkupvm/>
                            <PostiosoiteLoppupvm/>
                        </KotimainenPostiosoite>
                        <UlkomainenPostiosoite>
                            <UlkomainenLahiosoite/>
                            <UlkomainenPaikkakunta/>
                            <Valtiokoodi3/>
                            <ValtioS/>
                            <ValtioR/>
                            <ValtioSelvakielinen/>
                            <PostiosoiteAlkupvm/>
                            <PostiosoiteLoppupvm/>
                        </UlkomainenPostiosoite>
                        <Kotikunta>
                            <Kuntanumero>905</Kuntanumero>
                            <KuntaS>Vaasa</KuntaS>
                            <KuntaR>Vasa</KuntaR>
                            <KuntasuhdeAlkupvm>20150601</KuntasuhdeAlkupvm>
                        </Kotikunta>
                        <Kuolintiedot>
                            <Kuollut/>
                            <Kuolinpvm/>
                        </Kuolintiedot>
                        <Kuolleeksijulistamistiedot>
                            <Kuolleeksijulistamispvm/>
                        </Kuolleeksijulistamistiedot>
                        <Aidinkieli>
                            <Kielikoodi>et</Kielikoodi>
                            <KieliS>viro, eesti</KieliS>
                            <KieliR>estniska</KieliR>
                            <KieliSelvakielinen/>
                        </Aidinkieli>
                        <Turvakielto>
                            <TurvakieltoTieto/>
                            <TurvakieltoPaattymispvm/>
                        </Turvakielto>
                        <Sukupuoli>
                            <Sukupuolikoodi>2</Sukupuolikoodi>
                            <SukupuoliS>Nainen</SukupuoliS>
                            <SukupuoliR>Kvinna</SukupuoliR>
                        </Sukupuoli>
                        <Huollettava>
                            <Henkilotunnus>011014A9510</Henkilotunnus>
                            <Syntymaaika/>
                            <NykyinenSukunimi>
                                <Sukunimi>Popov</Sukunimi>
                            </NykyinenSukunimi>
                            <NykyisetEtunimet>
                                <Etunimet>Kak Ty</Etunimet>
                            </NykyisetEtunimet>
                        </Huollettava>
                        <Huollettava>
                            <Henkilotunnus>311211A9527</Henkilotunnus>
                            <Syntymaaika/>
                            <NykyinenSukunimi>
                                <Sukunimi>Popov</Sukunimi>
                            </NykyinenSukunimi>
                            <NykyisetEtunimet>
                                <Etunimet>Minja Zavutina</Etunimet>
                            </NykyisetEtunimet>
                        </Huollettava>
                        <Huollettava>
                            <Henkilotunnus>010110A951P</Henkilotunnus>
                            <Syntymaaika/>
                            <NykyinenSukunimi>
                                <Sukunimi>Popov</Sukunimi>
                            </NykyinenSukunimi>
                            <NykyisetEtunimet>
                                <Etunimet>Mir</Etunimet>
                            </NykyisetEtunimet>
                        </Huollettava>
                        <Huollettava>
                            <Henkilotunnus>010108A9516</Henkilotunnus>
                            <Syntymaaika/>
                            <NykyinenSukunimi>
                                <Sukunimi>Popov</Sukunimi>
                            </NykyinenSukunimi>
                            <NykyisetEtunimet>
                                <Etunimet>Petrus</Etunimet>
                            </NykyisetEtunimet>
                        </Huollettava>
                        <Huollettava>
                            <Henkilotunnus>010506A951W</Henkilotunnus>
                            <Syntymaaika/>
                            <NykyinenSukunimi>
                                <Sukunimi>Popov</Sukunimi>
                            </NykyinenSukunimi>
                            <NykyisetEtunimet>
                                <Etunimet>Pan Olov</Etunimet>
                            </NykyisetEtunimet>
                        </Huollettava>
                    </Henkilo>
                </VTJHenkiloVastaussanoma>
            </response>
        </HenkilonTunnusKyselyResponse>
    </s:Body>
</s:Envelope>
    """.trimIndent()
        .let(::StringSource)

@Language("xml")
private val huollettavaHuoltajaResponse =
    """
<?xml version="1.0" encoding="UTF-8"?>
<s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/">
    <s:Header>
        <h:client xmlns="http://x-road.eu/xsd/xroad.xsd" xmlns:a="http://x-road.eu/xsd/identifiers"
                  xmlns:h="http://x-road.eu/xsd/xroad.xsd" xmlns:xsd="http://www.w3.org/2001/XMLSchema"
                  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" a:objectType="SUBSYSTEM">
            <a:xRoadInstance>FI-TEST</a:xRoadInstance>
            <a:memberClass>MUN</a:memberClass>
            <a:memberCode>0101263-6</a:memberCode>
            <a:subsystemCode>evaka-test</a:subsystemCode>
        </h:client>
        <h:id xmlns="http://x-road.eu/xsd/xroad.xsd" xmlns:h="http://x-road.eu/xsd/xroad.xsd">838b02eccdc77bcc</h:id>
        <h:requestHash xmlns="http://x-road.eu/xsd/xroad.xsd" xmlns:h="http://x-road.eu/xsd/xroad.xsd"
                       algorithmId="http://www.w3.org/2001/04/xmlenc#sha512">
            JmszhwMnjlSvTjaufkksI7YbHcnyI1Oc4YX5R90hsdQMv/d1TeTg28VPOFcuQ7Qscbz3fRvJQzugQQvJfADrHw==
        </h:requestHash>
        <h:issue xmlns="http://x-road.eu/xsd/xroad.xsd" xmlns:h="http://x-road.eu/xsd/xroad.xsd"/>
        <h:protocolVersion xmlns="http://x-road.eu/xsd/xroad.xsd" xmlns:h="http://x-road.eu/xsd/xroad.xsd">4.0
        </h:protocolVersion>
        <h:service xmlns="http://x-road.eu/xsd/xroad.xsd" xmlns:a="http://x-road.eu/xsd/identifiers"
                   xmlns:h="http://x-road.eu/xsd/xroad.xsd" a:objectType="SERVICE">
            <a:xRoadInstance>FI-TEST</a:xRoadInstance>
            <a:memberClass>GOV</a:memberClass>
            <a:memberCode>0245437-2</a:memberCode>
            <a:subsystemCode>VTJkysely</a:subsystemCode>
            <a:serviceCode>HenkilonTunnusKysely</a:serviceCode>
            <a:serviceVersion>v1</a:serviceVersion>
        </h:service>
        <h:userId xmlns="http://x-road.eu/xsd/xroad.xsd" xmlns:h="http://x-road.eu/xsd/xroad.xsd">
            00000000-0000-0000-0000-000000000000
        </h:userId>
    </s:Header>
    <s:Body xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
        <HenkilonTunnusKyselyResponse xmlns="http://xml.vrk.fi/ws/vtj/vtjkysely/1">
            <request>
                <SoSoNimi>Huollettava-Huoltajat</SoSoNimi>
                <Kayttajatunnus>T09w0111</Kayttajatunnus>
                <Salasana/>
                <Loppukayttaja>voltti-id: 00000000-0000-0000-0000-000000000000</Loppukayttaja>
                <Henkilotunnus>311211A9527</Henkilotunnus>
            </request>
            <response>
                <VTJHenkiloVastaussanoma xmlns="http://xml.vrk.fi/schema/vtjkysely"
                                         xmlns:vtj="http://xml.vrk.fi/schema/vtj/henkilotiedot/1"
                                         sanomatunnus="HUOLLETTAVA-HUOLTAJAT" tietojenPoimintaaika="20200424124518"
                                         versio="1.0"
                                         xsi:schemaLocation="http://xml.vrk.fi/schema/vtjkysely HUOLLETTAVA-HUOLTAJAT.xsd">
                    <Asiakasinfo>
                        <InfoS>24.04.2020 12:45</InfoS>
                        <InfoR>24.04.2020 12:45</InfoR>
                        <InfoE>24.04.2020 12:45</InfoE>
                    </Asiakasinfo>
                    <Paluukoodi koodi="0000">Haku onnistui</Paluukoodi>
                    <Hakuperusteet>
                        <Henkilotunnus hakuperustePaluukoodi="1" hakuperusteTekstiE="Found"
                                       hakuperusteTekstiR="Hittades" hakuperusteTekstiS="Löytyi">311211A9527
                        </Henkilotunnus>
                    </Hakuperusteet>
                    <Henkilo>
                        <Henkilotunnus voimassaolokoodi="1">311211A9527</Henkilotunnus>
                        <NykyinenSukunimi>
                            <Sukunimi>Popov</Sukunimi>
                        </NykyinenSukunimi>
                        <NykyisetEtunimet>
                            <Etunimet>Minja Zavutina</Etunimet>
                        </NykyisetEtunimet>
                        <VakinainenKotimainenLahiosoite>
                            <LahiosoiteS>Kurteninkatu 13 H 45</LahiosoiteS>
                            <LahiosoiteR>Kurtensgatan 13 H 45</LahiosoiteR>
                            <Postinumero>65100</Postinumero>
                            <PostitoimipaikkaS>VAASA</PostitoimipaikkaS>
                            <PostitoimipaikkaR>VASA</PostitoimipaikkaR>
                            <AsuminenAlkupvm>20150601</AsuminenAlkupvm>
                            <AsuminenLoppupvm/>
                        </VakinainenKotimainenLahiosoite>
                        <VakinainenUlkomainenLahiosoite>
                            <UlkomainenLahiosoite/>
                            <UlkomainenPaikkakuntaJaValtioS/>
                            <UlkomainenPaikkakuntaJaValtioR/>
                            <UlkomainenPaikkakuntaJaValtioSelvakielinen/>
                            <Valtiokoodi3/>
                            <AsuminenAlkupvm/>
                            <AsuminenLoppupvm/>
                        </VakinainenUlkomainenLahiosoite>
                        <VakinainenAsuinpaikka>
                            <Asuinpaikantunnus>90000130991H045</Asuinpaikantunnus>
                        </VakinainenAsuinpaikka>
                        <TilapainenKotimainenLahiosoite>
                            <LahiosoiteS/>
                            <LahiosoiteR/>
                            <Postinumero/>
                            <PostitoimipaikkaS/>
                            <PostitoimipaikkaR/>
                            <AsuminenAlkupvm/>
                            <AsuminenLoppupvm/>
                        </TilapainenKotimainenLahiosoite>
                        <TilapainenUlkomainenLahiosoite>
                            <UlkomainenLahiosoite/>
                            <UlkomainenPaikkakuntaJaValtioS/>
                            <UlkomainenPaikkakuntaJaValtioR/>
                            <UlkomainenPaikkakuntaJaValtioSelvakielinen/>
                            <Valtiokoodi3/>
                            <AsuminenAlkupvm/>
                            <AsuminenLoppupvm/>
                        </TilapainenUlkomainenLahiosoite>
                        <KotimainenPostiosoite>
                            <PostiosoiteS/>
                            <PostiosoiteR/>
                            <Postinumero/>
                            <PostitoimipaikkaS/>
                            <PostitoimipaikkaR/>
                            <PostiosoiteAlkupvm/>
                            <PostiosoiteLoppupvm/>
                        </KotimainenPostiosoite>
                        <UlkomainenPostiosoite>
                            <UlkomainenLahiosoite/>
                            <UlkomainenPaikkakunta/>
                            <Valtiokoodi3/>
                            <ValtioS/>
                            <ValtioR/>
                            <ValtioSelvakielinen/>
                            <PostiosoiteAlkupvm/>
                            <PostiosoiteLoppupvm/>
                        </UlkomainenPostiosoite>
                        <Kotikunta>
                            <Kuntanumero>905</Kuntanumero>
                            <KuntaS>Vaasa</KuntaS>
                            <KuntaR>Vasa</KuntaR>
                            <KuntasuhdeAlkupvm>20150601</KuntasuhdeAlkupvm>
                        </Kotikunta>
                        <Kuolintiedot>
                            <Kuollut/>
                            <Kuolinpvm/>
                        </Kuolintiedot>
                        <Kuolleeksijulistamistiedot>
                            <Kuolleeksijulistamispvm/>
                        </Kuolleeksijulistamistiedot>
                        <Aidinkieli>
                            <Kielikoodi>fi</Kielikoodi>
                            <KieliS>suomi</KieliS>
                            <KieliR>finska</KieliR>
                            <KieliSelvakielinen/>
                        </Aidinkieli>
                        <Turvakielto>
                            <TurvakieltoTieto/>
                            <TurvakieltoPaattymispvm/>
                        </Turvakielto>
                        <Sukupuoli>
                            <Sukupuolikoodi>2</Sukupuolikoodi>
                            <SukupuoliS>Nainen</SukupuoliS>
                            <SukupuoliR>Kvinna</SukupuoliR>
                        </Sukupuoli>
                        <Huoltaja>
                            <Henkilotunnus>010181-9533</Henkilotunnus>
                            <Syntymaaika/>
                            <NykyinenSukunimi>
                                <Sukunimi>Popov</Sukunimi>
                            </NykyinenSukunimi>
                            <NykyisetEtunimet>
                                <Etunimet>Andre Olav</Etunimet>
                            </NykyisetEtunimet>
                        </Huoltaja>
                        <Huoltaja>
                            <Henkilotunnus>020190-9521</Henkilotunnus>
                            <Syntymaaika/>
                            <NykyinenSukunimi>
                                <Sukunimi>Popov</Sukunimi>
                            </NykyinenSukunimi>
                            <NykyisetEtunimet>
                                <Etunimet>Anna Alexandra</Etunimet>
                            </NykyisetEtunimet>
                        </Huoltaja>
                    </Henkilo>
                </VTJHenkiloVastaussanoma>
            </response>
        </HenkilonTunnusKyselyResponse>
    </s:Body>
</s:Envelope>    
    """.trimIndent()
        .let(::StringSource)

@Language("xml")
private val asukasMaaraResponse =
    """
<?xml version="1.0" encoding="UTF-8"?>
<s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/">
    <s:Header>
        <h:client xmlns="http://x-road.eu/xsd/xroad.xsd" xmlns:a="http://x-road.eu/xsd/identifiers"
                  xmlns:h="http://x-road.eu/xsd/xroad.xsd" xmlns:xsd="http://www.w3.org/2001/XMLSchema"
                  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" a:objectType="SUBSYSTEM">
            <a:xRoadInstance>FI-TEST</a:xRoadInstance>
            <a:memberClass>MUN</a:memberClass>
            <a:memberCode>0101263-6</a:memberCode>
            <a:subsystemCode>evaka-test</a:subsystemCode>
        </h:client>
        <h:id xmlns="http://x-road.eu/xsd/xroad.xsd" xmlns:h="http://x-road.eu/xsd/xroad.xsd">253ba59ba28de08e</h:id>
        <h:requestHash xmlns="http://x-road.eu/xsd/xroad.xsd" xmlns:h="http://x-road.eu/xsd/xroad.xsd"
                       algorithmId="http://www.w3.org/2001/04/xmlenc#sha512">
            hUoi8LkkYs1ITRMrincOfSdY+lFinIHhuUE5dLqz3/UMWM5DuGh8FuxzdAuOV/9AqDpGwrKepoxHIcibkJQcOw==
        </h:requestHash>
        <h:issue xmlns="http://x-road.eu/xsd/xroad.xsd" xmlns:h="http://x-road.eu/xsd/xroad.xsd"/>
        <h:protocolVersion xmlns="http://x-road.eu/xsd/xroad.xsd" xmlns:h="http://x-road.eu/xsd/xroad.xsd">4.0
        </h:protocolVersion>
        <h:service xmlns="http://x-road.eu/xsd/xroad.xsd" xmlns:a="http://x-road.eu/xsd/identifiers"
                   xmlns:h="http://x-road.eu/xsd/xroad.xsd" a:objectType="SERVICE">
            <a:xRoadInstance>FI-TEST</a:xRoadInstance>
            <a:memberClass>GOV</a:memberClass>
            <a:memberCode>0245437-2</a:memberCode>
            <a:subsystemCode>VTJkysely</a:subsystemCode>
            <a:serviceCode>HenkilonTunnusKysely</a:serviceCode>
            <a:serviceVersion>v1</a:serviceVersion>
        </h:service>
        <h:userId xmlns="http://x-road.eu/xsd/xroad.xsd" xmlns:h="http://x-road.eu/xsd/xroad.xsd">
            00000000-0000-0000-0000-000000000000
        </h:userId>
    </s:Header>
    <s:Body xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
        <HenkilonTunnusKyselyResponse xmlns="http://xml.vrk.fi/ws/vtj/vtjkysely/1">
            <request>
                <SoSoNimi>ASUKASLKM2</SoSoNimi>
                <Kayttajatunnus>*****</Kayttajatunnus>
                <Salasana/>
                <Loppukayttaja>azure-id: 00000000-0000-0000-0000-000000000000</Loppukayttaja>
                <Henkilotunnus>020501A999T</Henkilotunnus>
            </request>
            <response>
                <VTJHenkiloVastaussanoma xmlns="http://xml.vrk.fi/schema/vtjkysely"
                                         xmlns:vtj="http://xml.vrk.fi/schema/vtj/henkilotiedot/1"
                                         sanomatunnus="ASUKASLKM2" tietojenPoimintaaika="20190625134846" versio="1.0"
                                         xsi:schemaLocation="http://xml.vrk.fi/schema/vtjkysely ASUKASLKM2.xsd">
                    <Asiakasinfo>
                        <InfoS>25.06.2019 13:48</InfoS>
                        <InfoR>25.06.2019 13:48</InfoR>
                        <InfoE>25.06.2019 13:48</InfoE>
                    </Asiakasinfo>
                    <Paluukoodi koodi="0000">Haku onnistui</Paluukoodi>
                    <Hakuperusteet>
                        <Henkilotunnus hakuperustePaluukoodi="1" hakuperusteTekstiE="Found"
                                       hakuperusteTekstiR="Hittades" hakuperusteTekstiS="Löytyi">020501A999T
                        </Henkilotunnus>
                    </Hakuperusteet>
                    <Henkilo>
                        <Henkilotunnus voimassaolokoodi="1">020501A999T</Henkilotunnus>
                        <AsukasLkm>4</AsukasLkm>
                        <AsukkaatAlle18v>
                            <AsukasLkm>1</AsukasLkm>
                        </AsukkaatAlle18v>
                        <AsukasAlle18v>
                            <Henkilotunnus>090702A9996</Henkilotunnus>
                        </AsukasAlle18v>
                    </Henkilo>
                </VTJHenkiloVastaussanoma>
            </response>
        </HenkilonTunnusKyselyResponse>
    </s:Body>
</s:Envelope>
    """.trimIndent()
        .let(::StringSource)
