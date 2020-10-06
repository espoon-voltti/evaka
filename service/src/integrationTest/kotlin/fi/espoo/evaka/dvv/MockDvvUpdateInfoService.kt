// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.dvv

import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.springframework.context.annotation.Profile
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.annotation.PostConstruct

private val logger = KotlinLogging.logger {}

@Profile("enable_mock_dvv_api")
@RestController
@RequestMapping("/mock-integration/dvv/api")
class MockDvvUpdateInfoService(private val mapper: ObjectMapper) {

    @PostConstruct
    fun jee() {
        println("DDEBUG ALIVE")
    }

    @GetMapping("/v1/kirjausavain/{date}")
    fun getApiKey(@PathVariable("date") date: String?): ResponseEntity<String> {
        logger.info { "Mock dvv GET /kirjausavain/$date called" }
        return ResponseEntity.ok("{\"viimeisinKirjausavain\":100000021}")
    }

    @PostMapping("/v1/muutokset")
    fun getUpdateInfo(
        @RequestBody body: UpdateInfoRequest
    ): ResponseEntity<String> {
        logger.info { "Mock dvv POST /muutokset called, body: $body" }
        return ResponseEntity.ok(
            """
            {
              "viimeisinKirjausavain": 100000021,
              "muutokset": [${getUpdateInfos(body.hetulista)}],
              "ajanTasalla": true
            }
        """
        )
    }
}

fun getUpdateInfos(ssns: List<String>): String {
    return ssns.map { ssn -> if (updateInfos.containsKey(ssn)) updateInfos[ssn] else null }.filter { it != null }.joinToString(",")
}

val updateInfos = mapOf<String, String>(
"010579-9999" to """
{
  "henkilotunnus": "010579-9999",
  "tietoryhmat": [
    {
      "tietoryhma": "HENKILON_NIMI",
      "muutosattribuutti": "MUUTETTU",
      "alkupv": {
        "arvo": "2019-09-25",
        "tarkkuus": "PAIVA"
      },
      "etunimi": "Etunimi5_muutos",
      "sukunimi": "Sukunimi5"
    },
    {
      "tietoryhma": "NIMENMUUTOS",
      "muutosattribuutti": "LISATTY",
      "nimilaji": "NYKYINEN_ETUNIMI",
      "nimi": "Etunimi5_muutos",
      "alkupv": {
        "arvo": "2019-09-25",
        "tarkkuus": "PAIVA"
      }
    },
    {
      "tietoryhma": "NIMENMUUTOS",
      "muutosattribuutti": "MUUTETTU",
      "nimilaji": "EDELLINEN_ETUNIMI",
      "nimi": "Etunimi5",
      "alkupv": {
        "arvo": "2019-09-25",
        "tarkkuus": "PAIVA"
      },
      "loppupv": {
        "arvo": "2019-09-25",
        "tarkkuus": "PAIVA"
      }
    },
    {
      "tietoryhma": "NIMENMUUTOS_LAAJA",
      "muutosattribuutti": "LISATTY",
      "nimilaji": "NYKYINEN_ETUNIMI",
      "nimi": "Etunimi5_muutos",
      "alkupv": {
        "arvo": "2019-09-25",
        "tarkkuus": "PAIVA"
      }
    },
    {
      "tietoryhma": "NIMENMUUTOS_LAAJA",
      "muutosattribuutti": "MUUTETTU",
      "nimilaji": "EDELLINEN_ETUNIMI",
      "nimi": "Etunimi5",
      "alkupv": {
        "arvo": "2019-09-25",
        "tarkkuus": "PAIVA"
      },
      "loppupv": {
        "arvo": "2019-09-25",
        "tarkkuus": "PAIVA"
      }
    }
  ],
  "muutospv": "2019-09-24T21:00:00.000Z"
}""".trimIndent(),
"010180-9999" to """
{
  "henkilotunnus": "010180-9999",
  "tietoryhmat": [
    {
      "tietoryhma": "HENKILON_NIMI",
      "muutosattribuutti": "MUUTETTU",
      "alkupv": {
        "arvo": "2019-09-25",
        "tarkkuus": "PAIVA"
      },
      "etunimi": "Etunimi5_muutos",
      "sukunimi": "Sukunimi5"
    }
  ],
  "muutospv": "2019-09-24T21:00:00.000Z"
}""".trimIndent(),
"turvakielto-lisatty" to """
{
  "henkilotunnus": "010579-9999",
  "tietoryhmat": [
    {
      "tietoryhma": "TURVAKIELTO",
      "muutosattribuutti": "LISATTY",
      "turvakieltoAktiivinen": true
    },
    {
      "tietoryhma": "VAKINAINEN_KOTIMAINEN_OSOITE",
      "turvakiellonAlaisetKentat": [
        "katunimi",
        "katunumero",
        "huoneistokirjain",
        "huoneistonumero",
        "jakokirjain",
        "postinumero",
        "postitoimipaikka",
        "rakennustunnus",
        "osoitenumero"
      ],
      "muutosattribuutti": "MUUTETTU"
    }
  ],
  "muutospv": "2019-09-24T21:00:00.000Z"
}""".trimIndent(),
"turvakielto-poistettu" to """
{
  "henkilotunnus": "010579-9999",
  "tietoryhmat": [
    {
      "tietoryhma": "TURVAKIELTO",
      "muutosattribuutti": "MUUTETTU",
      "turvaLoppuPv": {
        "arvo": "2019-09-25",
        "tarkkuus": "PAIVA"
      },
      "turvakieltoAktiivinen": false
    },
    {
      "tietoryhma": "VAKINAINEN_KOTIMAINEN_OSOITE",
      "katunimi": {
        "fi": "Vanhakatu",
        "sv": "Gamlagatan"
      },
      "katunumero": "10h5",
      "huoneistonumero": "003",
      "postinumero": "02230",
      "postitoimipaikka": {
        "fi": "Espoo",
        "sv": "Esbo"
      },
      "rakennustunnus": "1234567890",
      "osoitenumero": 1,
      "alkupv": {
        "arvo": "1986-06-02",
        "tarkkuus": "PAIVA"
      },
      "loppupv": {
        "arvo": "2999-02-28",
        "tarkkuus": "PAIVA"
      },
      "muutosattribuutti": "MUUTETTU"
    }
  ],
  "muutospv": "2019-09-24T21:00:00.000Z"
}    
""".trimIndent()


)

data class UpdateInfoRequest(
    val viimeisinKirjausavain: String,
    val tuotekoodi: String,
    val hetulista: List<String>
)
