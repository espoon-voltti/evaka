// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.varda.new

import com.fasterxml.jackson.databind.json.JsonMapper
import fi.espoo.evaka.OphEnv
import fi.espoo.evaka.VardaEnv
import fi.espoo.evaka.pis.updateOphPersonOid
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.varda.updateUnits
import fi.espoo.voltti.logging.loggers.info
import java.net.URI
import java.time.Duration
import java.time.LocalDate
import mu.KotlinLogging
import okhttp3.OkHttpClient
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class VardaUpdateServiceNew(
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>,
    jsonMapper: JsonMapper,
    private val ophEnv: OphEnv,
    private val vardaEnv: VardaEnv
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
    private val httpClient: OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(Duration.ofMinutes(2))
            .readTimeout(Duration.ofMinutes(2))
            .writeTimeout(Duration.ofMinutes(2))
            .let {
                if (vardaEnv.localDevPort != null) {
                    it
                        // Disable ssl certificate check
                        .hostnameVerifier { _, _ -> true }
                        // Rewrite requests to Varda to go through the local port
                        .addInterceptor { chain ->
                            val originalRequest = chain.request()
                            val originalUri = originalRequest.url.toUri()
                            val proxyUri =
                                originalUri.copy(host = "localhost", port = vardaEnv.localDevPort)
                            val newRequest =
                                originalRequest
                                    .newBuilder()
                                    .url(proxyUri.toString())
                                    .header("Host", vardaEnv.url.host)
                                    .build()
                            chain.proceed(newRequest)
                        }
                } else {
                    it
                }
            }
            .build()

    private val vardaClient =
        VardaClient(
            httpClient,
            jsonMapper,
            vardaEnv.url,
            vardaEnv.basicAuth.value,
        )

    private val vardaEnabledRange =
        DateRange(
            // 2019-01-01 was the hard-coded cutoff date of the old Varda integration
            vardaEnv.startDate ?: LocalDate.of(2019, 1, 1),
            vardaEnv.endDate
        )

    init {
        check(vardaEnabledRange.start >= LocalDate.of(2019, 1, 1)) {
            "Varda enabled range must start after 2019-01-01"
        }
        asyncJobRunner.registerHandler(::updateChildJob)
    }

    fun updateUnits(dbc: Database.Connection, clock: EvakaClock) {
        updateUnits(
            dbc,
            clock,
            vardaClient,
            lahdejarjestelma = vardaEnv.sourceSystem,
            kuntakoodi = ophEnv.municipalityCode,
            vakajarjestajaUrl = vardaClient.vakajarjestajaUrl(ophEnv.organizerId)
        )
    }

    fun planChildrenUpdate(dbc: Database.Connection, clock: EvakaClock, migrationSpeed: Int = 0) {
        logger.info { "Planning Varda child updates" }

        val chunkSize = 1000
        val maxUpdatesPerDay = 5000

        val today = clock.today()
        val updater = VardaUpdater(vardaEnabledRange, ophEnv.organizerOid, vardaEnv.sourceSystem)

        val childIds =
            dbc.transaction { tx ->
                val count = tx.addNewChildrenForVardaUpdate(migrationSpeed)
                logger.info { "Added $count new children for Varda update" }

                tx.getVardaUpdateChildIds()
            }

        val childIdsRequiringUpdate =
            // Process children in chunks to avoid running out of memory
            childIds.chunked(chunkSize).flatMap { chunk ->
                dbc.read { updater.getEvakaStates(it, today, chunk) }
                    .filter { (_, _, status) -> status == VardaUpdater.Status.NEEDS_UPDATE }
                    .map { (childId, _, _) -> childId }
            }

        logger.info {
            "Children requiring Varda update: ${childIdsRequiringUpdate.size} out of ${childIds.size}"
        }

        dbc.transaction { tx ->
            asyncJobRunner.plan(
                tx,
                payloads =
                    // Children that are left out will be updated tomorrow
                    childIdsRequiringUpdate.asSequence().take(maxUpdatesPerDay).map { childId ->
                        AsyncJob.VardaUpdateChild(childId, dryRun = false)
                    },
                runAt = clock.now(),
                retryCount = 1
            )
        }
    }

    fun updateChildJob(
        dbc: Database.Connection,
        clock: EvakaClock,
        job: AsyncJob.VardaUpdateChild
    ) {
        val dryRunClient = DryRunClient()
        val updater = VardaUpdater(vardaEnabledRange, ophEnv.organizerOid, vardaEnv.sourceSystem)

        updater.updateChild(
            dbc,
            readClient = vardaClient,
            writeClient = if (job.dryRun) dryRunClient else vardaClient,
            now = clock.now(),
            childId = job.childId,
            saveState = !job.dryRun
        )

        if (job.dryRun) {
            dryRunClient.operationsHumanReadable.also { ops ->
                logger.info(
                    mapOf("operations" to ops.joinToString("\n").takeIf { it.isNotBlank() })
                ) {
                    "Varda dry run for ${job.childId}: ${ops.size} operations (see meta.operations field)"
                }
            }
        }
    }
}

