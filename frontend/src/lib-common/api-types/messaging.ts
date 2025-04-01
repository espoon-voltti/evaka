// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from 'lib-common/local-date'

import { MessageReceiver } from '../generated/api-types/messaging'

export const sortReceivers = <T extends MessageReceiver>(
  receivers: T[]
): T[] =>
  receivers.length === 0
    ? []
    : receivers
        .map((receiver) =>
          receiver.type === 'AREA'
            ? { ...receiver, receivers: sortReceivers(receiver.receivers) }
            : receiver.type === 'UNIT'
              ? { ...receiver, receivers: sortReceivers(receiver.receivers) }
              : receiver.type === 'GROUP'
                ? { ...receiver, receivers: sortReceivers(receiver.receivers) }
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
