// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import ReactTooltip from 'react-tooltip'
import styled from 'styled-components'
import colors from 'lib-customizations/common'

interface Props {
  children: React.ReactNode
  tooltipId: string
  tooltipText: string
  place: 'left' | 'right' | 'top' | 'bottom' | undefined
  className?: string
  delayShow?: number
  dataQa?: string
}

const ContentWrapper = styled.div`
  width: fit-content;

  .type-dark {
    background: ${colors.grayscale.g70};
    color: ${colors.grayscale.g0};
    font-family: 'Open Sans', 'Arial', sans-serif;
    padding: 8px 12px;

    &.place-right {
      &:after {
        border-right-color: ${colors.grayscale.g70};
      }
    }
    &.place-left {
      &:after {
        border-left-color: ${colors.grayscale.g70};
      }
    }
    &.place-top {
      &:after {
        border-top-color: ${colors.grayscale.g70};
      }
    }
    &.place-bottom {
      &:after {
        border-bottom-color: ${colors.grayscale.g70};
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
  delayShow,
  dataQa
}: Props) {
  return (
    <>
      <ContentWrapper
        data-for={tooltipId}
        data-tip={tooltipText}
        data-qa={dataQa}
      >
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
