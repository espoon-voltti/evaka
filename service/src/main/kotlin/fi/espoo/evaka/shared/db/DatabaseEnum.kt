// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.db

/**
 * Marker interface for enums that have a corresponding enum type in PostgreSQL.
 * The enum class should implement this interface, and provide *one* sqlType for the entire class. Example:
 *
 * ```
 * enum class MyEnum {
 *   A, B;
 *
 *   override val sqlType: String = "my_enum"
 * }
 * ```
 */
interface DatabaseEnum {
    val sqlType: String
}
