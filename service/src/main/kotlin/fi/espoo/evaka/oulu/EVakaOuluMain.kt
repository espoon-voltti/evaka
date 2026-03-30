// SPDX-FileCopyrightText: 2021 City of Oulu
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.oulu

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration
import org.springframework.boot.transaction.autoconfigure.TransactionAutoConfiguration

@SpringBootApplication(
    scanBasePackages = ["fi.espoo.evaka.oulu", "fi.espoo.evaka"],
    exclude = [DataSourceAutoConfiguration::class, TransactionAutoConfiguration::class],
)
@ConfigurationPropertiesScan(basePackages = ["fi.espoo.evaka.oulu"])
class EvakaOuluMain

fun main(args: Array<String>) {
    SpringApplicationBuilder().sources(EvakaOuluMain::class.java).profiles("evakaoulu").run(*args)
}
