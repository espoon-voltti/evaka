// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared

import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.mapColumn
import fi.espoo.evaka.shared.db.mapRow
import org.jdbi.v3.core.mapper.RowViewMapper
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

    fun <U> map(mapper: (T) -> U): Paged<U> = Paged(data.map(mapper), total, pages)

    fun <U> flatMap(mapper: (T) -> List<U>): Paged<U> = Paged(data.flatMap(mapper), total, pages)
}

fun <T> List<WithCount<T>>.mapToPaged(pageSize: Int): Paged<T> =
    if (this.isEmpty()) {
        Paged(listOf(), 0, 1)
    } else {
        val count = this.first().count
        Paged.forPageSize(this.map { it.data }, count, pageSize)
    }

fun <T> Database.Query.mapToPaged(
    pageSize: Int,
    countColumn: String,
    mapper: (row: RowView) -> T
): Paged<T> =
    this.map { row -> WithCount(row.mapColumn(countColumn), mapper(row)) }
        .list()
        .mapToPaged(pageSize)

fun <T> Database.Query.mapToPaged(pageSize: Int, mapper: (row: RowView) -> T): Paged<T> =
    this.mapToPaged(pageSize, "count", mapper)

inline fun <reified T> Database.Query.mapToPaged(pageSize: Int): Paged<T> =
    this.map(withCountMapper<T>()).list().mapToPaged(pageSize)

data class WithCount<T>(val count: Int, val data: T)

inline fun <reified T> withCountMapper(): RowViewMapper<WithCount<T>> = RowViewMapper { row ->
    WithCount(row.mapColumn("count"), row.mapRow())
}
