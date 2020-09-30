// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import * as React from 'react'

interface Props {
  dataQa?: string
  children: React.ReactNode
}

export const Footer: React.FC<Props> = ({ children, dataQa }) => (
  <div className="view-footer" data-qa={dataQa}>
    <div className="view-footer--content">{children}</div>
  </div>
)
