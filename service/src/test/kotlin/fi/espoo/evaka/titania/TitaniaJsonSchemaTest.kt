// SPDX-FileCopyrightText: 2021-2022 City of Tampere
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.titania

import com.networknt.schema.InputFormat
import com.networknt.schema.Schema
import com.networknt.schema.SchemaRegistry
import com.networknt.schema.SpecificationVersion
import fi.espoo.evaka.shared.config.defaultJsonMapperBuilder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper

class TitaniaJsonSchemaTest {

    val jsonMapper: JsonMapper =
        defaultJsonMapperBuilder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build()

    @Test
    fun `example data should be valid with json schema`() {
        val jsonNode = jsonMapper.valueToTree<JsonNode>(titaniaUpdateRequestValidExampleData)
        val errors = updateRequestJsonSchema().validate(jsonNode)
        assertThat(errors)
            .withFailMessage("Errors should be empty but contains: %s", errors)
            .isEmpty()
    }

    @Test
    fun `minimal data should be valid with json schema`() {
        val jsonNode = jsonMapper.valueToTree<JsonNode>(titaniaUpdateRequestValidMinimalData)
        val errors = updateRequestJsonSchema().validate(jsonNode)
        assertThat(errors)
            .withFailMessage("Errors should be empty but contains: %s", errors)
            .isEmpty()
    }

    @Test
    fun `get example request data should be valid with json schema`() {
        val jsonNode = jsonMapper.valueToTree<JsonNode>(titaniaGetRequestValidExampleData)
        val errors = getRequestJsonSchema().validate(jsonNode)
        assertThat(errors)
            .withFailMessage("Errors should be empty but contains: %s", errors)
            .isEmpty()
    }

    @Test
    fun `get example response data should be valid with json schema`() {
        val jsonNode = jsonMapper.valueToTree<JsonNode>(titaniaGetResponseValidExampleData)
        val errors = getResponseJsonSchema().validate(jsonNode)
        assertThat(errors)
            .withFailMessage("Errors should be empty but contains: %s", errors)
            .isEmpty()
    }

    private fun updateRequestJsonSchema() =
        getJsonSchema(ClassPathResource("titania/schema/titania-update-request-input.schema.json"))

    private fun getRequestJsonSchema() =
        getJsonSchema(ClassPathResource("titania/schema/titania-get-request-input.schema.json"))

    private fun getResponseJsonSchema() =
        getJsonSchema(ClassPathResource("titania/schema/titania-get-request-output.schema.json"))

    private fun getJsonSchema(resource: Resource): Schema {
        val schemaData = resource.inputStream.use { it.readAllBytes().toString(Charsets.UTF_8) }
        val schemaRegistry = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12)
        return schemaRegistry.getSchema(schemaData, InputFormat.JSON)
    }
}
