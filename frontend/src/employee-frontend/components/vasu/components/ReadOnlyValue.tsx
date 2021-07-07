// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { Label } from '../../../../lib-components/typography'
import { OrNoRecord } from './OrNoRecord'

export function ReadOnlyValue(props: { label: string; value?: string }) {
  return (
    <>
      <Label>{props.label}</Label>
      <OrNoRecord>{props.value}</OrNoRecord>
    </>
  )
}
