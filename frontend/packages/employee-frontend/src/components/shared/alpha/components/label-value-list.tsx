// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import * as React from 'react'
import { Columns, Column } from '../elements/columns'

export interface Props {
  children: React.ReactNode
  dataQa?: string
}

export const LabelValueList: React.FunctionComponent<Props> = ({
  children,
  dataQa
}) => (
  <div className="label-value-list" data-qa={dataQa}>
    {children}
  </div>
)

export interface LabelValueItem {
  label: string
  value: React.ReactNode
  dataQa: string
}

export const LabelValueListItem: React.FunctionComponent<LabelValueItem> = ({
  label,
  value,
  dataQa
}: LabelValueItem) => (
  <Columns className="label-value-list-row" gapless dataQa={dataQa}>
    <Column dataQa="label" size={5} className="label-value-list-row-label">
      {label}
    </Column>
    <Column dataQa="value" className="column">
      {value}
    </Column>
  </Columns>
)
