// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.db

import java.sql.ResultSet
import java.util.UUID

fun ResultSet.getUUID(colName: String): UUID = UUID.fromString(this.getString(colName))
fun ResultSet.getNullableUUID(colName: String): UUID? = this.getString(colName)?.let { UUID.fromString(it) }
inline fun <reified T : Enum<T>> ResultSet.getEnum(colName: String) = enumValueOf<T>(this.getString(colName))
