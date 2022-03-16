// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared

import fi.espoo.evaka.shared.db.mapColumn
import org.jdbi.v3.core.mapper.RowMapper
import org.jdbi.v3.core.result.ResultBearing
import org.jdbi.v3.core.result.RowView

data class Paged<T>(val data: List<T>, val total: Int, val pages: Int) {
    companion object {
        fun <T> forPageSize(data: List<T>, total: Int, pageSize: Int): Paged<T> =
            Paged(
                data,
                total,
                if (total % pageSize == 0) total / pageSize else total / pageSize + 1
            )
    }
}

fun <T> List<WithCount<T>>.mapToPaged(pageSize: Int): Paged<T> =
    if (this.isEmpty()) {
        Paged(listOf(), 0, 1)
    } else {
        val count = this.first().count
        Paged.forPageSize(this.map { it.data }, count, pageSize)
    }

fun <T> ResultBearing.mapToPaged(pageSize: Int, countColumn: String, mapper: (row: RowView) -> T): Paged<T> =
    this.map { row -> WithCount(row.mapColumn(countColumn), mapper(row)) }.list().mapToPaged(pageSize)

fun <T> ResultBearing.mapToPaged(pageSize: Int, mapper: (row: RowView) -> T): Paged<T> =
    this.mapToPaged(pageSize, "count", mapper)

inline fun <reified T> ResultBearing.mapToPaged(pageSize: Int): Paged<T> =
    this.map(withCountMapper<T>()).list().mapToPaged(pageSize)

data class WithCount<T>(val count: Int, val data: T)

inline fun <reified T> withCountMapper(): RowMapper<WithCount<T>> = RowMapper { rs, ctx ->
    WithCount(
        ctx.mapColumn(rs, "count"),
        ctx.findRowMapperFor(T::class.java).map { mapper -> mapper.map(rs, ctx) }.orElseThrow { error("ASD") }
    )
}
