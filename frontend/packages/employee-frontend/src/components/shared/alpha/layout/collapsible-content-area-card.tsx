// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import * as React from 'react'
import classNames from 'classnames'

import { Button } from '../elements/button'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faChevronDown, faChevronUp } from '@evaka/icons'
import { Title, Subtitle } from '../elements'
import { Section } from './section'

interface CollapsibleContentAreaCardProps {
  children: React.ReactNode
  header: React.ReactNode
  open: boolean
  onToggle: (e: React.MouseEvent<HTMLButtonElement>) => void
  title: string
  className?: string
  dataQa?: string
}

export const CollapsibleContentAreaCard: React.FunctionComponent<CollapsibleContentAreaCardProps> = ({
  title,
  header,
  children,
  className,
  dataQa,
  onToggle,
  open = false
}) => (
  <section
    className={classNames('content-area-card', 'is-collapsible', className)}
    data-qa={dataQa}
    data-status={open ? 'open' : 'closed'}
  >
    {title && (
      <Title size={2} className="content-area-card-title" dataQa="title">
        {title}
      </Title>
    )}
    <div className="content-area-card-content" data-qa="content">
      <Section>{header}</Section>
      {open ? children : null}
    </div>
    <div className="trigger">
      <Button
        className="trigger-button"
        dataQa="collapsible-trigger"
        onClick={onToggle}
      >
        <FontAwesomeIcon icon={open ? faChevronUp : faChevronDown} />
      </Button>
    </div>
  </section>
)

interface HCollapsibleContentAreaCardProps {
  title: string
  subtitle: string
  header?: React.ReactNode
  children: React.ReactNode
  open: boolean
  onToggle: (e: React.MouseEvent<HTMLButtonElement>) => void
  className?: string
  dataQa?: string
}
// TODO: rename this
export const HeaderlessCollapsibleContentAreaCard: React.FunctionComponent<HCollapsibleContentAreaCardProps> = ({
  title,
  header,
  subtitle,
  children,
  className,
  dataQa,
  onToggle,
  open = false
}) => (
  <section
    className={classNames(
      'content-area-card',
      'is-collapsible',
      'is-headerless', // TODO: rename this
      className
    )}
    data-qa={dataQa}
    data-status={open ? 'open' : 'closed'}
  >
    <div className="content-area-card-content" data-qa="content-wrapper">
      <div className="content-area-card-content-header">
        <div className="content-area-card-content-header-title-wrapper">
          <Title size={2} className="content-area-card-title" dataQa="title">
            {title}
          </Title>
          <Subtitle dataQa="subtitle">{subtitle}</Subtitle>
        </div>
        <Section>{header}</Section>
      </div>
      <div data-qa="content">{open ? children : null}</div>
    </div>
    <div className="trigger">
      <Button
        className="trigger-button"
        dataQa="collapsible-trigger"
        onClick={onToggle}
      >
        <FontAwesomeIcon icon={open ? faChevronUp : faChevronDown} />
      </Button>
    </div>
  </section>
)
