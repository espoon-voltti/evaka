// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import * as React from 'react'

interface Props {
  children?: React.ReactNode
}

export const Loader = ({ children }: Props) => (
  <div className="loader-wrapper">
    <div className="loader-spinner"></div>
    {children && <div className="loader-label">{children}</div>}
  </div>
)
