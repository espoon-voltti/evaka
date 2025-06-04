// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import type LocalDate from 'lib-common/local-date'
import { formatPersonName } from 'lib-common/names'

import { useTranslation } from '../../state/i18n'

import NameWithSsn from './NameWithSsn'

type Props = {
  people: {
    firstName: string
    lastName: string
    dateOfBirth: LocalDate
    ssn: string | null
  }[]
}

function ChildrenCell({ people }: Props) {
  const { i18n } = useTranslation()

  if (people.length === 0) {
    return <span />
  }

  const visibleContent =
    people.length > 1 ? (
      i18n.common.multipleChildren
    ) : (
      <NameWithSsn {...people[0]} />
    )

  const htmlTitle = people
    .map(
      ({ firstName, lastName, dateOfBirth, ssn }) =>
        `${formatPersonName({ firstName, lastName }, 'Last First')} (${
          ssn || dateOfBirth.format()
        })`
    )
    .join('\n')

  return <span title={htmlTitle}>{visibleContent}</span>
}

export default ChildrenCell
