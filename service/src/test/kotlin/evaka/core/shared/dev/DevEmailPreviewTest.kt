// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.shared.dev

import evaka.core.daycare.domain.Language
import evaka.core.emailclient.EmailContent
import evaka.core.emailclient.IEmailMessageProvider
import evaka.core.emailclient.MessageThreadData
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.jvmErasure
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

class DevEmailPreviewTest {
    private val employeeOnlyEmails =
        setOf("messageDeletionSenderEmail", "messageDeletionNotificationEmail")

    private val propertiesNotAffectingContent: Set<KProperty1<*, *>> =
        setOf(
            MessageThreadData::sensitive,
            MessageThreadData::isCopy,
            MessageThreadData::senderType,
        )

    private val citizenEmailMethods =
        IEmailMessageProvider::class.java.methods.filter {
            !it.isSynthetic && it.name !in employeeOnlyEmails
        }

    private fun previewCalls(): List<Pair<Method, Array<Any?>>> {
        val calls = mutableListOf<Pair<Method, Array<Any?>>>()
        val provider =
            Proxy.newProxyInstance(
                IEmailMessageProvider::class.java.classLoader,
                arrayOf(IEmailMessageProvider::class.java),
            ) { _, method, args ->
                calls += method to (args ?: emptyArray())
                EmailContent(subject = "", text = "", html = "")
            } as IEmailMessageProvider

        EmailMessageFilter.entries.forEach {
            val _ = emailPreviewContent(provider, it, Language.fi)
        }
        return calls
    }

    private fun variantsOf(type: Class<*>): List<Any> =
        when {
            type == Language::class.java -> emptyList()
            type == Boolean::class.java -> listOf(true, false)
            type.isEnum -> type.enumConstants.toList()
            else -> emptyList()
        }

    @Test
    fun `every citizen email has a preview`() {
        val calledMethods = previewCalls().map { it.first.name }.toSet()

        assertEquals(emptySet(), citizenEmailMethods.map { it.name }.toSet() - calledMethods)
    }

    @Test
    fun `every enum and boolean variant of a citizen email has a preview`() {
        val calls = previewCalls()

        val missing = citizenEmailMethods.flatMap { method ->
            val args = calls.filter { it.first == method }.map { it.second }
            if (args.isEmpty()) return@flatMap emptyList()
            method.parameterTypes.withIndex().flatMap { (index, type) ->
                val values = args.map { it[index] }
                val missingParameterVariants =
                    (variantsOf(type) - values.toSet()).map {
                        "${method.name}: ${type.simpleName}.$it"
                    }
                val missingPropertyVariants =
                    if (type.kotlin.isData) {
                        type.kotlin.memberProperties
                            .filter { it !in propertiesNotAffectingContent }
                            .flatMap { property ->
                                val propertyValues =
                                    values.filterNotNull().map { property.getter.call(it) }.toSet()
                                (variantsOf(property.returnType.jvmErasure.java) - propertyValues)
                                    .map {
                                        "${method.name}: ${type.simpleName}.${property.name} = $it"
                                    }
                            }
                    } else {
                        emptyList()
                    }
                missingParameterVariants + missingPropertyVariants
            }
        }
        assertEquals(emptyList(), missing)
    }
}
