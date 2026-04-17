// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration
import org.springframework.boot.transaction.autoconfigure.TransactionAutoConfiguration
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration

@SpringBootApplication(
    scanBasePackages = ["evaka.core"],
    exclude = [DataSourceAutoConfiguration::class, TransactionAutoConfiguration::class],
)
class Main

fun main(args: Array<String>) {
    val municipality = System.getenv("EVAKA_MUNICIPALITY") ?: "espoo"
    val municipalityProfile = "${municipality}_evaka"

    val municipalityConfig =
        when (municipality) {
            "espoo" -> EspooInstance::class.java
            "oulu" -> OuluInstance::class.java
            "turku" -> TurkuInstance::class.java
            "tampere" -> TampereInstance::class.java
            "vesilahti" -> VesilahtiInstance::class.java
            "hameenkyro" -> HameenkyroInstance::class.java
            "ylojarvi" -> YlojarviInstance::class.java
            "pirkkala" -> PirkkalaInstance::class.java
            "nokia" -> NokiaInstance::class.java
            "kangasala" -> KangasalaInstance::class.java
            "lempaala" -> LempaalaInstance::class.java
            "orivesi" -> OrivesiInstance::class.java
            else -> error("Unknown municipality: $municipality")
        }

    val profiles =
        when (System.getenv("VOLTTI_ENV")) {
            "dev",
            "test" -> arrayOf(municipalityProfile, "enable_dev_api")

            else -> arrayOf(municipalityProfile)
        }

    SpringApplicationBuilder()
        .profiles(*profiles)
        .sources(Main::class.java, municipalityConfig)
        .run(*args)
}

@Configuration
@ComponentScan("evaka.instance.espoo")
@ConfigurationPropertiesScan(basePackages = ["evaka.instance.espoo"])
class EspooInstance

@Configuration
@ComponentScan("evaka.instance.oulu")
@ConfigurationPropertiesScan(basePackages = ["evaka.instance.oulu"])
class OuluInstance

@Configuration
@ComponentScan("evaka.instance.turku")
@ConfigurationPropertiesScan(basePackages = ["evaka.instance.turku"])
class TurkuInstance

@Configuration
@ComponentScan("evaka.trevaka", "evaka.instance.tampere")
@ConfigurationPropertiesScan(basePackages = ["evaka.trevaka", "evaka.instance.tampere"])
class TampereInstance

@Configuration
@ComponentScan("evaka.trevaka", "evaka.instance.vesilahti")
@ConfigurationPropertiesScan(basePackages = ["evaka.trevaka", "evaka.instance.vesilahti"])
class VesilahtiInstance

@Configuration
@ComponentScan("evaka.trevaka", "evaka.instance.hameenkyro")
@ConfigurationPropertiesScan(basePackages = ["evaka.trevaka", "evaka.instance.hameenkyro"])
class HameenkyroInstance

@Configuration
@ComponentScan("evaka.trevaka", "evaka.instance.ylojarvi")
@ConfigurationPropertiesScan(basePackages = ["evaka.trevaka", "evaka.instance.ylojarvi"])
class YlojarviInstance

@Configuration
@ComponentScan("evaka.trevaka", "evaka.instance.pirkkala")
@ConfigurationPropertiesScan(basePackages = ["evaka.trevaka", "evaka.instance.pirkkala"])
class PirkkalaInstance

@Configuration
@ComponentScan("evaka.trevaka", "evaka.instance.nokia")
@ConfigurationPropertiesScan(basePackages = ["evaka.trevaka", "evaka.instance.nokia"])
class NokiaInstance

@Configuration
@ComponentScan("evaka.trevaka", "evaka.instance.kangasala")
@ConfigurationPropertiesScan(basePackages = ["evaka.trevaka", "evaka.instance.kangasala"])
class KangasalaInstance

@Configuration
@ComponentScan("evaka.trevaka", "evaka.instance.lempaala")
@ConfigurationPropertiesScan(basePackages = ["evaka.trevaka", "evaka.instance.lempaala"])
class LempaalaInstance

@Configuration
@ComponentScan("evaka.trevaka", "evaka.instance.orivesi")
@ConfigurationPropertiesScan(basePackages = ["evaka.trevaka", "evaka.instance.orivesi"])
class OrivesiInstance
