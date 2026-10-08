// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.shared.async

/**
 * Thrown from an async job handler when the failure is deterministic and retrying cannot help. The
 * job fails permanently right away instead of using up its remaining attempts.
 */
class PermanentAsyncJobFailure(message: String, cause: Throwable? = null) :
    RuntimeException(message, cause)
