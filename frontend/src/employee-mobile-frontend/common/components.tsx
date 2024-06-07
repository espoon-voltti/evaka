// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import styled, { css } from 'styled-components'

import HorizontalLine from 'lib-components/atoms/HorizontalLine'
import Title from 'lib-components/atoms/Title'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import { LegacyAsyncButton } from 'lib-components/atoms/buttons/LegacyAsyncButton'
import { MutateButton } from 'lib-components/atoms/buttons/MutateButton'
import { fontWeights } from 'lib-components/typography'
import {
  defaultMargins,
  isSpacingSize,
  SpacingSize
} from 'lib-components/white-space'
import colors from 'lib-customizations/common'

const wideButtonCss = css`
  @media screen and (max-width: 1023px) {
    margin-bottom: ${defaultMargins.s};
    width: 100%;
    white-space: normal;
    height: 64px;
  }
`

export const WideAsyncButton = styled(LegacyAsyncButton)`
  ${wideButtonCss}
`

export const WideMutateButton = styled(MutateButton)`
  ${wideButtonCss}
` as typeof MutateButton

export const InlineWideAsyncButton = styled(WideAsyncButton)`
  border: none;
`

export const FlexColumn = styled.div<{
  // eslint-disable-next-line @typescript-eslint/no-redundant-type-constituents
  paddingHorizontal?: SpacingSize | string
}>`
  @media screen and (max-width: 1023px) {
    justify-content: space-between;
  }
  display: flex;
  flex-direction: column;
  flex-wrap: wrap;
  padding: ${(p) =>
    `0 ${
      p.paddingHorizontal
        ? isSpacingSize(p.paddingHorizontal)
          ? defaultMargins[p.paddingHorizontal]
          : p.paddingHorizontal
        : 0
    }`};
`

export const ArrivalTime = styled.span`
  font-family: 'Open Sans', sans-serif;
  font-size: 16px;
  font-style: normal;
  font-weight: ${fontWeights.medium};
  line-height: 27px;
  letter-spacing: 0em;
  display: flex;
  flex-direction: row;
  color: ${colors.grayscale.g70};

  span:first-child {
    margin-right: ${defaultMargins.xs};
    font-weight: ${fontWeights.semibold};
  }
`

export const CustomHorizontalLine = styled(HorizontalLine)`
  margin-bottom: 14px;
  margin-top: 14px;
`

export const ServiceTime = styled.div`
  padding: ${defaultMargins.xxs};
  color: ${colors.grayscale.g70};
  text-align: center;
`

export const Actions = styled.div`
  width: 100%;
  display: flex;

  * > button {
    flex-grow: 1;
  }
`

export const CustomTitle = styled(Title)`
  margin-top: 0;
  margin-bottom: 0;
  text-align: center;
`

export const DailyNotes = styled.div`
  display: flex;
`

export const BackButtonInline = styled(InlineButton)`
  color: ${colors.main.m1};
  margin-top: ${defaultMargins.s};
  margin-left: ${defaultMargins.s};
  margin-bottom: ${defaultMargins.s};
  overflow-x: hidden;
  width: calc(100vw - 16px);
  display: flex;
`

export const TimeWrapper = styled.div`
  display: flex;
  flex-direction: column;
  align-items: center;
  font-size: 60px;
  color: ${colors.main.m1};
  font-weight: ${fontWeights.semibold};

  input {
    font-size: 60px;
    color: ${colors.main.m1};
    font-family: Montserrat, sans-serif;
    font-weight: ${fontWeights.light};
    border-bottom: none;
  }
`
