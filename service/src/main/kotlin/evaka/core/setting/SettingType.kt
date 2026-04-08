// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.setting

import evaka.core.ConstList
import evaka.core.shared.db.DatabaseEnum

@ConstList("settings")
enum class SettingType : DatabaseEnum {
    DECISION_MAKER_NAME,
    DECISION_MAKER_TITLE;

    override val sqlType: String = "setting_type"
}
