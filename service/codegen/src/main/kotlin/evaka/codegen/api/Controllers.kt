// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.codegen.api

import fi.espoo.evaka.shared.auth.AuthenticatedUser
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.full.*
import kotlin.reflect.jvm.javaMethod
import kotlin.reflect.typeOf
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner
import org.springframework.core.MethodParameter
import org.springframework.core.env.StandardEnvironment
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.StringHttpMessageConverter
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.context.support.StaticWebApplicationContext
import org.springframework.web.method.annotation.RequestParamMethodArgumentResolver
import org.springframework.web.servlet.mvc.method.annotation.PathVariableMethodArgumentResolver
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
import org.springframework.web.servlet.mvc.method.annotation.RequestResponseBodyMethodProcessor

/** Scans all REST endpoints using Spring, returning metadata about all of them */
fun scanEndpoints(
    packageName: String,
    profiles: List<String> = emptyList()
): List<EndpointMetadata> =
    StaticWebApplicationContext().use { ctx ->
        val env = StandardEnvironment()
        env.setActiveProfiles(*profiles.toTypedArray())
        val scanner = ClassPathBeanDefinitionScanner(ctx, true, env)
        scanner.scan(packageName)
        ctx.getEndpointMetadata()
    }

fun ApplicationContext.getEndpointMetadata(): List<EndpointMetadata> =
    RequestMappingHandlerMapping()
        .also { mapping ->
            mapping.applicationContext = this
            mapping.afterPropertiesSet()
        }
        .getEndpointMetadata()

data class EndpointMetadata(
    val controllerClass: KClass<*>,
    val controllerMethod: KFunction<*>,
    val isJsonEndpoint: Boolean,
    val isDeprecated: Boolean,
    val path: String,
    val httpMethod: RequestMethod,
    val pathVariables: List<NamedParameter>,
    val requestParameters: List<NamedParameter>,
    val requestBodyType: KType?,
    val responseBodyType: KType?,
    val authenticatedUserType: KType?,
) {
    fun types(): Sequence<KType> =
        pathVariables.asSequence().map { it.type } +
            requestParameters.asSequence().map { it.type } +
            listOfNotNull(requestBodyType, responseBodyType)

    fun validate() {
        fun fail(reason: String): Nothing =
            error("Invalid $httpMethod endpoint $path in $controllerClass: $reason")

        requestParameters.forEach {
            if (!it.isOptional && it.type.isSubtypeOf(typeOf<Collection<*>>())) {
                fail("Collection request params must be optional (add default to empty list)")
            }
        }

        if (responseBodyType?.isMarkedNullable == true) {
            fail("It must not have a nullable response body")
        }

        when (httpMethod) {
            RequestMethod.GET,
            RequestMethod.HEAD,
            RequestMethod.DELETE, -> {
                if (isJsonEndpoint && httpMethod == RequestMethod.GET && responseBodyType == null) {
                    fail("It should have a response body")
                }
                if (requestBodyType != null) {
                    fail("It should not use a request body")
                }
            }
            RequestMethod.POST,
            RequestMethod.PUT,
            RequestMethod.PATCH -> {}
            RequestMethod.TRACE,
            RequestMethod.OPTIONS -> fail("Method not supported")
        }

        when {
            path.startsWith("/citizen/public/") ||
                path.startsWith("/employee/public/") ||
                path.startsWith("/employee-mobile/public/") ->
                if (authenticatedUserType != null) {
                    fail("It must not include any AuthenticatedUser as a parameter")
                }
            path.startsWith("/system/") ->
                if (authenticatedUserType != typeOf<AuthenticatedUser.SystemInternalUser>()) {
                    fail("It must include an AuthenticatedUser.SystemInternalUser parameter")
                }
            path.startsWith("/integration/") ->
                if (authenticatedUserType != typeOf<AuthenticatedUser.Integration>()) {
                    fail("It must include an AuthenticatedUser.Integration parameter")
                }
            path.startsWith("/citizen/") ->
                if (authenticatedUserType != typeOf<AuthenticatedUser.Citizen>()) {
                    fail("It must include an AuthenticatedUser.Citizen parameter")
                }
            path.startsWith("/employee/") ->
                if (authenticatedUserType != typeOf<AuthenticatedUser.Employee>()) {
                    fail("It must include an AuthenticatedUser.Employee parameter")
                }
            path.startsWith("/employee-mobile/") ->
                if (authenticatedUserType != typeOf<AuthenticatedUser.MobileDevice>()) {
                    fail("It must include an AuthenticatedUser.MobileDevice parameter")
                }
            else ->
                if (!knownLegacyPaths.contains(path)) {
                    fail("It must have a valid prefix (e.g. /citizen/ or /employee/)")
                }
        }
    }
}

