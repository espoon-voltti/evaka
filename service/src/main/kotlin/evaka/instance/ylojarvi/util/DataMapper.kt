// SPDX-FileCopyrightText: 2024 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.ylojarvi.util

class DataMapper<EnumClass> {
    val alphanumericValues: MutableMap<EnumClass, String> = mutableMapOf()
    val numericValues: MutableMap<EnumClass, Int> = mutableMapOf()
    var rowMap: Map<String, List<DataMapper<EnumClass>>> = mapOf()

    fun setAlphanumericValue(field: EnumClass, value: String) {
        alphanumericValues[field] = value
    }

    fun getAlphanumericValue(field: EnumClass): String? = alphanumericValues[field]

    fun setNumericValue(field: EnumClass, value: Int) {
        numericValues[field] = value
    }

    fun getNumericValue(field: EnumClass): Int? = numericValues[field]

    fun setChildRowMap(childMap: Map<String, List<DataMapper<EnumClass>>>) {
        rowMap = childMap
    }

    fun getChildRowMap(): Map<String, List<DataMapper<EnumClass>>> = rowMap
}

enum class FieldType {
    ALPHANUMERIC,
    NUMERIC,

    // we need a specific monetary type because they are prescaled by 100, so they include two
    // decimals
    MONETARY,
}
