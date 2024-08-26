// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.service

import fi.espoo.evaka.invoicing.domain.IncomeType

fun interface IncomeTypesProvider {

    fun get(): Map<String, IncomeType>
}

class EspooIncomeTypesProvider : IncomeTypesProvider {
    override fun get(): Map<String, IncomeType> {
        return linkedMapOf(
            "MAIN_INCOME" to IncomeType("Päätulot", 1, true, false),
            "SHIFT_WORK_ADD_ON" to IncomeType("Vuorotyölisät", 1, false, true),
            "PERKS" to IncomeType("Luontaisedut", 1, false, true),
            "SECONDARY_INCOME" to IncomeType("Sivutulo", 1, true, false),
            "PENSION" to IncomeType("Eläkkeet", 1, false, false),
            "UNEMPLOYMENT_BENEFITS" to
                IncomeType("Työttömyyskorvaus/työmarkkinatuki", 1, false, false),
            "SICKNESS_ALLOWANCE" to IncomeType("Sairauspäiväraha", 1, false, false),
            "PARENTAL_ALLOWANCE" to IncomeType("Äitiys- ja vanhempainraha", 1, false, false),
            "HOME_CARE_ALLOWANCE" to
                IncomeType("Kotihoidontuki, joustava/osittainen hoitoraha", 1, false, false),
            "ALIMONY" to IncomeType("Elatusapu/-tuki", 1, false, false),
            "OTHER_INCOME" to IncomeType("Muu tulo (korko, vuokra, osinko jne.)", 1, true, false),
            "ALL_EXPENSES" to
                IncomeType("Menot (esim. maksetut elatusmaksut tai syytinki)", -1, false, false),
        )
    }
}
