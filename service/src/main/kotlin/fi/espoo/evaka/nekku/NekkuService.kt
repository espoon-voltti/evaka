package fi.espoo.evaka.nekku

import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.kotlin.readValue
import fi.espoo.evaka.NekkuEnv
import io.github.oshai.kotlinlogging.KotlinLogging
import okhttp3.OkHttpClient
import okhttp3.Request
import org.springframework.stereotype.Service
import okhttp3.Response
import java.io.IOException

private val logger = KotlinLogging.logger {}

@Service
class NekkuService (
    env: NekkuEnv,
    jsonMapper: JsonMapper) {
    private val client = NekkuHttpClient(env, jsonMapper)

    fun getCustomers(client: NekkuHttpClient, jsonMapper: JsonMapper) {
        val customers = getCustomerMapping(client)
        println(customers.values.toString())
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

        val request = Request.Builder()
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
                println("Request failed with code: ${response.code}")
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        throw IllegalStateException("Request failed")
    }

}

data class NekkuCustomer(val number: String, val name: String)
