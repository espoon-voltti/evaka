// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import * as React from 'react'

interface Props {
  children: React.ReactNode
}

export const HelperText: React.FunctionComponent<Props> = (props) => (
  <p className="helper-text">{props.children}</p>
)
