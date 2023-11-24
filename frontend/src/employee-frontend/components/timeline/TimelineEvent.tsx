// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { faChevronDown, faChevronUp } from 'Icons'
import React, { useState } from 'react'
import { Link } from 'react-router-dom'
import styled from 'styled-components'

import FiniteDateRange from 'lib-common/finite-date-range'
import { TooltipWithoutAnchor } from 'lib-components/atoms/Tooltip'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'

import { WithRange } from './common'
import { EventRenderer } from './renderers'

export default function TlEvent<T extends WithRange>({
  event,
  renderer,
  left,
  width,
  timelineRange,
  zoom
}: {
  event: T
  renderer: EventRenderer<T>
  left: number
  width: number
  timelineRange: FiniteDateRange
  zoom: number
}) {
  const [open, setOpen] = useState(false)
  return (
    <TlEventContainer left={left} width={width} color={renderer.color(event)}>
      <FixedSpaceColumn spacing="xs">
        <Header>
          <StickyHeaderPositioner>
            <HeaderInner>
              {renderer.NestedContent && (
                <ExpanderButton>
                  <IconButton
                    icon={open ? faChevronUp : faChevronDown}
                    size="xs"
                    aria-label={open ? 'sulje' : 'avaa'}
                    onClick={() => setOpen(!open)}
                  />
                </ExpanderButton>
              )}
              <SummaryLabel>
                {renderer.linkProvider ? (
                  <Link to={renderer.linkProvider(event)} target="_blank">
                    <renderer.Summary elem={event} />
                  </Link>
                ) : (
                  <renderer.Summary elem={event} />
                )}
              </SummaryLabel>
            </HeaderInner>
            {renderer.Tooltip && (
              <FixedTooltip
                tooltip={<renderer.Tooltip elem={event} />}
                width="large"
                position="bottom"
              />
            )}
          </StickyHeaderPositioner>
        </Header>
        {open && renderer.NestedContent && (
          <ExpandedContent>
            {!!renderer.Metadata && (
              <StickyHeaderPositioner>
                <renderer.Metadata elem={event} />
              </StickyHeaderPositioner>
            )}
            <renderer.NestedContent
              elem={event}
              timelineRange={timelineRange}
              zoom={zoom}
            />
          </ExpandedContent>
        )}
      </FixedSpaceColumn>
    </TlEventContainer>
  )
}

const TlEventContainer = styled.div<{
  left: number
  width: number
  color: string
}>`
  position: relative;
  left: ${(p) => p.left}px;
  width: ${(p) => p.width}px;
  min-width: ${(p) => p.width}px;
  max-width: ${(p) => p.width}px;
  background-color: ${(p) => p.color};
  border: 1px solid black;
`

const StickyHeaderPositioner = styled.div`
  position: sticky;
  left: 0;
  display: inline-block;
  max-width: 100%;
  height: 100%;
`

const Header = styled.div`
  width: 100%;
  height: 30px;

  &:hover {
    ${StickyHeaderPositioner} {
      z-index: 1;
    }
  }
  &:not(:hover) {
    .tooltip {
      display: none;
    }
  }
`

const HeaderInner = styled(FixedSpaceRow)`
  overflow: hidden;
  max-width: 100%;
  display: flex;
  align-items: center;
  padding-left: 4px;
  height: 100%;
`

const ExpanderButton = styled.div``

const SummaryLabel = styled.div`
  display: inline-block;
  white-space: nowrap;
`

const ExpandedContent = styled.div`
  width: 100%;
`

const FixedTooltip = styled(TooltipWithoutAnchor)`
  position: relative;
  top: 0;
  left: 0;
  width: 100%;
  > div {
    width: 100%;
  }
`
