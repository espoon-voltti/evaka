// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.messaging.message

import fi.espoo.evaka.shared.Paged
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import java.util.UUID

private val supervisorAccount: MessageAccount = MessageAccount(id = UUID.randomUUID(), name = "Esimies Ella")
private val aatusMomAccount: MessageAccount = MessageAccount(id = UUID.randomUUID(), name = "Aatun äiti")
private val aatusDadAccount: MessageAccount = MessageAccount(id = UUID.randomUUID(), name = "Aatun isä")

fun mockThreadData(): Paged<MessageThread> {
    val aatuMeritahtiMessage = Message(
        id = UUID.randomUUID(),
        senderId = supervisorAccount.id,
        senderName = supervisorAccount.name,
        content = "Hei Aatun äiti ja isä! Aatu on kasvanut kovaa vauhtia ja nyt on tullut aika siirtyä seuraavaan ikäryhmään. Aatu aloittaa Meritähdissä kesäkuussa.",
        sentAt = HelsinkiDateTime.now().minusDays(3)
    )
    val aatusDadsResponse = Message(
        id = UUID.randomUUID(),
        senderId = aatusDadAccount.id,
        senderName = aatusDadAccount.name,
        content = "Kuulostaa hyvältä, kiitos tiedosta.",
        sentAt = HelsinkiDateTime.now().minusDays(2)
    )
    val aatusMomsResponse = Message(
        id = UUID.randomUUID(),
        senderId = aatusMomAccount.id,
        senderName = aatusMomAccount.name,
        content = "Joo, se käy oikein mainiosti :-) Hyvää kevään jatkoa!",
        sentAt = HelsinkiDateTime.now().minusDays(2).plusHours(2)
    )
    val bulletin = Message(
        id = UUID.randomUUID(),
        senderId = supervisorAccount.id,
        senderName = supervisorAccount.name,
        content = """Hei vanhemmat!
    
    Kevät lähestyy jo kovaa vauhtia. Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas dapibus blandit porta. Aliquam pellentesque ex sed magna efficitur convallis. Sed elementum felis quis placerat rutrum.
    
    Suspendisse pretium pulvinar ligula, nec pellentesque tortor eleifend sed. Fusce placerat luctus erat a lobortis. Aenean et finibus purus, a rhoncus odio. Donec at velit laoreet, convallis ex vel, iaculis lorem. Vestibulum posuere dapibus est, at venenatis nunc consequat pharetra. Nullam sollicitudin orci sed accumsan consectetur. Sed urna augue, sollicitudin at ex sed, hendrerit malesuada enim.
    
    Nyt kun viimeiset lumet sulavat ja on märät kelit, niin huolehdittehan, että lapsen kuravaatteet ovat päiväkodilla, ja että lapsella on lokerossa tarpeeksi vaihtovaatteita niin sisälle kuin ulkoleikkeihinkin.
    
    Aurinkoisia päiviä!
    
    T. Ella Esimies""",
        sentAt = HelsinkiDateTime.now().minusWeeks(1)
    )

    return Paged(
        total = 2, pages = 1,
        data = listOf(
            MessageThread(
                id = UUID.randomUUID(),
                type = MessageType.MESSAGE,
                title = "Aatun siirtyminen Meritähtiin",
                messages = listOf(
                    aatuMeritahtiMessage,
                    aatusDadsResponse,
                    aatusMomsResponse
                )
            ),
            MessageThread(
                id = UUID.randomUUID(),
                type = MessageType.BULLETIN,
                title = "Kevät",
                messages = listOf(bulletin)
            )
        )
    )
}
