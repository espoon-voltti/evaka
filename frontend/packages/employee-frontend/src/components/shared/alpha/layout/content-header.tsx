// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import * as React from 'react'
import { Title, Subtitle } from '../elements/title'

interface Props {
  title: string
  subtitle: React.ReactNode
  status: React.ReactNode
}

export const ContentHeader = ({ title, subtitle, status }: Props) => (
  <div className="content-header">
    <Title size={2}>{title}</Title>
    <Subtitle>{subtitle}</Subtitle>
    <div className="status">{status}</div>
  </div>
)
