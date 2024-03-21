// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.varda.new

import com.fasterxml.jackson.databind.json.JsonMapper
import com.github.kittinunf.fuel.core.FuelManager
import fi.espoo.evaka.OphEnv
import fi.espoo.evaka.VardaEnv
import fi.espoo.evaka.pis.getDependantGuardians
import fi.espoo.evaka.pis.updateOphPersonOid
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.config.FuelManagerConfig
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.varda.integration.VardaTempTokenProvider
import java.net.URI
import java.time.LocalDate
import org.springframework.stereotype.Service

@Service
class VardaUpdateServiceNew(
    asyncJobRunner: AsyncJobRunner<AsyncJob>,
    globalFuel: FuelManager,
    mapper: JsonMapper,
    ophEnv: OphEnv,
    vardaEnv: VardaEnv
) {
    // To test against Varda QA environment from your local machine:
    //
    // 1. Get your municipality's basic auth credentials for Varda QA environment
    //
    // 2. Set up port forwarding to Varda via a bastion host, e.g.:
    //
    //    ssh -L 65443:backend.qa.varda.opintopolku.fi:443 <bastion-host>
    //
    // 3. Edit application-local.yml:
    //
    //     evaka:
    //       ...
    //       integration:
    //         ...
    //         varda:
    //           source_system: 31
    //           url: "https://backend.qa.varda.opintopolku.fi/api"
    //           basic_auth: "<your-municipality-basic-auth-credentials>"
    //           local_dev_port: 65443
    //
    private val fuel: FuelManager =
        if (vardaEnv.localDevPort != null) {
            // Required to allow overriding the Host header
            System.setProperty("sun.net.http.allowRestrictedHeaders", "true")

            val fuelManager = FuelManagerConfig().noCertCheckFuelManager()
            fuelManager.addRequestInterceptor { next ->
                { request ->
                    val originalUri = request.url.toURI()
                    val proxyUri =
                        originalUri.copy(host = "localhost", port = vardaEnv.localDevPort)
                    request.url = proxyUri.toURL()
                    request.header("Host", vardaEnv.url.host)
                    next(request)
                }
            }
        } else {
            globalFuel
        }

    private val client =
        VardaClient(VardaTempTokenProvider(fuel, mapper, vardaEnv), fuel, mapper, vardaEnv.url)

    private val vardaEnabledRange =
        DateRange(
            // 2019-01-01 was the hard-coded cutoff date of the old Varda integration
            vardaEnv.startDate ?: LocalDate.of(2019, 1, 1),
            vardaEnv.endDate
        )

    private val omaOrganisaatioOid = ophEnv.organizerOid
    private val lahdejarjestelma = vardaEnv.sourceSystem

    init {
        asyncJobRunner.registerHandler(::updateChildJob)
    }

    fun updateChildJob(
        dbc: Database.Connection,
        clock: EvakaClock,
        job: AsyncJob.VardaUpdateChild
    ) {
        updateChild(dbc, job.childId)
    }

    fun updateChild(dbc: Database.Connection, childId: ChildId) {
        // Varda's validation rules can be deduced from the list of error codes:
        // https://virkailija.opintopolku.fi/varda/julkinen/koodistot/vardavirheviestit

        val person = dbc.read { it.getVardaPerson(childId) }
        if (person.ophPersonOid == null && person.socialSecurityNumber == null) {
            throw IllegalStateException("Child $childId has no ophOid or ssn")
        }

        val serviceNeeds = dbc.read { it.getVardaServiceNeeds(childId, vardaEnabledRange) }

        // Only fee data after 2019-09-01 can be sent to Varda (error code MA019)
        val vardaFeeDataRange = DateRange(LocalDate.of(2019, 9, 1), null)

        val feeData =
            if (serviceNeeds.isNotEmpty()) {
                    // Each maksutieto must be within the range of the start of first
                    // varhaiskasvatuspaatos and the end of the last varhaiskasvatuspaatos
                    // (see error codes MA005, MA006, MA007)
                    val serviceNeedRange =
                        FiniteDateRange(
                            serviceNeeds.minOf { it.range.start },
                            serviceNeeds.maxOf { it.range.end }
                        )
                    val feeDataRange =
                        vardaEnabledRange
                            .intersection(vardaFeeDataRange)
                            ?.intersection(serviceNeedRange)
                    if (feeDataRange != null) {
                        dbc.read { it.getVardaFeeData(childId, feeDataRange) }
                    } else {
                        emptyList()
                    }
                } else {
                    emptyList()
                }
                .filter { fee ->
                    val serviceNeed = serviceNeeds.find { it.range.overlaps(fee.validDuring) }
                    serviceNeed != null && serviceNeed.unitInvoicedByMunicipality
                }

        val henkilo = getOrCreateHenkilo(person)
        if (person.ophPersonOid == null) {
            dbc.transaction { it.updateOphPersonOid(childId, henkilo.henkilo_oid) }
        }

        val guardians = dbc.read { it.getDependantGuardians(childId) }

        val vardaLapset =
            henkilo.lapsi.map { lapsiUrl ->
                val lapsiResponse = client.getLapsi(lapsiUrl)
                val maksutiedotResponse = client.getMaksutiedotByLapsi(lapsiUrl)
                val paatoksetResponse = client.getVarhaiskasvatuspaatoksetByLapsi(lapsiUrl)
                val varhaiskasvatussuhteetResponse =
                    client.getVarhaiskasvatussuhteetByLapsi(lapsiUrl)

                VardaLapsiNode(
                    lapsi = lapsiResponse,
                    varhaiskasvatuspaatokset =
                        paatoksetResponse.map { paatos ->
                            VardaVarhaiskasvatuspaatosNode(
                                varhaiskasvatuspaatos = paatos,
                                varhaiskasvatussuhteet =
                                    varhaiskasvatussuhteetResponse.filter {
                                        it.varhaiskasvatuspaatos == paatos.url
                                    }
                            )
                        },
                    maksutiedot = maksutiedotResponse
                )
            }

        val uniqueEvakaLapset = serviceNeeds.map { Lapsi.fromEvaka(it, omaOrganisaatioOid) }.toSet()
        val evakaFeeData =
            feeData
                .mapNotNull { fee ->
                    Maksutieto.fromEvaka(guardians, fee)?.let { fee.voucherUnitOrganizerOid to it }
                }
                .groupBy({ it.first }, { it.second })

        val evakaLapset =
            uniqueEvakaLapset.map { lapsi ->
                EvakaLapsiNode(
                    lapsi = lapsi,
                    varhaiskasvatuspaatokset =
                        serviceNeeds.map { serviceNeed ->
                            EvakaVarhaiskasvatuspaatosNode(
                                varhaiskasvatuspaatos =
                                    Varhaiskasvatuspaatos.fromEvaka(serviceNeed),
                                varhaiskasvatussuhteet =
                                    listOf(Varhaiskasvatussuhde.fromEvaka(serviceNeed))
                            )
                        },
                    maksutiedot = evakaFeeData[lapsi.paos_organisaatio_oid] ?: emptyList()
                )
            }

        // To avoid validation errors caused by overlapping data, first delete outdated entries and
        // then delete new ones

        diff(
            old = vardaLapset,
            new = evakaLapset,
            eq = ::lapsiEq,
            onRemoved = { deleteLapsiRecursive(it) },
            onUnchanged = { vardaLapsi, evakaLapsi ->
                diff(
                    old = vardaLapsi.maksutiedot,
                    new = evakaLapsi.maksutiedot,
                    eq = ::maksutietoEq,
                    onRemoved = { deleteMaksutieto(it) },
                )
                diff(
                    old = vardaLapsi.varhaiskasvatuspaatokset,
                    new = evakaLapsi.varhaiskasvatuspaatokset,
                    onRemoved = { deleteVarhaiskasvatuspaatosRecursive(it) },
                    eq = ::varhaiskasvatuspaatosEq,
                    onUnchanged = { vardaPaatos, evakaPaatos ->
                        diff(
                            old = vardaPaatos.varhaiskasvatussuhteet,
                            new = evakaPaatos.varhaiskasvatussuhteet,
                            eq = ::varhaiskasvatussuhdeEq,
                            onRemoved = { deleteVarhaiskasvatussuhde(it) },
                        )
                    }
                )
            }
        )
        diff(
            old = vardaLapset,
            new = evakaLapset,
            onAdded = { createLapsiRecursive(it, henkilo.url) },
            eq = ::lapsiEq,
            onUnchanged = { vardaLapsi, evakaLapsi ->
                diff(
                    old = vardaLapsi.varhaiskasvatuspaatokset,
                    new = evakaLapsi.varhaiskasvatuspaatokset,
                    eq = ::varhaiskasvatuspaatosEq,
                    onAdded = { createVarhaiskasvatuspaatosRecursive(it, vardaLapsi.lapsi.url) },
                    onUnchanged = { vardaPaatos, evakaPaatos ->
                        diff(
                            old = vardaPaatos.varhaiskasvatussuhteet,
                            new = evakaPaatos.varhaiskasvatussuhteet,
                            eq = ::varhaiskasvatussuhdeEq,
                            onAdded = {
                                createVarhaiskasvatussuhde(
                                    it,
                                    vardaPaatos.varhaiskasvatuspaatos.url
                                )
                            },
                        )
                    }
                )
                diff(
                    old = vardaLapsi.maksutiedot,
                    new = evakaLapsi.maksutiedot,
                    eq = ::maksutietoEq,
                    onAdded = { createMaksutieto(it, vardaLapsi.lapsi.url) },
                )
            }
        )
    }

    private fun getOrCreateHenkilo(person: VardaPerson): VardaClient.HenkiloResponse =
        client.haeHenkilo(
            if (person.ophPersonOid != null) {
                VardaClient.VardaPersonSearchRequest(
                    henkilotunnus = null,
                    henkilo_oid = person.ophPersonOid
                )
            } else {
                VardaClient.VardaPersonSearchRequest(
                    henkilotunnus = person.socialSecurityNumber,
                    henkilo_oid = null
                )
            }
        )
            ?: client.createHenkilo(
                VardaClient.CreateHenkiloRequest(
                    etunimet = person.firstName,
                    sukunimi = person.lastName,
                    kutsumanimi = person.firstName.split(" ").first(),
                    henkilotunnus = person.socialSecurityNumber,
                    henkilo_oid = person.ophPersonOid
                )
            )

    private fun deleteLapsiRecursive(vardaLapsi: VardaLapsiNode) {
        var deleteLapsi = true
        vardaLapsi.varhaiskasvatuspaatokset.forEach { paatosNode ->
            val deleted = deleteVarhaiskasvatuspaatosRecursive(paatosNode)
            if (!deleted) {
                deleteLapsi = false
            }
        }
        if (deleteLapsi) {
            client.delete(vardaLapsi.lapsi.url)
        }
    }

    private fun createLapsiRecursive(evakaLapsi: EvakaLapsiNode, henkiloUrl: URI) {
        val lapsi = client.createLapsi(evakaLapsi.lapsi.toVarda(lahdejarjestelma, henkiloUrl))
        evakaLapsi.varhaiskasvatuspaatokset.forEach { paatosNode ->
            createVarhaiskasvatuspaatosRecursive(paatosNode, lapsi.url)
        }
    }

    /** Returns true if the varhaiskasvatuspaatos and associated data was deleted */
    private fun deleteVarhaiskasvatuspaatosRecursive(
        vardaVarhaiskasvatuspaatos: VardaVarhaiskasvatuspaatosNode
    ): Boolean {
        var deletePaatos = true
        vardaVarhaiskasvatuspaatos.varhaiskasvatussuhteet.forEach { suhde ->
            val deleted = deleteVarhaiskasvatussuhde(suhde)
            if (!deleted) {
                deletePaatos = false
            }
        }
        if (
            !deletePaatos ||
                (vardaVarhaiskasvatuspaatos.varhaiskasvatuspaatos.lahdejarjestelma !=
                    lahdejarjestelma ||
                    !vardaEnabledRange.contains(
                        DateRange(
                            vardaVarhaiskasvatuspaatos.varhaiskasvatuspaatos.alkamis_pvm,
                            vardaVarhaiskasvatuspaatos.varhaiskasvatuspaatos.paattymis_pvm
                        )
                    ))
        ) {
            return false
        }
        client.delete(vardaVarhaiskasvatuspaatos.varhaiskasvatuspaatos.url)
        return true
    }

    private fun createVarhaiskasvatuspaatosRecursive(
        evakaVarhaiskasvatuspaatos: EvakaVarhaiskasvatuspaatosNode,
        lapsiUrl: URI
    ) {
        val paatos =
            client.createVarhaiskasvatuspaatos(
                evakaVarhaiskasvatuspaatos.varhaiskasvatuspaatos.toVarda(lahdejarjestelma, lapsiUrl)
            )
        evakaVarhaiskasvatuspaatos.varhaiskasvatussuhteet.forEach { suhde ->
            createVarhaiskasvatussuhde(suhde, paatos.url)
        }
    }

    /** Returns true if the varhaiskasvatussuhde was deleted */
    private fun deleteVarhaiskasvatussuhde(
        vardaVarhaiskasvatussuhde: VardaClient.VarhaiskasvatussuhdeResponse
    ): Boolean {
        if (
            vardaVarhaiskasvatussuhde.lahdejarjestelma != lahdejarjestelma ||
                !vardaEnabledRange.contains(
                    DateRange(
                        vardaVarhaiskasvatussuhde.alkamis_pvm,
                        vardaVarhaiskasvatussuhde.paattymis_pvm
                    )
                )
        ) {
            return false
        }
        client.delete(vardaVarhaiskasvatussuhde.url)
        return true
    }

    private fun createVarhaiskasvatussuhde(
        evakaVarhaiskasvatussuhde: Varhaiskasvatussuhde,
        paatosUrl: URI
    ) {
        client.createVarhaiskasvatussuhde(
            evakaVarhaiskasvatussuhde.toVarda(lahdejarjestelma, paatosUrl)
        )
    }

    /** Returns true if the maksutieto was deleted */
    private fun deleteMaksutieto(vardaMaksutieto: VardaClient.MaksutietoResponse): Boolean {
        if (
            vardaMaksutieto.lahdejarjestelma != lahdejarjestelma ||
                !vardaEnabledRange.contains(
                    DateRange(vardaMaksutieto.alkamis_pvm, vardaMaksutieto.paattymis_pvm)
                )
        ) {
            return false
        }
        client.delete(vardaMaksutieto.url)
        return true
    }

    private fun createMaksutieto(vardaMaksutieto: Maksutieto, lapsiUrl: URI) {
        client.createMaksutieto(vardaMaksutieto.toVarda(lahdejarjestelma, lapsiUrl))
    }
}