data class NamedParameter(val name: String, val type: KType, val isOptional: Boolean) {
    /**
     * Returns a type that represents both the nullability and optionality of the parameter type.
     *
     * This can be different from the actual parameter type, because a Kotlin method can have a
     * non-nullable parameter with a default value. In this case `type` would not be marked nullable
     * but the type returned by `toOptionalType()` would be.
     */
    fun toOptionalType() =
        type.classifier!!.createType(
            type.arguments,
            type.isMarkedNullable || isOptional,
            type.annotations
        )
}

private fun RequestMappingHandlerMapping.getEndpointMetadata(): List<EndpointMetadata> {
    fun KFunction<*>.find(param: MethodParameter): KParameter =
        valueParameters[param.parameterIndex]
    fun <A : Annotation> KFunction<*>.findAnnotatedParameter(
        annotation: KClass<A>,
        getName: (A) -> String?,
        isRequired: (A) -> Boolean,
        param: MethodParameter
    ): NamedParameter {
        val kotlinParam = find(param)
        val paramAnnotation = param.getParameterAnnotation(annotation.java)
        return NamedParameter(
            name = paramAnnotation?.let(getName)?.takeIf { it.isNotBlank() } ?: kotlinParam.name!!,
            type = kotlinParam.type,
            isOptional = !(paramAnnotation?.let(isRequired) ?: true) || kotlinParam.isOptional
        )
    }

    val pathSupport = PathVariableMethodArgumentResolver()
    val paramSupport = RequestParamMethodArgumentResolver(false)
    val bodySupport = RequestResponseBodyMethodProcessor(listOf(StringHttpMessageConverter()))
    return handlerMethods
        .asSequence()
        .flatMap { (info, method) ->
            val controllerClass = method.beanType.kotlin
            val kotlinMethod = controllerClass.functions.find { it.javaMethod == method.method }!!
            val authenticatedUserType =
                kotlinMethod.parameters
                    .singleOrNull { it.type.isSubtypeOf(typeOf<AuthenticatedUser>()) }
                    ?.type
            val pathVariables =
                method.methodParameters
                    .filter { pathSupport.supportsParameter(it) }
                    .mapNotNull { param ->
                        kotlinMethod.findAnnotatedParameter(
                            PathVariable::class,
                            { it.name },
                            { it.required },
                            param
                        )
                    }
            val requestParameters =
                method.methodParameters
                    .filter { paramSupport.supportsParameter(it) }
                    .mapNotNull { param ->
                        kotlinMethod.findAnnotatedParameter(
                            RequestParam::class,
                            { it.name },
                            { it.required },
                            param
                        )
                    }
            val requestBodyType =
                method.methodParameters
                    .firstOrNull { bodySupport.supportsParameter(it) }
                    ?.let { kotlinMethod.find(it).type }
            val responseBodyType =
                if (!method.isVoid && bodySupport.supportsReturnType(method.returnType)) {
                    if (kotlinMethod.returnType.classifier == ResponseEntity::class) {
                        kotlinMethod.returnType.arguments.single().type
                    } else kotlinMethod.returnType
                } else null
            val paths = info.directPaths + info.patternValues
            val methods = info.methodsCondition.methods
            val consumesJson =
                info.consumesCondition.isEmpty ||
                    info.consumesCondition.consumableMediaTypes.contains(MediaType.APPLICATION_JSON)
            val producesJson =
                info.producesCondition.isEmpty ||
                    info.producesCondition.producibleMediaTypes.contains(MediaType.APPLICATION_JSON)
            paths
                .flatMap { path -> methods.map { method -> Pair(path, method) } }
                .map { (path, method) ->
                    EndpointMetadata(
                        controllerClass = controllerClass,
                        controllerMethod = kotlinMethod,
                        isJsonEndpoint =
                            consumesJson && producesJson && responseBodyType != typeOf<Any>(),
                        isDeprecated = kotlinMethod.hasAnnotation<Deprecated>(),
                        path = path,
                        httpMethod = method,
                        pathVariables = pathVariables,
                        requestParameters = requestParameters,
                        requestBodyType = requestBodyType,
                        responseBodyType = responseBodyType,
                        authenticatedUserType = authenticatedUserType,
                    )
                }
        }
        .toList()
}

