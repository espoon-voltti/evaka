// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import * as React from 'react'
import { faChevronUp, faChevronDown } from '@evaka/icons'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import classNames from 'classnames'
import { Title } from '../elements/title'
import { IconProp } from '@fortawesome/fontawesome-svg-core'

interface CollapsibleProps {
  title: string
  children: React.ReactNode
  onToggle: (e: React.MouseEvent<HTMLDivElement>) => void
  dataQa?: string
  icon: IconProp
  className?: string
  open: boolean
}

export const Collapsible = ({
  title,
  children,
  onToggle,
  className,
  dataQa,
  icon,
  open = false
}: CollapsibleProps) => (
  <section
    className={classNames('collapsible', className)}
    data-qa={dataQa}
    data-status={open ? 'open' : 'closed'}
  >
    <div
      className={classNames('section-title', '-collapsible')}
      onClick={onToggle}
    >
      <Title size={3} dataQa={'title'}>
        <span className="section-title-icon" data-qa="icon">
          <FontAwesomeIcon icon={icon} />
        </span>
        {title}
      </Title>
      <div className="section-title-trigger" data-qa="collapsible-trigger">
        <FontAwesomeIcon icon={open ? faChevronUp : faChevronDown} />
      </div>
    </div>
    <div data-qa="content">{open ? children : null}</div>
  </section>
)
