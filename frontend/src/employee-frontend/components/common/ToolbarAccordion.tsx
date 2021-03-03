// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { ReactElement } from 'react'
import styled from 'styled-components'
import { faChevronUp, faChevronDown } from '@evaka/lib-icons'
import { H4 } from '@evaka/lib-components/src/typography'
import { Gap } from '@evaka/lib-components/src/white-space'
import IconButton from '@evaka/lib-components/src/atoms/buttons/IconButton'
import StatusLabel, {
  StatusLabelType
} from '../../components/common/StatusLabel'

interface Props {
  title?: string
  subtitle?: string
  children: React.ReactNode
  onToggle: () => void
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
    <section data-qa={dataQa} data-status={open ? 'open' : 'closed'}>
      <AccordionCollapseWrapper>
        <TitleRow showBorder={showBorder}>
          <Title>
            <H4 noMargin>{title}</H4>
            <Gap size="s" horizontal />
            <H4 noMargin>{subtitle}</H4>
          </Title>
          <Gap size="m" horizontal />
          <Toolbar>
            {toolbar ? (
              <>
                {toolbar}
                <Gap size="s" horizontal />
              </>
            ) : null}
            <IconButton
              icon={open ? faChevronUp : faChevronDown}
              onClick={onToggle}
              dataQa="collapsible-trigger"
            />
          </Toolbar>
        </TitleRow>
        <div data-qa="content">
          {open ? (
            <>
              <Gap size="m" />
              {children}
            </>
          ) : null}
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
    <TitleRow>
      <Title>
        <RestrictedTitle noMargin>{title}</RestrictedTitle>
        <Gap size="s" horizontal />
        <H4 noMargin>{subtitle}</H4>
      </Title>
      <StatusContainer>
        <StatusLabel status={statusLabel} />
      </StatusContainer>
    </TitleRow>
  )
}

const RestrictedTitle = styled(H4)`
  font-style: italic;
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

const TitleRow = styled.div<GaplessColumnsProps>`
  display: flex;
  flex-direction: row;
  flex-wrap: nowrap;
  justify-content: space-between;
  align-items: center;
  border: ${(p) =>
    p.showBorder ? 'border-bottom: 1px solid $grey-light' : 'none'};
  padding-bottom: ${(p) => (p.showBorder ? '11px' : '0')};
`

const Title = styled.div`
  display: flex;
  flex-direction: row;
  flex-wrap: nowrap;
  align-items: center;
`

const Toolbar = styled.div`
  display: flex;
  flex-direction: row;
  flex-wrap: nowrap;
  align-items: center;
`