class VardaUpdater(
    private val vardaEnabledRange: DateRange,
    private val omaOrganisaatioOid: String,
    private val lahdejarjestelma: String
) {
    // Varda's validation rules can be deduced from the list of error codes:
    // https://virkailija.opintopolku.fi/varda/julkinen/koodistot/vardavirheviestit

    fun updateChild(
        dbc: Database.Connection,
        readClient: VardaReadClient,
        writeClient: VardaWriteClient,
        now: HelsinkiDateTime,
        childId: ChildId,
        saveState: Boolean
    ) {
        logger.info { "Starting Varda update for child $childId" }

        try {
            val evakaState = dbc.read { tx -> getEvakaState(tx, now.toLocalDate(), childId) }
            if (evakaState == null) {
                logger.info { "Cannot compute Varda state for $childId" }
                return
            }

            val vardaState = getVardaState(readClient, evakaState.henkilo)

            val henkiloOid = vardaState.henkilo.henkilo_oid
            dbc.transaction { tx ->
                if (henkiloOid != null && evakaState.henkilo.henkilo_oid != henkiloOid) {
                    tx.updateOphPersonOid(childId, henkiloOid)
                }
            }

            logger.info(mapOf("varda" to vardaState.toString(), "evaka" to evakaState.toString())) {
                "Varda state for $childId (see the meta.varda and meta.evaka fields)"
            }

            diffAndUpdate(writeClient, vardaState, evakaState)
            if (saveState) {
                dbc.transaction { it.setVardaUpdateSuccess(childId, now, evakaState) }
            }
            logger.info { "Varda update succeeded for child $childId" }
        } catch (e: Exception) {
            logger.error(e) { "Varda update failed for child $childId" }
            if (saveState) {
                dbc.transaction { tx -> tx.setVardaUpdateError(childId, now, e.localizedMessage) }
            }
        }
    }

    fun getEvakaState(tx: Database.Read, today: LocalDate, childId: ChildId): EvakaHenkiloNode? =
        getEvakaStates(tx, today, listOf(childId)).firstOrNull()?.second

    fun getEvakaStates(
        tx: Database.Read,
        today: LocalDate,
        childIds: List<ChildId>
    ): List<Triple<ChildId, EvakaHenkiloNode, Status>> {
        val children = tx.getVardaChildren(childIds)
        val guardians = tx.getVardaGuardians(childIds)
        val serviceNeeds = tx.getVardaServiceNeeds(childIds, vardaEnabledRange)
        val feeData = tx.getVardaFeeData(childIds, vardaEnabledRange)
        val updateStates = tx.getVardaUpdateState<EvakaHenkiloNode>(childIds)
        return children.entries.mapNotNull { (childId, child) ->
            computeEvakaState(
                    today,
                    child,
                    guardians[childId] ?: emptyList(),
                    serviceNeeds[childId] ?: emptyList(),
                    feeData[childId] ?: emptyList(),
                )
                ?.let { evakaState ->
                    Triple(
                        childId,
                        evakaState,
                        if (evakaState == updateStates[childId]) Status.UP_TO_DATE
                        else Status.NEEDS_UPDATE
                    )
                }
        }
    }

    private fun computeEvakaState(
        today: LocalDate,
        child: VardaChild,
        guardians: List<VardaGuardian>,
        serviceNeeds: List<VardaServiceNeed>,
        feeData: List<VardaFeeData>
    ): EvakaHenkiloNode? {
        if (child.ophPersonOid.isNullOrBlank() && child.socialSecurityNumber == null) {
            // Child has no identifiers, so we can't send data to Varda
            return null
        }

        // Only fee data after 2019-09-01 can be sent to Varda (error code MA019)
        val vardaFeeDataRange =
            vardaEnabledRange.intersection(DateRange(LocalDate.of(2019, 9, 1), null))

        val evakaLapsiServiceNeeds =
            serviceNeeds
                .filter { it.range.start <= today } // Don't send future service needs to Varda
                .groupBy { Lapsi.fromEvaka(it, omaOrganisaatioOid) }

        return EvakaHenkiloNode(
            henkilo = Henkilo.fromEvaka(child),
            lapset =
                evakaLapsiServiceNeeds.mapNotNull { (lapsi, serviceNeedsOfLapsi) ->
                    if (serviceNeedsOfLapsi.isEmpty()) {
                        null
                    } else {
                        // Each maksutieto must be within the range of the start of first
                        // varhaiskasvatuspaatos and the end of the last varhaiskasvatuspaatos
                        // of this lapsi (see error codes MA005, MA006, MA007)
                        val feeDataRange =
                            vardaFeeDataRange?.intersection(
                                FiniteDateRange(
                                    serviceNeedsOfLapsi.minOf { it.range.start },
                                    serviceNeedsOfLapsi.maxOf { it.range.end }
                                )
                            )

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
                            maksutiedot =
                                if (feeDataRange == null) {
                                    emptyList()
                                } else {
                                    feeData
                                        .filter { fee ->
                                            fee.ophOrganizerOid == lapsi.effectiveOrganizerOid()
                                        }
                                        .mapNotNull { fee ->
                                            // Make sure the maksutieto is within the range of
                                            // varhaiskasvatuspaatokset
                                            fee.validDuring.intersection(feeDataRange)?.let {
                                                Maksutieto.fromEvaka(
                                                    guardians,
                                                    fee.copy(validDuring = it)
                                                )
                                            }
                                        }
                                }
                        )
                    }
                }
        )
    }

    private fun getVardaState(
        client: VardaReadClient,
        evakaHenkilo: Henkilo,
    ): VardaHenkiloNode {
        val henkilo =
            if (evakaHenkilo.henkilotunnus != null) {
                // Get or create henkilo if they have a henkilotunnus. Varda validates the name of
                // the person, and in this case the names are probably correct since eVaka got them
                // from VTJ.
                client.getOrCreateHenkilo(
                    VardaReadClient.GetOrCreateHenkiloRequest(
                        etunimet = evakaHenkilo.etunimet,
                        sukunimi = evakaHenkilo.sukunimi,
                        // Avoid sending both henkilotunnus and henkilo_oid (error code HE004)
                        henkilotunnus = evakaHenkilo.henkilotunnus,
                        henkilo_oid = null
                    )
                )
            } else {
                // The hae-henkilo endpoint is deprecated and limited to 500 requests/day, so only
                // use it as a fallback if the child doesn't have a henkilotunnus.
                client.haeHenkilo(
                    VardaReadClient.HaeHenkiloRequest(henkilo_oid = evakaHenkilo.henkilo_oid)
                )
            }
        return VardaHenkiloNode(
            henkilo = henkilo,
            lapset =
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
        )
    }

    fun diffAndUpdate(
        client: VardaWriteClient,
        vardaHenkilo: VardaHenkiloNode,
        evakaHenkilo: EvakaHenkiloNode,
    ) {
        // End or delete data from other source systems if needed
        val dayBeforeEvaka =
            evakaHenkilo.lapset
                .asSequence()
                .flatMap { l -> l.varhaiskasvatuspaatokset.map { it.varhaiskasvatuspaatos } }
                .minOfOrNull { it.alkamis_pvm }
                ?.minusDays(1)
        if (dayBeforeEvaka != null) {
            vardaHenkilo.lapset
                .asSequence()
                .flatMap { lapsi ->
                    lapsi.maksutiedot +
                        lapsi.varhaiskasvatuspaatokset.flatMap {
                            it.varhaiskasvatussuhteet + it.varhaiskasvatuspaatos
                        }
                }
                .filter { it.lahdejarjestelma != lahdejarjestelma }
                .forEach { client.endOrDeleteIfNeeded(it, dayBeforeEvaka) }
        }

        // Handle changes in eVaka data
        diff(
            old = vardaHenkilo.lapset,
            new = evakaHenkilo.lapset,
            eq = { vardaNode, evakaNode -> Lapsi.fromVarda(vardaNode.lapsi) == evakaNode.lapsi },
            onDeleted = { client.deleteLapsiDeep(it) },
            onUnchanged = { vardaLapsi, evakaLapsi ->
                // - Maksutieto must be *removed first* and *added last* to avoid validation
                //   errors.
                // - Neither Varda nor eVaka track children's guardian history, so only the current
                //   state of guardians is recorded to the maksutieto. Ignore guardians when
                //   comparing maksutiedot to not trigger an update if nothing else than guardians
                //   have changed.
                diff(
                    old = vardaLapsi.maksutiedot,
                    new = evakaLapsi.maksutiedot,
                    eq = { varda, evaka ->
                        Maksutieto.fromVarda(varda).copy(huoltajat = emptyList()) ==
                            evaka.copy(huoltajat = emptyList())
                    },
                    onDeleted = { client.deleteMaksutieto(it) },
                )
                diff(
                    old = vardaLapsi.varhaiskasvatuspaatokset,
                    new = evakaLapsi.varhaiskasvatuspaatokset,
                    eq = { vardaNode, evakaNode ->
                        Varhaiskasvatuspaatos.fromVarda(vardaNode.varhaiskasvatuspaatos) ==
                            evakaNode.varhaiskasvatuspaatos
                    },
                    onDeleted = { client.deleteVarhaiskasvatuspaatosDeep(it) },
                    onUnchanged = { vardaPaatos, evakaPaatos ->
                        diff(
                            old = vardaPaatos.varhaiskasvatussuhteet,
                            new = evakaPaatos.varhaiskasvatussuhteet,
                            eq = { varda, evaka -> Varhaiskasvatussuhde.fromVarda(varda) == evaka },
                            onDeleted = { client.deleteVarhaiskasvatussuhde(it) },
                            onAdded = {
                                client.createVarhaiskasvatussuhde(
                                    vardaPaatos.varhaiskasvatuspaatos.url,
                                    it
                                )
                            },
                        )
                    },
                    onAdded = { client.createVarhaiskasvatuspaatosDeep(vardaLapsi.lapsi.url, it) },
                )
                diff(
                    old = vardaLapsi.maksutiedot,
                    new = evakaLapsi.maksutiedot,
                    eq = { varda, evaka ->
                        Maksutieto.fromVarda(varda).copy(huoltajat = emptyList()) ==
                            evaka.copy(huoltajat = emptyList())
                    },
                    onAdded = { client.createMaksutieto(vardaLapsi.lapsi.url, it) },
                )
            },
            onAdded = { client.createLapsiDeep(vardaHenkilo.henkilo.url, it) },
        )
    }

    /** Like `Iterable.all`, but runs all the side effects regardless of what they return */
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
        val maksutiedotDeleted = vardaLapsi.maksutiedot.allSucceed { deleteMaksutieto(it) }
        val varhaiskasvatuspaatoksetDeleted =
            vardaLapsi.varhaiskasvatuspaatokset.allSucceed { deleteVarhaiskasvatuspaatosDeep(it) }
        return if (maksutiedotDeleted && varhaiskasvatuspaatoksetDeleted) {
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

    private fun VardaWriteClient.endOrDeleteIfNeeded(
        entity: VardaEntityWithValidity,
        endDate: LocalDate
    ) {
        if (endDate < LocalDate.of(2019, 1, 1)) {
            // Delete old entries that would be set to end before 2019-01-01, because sending data
            // before 2019-01-01 is not supported by Varda (error MI021)
            this.delete(entity)
        } else if (entity.alkamis_pvm > endDate) {
            this.delete(entity)
        } else {
            val paattymisPvm: LocalDate? = entity.paattymis_pvm
            if (paattymisPvm == null || paattymisPvm > endDate) {
                this.setPaattymisPvm(entity.url, VardaWriteClient.SetPaattymisPvmRequest(endDate))
            }
        }
    }

    enum class Status {
        UP_TO_DATE,
        NEEDS_UPDATE
    }

    data class EvakaHenkiloNode(val henkilo: Henkilo, val lapset: List<EvakaLapsiNode>)

    data class EvakaLapsiNode(
        val lapsi: Lapsi,
        val varhaiskasvatuspaatokset: List<EvakaVarhaiskasvatuspaatosNode>,
        val maksutiedot: List<Maksutieto>
    )

    data class EvakaVarhaiskasvatuspaatosNode(
        val varhaiskasvatuspaatos: Varhaiskasvatuspaatos,
        val varhaiskasvatussuhteet: List<Varhaiskasvatussuhde>,
    )

    data class VardaHenkiloNode(
        val henkilo: VardaReadClient.HenkiloResponse,
        val lapset: List<VardaLapsiNode>
    )

    data class VardaLapsiNode(
        val lapsi: VardaReadClient.LapsiResponse,
        val varhaiskasvatuspaatokset: List<VardaVarhaiskasvatuspaatosNode>,
        val maksutiedot: List<VardaReadClient.MaksutietoResponse>
    )

    data class VardaVarhaiskasvatuspaatosNode(
        val varhaiskasvatuspaatos: VardaReadClient.VarhaiskasvatuspaatosResponse,
        val varhaiskasvatussuhteet: List<VardaReadClient.VarhaiskasvatussuhdeResponse>,
    )
}

