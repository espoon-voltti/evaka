// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare.domain

import fi.espoo.evaka.shared.db.DatabaseEnum

enum class ProviderType : DatabaseEnum {
    MUNICIPAL,
    PURCHASED,
    PRIVATE,
    MUNICIPAL_SCHOOL,
    PRIVATE_SERVICE_VOUCHER,
    EXTERNAL_PURCHASED;

    override val sqlType: String = "unit_provider_type"
}
