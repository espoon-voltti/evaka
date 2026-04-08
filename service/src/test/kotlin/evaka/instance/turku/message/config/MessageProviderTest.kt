// SPDX-FileCopyrightText: 2021 City of Turku
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.turku.message.config

import evaka.core.shared.domain.OfficialLanguage
import evaka.core.shared.message.IMessageProvider
import evaka.instance.turku.TurkuMessageProvider
import evaka.instance.turku.YamlMessageSource
import java.lang.reflect.Method
import kotlin.reflect.full.functions
import kotlin.reflect.full.valueParameters
import kotlin.reflect.jvm.javaMethod
import org.assertj.core.api.Assertions.assertThat
import org.junitpioneer.jupiter.cartesian.ArgumentSets
import org.junitpioneer.jupiter.cartesian.CartesianTest
import org.springframework.core.io.ClassPathResource

internal class MessageProviderTest {
    private val messageProvider: IMessageProvider =
        TurkuMessageProvider(YamlMessageSource(ClassPathResource("turku/messages.yaml")))

    @CartesianTest()
    @CartesianTest.MethodFactory("methodsWithLang")
    fun `get works for every message type and language`(method: Method, lang: OfficialLanguage) {
        assertThat(((method.invoke(messageProvider, lang)) as String).also(::println))
            .isNotBlank
            .doesNotContainIgnoringCase("espoo")
            .doesNotContainIgnoringCase("esbo")
    }

    companion object {
        @JvmStatic
        fun methodsWithLang(): ArgumentSets {
            val allMethods =
                IMessageProvider::class
                    .functions
                    .filter {
                        it.valueParameters.map { param -> param.type.classifier } ==
                            listOf(OfficialLanguage::class)
                    }
                    .filter { it.returnType.classifier == String::class }
                    .map { it.javaMethod }
            return ArgumentSets.create()
                .argumentsForNextParameter(allMethods)
                .argumentsForNextParameter(OfficialLanguage.entries)
        }
    }
}
