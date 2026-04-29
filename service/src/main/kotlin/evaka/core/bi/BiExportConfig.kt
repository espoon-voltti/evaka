// SPDX-FileCopyrightText: 2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.bi

/**
 * Column containing personally identifiable information (PII) that should be conditionally included
 * in BI exports based on the BiExportConfig.includePII flag.
 */
@Target(AnnotationTarget.PROPERTY) @Retention(AnnotationRetention.RUNTIME) annotation class Pii

/**
 * Column retained for downstream DW schema stability even though the underlying eVaka data has been
 * removed (e.g., field dropped during refactor).
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class LegacyColumn

data class BiExportConfig(
    val includePII: Boolean,
    val includeLegacyColumns: Boolean,
    val deltaWindowDays: Int,
)
