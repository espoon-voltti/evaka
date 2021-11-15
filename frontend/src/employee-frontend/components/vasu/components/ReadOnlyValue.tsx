// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { Label } from 'lib-components/typography'
import { ValueOrNoRecord } from './ValueOrNoRecord'
import { VasuTranslations } from 'lib-customizations/employee'

interface Props {
  label: string
  value?: string
  translations: VasuTranslations
}

export function ReadOnlyValue({ label, value, translations }: Props) {
  return (
    <>
      <Label>{label}</Label>
      <ValueOrNoRecord text={value} translations={translations} />
    </>
  )
}