// Set of legacy endpoint paths that are allowed to fail validation. *DO NOT* add any new endpoints
// here
private val knownLegacyPaths =
    setOf(
        "/absences/by-child/{childId}",
        "/absences/by-child/{childId}/delete",
        "/absences/by-child/{childId}/future",
        "/absences/{groupId}",
        "/absences/{groupId}/delete-holiday-reservations",
        "/absences/{groupId}/present",
        "/areas",
        "/assistance-action-options",
        "/assistance-actions/{id}",
        "/assistance-factors/{id}",
        "/assistance-need-decision/{id}",
        "/assistance-need-decision/{id}/annul",
        "/assistance-need-decision/{id}/decide",
        "/assistance-need-decision/{id}/decision-maker-option",
        "/assistance-need-decision/{id}/mark-as-opened",
        "/assistance-need-decision/{id}/revert-to-unsent",
        "/assistance-need-decision/{id}/send",
        "/assistance-need-decision/{id}/update-decision-maker",
        "/assistance-need-preschool-decisions/{id}",
        "/assistance-need-preschool-decisions/{id}/annul",
        "/assistance-need-preschool-decisions/{id}/decide",
        "/assistance-need-preschool-decisions/{id}/decision-maker",
        "/assistance-need-preschool-decisions/{id}/decision-maker-options",
        "/assistance-need-preschool-decisions/{id}/mark-as-opened",
        "/assistance-need-preschool-decisions/{id}/send",
        "/assistance-need-preschool-decisions/{id}/unsend",
        "/assistance-need-voucher-coefficients/{id}",
        "/attachments/applications/{applicationId}",
        "/attachments/{attachmentId}",
        "/attachments/{attachmentId}/download/{requestedFilename}",
        "/attachments/citizen/applications/{applicationId}",
        "/attachments/citizen/income-statements",
        "/attachments/citizen/income-statements/{incomeStatementId}",
        "/attachments/fee-alteration",
        "/attachments/fee-alteration/{feeAlterationId}",
        "/attachments/income",
        "/attachments/income/{incomeId}",
        "/attachments/income-statements/{incomeStatementId}",
        "/attachments/messages/{draftId}",
        "/attachments/pedagogical-documents/{documentId}",
        "/attendance-reservations",
        "/attendance-reservations/by-child/{childId}/confirmed-range",
        "/attendance-reservations/child-date",
        "/attendance-reservations/child-date/expected-absences",
        "/attendance-reservations/confirmed-days/daily",
        "/attendance-reservations/confirmed-days/stats",
        "/attendances/units/{unitId}/attendances",
        "/attendances/units/{unitId}/children",
        "/attendances/units/{unitId}/children/{childId}/absence-range",
        "/attendances/units/{unitId}/children/{childId}/arrival",
        "/attendances/units/{unitId}/children/{childId}/departure",
        "/attendances/units/{unitId}/children/{childId}/departure/expected-absences",
        "/attendances/units/{unitId}/children/{childId}/full-day-absence",
        "/attendances/units/{unitId}/children/{childId}/return-to-coming",
        "/attendances/units/{unitId}/children/{childId}/return-to-present",
        "/backup-cares/{id}",
        "/backup-pickups/{id}",
        "/calendar-event",
        "/calendar-event/{id}",
        "/calendar-event/{id}/time",
        "/calendar-event/reservation",
        "/calendar-event-time/{id}",
        "/child/{childId}/recipients",
        "/child/{childId}/recipients/{personId}",
        "/child-daily-notes/{noteId}",
        "/child-documents",
        "/child-documents/{documentId}",
        "/child-documents/{documentId}/content",
        "/child-documents/{documentId}/lock",
        "/child-documents/{documentId}/next-status",
        "/child-documents/{documentId}/pdf",
        "/child-documents/{documentId}/prev-status",
        "/child-documents/{documentId}/publish",
        "/child-images/{imageId}",
        "/children/{child}/assistance",
        "/children/{child}/assistance-factors",
        "/children/{child}/daycare-assistances",
        "/children/{childId}",
        "/children/{childId}/additional-information",
        "/children/{childId}/assistance-actions",
        "/children/{childId}/assistance-need-preschool-decisions",
        "/children/{childId}/assistance-needs/decision",
        "/children/{childId}/assistance-needs/decisions",
        "/children/{childId}/assistance-need-voucher-coefficients",
        "/children/{childId}/backup-cares",
        "/children/{childId}/backup-pickups",
        "/children/{childId}/child-daily-notes",
        "/children/{childId}/child-sticky-notes",
        "/children/{childId}/daily-service-times",
        "/children/{childId}/image",
        "/children/{childId}/sensitive-info",
        "/children/{childId}/vasu",
        "/children/{childId}/vasu-summaries",
        "/children/{child}/other-assistance-measures",
        "/children/{child}/preschool-assistances",
        "/child-sticky-notes/{noteId}",
        "/club-terms",
        "/club-terms/{id}",
        "/daily-service-times/{id}",
        "/daily-service-times/{id}/end",
        "/daycare-assistances/{id}",
        "/daycare-groups/{groupId}/group-notes",
        "/daycare-groups/{groupId}/notes",
        "/daycares",
        "/daycares/{daycareId}",
        "/daycares/{daycareId}/acl",
        "/daycares/{daycareId}/backup-cares",
        "/daycares/{daycareId}/earlychildhoodeducationsecretary/{employeeId}",
        "/daycares/{daycareId}/full-acl/{employeeId}",
        "/daycares/{daycareId}/groups",
        "/daycares/{daycareId}/groups/{groupId}",
        "/daycares/{daycareId}/groups/{groupId}/caretakers",
        "/daycares/{daycareId}/groups/{groupId}/caretakers/{id}",
        "/daycares/{daycareId}/notifications",
        "/daycares/{daycareId}/specialeducationteacher/{employeeId}",
        "/daycares/{daycareId}/staff/{employeeId}",
        "/daycares/{daycareId}/staff/{employeeId}/groups",
        "/daycares/{daycareId}/supervisors/{employeeId}",
        "/daycares/features",
        "/daycares/unit-features",
        "/daycares/{unitId}/group-details",
        "/daycares/{unitId}/temporary",
        "/daycares/{unitId}/temporary/{employeeId}",
        "/daycares/{unitId}/temporary/{employeeId}/acl",
        "/decisions2/by-application",
        "/decisions2/by-child",
        "/decisions2/by-guardian",
        "/decisions2/{id}/download",
        "/decisions2/units",
        "/decisions/confirm",
        "/decisions/head-of-family/{id}",
        "/decisions/head-of-family/{id}/create-retroactive",
        "/decisions/{id}",
        "/decisions/ignore",
        "/decisions/mark-sent",
        "/decisions/pdf/{decisionId}",
        "/decisions/search",
        "/decisions/set-type/{id}",
        "/decisions/unignore",
        "/diets",
        "/document-templates",
        "/document-templates/active",
        "/document-templates/import",
        "/document-templates/{templateId}",
        "/document-templates/{templateId}/content",
        "/document-templates/{templateId}/duplicate",
        "/document-templates/{templateId}/export",
        "/document-templates/{templateId}/force-unpublish",
        "/document-templates/{templateId}/publish",
        "/document-templates/{templateId}/validity",
        "/employee",
        "/family/by-adult/{id}",
        "/family/contacts",
        "/family/contacts/priority",
        "/fee-alterations",
        "/fee-alterations/{feeAlterationId}",
        "/fee-decision-generator/generate",
        "/fee-decisions/confirm",
        "/fee-decisions/head-of-family/{id}",
        "/fee-decisions/head-of-family/{id}/create-retroactive",
        "/fee-decisions/{id}",
        "/fee-decisions/ignore",
        "/fee-decisions/mark-sent",
        "/fee-decisions/pdf/{decisionId}",
        "/fee-decisions/search",
        "/fee-decisions/set-type/{id}",
        "/fee-decisions/unignore",
        "/filters/units",
        "/finance-basics/fee-thresholds",
        "/finance-basics/fee-thresholds/{id}",
        "/finance-basics/voucher-values",
        "/finance-basics/voucher-values/{id}",
        "/finance-decisions/selectable-handlers",
        "/foster-parent",
        "/foster-parent/by-child/{childId}",
        "/foster-parent/by-parent/{parentId}",
        "/foster-parent/{id}",
        "/group-notes/{noteId}",
        "/group-placements/{groupPlacementId}",
        "/group-placements/{groupPlacementId}/transfer",
        "/holiday-period",
        "/holiday-period/{id}",
        "/holiday-period/questionnaire",
        "/holiday-period/questionnaire/{id}",
        "/incomes",
        "/incomes/{incomeId}",
        "/incomes/multipliers",
        "/incomes/notifications",
        "/income-statements/awaiting-handler",
        "/income-statements/guardian/{guardianId}/children",
        "/income-statements/{incomeStatementId}/handled",
        "/income-statements/person/{personId}",
        "/income-statements/person/{personId}/{incomeStatementId}",
        "/incomes/types",
        "/invoice-corrections",
        "/invoice-corrections/{id}",
        "/invoice-corrections/{id}/note",
        "/invoice-corrections/{personId}",
        "/invoices/codes",
        "/invoices/create-drafts",
        "/invoices/delete-drafts",
        "/invoices/head-of-family/{id}",
        "/invoices/{id}",
        "/invoices/mark-sent",
        "/invoices/search",
        "/invoices/send",
        "/invoices/send/by-date",
        "/messages/{accountId}",
        "/messages/{accountId}/archived",
        "/messages/{accountId}/copies",
        "/messages/{accountId}/drafts",
        "/messages/{accountId}/drafts/{draftId}",
        "/messages/{accountId}/{messageId}/reply",
        "/messages/{accountId}/preflight-check",
        "/messages/{accountId}/received",
        "/messages/{accountId}/sent",
        "/messages/{accountId}/threads/{threadId}/archive",
        "/messages/{accountId}/threads/{threadId}/read",
        "/messages/{accountId}/thread/{threadId}",
        "/messages/{accountId}/undo-message",
        "/messages/{accountId}/undo-reply",
        "/messages/application/{applicationId}",
        "/messages/mobile/my-accounts/{unitId}",
        "/messages/my-accounts",
        "/messages/receivers",
        "/messages/unread",
        "/messages/unread/{unitId}",
        "/mobile-devices",
        "/mobile-devices/{id}",
        "/mobile-devices/{id}/name",
        "/mobile-devices/personal",
        "/mobile-devices/pin-login",
        "/mobile-devices/push-settings",
        "/mobile-devices/push-subscription",
        "/mobile/realtime-staff-attendances",
        "/mobile/realtime-staff-attendances/arrival",
        "/mobile/realtime-staff-attendances/arrival-external",
        "/mobile/realtime-staff-attendances/departure",
        "/mobile/realtime-staff-attendances/departure-external",
        "/mobile/units/stats",
        "/mobile/units/{unitId}",
        "/note/application/{applicationId}",
        "/note/{noteId}",
        "/note/service-worker/application/{applicationId}",
        "/occupancy/by-unit/{unitId}",
        "/occupancy/by-unit/{unitId}/groups",
        "/occupancy/by-unit/{unitId}/speculated/{applicationId}",
        "/occupancy/units/{unitId}",
        "/other-assistance-measures/{id}",
        "/pairings",
        "/pairings/{id}/response",
        "/parentships",
        "/parentships/{id}",
        "/parentships/{id}/retry",
        "/partnerships",
        "/partnerships/{partnershipId}",
        "/partnerships/{partnershipId}/retry",
        "/patu-report",
        "/payments/create-drafts",
        "/payments/delete-drafts",
        "/payments/search",
        "/payments/send",
        "/pedagogical-document",
        "/pedagogical-document/child/{childId}",
        "/pedagogical-document/{documentId}",
        "/person",
        "/person/blocked-guardians/{personId}",
        "/person/{childId}/evaka-rights",
        "/person/create",
        "/person/dependants/{personId}",
        "/person/details/{personId}",
        "/person/details/ssn",
        "/person/{guardianId}/address-page/download",
        "/person/guardians/{personId}",
        "/person/identity/{personId}",
        "/person/merge",
        "/person/{personId}",
        "/person/{personId}/duplicate",
        "/person/{personId}/ssn",
        "/person/{personId}/ssn/disable",
        "/person/{personId}/vtj-update",
        "/person/search",
        "/placements",
        "/placements/child-placement-periods/{adultId}",
        "/placements/{placementId}",
        "/placements/{placementId}/group-placements",
        "/placements/plans",
        "/preschool-assistances/{id}",
        "/preschool-terms",
        "/preschool-terms/{id}",
        "/public/club-terms",
        "/public/pairings/challenge",
        "/public/pairings/{id}/status",
        "/public/preschool-terms",
        "/public/service-needs/options",
        "/public/units",
        "/public/units/{applicationType}",
        "/reports",
        "/reports/applications",
        "/reports/assistance-need-decisions",
        "/reports/assistance-need-decisions/unread-count",
        "/reports/assistance-needs-and-actions",
        "/reports/assistance-needs-and-actions/by-child",
        "/reports/attendance-reservation/{unitId}",
        "/reports/attendance-reservation/{unitId}/by-child",
        "/reports/child-age-language",
        "/reports/children-in-different-address",
        "/reports/decisions",
        "/reports/duplicate-people",
        "/reports/ended-placements",
        "/reports/exceeded-service-need/rows",
        "/reports/exceeded-service-need/units",
        "/reports/family-conflicts",
        "/reports/family-contacts",
        "/reports/family-daycare-meal-count",
        "/reports/future-preschoolers",
        "/reports/future-preschoolers/groups",
        "/reports/invoices",
        "/reports/manual-duplication",
        "/reports/meal/{unitId}",
        "/reports/missing-head-of-family",
        "/reports/missing-service-need",
        "/reports/non-ssn-children",
        "/reports/occupancy-by-group",
        "/reports/occupancy-by-unit",
        "/reports/partners-in-different-address",
        "/reports/placement-count",
        "/reports/placement-guarantee",
        "/reports/placement-sketching",
        "/reports/presences",
        "/reports/raw",
        "/reports/service-need",
        "/reports/service-voucher-value/units",
        "/reports/service-voucher-value/units/{unitId}",
        "/reports/sextet",
        "/reports/starting-placements",
        "/reports/units",
        "/reports/varda-child-errors",
        "/reports/varda-unit-errors",
        "/service-needs",
        "/service-needs/{id}",
        "/service-needs/options",
        "/settings",
        "/staff-attendances/group/{groupId}",
        "/staff-attendances/realtime",
        "/staff-attendances/realtime/{unitId}/{attendanceId}",
        "/staff-attendances/realtime/{unitId}/external/{attendanceId}",
        "/staff-attendances/realtime/upsert",
        "/staff-attendances/realtime/upsert-external",
        "/staff-attendances/unit/{unitId}",
        "/timeline",
        "/units",
        "/units/{unitId}/calendar-events",
        "/units/{unitId}/groups/{groupId}/discussion-reservation-days",
        "/units/{unitId}/groups/{groupId}/discussion-surveys",
        "/v2/applications",
        "/v2/applications/{applicationId}",
        "/v2/applications/{applicationId}/actions/accept-decision",
        "/v2/applications/{applicationId}/actions/{action}",
        "/v2/applications/{applicationId}/actions/create-placement-plan",
        "/v2/applications/{applicationId}/actions/reject-decision",
        "/v2/applications/{applicationId}/actions/respond-to-placement-proposal",
        "/v2/applications/{applicationId}/actions/send-application",
        "/v2/applications/{applicationId}/decision-drafts",
        "/v2/applications/{applicationId}/placement-draft",
        "/v2/applications/batch/actions/{action}",
        "/v2/applications/by-child/{childId}",
        "/v2/applications/by-guardian/{guardianId}",
        "/v2/applications/placement-proposals/{unitId}/accept",
        "/v2/applications/search",
        "/v2/applications/units/{unitId}",
        "/value-decisions/head-of-family/{headOfFamilyId}",
        "/value-decisions/head-of-family/{id}/create-retroactive",
        "/value-decisions/{id}",
        "/value-decisions/ignore",
        "/value-decisions/mark-sent",
        "/value-decisions/pdf/{decisionId}",
        "/value-decisions/search",
        "/value-decisions/send",
        "/value-decisions/set-type/{id}",
        "/value-decisions/unignore",
        "/varda/child/reset/{childId}",
        "/varda/start-reset",
        "/varda/start-update",
        "/vasu/{id}",
        "/vasu/{id}/update-state",
        "/vasu/templates",
        "/vasu/templates/{id}",
        "/vasu/templates/{id}/content",
        "/vasu/templates/{id}/copy",
        "/vasu/templates/{id}/migrate",
    )
