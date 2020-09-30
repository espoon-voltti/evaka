// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.application.utils

import kotlin.reflect.KClass

interface IdentifiableEnum<out ID> {
    fun getId(): ID
}

fun <ID, E> KClass<E>.getInstanceById(id: ID): E where E : Enum<E>, E : IdentifiableEnum<ID> =
    this.java.enumConstants.find { it.getId() == id }
        ?: throw IllegalArgumentException("No enum (class: ${this.java.name}) found with id: $id")
