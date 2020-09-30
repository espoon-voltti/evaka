// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.msg.controllers

import fi.espoo.evaka.msg.Audit
import fi.espoo.evaka.msg.async.AsyncJobRunner
import fi.espoo.evaka.msg.async.SendMessage
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/message")
class MessagesController(private val asyncJobRunner: AsyncJobRunner) {
    @PostMapping("/send")
    fun sendMessage(@RequestBody message: PdfSendMessage): ResponseEntity<Unit> {
        Audit.MessageSendSfi.log(targetId = message.documentId)
        asyncJobRunner.plan(listOf(SendMessage(message)))
        return ResponseEntity.noContent().build()
    }
}
