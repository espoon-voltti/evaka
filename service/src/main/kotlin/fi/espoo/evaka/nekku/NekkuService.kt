package fi.espoo.evaka.nekku

import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import fi.espoo.evaka.NekkuEnv
import okhttp3.OkHttpClient
import okhttp3.Request
import org.springframework.stereotype.Service
import okhttp3.Response
import java.io.IOException

@Service
class NekkuService {
}

interface NekkuClient {

    fun getCustomers(): List<NekkuCustomer>

}

class NekkuHttpClient(private val env: NekkuEnv, private val jsonMapper: JsonMapper) : NekkuClient {
    val client = OkHttpClient()

    override fun getCustomers(): List<NekkuCustomer> = request(env, "customers")

    private inline fun <reified R> request(env: NekkuEnv, endpoint: String): R {
        val client = OkHttpClient()
        val fullUrl = env.url.resolve(endpoint).toString()

        val request = Request.Builder()
            .url(fullUrl)
            .addHeader("Authorization", "Bearer ${env.apikey.value}")
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


data class NekkuCustomer(val customerNumber: String, val customerName: String)
