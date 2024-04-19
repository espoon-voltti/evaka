// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.setting

import fi.espoo.evaka.ConstList
import fi.espoo.evaka.shared.db.DatabaseEnum

@ConstList("settings")
enum class SettingType : DatabaseEnum {
    DECISION_MAKER_NAME,
    DECISION_MAKER_TITLE;

    override val sqlType: String = "setting_type"
}
