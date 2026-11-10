// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.shared.dev

import evaka.core.daycare.domain.Language
import evaka.core.emailclient.EmailContent
import evaka.core.emailclient.IEmailMessageProvider
import java.lang.reflect.Proxy
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

class DevEmailPreviewTest {
    private val employeeOnlyEmails =
        setOf("messageDeletionSenderEmail", "messageDeletionNotificationEmail")

    @Test
    fun `every citizen email has a preview`() {
        val calledMethods = mutableSetOf<String>()
        val provider =
            Proxy.newProxyInstance(
                IEmailMessageProvider::class.java.classLoader,
                arrayOf(IEmailMessageProvider::class.java),
            ) { _, method, _ ->
                calledMethods += method.name
                EmailContent(subject = "", text = "", html = "")
            } as IEmailMessageProvider

        EmailMessageFilter.entries.forEach {
            val _ = emailPreviewContent(provider, it, Language.fi)
        }

        val citizenEmails =
            IEmailMessageProvider::class
                .java
                .methods
                .filterNot { it.isSynthetic }
                .map { it.name }
                .toSet() - employeeOnlyEmails
        assertEquals(emptySet(), citizenEmails - calledMethods)
    }
}
