// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import * as React from 'react'
import classNames from 'classnames'

interface Props {
  children: React.ReactNode
  className?: string
  dataQa?: string
  opaque?: boolean
}

export const ContentArea: React.FunctionComponent<Props> = ({
  children,
  className,
  dataQa,
  opaque = false
}) => (
  <section
    className={classNames(
      'content-area',
      { 'has-background': opaque },
      className
    )}
    data-qa={dataQa}
  >
    {children}
  </section>
)
