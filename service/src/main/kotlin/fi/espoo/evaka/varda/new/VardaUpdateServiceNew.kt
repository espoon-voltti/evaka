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

    private val vardaClient =
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
        if (job.dryRun) {
            val dryRunClient = DryRunClient()
            updateChild(dbc, job.childId, vardaRead = vardaClient, vardaWrite = dryRunClient)

            dryRunClient.operations.also { ops ->
                logger.info(
                    mapOf("operations" to ops.joinToString("\n").takeIf { it.isNotBlank() })
                ) {
                    "Varda dry run for ${job.childId}: ${ops.size} operations (see meta.operations field)"
                }
            }
        } else {
            updateChild(dbc, job.childId, vardaRead = vardaClient, vardaWrite = vardaClient)
        }
    }

    fun updateChild(
        dbc: Database.Connection,
        childId: ChildId,
        vardaRead: VardaReadClient,
        vardaWrite: VardaWriteClient
    ) {
        // Varda's validation rules can be deduced from the list of error codes:
        // https://virkailija.opintopolku.fi/varda/julkinen/koodistot/vardavirheviestit

        val person = dbc.read { it.getVardaPerson(childId) }
        if (person.ophPersonOid == null && person.socialSecurityNumber == null) {
            throw IllegalStateException("Child $childId has no ophOid or ssn")
        }

        val guardians = dbc.read { it.getDependantGuardians(childId) }
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

        val evakaLapsiServiceNeeds =
            serviceNeeds.groupBy { Lapsi.fromEvaka(it, omaOrganisaatioOid) }
        val evakaFeeData =
            feeData
                .mapNotNull { fee ->
                    Maksutieto.fromEvaka(guardians, fee)?.let { fee.voucherUnitOrganizerOid to it }
                }
                .groupBy({ it.first }, { it.second })

        val evakaHenkilo =
            EvakaHenkiloNode(
                henkilo = Henkilo.fromEvaka(person),
                lapset =
                    evakaLapsiServiceNeeds.map { (lapsi, serviceNeedsOfLapsi) ->
                        EvakaLapsiNode(
                            lapsi = lapsi,
                            varhaiskasvatuspaatokset =
                                serviceNeedsOfLapsi.map { serviceNeed ->
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
            )

        val vardaHenkilo =
            vardaRead
                .haeHenkilo(
                    if (person.ophPersonOid != null) {
                        VardaReadClient.HaeHenkiloRequest(
                            henkilotunnus = null,
                            henkilo_oid = person.ophPersonOid
                        )
                    } else {
                        VardaReadClient.HaeHenkiloRequest(
                            henkilotunnus = person.socialSecurityNumber,
                            henkilo_oid = null
                        )
                    }
                )
                ?.let { henkilo ->
                    VardaHenkilo(
                        henkilo = henkilo,
                        lapset =
                            henkilo.lapsi.map { lapsiUrl ->
                                val lapsiResponse = vardaRead.getLapsi(lapsiUrl)
                                val maksutiedotResponse = vardaRead.getMaksutiedotByLapsi(lapsiUrl)
                                val paatoksetResponse =
                                    vardaRead.getVarhaiskasvatuspaatoksetByLapsi(lapsiUrl)
                                val varhaiskasvatussuhteetResponse =
                                    vardaRead.getVarhaiskasvatussuhteetByLapsi(lapsiUrl)

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
                    )
                }

        logger.info(
            mapOf(
                "varda" to vardaHenkilo?.toPrettyString(),
                "evaka" to evakaHenkilo.toPrettyString()
            )
        ) {
            "Varda update state for $childId (see the meta.varda and meta.evaka fields)"
        }

        val henkilo =
            vardaHenkilo?.henkilo ?: vardaWrite.createHenkilo(evakaHenkilo.henkilo.toVarda())
        if (henkilo.henkilo_oid != null && person.ophPersonOid != henkilo.henkilo_oid) {
            dbc.transaction { it.updateOphPersonOid(childId, henkilo.henkilo_oid) }
        }

        diff(
            old = vardaHenkilo?.lapset ?: emptyList(),
            new = evakaHenkilo.lapset,
            eq = { vardaNode, evakaNode -> Lapsi.fromVarda(vardaNode.lapsi) == evakaNode.lapsi },
            onRemoved = { vardaWrite.deleteLapsiDeep(it) },
            onAdded = { vardaWrite.createLapsiDeep(henkilo.url, it) },
            onUnchanged = { vardaLapsi, evakaLapsi ->
                // Maksutieto must be *removed first* and *added last* to avoid validation
                // errors
                diff(
                    old = vardaLapsi.maksutiedot,
                    new = evakaLapsi.maksutiedot,
                    eq = { varda, evaka -> Maksutieto.fromVarda(varda) == evaka },
                    onRemoved = { vardaWrite.deleteMaksutieto(it) },
                )
                diff(
                    old = vardaLapsi.varhaiskasvatuspaatokset,
                    new = evakaLapsi.varhaiskasvatuspaatokset,
                    eq = { vardaNode, evakaNode ->
                        Varhaiskasvatuspaatos.fromVarda(vardaNode.varhaiskasvatuspaatos) ==
                            evakaNode.varhaiskasvatuspaatos
                    },
                    onRemoved = { vardaWrite.deleteVarhaiskasvatuspaatosDeep(it) },
                    onAdded = {
                        vardaWrite.createVarhaiskasvatuspaatosDeep(vardaLapsi.lapsi.url, it)
                    },
                    onUnchanged = { vardaPaatos, evakaPaatos ->
                        diff(
                            old = vardaPaatos.varhaiskasvatussuhteet,
                            new = evakaPaatos.varhaiskasvatussuhteet,
                            eq = { varda, evaka -> Varhaiskasvatussuhde.fromVarda(varda) == evaka },
                            onRemoved = { vardaWrite.deleteVarhaiskasvatussuhde(it) },
                            onAdded = {
                                vardaWrite.createVarhaiskasvatussuhde(
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
                    onAdded = { vardaWrite.createMaksutieto(vardaLapsi.lapsi.url, it) },
                )
            }
        )
    }

    private fun <T> Iterable<T>.allSucceed(sideEffect: (T) -> Boolean): Boolean {
        var result = true
        for (e in this) {
            val x = sideEffect(e)
            if (!x) {
                result = false
            }
        }
        return result
    }

    /** Returns true if the lapsi and associated data were deleted */
    private fun VardaWriteClient.deleteLapsiDeep(vardaLapsi: VardaLapsiNode): Boolean {
        val varhaiskasvatuspaatoksetDeleted =
            vardaLapsi.varhaiskasvatuspaatokset.allSucceed { deleteVarhaiskasvatuspaatosDeep(it) }
        val maksutiedotDeleted = vardaLapsi.maksutiedot.allSucceed { deleteMaksutieto(it) }
        return if (varhaiskasvatuspaatoksetDeleted && maksutiedotDeleted) {
            delete(vardaLapsi.lapsi)
            true
        } else {
            false
        }
    }

    /** Returns true if the varhaiskasvatuspaatos and associated data were deleted */
    private fun VardaWriteClient.deleteVarhaiskasvatuspaatosDeep(
        vardaVarhaiskasvatuspaatos: VardaVarhaiskasvatuspaatosNode
    ): Boolean {
        val deletePaatos =
            vardaVarhaiskasvatuspaatos.varhaiskasvatussuhteet.allSucceed { suhde ->
                deleteVarhaiskasvatussuhde(suhde)
            }
        return if (
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
            false
        } else {
            delete(vardaVarhaiskasvatuspaatos.varhaiskasvatuspaatos)
            true
        }
    }

    /** Returns true if the varhaiskasvatussuhde was deleted */
    private fun VardaWriteClient.deleteVarhaiskasvatussuhde(
        vardaVarhaiskasvatussuhde: VardaReadClient.VarhaiskasvatussuhdeResponse
    ): Boolean =
        if (
            vardaVarhaiskasvatussuhde.lahdejarjestelma != lahdejarjestelma ||
                !vardaEnabledRange.contains(
                    DateRange(
                        vardaVarhaiskasvatussuhde.alkamis_pvm,
                        vardaVarhaiskasvatussuhde.paattymis_pvm
                    )
                )
        ) {
            false
        } else {
            delete(vardaVarhaiskasvatussuhde)
            true
        }

    /** Returns true if the maksutieto was deleted */
    private fun VardaWriteClient.deleteMaksutieto(
        vardaMaksutieto: VardaReadClient.MaksutietoResponse
    ): Boolean =
        if (
            vardaMaksutieto.lahdejarjestelma != lahdejarjestelma ||
                !vardaEnabledRange.contains(
                    DateRange(vardaMaksutieto.alkamis_pvm, vardaMaksutieto.paattymis_pvm)
                )
        ) {
            false
        } else {
            delete(vardaMaksutieto)
            true
        }

    private fun VardaWriteClient.createLapsiDeep(henkiloUrl: URI, evakaLapsi: EvakaLapsiNode) {
        val lapsiUrl = this.createLapsi(evakaLapsi.lapsi.toVarda(lahdejarjestelma, henkiloUrl)).url
        evakaLapsi.varhaiskasvatuspaatokset.forEach { paatosNode ->
            createVarhaiskasvatuspaatosDeep(lapsiUrl, paatosNode)
        }
        evakaLapsi.maksutiedot.forEach { maksutieto -> createMaksutieto(lapsiUrl, maksutieto) }
    }

    private fun VardaWriteClient.createVarhaiskasvatuspaatosDeep(
        lapsiUrl: URI,
        evakaVarhaiskasvatuspaatos: EvakaVarhaiskasvatuspaatosNode,
    ) {
        val paatosUrl =
            createVarhaiskasvatuspaatos(
                    evakaVarhaiskasvatuspaatos.varhaiskasvatuspaatos.toVarda(
                        lahdejarjestelma,
                        lapsiUrl
                    )
                )
                .url
        evakaVarhaiskasvatuspaatos.varhaiskasvatussuhteet.forEach { suhde ->
            createVarhaiskasvatussuhde(paatosUrl, suhde)
        }
    }

    private fun VardaWriteClient.createVarhaiskasvatussuhde(
        paatosUrl: URI,
        evakaVarhaiskasvatussuhde: Varhaiskasvatussuhde
    ) {
        createVarhaiskasvatussuhde(evakaVarhaiskasvatussuhde.toVarda(lahdejarjestelma, paatosUrl))
    }

    private fun VardaWriteClient.createMaksutieto(lapsiUrl: URI, evakaMaksutieto: Maksutieto) {
        createMaksutieto(evakaMaksutieto.toVarda(lahdejarjestelma, lapsiUrl))
    }

    private data class EvakaHenkiloNode(val henkilo: Henkilo, val lapset: List<EvakaLapsiNode>) {
        fun toPrettyString(): String =
            "Henkilo(\n  $henkilo,\n  lapset = [\n" +
                lapset.joinToString(",\n") { "    $it" } +
                "  ]\n)"
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

    private data class VardaHenkilo(
        val henkilo: VardaReadClient.HenkiloResponse,
        val lapset: List<VardaLapsiNode>
    ) {
        fun toPrettyString(): String =
            "HenkiloResponse(\n" +
                "  henkilo = $henkilo,\n" +
                "  lapset = [\n" +
                lapset.joinToString(",\n") { "    $it" } +
                "  ]\n" +
                ")"
    }

    private data class VardaLapsiNode(
        val lapsi: VardaReadClient.LapsiResponse,
        val varhaiskasvatuspaatokset: List<VardaVarhaiskasvatuspaatosNode>,
        val maksutiedot: List<VardaReadClient.MaksutietoResponse>
    )

    private data class VardaVarhaiskasvatuspaatosNode(
        val varhaiskasvatuspaatos: VardaReadClient.VarhaiskasvatuspaatosResponse,
        val varhaiskasvatussuhteet: List<VardaReadClient.VarhaiskasvatussuhdeResponse>,
    )
}

private fun <Old, New> diff(
    old: List<Old>,
    new: List<New>,
    eq: (Old, New) -> Boolean,
    onRemoved: ((Old) -> Unit)? = null,
    onAdded: ((New) -> Unit)? = null,
    onUnchanged: ((Old, New) -> Unit)? = null
) {
    if (onRemoved != null) {
        old.filter { oldItem -> new.none { newItem -> eq(oldItem, newItem) } }
            .forEach { onRemoved(it) }
    }
    if (onAdded != null) {
        new.filter { newItem -> old.none { oldItem -> eq(oldItem, newItem) } }
            .forEach { onAdded(it) }
    }
    if (onUnchanged != null) {
        old.forEach { oldItem ->
            val newItem = new.find { eq(oldItem, it) }
            if (newItem != null) {
                onUnchanged(oldItem, newItem)
            }
        }
    }
}

private class DryRunClient : VardaWriteClient {
    private var ids = mutableMapOf<String, Int>()
    private val _operations = mutableListOf<String>()

    private fun nextUri(type: String): URI {
        val i = ids.getOrDefault(type, 0)
        ids[type] = i + 1
        return URI("${type}_$i")
    }

    override fun createHenkilo(
        body: VardaWriteClient.CreateHenkiloRequest
    ): VardaReadClient.HenkiloResponse {
        _operations.add("Create henkilo: $body")
        return VardaReadClient.HenkiloResponse(
            henkilo_oid = null,
            url = nextUri("henkilo"),
            lapsi = emptyList()
        )
    }

    override fun createLapsi(
        body: VardaWriteClient.CreateLapsiRequest
    ): VardaWriteClient.CreateResponse {
        _operations.add("Create lapsi: $body")
        return VardaWriteClient.CreateResponse(url = nextUri("lapsi"))
    }

    override fun createVarhaiskasvatuspaatos(
        body: VardaWriteClient.CreateVarhaiskasvatuspaatosRequest
    ): VardaWriteClient.CreateResponse {
        _operations.add("Create varhaiskasvatuspaatos: $body")
        return VardaWriteClient.CreateResponse(url = nextUri("varhaiskasvatuspaatos"))
    }

    override fun createVarhaiskasvatussuhde(
        body: VardaWriteClient.CreateVarhaiskasvatussuhdeRequest
    ): VardaWriteClient.CreateResponse {
        _operations.add("Create varhaiskasvatussuhde: $body")
        return VardaWriteClient.CreateResponse(url = nextUri("varhaiskasvatussuhde"))
    }

    override fun createMaksutieto(
        body: VardaWriteClient.CreateMaksutietoRequest
    ): VardaWriteClient.CreateResponse {
        _operations.add("Create maksutieto: $body")
        return VardaWriteClient.CreateResponse(url = nextUri("maksutieto"))
    }

    override fun <T : VardaEntity> delete(data: T) {
        _operations.add("Delete $data")
    }

    val operations: List<String>
        get() = _operations
}
