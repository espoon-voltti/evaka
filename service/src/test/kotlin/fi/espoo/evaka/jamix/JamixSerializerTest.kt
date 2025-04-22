// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.jamix

import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import fi.espoo.evaka.shared.config.defaultJsonMapperBuilder
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach

class JamixSerializerTest {

    private lateinit var jsonMapper: JsonMapper
    private lateinit var serializer: JacksonJamixSerializer

    @BeforeEach
    fun setup() {
        jsonMapper = defaultJsonMapperBuilder().build()
        serializer = JacksonJamixSerializer(jsonMapper)
    }

    @Test
    fun `serializes MealOrder object correctly for Jamix API`() {
        // Arrange
        val order =
            JamixClient.MealOrder(
                customerID = 456,
                deliveryDate = LocalDate.of(2023, 6, 15),
                mealOrderRows =
                    listOf(
                        JamixClient.MealOrderRow(
                            orderAmount = 7,
                            mealTypeID = 3,
                            dietID = null,
                            additionalInfo = null,
                            textureID = null,
                        ),
                        JamixClient.MealOrderRow(
                            orderAmount = 1,
                            mealTypeID = 5,
                            dietID = 123,
                            additionalInfo = "Erkki Esimerkki",
                            textureID = 1234,
                        ),
                    ),
            )

        // Act
        val serialized = serializer.serialize(order)

        // Assert
        // Convert serialized string to JsonNode for comparison
        val actualJson = jsonMapper.readTree(serialized)

        // Create expected structure directly
        val expectedJson =
            jsonMapper.createObjectNode().apply {
                put("customerID", 456)
                put("deliveryDate", "2023-06-15")
                val rows = jsonMapper.createArrayNode()
                rows.add(
                    jsonMapper.createObjectNode().apply {
                        put("orderAmount", 7)
                        put("mealTypeID", 3)
                    }
                )
                rows.add(
                    jsonMapper.createObjectNode().apply {
                        put("orderAmount", 1)
                        put("mealTypeID", 5)
                        put("dietID", 123)
                        put("additionalInfo", "Erkki Esimerkki")
                        put("textureID", 1234)
                    }
                )
                set<ObjectNode>("mealOrderRows", rows)
            }

        assertEquals(expectedJson, actualJson)
    }
}