private data class EvakaLapsiNode(
    val lapsi: Lapsi,
    val varhaiskasvatuspaatokset: List<EvakaVarhaiskasvatuspaatosNode>,
    val maksutiedot: List<Maksutieto>
)

private data class EvakaVarhaiskasvatuspaatosNode(
    val varhaiskasvatuspaatos: Varhaiskasvatuspaatos,
    val varhaiskasvatussuhteet: List<Varhaiskasvatussuhde>,
)

private data class VardaLapsiNode(
    val lapsi: VardaClient.LapsiResponse,
    val varhaiskasvatuspaatokset: List<VardaVarhaiskasvatuspaatosNode>,
    val maksutiedot: List<VardaClient.MaksutietoResponse>
)

private data class VardaVarhaiskasvatuspaatosNode(
    val varhaiskasvatuspaatos: VardaClient.VarhaiskasvatuspaatosResponse,
    val varhaiskasvatussuhteet: List<VardaClient.VarhaiskasvatussuhdeResponse>,
)

private fun lapsiEq(a: VardaLapsiNode, b: EvakaLapsiNode): Boolean {
    return Lapsi.fromVarda(a.lapsi) == b.lapsi
}

private fun varhaiskasvatuspaatosEq(
    a: VardaVarhaiskasvatuspaatosNode,
    b: EvakaVarhaiskasvatuspaatosNode
): Boolean {
    return Varhaiskasvatuspaatos.fromVarda(a.varhaiskasvatuspaatos) == b.varhaiskasvatuspaatos
}

private fun varhaiskasvatussuhdeEq(
    a: VardaClient.VarhaiskasvatussuhdeResponse,
    b: Varhaiskasvatussuhde
): Boolean {
    return Varhaiskasvatussuhde.fromVarda(a) == b
}

private fun maksutietoEq(a: VardaClient.MaksutietoResponse, b: Maksutieto): Boolean {
    return Maksutieto.fromVarda(a) == b
}
