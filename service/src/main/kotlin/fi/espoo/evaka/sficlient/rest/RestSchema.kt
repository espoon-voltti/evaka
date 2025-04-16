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
import org.joda.time.DateTime

// https://api.messages-qa.suomi.fi/api-docs
data class ApiUrls(
    val token: HttpUrl,
    val attachments: HttpUrl,
    val messages: HttpUrl,
    val changePassword: HttpUrl,
    val events: HttpUrl,
) {
    constructor(
        base: HttpUrl
    ) : this(
        token = base.newBuilder().addPathSegments("v1/token").build(),
        changePassword = base.newBuilder().addPathSegments("v1/change-password").build(),
        attachments = base.newBuilder().addPathSegments("v2/attachments").build(),
        messages = base.newBuilder().addPathSegments("v2/messages").build(),
        events = base.newBuilder().addPathSegments("v2/events").build(),
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
data class ValidationError(val error: String, val errorCode: String?)

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

// https://api.messages-qa.suomi.fi/api-docs#operations-messages-postV3Attachments
fun pdfUploadBody(fileName: String, pdfBytes: ByteArray) =
    MultipartBody.Builder()
        .setType(MultipartBody.FORM)
        .addFormDataPart("file", fileName, pdfBytes.toRequestBody(pdfMediaType))
        .build()

// https://api.messages.suomi.fi/api-docs/#model-messages.api.rest.v2.AttachmentReference
data class AttachmentReference(val attachmentId: UUID)

// https://api.messages-qa.suomi.fi/api-docs#model-model-messages.api.rest.v2.MultichannelMessageRequestBody
data class MultichannelMessageRequestBody(
    val externalId: String,
    val electronic: ElectronicPart,
    val paperMail: PaperMailPart,
    val recipient: Recipient,
    val sender: Sender,
) {
    init {
        require(externalId.isNotBlank()) { "externalId must not be blank" }
    }
}

// https://api.messages-qa.suomi.fi/api-docs#model-MessageResponse
data class MessageResponse(val messageId: Long)

// https://api.messages-qa.suomi.fi/api-docs#model-messages.api.rest.v2.ElectronicPart
data class ElectronicPart(
    val attachments: List<AttachmentReference>,
    val body: String,
    val bodyFormat: BodyFormat = BodyFormat.TEXT,
    val messageServiceType: MessageServiceType = MessageServiceType.NORMAL,
    val notifications: MessageNotifications,
    val replyAllowedBy: ReplyAllowedBy,
    val title: String,
    val visibility: Visibility,
) {
    init {
        require(title.isNotBlank()) { "title must not be blank" }
        require(body.isNotBlank()) { "body must not be blank" }
        require(attachments.size == 1) { "attachments must contain exactly one attachment" }
    }
}

enum class BodyFormat(@JsonValue val jsonValue: String) {
    TEXT("Text"),
    MARKDOWN("Markdown"),
}

enum class MessageServiceType(@JsonValue val jsonValue: String) {
    NORMAL("Normal"),
    VERIFIABLE("Verifiable"),
}

// https://api.messages-qa.suomi.fi/api-docs#model-messages.api.rest.v2.MessageNotifications
data class MessageNotifications(
    val senderDetailsInNotifications: SenderDetailsInNotifications,
    val unreadMessageNotification: UnreadMessageNotification,
)

enum class ReplyAllowedBy(@JsonValue val jsonValue: String) {
    ANYONE("Anyone"),
    NO_ONE("No one"),
}

enum class SenderDetailsInNotifications(@JsonValue val jsonValue: String) {
    ORGANIZATION_AND_SERVICE_NAME("Organisation and service name"),
    NONE("None"),
}

data class UnreadMessageNotification(val reminder: Reminder)

enum class Reminder(@JsonValue val jsonValue: String) {
    DEFAULT_REMINDER("Default reminder"),
    NO_REMINDERS("No reminders"),
}

enum class Visibility(@JsonValue val jsonValue: String) {
    NORMAL("Normal"),
    RECIPIENT_ONLY("Recipient only"),
}

// https://api.messages-qa.suomi.fi/api-docs#model-messages.api.rest.v2.PaperMailPart
data class PaperMailPart(
    val attachments: List<AttachmentReference>,
    val colorPrinting: Boolean,
    val createAddressPage: Boolean,
    val messageServiceType: MessageServiceType,
    val printingAndEnvelopingService: PrintingAndEnvelopingService,
    val recipient: NewPaperMailRecipient,
    val rotateLandscapePages: Boolean,
    val sender: NewPaperMailSender,
    val twoSidedPrinting: Boolean,
) {
    init {
        require(attachments.size == 1) { "attachments must contain exactly one attachment" }
    }
}

// https://api.messages-qa.suomi.fi/api-docs#model-messages.api.rest.v2.PrintingAndEnvelopingService
data class PrintingAndEnvelopingService(val postiMessaging: PostiMessaging)

// https://api.messages-qa.suomi.fi/api-docs#model-messages.api.rest.v2.PostiMessaging
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

// https://api.messages-qa.suomi.fi/api-docs#model-messages.api.rest.v2.ContactDetails
data class ContactDetails(val email: String) {
    init {
        require(email.isNotBlank()) { "email must not be blank" }
    }
}

// https://api.messages-qa.suomi.fi/api-docs#model-messages.api.rest.v2.NewPaperMailRecipient
data class NewPaperMailRecipient(val address: Address)

// https://api.messages-qa.suomi.fi/api-docs#model-messages.api.rest.v2.PaperMailPart
data class NewPaperMailSender(val address: Address)

// https://api.messages-qa.suomi.fi/api-docs#model-messages.api.rest.v2.Recipient
data class Recipient(
    /** SSN */
    val id: String
) {
    init {
        require(id.isNotBlank()) { "id must not be blank" }
    }
}

// https://api.messages-qa.suomi.fi/api-docs#model-messages.api.rest.v2.Sender
data class Sender(val serviceId: String) {
    init {
        require(serviceId.isNotBlank()) { "serviceId must not be blank" }
    }
}

// https://api.messages-qa.suomi.fi/api-docs#model-messages.api.rest.v2.Address
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

// https://api.messages-qa.suomi.fi/api-docs#model-messages.api.rest.v2.GetV2Event
data class GetEventsResponse(val continuationToken: String, val events: List<GetEvent>)

data class GetEvent(val eventTime: DateTime, val metadata: String, val type: EventType)

data class MessageEventMetadata(
    @JsonInclude(JsonInclude.Include.NON_NULL) val externalId: String? = null,
    val messageId: Long,
    val serviceId: String,
) {
    init {
        require(serviceId.isNotBlank()) { "serviceId must not be blank" }
    }
}

enum class EventType(@JsonValue val jsonValue: String) {
    ELECTRONIC_MESSAGE_CREATED("Electronic message created"),
    ELECTRONIC_MESSAGE_READ("Electronic message read"),
    ELECTRONIC_MESSAGE_FROM_END_USER("Electronic message from end user"),
    RECEIPT_CONFIRMED("Receipt confirmed"),
    PAPER_MAIL_CREATED("Paper mail created"),
    SENT_FOR_PRINTING_AND_ENVELOPING("Sent for printing and enveloping"),
    POSTI_RECEIPT_CONFIRMED("Posti: receipt confirmed"),
    POSTI_RETURNED_TO_SENDER("Posti: returned to sender"),
    POSTI_UNRESOLVED("Posti: unresolved"),
}
