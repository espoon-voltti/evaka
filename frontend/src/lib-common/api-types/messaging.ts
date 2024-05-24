// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { UUID } from 'lib-common/types'

import { MessageRecipientType } from '../generated/api-types/messaging'

export type MessageReceiver =
  | MessageReceiverBase // unit in area, or child, or citizen
  | MessageReceiverArea
  | MessageReceiverUnit
  | MessageReceiverGroup

interface MessageReceiverBase {
  id: UUID
  name: string
  type: MessageRecipientType
}

interface MessageReceiverArea extends MessageReceiverBase {
  receivers: MessageReceiverUnitInArea[]
}

type MessageReceiverUnitInArea = MessageReceiverBase

interface MessageReceiverUnit extends MessageReceiverBase {
  receivers: MessageReceiverGroup[]
}

interface MessageReceiverGroup extends MessageReceiverBase {
  receivers: MessageReceiverBase[]
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
    .sort((a, b) =>
      a.name.toLocaleLowerCase().localeCompare(b.name.toLocaleLowerCase())
    )
