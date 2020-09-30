// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { ReactElement } from 'react'
import { faChevronUp, faChevronDown } from 'icon-set'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'

import '~components/common/ToolbarAccordion.scss'
import { Title } from '~components/shared/alpha'
import styled from 'styled-components'
import StatusLabel, { StatusLabelType } from '~components/common/StatusLabel'

// workaround against issue where "is-4" class is used for two different purposes.
const SubTitle = styled(Title)`
  flex: auto !important;
`

interface Props {
  title?: string
  subtitle?: string
  children: React.ReactNode
  onToggle: (e: React.MouseEvent<HTMLDivElement>) => void
  toolbar?: ReactElement
  showBorder?: boolean
  columnSize?: number
  dataQa?: string
  open: boolean
}

function ToolbarAccordion({
  title,
  subtitle,
  children,
  onToggle,
  toolbar = undefined,
  showBorder = false,
  dataQa,
  open = false
}: Props) {
  return (
    <section
      className={`collapsible ${open ? 'is-open' : ''}`}
      data-qa={dataQa}
      data-status={open ? 'open' : 'closed'}
    >
      <div className="accordion-collapse-wrapper">
        <div className={'columns is-gapless ' + (showBorder ? 'border' : '')}>
          <Title className={`column ${subtitle ? 'is-5' : 'is-8'}`} size={4}>
            {title}
          </Title>
          <SubTitle className="column" size={4}>
            {subtitle}
          </SubTitle>
          {toolbar && <div className="toolbar">{toolbar}</div>}
          <div
            onClick={onToggle}
            className="service-need-section-title-trigger"
            data-qa="collapsible-trigger"
          >
            <FontAwesomeIcon
              icon={open ? faChevronUp : faChevronDown}
              size="lg"
            />
          </div>
        </div>
        <div data-qa="content" className="accordion-content">
          {open ? children : null}
        </div>
      </div>
    </section>
  )
}

export default ToolbarAccordion

interface RestrictedProps {
  title: string
  subtitle: string
  statusLabel: StatusLabelType
}

export function RestrictedToolbar({
  title,
  subtitle,
  statusLabel
}: RestrictedProps) {
  return (
    <div className={'is-gapless columns'}>
      <RestrictedTitle className={'column is-5'} size={4}>
        {title}
      </RestrictedTitle>
      <RestrictedDate className={'column'} size={4}>
        {subtitle}
      </RestrictedDate>
      <StatusContainer>
        <StatusLabel status={statusLabel} />
      </StatusContainer>
    </div>
  )
}

const RestrictedTitle = styled(Title)`
  font-style: italic;
`

const RestrictedDate = styled(Title)`
  flex: auto !important;
`

const StatusContainer = styled.div`
  margin-right: 38px;
`
