// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.sficlient.rest

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonValue
import java.util.UUID
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

// https://api.messages-qa.suomi.fi/api-docs
data class ApiUrls(
    val token: HttpUrl,
    val files: HttpUrl,
    val messages: HttpUrl,
    val changePassword: HttpUrl,
) {
    constructor(
        base: HttpUrl
    ) : this(
        token = base.newBuilder().addPathSegments("v1/token").build(),
        changePassword = base.newBuilder().addPathSegments("v1/change-password").build(),
        files = base.newBuilder().addPathSegments("v1/files").build(),
        messages = base.newBuilder().addPathSegments("v1/messages").build(),
    )
}

// A merged API error, where fields present in only some of the error types are marked nullable
@JsonIgnoreProperties(ignoreUnknown = true)
data class ApiError(
    val reason: String,
    val validationErrors: List<ValidationError>? = null,
    val messageId: Long? = null,
)

// https://api.messages-qa.suomi.fi/api-docs#model-ValidationError
data class ValidationError(val error: String)

// https://api.messages-qa.suomi.fi/api-docs#model-AccessTokenRequestBody
data class AccessTokenRequestBody(val username: String, val password: String)

// https://api.messages-qa.suomi.fi/api-docs#model-AccessTokenResponse
// Standard OAuth 2.0 response: https://datatracker.ietf.org/doc/html/rfc6749#section-5.1
data class AccessTokenResponse(
    val access_token: String,
    val token_type: String,
    @JsonInclude(JsonInclude.Include.NON_NULL) val expires_in: Long? = null,
    @JsonInclude(JsonInclude.Include.NON_NULL) val refresh_token: String? = null,
    @JsonInclude(JsonInclude.Include.NON_NULL) val scope: String? = null,
)

private val pdfMediaType = "application/pdf".toMediaType()

// https://api.messages-qa.suomi.fi/api-docs#operations-messages-postV1Files
fun pdfUploadBody(fileName: String, pdfBytes: ByteArray) =
    MultipartBody.Builder()
        .setType(MultipartBody.FORM)
        .addFormDataPart("file", fileName, pdfBytes.toRequestBody(pdfMediaType))
        .build()

// https://api.messages-qa.suomi.fi/api-docs#model-NewFileResponse
data class NewFileResponse(val fileId: UUID)

// https://api.messages-qa.suomi.fi/api-docs#model-FileReference
data class FileReference(val fileId: UUID)

// https://api.messages-qa.suomi.fi/api-docs#model-NewMessageFromClientOrganisation
data class NewMessageFromClientOrganisation(
    val externalId: String,
    val electronic: NewElectronicMessage,
    val paperMail: NewNormalPaperMail,
    val recipient: Recipient,
    val sender: Sender,
) {
    init {
        require(externalId.isNotBlank()) { "externalId must not be blank" }
    }
}

// https://api.messages-qa.suomi.fi/api-docs#model-MessageResponse
data class MessageResponse(val messageId: Long)

// https://api.messages-qa.suomi.fi/api-docs#model-NewElectronicMessage
data class NewElectronicMessage(
    val title: String,
    val body: String,
    val files: List<FileReference>,
    val visibility: Visibility = Visibility.RECIPIENT_ONLY,
) {
    init {
        require(title.isNotBlank()) { "title must not be blank" }
        require(body.isNotBlank()) { "body must not be blank" }
        require(files.size == 1) { "files must contain exactly one file" }
    }
}

enum class Visibility(@JsonValue val jsonValue: String) {
    NORMAL("Normal"),
    RECIPIENT_ONLY("Recipient only"),
}

// https://api.messages-qa.suomi.fi/api-docs#model-NewNormalPaperMail
data class NewNormalPaperMail(
    val createCoverPage: Boolean,
    val files: List<FileReference>,
    val printingAndEnvelopingService: PrintingAndEnvelopingService,
    val recipient: NewPaperMailRecipient,
    val sender: NewPaperMailSender,
) {
    init {
        require(files.size == 1) { "files must contain exactly one file" }
    }
}

// https://api.messages-qa.suomi.fi/api-docs#model-PrintingAndEnvelopingService
data class PrintingAndEnvelopingService(val postiMessaging: PostiMessaging)

// https://api.messages-qa.suomi.fi/api-docs#model-PostiMessaging
data class PostiMessaging(
    val contactDetails: ContactDetails,
    val username: String,
    val password: String,
) {
    init {
        require(username.isNotBlank()) { "username must not be blank" }
        require(password.isNotBlank()) { "password must not be blank" }
    }
}

// https://api.messages-qa.suomi.fi/api-docs#model-ContactDetails
data class ContactDetails(val email: String) {
    init {
        require(email.isNotBlank()) { "email must not be blank" }
    }
}

// https://api.messages-qa.suomi.fi/api-docs#model-NewPaperMailRecipient
data class NewPaperMailRecipient(val address: Address)

// https://api.messages-qa.suomi.fi/api-docs#model-NewPaperMailSender
data class NewPaperMailSender(val address: Address)

// https://api.messages-qa.suomi.fi/api-docs#model-Sender
data class Recipient(
    /** SSN */
    val id: String
) {
    init {
        require(id.isNotBlank()) { "id must not be blank" }
    }
}

// https://api.messages-qa.suomi.fi/api-docs#model-Sender
data class Sender(val serviceId: String) {
    init {
        require(serviceId.isNotBlank()) { "serviceId must not be blank" }
    }
}

// https://api.messages-qa.suomi.fi/api-docs#model-Address
data class Address(
    val name: String,
    /** Second address row. Used to specify, for example, addressee company or department. */
    @JsonInclude(JsonInclude.Include.NON_NULL) val additionalName: String? = null,
    val streetAddress: String,
    val zipCode: String,
    val city: String,
    /** ISO 3166-1 alpha-2 country code */
    val countryCode: String = "FI",
) {
    init {
        require(name.isNotBlank()) { "name must not be blank" }
        require(streetAddress.isNotBlank()) { "streetAddress must not be blank" }
        require(zipCode.isNotBlank()) { "zipCode must not be blank" }
        require(city.isNotBlank()) { "city must not be blank" }
        require(countryCode.isNotBlank()) { "countryCode must not be blank" }
    }
}

// https://api.messages-qa.suomi.fi/api-docs#model-ChangePasswordRequestBody
data class ChangePasswordRequestBody(
    val accessToken: String,
    val currentPassword: String,
    val newPassword: String,
)
