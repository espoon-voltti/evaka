// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.data

import fi.espoo.evaka.invoicing.domain.FeeThresholds
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.bindNullable
import org.jdbi.v3.core.kotlin.mapTo
import java.time.LocalDate

fun Database.Read.getFeeThresholds(from: LocalDate? = null): List<FeeThresholds> {
    return createQuery("SELECT * FROM fee_thresholds WHERE valid_during && daterange(:from, null)")
        .bindNullable("from", from)
        .mapTo<FeeThresholds>()
        .toList()
}
