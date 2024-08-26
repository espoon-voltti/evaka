// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared

import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.Row

typealias PagedFactory<T, R> = (data: List<T>, total: Int, pages: Int) -> R

fun <T, R> List<T>.pagedForPageSize(f: PagedFactory<T, R>, total: Int, pageSize: Int): R =
    f(this, total, if (total % pageSize == 0) total / pageSize else total / pageSize + 1)

fun <T, R> List<WithCount<T>>.mapToPaged(f: PagedFactory<T, R>, pageSize: Int): R =
    if (this.isEmpty()) {
        f(listOf(), 0, 1)
    } else {
        val count = this.first().count
        this.map { it.data }.pagedForPageSize(f, count, pageSize)
    }

fun <T, R> Database.Query.mapToPaged(
    f: PagedFactory<T, R>,
    pageSize: Int,
    countColumn: String,
    mapper: Row.() -> T,
): R = this.toList { WithCount(column(countColumn), mapper()) }.mapToPaged(f, pageSize)

fun <T, R> Database.Query.mapToPaged(f: PagedFactory<T, R>, pageSize: Int, mapper: Row.() -> T): R =
    this.mapToPaged(f, pageSize, "count", mapper)

inline fun <reified T, reified R> Database.Query.mapToPaged(
    noinline f: PagedFactory<T, R>,
    pageSize: Int,
): R = this.map(withCountMapper<T>()).toList().mapToPaged(f, pageSize)

data class WithCount<T>(val count: Int, val data: T)

inline fun <reified T> withCountMapper(): Row.() -> WithCount<T> = {
    WithCount(column("count"), row())
}
