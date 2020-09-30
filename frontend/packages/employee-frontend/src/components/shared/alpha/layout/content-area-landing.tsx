// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import * as React from 'react'
import classNames from 'classnames'
import { Title } from '../elements/title'

interface Props {
  children: React.ReactNode
  title: string
  subtitle: string
  image?: React.ReactNode
  className?: string
  dataQa?: string
}

export const ContentAreaLanding: React.FunctionComponent<Props> = ({
  className,
  dataQa,
  title,
  subtitle,
  children,
  image
}) => (
  <section
    className={classNames('content-area-landing', className)}
    data-qa={dataQa}
  >
    {image && <div className="content-area-landing-image">{image}</div>}
    <Title className="content-area-landing-title" size={1}>
      {title}
    </Title>
    <Title className="content-area-landing-subtitle title" size={3} tag={2}>
      {subtitle}
    </Title>
    {children}
  </section>
)
