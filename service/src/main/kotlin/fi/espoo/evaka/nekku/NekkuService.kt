package fi.espoo.evaka.nekku

import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.kotlin.readValue
import fi.espoo.evaka.NekkuEnv
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.async.AsyncJobType
import fi.espoo.evaka.shared.async.removeUnclaimedJobs
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.IOException
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class NekkuService(
    env: NekkuEnv?,
    jsonMapper: JsonMapper,
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>,
) {
    private val client = env?.let { NekkuHttpClient(it, jsonMapper) }

    init {
        asyncJobRunner.registerHandler(::syncNekkuCustomers)
    }

    fun syncNekkuCustomers(
        db: Database.Connection,
        clock: EvakaClock,
        job: AsyncJob.SyncNekkuCustomers,
    ) {
        if (client == null) error("Cannot sync diet list: NekkuEnv is not configured")
        val customers = getCustomerMapping(client)
        logger.info { customers.values.toString() }
    }

    fun getCustomers(client: NekkuHttpClient, jsonMapper: JsonMapper) {
        val customers = getCustomerMapping(client)
        logger.info { customers.values.toString() }
    }

    fun planNekkuCustomersSync(db: Database.Connection, clock: EvakaClock) {
        db.transaction { tx ->
            tx.removeUnclaimedJobs(setOf(AsyncJobType(AsyncJob.SyncNekkuCustomers::class)))
            asyncJobRunner.plan(
                tx,
                listOf(AsyncJob.SyncNekkuCustomers()),
                runAt = clock.now(),
                retryCount = 1,
            )
        }
    }
}

private fun getCustomerMapping(client: NekkuClient): Map<String, String> {
    logger.info { "Getting Nekku customers" }
    val customers = client.getCustomers()
    val customerMapping = customers.associateBy({ it.number }, { it.name })
    return customerMapping
}

interface NekkuClient {

    fun getCustomers(): List<NekkuCustomer>
}

class NekkuHttpClient(private val env: NekkuEnv, private val jsonMapper: JsonMapper) : NekkuClient {
    val client = OkHttpClient()

    override fun getCustomers(): List<NekkuCustomer> = request(env, "customers")

    private inline fun <reified R> request(env: NekkuEnv, endpoint: String): R {
        val fullUrl = env.url.resolve(endpoint).toString()

        val request =
            Request.Builder()
                .url(fullUrl)
                .addHeader("Accept", "application/json")
                .addHeader("X-Api-Key", env.apikey.value)
                .build()

        try {
            val response: Response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                return jsonMapper.readValue(responseBody.toString())
            } else {
                logger.info { "Request failed with code: ${response.code}" }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        throw IllegalStateException("Request failed")
    }
}

data class NekkuCustomer(val number: String, val name: String)
