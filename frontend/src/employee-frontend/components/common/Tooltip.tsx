// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import ReactTooltip from 'react-tooltip'
import styled from 'styled-components'
import colors from 'lib-components/colors'

interface Props {
  children: React.ReactNode
  tooltipId: string
  tooltipText: string
  place: 'left' | 'right' | 'top' | 'bottom' | undefined
  className?: string
  delayShow?: number
}

const ContentWrapper = styled.div`
  width: fit-content;

  .type-dark {
    background: ${colors.greyscale.dark};
    color: ${colors.greyscale.white};
    font-family: 'Open Sans', 'Arial', sans-serif;
    padding: 8px 12px;

    &.place-right {
      &:after {
        border-right-color: ${colors.greyscale.dark};
      }
    }
    &.place-left {
      &:after {
        border-left-color: ${colors.greyscale.dark};
      }
    }
    &.place-top {
      &:after {
        border-top-color: ${colors.greyscale.dark};
      }
    }
    &.place-bottom {
      &:after {
        border-bottom-color: ${colors.greyscale.dark};
      }
    }
  }
`

function Tooltip({
  children,
  tooltipText,
  tooltipId,
  place,
  className,
  delayShow
}: Props) {
  return (
    <>
      <ContentWrapper data-for={tooltipId} data-tip={tooltipText}>
        {children}
        <ReactTooltip
          id={tooltipId}
          type="dark"
          place={place}
          effect="solid"
          multiline={true}
          className={className}
          delayShow={delayShow}
        />
      </ContentWrapper>
    </>
  )
}

export default Tooltip
