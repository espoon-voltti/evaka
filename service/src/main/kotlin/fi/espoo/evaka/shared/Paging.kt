package fi.espoo.evaka.shared

import fi.espoo.evaka.shared.db.mapColumn
import org.jdbi.v3.core.mapper.RowMapper

data class Paged<T>(val data: List<T>, val total: Int, val pages: Int)

fun <T> mapToPaged(pageSize: Int): (Iterable<WithCount<T>>) -> Paged<T> = { rows ->
    if (rows.firstOrNull() == null) Paged(listOf(), 0, 1)
    else {
        val count = rows.first().count
        Paged(
            rows.map { it.data },
            count,
            if (count % pageSize == 0) count / pageSize else count / pageSize + 1
        )
    }
}

data class WithCount<T>(val count: Int, val data: T)

inline fun <reified T> withCountMapper(): RowMapper<WithCount<T>> = RowMapper { rs, ctx ->
    WithCount(
        ctx.mapColumn(rs, "count"),
        ctx.findRowMapperFor(T::class.java).map { mapper -> mapper.map(rs, ctx) }.orElseThrow { error("ASD") }
    )
}
