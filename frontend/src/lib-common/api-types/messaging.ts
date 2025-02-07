// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'

import { MessageRecipientType } from '../generated/api-types/messaging'

export type MessageReceiver =
  | MessageReceiverBase // unit in area, or citizen
  | MessageReceiverChild
  | MessageReceiverArea
  | MessageReceiverUnit
  | MessageReceiverGroup

interface MessageReceiverBase {
  id: UUID
  name: string
  type: MessageRecipientType
}

interface MessageReceiverChild extends MessageReceiverBase {
  startDate: LocalDate | null
}

interface MessageReceiverArea extends MessageReceiverBase {
  receivers: MessageReceiverUnitInArea[]
}

type MessageReceiverUnitInArea = MessageReceiverBase

interface MessageReceiverUnit extends MessageReceiverBase {
  receivers: MessageReceiverGroup[]
  hasStarters: boolean
}

interface MessageReceiverGroup extends MessageReceiverBase {
  receivers: MessageReceiverBase[]
  hasStarters: boolean
}

export const sortReceivers = (
  receivers: MessageReceiver[]
): MessageReceiver[] =>
  receivers
    .map((receiver) =>
      'receivers' in receiver
        ? {
            ...receiver,
            receivers:
              receiver.receivers.length === 0
                ? []
                : sortReceivers(receiver.receivers)
          }
        : receiver
    )
    .sort((a, b) => {
      const aStarter = messageReceiverIsStarter(a) ? 1 : 0
      const bStarter = messageReceiverIsStarter(b) ? 1 : 0

      if (aStarter !== bStarter) {
        return aStarter - bStarter
      }
      const aDate = messageReceiverStartDate(a)
      const bDate = messageReceiverStartDate(b)

      if (aDate && bDate) {
        return aDate.compareTo(bDate)
      }
      return a.name
        .toLocaleLowerCase()
        .localeCompare(b.name.toLocaleLowerCase())
    })

export function messageReceiverStartDate(
  receiver: MessageReceiver
): LocalDate | null {
  return 'startDate' in receiver ? receiver.startDate : null
}

export function messageReceiverIsStarter(receiver: MessageReceiver): boolean {
  return (
    ('hasStarters' in receiver && receiver.hasStarters) ||
    messageReceiverStartDate(receiver) !== null
  )
}

export function deserializeMessageReceiver(
  json: JsonOf<MessageReceiver>
): MessageReceiver {
  const result: MessageReceiver = {
    ...json,
    ...('startDate' in json && {
      startDate:
        json.startDate !== null ? LocalDate.parseIso(json.startDate) : null
    }),
    ...('receivers' in json && {
      receivers: json.receivers.map(deserializeMessageReceiver)
    })
  }
  return result
}
