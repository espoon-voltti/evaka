// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

@file:Suppress("UNUSED_PARAMETER")

package fi.espoo.evaka.daycare.controllers.utils

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import java.net.URI

fun <T : Any?> ok(body: T): ResponseEntity<T> =
    if (body == null || body is Unit) ResponseEntity.ok().build()
    else ResponseEntity.ok(body)

fun <T : Any?> accepted(): ResponseEntity<T> = ResponseEntity.accepted().build()

fun <T : Any?> noContent(): ResponseEntity<T> = ResponseEntity.noContent().build()
fun <T : Any?> noContent(u: Unit): ResponseEntity<T> = noContent()

fun <T : Any?> badRequest(): ResponseEntity<T> = ResponseEntity.badRequest().build()

fun <T : Any?> notFound(): ResponseEntity<T> = ResponseEntity.notFound().build()

fun <T : Any?> created(body: T?, uri: URI? = null): ResponseEntity<T> {
    var builder = if (uri == null) ResponseEntity.status(HttpStatus.CREATED) else ResponseEntity.created(uri)
    return if (body == null) builder.build() else builder.body(body)
}

fun <T : Any?> created(u: Unit): ResponseEntity<T> = created(null, null)

data class Wrapper<T>(val data: T)
