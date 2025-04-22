// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.nekku

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.absence.AbsenceCategory
import fi.espoo.evaka.absence.AbsenceType
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.dev.DevAbsence
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevChild
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.DevDaycareGroupPlacement
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.DevReservation
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.TimeRange
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

class NekkuIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var asyncJobRunner: AsyncJobRunner<AsyncJob>

    @Test
    fun `Nekku customer sync does not sync empty data`() {
        val client = TestNekkuClient()
        assertThrows<Exception> { fetchAndUpdateNekkuCustomers(client, db) }
    }

    @Test
    fun `Nekku customer sync does sync non-empty data`() {
        val client =
            TestNekkuClient(
                customers =
                    listOf(
                        NekkuApiCustomer(
                            "2501K6089",
                            "Ahvenojan päiväkoti",
                            "Varhaiskasvatus",
                            listOf(
                                CustomerApiType(
                                    listOf(
                                        NekkuCustomerApiWeekday.MONDAY,
                                        NekkuCustomerApiWeekday.TUESDAY,
                                        NekkuCustomerApiWeekday.WEDNESDAY,
                                        NekkuCustomerApiWeekday.THURSDAY,
                                        NekkuCustomerApiWeekday.FRIDAY,
                                        NekkuCustomerApiWeekday.SATURDAY,
                                        NekkuCustomerApiWeekday.SUNDAY,
                                        NekkuCustomerApiWeekday.WEEKDAYHOLIDAY,
                                    ),
                                    "100-lasta",
                                )
                            ),
                        )
                    )
            )
        fetchAndUpdateNekkuCustomers(client, db)
        db.transaction { tx ->
            val customers = tx.getNekkuCustomers()
            assertEquals(1, customers.size)
        }
    }

    @Test
    fun `Nekku customer lists only 'Varhaiskasvatus'-data`() {
        val client =
            TestNekkuClient(
                customers =
                    listOf(
                        NekkuApiCustomer(
                            "2501K6089",
                            "Ahvenojan päiväkoti",
                            "Varhaiskasvatus",
                            listOf(
                                CustomerApiType(
                                    listOf(
                                        NekkuCustomerApiWeekday.MONDAY,
                                        NekkuCustomerApiWeekday.TUESDAY,
                                        NekkuCustomerApiWeekday.WEDNESDAY,
                                        NekkuCustomerApiWeekday.THURSDAY,
                                        NekkuCustomerApiWeekday.FRIDAY,
                                        NekkuCustomerApiWeekday.SATURDAY,
                                        NekkuCustomerApiWeekday.SUNDAY,
                                        NekkuCustomerApiWeekday.WEEKDAYHOLIDAY,
                                    ),
                                    "100-lasta",
                                )
                            ),
                        ),
                        NekkuApiCustomer(
                            "4282K9253",
                            "Haukiputaan lukio lipa",
                            "Liikunta",
                            listOf(
                                CustomerApiType(
                                    listOf(
                                        NekkuCustomerApiWeekday.MONDAY,
                                        NekkuCustomerApiWeekday.TUESDAY,
                                        NekkuCustomerApiWeekday.WEDNESDAY,
                                        NekkuCustomerApiWeekday.THURSDAY,
                                        NekkuCustomerApiWeekday.FRIDAY,
                                        NekkuCustomerApiWeekday.SATURDAY,
                                        NekkuCustomerApiWeekday.SUNDAY,
                                        NekkuCustomerApiWeekday.WEEKDAYHOLIDAY,
                                    ),
                                    "palvelu-ovelle-viikonloppu-ja-arkipyhat",
                                )
                            ),
                        ),
                    )
            )
        fetchAndUpdateNekkuCustomers(client, db)
        db.transaction { tx ->
            val customers = tx.getNekkuCustomers().toSet()
            assertEquals(1, customers.size)
        }
    }

    @Test
    fun `Nekku customer updates name and unit_size`() {
        var client =
            TestNekkuClient(
                customers =
                    listOf(
                        NekkuApiCustomer(
                            "2501K6089",
                            "Ahvenojan päiväkoti",
                            "Varhaiskasvatus",
                            listOf(
                                CustomerApiType(
                                    listOf(
                                        NekkuCustomerApiWeekday.MONDAY,
                                        NekkuCustomerApiWeekday.TUESDAY,
                                        NekkuCustomerApiWeekday.WEDNESDAY,
                                        NekkuCustomerApiWeekday.THURSDAY,
                                        NekkuCustomerApiWeekday.FRIDAY,
                                        NekkuCustomerApiWeekday.SATURDAY,
                                        NekkuCustomerApiWeekday.SUNDAY,
                                        NekkuCustomerApiWeekday.WEEKDAYHOLIDAY,
                                    ),
                                    "100-lasta",
                                )
                            ),
                        )
                    )
            )
        fetchAndUpdateNekkuCustomers(client, db)

        db.transaction { tx ->
            val customers = tx.getNekkuCustomers().toSet()

            assertEquals(1, customers.size)
            assertEquals("Ahvenojan päiväkoti", customers.first().name)
            assertEquals("100-lasta", customers.first().customerType.first().type)
        }

        client =
            TestNekkuClient(
                customers =
                    listOf(
                        NekkuApiCustomer(
                            "2501K6089",
                            "Ahvenojan päiväkoti MUUTETTU",
                            "Varhaiskasvatus",
                            listOf(
                                CustomerApiType(
                                    listOf(
                                        NekkuCustomerApiWeekday.MONDAY,
                                        NekkuCustomerApiWeekday.TUESDAY,
                                        NekkuCustomerApiWeekday.WEDNESDAY,
                                        NekkuCustomerApiWeekday.THURSDAY,
                                        NekkuCustomerApiWeekday.FRIDAY,
                                        NekkuCustomerApiWeekday.SATURDAY,
                                        NekkuCustomerApiWeekday.SUNDAY,
                                        NekkuCustomerApiWeekday.WEEKDAYHOLIDAY,
                                    ),
                                    "alle 50-lasta",
                                )
                            ),
                        )
                    )
            )
        fetchAndUpdateNekkuCustomers(client, db)
        db.transaction { tx ->
            val customers = tx.getNekkuCustomers().toSet()

            assertEquals(1, customers.size)
            assertEquals("Ahvenojan päiväkoti MUUTETTU", customers.first().name)
            assertEquals("alle 50-lasta", customers.first().customerType.first().type)
        }
    }

    fun getNekkuSpecialDiet(): NekkuApiSpecialDiet {
        val nekkuSpecialDiet =
            NekkuApiSpecialDiet(
                "2",
                "Päiväkodit er.",
                listOf(
                    NekkuApiSpecialDietsField(
                        "17A9ACF0-DE9E-4C07-882E-C8C47351D009",
                        "Muu erityisruokavalio, mikä?",
                        NekkuApiSpecialDietType.Text,
                    ),
                    NekkuApiSpecialDietsField(
                        "AE1FE5FE-9619-4D7A-9043-A6B0C615156B",
                        "Erityisruokavaliot",
                        NekkuApiSpecialDietType.CheckBoxLst,
                        listOf(
                            NekkuSpecialDietOption(
                                1,
                                "Kananmunaton ruokavalio",
                                "Kananmunaton ruokavalio",
                            ),
                            NekkuSpecialDietOption(
                                2,
                                "Sianlihaton ruokavalio",
                                "Sianlihaton ruokavalio",
                            ),
                            NekkuSpecialDietOption(
                                3,
                                "Luontaisesti gluteeniton ruokavalio",
                                "Luontaisesti gluteeniton ruokavalio",
                            ),
                            NekkuSpecialDietOption(
                                4,
                                "Maitoallergisen ruokavalio",
                                "Maitoallergisen ruokavalio",
                            ),
                            NekkuSpecialDietOption(
                                5,
                                "Laktoositon ruokavalio",
                                "Laktoositon ruokavalio",
                            ),
                        ),
                    ),
                ),
            )
        return nekkuSpecialDiet
    }

    @Test
    fun `Nekku special diets sync does not sync empty data`() {
        val client = TestNekkuClient()
        assertThrows<Exception> { fetchAndUpdateNekkuSpecialDiets(client, db) }
    }

    @Test
    fun `Nekku special diets sync does sync non-empty data`() {
        val client = TestNekkuClient(specialDiets = listOf(getNekkuSpecialDiet()))

        fetchAndUpdateNekkuSpecialDiets(client, db)
        db.transaction { tx ->
            val specialDiets = tx.getNekkuSpecialOptions().toSet()
            assertEquals(5, specialDiets.size)
        }
    }

    @Test
    fun `Nekku special diets sync does update data`() {
        var client = TestNekkuClient(specialDiets = listOf(getNekkuSpecialDiet()))
        fetchAndUpdateNekkuSpecialDiets(client, db)
        db.transaction { tx ->
            val specialDiets = tx.getNekkuSpecialOptions().toSet()
            assertEquals(5, specialDiets.size)
        }

        client =
            TestNekkuClient(
                specialDiets =
                    listOf(
                        NekkuApiSpecialDiet(
                            "2",
                            "Päiväkodit erikois",
                            listOf(
                                NekkuApiSpecialDietsField(
                                    "17A9ACF0-DE9E-4C07-882E-C8C47351D009",
                                    "Muu erityisruokavalio, mikä?",
                                    NekkuApiSpecialDietType.Text,
                                ),
                                NekkuApiSpecialDietsField(
                                    "AE1FE5FE-9619-4D7A-9043-A6B0C615156B",
                                    "Erityisruokavaliot",
                                    NekkuApiSpecialDietType.CheckBoxLst,
                                    listOf(
                                        NekkuSpecialDietOption(
                                            1,
                                            "Kananmunaton ruokavalio",
                                            "Kananmunaton ruokavalio",
                                        ),
                                        NekkuSpecialDietOption(
                                            2,
                                            "Sianlihaton ruokavalio",
                                            "Sianlihaton ruokavalio",
                                        ),
                                        NekkuSpecialDietOption(
                                            3,
                                            "Luontaisesti gluteeniton ruokavalio",
                                            "Luontaisesti gluteeniton ruokavalio",
                                        ),
                                        NekkuSpecialDietOption(
                                            4,
                                            "Maitoallergisen ruokavalio",
                                            "Maitoallergisen ruokavalio",
                                        ),
                                        NekkuSpecialDietOption(
                                            5,
                                            "Laktoositon ruokavalio",
                                            "Laktoositon ruokavalio",
                                        ),
                                    ),
                                ),
                            ),
                        )
                    )
            )
        fetchAndUpdateNekkuSpecialDiets(client, db)
        db.transaction { tx ->
            val specialDiets = tx.getNekkuSpecialOptions()
            assertEquals(5, specialDiets.size)
        }
    }

    @Test
    fun `Nekku special diets sync removes old data and creates new data`() {
        var client = TestNekkuClient(specialDiets = listOf(getNekkuSpecialDiet()))
        fetchAndUpdateNekkuSpecialDiets(client, db)

        db.transaction { tx ->
            val specialDiets = tx.getNekkuSpecialOptions().toSet()
            assertEquals(5, specialDiets.size)
        }

        client =
            TestNekkuClient(
                specialDiets =
                    listOf(
                        NekkuApiSpecialDiet(
                            "2",
                            "Päiväkodit er.",
                            listOf(
                                NekkuApiSpecialDietsField(
                                    "17A9ACF0-DE9E-4C07-882E-C8C47351D009",
                                    "Muu erityisruokavalio, mikä?",
                                    NekkuApiSpecialDietType.Text,
                                ),
                                NekkuApiSpecialDietsField(
                                    "AE1FE5FE-9619-4D7A-9043-A6B0C615156B",
                                    "Erityisruokavaliot",
                                    NekkuApiSpecialDietType.CheckBoxLst,
                                    listOf(
                                        NekkuSpecialDietOption(
                                            1,
                                            "Kananmunaton ruokavalio",
                                            "Kananmunaton ruokavalio",
                                        ),
                                        NekkuSpecialDietOption(
                                            2,
                                            "Sianlihaton ruokavalio",
                                            "Sianlihaton ruokavalio",
                                        ),
                                    ),
                                ),
                            ),
                        )
                    )
            )

        fetchAndUpdateNekkuSpecialDiets(client, db)

        db.transaction { tx ->
            val specialDiets = tx.getNekkuSpecialOptions()
            assertEquals(2, specialDiets.size)
        }
    }

    @Test
    fun `Nekku special diets sync adds new special diet objects`() {
        var client = TestNekkuClient(specialDiets = listOf(getNekkuSpecialDiet()))
        fetchAndUpdateNekkuSpecialDiets(client, db)

        db.transaction { tx ->
            val nekkuSpecialDietOptions = tx.getNekkuSpecialOptions()
            assertEquals(5, nekkuSpecialDietOptions.size)
        }

        client =
            TestNekkuClient(
                specialDiets =
                    listOf(
                        getNekkuSpecialDiet(),
                        NekkuApiSpecialDiet(
                            "3",
                            "Päiväkodit erikoiset",
                            listOf(
                                NekkuApiSpecialDietsField(
                                    "17A9ACF0-DE9E-4C07-882E-C8C47351D008",
                                    "Muu erityisruokavalio, mikä?",
                                    NekkuApiSpecialDietType.Text,
                                ),
                                NekkuApiSpecialDietsField(
                                    "AE1FE5FE-9619-4D7A-9043-A6B0C6151566",
                                    "Erityisruokavaliot",
                                    NekkuApiSpecialDietType.CheckBoxLst,
                                    listOf(
                                        NekkuSpecialDietOption(
                                            1,
                                            "Kananmunaton ruokavalio",
                                            "Kananmunaton ruokavalio",
                                        ),
                                        NekkuSpecialDietOption(
                                            2,
                                            "Sianlihaton ruokavalio",
                                            "Sianlihaton ruokavalio",
                                        ),
                                    ),
                                ),
                            ),
                        ),
                    )
            )

        fetchAndUpdateNekkuSpecialDiets(client, db)

        db.transaction { tx ->
            val nekkuSpecialDietOptions = tx.getNekkuSpecialOptions()
            assertEquals(7, nekkuSpecialDietOptions.size)
        }
    }

    val nekkuProducts =
        listOf(
            NekkuApiProduct(
                "Ateriapalvelu 1 kasvis",
                "31000010",
                "",
                listOf("alle-50-lasta"),
                listOf(
                    NekkuProductMealTime.BREAKFAST,
                    NekkuProductMealTime.LUNCH,
                    NekkuProductMealTime.SNACK,
                ),
                NekkuApiProductMealType.Kasvis,
            ),
            NekkuApiProduct(
                "Ateriapalvelu 1 kasvis er",
                "31000011",
                "2",
                listOf("alle-50-lasta"),
                listOf(
                    NekkuProductMealTime.BREAKFAST,
                    NekkuProductMealTime.LUNCH,
                    NekkuProductMealTime.SNACK,
                ),
                NekkuApiProductMealType.Kasvis,
            ),
            NekkuApiProduct(
                "Päivällinen vegaani päiväkoti",
                "31000008",
                "",
                listOf("100-lasta"),
                listOf(NekkuProductMealTime.DINNER),
                NekkuApiProductMealType.Vegaani,
            ),
            NekkuApiProduct("Lounas kasvis er", "31001011", "2", listOf("50-99-lasta"), null, null),
        )

    val nekkuProductsForOrder =
        listOf(
            NekkuApiProduct(
                "Ateriapalvelu 1 aamupala",
                "31000010",
                "",
                listOf("100-lasta"),
                listOf(NekkuProductMealTime.BREAKFAST),
                null,
            ),
            NekkuApiProduct(
                "Ateriapalvelu 1 lounas",
                "31000011",
                "",
                listOf("100-lasta"),
                listOf(NekkuProductMealTime.LUNCH),
                null,
            ),
            NekkuApiProduct(
                "Ateriapalvelu 1 välipala",
                "31000012",
                "",
                listOf("100-lasta"),
                listOf(NekkuProductMealTime.SNACK),
                null,
            ),
            NekkuApiProduct(
                "Ateriapalvelu 1 iltapala",
                "31000013",
                "",
                listOf("100-lasta"),
                listOf(NekkuProductMealTime.SUPPER),
                null,
            ),
            NekkuApiProduct(
                "Ateriapalvelu kasvis 1 aamupala",
                "31000014",
                "",
                listOf("100-lasta"),
                listOf(NekkuProductMealTime.BREAKFAST),
                NekkuApiProductMealType.Kasvis,
            ),
            NekkuApiProduct(
                "Ateriapalvelu kasvis 1 lounas",
                "31000015",
                "",
                listOf("100-lasta"),
                listOf(NekkuProductMealTime.LUNCH),
                NekkuApiProductMealType.Kasvis,
            ),
            NekkuApiProduct(
                "Ateriapalvelu kasvis 1 välipala",
                "31000016",
                "",
                listOf("100-lasta"),
                listOf(NekkuProductMealTime.SNACK),
                NekkuApiProductMealType.Kasvis,
            ),
            NekkuApiProduct(
                "Ateriapalvelu kasvis 1 iltapala",
                "31000017",
                "",
                listOf("100-lasta"),
                listOf(NekkuProductMealTime.SUPPER),
                NekkuApiProductMealType.Kasvis,
            ),
            NekkuApiProduct(
                "Ateriapalvelu 2 aamupala ja lounas",
                "31000018",
                "",
                listOf("50-99-lasta"),
                listOf(NekkuProductMealTime.BREAKFAST, NekkuProductMealTime.LUNCH),
                null,
            ),
            NekkuApiProduct(
                "Ateriapalvelu 2 välipala",
                "31000019",
                "",
                listOf("50-99-lasta"),
                listOf(NekkuProductMealTime.SNACK),
                null,
            ),
        )

    @Test
    fun `Nekku product sync does not sync empty data`() {
        val client = TestNekkuClient()
        assertThrows<Exception> { fetchAndUpdateNekkuProducts(client, db) }
    }

    @Test
    fun `Nekku product sync does sync non-empty data`() {
        val client = TestNekkuClient(nekkuProducts = nekkuProducts)
        fetchAndUpdateNekkuProducts(client, db)
        db.read { tx ->
            val products = tx.getNekkuProducts()
            assertEquals(4, products.size)
        }
    }

    @Test
    fun `Nekku product deletes old products`() {
        var client = TestNekkuClient(nekkuProducts = nekkuProducts)
        fetchAndUpdateNekkuProducts(client, db)
        db.read { tx ->
            val products = tx.getNekkuProducts()
            assertEquals(4, products.size)
        }

        client =
            TestNekkuClient(
                nekkuProducts =
                    listOf(
                        NekkuApiProduct(
                            "Ateriapalvelu 1 kasvis",
                            "31000010",
                            "",
                            listOf("alle-50-lasta"),
                            listOf(
                                NekkuProductMealTime.BREAKFAST,
                                NekkuProductMealTime.LUNCH,
                                NekkuProductMealTime.SNACK,
                            ),
                            NekkuApiProductMealType.Kasvis,
                        ),
                        NekkuApiProduct(
                            "Päivällinen vegaani päiväkoti",
                            "31000008",
                            "",
                            listOf("100-lasta"),
                            listOf(NekkuProductMealTime.DINNER),
                            NekkuApiProductMealType.Kasvis,
                        ),
                    )
            )
        fetchAndUpdateNekkuProducts(client, db)
        db.read { tx ->
            val products = tx.getNekkuProducts()
            assertEquals(2, products.size)
        }
    }

    @Test
    fun `Nekku product updates values for old products`() {
        var client =
            TestNekkuClient(
                nekkuProducts =
                    listOf(
                        NekkuApiProduct(
                            "Ateriapalvelu 1 kasvis",
                            "31000010",
                            "",
                            listOf("alle-50-lasta"),
                            listOf(
                                NekkuProductMealTime.BREAKFAST,
                                NekkuProductMealTime.LUNCH,
                                NekkuProductMealTime.SNACK,
                            ),
                            NekkuApiProductMealType.Kasvis,
                        )
                    )
            )
        fetchAndUpdateNekkuProducts(client, db)
        db.read { tx ->
            val products = tx.getNekkuProducts()
            assertEquals(1, products.size)
        }

        client =
            TestNekkuClient(
                nekkuProducts =
                    listOf(
                        NekkuApiProduct(
                            "Ateriapalvelu 1 vegaani",
                            "31000010",
                            "2",
                            listOf("100-lasta"),
                            null,
                            NekkuApiProductMealType.Vegaani,
                        )
                    )
            )
        fetchAndUpdateNekkuProducts(client, db)
        db.transaction { tx ->
            val products = tx.getNekkuProducts()
            assertEquals("Ateriapalvelu 1 vegaani", products[0].name)
            assertEquals("2", products[0].optionsId)
            assertEquals("100-lasta", products[0].customerTypes.first())
            assertEquals(null, products[0].mealTime)
            assertEquals(NekkuProductMealType.VEGAN, products[0].mealType)
        }
    }

    @Test
    fun `meal order jobs for daycare groups without customer number are not planned`() {

        var client =
            TestNekkuClient(
                customers =
                    listOf(
                        NekkuApiCustomer(
                            "2501K6089",
                            "Ahvenojan päiväkoti",
                            "Varhaiskasvatus",
                            listOf(
                                CustomerApiType(
                                    listOf(
                                        NekkuCustomerApiWeekday.MONDAY,
                                        NekkuCustomerApiWeekday.TUESDAY,
                                        NekkuCustomerApiWeekday.WEDNESDAY,
                                        NekkuCustomerApiWeekday.THURSDAY,
                                        NekkuCustomerApiWeekday.FRIDAY,
                                        NekkuCustomerApiWeekday.SATURDAY,
                                        NekkuCustomerApiWeekday.SUNDAY,
                                        NekkuCustomerApiWeekday.WEEKDAYHOLIDAY,
                                    ),
                                    "100-lasta",
                                )
                            ),
                        )
                    )
            )
        fetchAndUpdateNekkuCustomers(client, db)

        val area = DevCareArea()
        val daycare =
            DevDaycare(
                areaId = area.id,
                mealtimeBreakfast = TimeRange(LocalTime.of(8, 0), LocalTime.of(8, 20)),
                mealtimeLunch = TimeRange(LocalTime.of(11, 15), LocalTime.of(11, 45)),
                mealtimeSnack = TimeRange(LocalTime.of(13, 30), LocalTime.of(13, 50)),
            )
        val group = DevDaycareGroup(daycareId = daycare.id, nekkuCustomerNumber = null)

        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(group)
        }

        // Tuesday
        val now = HelsinkiDateTime.of(LocalDate.of(2025, 4, 1), LocalTime.of(2, 25))

        planNekkuOrderJobs(db, asyncJobRunner, now)

        assertEquals(emptyList(), getNekkuWeeklyJobs())
    }

    @Test
    fun `meal order jobs for daycare groups with customer number are planned`() {

        var client =
            TestNekkuClient(
                customers =
                    listOf(
                        NekkuApiCustomer(
                            "2501K6089",
                            "Ahvenojan päiväkoti",
                            "Varhaiskasvatus",
                            listOf(
                                CustomerApiType(
                                    listOf(
                                        NekkuCustomerApiWeekday.MONDAY,
                                        NekkuCustomerApiWeekday.TUESDAY,
                                        NekkuCustomerApiWeekday.WEDNESDAY,
                                        NekkuCustomerApiWeekday.THURSDAY,
                                        NekkuCustomerApiWeekday.FRIDAY,
                                        NekkuCustomerApiWeekday.SATURDAY,
                                        NekkuCustomerApiWeekday.SUNDAY,
                                        NekkuCustomerApiWeekday.WEEKDAYHOLIDAY,
                                    ),
                                    "100-lasta",
                                )
                            ),
                        )
                    )
            )
        fetchAndUpdateNekkuCustomers(client, db)

        val area = DevCareArea()

        val daycare =
            DevDaycare(
                areaId = area.id,
                mealtimeBreakfast = TimeRange(LocalTime.of(8, 0), LocalTime.of(8, 20)),
                mealtimeLunch = TimeRange(LocalTime.of(11, 15), LocalTime.of(11, 45)),
                mealtimeSnack = TimeRange(LocalTime.of(13, 30), LocalTime.of(13, 50)),
            )
        val group = DevDaycareGroup(daycareId = daycare.id, nekkuCustomerNumber = "2501K6089")

        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(group)
        }

        // Tuesday
        val now = HelsinkiDateTime.of(LocalDate.of(2025, 3, 31), LocalTime.of(2, 25))

        planNekkuOrderJobs(db, asyncJobRunner, now)

        assertEquals(7, getNekkuWeeklyJobs().count())
    }

    @Test
    fun `meal order jobs for daycare groups are planned for two week time`() {

        var client =
            TestNekkuClient(
                customers =
                    listOf(
                        NekkuApiCustomer(
                            "2501K6089",
                            "Ahvenojan päiväkoti",
                            "Varhaiskasvatus",
                            listOf(
                                CustomerApiType(
                                    listOf(
                                        NekkuCustomerApiWeekday.MONDAY,
                                        NekkuCustomerApiWeekday.TUESDAY,
                                        NekkuCustomerApiWeekday.WEDNESDAY,
                                        NekkuCustomerApiWeekday.THURSDAY,
                                        NekkuCustomerApiWeekday.FRIDAY,
                                        NekkuCustomerApiWeekday.SATURDAY,
                                        NekkuCustomerApiWeekday.SUNDAY,
                                        NekkuCustomerApiWeekday.WEEKDAYHOLIDAY,
                                    ),
                                    "100-lasta",
                                )
                            ),
                        )
                    )
            )
        fetchAndUpdateNekkuCustomers(client, db)

        val area = DevCareArea()

        val daycare =
            DevDaycare(
                areaId = area.id,
                mealtimeBreakfast = TimeRange(LocalTime.of(8, 0), LocalTime.of(8, 20)),
                mealtimeLunch = TimeRange(LocalTime.of(11, 15), LocalTime.of(11, 45)),
                mealtimeSnack = TimeRange(LocalTime.of(13, 30), LocalTime.of(13, 50)),
            )
        val group = DevDaycareGroup(daycareId = daycare.id, nekkuCustomerNumber = "2501K6089")

        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(group)
        }

        // monday
        val now = HelsinkiDateTime.of(LocalDate.of(2025, 3, 31), LocalTime.of(2, 25))

        val nowPlusTwoWeeks = HelsinkiDateTime.of(LocalDate.of(2025, 4, 14), LocalTime.of(2, 25))

        planNekkuOrderJobs(db, asyncJobRunner, now)

        assertEquals(
            nowPlusTwoWeeks.toLocalDate().toString(),
            getNekkuWeeklyJobs().first().date.toString(),
        )
    }

    @Test
    fun `meal order jobs for daycare groups are re-planned for tomorrow`() {

        var client =
            TestNekkuClient(
                customers =
                    listOf(
                        NekkuApiCustomer(
                            "2501K6089",
                            "Ahvenojan päiväkoti",
                            "Varhaiskasvatus",
                            listOf(
                                CustomerApiType(
                                    listOf(
                                        NekkuCustomerApiWeekday.MONDAY,
                                        NekkuCustomerApiWeekday.TUESDAY,
                                        NekkuCustomerApiWeekday.WEDNESDAY,
                                        NekkuCustomerApiWeekday.THURSDAY,
                                        NekkuCustomerApiWeekday.FRIDAY,
                                        NekkuCustomerApiWeekday.SATURDAY,
                                        NekkuCustomerApiWeekday.SUNDAY,
                                        NekkuCustomerApiWeekday.WEEKDAYHOLIDAY,
                                    ),
                                    "100-lasta",
                                )
                            ),
                        )
                    )
            )
        fetchAndUpdateNekkuCustomers(client, db)

        val area = DevCareArea()

        val daycare =
            DevDaycare(
                areaId = area.id,
                mealtimeBreakfast = TimeRange(LocalTime.of(8, 0), LocalTime.of(8, 20)),
                mealtimeLunch = TimeRange(LocalTime.of(11, 15), LocalTime.of(11, 45)),
                mealtimeSnack = TimeRange(LocalTime.of(13, 30), LocalTime.of(13, 50)),
            )
        val group = DevDaycareGroup(daycareId = daycare.id, nekkuCustomerNumber = "2501K6089")

        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(group)
        }

        // Tuesday
        val now = HelsinkiDateTime.of(LocalDate.of(2025, 4, 1), LocalTime.of(2, 25))

        val tomorrow = HelsinkiDateTime.of(LocalDate.of(2025, 4, 2), LocalTime.of(2, 25))

        planNekkuDailyOrderJobs(db, asyncJobRunner, now)

        assertEquals(
            tomorrow.toLocalDate().toString(),
            getNekkuDailyJobs().single().date.toString(),
        )
    }

    @Test
    fun `Send Nekku orders with known reservations`() {
        val monday = LocalDate.of(2025, 4, 14)
        val tuesday = LocalDate.of(2025, 4, 15)

        // First create all of the basic backgrounds like
        // Customer numbers
        var client =
            TestNekkuClient(
                customers =
                    listOf(
                        NekkuApiCustomer(
                            "2501K6089",
                            "Ahvenojan päiväkoti",
                            "Varhaiskasvatus",
                            listOf(
                                CustomerApiType(
                                    listOf(
                                        NekkuCustomerApiWeekday.MONDAY,
                                        NekkuCustomerApiWeekday.TUESDAY,
                                        NekkuCustomerApiWeekday.WEDNESDAY,
                                        NekkuCustomerApiWeekday.THURSDAY,
                                        NekkuCustomerApiWeekday.FRIDAY,
                                        NekkuCustomerApiWeekday.SATURDAY,
                                        NekkuCustomerApiWeekday.SUNDAY,
                                        NekkuCustomerApiWeekday.WEEKDAYHOLIDAY,
                                    ),
                                    "100-lasta",
                                )
                            ),
                        )
                    ),
                nekkuProducts = nekkuProductsForOrder,
            )
        fetchAndUpdateNekkuCustomers(client, db)
        // products
        fetchAndUpdateNekkuProducts(client, db)
        // Daycare with groups
        val area = DevCareArea()
        val daycare =
            DevDaycare(
                areaId = area.id,
                mealtimeBreakfast = TimeRange(LocalTime.of(8, 0), LocalTime.of(8, 20)),
                mealtimeLunch = TimeRange(LocalTime.of(11, 15), LocalTime.of(11, 45)),
                mealtimeSnack = TimeRange(LocalTime.of(13, 30), LocalTime.of(13, 50)),
            )
        val group = DevDaycareGroup(daycareId = daycare.id, nekkuCustomerNumber = "2501K6089")
        val employee = DevEmployee()

        // Children with placements in the group and they are not absent
        val child = DevPerson()

        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(group)
            tx.insert(employee)
            tx.insert(child, DevPersonType.CHILD)
            tx.insert(
                    DevPlacement(
                        childId = child.id,
                        unitId = daycare.id,
                        startDate = monday,
                        endDate = tuesday,
                    )
                )
                .also { placementId ->
                    tx.insert(
                        DevDaycareGroupPlacement(
                            daycarePlacementId = placementId,
                            daycareGroupId = group.id,
                            startDate = monday,
                            endDate = tuesday,
                        )
                    )
                }
            listOf(
                    // Two meals on Monday
                    DevReservation(
                        childId = child.id,
                        date = monday,
                        startTime = LocalTime.of(8, 0),
                        endTime = LocalTime.of(12, 0),
                        createdBy = employee.evakaUserId,
                    ),
                    // Breakfast only on Tuesday
                    DevReservation(
                        childId = child.id,
                        date = tuesday,
                        startTime = LocalTime.of(8, 0),
                        endTime = LocalTime.of(9, 0),
                        createdBy = employee.evakaUserId,
                    ),
                )
                .forEach { tx.insert(it) }
        }

        createAndSendNekkuOrder(client, db, group.id, monday, 0.9)
        createAndSendNekkuOrder(client, db, group.id, tuesday, 0.9)

        assertEquals(
            listOf(
                NekkuClient.NekkuOrders(
                    listOf(
                        NekkuClient.NekkuOrder(
                            monday.toString(),
                            "2501K6089",
                            group.id.toString(),
                            listOf(
                                NekkuClient.Item("31000010", 1, null),
                                NekkuClient.Item("31000011", 1, null),
                            ),
                            group.name,
                        )
                    ),
                    dryRun = false,
                ),
                NekkuClient.NekkuOrders(
                    listOf(
                        NekkuClient.NekkuOrder(
                            tuesday.toString(),
                            "2501K6089",
                            group.id.toString(),
                            listOf(NekkuClient.Item("31000010", 1, null)),
                            group.name,
                        )
                    ),
                    dryRun = false,
                ),
            ),
            client.orders,
        )
    }

    @Test
    fun `Send Nekku orders with known reservations and remove 10prcent of normal orders`() {
        val monday = LocalDate.of(2025, 4, 14)
        val tuesday = LocalDate.of(2025, 4, 15)

        // First create all of the basic backgrounds like
        // Customer numbers
        var client =
            TestNekkuClient(
                customers =
                    listOf(
                        NekkuApiCustomer(
                            "2501K6089",
                            "Ahvenojan päiväkoti",
                            "Varhaiskasvatus",
                            listOf(
                                CustomerApiType(
                                    listOf(
                                        NekkuCustomerApiWeekday.MONDAY,
                                        NekkuCustomerApiWeekday.TUESDAY,
                                        NekkuCustomerApiWeekday.WEDNESDAY,
                                        NekkuCustomerApiWeekday.THURSDAY,
                                        NekkuCustomerApiWeekday.FRIDAY,
                                        NekkuCustomerApiWeekday.SATURDAY,
                                        NekkuCustomerApiWeekday.SUNDAY,
                                        NekkuCustomerApiWeekday.WEEKDAYHOLIDAY,
                                    ),
                                    "100-lasta",
                                )
                            ),
                        )
                    ),
                nekkuProducts = nekkuProductsForOrder,
            )
        fetchAndUpdateNekkuCustomers(client, db)
        // products
        fetchAndUpdateNekkuProducts(client, db)
        // Daycare with groups
        val area = DevCareArea()
        val daycare =
            DevDaycare(
                areaId = area.id,
                mealtimeBreakfast = TimeRange(LocalTime.of(8, 0), LocalTime.of(8, 20)),
                mealtimeLunch = TimeRange(LocalTime.of(11, 15), LocalTime.of(11, 45)),
                mealtimeSnack = TimeRange(LocalTime.of(13, 30), LocalTime.of(13, 50)),
            )
        val group = DevDaycareGroup(daycareId = daycare.id, nekkuCustomerNumber = "2501K6089")
        val employee = DevEmployee()

        // Children with placements in the group and they are not absent

        val children =
            listOf(
                DevPerson(),
                DevPerson(),
                DevPerson(),
                DevPerson(),
                DevPerson(),
                DevPerson(),
                DevPerson(),
                DevPerson(),
                DevPerson(),
                DevPerson(),
            )

        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(group)
            tx.insert(employee)
            children.map { child ->
                tx.insert(child, DevPersonType.CHILD)
                tx.insert(
                        DevPlacement(
                            childId = child.id,
                            unitId = daycare.id,
                            startDate = monday,
                            endDate = tuesday,
                        )
                    )
                    .also { placementId ->
                        tx.insert(
                            DevDaycareGroupPlacement(
                                daycarePlacementId = placementId,
                                daycareGroupId = group.id,
                                startDate = monday,
                                endDate = tuesday,
                            )
                        )
                    }
                listOf(
                        // Two meals on Monday
                        DevReservation(
                            childId = child.id,
                            date = monday,
                            startTime = LocalTime.of(8, 0),
                            endTime = LocalTime.of(12, 0),
                            createdBy = employee.evakaUserId,
                        ),
                        // Breakfast only on Tuesday
                        DevReservation(
                            childId = child.id,
                            date = tuesday,
                            startTime = LocalTime.of(8, 0),
                            endTime = LocalTime.of(9, 0),
                            createdBy = employee.evakaUserId,
                        ),
                    )
                    .forEach { tx.insert(it) }
            }
        }

        createAndSendNekkuOrder(client, db, group.id, monday, 0.9)
        createAndSendNekkuOrder(client, db, group.id, tuesday, 0.9)

        assertEquals(
            listOf(
                NekkuClient.NekkuOrders(
                    listOf(
                        NekkuClient.NekkuOrder(
                            monday.toString(),
                            "2501K6089",
                            group.id.toString(),
                            listOf(
                                NekkuClient.Item("31000010", 9, null),
                                NekkuClient.Item("31000011", 9, null),
                            ),
                            group.name,
                        )
                    ),
                    dryRun = false,
                ),
                NekkuClient.NekkuOrders(
                    listOf(
                        NekkuClient.NekkuOrder(
                            tuesday.toString(),
                            "2501K6089",
                            group.id.toString(),
                            listOf(NekkuClient.Item("31000010", 9, null)),
                            group.name,
                        )
                    ),
                    dryRun = false,
                ),
            ),
            client.orders,
        )
    }

    @Test
    fun `10percnt deduction does not remove any vegan or vegetable diets`() {
        val monday = LocalDate.of(2025, 4, 14)
        val tuesday = LocalDate.of(2025, 4, 15)

        // First create all of the basic backgrounds like
        // Customer numbers
        var client =
            TestNekkuClient(
                customers =
                    listOf(
                        NekkuApiCustomer(
                            "2501K6089",
                            "Ahvenojan päiväkoti",
                            "Varhaiskasvatus",
                            listOf(
                                CustomerApiType(
                                    listOf(
                                        NekkuCustomerApiWeekday.MONDAY,
                                        NekkuCustomerApiWeekday.TUESDAY,
                                        NekkuCustomerApiWeekday.WEDNESDAY,
                                        NekkuCustomerApiWeekday.THURSDAY,
                                        NekkuCustomerApiWeekday.FRIDAY,
                                        NekkuCustomerApiWeekday.SATURDAY,
                                        NekkuCustomerApiWeekday.SUNDAY,
                                        NekkuCustomerApiWeekday.WEEKDAYHOLIDAY,
                                    ),
                                    "100-lasta",
                                )
                            ),
                        )
                    ),
                nekkuProducts = nekkuProductsForOrder,
            )
        fetchAndUpdateNekkuCustomers(client, db)
        // products
        fetchAndUpdateNekkuProducts(client, db)
        // Daycare with groups
        val area = DevCareArea()
        val daycare =
            DevDaycare(
                areaId = area.id,
                mealtimeBreakfast = TimeRange(LocalTime.of(8, 0), LocalTime.of(8, 20)),
                mealtimeLunch = TimeRange(LocalTime.of(11, 15), LocalTime.of(11, 45)),
                mealtimeSnack = TimeRange(LocalTime.of(13, 30), LocalTime.of(13, 50)),
            )
        val group = DevDaycareGroup(daycareId = daycare.id, nekkuCustomerNumber = "2501K6089")
        val employee = DevEmployee()

        // Children with placements in the group and they are not absent

        val children =
            listOf(
                DevPerson(),
                DevPerson(),
                DevPerson(),
                DevPerson(),
                DevPerson(),
                DevPerson(),
                DevPerson(),
                DevPerson(),
                DevPerson(),
                DevPerson(),
            )

        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(group)
            tx.insert(employee)
            children.map { child ->
                tx.insert(child, DevPersonType.CHILD)
                tx.insert(DevChild(id = child.id, nekkuDiet = NekkuProductMealType.VEGETABLE))
                tx.insert(
                        DevPlacement(
                            childId = child.id,
                            unitId = daycare.id,
                            startDate = monday,
                            endDate = tuesday,
                        )
                    )
                    .also { placementId ->
                        tx.insert(
                            DevDaycareGroupPlacement(
                                daycarePlacementId = placementId,
                                daycareGroupId = group.id,
                                startDate = monday,
                                endDate = tuesday,
                            )
                        )
                    }
                listOf(
                        // Two meals on Monday
                        DevReservation(
                            childId = child.id,
                            date = monday,
                            startTime = LocalTime.of(8, 0),
                            endTime = LocalTime.of(12, 0),
                            createdBy = employee.evakaUserId,
                        ),
                        // Breakfast only on Tuesday
                        DevReservation(
                            childId = child.id,
                            date = tuesday,
                            startTime = LocalTime.of(8, 0),
                            endTime = LocalTime.of(9, 0),
                            createdBy = employee.evakaUserId,
                        ),
                    )
                    .forEach { tx.insert(it) }
            }
        }

        createAndSendNekkuOrder(client, db, group.id, monday, 0.9)
        createAndSendNekkuOrder(client, db, group.id, tuesday, 0.9)

        assertEquals(
            listOf(
                NekkuClient.NekkuOrders(
                    listOf(
                        NekkuClient.NekkuOrder(
                            monday.toString(),
                            "2501K6089",
                            group.id.toString(),
                            listOf(
                                NekkuClient.Item("31000014", 10, null),
                                NekkuClient.Item("31000015", 10, null),
                            ),
                            group.name,
                        )
                    ),
                    dryRun = false,
                ),
                NekkuClient.NekkuOrders(
                    listOf(
                        NekkuClient.NekkuOrder(
                            tuesday.toString(),
                            "2501K6089",
                            group.id.toString(),
                            listOf(NekkuClient.Item("31000014", 10, null)),
                            group.name,
                        )
                    ),
                    dryRun = false,
                ),
            ),
            client.orders,
        )
    }

    @Test
    fun `Combined breakfast and lunch is ordered even if child is not eating breakfast`() {

        val monday = LocalDate.of(2025, 4, 14)
        val tuesday = LocalDate.of(2025, 4, 15)

        // First create all of the basic backgrounds like
        // Customer numbers
        var client =
            TestNekkuClient(
                customers =
                    listOf(
                        NekkuApiCustomer(
                            "2501K6090",
                            "Apporan päiväkoti",
                            "Varhaiskasvatus",
                            listOf(
                                CustomerApiType(
                                    listOf(
                                        NekkuCustomerApiWeekday.MONDAY,
                                        NekkuCustomerApiWeekday.TUESDAY,
                                        NekkuCustomerApiWeekday.WEDNESDAY,
                                        NekkuCustomerApiWeekday.THURSDAY,
                                        NekkuCustomerApiWeekday.FRIDAY,
                                        NekkuCustomerApiWeekday.SATURDAY,
                                        NekkuCustomerApiWeekday.SUNDAY,
                                        NekkuCustomerApiWeekday.WEEKDAYHOLIDAY,
                                    ),
                                    "50-99-lasta",
                                )
                            ),
                        )
                    ),
                nekkuProducts = nekkuProductsForOrder,
            )
        fetchAndUpdateNekkuCustomers(client, db)
        // products
        fetchAndUpdateNekkuProducts(client, db)
        // Daycare with groups
        val area = DevCareArea()
        val daycare =
            DevDaycare(
                areaId = area.id,
                mealtimeBreakfast = TimeRange(LocalTime.of(8, 0), LocalTime.of(8, 20)),
                mealtimeLunch = TimeRange(LocalTime.of(11, 15), LocalTime.of(11, 45)),
                mealtimeSnack = TimeRange(LocalTime.of(13, 30), LocalTime.of(13, 50)),
            )
        val group = DevDaycareGroup(daycareId = daycare.id, nekkuCustomerNumber = "2501K6090")
        val employee = DevEmployee()

        // Children with placements in the group and they are not absent
        val child = DevPerson()

        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(group)
            tx.insert(employee)
            tx.insert(child, DevPersonType.CHILD)
            tx.insert(
                DevChild(id = child.id, eatsBreakfast = false)
            ) // child does not eat breakfast
            tx.insert(
                    DevPlacement(
                        childId = child.id,
                        unitId = daycare.id,
                        startDate = monday,
                        endDate = tuesday,
                    )
                )
                .also { placementId ->
                    tx.insert(
                        DevDaycareGroupPlacement(
                            daycarePlacementId = placementId,
                            daycareGroupId = group.id,
                            startDate = monday,
                            endDate = tuesday,
                        )
                    )
                }
            listOf(
                    // Two meals on Monday
                    DevReservation(
                        childId = child.id,
                        date = monday,
                        startTime = LocalTime.of(8, 0),
                        endTime = LocalTime.of(12, 0),
                        createdBy = employee.evakaUserId,
                    ),
                    // Breakfast only on Tuesday
                    DevReservation(
                        childId = child.id,
                        date = tuesday,
                        startTime = LocalTime.of(8, 0),
                        endTime = LocalTime.of(9, 0),
                        createdBy = employee.evakaUserId,
                    ),
                )
                .forEach { tx.insert(it) }
        }

        createAndSendNekkuOrder(client, db, group.id, monday, 0.9)

        assertEquals(
            listOf(
                NekkuClient.NekkuOrders(
                    listOf(
                        NekkuClient.NekkuOrder(
                            monday.toString(),
                            "2501K6090",
                            group.id.toString(),
                            listOf(NekkuClient.Item("31000018", 1, null)),
                            group.name,
                        )
                    ),
                    dryRun = false,
                )
            ),
            client.orders,
        )
    }

    @Test
    fun `Breakfast is deducted from meals if child is not eating breakfast according to child details`() {

        val monday = LocalDate.of(2025, 4, 14)
        val tuesday = LocalDate.of(2025, 4, 15)

        // First create all of the basic backgrounds like
        // Customer numbers
        var client =
            TestNekkuClient(
                customers =
                    listOf(
                        NekkuApiCustomer(
                            "2501K6089",
                            "Ahvenojan päiväkoti",
                            "Varhaiskasvatus",
                            listOf(
                                CustomerApiType(
                                    listOf(
                                        NekkuCustomerApiWeekday.MONDAY,
                                        NekkuCustomerApiWeekday.TUESDAY,
                                        NekkuCustomerApiWeekday.WEDNESDAY,
                                        NekkuCustomerApiWeekday.THURSDAY,
                                        NekkuCustomerApiWeekday.FRIDAY,
                                        NekkuCustomerApiWeekday.SATURDAY,
                                        NekkuCustomerApiWeekday.SUNDAY,
                                        NekkuCustomerApiWeekday.WEEKDAYHOLIDAY,
                                    ),
                                    "100-lasta",
                                )
                            ),
                        )
                    ),
                nekkuProducts = nekkuProductsForOrder,
            )
        fetchAndUpdateNekkuCustomers(client, db)
        // products
        fetchAndUpdateNekkuProducts(client, db)
        // Daycare with groups
        val area = DevCareArea()
        val daycare =
            DevDaycare(
                areaId = area.id,
                mealtimeBreakfast = TimeRange(LocalTime.of(8, 0), LocalTime.of(8, 20)),
                mealtimeLunch = TimeRange(LocalTime.of(11, 15), LocalTime.of(11, 45)),
                mealtimeSnack = TimeRange(LocalTime.of(13, 30), LocalTime.of(13, 50)),
            )
        val group = DevDaycareGroup(daycareId = daycare.id, nekkuCustomerNumber = "2501K6089")
        val employee = DevEmployee()

        // Children with placements in the group and they are not absent
        val child = DevPerson()

        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(group)
            tx.insert(employee)
            tx.insert(child, DevPersonType.CHILD)
            tx.insert(
                DevChild(id = child.id, eatsBreakfast = false)
            ) // child does not eat breakfast
            tx.insert(
                    DevPlacement(
                        childId = child.id,
                        unitId = daycare.id,
                        startDate = monday,
                        endDate = tuesday,
                    )
                )
                .also { placementId ->
                    tx.insert(
                        DevDaycareGroupPlacement(
                            daycarePlacementId = placementId,
                            daycareGroupId = group.id,
                            startDate = monday,
                            endDate = tuesday,
                        )
                    )
                }
            listOf(
                    // Two meals on Monday
                    DevReservation(
                        childId = child.id,
                        date = monday,
                        startTime = LocalTime.of(8, 0),
                        endTime = LocalTime.of(12, 0),
                        createdBy = employee.evakaUserId,
                    )
                )
                .forEach { tx.insert(it) }
        }

        createAndSendNekkuOrder(client, db, group.id, monday, 0.9)

        assertEquals(
            listOf(
                NekkuClient.NekkuOrders(
                    listOf(
                        NekkuClient.NekkuOrder(
                            monday.toString(),
                            "2501K6089",
                            group.id.toString(),
                            listOf(NekkuClient.Item("31000011", 1, null)),
                            group.name,
                        )
                    ),
                    dryRun = false,
                )
            ),
            client.orders,
        )
    }

    @Test
    fun `Send Nekku orders with different meal types`() {
        val monday = LocalDate.of(2025, 4, 14)
        val tuesday = LocalDate.of(2025, 4, 15)

        // First create all of the basic backgrounds like
        // Customer numbers
        var client =
            TestNekkuClient(
                customers =
                    listOf(
                        NekkuApiCustomer(
                            "2501K6089",
                            "Ahvenojan päiväkoti",
                            "Varhaiskasvatus",
                            listOf(
                                CustomerApiType(
                                    listOf(
                                        NekkuCustomerApiWeekday.MONDAY,
                                        NekkuCustomerApiWeekday.TUESDAY,
                                        NekkuCustomerApiWeekday.WEDNESDAY,
                                        NekkuCustomerApiWeekday.THURSDAY,
                                        NekkuCustomerApiWeekday.FRIDAY,
                                        NekkuCustomerApiWeekday.SATURDAY,
                                        NekkuCustomerApiWeekday.SUNDAY,
                                        NekkuCustomerApiWeekday.WEEKDAYHOLIDAY,
                                    ),
                                    "100-lasta",
                                )
                            ),
                        )
                    ),
                nekkuProducts = nekkuProductsForOrder,
            )
        fetchAndUpdateNekkuCustomers(client, db)
        // products
        fetchAndUpdateNekkuProducts(client, db)
        // Daycare with groups
        val area = DevCareArea()
        val daycare =
            DevDaycare(
                areaId = area.id,
                mealtimeBreakfast = TimeRange(LocalTime.of(8, 0), LocalTime.of(8, 20)),
                mealtimeLunch = TimeRange(LocalTime.of(11, 15), LocalTime.of(11, 45)),
                mealtimeSnack = TimeRange(LocalTime.of(13, 30), LocalTime.of(13, 50)),
            )
        val group = DevDaycareGroup(daycareId = daycare.id, nekkuCustomerNumber = "2501K6089")
        val employee = DevEmployee()

        // Children with placements in the group and they are not absent
        val child = DevPerson()

        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(group)
            tx.insert(employee)
            tx.insert(child, DevPersonType.CHILD)
            tx.insert(DevChild(id = child.id, nekkuDiet = NekkuProductMealType.VEGETABLE))
            tx.insert(
                    DevPlacement(
                        childId = child.id,
                        unitId = daycare.id,
                        startDate = monday,
                        endDate = tuesday,
                    )
                )
                .also { placementId ->
                    tx.insert(
                        DevDaycareGroupPlacement(
                            daycarePlacementId = placementId,
                            daycareGroupId = group.id,
                            startDate = monday,
                            endDate = tuesday,
                        )
                    )
                }
            listOf(
                    // Three meals on Monday
                    DevReservation(
                        childId = child.id,
                        date = monday,
                        startTime = LocalTime.of(8, 0),
                        endTime = LocalTime.of(16, 0),
                        createdBy = employee.evakaUserId,
                    ),
                    // Breakfast only on Tuesday
                    DevReservation(
                        childId = child.id,
                        date = tuesday,
                        startTime = LocalTime.of(8, 0),
                        endTime = LocalTime.of(9, 0),
                        createdBy = employee.evakaUserId,
                    ),
                )
                .forEach { tx.insert(it) }
        }

        createAndSendNekkuOrder(client, db, group.id, monday, 0.9)
        createAndSendNekkuOrder(client, db, group.id, tuesday, 0.9)

        assertEquals(
            listOf(
                NekkuClient.NekkuOrders(
                    listOf(
                        NekkuClient.NekkuOrder(
                            monday.toString(),
                            "2501K6089",
                            group.id.toString(),
                            listOf(
                                NekkuClient.Item("31000014", 1, null),
                                NekkuClient.Item("31000015", 1, null),
                                NekkuClient.Item("31000016", 1, null),
                            ),
                            group.name,
                        )
                    ),
                    dryRun = false,
                ),
                NekkuClient.NekkuOrders(
                    listOf(
                        NekkuClient.NekkuOrder(
                            tuesday.toString(),
                            "2501K6089",
                            group.id.toString(),
                            listOf(NekkuClient.Item("31000014", 1, null)),
                            group.name,
                        )
                    ),
                    dryRun = false,
                ),
            ),
            client.orders,
        )
    }

    @Test
    fun `Send Nekku orders with 2 children without reservations uses default meal amounts`() {
        val monday = LocalDate.of(2025, 4, 14)
        val tuesday = LocalDate.of(2025, 4, 15)

        // First create all of the basic backgrounds like
        // Customer numbers
        var client =
            TestNekkuClient(
                customers =
                    listOf(
                        NekkuApiCustomer(
                            "2501K6089",
                            "Ahvenojan päiväkoti",
                            "Varhaiskasvatus",
                            listOf(
                                CustomerApiType(
                                    listOf(
                                        NekkuCustomerApiWeekday.MONDAY,
                                        NekkuCustomerApiWeekday.TUESDAY,
                                        NekkuCustomerApiWeekday.WEDNESDAY,
                                        NekkuCustomerApiWeekday.THURSDAY,
                                        NekkuCustomerApiWeekday.FRIDAY,
                                        NekkuCustomerApiWeekday.SATURDAY,
                                        NekkuCustomerApiWeekday.SUNDAY,
                                        NekkuCustomerApiWeekday.WEEKDAYHOLIDAY,
                                    ),
                                    "100-lasta",
                                )
                            ),
                        )
                    ),
                nekkuProducts = nekkuProductsForOrder,
            )
        fetchAndUpdateNekkuCustomers(client, db)
        // products
        fetchAndUpdateNekkuProducts(client, db)
        // Daycare with groups
        val area = DevCareArea()
        val daycare =
            DevDaycare(
                areaId = area.id,
                mealtimeBreakfast = TimeRange(LocalTime.of(8, 0), LocalTime.of(8, 20)),
                mealtimeLunch = TimeRange(LocalTime.of(11, 15), LocalTime.of(11, 45)),
                mealtimeSnack = TimeRange(LocalTime.of(13, 30), LocalTime.of(13, 50)),
            )
        val group = DevDaycareGroup(daycareId = daycare.id, nekkuCustomerNumber = "2501K6089")
        val employee = DevEmployee()

        // Children with placements in the group and they are not absent
        val child = DevPerson()
        val child2 = DevPerson()

        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(group)
            tx.insert(employee)
            tx.insert(child, DevPersonType.CHILD)
            tx.insert(
                    DevPlacement(
                        childId = child.id,
                        unitId = daycare.id,
                        startDate = monday,
                        endDate = tuesday,
                    )
                )
                .also { placementId ->
                    tx.insert(
                        DevDaycareGroupPlacement(
                            daycarePlacementId = placementId,
                            daycareGroupId = group.id,
                            startDate = monday,
                            endDate = tuesday,
                        )
                    )
                }
            tx.insert(child2, DevPersonType.CHILD)
            tx.insert(
                    DevPlacement(
                        childId = child2.id,
                        unitId = daycare.id,
                        startDate = monday,
                        endDate = tuesday,
                    )
                )
                .also { placementId ->
                    tx.insert(
                        DevDaycareGroupPlacement(
                            daycarePlacementId = placementId,
                            daycareGroupId = group.id,
                            startDate = monday,
                            endDate = tuesday,
                        )
                    )
                }
        }

        createAndSendNekkuOrder(client, db, group.id, monday, 0.9)
        createAndSendNekkuOrder(client, db, group.id, tuesday, 0.9)

        assertEquals(
            listOf(
                NekkuClient.NekkuOrders(
                    listOf(
                        NekkuClient.NekkuOrder(
                            monday.toString(),
                            "2501K6089",
                            group.id.toString(),
                            listOf(
                                NekkuClient.Item("31000010", 2, null),
                                NekkuClient.Item("31000011", 2, null),
                                NekkuClient.Item("31000012", 2, null),
                            ),
                            group.name,
                        )
                    ),
                    dryRun = false,
                ),
                NekkuClient.NekkuOrders(
                    listOf(
                        NekkuClient.NekkuOrder(
                            tuesday.toString(),
                            "2501K6089",
                            group.id.toString(),
                            listOf(
                                NekkuClient.Item("31000010", 2, null),
                                NekkuClient.Item("31000011", 2, null),
                                NekkuClient.Item("31000012", 2, null),
                            ),
                            group.name,
                        )
                    ),
                    dryRun = false,
                ),
            ),
            client.orders,
        )
    }

    @Test
    fun `Nekku order items are not generated if child is absent`() {
        val monday = LocalDate.of(2025, 4, 14)
        val tuesday = LocalDate.of(2025, 4, 15)

        // First create all of the basic backgrounds like
        // Customer numbers
        var client =
            TestNekkuClient(
                customers =
                    listOf(
                        NekkuApiCustomer(
                            "2501K6089",
                            "Ahvenojan päiväkoti",
                            "Varhaiskasvatus",
                            listOf(
                                CustomerApiType(
                                    listOf(
                                        NekkuCustomerApiWeekday.MONDAY,
                                        NekkuCustomerApiWeekday.TUESDAY,
                                        NekkuCustomerApiWeekday.WEDNESDAY,
                                        NekkuCustomerApiWeekday.THURSDAY,
                                        NekkuCustomerApiWeekday.FRIDAY,
                                        NekkuCustomerApiWeekday.SATURDAY,
                                        NekkuCustomerApiWeekday.SUNDAY,
                                        NekkuCustomerApiWeekday.WEEKDAYHOLIDAY,
                                    ),
                                    "100-lasta",
                                )
                            ),
                        )
                    ),
                nekkuProducts = nekkuProductsForOrder,
            )
        fetchAndUpdateNekkuCustomers(client, db)
        // products
        fetchAndUpdateNekkuProducts(client, db)
        // Daycare with groups
        val area = DevCareArea()
        val daycare =
            DevDaycare(
                areaId = area.id,
                mealtimeBreakfast = TimeRange(LocalTime.of(8, 0), LocalTime.of(8, 20)),
                mealtimeLunch = TimeRange(LocalTime.of(11, 15), LocalTime.of(11, 45)),
                mealtimeSnack = TimeRange(LocalTime.of(13, 30), LocalTime.of(13, 50)),
            )
        val group = DevDaycareGroup(daycareId = daycare.id, nekkuCustomerNumber = "2501K6089")
        val employee = DevEmployee()

        // Children with placements in the group and they are not absent
        val child = DevPerson()

        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(group)
            tx.insert(employee)
            tx.insert(child, DevPersonType.CHILD)
            tx.insert(
                    DevPlacement(
                        childId = child.id,
                        unitId = daycare.id,
                        startDate = monday,
                        endDate = tuesday,
                    )
                )
                .also { placementId ->
                    tx.insert(
                        DevDaycareGroupPlacement(
                            daycarePlacementId = placementId,
                            daycareGroupId = group.id,
                            startDate = monday,
                            endDate = tuesday,
                        )
                    )
                }
            listOf(
                    // Child is absent, so no meals on Monday
                    DevAbsence(
                        childId = child.id,
                        date = monday,
                        absenceType = AbsenceType.PLANNED_ABSENCE,
                        absenceCategory = AbsenceCategory.BILLABLE,
                        modifiedBy = employee.evakaUserId,
                        modifiedAt = HelsinkiDateTime.now(),
                    ),
                    // Child is absent, so no meals on Tuesday
                    DevAbsence(
                        childId = child.id,
                        date = tuesday,
                        absenceType = AbsenceType.PLANNED_ABSENCE,
                        absenceCategory = AbsenceCategory.BILLABLE,
                        modifiedBy = employee.evakaUserId,
                        modifiedAt = HelsinkiDateTime.now(),
                    ),
                )
                .forEach { tx.insert(it) }
        }

        createAndSendNekkuOrder(client, db, group.id, monday, 0.9)
        createAndSendNekkuOrder(client, db, group.id, tuesday, 0.9)

        assertEquals(
            listOf(
                NekkuClient.NekkuOrders(
                    listOf(
                        NekkuClient.NekkuOrder(
                            monday.toString(),
                            "2501K6089",
                            group.id.toString(),
                            emptyList(),
                            group.name,
                        )
                    ),
                    dryRun = false,
                ),
                NekkuClient.NekkuOrders(
                    listOf(
                        NekkuClient.NekkuOrder(
                            tuesday.toString(),
                            "2501K6089",
                            group.id.toString(),
                            emptyList(),
                            group.name,
                        )
                    ),
                    dryRun = false,
                ),
            ),
            client.orders,
        )
    }

    @Test
    fun `Send Nekku orders with different size customers`() {
        val monday = LocalDate.of(2025, 4, 14)
        val tuesday = LocalDate.of(2025, 4, 15)

        // First create all of the basic backgrounds like
        // Customer numbers
        var client =
            TestNekkuClient(
                customers =
                    listOf(
                        NekkuApiCustomer(
                            "2501K6090",
                            "Apporan päiväkoti",
                            "Varhaiskasvatus",
                            listOf(
                                CustomerApiType(
                                    listOf(
                                        NekkuCustomerApiWeekday.MONDAY,
                                        NekkuCustomerApiWeekday.TUESDAY,
                                        NekkuCustomerApiWeekday.WEDNESDAY,
                                        NekkuCustomerApiWeekday.THURSDAY,
                                        NekkuCustomerApiWeekday.FRIDAY,
                                        NekkuCustomerApiWeekday.SATURDAY,
                                        NekkuCustomerApiWeekday.SUNDAY,
                                        NekkuCustomerApiWeekday.WEEKDAYHOLIDAY,
                                    ),
                                    "50-99-lasta",
                                )
                            ),
                        )
                    ),
                nekkuProducts = nekkuProductsForOrder,
            )
        fetchAndUpdateNekkuCustomers(client, db)
        // products
        fetchAndUpdateNekkuProducts(client, db)
        // Daycare with groups
        val area = DevCareArea()
        val daycare =
            DevDaycare(
                areaId = area.id,
                mealtimeBreakfast = TimeRange(LocalTime.of(8, 0), LocalTime.of(8, 20)),
                mealtimeLunch = TimeRange(LocalTime.of(11, 15), LocalTime.of(11, 45)),
                mealtimeSnack = TimeRange(LocalTime.of(13, 30), LocalTime.of(13, 50)),
            )
        val group = DevDaycareGroup(daycareId = daycare.id, nekkuCustomerNumber = "2501K6090")
        val employee = DevEmployee()

        // Children with placements in the group and they are not absent
        val child = DevPerson()

        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(group)
            tx.insert(employee)
            tx.insert(child, DevPersonType.CHILD)
            tx.insert(
                    DevPlacement(
                        childId = child.id,
                        unitId = daycare.id,
                        startDate = monday,
                        endDate = tuesday,
                    )
                )
                .also { placementId ->
                    tx.insert(
                        DevDaycareGroupPlacement(
                            daycarePlacementId = placementId,
                            daycareGroupId = group.id,
                            startDate = monday,
                            endDate = tuesday,
                        )
                    )
                }
            listOf(
                    // Two meals on Monday
                    DevReservation(
                        childId = child.id,
                        date = monday,
                        startTime = LocalTime.of(8, 0),
                        endTime = LocalTime.of(12, 0),
                        createdBy = employee.evakaUserId,
                    ),
                    // Breakfast only on Tuesday
                    DevReservation(
                        childId = child.id,
                        date = tuesday,
                        startTime = LocalTime.of(8, 0),
                        endTime = LocalTime.of(9, 0),
                        createdBy = employee.evakaUserId,
                    ),
                )
                .forEach { tx.insert(it) }
        }

        createAndSendNekkuOrder(client, db, group.id, monday, 0.9)
        createAndSendNekkuOrder(client, db, group.id, tuesday, 0.9)

        assertEquals(
            listOf(
                NekkuClient.NekkuOrders(
                    listOf(
                        NekkuClient.NekkuOrder(
                            monday.toString(),
                            "2501K6090",
                            group.id.toString(),
                            listOf(NekkuClient.Item("31000018", 1, null)),
                            group.name,
                        )
                    ),
                    dryRun = false,
                ),
                NekkuClient.NekkuOrders(
                    listOf(
                        NekkuClient.NekkuOrder(
                            tuesday.toString(),
                            "2501K6090",
                            group.id.toString(),
                            listOf(NekkuClient.Item("31000018", 1, null)),
                            group.name,
                        )
                    ),
                    dryRun = false,
                ),
            ),
            client.orders,
        )
    }

    @Test
    fun `Order is not done if customer has not set a weekday in Nekku`() {
        val monday = LocalDate.of(2025, 4, 14)
        val tuesday = LocalDate.of(2025, 4, 15)

        // First create all of the basic backgrounds like
        // Customer numbers
        var client =
            TestNekkuClient(
                customers =
                    listOf(
                        NekkuApiCustomer(
                            "2501K6089",
                            "Ahvenojan päiväkoti",
                            "Varhaiskasvatus",
                            listOf(
                                CustomerApiType(
                                    listOf(NekkuCustomerApiWeekday.TUESDAY),
                                    "100-lasta",
                                )
                            ),
                        )
                    ),
                nekkuProducts = nekkuProductsForOrder,
            )
        fetchAndUpdateNekkuCustomers(client, db)
        // products
        fetchAndUpdateNekkuProducts(client, db)
        // Daycare with groups
        val area = DevCareArea()
        val daycare =
            DevDaycare(
                areaId = area.id,
                mealtimeBreakfast = TimeRange(LocalTime.of(8, 0), LocalTime.of(8, 20)),
                mealtimeLunch = TimeRange(LocalTime.of(11, 15), LocalTime.of(11, 45)),
                mealtimeSnack = TimeRange(LocalTime.of(13, 30), LocalTime.of(13, 50)),
            )
        val group = DevDaycareGroup(daycareId = daycare.id, nekkuCustomerNumber = "2501K6089")
        val employee = DevEmployee()

        // Children with placements in the group and they are not absent
        val child = DevPerson()

        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(group)
            tx.insert(employee)
            tx.insert(child, DevPersonType.CHILD)
            tx.insert(
                    DevPlacement(
                        childId = child.id,
                        unitId = daycare.id,
                        startDate = monday,
                        endDate = tuesday,
                    )
                )
                .also { placementId ->
                    tx.insert(
                        DevDaycareGroupPlacement(
                            daycarePlacementId = placementId,
                            daycareGroupId = group.id,
                            startDate = monday,
                            endDate = tuesday,
                        )
                    )
                }
            listOf(
                    // Two meals on Monday
                    DevReservation(
                        childId = child.id,
                        date = monday,
                        startTime = LocalTime.of(8, 0),
                        endTime = LocalTime.of(12, 0),
                        createdBy = employee.evakaUserId,
                    ),
                    // Breakfast only on Tuesday
                    DevReservation(
                        childId = child.id,
                        date = tuesday,
                        startTime = LocalTime.of(8, 0),
                        endTime = LocalTime.of(9, 0),
                        createdBy = employee.evakaUserId,
                    ),
                )
                .forEach { tx.insert(it) }
        }

        createAndSendNekkuOrder(client, db, group.id, monday, 0.9)
        createAndSendNekkuOrder(client, db, group.id, tuesday, 0.9)

        assertEquals(
            listOf(
                NekkuClient.NekkuOrders(
                    listOf(
                        NekkuClient.NekkuOrder(
                            tuesday.toString(),
                            "2501K6089",
                            group.id.toString(),
                            listOf(NekkuClient.Item("31000010", 1, null)),
                            group.name,
                        )
                    ),
                    dryRun = false,
                )
            ),
            client.orders,
        )
    }

    private fun getNekkuWeeklyJobs() =
        db.read { tx ->
            tx.createQuery { sql("SELECT payload FROM async_job WHERE type = 'SendNekkuOrder'") }
                .map { jsonColumn<AsyncJob.SendNekkuOrder>("payload") }
                .toList()
        }

    private fun getNekkuDailyJobs() =
        db.read { tx ->
            tx.createQuery {
                    sql("SELECT payload FROM async_job WHERE type = 'SendNekkuDailyOrder'")
                }
                .map { jsonColumn<AsyncJob.SendNekkuOrder>("payload") }
                .toList()
        }
}

class TestNekkuClient(
    private val customers: List<NekkuApiCustomer> = emptyList(),
    private val specialDiets: List<NekkuApiSpecialDiet> = emptyList(),
    private val nekkuProducts: List<NekkuApiProduct> = emptyList(),
) : NekkuClient {
    val orders = mutableListOf<NekkuClient.NekkuOrders>()

    override fun getCustomers(): List<NekkuApiCustomer> {
        return customers
    }

    override fun getSpecialDiets(): List<NekkuApiSpecialDiet> {
        return specialDiets
    }

    override fun getProducts(): List<NekkuApiProduct> {
        return nekkuProducts
    }

    override fun createNekkuMealOrder(nekkuOrders: NekkuClient.NekkuOrders): NekkuOrderResult {
        orders.add(nekkuOrders)

        return NekkuOrderResult(
            message = "Input ok, 5 orders would be created.",
            created = listOf("12345", "65432"),
            cancelled = emptyList(),
        )
    }
}
