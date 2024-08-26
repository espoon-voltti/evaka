// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.koski

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.KoskiEnv
import fi.espoo.evaka.OphEnv
import fi.espoo.evaka.defaultMunicipalOrganizerOid
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.testArea
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testChild_2
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDaycare2
import java.time.LocalDate
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import org.springframework.beans.factory.annotation.Autowired

class KoskiPayloadIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var koskiEndpoint: MockKoskiEndpoint
    private lateinit var koskiTester: KoskiTester

    private val preschoolTerm2019 =
        FiniteDateRange(LocalDate.of(2019, 8, 8), LocalDate.of(2020, 5, 29))

    @BeforeAll
    fun initDependencies() {
        koskiTester =
            KoskiTester(
                db,
                KoskiClient(
                    KoskiEnv.fromEnvironment(env)
                        .copy(url = "http://localhost:$httpPort/public/mock-koski"),
                    OphEnv.fromEnvironment(env),
                    asyncJobRunner = null,
                ),
            )
    }

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insert(testArea)
            tx.insert(testDaycare)
            tx.insert(testDaycare2)
            listOf(testChild_1, testChild_2).forEach { tx.insert(it, DevPersonType.CHILD) }
            tx.setUnitOids()
        }
        koskiEndpoint.clearData()
    }

    @Test
    fun `simple preschool placement in 2019`() {
        db.transaction {
            it.insert(
                DevPlacement(
                    childId = testChild_1.id,
                    unitId = testDaycare.id,
                    startDate = preschoolTerm2019.start,
                    endDate = preschoolTerm2019.end,
                    type = PlacementType.PRESCHOOL,
                )
            )
        }

        koskiTester.triggerUploads(today = preschoolTerm2019.end.plusDays(1))

        val stored = db.read { it.getStoredResults() }.single()
        val expected =
            """
            {
                "henkilö": {
                    "hetu": "010617A123U",
                    "etunimet": "Ricky",
                    "sukunimi": "Doe"
                },
                "opiskeluoikeudet": [
                    {
                        "tila": {
                            "opiskeluoikeusjaksot": [
                                {
                                    "alku": "2019-08-08",
                                    "tila": {
                                        "koodiarvo": "lasna",
                                        "koodistoUri": "koskiopiskeluoikeudentila"
                                    }
                                },
                                {
                                    "alku": "2020-05-29",
                                    "tila": {
                                        "koodiarvo": "valmistunut",
                                        "koodistoUri": "koskiopiskeluoikeudentila"
                                    }
                                }
                            ]
                        },
                        "suoritukset": [
                            {
                                "koulutusmoduuli": {
                                    "perusteenDiaarinumero": "102/011/2014",
                                    "tunniste": {
                                        "koodiarvo": "001102",
                                        "koodistoUri": "koulutus"
                                    }
                                },
                                "toimipiste": {
                                    "oid": "1.2.246.562.10.1111111111"
                                },
                                "suorituskieli": {
                                    "koodiarvo": "FI",
                                    "koodistoUri": "kieli"
                                },
                                "tyyppi": {
                                    "koodiarvo": "esiopetuksensuoritus",
                                    "koodistoUri": "suorituksentyyppi"
                                },
                                "vahvistus": {
                                    "päivä":"2020-05-29",
                                    "paikkakunta":{"koodiarvo":"049","koodistoUri":"kunta"},
                                    "myöntäjäOrganisaatio":{"oid":$defaultMunicipalOrganizerOid},
                                    "myöntäjäHenkilöt":[{
                                        "nimi":"Unit Manager",
                                        "titteli":{"fi":"Esiopetusyksikön johtaja"},
                                        "organisaatio":{"oid":$defaultMunicipalOrganizerOid}
                                    }]
                                },
                                "osasuoritukset": null
                            }
                        ],                        
                        "tyyppi": {
                            "koodiarvo": "esiopetus",
                            "koodistoUri": "opiskeluoikeudentyyppi"
                        },
                        "lähdejärjestelmänId":{
                            "id": "${stored.id}",
                            "lähdejärjestelmä":{
                                "koodiarvo": "TestSystemCode",
                                "koodistoUri": "lahdejarjestelma"
                            }
                        },
                        "oid": "${stored.studyRightOid}",
                        "lisätiedot": null
                    }
                ]
            }
            """
                .trimIndent()
        JSONAssert.assertEquals(expected, stored.payload, JSONCompareMode.STRICT)
    }

    @Test
    fun `child with two preschool placements in the past`() {
        db.transaction {
            it.insert(
                DevPlacement(
                    childId = testChild_1.id,
                    unitId = testDaycare.id,
                    startDate = LocalDate.of(2018, 8, 1),
                    endDate = LocalDate.of(2018, 12, 31),
                    type = PlacementType.PRESCHOOL,
                )
            )
            it.insert(
                DevPlacement(
                    childId = testChild_1.id,
                    unitId = testDaycare2.id,
                    startDate = LocalDate.of(2019, 1, 1),
                    endDate = LocalDate.of(2019, 5, 31),
                    type = PlacementType.PRESCHOOL,
                )
            )
        }

        koskiTester.triggerUploads(today = preschoolTerm2019.end.plusDays(1))
        val stored =
            db.read { it.getStoredResults() }
                .let { studyRights ->
                    Pair(
                        studyRights.single { it.unitId == testDaycare.id },
                        studyRights.single { it.unitId == testDaycare2.id },
                    )
                }

        val expected0 =
            """
               
                {
                    "henkilö": {
                        "hetu": "010617A123U",
                        "etunimet": "Ricky",
                        "sukunimi": "Doe"
                    },
                    "opiskeluoikeudet": [
                        {
                            "tila": {
                                "opiskeluoikeusjaksot": [
                                    {
                                        "alku": "2018-08-01",
                                        "tila": {
                                            "koodiarvo": "lasna",
                                            "koodistoUri": "koskiopiskeluoikeudentila"
                                        }
                                    },
                                    {
                                        "alku": "2018-12-31",
                                        "tila": {
                                            "koodiarvo": "eronnut",
                                            "koodistoUri": "koskiopiskeluoikeudentila"
                                        }
                                    }
                                ]
                            },
                            "suoritukset": [
                                {
                                    "koulutusmoduuli": {
                                        "perusteenDiaarinumero": "102/011/2014",
                                        "tunniste": {
                                            "koodiarvo": "001102",
                                            "koodistoUri": "koulutus"
                                        }
                                    },
                                    "toimipiste": {
                                        "oid": "1.2.246.562.10.1111111111"
                                    },
                                    "suorituskieli": {
                                        "koodiarvo": "FI",
                                        "koodistoUri": "kieli"
                                    },
                                    "tyyppi": {
                                        "koodiarvo": "esiopetuksensuoritus",
                                        "koodistoUri": "suorituksentyyppi"
                                    },
                                    "vahvistus": null,
                                    "osasuoritukset": null
                                }
                            ],
                            "tyyppi": {
                                "koodiarvo": "esiopetus",
                                "koodistoUri": "opiskeluoikeudentyyppi"
                            },
                            "lähdejärjestelmänId":{
                                "id": "${stored.first.id}",
                                "lähdejärjestelmä":{
                                    "koodiarvo": "TestSystemCode",
                                    "koodistoUri": "lahdejarjestelma"
                                }
                            },
                            "oid": "${stored.first.studyRightOid}",
                            "lisätiedot": null
                        }
                    ]
                }"""
        JSONAssert.assertEquals(expected0, stored.first.payload, JSONCompareMode.STRICT)

        val expected1 =
            """
                {
                    "henkilö": {
                        "hetu": "010617A123U",
                        "etunimet": "Ricky",
                        "sukunimi": "Doe"
                    },
                    "opiskeluoikeudet": [
                        {
                            "tila": {
                                "opiskeluoikeusjaksot": [
                                    {
                                        "alku": "2019-01-01",
                                        "tila": {
                                            "koodiarvo": "lasna",
                                            "koodistoUri": "koskiopiskeluoikeudentila"
                                        }
                                    },
                                    {
                                        "alku": "2019-05-31",
                                        "tila": {
                                            "koodiarvo": "valmistunut",
                                            "koodistoUri": "koskiopiskeluoikeudentila"
                                        }
                                    }
                                ]
                            },
                            "suoritukset": [
                                {
                                    "koulutusmoduuli": {
                                        "perusteenDiaarinumero": "102/011/2014",
                                        "tunniste": {
                                            "koodiarvo": "001102",
                                            "koodistoUri": "koulutus"
                                        }
                                    },
                                    "toimipiste": {
                                        "oid": "1.2.246.562.10.2222222222"
                                    },
                                    "suorituskieli": {
                                        "koodiarvo": "FI",
                                        "koodistoUri": "kieli"
                                    },
                                    "tyyppi": {
                                        "koodiarvo": "esiopetuksensuoritus",
                                        "koodistoUri": "suorituksentyyppi"
                                    },                                    
                                    "vahvistus": {
                                        "päivä":"2019-05-31",
                                        "paikkakunta":{"koodiarvo":"049","koodistoUri":"kunta"},
                                        "myöntäjäOrganisaatio":{"oid": $defaultMunicipalOrganizerOid},
                                        "myöntäjäHenkilöt":[{
                                            "nimi":"Unit Manager",
                                            "titteli":{"fi":"Esiopetusyksikön johtaja"},
                                            "organisaatio":{"oid":"1.2.3.4.5"}
                                        }]
                                    },
                                    "osasuoritukset": null
                                }
                            ],
                            "tyyppi": {
                                "koodiarvo": "esiopetus",
                                "koodistoUri": "opiskeluoikeudentyyppi"
                            },
                            "lähdejärjestelmänId":{
                                "id": "${stored.second.id}",
                                "lähdejärjestelmä":{
                                    "koodiarvo": "TestSystemCode",
                                    "koodistoUri": "lahdejarjestelma"
                                }
                            },
                            "oid": "${stored.second.studyRightOid}",
                            "lisätiedot": null
                        }
                    ]
                }
        """
        JSONAssert.assertEquals(expected1, stored.second.payload, JSONCompareMode.STRICT)
    }

    @Test
    fun `two children with placements in the past`() {
        db.transaction { tx ->
            listOf(
                    DevPlacement(
                        childId = testChild_2.id,
                        unitId = testDaycare.id,
                        startDate = LocalDate.of(2018, 8, 1),
                        endDate = LocalDate.of(2019, 5, 31),
                        type = PlacementType.PRESCHOOL,
                    ),
                    DevPlacement(
                        childId = testChild_1.id,
                        unitId = testDaycare.id,
                        startDate = LocalDate.of(2018, 8, 2),
                        endDate = LocalDate.of(2018, 12, 31),
                        type = PlacementType.PRESCHOOL,
                    ),
                    DevPlacement(
                        childId = testChild_1.id,
                        unitId = testDaycare2.id,
                        startDate = LocalDate.of(2019, 1, 1),
                        endDate = LocalDate.of(2019, 5, 31),
                        type = PlacementType.PRESCHOOL,
                    ),
                )
                .forEach { tx.insert(it) }
        }

        koskiTester.triggerUploads(today = preschoolTerm2019.end.plusDays(1))
        val stored =
            db.read { it.getStoredResults() }
                .let { studyRights ->
                    Triple(
                        studyRights.single { it.childId == testChild_2.id },
                        studyRights.single {
                            it.childId == testChild_1.id && it.unitId == testDaycare.id
                        },
                        studyRights.single {
                            it.childId == testChild_1.id && it.unitId == testDaycare2.id
                        },
                    )
                }

        val expected0 =
            """
                {
                    "henkilö": {
                        "hetu": "010316A1235",
                        "etunimet": "Micky",
                        "sukunimi": "Doe"
                    },
                    "opiskeluoikeudet": [
                        {
                            "tila": {
                                "opiskeluoikeusjaksot": [
                                    {
                                        "alku": "2018-08-01",
                                        "tila": {
                                            "koodiarvo": "lasna",
                                            "koodistoUri": "koskiopiskeluoikeudentila"
                                        }
                                    },
                                    {
                                        "alku": "2019-05-31",
                                        "tila": {
                                            "koodiarvo": "valmistunut",
                                            "koodistoUri": "koskiopiskeluoikeudentila"
                                        }
                                    }
                                ]
                            },
                            "suoritukset": [
                                {
                                    "koulutusmoduuli": {
                                        "perusteenDiaarinumero": "102/011/2014",
                                        "tunniste": {
                                            "koodiarvo": "001102",
                                            "koodistoUri": "koulutus"
                                        }
                                    },
                                    "toimipiste": {
                                        "oid": "1.2.246.562.10.1111111111"
                                    },
                                    "suorituskieli": {
                                        "koodiarvo": "FI",
                                        "koodistoUri": "kieli"
                                    },
                                    "tyyppi": {
                                        "koodiarvo": "esiopetuksensuoritus",
                                        "koodistoUri": "suorituksentyyppi"
                                    },                                    
                                    "vahvistus": {
                                        "päivä":"2019-05-31",
                                        "paikkakunta":{"koodiarvo":"049","koodistoUri":"kunta"},
                                        "myöntäjäOrganisaatio":{"oid":$defaultMunicipalOrganizerOid},
                                        "myöntäjäHenkilöt":[{
                                            "nimi":"Unit Manager",
                                            "titteli":{"fi":"Esiopetusyksikön johtaja"},
                                            "organisaatio":{"oid":"$defaultMunicipalOrganizerOid"}
                                        }]
                                    },
                                    "osasuoritukset": null
                                }
                            ],
                            "tyyppi": {
                                "koodiarvo": "esiopetus",
                                "koodistoUri": "opiskeluoikeudentyyppi"
                            },
                            "lähdejärjestelmänId":{
                                "id": "${stored.first.id}",
                                "lähdejärjestelmä":{
                                    "koodiarvo": "TestSystemCode",
                                    "koodistoUri": "lahdejarjestelma"
                                }
                            },
                            "oid": "${stored.first.studyRightOid}",
                            "lisätiedot": null
                        }
                    ]
                }"""
        JSONAssert.assertEquals(expected0, stored.first.payload, JSONCompareMode.STRICT)

        val expected1 =
            """  {
                    "henkilö": {
                        "hetu": "010617A123U",
                        "etunimet": "Ricky",
                        "sukunimi": "Doe"
                    },
                    "opiskeluoikeudet": [
                        {
                            "tila": {
                                "opiskeluoikeusjaksot": [
                                    {
                                        "alku": "2018-08-02",
                                        "tila": {
                                            "koodiarvo": "lasna",
                                            "koodistoUri": "koskiopiskeluoikeudentila"
                                        }
                                    },
                                    {
                                        "alku": "2018-12-31",
                                        "tila": {
                                            "koodiarvo": "eronnut",
                                            "koodistoUri": "koskiopiskeluoikeudentila"
                                        }
                                    }
                                ]
                            },
                            "suoritukset": [
                                {
                                    "koulutusmoduuli": {
                                        "perusteenDiaarinumero": "102/011/2014",
                                        "tunniste": {
                                            "koodiarvo": "001102",
                                            "koodistoUri": "koulutus"
                                        }
                                    },
                                    "toimipiste": {
                                        "oid": "1.2.246.562.10.1111111111"
                                    },
                                    "suorituskieli": {
                                        "koodiarvo": "FI",
                                        "koodistoUri": "kieli"
                                    },
                                    "tyyppi": {
                                        "koodiarvo": "esiopetuksensuoritus",
                                        "koodistoUri": "suorituksentyyppi"
                                    },
                                    "vahvistus": null,
                                    "osasuoritukset": null
                                }
                            ],
                            "tyyppi": {
                                "koodiarvo": "esiopetus",
                                "koodistoUri": "opiskeluoikeudentyyppi"
                            },
                            "lähdejärjestelmänId":{
                                "id": "${stored.second.id}",
                                "lähdejärjestelmä":{
                                    "koodiarvo": "TestSystemCode",
                                    "koodistoUri": "lahdejarjestelma"
                                }
                            },
                            "oid": "${stored.second.studyRightOid}",
                            "lisätiedot": null
                        }
                    ]
                }
            """
        JSONAssert.assertEquals(expected1, stored.second.payload, JSONCompareMode.STRICT)

        val expected2 =
            """
                {
                    "henkilö": {
                        "hetu": "010617A123U",
                        "etunimet": "Ricky",
                        "sukunimi": "Doe"
                    },
                    "opiskeluoikeudet": [
                        {
                            "tila": {
                                "opiskeluoikeusjaksot": [
                                    {
                                        "alku": "2019-01-01",
                                        "tila": {
                                            "koodiarvo": "lasna",
                                            "koodistoUri": "koskiopiskeluoikeudentila"
                                        }
                                    },
                                    {
                                        "alku": "2019-05-31",
                                        "tila": {
                                            "koodiarvo": "valmistunut",
                                            "koodistoUri": "koskiopiskeluoikeudentila"
                                        }
                                    }
                                ]
                            },
                            "suoritukset": [
                                {
                                    "koulutusmoduuli": {
                                        "perusteenDiaarinumero": "102/011/2014",
                                        "tunniste": {
                                            "koodiarvo": "001102",
                                            "koodistoUri": "koulutus"
                                        }
                                    },
                                    "toimipiste": {
                                        "oid": "1.2.246.562.10.2222222222"
                                    },
                                    "suorituskieli": {
                                        "koodiarvo": "FI",
                                        "koodistoUri": "kieli"
                                    },
                                    "tyyppi": {
                                        "koodiarvo": "esiopetuksensuoritus",
                                        "koodistoUri": "suorituksentyyppi"
                                    },                                    
                                    "vahvistus": {
                                        "päivä":"2019-05-31",
                                        "paikkakunta":{"koodiarvo":"049","koodistoUri":"kunta"},
                                        "myöntäjäOrganisaatio":{"oid":$defaultMunicipalOrganizerOid},
                                        "myöntäjäHenkilöt":[{
                                            "nimi":"Unit Manager",
                                            "titteli":{"fi":"Esiopetusyksikön johtaja"},
                                            "organisaatio":{"oid":"1.2.3.4.5"}
                                        }]
                                    },
                                    "osasuoritukset": null
                                }
                            ],
                            "tyyppi": {
                                "koodiarvo": "esiopetus",
                                "koodistoUri": "opiskeluoikeudentyyppi"
                            },
                            "lähdejärjestelmänId":{
                                "id": "${stored.third.id}",
                                "lähdejärjestelmä":{
                                    "koodiarvo": "TestSystemCode",
                                    "koodistoUri": "lahdejarjestelma"
                                }
                            },
                            "oid": "${stored.third.studyRightOid}",
                            "lisätiedot": null
                        }
                    ]
                }
        """
        JSONAssert.assertEquals(expected2, stored.third.payload, JSONCompareMode.STRICT)
    }

    @Test
    fun `simple preparatory education placement in the past`() {
        db.transaction {
            it.insert(
                DevPlacement(
                    childId = testChild_1.id,
                    unitId = testDaycare.id,
                    startDate = preschoolTerm2019.start,
                    endDate = preschoolTerm2019.end,
                    type = PlacementType.PREPARATORY,
                )
            )
        }

        koskiTester.triggerUploads(today = preschoolTerm2019.end.plusDays(1))

        val stored = db.read { it.getStoredResults() }.single()
        val expected =
            """
            {
                "henkilö": {
                    "hetu": "010617A123U",
                    "etunimet": "Ricky",
                    "sukunimi": "Doe"
                },
                "opiskeluoikeudet": [
                    {
                        "tila": {
                            "opiskeluoikeusjaksot": [
                                {
                                    "alku": "2019-08-08",
                                    "tila": {
                                        "koodiarvo": "lasna",
                                        "koodistoUri": "koskiopiskeluoikeudentila"
                                    }
                                },
                                {
                                    "alku": "2020-05-29",
                                    "tila": {
                                        "koodiarvo": "valmistunut",
                                        "koodistoUri": "koskiopiskeluoikeudentila"
                                    }
                                }
                            ]
                        },
                        "suoritukset": [
                            {
                                "koulutusmoduuli": {
                                    "perusteenDiaarinumero": "57/011/2015",
                                    "tunniste": {
                                        "koodiarvo": "999905",
                                        "koodistoUri": "koulutus"
                                    }
                                },
                                "toimipiste": {
                                    "oid": "1.2.246.562.10.1111111111"
                                },
                                "suorituskieli": {
                                    "koodiarvo": "FI",
                                    "koodistoUri": "kieli"
                                },
                                "tyyppi": {
                                    "koodiarvo": "perusopetukseenvalmistavaopetus",
                                    "koodistoUri": "suorituksentyyppi"
                                },
                                "vahvistus": {
                                    "päivä": "2020-05-29",
                                    "paikkakunta": {
                                        "koodiarvo": "049",
                                        "koodistoUri": "kunta"
                                    },
                                    "myöntäjäOrganisaatio": {
                                        "oid": $defaultMunicipalOrganizerOid
                                    },
                                    "myöntäjäHenkilöt": [
                                        {
                                            "nimi": "Unit Manager",
                                            "titteli": {
                                                "fi": "Esiopetusyksikön johtaja"
                                            },
                                            "organisaatio": {
                                                "oid": "$defaultMunicipalOrganizerOid"
                                            }
                                        }
                                    ]
                                },
                                "osasuoritukset": [
                                    {
                                        "koulutusmoduuli": {
                                            "tunniste": {
                                                "koodiarvo": "ai",
                                                "nimi": {
                                                    "fi": "Suomen kieli"
                                                }
                                            },
                                            "laajuus": {
                                                "arvo": 25,
                                                "yksikkö": {
                                                    "koodiarvo": "3",
                                                    "koodistoUri": "opintojenlaajuusyksikko"
                                                }
                                            }
                                        },
                                        "arviointi": [
                                            {
                                                "arvosana": {
                                                    "koodiarvo": "O",
                                                    "koodistoUri": "arviointiasteikkoyleissivistava"
                                                },
                                                "kuvaus": {
                                                    "fi": "Suorittanut perusopetukseen valmistavan opetuksen esiopetuksen yhteydessä"
                                                }
                                            }
                                        ],
                                        "tyyppi": {
                                            "koodiarvo": "perusopetukseenvalmistavanopetuksenoppiaine",
                                            "koodistoUri": "suorituksentyyppi"
                                        },
                                        "vahvistus": null
                                    }
                                ]
                            }
                        ],
                        "tyyppi": {
                            "koodiarvo": "perusopetukseenvalmistavaopetus",
                            "koodistoUri": "opiskeluoikeudentyyppi"
                        },
                        "lähdejärjestelmänId":{
                            "id": "${stored.id}",
                            "lähdejärjestelmä":{
                                "koodiarvo": "TestSystemCode",
                                "koodistoUri": "lahdejarjestelma"
                            }
                        },
                        "oid": "${stored.studyRightOid}",
                        "lisätiedot": null
                    }
                ]
            }
        """
        JSONAssert.assertEquals(expected, stored.payload, JSONCompareMode.STRICT)
    }

    @Test
    fun `deleting all placements voids the study right`() {
        val placementId =
            db.transaction {
                it.insert(
                    DevPlacement(
                        childId = testChild_1.id,
                        unitId = testDaycare.id,
                        startDate = preschoolTerm2019.start,
                        endDate = preschoolTerm2019.end,
                        type = PlacementType.PRESCHOOL,
                    )
                )
            }

        koskiTester.triggerUploads(today = preschoolTerm2019.end.plusDays(1))

        db.transaction {
            it.execute { sql("DELETE FROM placement WHERE id = ${bind(placementId)}") }
        }
        koskiTester.triggerUploads(today = preschoolTerm2019.end.plusDays(2))

        val stored = db.read { it.getStoredResults() }.single()
        val expected =
            """
            {
                "henkilö": {
                    "hetu": "010617A123U",
                    "etunimet": "Ricky",
                    "sukunimi": "Doe"
                },
                "opiskeluoikeudet": [
                    {
                        "tila": {
                            "opiskeluoikeusjaksot": [
                                {
                                    "alku": "${preschoolTerm2019.end.plusDays(2)}",
                                    "tila": {
                                        "koodiarvo": "mitatoity",
                                        "koodistoUri": "koskiopiskeluoikeudentila"
                                    }
                                }
                            ]
                        },
                        "suoritukset": [
                            {
                                "koulutusmoduuli": {
                                    "perusteenDiaarinumero": "102/011/2014",
                                    "tunniste": {
                                        "koodiarvo": "001102",
                                        "koodistoUri": "koulutus"
                                    }
                                },
                                "toimipiste": {
                                    "oid": "1.2.246.562.10.1111111111"
                                },
                                "suorituskieli": {
                                    "koodiarvo": "FI",
                                    "koodistoUri": "kieli"
                                },
                                "tyyppi": {
                                    "koodiarvo": "esiopetuksensuoritus",
                                    "koodistoUri": "suorituksentyyppi"
                                },
                                "vahvistus": null,
                                "osasuoritukset": null
                            }
                        ],
                        "tyyppi": {
                            "koodiarvo": "esiopetus",
                            "koodistoUri": "opiskeluoikeudentyyppi"
                        },
                        "lähdejärjestelmänId":{
                            "id": "${stored.id}",
                            "lähdejärjestelmä":{
                                "koodiarvo": "TestSystemCode",
                                "koodistoUri": "lahdejarjestelma"
                            }
                        },
                        "oid": "${stored.studyRightOid}",
                        "lisätiedot": null
                    }
                ]
            }
        """
        JSONAssert.assertEquals(expected, stored.payload, JSONCompareMode.STRICT)
    }
}
