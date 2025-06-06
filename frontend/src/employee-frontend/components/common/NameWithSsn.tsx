// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import type LocalDate from 'lib-common/local-date'
import { PersonName } from 'lib-components/molecules/PersonNames'

interface Props {
  firstName: string
  lastName: string
  dateOfBirth: LocalDate
  ssn: string | null
}

export default function NameWithSsn({
  firstName,
  lastName,
  dateOfBirth,
  ssn
}: Props) {
  return (
    <>
      <div>
        <PersonName person={{ firstName, lastName }} format="Last First" />
      </div>
      <div>{ssn || dateOfBirth.format()}</div>
    </>
  )
}
