// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.dvv

import mu.KotlinLogging
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

private val logger = KotlinLogging.logger {}

@Profile("enable_mock_dvv_api")
@RestController
@RequestMapping("/mock-integration/dvv/api")
class MockDvvModificationsService {

    @GetMapping("/v1/kirjausavain/{date}")
    fun getApiKey(@PathVariable("date") date: String?): String {
        logger.info { "Mock dvv GET /kirjausavain/$date called" }
        return "{\"viimeisinKirjausavain\":100000021}"
    }

    @PostMapping("/v1/muutokset")
    fun getModifications(@RequestBody body: ModificationsRequest): String {
        logger.info { "Mock dvv POST /muutokset called, body: $body" }

        val nextToken = body.viimeisinKirjausavain.toInt() + 1
        return """
            {
              "viimeisinKirjausavain": $nextToken,
              "muutokset": [${getModifications(body.hetulista)}],
              "ajanTasalla": ${nextToken > 0}
            }
        """
    }
}

fun getModifications(ssns: List<String>): String {
    return ssns
        .map { ssn -> if (modifications.containsKey(ssn)) modifications[ssn] else null }
        .filterNotNull()
        .joinToString(",")
}

val modifications =
    mapOf<String, String>(
        "nimenmuutos" to
            """
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
}
    """
                .trimIndent(),
        "010179-9992" to
            """
{
  "henkilotunnus": "010179-9992",
  "tietoryhmat": [
    {
      "tietoryhma": "HENKILON_NIMI",
      "muutosattribuutti": "MUUTETTU",
      "alkupv": {
        "arvo": "2019-09-25",
        "tarkkuus": "PAIVA"
      },
      "etunimi": "Uusinimi",
      "sukunimi": "Urkki"
    }
  ],
  "muutospv": "2019-09-24T21:00:00.000Z"
}
    """
                .trimIndent(),
        "020180-999Y" to
            """
{
  "henkilotunnus": "020180-999Y",
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
}
    """
                .trimIndent(),
        "030180-999L" to
            """
{
  "henkilotunnus": "030180-999L",
  "tietoryhmat": [
    {
      "tietoryhma": "TURVAKIELTO",
      "muutosattribuutti": "MUUTETTU",
      "turvaLoppuPv": {
        "arvo": "2030-01-01",
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
    },
    {
      "tietoryhma": "KOTIKUNTA",
      "kuntakoodi": "049",
      "kuntaanMuuttopv": {
        "arvo": "1986-06-02",
        "tarkkuus": "PAIVA"
      },
      "kuntaanMuuttoPv": {
        "arvo": "1986-06-02",
        "tarkkuus": "PAIVA"
      },
      "muutosattribuutti": "LISATIETO"
    }
  ],
  "muutospv": "2019-09-24T21:00:00.000Z"
}
    """
                .trimIndent(),
        "010180-999A" to
            """
{
  "henkilotunnus": "010180-999A",
  "tietoryhmat": [
    {
      "tietoryhma": "KUOLINPAIVA",
      "muutosattribuutti": "LISATTY",
      "kuollut": true,
      "kuolinpv": {
        "arvo": "2019-07-30",
        "tarkkuus": "PAIVA"
      }
    }
  ],
  "muutospv": "2019-09-24T21:00:00.000Z"
}
    """
                .trimIndent(),
        "yksinhuoltaja-muutos" to
            """
{
  "henkilotunnus": "010579-9999",
  "tietoryhmat": [
    {
      "tietoryhma": "HUOLLETTAVA_SUPPEA",
      "huollettava": {
        "henkilotunnus": "010118-999A",
        "etunimet": "Etu",
        "sukunimi": "Suku"
      },
      "huoltajanLaji": "MAARATTY_HUOLTAJA",
      "huoltajanRooli": "AITI",
      "huoltosuhteenAlkupv": {
        "arvo": "2020-09-08",
        "tarkkuus": "PAIVA"
      },
      "huoltosuhteenLoppupv": {
        "arvo": "2036-11-16",
        "tarkkuus": "PAIVA"
      },
      "asuminen": "AIDIN_LUONA",
      "asumisenAlkupv": {
        "arvo": "2020-09-08",
        "tarkkuus": "PAIVA"
      },
      "asumisenLoppupv": {
        "arvo": "2036-11-16",
        "tarkkuus": "PAIVA"
      },
      "oikeudet": [],
      "muutosattribuutti": "LISATTY",
      "lisatytOikeudet": [],
      "poistetutOikeudet": [],
      "muutetutOikeudet": []
    },
    {
      "tietoryhma": "HUOLLETTAVA_SUPPEA",
      "huollettava": {
        "henkilotunnus": "010118-999A",
        "etunimet": "Etu",
        "sukunimi": "Suku"
      },
      "huoltajanLaji": "LAKISAATEINEN_HUOLTAJA",
      "huoltajanRooli": "AITI",
      "huoltosuhteenAlkupv": {
        "arvo": "2018-11-16",
        "tarkkuus": "PAIVA"
      },
      "huoltosuhteenLoppupv": {
        "arvo": "2020-09-08",
        "tarkkuus": "PAIVA"
      },
      "asuminen": "AIDIN_LUONA",
      "asumisenAlkupv": {
        "arvo": "2020-09-08",
        "tarkkuus": "PAIVA"
      },
      "asumisenLoppupv": {
        "arvo": "2036-11-16",
        "tarkkuus": "PAIVA"
      },
      "oikeudet": [],
      "muutosattribuutti": "MUUTETTU",
      "lisatytOikeudet": [],
      "poistetutOikeudet": [],
      "muutetutOikeudet": []
    }
  ],
  "muutospv": "2020-10-01T04:38:04.394Z"
}
    """
                .trimIndent(),
        "huoltaja" to
            """
{
  "henkilotunnus": "010118-9999",
  "tietoryhmat": [
    {
      "tietoryhma": "HUOLTAJA_SUPPEA",
      "huoltaja": {
        "henkilotunnus": "010579-9999",
        "etunimet": "Etu",
        "sukunimi": "Suku"
      },
      "huoltajanLaji": "MAARATTY_HUOLTAJA",
      "huoltajanRooli": "AITI",
      "huoltosuhteenAlkupv": {
        "arvo": "2020-09-08",
        "tarkkuus": "PAIVA"
      },
      "huoltosuhteenLoppupv": {
        "arvo": "2036-11-16",
        "tarkkuus": "PAIVA"
      },
      "asuminen": "AIDIN_LUONA",
      "asumisenAlkupv": {
        "arvo": "2020-09-08",
        "tarkkuus": "PAIVA"
      },
      "asumisenLoppupv": {
        "arvo": "2036-11-16",
        "tarkkuus": "PAIVA"
      },
      "oikeudet": [],
      "muutosattribuutti": "LISATTY",
      "lisatytOikeudet": [],
      "poistetutOikeudet": [],
      "muutetutOikeudet": []
    }
  ],
  "muutospv": "2020-10-01T04:38:04.394Z"
}            
    """
                .trimIndent(),
        "010181-999K" to
            """
{
      "henkilotunnus": "010181-999K",
      "tietoryhmat": [
        {
          "tietoryhma": "HENKILOTUNNUS_KORJAUS",
          "voimassaolo": "AKTIIVI",
          "muutosattribuutti": "LISATTY",
          "muutettuHenkilotunnus": "010281-999C",
          "aktiivinenHenkilotunnus": "010281-999C",
          "edellisetHenkilotunnukset": [
            "010181-999K"
          ]
        },
        {
          "tietoryhma": "HENKILOTUNNUS_KORJAUS",
          "voimassaolo": "PASSIIVI",
          "muutosattribuutti": "MUUTETTU",
          "muutettuHenkilotunnus": "010118-9999",
          "aktiivinenHenkilotunnus": "010281-999C",
          "edellisetHenkilotunnukset": [
            "010118-9999"
          ]
        }
      ],
      "muutospv": "2019-09-24T21:00:00.000Z"
    }
    """
                .trimIndent(),
        "040180-9998" to
            """
{
  "henkilotunnus": "040180-9998",
  "tietoryhmat": [
    {
      "tietoryhma": "VAKINAINEN_KOTIMAINEN_OSOITE",
      "muutosattribuutti": "LISATTY",
      "alkupv": {
        "arvo": "2020-10-01",
        "tarkkuus": "PAIVA"
      },
      "rakennustunnus": "1234567890",
      "katunumero": "17",
      "osoitenumero": 1,
      "huoneistokirjain": "A",
      "huoneistonumero": "002",
      "postinumero": "02940",
      "katunimi": {
        "fi": "Uusitie",
        "sv": "Nyvägen"
      },
      "postitoimipaikka": {
        "fi": "ESPOO",
        "sv": "ESBO"
      }
    },
    {
      "tietoryhma": "VAKINAINEN_KOTIMAINEN_OSOITE",
      "muutosattribuutti": "MUUTETTU",
      "alkupv": {
        "arvo": "2018-03-01",
        "tarkkuus": "PAIVA"
      },
      "loppupv": {
        "arvo": "2020-09-30",
        "tarkkuus": "PAIVA"
      },
      "rakennustunnus": "1234567880",
      "katunumero": "2",
      "osoitenumero": 1,
      "huoneistokirjain": "A",
      "huoneistonumero": "031",
      "postinumero": "02600",
      "katunimi": {
        "fi": "Samatie",
        "sv": "Sammavägen"
      },
      "postitoimipaikka": {
        "fi": "ESPOO",
        "sv": "ESBO"
      }
    },
    {
      "tietoryhma": "VAKINAINEN_KOTIMAINEN_ASUINPAIKKATUNNUS",
      "muutosattribuutti": "LISATTY",
      "rakennustunnus": "123456789V",
      "osoitenumero": 1,
      "huoneistonumero": "033",
      "huoneistokirjain": "B",
      "kuntakoodi": "049",
      "alkupv": {
        "arvo": "2020-10-12",
        "tarkkuus": "PAIVA"
      },
      "asuinpaikantunnus": "123456789V1B033 "
    }    
  ],
  "muutospv": "2020-10-01T04:38:04.394Z"
}
    """
                .trimIndent(),
        "050180-999W" to
            """
{
  "henkilotunnus": "050180-999W",
  "tietoryhmat": [
    {
      "tietoryhma": "HUOLLETTAVA_SUPPEA",
      "huollettava": {
        "henkilotunnus": "050118A999W"
      },
      "huoltajanLaji": "LAKISAATEINEN_HUOLTAJA",
      "huoltajanRooli": "AITI",
      "huoltosuhteenAlkupv": {
        "arvo": "2020-10-01",
        "tarkkuus": "PAIVA"
      },
      "huoltosuhteenLoppupv": {
        "arvo": "2038-10-01",
        "tarkkuus": "PAIVA"
      },
      "oikeudet": [],
      "muutosattribuutti": "LISATTY",
      "lisatytOikeudet": [],
      "poistetutOikeudet": [],
      "muutetutOikeudet": []
    }
  ],
  "muutospv": "2020-09-30T22:01:04.568Z"
}            
    """
                .trimIndent(),
        "060118A999J" to
            """
{
  "henkilotunnus": "060118A999J",
  "tietoryhmat": [
    {
      "tietoryhma": "HUOLTAJA_SUPPEA",
      "huoltaja": {
        "henkilotunnus": "060180-999J"
      },
      "huoltajanLaji": "LAKISAATEINEN_HUOLTAJA",
      "huoltajanRooli": "AITI",
      "huoltosuhteenAlkupv": {
        "arvo": "2020-10-01",
        "tarkkuus": "PAIVA"
      },
      "huoltosuhteenLoppupv": {
        "arvo": "2038-10-01",
        "tarkkuus": "PAIVA"
      },
      "oikeudet": [],
      "muutosattribuutti": "LISATTY",
      "lisatytOikeudet": [],
      "poistetutOikeudet": [],
      "muutetutOikeudet": []
    }
  ],
  "muutospv": "2020-09-30T22:01:04.568Z"
}            
    """
                .trimIndent(),
        "tuntematon_muutos" to
            """
{
  "henkilotunnus": "140921A999X",
  "tietoryhmat": [
    {
      "tietoryhma": "JOKU_MUU",
      "muutosattribuutti": "LISATTY"
    }
  ],
  "muutospv": "2021-09-14T12:01:04.568Z"
}            
    """
                .trimIndent(),
        "010170-123F" to
            """
{
  "henkilotunnus": "010170-123F",
  "tietoryhmat": [
    {
      "tietoryhma": "LAPSI_SUPPEA",
      "muutosattribuutti": "LISATTY",
      "lapsi": {
        "henkilotunnus": "010120A123K",
        "sukupuoli": "PUUTTUU",
        "kansalaisuuskoodi": "247"
      },
      "isaAiti": "ISA",
      "lapsiVanhempiSuhdePaattynyt": false
    }
  ],
  "muutospv": "2021-09-14T12:01:04.568Z"
}
    """
                .trimIndent(),
        "rikkinainen_tietue" to
            """
{
  "henkilotunnus": "rikkinainen_tietue",
  "bogus": [],
  "bogus": "2021-09-14T12:01:04.568Z"
}
    """
                .trimIndent()
    )

data class ModificationsRequest(val viimeisinKirjausavain: String, val hetulista: List<String>)