private fun <Old, New> diff(
    old: List<Old>,
    new: List<New>,
    eq: (Old, New) -> Boolean,
    onDeleted: ((Old) -> Unit)? = null,
    onAdded: ((New) -> Unit)? = null,
    onUnchanged: ((Old, New) -> Unit)? = null
) {
    if (onDeleted != null) {
        old.filter { oldItem -> new.none { newItem -> eq(oldItem, newItem) } }
            .forEach { onDeleted(it) }
    }
    if (onUnchanged != null) {
        old.forEach { oldItem ->
            val newItem = new.find { eq(oldItem, it) }
            if (newItem != null) {
                onUnchanged(oldItem, newItem)
            }
        }
    }
    if (onAdded != null) {
        new.filter { newItem -> old.none { oldItem -> eq(oldItem, newItem) } }
            .forEach { onAdded(it) }
    }
}

class DryRunClient : VardaWriteClient {
    private var ids = mutableMapOf<String, Int>()

    private data class Operation(val op: String, val type: String?, val data: Any)

    private val _operations = mutableListOf<Operation>()

    private fun create(what: String?, data: Any) {
        _operations.add(Operation("Create", what, data))
    }

    private fun nextUri(type: String): URI {
        val i = ids.getOrDefault(type, 0)
        ids[type] = i + 1
        return URI("${type}_$i")
    }

