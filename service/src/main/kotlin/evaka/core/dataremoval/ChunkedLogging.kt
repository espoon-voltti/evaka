// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.dataremoval

import fi.espoo.voltti.logging.loggers.info
import io.github.oshai.kotlinlogging.KLogger

private const val DEFAULT_META_CHUNK_SIZE = 100

/**
 * Logging for a bulk operation as one INFO line per chunk, preventing excessive lengths in meta
 * properties. [message] receives the 1-based chunk index and total chunk count.
 *
 * Nothing is logged when [items] is empty; the caller logs any zero-case summary itself.
 */
fun <T> KLogger.infoChunked(
    items: List<T>,
    chunkSize: Int = DEFAULT_META_CHUNK_SIZE,
    meta: (chunk: List<T>) -> Map<String, Any?>,
    message: (chunkIndex: Int, chunkCount: Int) -> String,
) {
    require(chunkSize > 0) { "chunkSize must be positive" }
    if (items.isEmpty()) return
    val chunks = items.chunked(chunkSize)
    val total = chunks.size
    chunks.forEachIndexed { idx, chunk -> info(meta(chunk)) { message(idx + 1, total) } }
}
