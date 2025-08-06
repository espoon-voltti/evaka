// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.nekku

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.kotlin.readValue
import fi.espoo.evaka.NekkuEnv
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.jdbi.v3.json.Json

private val logger = KotlinLogging.logger {}

interface NekkuClient {

    data class NekkuOrder(
        @JsonProperty("delivery_date") val deliveryDate: String,
        @JsonProperty("customer_number") val customerNumber: String,
        @JsonProperty("group_id") val groupId: String,
        val items: List<Item>,
        val description: String?,
    )

    data class Item(
        val sku: String,
        val quantity: Int,
        @JsonProperty("product_options") val productOptions: Set<ProductOption>?,
    )

    data class ProductOption(@JsonProperty("field_id") val fieldId: String, val value: String)

    data class NekkuOrders(
        val orders: List<NekkuOrder>,
        @JsonProperty("dry_run") val dryRun: Boolean,
    )

    fun getCustomers(): List<NekkuCustomer>

    fun getSpecialDiets(): List<NekkuSpecialDiet>

    fun getProducts(): List<NekkuProduct>

    fun createNekkuMealOrder(nekkuOrders: NekkuOrders): NekkuOrderResult
}

class NekkuHttpClient(private val env: NekkuEnv, private val jsonMapper: JsonMapper) : NekkuClient {
    val client = OkHttpClient()

    override fun getCustomers(): List<NekkuCustomer> {
        val request = getBaseRequest().get().url(env.url.resolve("customers").toString()).build()

        return executeRequest<List<NekkuApiCustomer>>(request).map { it.toEvaka() }
    }

    override fun getSpecialDiets(): List<NekkuSpecialDiet> {
        val request =
            getBaseRequest().get().url(env.url.resolve("products/options").toString()).build()

        return executeRequest<List<NekkuApiSpecialDiet>>(request).map { it.toEvaka() }
    }

    override fun getProducts(): List<NekkuProduct> {
        val request = getBaseRequest().get().url(env.url.resolve("products").toString()).build()

        return executeRequest<List<NekkuApiProduct>>(request).map { it.toEvaka() }
    }

    override fun createNekkuMealOrder(nekkuOrders: NekkuClient.NekkuOrders): NekkuOrderResult {
        val requestBody =
            jsonMapper
                .writeValueAsString(nekkuOrders)
                .toRequestBody("application/json".toMediaTypeOrNull())
        val request =
            getBaseRequest().post(requestBody).url(env.url.resolve("orders").toString()).build()

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
                        response.body.string()
                    } catch (e: Exception) {
                        "Failed to read error body: ${e.message}"
                    }
                logger.error {
                    "Nekku API request failed with code ${response.code}: $errorMessage"
                }
                throw IOException("Nekku API request failed with status ${response.code}")
            }

            val body = response.body

            return try {
                jsonMapper.readValue<T>(body.string())
            } catch (e: Exception) {
                logger.error(e) { "Failed to parse Nekku API response: ${body.string()}" }
                throw IOException("Failed to parse Nekku API response", e)
            }
        }
    }
}

data class NekkuApiCustomer(
    val number: String,
    val name: String,
    val group: String,
    @Json @JsonProperty("type_map") val customerType: List<CustomerApiType>,
) {
    fun toEvaka(): NekkuCustomer =
        NekkuCustomer(number, name, group, customerType.map { it.toEvaka() })
}

data class CustomerApiType(val weekdays: List<NekkuCustomerApiWeekday>, val type: String) {
    fun toEvaka(): CustomerType = CustomerType(weekdays.map { it.toEvaka() }, type)
}

enum class NekkuCustomerApiWeekday(@JsonValue val description: String) {
    MONDAY("monday") {
        override fun toEvaka(): NekkuCustomerWeekday = NekkuCustomerWeekday.MONDAY
    },
    TUESDAY("tuesday") {
        override fun toEvaka(): NekkuCustomerWeekday = NekkuCustomerWeekday.TUESDAY
    },
    WEDNESDAY("wednesday") {
        override fun toEvaka(): NekkuCustomerWeekday = NekkuCustomerWeekday.WEDNESDAY
    },
    THURSDAY("thursday") {
        override fun toEvaka(): NekkuCustomerWeekday = NekkuCustomerWeekday.THURSDAY
    },
    FRIDAY("friday") {
        override fun toEvaka(): NekkuCustomerWeekday = NekkuCustomerWeekday.FRIDAY
    },
    SATURDAY("saturday") {
        override fun toEvaka(): NekkuCustomerWeekday = NekkuCustomerWeekday.SATURDAY
    },
    SUNDAY("sunday") {
        override fun toEvaka(): NekkuCustomerWeekday = NekkuCustomerWeekday.SUNDAY
    },
    WEEKDAYHOLIDAY("weekday_holiday") {
        override fun toEvaka(): NekkuCustomerWeekday = NekkuCustomerWeekday.WEEKDAYHOLIDAY
    };

    abstract fun toEvaka(): NekkuCustomerWeekday
}

data class NekkuApiSpecialDiet(
    val id: String,
    val name: String,
    val fields: List<NekkuApiSpecialDietsField>,
) {
    fun toEvaka(): NekkuSpecialDiet = NekkuSpecialDiet(id, name, fields.map { it.toEvaka() })
}

data class NekkuApiSpecialDietsField(
    val id: String,
    val name: String,
    val type: NekkuApiSpecialDietType,
    val options: List<NekkuSpecialDietOption>? = null,
) {
    fun toEvaka(): NekkuSpecialDietsField =
        NekkuSpecialDietsField(id, name, type.toEvaka(), options)
}

enum class NekkuApiSpecialDietType(@JsonValue val jsonValue: String) {
    Text("text") {
        override fun toEvaka(): NekkuSpecialDietType = NekkuSpecialDietType.TEXT
    },
    CheckBoxLst("checkboxlst") {
        override fun toEvaka(): NekkuSpecialDietType = NekkuSpecialDietType.CHECKBOXLIST
    },
    Checkbox("checkbox") {
        override fun toEvaka(): NekkuSpecialDietType = NekkuSpecialDietType.CHECKBOX
    },
    Radio("radio") {
        override fun toEvaka(): NekkuSpecialDietType = NekkuSpecialDietType.RADIO
    },
    Textarea("textarea") {
        override fun toEvaka(): NekkuSpecialDietType = NekkuSpecialDietType.TEXTAREA
    },
    Email("email") {
        override fun toEvaka(): NekkuSpecialDietType = NekkuSpecialDietType.EMAIL
    };

    abstract fun toEvaka(): NekkuSpecialDietType
}

data class NekkuApiProduct(
    val name: String,
    val sku: String,
    @JsonProperty("options_id") val optionsId: String,
    @JsonProperty("customer_types") val customerTypes: List<String>,
    @JsonProperty("meal_time") val mealTime: List<NekkuProductMealTime>? = null,
    @JsonProperty("meal_type") val mealType: NekkuApiProductMealType? = null,
) {
    fun toEvaka(): NekkuProduct =
        NekkuProduct(name, sku, optionsId, customerTypes, mealTime, mealType?.toEvaka())
}

enum class NekkuApiProductMealType(@JsonValue val description: String) {
    Vegaani("vegaani") {
        override fun toEvaka(): NekkuProductMealType = NekkuProductMealType.VEGAN
    },
    Kasvis("kasvis") {
        override fun toEvaka(): NekkuProductMealType = NekkuProductMealType.VEGETABLE
    };

    abstract fun toEvaka(): NekkuProductMealType
}
