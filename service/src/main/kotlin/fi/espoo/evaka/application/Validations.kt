// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.application

import kotlin.reflect.KProperty0

class ValidationResult(private val errors: MutableList<ValidationError> = mutableListOf()) {
    fun add(result: ValidationResult) {
        errors.addAll(result.errors)
    }

    fun add(error: ValidationError) {
        errors.add(error)
    }

    fun isValid(): Boolean = errors.size == 0

    fun getErrors(): List<ValidationError> = errors
}

fun ValidationResult.add(prop: KProperty0<Any?>, errorMsg: String, vararg parents: String) =
    add(ValidationError(prop.name, errorMsg, parents.toList()))

data class ValidationError(
    val field: String,
    val error: String,
    private val parentNames: List<String> = emptyList()
) {
    val path: String
        get() = (this.parentNames + this.field).reduce { acc, s -> "$acc.$s" }
}

/**
 * Exception to be thrown if validation fails
 */
class ValidationException(result: ValidationResult) : RuntimeException(result.getErrors().toString())
