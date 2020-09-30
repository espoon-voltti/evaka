// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import * as React from 'react'
import classNames from 'classnames'
import { StatusType } from '../types/common'

export interface Props {
  className?: string
  status?: StatusType
  dataQa?: string
}

export const StatusBorder: React.FunctionComponent<Props> = ({
  className,
  status,
  dataQa
}) => (
  <div
    className={classNames(
      'status-border',
      status ? `is-${status}` : '',
      className
    )}
    data-qa={dataQa}
  />
)
