// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.user

import org.springframework.stereotype.Component
import ua_parser.Client
import ua_parser.Parser

enum class DeviceClass {
    PHONE,
    TABLET,
    DESKTOP,
    UNKNOWN,
}

data class ParsedUserAgent(
    val deviceClass: DeviceClass,
    val operatingSystemName: String,
    val agentName: String,
)

private val phoneOperatingSystems =
    setOf("Windows Phone", "Windows Mobile", "BlackBerry OS", "KaiOS")

@Component
class UserAgentParser {
    private val parser = Parser()

    fun parse(userAgent: String?): ParsedUserAgent {
        if (userAgent.isNullOrBlank()) return ParsedUserAgent(DeviceClass.UNKNOWN, "", "")
        val client = parser.parse(userAgent)
        return ParsedUserAgent(
            deviceClass = deviceClassOf(client, userAgent),
            operatingSystemName = currentOperatingSystemName(client.os.family.known()),
            agentName = simpleAgentName(client.userAgent.family.known()),
        )
    }
}

/**
 * The device family of uap-java is a model name such as "SM-X710", which does not tell a tablet
 * from a phone. Android marks a phone, and only a phone, with "Mobile" in the header, and an iPad
 * is the only tablet that runs iOS.
 */
private fun deviceClassOf(client: Client, userAgent: String): DeviceClass =
    when (client.os.family) {
        "Other" -> DeviceClass.UNKNOWN
        "iOS" -> if (client.device.family == "iPad") DeviceClass.TABLET else DeviceClass.PHONE
        "Android" -> if (userAgent.contains("Mobile")) DeviceClass.PHONE else DeviceClass.TABLET
        in phoneOperatingSystems -> DeviceClass.PHONE
        else -> DeviceClass.DESKTOP
    }

/** uap-java reports the names these systems were released under, not their current ones */
private fun currentOperatingSystemName(family: String): String =
    when (family) {
        "Mac OS X" -> "macOS"
        "Chrome OS" -> "ChromeOS"
        else -> family
    }

/** uap-java names the mobile build of a browser separately, e.g. "Chrome Mobile iOS" */
private fun simpleAgentName(family: String): String =
    family.removePrefix("Mobile ").substringBefore(" Mobile").removeSuffix(" iOS")

/** uap-java returns "Other" for a value it cannot decide */
private fun String?.known(): String = this?.takeUnless { it.isBlank() || it == "Other" } ?: ""
