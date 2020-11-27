// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.db

import fi.espoo.evaka.shared.domain.NotFound
import org.jdbi.v3.core.statement.Update
import java.sql.ResultSet
import java.util.UUID

fun ResultSet.getUUID(colName: String): UUID = UUID.fromString(this.getString(colName))
fun ResultSet.getNullableUUID(colName: String): UUID? = this.getString(colName)?.let { UUID.fromString(it) }
inline fun <reified T : Enum<T>> ResultSet.getEnum(colName: String) = enumValueOf<T>(this.getString(colName))

fun Update.updateExactlyOne(notFoundMsg: String = "Not found", foundMultipleMsg: String = "Found multiple") {
    val rows = this.execute()
    if (rows == 0) throw NotFound(notFoundMsg)
    if (rows > 1) throw Error(foundMultipleMsg)
}
