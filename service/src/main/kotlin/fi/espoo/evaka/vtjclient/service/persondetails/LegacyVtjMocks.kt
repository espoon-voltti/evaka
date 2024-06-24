// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.vtjclient.service.persondetails

import fi.espoo.evaka.vtjclient.dto.Nationality
import fi.espoo.evaka.vtjclient.dto.NativeLanguage
import fi.espoo.evaka.vtjclient.dto.PersonAddress
import fi.espoo.evaka.vtjclient.dto.RestrictedDetails
import java.time.LocalDate

fun legacyMockVtjDataset() =
    MockVtjDataset(
        persons =
            listOf(
                MockVtjPerson(
                    "Olavi Eino Väinö Valtteri",
                    "Heinjoki",
                    "070713A931J",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Kamreerintie 3", "01640", "Espoo"),
                    NativeLanguage.FI,
                    nationalities = listOf(Nationality(countryName = "Suomi", countryCode = "FIN")),
                    residenceCode = "hogfors_kamreerintie3"
                ),
                MockVtjPerson(
                    "Tapio Eeli Veeti Veikko",
                    "Heinjoki",
                    "070413A915L",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Kamreerintie 7", "02130", "Espoo"),
                    NativeLanguage.FI
                ),
                MockVtjPerson(
                    "Annikki Isla Mila Matilda",
                    "Karjala",
                    "080116A918P",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Kaivomestarinkatu 6", "00340", "Espoo"),
                    NativeLanguage.FI
                ),
                MockVtjPerson(
                    "Ilmari Emil Eemil Joona",
                    "Aspö",
                    "140714A9542",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Kamreerintie 10", "02160", "Espoo"),
                    NativeLanguage.FI
                ),
                MockVtjPerson(
                    "Maria Ellen Aava Seela",
                    "Alajärvi",
                    "280916A9259",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Kaivomestarinkatu 4", "02230", "Espoo"),
                    NativeLanguage.FI
                ),
                MockVtjPerson(
                    "Maria Lumi Matilda",
                    "Högfors",
                    "101016A935T",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Kamreerintie 5", "02110", "Espoo"),
                    NativeLanguage.FI,
                    residenceCode = "hogfors_kamreerintie5"
                ),
                MockVtjPerson(
                    "Liisa Maria Anneli",
                    "Finström",
                    "231090-912J",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Kaivomestarinkatu 9", "02100", "Espoo"),
                    NativeLanguage.FI
                ),
                MockVtjPerson(
                    "Tero Petteri Mikael",
                    "Testaaja",
                    "010170-999R",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Raivaajantie 12", "01640", "Espoo"),
                    NativeLanguage.FI
                ),
                MockVtjPerson(
                    "Olavi Kasper Veikko",
                    "Jokioinen",
                    "090514A974V",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Kaivomestarinkatu 12", "02130", "Espoo"),
                    NativeLanguage.FI
                ),
                MockVtjPerson(
                    "Johanna Vilja Minttu",
                    "Heinjoki",
                    "210216A911N",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Kamreerintie 3", "01640", "Espoo"),
                    NativeLanguage.FI,
                    residenceCode = "hogfors_kamreerintie3"
                ),
                MockVtjPerson(
                    "Helena Marjatta Sofia",
                    "Huittinen",
                    "091171-9975",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Raivaajantie 4", "02170", "Espoo"),
                    NativeLanguage.FI
                ),
                MockVtjPerson(
                    "Kaarina Elli Enni Iisa",
                    "Jurva",
                    "070115A9583",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Raivaajantie 7", "02210", "Espoo"),
                    NativeLanguage.FI
                ),
                MockVtjPerson(
                    "Mikael Jooa Aapo",
                    "Demo",
                    "060118A9908",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Raivaajantie 9", "02240", "Espoo"),
                    NativeLanguage.FI
                ),
                MockVtjPerson(
                    "Parkkonen",
                    "Äåöàáâãæçð Èèéêë-Ììíîïñòóôõ Øøßþùúûüýÿ",
                    "050391-999B",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Raivaajantie 17", "02140", "Espoo"),
                    NativeLanguage.FI
                ),
                MockVtjPerson(
                    "Antero Aaron Alvar",
                    "Huittinen",
                    "150817A939T",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Raivaajantie 4", "02170", "Espoo"),
                    NativeLanguage.FI
                ),
                MockVtjPerson(
                    "Tapio Antero Kalevi",
                    "Joensuu",
                    "090693-990K",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Kaivomestarinkatu 8", "01640", "Espoo"),
                    NativeLanguage.FI
                ),
                MockVtjPerson(
                    "Liisa Pihla Emilia Sara",
                    "Haaga",
                    "030816A910V",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Kaivomestarinkatu 7", "00370", "Espoo"),
                    NativeLanguage.FI
                ),
                MockVtjPerson(
                    "Juhani Rasmus Akseli",
                    "Jurva",
                    "070817A9876",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Raivaajantie 7", "02210", "Espoo"),
                    NativeLanguage.FI
                ),
                MockVtjPerson(
                    "Helena Elsa Kerttu Iina",
                    "Muurinen",
                    "021113A929F",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Raivaajantie 14", "02110", "Espoo"),
                    NativeLanguage.FI
                ),
                MockVtjPerson(
                    "Juhani Rasmus Akseli",
                    "Anttola",
                    "180613A920F",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Kaivomestarinkatu 10", "02110", "Espoo"),
                    NativeLanguage.FI
                ),
                MockVtjPerson(
                    "Ville",
                    "Vilkas",
                    "311299-999E",
                    RestrictedDetails(enabled = false),
                    PersonAddress(
                        "Toistie 33",
                        "02230",
                        "Espoo",
                        streetAddressSe = "Andervägen 33",
                        postOfficeSe = "Esbo"
                    ),
                    NativeLanguage.FI,
                    nationalities = listOf(Nationality(countryName = "Suomi", countryCode = "FIN")),
                    residenceCode = "hogfors_toistie33"
                ),
                MockVtjPerson(
                    "Sirkka-Liisa Marja-Leena Minna-Mari Anna-Kaisa",
                    "Korhonen-Hämäläinen",
                    "270372-905L",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Kamreerintie 2", "00370", "Espoo"),
                    NativeLanguage.FI,
                    nationalities = listOf(Nationality(countryName = "Suomi", countryCode = "FIN")),
                    residenceCode = "hogfors_kamreerintie2"
                ),
                MockVtjPerson(
                    "Sofia Minea Hilla Lumi",
                    "Hattula",
                    "090316A948X",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Kaivomestarinkatu 5", "02240", "Espoo"),
                    NativeLanguage.FI
                ),
                MockVtjPerson(
                    "Micky",
                    "Doe",
                    "010316A1235",
                    RestrictedDetails(enabled = false),
                    PersonAddress(
                        "Kamreerintie 2",
                        "02770",
                        "Espoo",
                        streetAddressSe = "Kamrersgatan 2",
                        postOfficeSe = "Esbo"
                    ),
                    NativeLanguage.FI,
                    nationalities = listOf(Nationality(countryName = "Suomi", countryCode = "FIN")),
                    residenceCode = "hogfors_kamreerintie2"
                ),
                MockVtjPerson(
                    "Kaarina Helena Annikki Anneli",
                    "Honkilahti",
                    "280674-990X",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Kamreerintie 6", "02120", "Espoo"),
                    NativeLanguage.FI,
                    residenceCode = "hogfors_kamreerintie6"
                ),
                MockVtjPerson(
                    "Hannele Johanna Kaarina Anneli",
                    "Hattula",
                    "150794-9463",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Kaivomestarinkatu 1", "02180", "Espoo"),
                    NativeLanguage.FI
                ),
                MockVtjPerson(
                    "Helena Emma Sofia Ronja",
                    "Anttola",
                    "220317A982N",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Raivaajantie 1", "02140", "Espoo"),
                    NativeLanguage.FI
                ),
                MockVtjPerson(
                    "Anna Maria Helena Sofia",
                    "Haaga",
                    "290385-9900",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Kaivomestarinkatu 2", "02200", "Espoo"),
                    NativeLanguage.FI
                ),
                MockVtjPerson(
                    "Matti Mikael Joel Kasper",
                    "Kalanti",
                    "231015A953H",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Kamreerintie 8", "02140", "Espoo"),
                    NativeLanguage.FI
                ),
                MockVtjPerson(
                    "Kaarina Marjatta Anna Liisa",
                    "Högfors",
                    "170590-9540",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Kamreerintie 4", "02110", "Espoo"),
                    NativeLanguage.FI,
                    residenceCode = "hogfors_kamreerintie4"
                ),
                MockVtjPerson(
                    "Helena Elsa Kerttu Iina",
                    "Haga",
                    "241016A927K",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Kaivomestarinkatu 3", "02210", "Espoo"),
                    NativeLanguage.FI
                ),
                MockVtjPerson(
                    "John",
                    "Doe",
                    "010180-1232",
                    RestrictedDetails(enabled = false),
                    PersonAddress(
                        "Kamreerintie 2",
                        "02770",
                        "Espoo",
                        streetAddressSe = "Kamrersgatan 2",
                        postOfficeSe = "Esbo"
                    ),
                    NativeLanguage.FI,
                    nationalities = listOf(Nationality(countryName = "Suomi", countryCode = "FIN")),
                    residenceCode = "hogfors_kamreerintie2"
                ),
                MockVtjPerson(
                    "Kalevi Matias Aatos",
                    "Hattula",
                    "290615A9203",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Kaivomestarinkatu 5", "02240", "Espoo"),
                    NativeLanguage.FI
                ),
                MockVtjPerson(
                    "Joan",
                    "Doe",
                    "010279-123L",
                    RestrictedDetails(enabled = false),
                    PersonAddress(
                        "Kamreerintie 2",
                        "02770",
                        "Espoo",
                        streetAddressSe = "Kamrersgatan 2",
                        postOfficeSe = "Esbo"
                    ),
                    NativeLanguage.FI,
                    nationalities = listOf(Nationality(countryName = "Suomi", countryCode = "FIN")),
                    residenceCode = "hogfors_kamreerintie2"
                ),
                MockVtjPerson(
                    "Kaarina Elli Enni Iisa",
                    "Anttola",
                    "171017A946X",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Kaivomestarinkatu 10", "02110", "Espoo"),
                    NativeLanguage.FI
                ),
                MockVtjPerson(
                    "Johanna Linnea Vilma",
                    "Haaga",
                    "240816A910D",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Kaivomestarinkatu 2", "02200", "Espoo"),
                    NativeLanguage.FI
                ),
                MockVtjPerson(
                    "Johanna Aada Ella Nelli",
                    "Jokioinen",
                    "210317A994D",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Kaivomestarinkatu 12", "02130", "Espoo"),
                    NativeLanguage.FI
                ),
                MockVtjPerson(
                    "Ilmari Oiva Samuel",
                    "Tammi",
                    "150818A995V",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Raivaajantie 8", "02230", "Espoo"),
                    NativeLanguage.FI
                ),
                MockVtjPerson(
                    "Kaarina Liisa Annikki Sofia",
                    "Heinjoki",
                    "271170-917X",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Kamreerintie 3", "01640", "Espoo"),
                    NativeLanguage.FI,
                    residenceCode = "hogfors_kamreerintie3"
                ),
                MockVtjPerson(
                    "Terttu Sylvi Sofia",
                    "Kankaanperä",
                    "020474-9187",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Raivaajantie 15", "02120", "Espoo"),
                    NativeLanguage.FI
                ),
                MockVtjPerson(
                    "Kaarina Veera Nelli",
                    "Karhula",
                    "160616A978U",
                    RestrictedDetails(enabled = false),
                    PersonAddress(
                        "Kamreerintie 1",
                        "00340",
                        "Espoo",
                        streetAddressSe = "Kamrersgatan 1",
                        postOfficeSe = "Esbo"
                    ),
                    NativeLanguage.FI,
                    nationalities = listOf(Nationality(countryName = "Suomi", countryCode = "FIN")),
                    residenceCode = "hogfors_kamreerintie1"
                ),
                MockVtjPerson(
                    "Marjatta Viola Anni",
                    "Aspö",
                    "271016A9209",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Kamreerintie 10", "02160", "Espoo"),
                    NativeLanguage.FI
                ),
                MockVtjPerson(
                    "Anneli Alisa Siiri",
                    "Hattula",
                    "270816A974D",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Kaivomestarinkatu 1", "02180", "Espoo"),
                    NativeLanguage.FI
                ),
                MockVtjPerson(
                    "Kaarina Iida Amanda",
                    "Äåöàáâãæçð Èèéêë-Ììíîïñòóôõ Øøßþùúûüýÿ",
                    "110414A9962",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Raivaajantie 17", "02140", "Espoo"),
                    NativeLanguage.FI
                ),
                MockVtjPerson(
                    "Anna Viivi Alma",
                    "Sotka",
                    "190513A9454",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Raivaajantie 19", "02160", "Espoo"),
                    NativeLanguage.FI
                ),
                MockVtjPerson(
                    "Tapani Niklas Luka",
                    "Marttila",
                    "040918A972U",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Raivaajantie 13", "02100", "Espoo"),
                    NativeLanguage.FI
                ),
                MockVtjPerson(
                    "Annikki Nella Alina",
                    "Heinjoki",
                    "100716A998D",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Kamreerintie 7", "02130", "Espoo"),
                    NativeLanguage.FI
                ),
                MockVtjPerson(
                    "Antero Toivo Peetu Oiva",
                    "Haga",
                    "020415A9833",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Kaivomestarinkatu 3", "02210", "Espoo"),
                    NativeLanguage.FI
                ),
                MockVtjPerson(
                    "Niilo",
                    "Nimettömänpoika",
                    "311218A999J",
                    RestrictedDetails(enabled = false),
                    PersonAddress(
                        "Kankkulankaivo 1",
                        "00340",
                        "Espoo",
                        streetAddressSe = "Kankkulaswell 1",
                        postOfficeSe = "Esbo"
                    ),
                    NativeLanguage.FI,
                    nationalities = listOf(Nationality(countryName = "Suomi", countryCode = "FIN")),
                    residenceCode = "nimeton_kankkulankaivo1"
                ),
                MockVtjPerson(
                    "Porri Hatter",
                    "Karhula",
                    "160620A999J",
                    RestrictedDetails(enabled = true),
                    PersonAddress("", "", "", streetAddressSe = "", postOfficeSe = ""),
                    NativeLanguage.FI,
                    nationalities = listOf(Nationality(countryName = "Suomi", countryCode = "FIN")),
                    residenceCode = ""
                ),
                MockVtjPerson(
                    "Johanna Linnea Vilma",
                    "Kankaanperä",
                    "160914A969E",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Raivaajantie 15", "02120", "Espoo"),
                    NativeLanguage.FI
                ),
                MockVtjPerson(
                    "Ilmari Oiva Samuel",
                    "Finström",
                    "161015A9862",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Kaivomestarinkatu 9", "02100", "Espoo"),
                    NativeLanguage.FI
                ),
                MockVtjPerson(
                    "Marjatta Viola Anni",
                    "Haahka",
                    "140514A966U",
                    RestrictedDetails(enabled = true),
                    PersonAddress("", "", ""),
                    NativeLanguage.FI
                ),
                MockVtjPerson(
                    "Liisa Oona Ilona",
                    "Kalanti",
                    "100916A979D",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Kamreerintie 8", "02140", "Espoo"),
                    NativeLanguage.FI
                ),
                MockVtjPerson(
                    "Liisa Pihla Emilia Sara",
                    "Demo",
                    "280714A905H",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Raivaajantie 10", "00340", "Espoo"),
                    NativeLanguage.FI
                ),
                MockVtjPerson(
                    "Kaarina Iida Amanda",
                    "Aspö",
                    "071116A9516",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Kamreerintie 11", "02170", "Espoo"),
                    NativeLanguage.FI
                ),
                MockVtjPerson(
                    "",
                    "",
                    "311288A999L",
                    RestrictedDetails(enabled = false),
                    PersonAddress(
                        "Kankkulankaivo 1",
                        "00340",
                        "Espoo",
                        streetAddressSe = "Kankkulaswell 1",
                        postOfficeSe = "Esbo"
                    ),
                    NativeLanguage.FI,
                    nationalities = listOf(Nationality(countryName = "Suomi", countryCode = "FIN")),
                    residenceCode = "nimeton_kankkulankaivo1"
                ),
                MockVtjPerson(
                    "Olavi Aleksi Nooa Samuel",
                    "Haaga",
                    "240413A9634",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Kaivomestarinkatu 2", "02200", "Espoo"),
                    NativeLanguage.FI
                ),
                MockVtjPerson(
                    "Johannes Joona Benjamin",
                    "Eno",
                    "280315A994D",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Kaivomestarinkatu 11", "02120", "Espoo"),
                    NativeLanguage.FI
                ),
                MockVtjPerson(
                    "Antero Onni Leevi Aatu",
                    "Högfors",
                    "071013A960W",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Kamreerintie 4", "02100", "Espoo"),
                    NativeLanguage.FI,
                    residenceCode = "hogfors_kamreerintie4"
                ),
                MockVtjPerson(
                    "Matti Lauri Otso",
                    "Demo",
                    "200418A9990",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Raivaajantie 10", "00340", "Espoo"),
                    NativeLanguage.FI
                ),
                MockVtjPerson(
                    "Liisa Oona Ilona",
                    "Silkkiuikku",
                    "061014A908U",
                    RestrictedDetails(enabled = true),
                    PersonAddress("", "", ""),
                    NativeLanguage.FI
                ),
                MockVtjPerson(
                    "Tapani Niilo Oliver Alvar",
                    "Högfors",
                    "050814A973N",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Kamreerintie 5", "02110", "Espoo"),
                    NativeLanguage.FI,
                    residenceCode = "hogfors_kamreerintie5"
                ),
                MockVtjPerson(
                    "Eemeli Mikael Tapio",
                    "Muurinen",
                    "020898-945H",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Raivaajantie 14", "02110", "Espoo"),
                    NativeLanguage.FI
                ),
                MockVtjPerson(
                    "Helena Kaarina Anna Johanna",
                    "Hattula",
                    "130894-917N",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Kaivomestarinkatu 5", "02240", "Espoo"),
                    NativeLanguage.FI
                ),
                MockVtjPerson(
                    "Marja-Leena Minna-Mari Sirkka-Liisa Anna-Kaisa",
                    "Finström",
                    "180213A909W",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Raivaajantie 3", "02160", "Espoo"),
                    NativeLanguage.FI
                ),
                MockVtjPerson(
                    "Anna Marjatta Helena Anneli",
                    "Kalanti",
                    "231182-9661",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Kamreerintie 8", "02140", "Espoo"),
                    NativeLanguage.FI
                ),
                MockVtjPerson(
                    "Marja-Leena Minna-Mari Sirkka-Liisa Anna-Kaisa",
                    "Jaala",
                    "170417A942Y",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Raivaajantie 2", "02150", "Espoo"),
                    NativeLanguage.FI
                ),
                MockVtjPerson(
                    "Ilmari Emil Eemil Joona",
                    "Haahka",
                    "260213A9125",
                    RestrictedDetails(enabled = true),
                    PersonAddress("", "", ""),
                    NativeLanguage.FI
                ),
                MockVtjPerson(
                    "Helena Sara Hilda",
                    "Högfors",
                    "230916A910K",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Kamreerintie 4", "02100", "Espoo"),
                    NativeLanguage.FI,
                    residenceCode = "hogfors_kamreerintie4"
                ),
                MockVtjPerson(
                    "Maria Helena Marjatta",
                    "Jaala",
                    "210593-9430",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Raivaajantie 2", "02150", "Espoo"),
                    NativeLanguage.FI
                ),
                MockVtjPerson(
                    "Heikki Tapio Eemeli",
                    "Haahka",
                    "300190-9257",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Raivaajantie 18", "02150", "Espoo"),
                    NativeLanguage.FI
                ),
                MockVtjPerson(
                    "Johannes Leo Elias Eemi",
                    "Korhonen-Hämäläinen",
                    "220314A983X",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Kamreerintie 2", "00370", "Espoo"),
                    NativeLanguage.FI,
                    nationalities = listOf(Nationality(countryName = "Suomi", countryCode = "FIN")),
                    residenceCode = "hogfors_kamreerintie2"
                ),
                MockVtjPerson(
                    "Jari-Petteri Mukkelis-Makkelis Vetelä-Viljami Eelis-Juhani",
                    "Karhula",
                    "070714A9126",
                    RestrictedDetails(enabled = false),
                    PersonAddress(
                        "Kamreerintie 1",
                        "00340",
                        "Espoo",
                        streetAddressSe = "Kamrersgatan 1",
                        postOfficeSe = "Esbo"
                    ),
                    NativeLanguage.FI,
                    nationalities = listOf(Nationality(countryName = "Suomi", countryCode = "FIN")),
                    residenceCode = "hogfors_kamreerintie1"
                ),
                MockVtjPerson(
                    "Hannele Johanna Anneli Hannele",
                    "Haga",
                    "130973-9825",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Kaivomestarinkatu 3", "02210", "Espoo"),
                    NativeLanguage.FI
                ),
                MockVtjPerson(
                    "Mikael Daniel Lenni Benjamin",
                    "Sotka",
                    "100915A900L",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Raivaajantie 19", "02160", "Espoo"),
                    NativeLanguage.FI
                ),
                MockVtjPerson(
                    "Teemu Taneli Tapio",
                    "Testaaja",
                    "010101-123N",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Raivaajantie 11", "00370", "Espoo"),
                    NativeLanguage.FI
                ),
                MockVtjPerson(
                    "Unelma",
                    "Aapinen",
                    "150515-999T",
                    RestrictedDetails(enabled = false),
                    PersonAddress(
                        "Aapiskatu 1",
                        "00340",
                        "Espoo",
                        streetAddressSe = "Aapisväg 1",
                        postOfficeSe = "Esbo"
                    ),
                    NativeLanguage.FI,
                    nationalities = listOf(Nationality(countryName = "Suomi", countryCode = "FIN")),
                    dateOfDeath = LocalDate.of(2020, 6, 1),
                    residenceCode = "aapinen_1"
                ),
                MockVtjPerson(
                    "Johannes Viljami Valtteri Benjamin",
                    "Hattula",
                    "160515A975N",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Kaivomestarinkatu 1", "02180", "Espoo"),
                    NativeLanguage.FI
                ),
                MockVtjPerson(
                    "Marjatta Selma Minttu Vilja",
                    "Finström",
                    "190316A916B",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Kaivomestarinkatu 9", "02100", "Espoo"),
                    NativeLanguage.FI
                ),
                MockVtjPerson(
                    "Anna Viivi Alma",
                    "Joensuu",
                    "281116A9003",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Kamreerintie 9", "02150", "Espoo"),
                    NativeLanguage.FI
                ),
                MockVtjPerson(
                    "Matti Tapani Antero Mikael",
                    "Karjala",
                    "010882-983Y",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Kaivomestarinkatu 6", "00340", "Espoo"),
                    NativeLanguage.FI
                ),
                MockVtjPerson(
                    "Anneli Alisa Siiri",
                    "Simakuutio",
                    "291013A950S",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Raivaajantie 16", "02130", "Espoo"),
                    NativeLanguage.FI
                ),
                MockVtjPerson(
                    "Tapani Aatu Valtteri",
                    "Jaala",
                    "031114A9203",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Raivaajantie 2", "02150", "Espoo"),
                    NativeLanguage.FI
                ),
                MockVtjPerson(
                    "Tapani Aatu Valtteri",
                    "Finström",
                    "040317A946W",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Raivaajantie 3", "02160", "Espoo"),
                    NativeLanguage.FI
                ),
                MockVtjPerson(
                    "Anna Venla Olivia Hilda",
                    "Joensuu",
                    "090116A9690",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Kaivomestarinkatu 8", "01640", "Espoo"),
                    NativeLanguage.FI
                ),
                MockVtjPerson(
                    "Helena Emma Sofia Ronja",
                    "Huittinen",
                    "051115A950L",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Raivaajantie 4", "02170", "Espoo"),
                    NativeLanguage.FI
                ),
                MockVtjPerson(
                    "Hillary",
                    "Foo",
                    "120220A995L",
                    RestrictedDetails(enabled = false),
                    PersonAddress(
                        "Kankkulankaivo 1",
                        "00340",
                        "Espoo",
                        streetAddressSe = "Kankkulaswell 1",
                        postOfficeSe = "Esbo"
                    ),
                    NativeLanguage.FI,
                    nationalities = listOf(Nationality(countryName = "Suomi", countryCode = "FIN")),
                    residenceCode = "hogfors_kamreerintie1"
                ),
                MockVtjPerson(
                    "Johanna Aada Ella Nelli",
                    "Joutsa",
                    "050314A946S",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Raivaajantie 5", "02180", "Espoo"),
                    NativeLanguage.FI
                ),
                MockVtjPerson(
                    "Mikael Ilmari Juhani Johannes",
                    "Högfors",
                    "220281-9456",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Kamreerintie 4", "02100", "Espoo"),
                    NativeLanguage.FI,
                    residenceCode = "hogfors_kamreerintie4"
                ),
                MockVtjPerson(
                    "Antero Aaron Alvar",
                    "Anttola",
                    "121114A921X",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Raivaajantie 1", "02140", "Espoo"),
                    NativeLanguage.FI
                ),
                MockVtjPerson(
                    "Annikki Isla Mila Matilda",
                    "Testaaja",
                    "210314A9784",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Raivaajantie 11", "00370", "Espoo"),
                    NativeLanguage.FI
                ),
                MockVtjPerson(
                    "Sirpa Maria Annikki",
                    "Silkkiuikku",
                    "150978-9025",
                    RestrictedDetails(enabled = true),
                    PersonAddress("", "", ""),
                    NativeLanguage.FI
                ),
                MockVtjPerson(
                    "Tapio Tapani Kalevi Matti",
                    "Heinjoki",
                    "071082-9435",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Kamreerintie 7", "02130", "Espoo"),
                    NativeLanguage.FI
                ),
                MockVtjPerson(
                    "Kalevi Matias Aatos",
                    "Testaaja",
                    "271018A911H",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Raivaajantie 12", "01640", "Espoo"),
                    NativeLanguage.FI
                ),
                MockVtjPerson(
                    "Tapani Matti Kalevi Olavi",
                    "Joensuu",
                    "221071-9131",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Kamreerintie 9", "02150", "Espoo"),
                    NativeLanguage.FI
                ),
                MockVtjPerson(
                    "Anna Kaarina Marjatta",
                    "Demo",
                    "010170-960F",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Raivaajantie 9", "02240", "Espoo"),
                    NativeLanguage.FI
                ),
                MockVtjPerson(
                    "Sofia Minea Hilla Lumi",
                    "Testaaja",
                    "290913A962B",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Raivaajantie 12", "01640", "Espoo"),
                    NativeLanguage.FI
                ),
                MockVtjPerson(
                    "Johannes Viljami Valtteri Benjamin",
                    "Simakuutio",
                    "280618A9750",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Raivaajantie 16", "02130", "Espoo"),
                    NativeLanguage.FI
                ),
                MockVtjPerson(
                    "Tauno Tapani Kalevi",
                    "Tammi",
                    "120482-955X",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Raivaajantie 8", "02230", "Espoo"),
                    NativeLanguage.FI
                ),
                MockVtjPerson(
                    "Maria Ellen Aava Seela",
                    "Marttila",
                    "170714A911L",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Raivaajantie 13", "02100", "Espoo"),
                    NativeLanguage.FI
                ),
                MockVtjPerson(
                    "Tapio Eemeli Anton",
                    "Karjala",
                    "110913A9434",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Kaivomestarinkatu 6", "00340", "Espoo"),
                    NativeLanguage.FI
                ),
                MockVtjPerson(
                    "Mikael Jooa Aapo",
                    "Joensuu",
                    "290413A902C",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Kaivomestarinkatu 8", "01640", "Espoo"),
                    NativeLanguage.FI
                ),
                MockVtjPerson(
                    "Helena Sofia Johanna",
                    "Jokioinen",
                    "090275-9724",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Kaivomestarinkatu 12", "02130", "Espoo"),
                    NativeLanguage.FI
                ),
                MockVtjPerson(
                    "Juhani Julius Viljami Akseli",
                    "Aspö",
                    "051015A981T",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Kamreerintie 11", "02170", "Espoo"),
                    NativeLanguage.FI
                ),
                MockVtjPerson(
                    "Matti Lauri Otso",
                    "Haaga",
                    "120313A995L",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Kaivomestarinkatu 7", "00370", "Espoo"),
                    NativeLanguage.FI
                ),
                MockVtjPerson(
                    "Hannele Kaarina Marjatta",
                    "Finström",
                    "060195-966B",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Raivaajantie 3", "02160", "Espoo"),
                    NativeLanguage.FI
                ),
                MockVtjPerson(
                    "Anneli Lilja Helmi Veera",
                    "Eno",
                    "020717A997F",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Kaivomestarinkatu 11", "02120", "Espoo"),
                    NativeLanguage.FI
                ),
                MockVtjPerson(
                    "Tapani Kalevi Tapio Antero",
                    "Alajärvi",
                    "050482-9741",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Kaivomestarinkatu 4", "02230", "Espoo"),
                    NativeLanguage.FI
                ),
                MockVtjPerson(
                    "Johannes Olavi Antero Tapio",
                    "Karhula",
                    "070644-937X",
                    RestrictedDetails(enabled = false),
                    PersonAddress(
                        "Kamreerintie 1",
                        "00340",
                        "Espoo",
                        streetAddressSe = "Kamrersgatan 1",
                        postOfficeSe = "Esbo"
                    ),
                    NativeLanguage.FI,
                    nationalities = listOf(Nationality(countryName = "Suomi", countryCode = "FIN")),
                    residenceCode = "hogfors_kamreerintie1"
                ),
                MockVtjPerson(
                    "Nordea Nalle Arne",
                    "Demo",
                    "210281-9988",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Raivaajantie 10", "00340", "Espoo"),
                    NativeLanguage.FI
                ),
                MockVtjPerson(
                    "Tapani Niklas Luka",
                    "Alajärvi",
                    "180214A928S",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Kaivomestarinkatu 4", "02230", "Espoo"),
                    NativeLanguage.FI
                ),
                MockVtjPerson(
                    "Olavi Aleksi Nooa Samuel",
                    "Kankaanperä",
                    "181118A9979",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Raivaajantie 15", "02120", "Espoo"),
                    NativeLanguage.FI
                ),
                MockVtjPerson(
                    "Ricky",
                    "Doe",
                    "010617A123U",
                    RestrictedDetails(enabled = false),
                    PersonAddress(
                        "Kamreerintie 2",
                        "02770",
                        "Espoo",
                        streetAddressSe = "Kamrersgatan 2",
                        postOfficeSe = "Esbo"
                    ),
                    NativeLanguage.FI,
                    nationalities = listOf(Nationality(countryName = "Suomi", countryCode = "FIN")),
                    residenceCode = "hogfors_kamreerintie2"
                ),
                MockVtjPerson(
                    "Sofia Iina Seela",
                    "Honkilahti",
                    "210216A977T",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Kamreerintie 6", "02120", "Espoo"),
                    NativeLanguage.FI,
                    residenceCode = "hogfors_kamreerintie6"
                ),
                MockVtjPerson(
                    "Johannes Joona Benjamin",
                    "Finström",
                    "130217A995F",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Raivaajantie 6", "02200", "Espoo"),
                    NativeLanguage.FI
                ),
                MockVtjPerson(
                    "Antero Toivo Peetu Oiva",
                    "Muurinen",
                    "260918A9787",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Raivaajantie 14", "02110", "Espoo"),
                    NativeLanguage.FI
                ),
                MockVtjPerson(
                    "Matti Mikael Joel Kasper",
                    "Silkkiuikku",
                    "010713A933R",
                    RestrictedDetails(enabled = true),
                    PersonAddress("", "", ""),
                    NativeLanguage.FI
                ),
                MockVtjPerson(
                    "Kaarina Helena Marjatta",
                    "Jurva",
                    "150288-971A",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Raivaajantie 7", "02210", "Espoo"),
                    NativeLanguage.FI
                ),
                MockVtjPerson(
                    "Matti Olavi Antero",
                    "Finström",
                    "290393-9913",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Raivaajantie 6", "02200", "Espoo"),
                    NativeLanguage.FI
                ),
                MockVtjPerson(
                    "Helena Anna Maria",
                    "Joutsa",
                    "100774-9306",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Raivaajantie 5", "02180", "Espoo"),
                    NativeLanguage.FI
                ),
                MockVtjPerson(
                    "Olavi Tapio Johannes",
                    "Haaga",
                    "100373-9733",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Kaivomestarinkatu 7", "00370", "Espoo"),
                    NativeLanguage.FI
                ),
                MockVtjPerson(
                    "Marjatta Selma Minttu Vilja",
                    "Tammi",
                    "080515A969A",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Raivaajantie 8", "02230", "Espoo"),
                    NativeLanguage.FI
                ),
                MockVtjPerson(
                    "Anna Venla Olivia Hilda",
                    "Demo",
                    "100513A918E",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Raivaajantie 9", "02240", "Espoo"),
                    NativeLanguage.FI
                ),
                MockVtjPerson(
                    "Annikki Maria Anna",
                    "Eno",
                    "290377-9377",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Kaivomestarinkatu 11", "02120", "Espoo"),
                    NativeLanguage.FI
                ),
                MockVtjPerson(
                    "Seija Anna Kaarina",
                    "Sotka",
                    "031083-910S",
                    RestrictedDetails(enabled = true),
                    PersonAddress("", "", ""),
                    NativeLanguage.FI
                ),
                MockVtjPerson(
                    "Liisa Anna Kaarina Marjatta",
                    "Aspö",
                    "260888-990V",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Kamreerintie 10", "02160", "Espoo"),
                    NativeLanguage.FI
                ),
                MockVtjPerson(
                    "Kalevi Eetu Noel Aaron",
                    "Honkilahti",
                    "290315A982N",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Kamreerintie 6", "02120", "Espoo"),
                    NativeLanguage.FI,
                    residenceCode = "hogfors_kamreerintie6"
                ),
                MockVtjPerson(
                    "Tapio Eemeli Anton",
                    "Testaaja",
                    "190618A918B",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Raivaajantie 11", "00370", "Espoo"),
                    NativeLanguage.FI
                ),
                MockVtjPerson(
                    "Anneli Lilja Helmi Veera",
                    "Finström",
                    "120315A951P",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Raivaajantie 6", "02200", "Espoo"),
                    NativeLanguage.FI
                ),
                MockVtjPerson(
                    "Juhani Julius Viljami Akseli",
                    "Äåöàáâãæçð Èèéêë-Ììíîïñòóôõ Øøßþùúûüýÿ",
                    "020818A959A",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Raivaajantie 17", "02140", "Espoo"),
                    NativeLanguage.FI
                ),
                MockVtjPerson(
                    "Mikael Daniel Lenni Benjamin",
                    "Joensuu",
                    "050813A914H",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Kamreerintie 9", "02150", "Espoo"),
                    NativeLanguage.FI
                ),
                MockVtjPerson(
                    "Olavi Kasper Veikko",
                    "Joutsa",
                    "120217A923H",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Raivaajantie 5", "02180", "Espoo"),
                    NativeLanguage.FI
                ),
                MockVtjPerson(
                    "Ahto Olavi Antero",
                    "Simakuutio",
                    "170595-9151",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Raivaajantie 16", "02130", "Espoo"),
                    NativeLanguage.FI
                ),
                MockVtjPerson(
                    "Anneli Selma Iisa",
                    "Korhonen-Hämäläinen",
                    "051116A902A",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Kamreerintie 2", "00370", "Espoo"),
                    NativeLanguage.FI,
                    residenceCode = "hogfors_kamreerintie2"
                ),
                MockVtjPerson(
                    "Marjatta Liisa Annikki",
                    "Anttola",
                    "010495-965H",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Kaivomestarinkatu 10", "02110", "Espoo"),
                    NativeLanguage.FI
                ),
                MockVtjPerson(
                    "Anelma",
                    "Aapinen",
                    "150580-999K",
                    RestrictedDetails(enabled = false),
                    PersonAddress(
                        "Aapiskatu 1",
                        "00340",
                        "Espoo",
                        streetAddressSe = "Aapisväg 1",
                        postOfficeSe = "Esbo"
                    ),
                    NativeLanguage.FI,
                    nationalities = listOf(Nationality(countryName = "Suomi", countryCode = "FIN")),
                    residenceCode = "aapinen_1"
                ),
                MockVtjPerson(
                    "Mikael Matti Ilmari",
                    "Anttola",
                    "020394-958V",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Raivaajantie 1", "02140", "Espoo"),
                    NativeLanguage.FI
                ),
                MockVtjPerson(
                    "Anneli Helena Johanna Kaarina",
                    "Aspö",
                    "210586-987L",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Kamreerintie 11", "02170", "Espoo"),
                    NativeLanguage.FI
                ),
                MockVtjPerson(
                    "Sylvi Liisa Sofia",
                    "Marttila",
                    "081181-9984",
                    RestrictedDetails(enabled = false),
                    PersonAddress("Raivaajantie 13", "02100", "Espoo"),
                    NativeLanguage.FI
                )
            ),
        guardianDependants =
            mapOf(
                "070644-937X" to listOf("070714A9126", "160616A978U", "160620A999J"),
                "270372-905L" to listOf("220314A983X", "051116A902A"),
                "271170-917X" to listOf("070713A931J", "210216A911N"),
                "220281-9456" to listOf("071013A960W", "230916A910K"),
                "170590-9540" to listOf("050814A973N", "101016A935T"),
                "280674-990X" to listOf("290315A982N", "210216A977T"),
                "071082-9435" to listOf("070413A915L", "100716A998D"),
                "231182-9661" to listOf("231015A953H", "100916A979D"),
                "221071-9131" to listOf("050813A914H", "281116A9003"),
                "260888-990V" to listOf("140714A9542", "271016A9209"),
                "210586-987L" to listOf("051015A981T", "071116A9516"),
                "150794-9463" to listOf("160515A975N", "270816A974D"),
                "290385-9900" to listOf("240413A9634", "240816A910D"),
                "130973-9825" to listOf("020415A9833", "241016A927K"),
                "050482-9741" to listOf("180214A928S", "280916A9259"),
                "130894-917N" to listOf("290615A9203", "090316A948X"),
                "010882-983Y" to listOf("110913A9434", "080116A918P"),
                "100373-9733" to listOf("120313A995L", "030816A910V"),
                "090693-990K" to listOf("290413A902C", "090116A9690"),
                "231090-912J" to listOf("161015A9862", "190316A916B"),
                "010495-965H" to listOf("180613A920F", "171017A946X"),
                "290377-9377" to listOf("280315A994D", "020717A997F"),
                "090275-9724" to listOf("090514A974V", "210317A994D"),
                "020394-958V" to listOf("121114A921X", "220317A982N"),
                "210593-9430" to listOf("031114A9203", "170417A942Y"),
                "060195-966B" to listOf("180213A909W", "040317A946W"),
                "091171-9975" to listOf("051115A950L", "150817A939T"),
                "100774-9306" to listOf("050314A946S", "120217A923H"),
                "290393-9913" to listOf("120315A951P", "130217A995F"),
                "150288-971A" to listOf("070115A9583", "070817A9876"),
                "120482-955X" to listOf("080515A969A", "150818A995V"),
                "010170-960F" to listOf("100513A918E", "060118A9908"),
                "210281-9988" to listOf("280714A905H", "200418A9990"),
                "010101-123N" to listOf("210314A9784", "190618A918B"),
                "010170-999R" to listOf("290913A962B", "271018A911H"),
                "081181-9984" to listOf("170714A911L", "040918A972U"),
                "020898-945H" to listOf("021113A929F", "260918A9787"),
                "020474-9187" to listOf("160914A969E", "181118A9979"),
                "170595-9151" to listOf("291013A950S", "280618A9750"),
                "050391-999B" to listOf("110414A9962", "020818A959A"),
                "300190-9257" to listOf("140514A966U", "260213A9125"),
                "031083-910S" to listOf("190513A9454", "100915A900L"),
                "150978-9025" to listOf("061014A908U", "010713A933R"),
                "010180-1232" to listOf("010617A123U", "010316A1235"),
                "010279-123L" to listOf("010617A123U"),
                "311299-999E" to listOf("070714A9126"),
                "311288A999L" to listOf("311218A999J"),
                "150580-999K" to listOf("150515-999T")
            )
    )
