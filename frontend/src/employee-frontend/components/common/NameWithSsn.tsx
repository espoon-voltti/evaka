// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { Translations } from '../../state/i18n'
import { formatName } from '../../utils'
import LocalDate from 'lib-common/local-date'

interface Props {
  firstName: string
  lastName: string
  dateOfBirth: LocalDate
  ssn: string | null
  i18n: Translations
}

export default function NameWithSsn({
  firstName,
  lastName,
  dateOfBirth,
  ssn,
  i18n
}: Props) {
  return (
    <>
      <div>{formatName(firstName, lastName, i18n, true)}</div>
      <div>{ssn || dateOfBirth.format()}</div>
    </>
  )
}
