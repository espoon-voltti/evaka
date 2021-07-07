// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { ReactNode } from 'react'
import { Dimmed } from '../../../../lib-components/typography'
import { useTranslation } from '../../../state/i18n'

export function OrNoRecord(props: { children: ReactNode }): JSX.Element {
  const { i18n } = useTranslation()
  return props.children ? (
    <>{props.children}</>
  ) : (
    <Dimmed>{i18n.vasu.noRecord}</Dimmed>
  )
}