    override fun createLapsi(
        body: VardaWriteClient.CreateLapsiRequest
    ): VardaWriteClient.CreateResponse {
        create("lapsi", body)
        return VardaWriteClient.CreateResponse(url = nextUri("lapsi"))
    }

    override fun createVarhaiskasvatuspaatos(
        body: VardaWriteClient.CreateVarhaiskasvatuspaatosRequest
    ): VardaWriteClient.CreateResponse {
        create("varhaiskasvatuspaatos", body)
        return VardaWriteClient.CreateResponse(url = nextUri("varhaiskasvatuspaatos"))
    }

    override fun createVarhaiskasvatussuhde(
        body: VardaWriteClient.CreateVarhaiskasvatussuhdeRequest
    ): VardaWriteClient.CreateResponse {
        create("varhaiskasvatussuhde", body)
        return VardaWriteClient.CreateResponse(url = nextUri("varhaiskasvatussuhde"))
    }

    override fun createMaksutieto(
        body: VardaWriteClient.CreateMaksutietoRequest
    ): VardaWriteClient.CreateResponse {
        create("maksutieto", body)
        return VardaWriteClient.CreateResponse(url = nextUri("maksutieto"))
    }

    override fun <T : VardaEntity> delete(data: T) {
        _operations.add(Operation("Delete", null, data))
    }

    override fun setPaattymisPvm(url: URI, body: VardaWriteClient.SetPaattymisPvmRequest) {
        _operations.add(Operation("SetPaattymisPvm", null, url to body))
    }

    val operationsHumanReadable: List<String>
        get() = _operations.map { "${it.op} ${it.type ?: ""}: ${it.data}" }

    val operations: List<Pair<String, Any>>
        get() =
            _operations.map {
                when (it.op) {
                    "Delete" -> Pair("Delete", (it.data as VardaEntity).url)
                    else -> Pair(it.op, it.data)
                }
            }
}
