// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { ReactElement } from 'react'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import styled from 'styled-components'

import { faChevronUp, faChevronDown } from 'icon-set'
import '~components/common/ToolbarAccordion.scss'
import Title from '~components/shared/atoms/Title'
import StatusLabel, { StatusLabelType } from '~components/common/StatusLabel'
import colors from '@evaka/lib-components/src/colors'
import { Gap } from '~components/shared/layout/white-space'

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
      <AccordionCollapseWrapper>
        <GaplessColumns showBorder={showBorder}>
          <Title className={`column ${subtitle ? 'is-5' : 'is-8'}`} size={4}>
            {title}
          </Title>
          <Gap horizontal size={'s'} />
          <SubTitle className="column" size={4}>
            {subtitle}
          </SubTitle>
          {toolbar && <div className="toolbar">{toolbar}</div>}
          <ServiceNeedSectionTitleTrigger
            onClick={onToggle}
            data-qa="collapsible-trigger"
          >
            <FontAwesomeIcon
              icon={open ? faChevronUp : faChevronDown}
              size="lg"
            />
          </ServiceNeedSectionTitleTrigger>
        </GaplessColumns>
        <div data-qa="content" className="accordion-content">
          {open ? children : null}
        </div>
      </AccordionCollapseWrapper>
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
    <GaplessColumns>
      <RestrictedTitle className={'column is-5'} size={4}>
        {title}
      </RestrictedTitle>
      <RestrictedDate className={'column'} size={4}>
        {subtitle}
      </RestrictedDate>
      <StatusContainer>
        <StatusLabel status={statusLabel} />
      </StatusContainer>
    </GaplessColumns>
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

const AccordionCollapseWrapper = styled.div`
  margin: 20px 0;
`

interface GaplessColumnsProps {
  showBorder?: boolean
}

const GaplessColumns = styled.div<GaplessColumnsProps>`
  display: flex;
  border: ${(p) =>
    p.showBorder ? 'border-bottom: 1px solid $grey-light' : 'none'};
  padding-bottom: ${(p) => (p.showBorder ? '11px' : '0')};
`

const ServiceNeedSectionTitleTrigger = styled.div`
  color: ${colors.greyscale.dark};
  cursor: pointer;
  margin-left: 20px;
`
