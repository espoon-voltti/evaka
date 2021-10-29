package fi.espoo.evaka.setting

import fi.espoo.evaka.ConstList

data class Setting(
    val key: SettingType,
    val value: String
)

@ConstList("settings")
enum class SettingType {
    DECISION_MAKER_NAME,
    DECISION_MAKER_TITLE,
}
