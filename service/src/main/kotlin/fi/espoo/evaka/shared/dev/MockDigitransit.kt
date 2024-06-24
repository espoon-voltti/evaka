// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.dev

import com.fasterxml.jackson.annotation.JsonFormat
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

class MockDigitransit {
    private val lock = ReentrantReadWriteLock()
    private var mockAutocomplete: Autocomplete = Autocomplete(emptyList())

    fun autocomplete(): Autocomplete = lock.read { mockAutocomplete }

    fun setAutocomplete(mockResponse: Autocomplete) = lock.write { mockAutocomplete = mockResponse }

    data class Autocomplete(
        val features: List<Feature>
    )

    data class Feature(
        val geometry: Geometry,
        val properties: FeatureProperties
    )

    data class Geometry(
        @JsonFormat(shape = JsonFormat.Shape.ARRAY) val coordinates: Pair<Double, Double>
    )

    data class FeatureProperties(
        val name: String,
        val postalcode: String?,
        val locality: String?,
        val localadmin: String?
    )
}
