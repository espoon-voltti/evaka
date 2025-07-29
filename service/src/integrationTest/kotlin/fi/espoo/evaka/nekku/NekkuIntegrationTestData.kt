package fi.espoo.evaka.nekku

import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import java.time.LocalDate
import java.time.LocalTime

val now = HelsinkiDateTime.of(LocalDate.of(2025, 5, 12), LocalTime.of(9, 50))

val basicTestClientCustomers =
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

val basicTestClientWithAnotherCustomerNumber =
    listOf(
        NekkuApiCustomer(
            "2501K6090",
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

val nekkuProducts =
    listOf(
        NekkuApiProduct(
            "Ateriapalvelu 1 kasvis",
            "31000010",
            "",
            listOf("alle-50-lasta", "alle-100-lasta"),
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
        NekkuApiProduct(
            "Ateriapalvelu 1 aamupala er",
            "31000020",
            "2",
            listOf("100-lasta"),
            listOf(NekkuProductMealTime.BREAKFAST),
            null,
        ),
        NekkuApiProduct(
            "Ateriapalvelu 1 lounas er",
            "31000021",
            "2",
            listOf("100-lasta"),
            listOf(NekkuProductMealTime.LUNCH),
            null,
        ),
        NekkuApiProduct(
            "Ateriapalvelu 1 välipala er",
            "31000022",
            "2",
            listOf("100-lasta"),
            listOf(NekkuProductMealTime.SNACK),
            null,
        ),
        NekkuApiProduct(
            "Ateriapalvelu 1 aamupala kasvis er",
            "31000023",
            "2",
            listOf("100-lasta"),
            listOf(NekkuProductMealTime.BREAKFAST),
            NekkuApiProductMealType.Kasvis,
        ),
        NekkuApiProduct(
            "Ateriapalvelu 1 lounas kasvis er",
            "31000024",
            "2",
            listOf("100-lasta"),
            listOf(NekkuProductMealTime.LUNCH),
            NekkuApiProductMealType.Kasvis,
        ),
        NekkuApiProduct(
            "Ateriapalvelu 1 välipala kasvis er",
            "31000025",
            "2",
            listOf("100-lasta"),
            listOf(NekkuProductMealTime.SNACK),
            NekkuApiProductMealType.Kasvis,
        ),
    )

val nekkuProductsForErrorOrder =
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
            "Ateriapalvelu 1 aamupala",
            "31000010",
            "",
            listOf("100-lasta"),
            listOf(NekkuProductMealTime.BREAKFAST),
            null,
        ),
    )

fun getNekkuSpecialDiet(): NekkuApiSpecialDiet =
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
                    NekkuSpecialDietOption(1, "Kananmunaton ruokavalio", "Kananmunaton ruokavalio"),
                    NekkuSpecialDietOption(2, "Sianlihaton ruokavalio", "Sianlihaton ruokavalio"),
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
                    NekkuSpecialDietOption(5, "Laktoositon ruokavalio", "Laktoositon ruokavalio"),
                ),
            ),
        ),
    )
