// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.data

import fi.espoo.evaka.invoicing.domain.PricingWithValidity
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.bindNullable
import org.jdbi.v3.core.kotlin.mapTo
import java.time.LocalDate

fun Database.Read.getPricing(from: LocalDate? = null): List<PricingWithValidity> {
    return createQuery("SELECT *, daterange(valid_from, valid_to, '[]') valid_during FROM pricing WHERE :from IS NULL OR valid_to IS NULL OR valid_to >= :from")
        .bindNullable("from", from)
        .mapTo<PricingWithValidity>()
        .toList()
}
