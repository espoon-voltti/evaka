// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type LocalDate from 'lib-common/local-date'

import type { SelectableRecipient } from '../generated/api-types/messaging'

export const sortSelectableRecipients = <T extends SelectableRecipient>(
  recipients: T[]
): T[] =>
  recipients.length === 0
    ? []
    : recipients
        .map((recipient) =>
          recipient.type === 'AREA'
            ? {
                ...recipient,
                receivers: sortSelectableRecipients(recipient.receivers)
              }
            : recipient.type === 'UNIT'
              ? {
                  ...recipient,
                  receivers: sortSelectableRecipients(recipient.receivers)
                }
              : recipient.type === 'GROUP'
                ? {
                    ...recipient,
                    receivers: sortSelectableRecipients(recipient.receivers)
                  }
                : recipient
        )
        .sort((a, b) => {
          const aStarter = selectableRecipientIsStarter(a) ? 1 : 0
          const bStarter = selectableRecipientIsStarter(b) ? 1 : 0

          if (aStarter !== bStarter) {
            return aStarter - bStarter
          }
          const aDate = selectableRecipientStartDate(a)
          const bDate = selectableRecipientStartDate(b)

          if (aDate && bDate) {
            return aDate.compareTo(bDate)
          }
          return a.name
            .toLocaleLowerCase()
            .localeCompare(b.name.toLocaleLowerCase())
        })

export function selectableRecipientStartDate(
  recipient: SelectableRecipient
): LocalDate | null {
  return 'startDate' in recipient ? recipient.startDate : null
}

export function selectableRecipientIsStarter(
  recipient: SelectableRecipient
): boolean {
  return (
    ('hasStarters' in recipient && recipient.hasStarters) ||
    selectableRecipientStartDate(recipient) !== null
  )
}
