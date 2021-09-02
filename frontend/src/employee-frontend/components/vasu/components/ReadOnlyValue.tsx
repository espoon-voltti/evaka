// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { Label } from 'lib-components/typography'
import { ValueOrNoRecord } from './ValueOrNoRecord'

interface Props {
  label: string
  value?: string
}

export function ReadOnlyValue({ label, value }: Props) {
  return (
    <>
      <Label>{label}</Label>
      <ValueOrNoRecord text={value} />
    </>
  )
}
