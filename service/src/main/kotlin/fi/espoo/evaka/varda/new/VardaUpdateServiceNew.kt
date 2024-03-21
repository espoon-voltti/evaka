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
import fi.espoo.voltti.logging.loggers.info
import java.net.URI
import java.time.LocalDate
import mu.KotlinLogging
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

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
        updateChild(dbc, job.childId, job.dryRun)
    }

    fun updateChild(dbc: Database.Connection, childId: ChildId, dryRun: Boolean) {
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

        logger.info(mapOf("state" to mapOf("varda" to vardaLapset, "evaka" to evakaLapset))) {
            "Varda update state for $childId (see the meta.state field)"
        }

        if (dryRun) {
            val operations = mutableListOf<Map<String, Any>>()
            val op = { operation: String, data: Any ->
                operations.add(mapOf("operation" to operation, "data" to data))
            }
            diffDeep(
                vardaLapset,
                evakaLapset,
                onLapsiRemoved = { op("Delete lapsi", it) },
                onLapsiAdded = { op("Create lapsi", it) },
                onVarhaiskasvatuspaatosRemoved = { op("Delete varhaiskasvatuspaatos", it) },
                onVarhaiskasvatuspaatosAdded = { _, it -> op("Create varhaiskasvatuspaatos", it) },
                onVarhaiskasvatussuhdeRemoved = { op("Delete varhaiskasvatussuhde", it) },
                onVarhaiskasvatussuhdeAdded = { _, it -> op("Create varhaiskasvatussuhde", it) },
                onMaksutietoRemoved = { op("Delete maksutieto", it) },
                onMaksutietoAdded = { _, it -> op("Create maksutieto", it) }
            )
            logger.info(mapOf("operations" to operations)) {
                "Dry run for $childId (see the meta.operations field)"
            }
        } else {
            // To avoid validation errors caused by overlapping data, first delete outdated entries
            // and then delete new ones
            diffDeep(
                vardaLapset,
                evakaLapset,
                onLapsiRemoved = ::deleteLapsiRecursive,
                onVarhaiskasvatuspaatosRemoved = ::deleteVarhaiskasvatuspaatosRecursive,
                onVarhaiskasvatussuhdeRemoved = ::deleteVarhaiskasvatussuhde,
                onMaksutietoRemoved = ::deleteMaksutieto
            )
            diffDeep(
                vardaLapset,
                evakaLapset,
                onLapsiAdded = { createLapsiRecursive(henkilo.url, it) },
                onVarhaiskasvatuspaatosAdded = ::createVarhaiskasvatuspaatosRecursive,
                onVarhaiskasvatussuhdeAdded = ::createVarhaiskasvatussuhde,
                onMaksutietoAdded = ::createMaksutieto
            )
        }
    }

    private fun diffDeep(
        vardaLapset: List<VardaLapsiNode>,
        evakaLapset: List<EvakaLapsiNode>,
        onLapsiRemoved: (VardaLapsiNode) -> Unit = {},
        onLapsiAdded: (EvakaLapsiNode) -> Unit = {},
        onVarhaiskasvatuspaatosRemoved: (VardaVarhaiskasvatuspaatosNode) -> Unit = {},
        onVarhaiskasvatuspaatosAdded: (URI, EvakaVarhaiskasvatuspaatosNode) -> Unit = { _, _ -> },
        onVarhaiskasvatussuhdeRemoved: (VardaClient.VarhaiskasvatussuhdeResponse) -> Unit = {},
        onVarhaiskasvatussuhdeAdded: (URI, Varhaiskasvatussuhde) -> Unit = { _, _ -> },
        onMaksutietoRemoved: (VardaClient.MaksutietoResponse) -> Unit = {},
        onMaksutietoAdded: (URI, Maksutieto) -> Unit = { _, _ -> }
    ) {
        diff(
            old = vardaLapset,
            new = evakaLapset,
            eq = { vardaNode, evakaNode -> Lapsi.fromVarda(vardaNode.lapsi) == evakaNode.lapsi },
            onRemoved = onLapsiRemoved,
            onAdded = onLapsiAdded,
            onUnchanged = { vardaLapsi, evakaLapsi ->
                // Maksutieto must be *removed first* and *added last* to avoid validation errors
                diff(
                    old = vardaLapsi.maksutiedot,
                    new = evakaLapsi.maksutiedot,
                    eq = { varda, evaka -> Maksutieto.fromVarda(varda) == evaka },
                    onRemoved = onMaksutietoRemoved,
                )
                diff(
                    old = vardaLapsi.varhaiskasvatuspaatokset,
                    new = evakaLapsi.varhaiskasvatuspaatokset,
                    eq = { vardaNode, evakaNode ->
                        Varhaiskasvatuspaatos.fromVarda(vardaNode.varhaiskasvatuspaatos) ==
                            evakaNode.varhaiskasvatuspaatos
                    },
                    onRemoved = onVarhaiskasvatuspaatosRemoved,
                    onAdded = { onVarhaiskasvatuspaatosAdded(vardaLapsi.lapsi.url, it) },
                    onUnchanged = { vardaPaatos, evakaPaatos ->
                        diff(
                            old = vardaPaatos.varhaiskasvatussuhteet,
                            new = evakaPaatos.varhaiskasvatussuhteet,
                            eq = { varda, evaka -> Varhaiskasvatussuhde.fromVarda(varda) == evaka },
                            onRemoved = onVarhaiskasvatussuhdeRemoved,
                            onAdded = {
                                onVarhaiskasvatussuhdeAdded(
                                    vardaPaatos.varhaiskasvatuspaatos.url,
                                    it
                                )
                            },
                        )
                    }
                )
                diff(
                    old = vardaLapsi.maksutiedot,
                    new = evakaLapsi.maksutiedot,
                    eq = { varda, evaka -> Maksutieto.fromVarda(varda) == evaka },
                    onAdded = { onMaksutietoAdded(vardaLapsi.lapsi.url, it) },
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

    private fun createLapsiRecursive(henkiloUrl: URI, evakaLapsi: EvakaLapsiNode) {
        val lapsi = client.createLapsi(evakaLapsi.lapsi.toVarda(lahdejarjestelma, henkiloUrl))
        evakaLapsi.varhaiskasvatuspaatokset.forEach { paatosNode ->
            createVarhaiskasvatuspaatosRecursive(lapsi.url, paatosNode)
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
        lapsiUrl: URI,
        evakaVarhaiskasvatuspaatos: EvakaVarhaiskasvatuspaatosNode,
    ) {
        val paatos =
            client.createVarhaiskasvatuspaatos(
                evakaVarhaiskasvatuspaatos.varhaiskasvatuspaatos.toVarda(lahdejarjestelma, lapsiUrl)
            )
        evakaVarhaiskasvatuspaatos.varhaiskasvatussuhteet.forEach { suhde ->
            createVarhaiskasvatussuhde(paatos.url, suhde)
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
        paatosUrl: URI,
        evakaVarhaiskasvatussuhde: Varhaiskasvatussuhde
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

    private fun createMaksutieto(lapsiUrl: URI, vardaMaksutieto: Maksutieto) {
        client.createMaksutieto(vardaMaksutieto.toVarda(lahdejarjestelma, lapsiUrl))
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
}

private fun <Old, New> diff(
    old: List<Old>,
    new: List<New>,
    eq: (Old, New) -> Boolean,
    onRemoved: (Old) -> Unit = { _ -> },
    onAdded: (New) -> Unit = { _ -> },
    onUnchanged: (Old, New) -> Unit = { _, _ -> }
) {
    old.filter { oldItem -> new.none { newItem -> eq(oldItem, newItem) } }.forEach(onRemoved)
    new.filter { newItem -> old.none { oldItem -> eq(oldItem, newItem) } }.forEach(onAdded)
    old.forEach { oldItem ->
        val newItem = new.find { eq(oldItem, it) }
        if (newItem != null) {
            onUnchanged(oldItem, newItem)
        }
    }
}
