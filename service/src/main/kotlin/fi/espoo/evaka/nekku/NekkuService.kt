// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later
package fi.espoo.evaka.nekku

import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.kotlin.readValue
import fi.espoo.evaka.ConstList
import fi.espoo.evaka.NekkuEnv
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.async.AsyncJobType
import fi.espoo.evaka.shared.async.removeUnclaimedJobs
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.DatabaseEnum
import fi.espoo.evaka.shared.domain.EvakaClock
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.IOException
import okhttp3.OkHttpClient
import okhttp3.Request
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
        asyncJobRunner.registerHandler(::syncNekkuSpecialDiets)
        asyncJobRunner.registerHandler(::syncNekkuProducts)
    }

    fun syncNekkuCustomers(
        db: Database.Connection,
        clock: EvakaClock,
        job: AsyncJob.SyncNekkuCustomers,
    ) {
        if (client == null) error("Cannot sync Nekku customers: NekkuEnv is not configured")
        fetchAndUpdateNekkuCustomers(client, db)
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

    fun syncNekkuSpecialDiets(
        db: Database.Connection,
        clock: EvakaClock,
        job: AsyncJob.SyncNekkuSpecialDiets,
    ) {
        if (client == null) error("Cannot sync Nekku special diets: NekkuEnv is not configured")
        fetchAndUpdateNekkuSpecialDiets(client, db)
    }

    fun planNekkuSpecialDietsSync(db: Database.Connection, clock: EvakaClock) {
        db.transaction { tx ->
            tx.removeUnclaimedJobs(setOf(AsyncJobType(AsyncJob.SyncNekkuSpecialDiets::class)))
            asyncJobRunner.plan(
                tx,
                listOf(AsyncJob.SyncNekkuSpecialDiets()),
                runAt = clock.now(),
                retryCount = 1,
            )
        }
    }

    fun syncNekkuProducts(
        db: Database.Connection,
        clock: EvakaClock,
        job: AsyncJob.SyncNekkuProducts,
    ) {
        if (client == null) error("Cannot sync Nekku products: NekkuEnv is not configured")
        fetchAndUpdateNekkuProducts(client, db)
    }
}

interface NekkuClient {

    fun getCustomers(): List<NekkuCustomer>

    fun getSpecialDiets(): List<NekkuSpecialDiet>

    fun getProducts(): List<NekkuProduct>
}

class NekkuHttpClient(private val env: NekkuEnv, private val jsonMapper: JsonMapper) : NekkuClient {
    val client = OkHttpClient()

    override fun getCustomers(): List<NekkuCustomer> {
        val request = getBaseRequest().get().url(env.url.resolve("customers").toString()).build()

        return executeRequest(request)
    }

    override fun getSpecialDiets(): List<NekkuSpecialDiet> {
        val request =
            getBaseRequest().get().url(env.url.resolve("products/options").toString()).build()

        return executeRequest(request)
    }

    private fun getBaseRequest() =
        Request.Builder()
            .addHeader("Accept", "application/json")
            .addHeader("X-Api-Key", env.apikey.value)

    private inline fun <reified T> executeRequest(request: Request): T {
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errorMessage =
                    try {
                        response.body?.string() ?: "No error body"
                    } catch (e: Exception) {
                        "Failed to read error body: ${e.message}"
                    }
                logger.error {
                    "Nekku API request failed with code ${response.code}: $errorMessage"
                }
                throw IOException("Nekku API request failed with status ${response.code}")
            }

            val body = response.body
            if (body == null) {
                logger.error { "Nekku API returned null body with status code ${response.code}" }
                throw IOException("Response body was null with status code ${response.code}")
            }

            return try {
                jsonMapper.readValue<T>(body.string())
            } catch (e: Exception) {
                logger.error(e) { "Failed to parse Nekku API response" }
                throw IOException("Failed to parse Nekku API response", e)
            }
        }
    }
}

data class NekkuCustomer(
    val number: String,
    val name: String,
    val group: String,
    val unit_size: String,
)

data class NekkuSpecialDiet(
    val id: String,
    val name: String,
    val fields: List<NekkuSpecialDietsField>,
)

data class NekkuSpecialDietsField(
    val id: String,
    val name: String,
    val type: NekkuSpecialDietType,
    val options: List<NekkuSpecialDietOption>? = null,
)

@ConstList("nekku_special_diet_type")
enum class NekkuSpecialDietType(@JsonValue val description: String) : DatabaseEnum {
    TEXT("text"),
    CHECKBOXLIST("checkboxlst");

    override val sqlType: String = "nekku_special_diet_type"
}

data class NekkuSpecialDietOption(val weight: Int, val key: String, val value: String)

data class NekkuProduct(
    val name: String,
    val sku: String,
    val options_id: String ,
    val meal_time: NekkuProductMealTime? = null,
    val meal_type: String,
)

enum class NekkuProductMealTime {

}