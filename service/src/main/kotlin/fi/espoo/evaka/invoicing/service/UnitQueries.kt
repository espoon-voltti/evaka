// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.service

import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.db.Database

fun Database.Read.getUnitsThatAreInvoiced(): List<DaycareId> =
    createQuery { sql("SELECT id FROM daycare WHERE invoiced_by_municipality") }
        .toList<DaycareId>()
