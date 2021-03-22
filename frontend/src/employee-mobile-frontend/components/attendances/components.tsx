// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'
import { AttendanceChild } from '../../api/attendances'

import AsyncButton from '@evaka/lib-components/atoms/buttons/AsyncButton'
import Button from '@evaka/lib-components/atoms/buttons/Button'
import InlineButton from '@evaka/lib-components/atoms/buttons/InlineButton'
import colors from '@evaka/lib-components/colors'
import { defaultMargins } from '@evaka/lib-components/white-space'
import { Label } from '@evaka/lib-components/typography'
import Title from '@evaka/lib-components/atoms/Title'
import HorizontalLine from '@evaka/lib-components/atoms/HorizontalLine'
import { useTranslation } from '../../state/i18n'
import { CareType, formatCareType } from '../../types'

// TODO: Refactor all of these to actual components that show up in storybook
export const WideButton = styled(Button)`
  width: 100%;
`

export const BigWideButton = styled(WideButton)`
  white-space: normal;
  height: 64px;
`

export const WideInlineButton = styled(InlineButton)`
  white-space: normal;
  width: 100%;
`

export const WideAsyncButton = styled(AsyncButton)`
  @media screen and (max-width: 1023px) {
    margin-bottom: ${defaultMargins.s};
    width: 100%;
    white-space: normal;
    height: 64px;
  }
`

export const InlineWideAsyncButton = styled(WideAsyncButton)`
  border: none;
`

export interface CustomButtonProps {
  color?: string
  backgroundColor?: string
  borderColor?: string
}

export const CustomButton = styled(Button)<CustomButtonProps>`
  @media screen and (max-width: 1023px) {
    margin-bottom: ${defaultMargins.s};
    width: calc(50vw - 40px);
    white-space: normal;
    height: 64px;
  }

  @media screen and (min-width: 1024px) {
    margin-right: ${defaultMargins.s};
  }
  ${(p) => (p.color ? `color: ${p.color};` : '')}
  ${(p) => (p.backgroundColor ? `background-color: ${p.backgroundColor};` : '')}
  ${(p) => (p.borderColor ? `border-color: ${p.borderColor};` : '')}

  :hover {
    ${(p) => (p.color ? `color: ${p.color};` : '')}
    ${(p) =>
      p.backgroundColor ? `background-color: ${p.backgroundColor};` : ''}
  ${(p) => (p.borderColor ? `border-color: ${p.borderColor};` : '')}
  }
`

export const Flex = styled.div`
  @media screen and (max-width: 1023px) {
    justify-content: space-between;
  }
  display: flex;
  flex-direction: row;
  flex-wrap: wrap;
`

export const FlexColumn = styled.div`
  @media screen and (max-width: 1023px) {
    justify-content: space-between;
  }
  display: flex;
  flex-direction: column;
  flex-wrap: wrap;
`

export const FlexLabel = styled(Label)`
  display: flex;
  align-items: center;

  span {
    margin-right: ${defaultMargins.m};
  }
`

export const ArrivalTime = styled.div`
  font-family: Montserrat, sans-serif;
  font-size: 18px;
  font-style: normal;
  font-weight: 500;
  line-height: 27px;
  letter-spacing: 0em;
  display: flex;
  justify-content: space-between;
  color: ${colors.greyscale.dark};
`

export const CustomHorizontalLine = styled(HorizontalLine)`
  margin-bottom: 14px;
  margin-top: 14px;
`

export const Actions = styled.div`
  width: 100%;
  display: flex;

  * > button {
    width: 50%;
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
  color: ${colors.blues.dark};
  margin-top: ${defaultMargins.s};
  margin-left: ${defaultMargins.s};
  margin-bottom: ${defaultMargins.s};
`

const AbsentFromWrapper = styled.div`
  display: flex;
  flex-direction: column;
`

const AbsenceTitle = styled(Title)`
  color: ${colors};
  font-size: 18px;
  font-style: normal;
  font-weight: 500;
  line-height: 27px;
  letter-spacing: 0em;
  text-align: left;
  margin-top: 0;
  margin-bottom: 0;
`

const InfoText = styled.div``

interface AbsentFromProps {
  child: AttendanceChild
  absentFrom: CareType[]
}

export function AbsentFrom({ child, absentFrom }: AbsentFromProps) {
  const { i18n } = useTranslation()

  return (
    <AbsentFromWrapper>
      <CustomHorizontalLine />
      <AbsenceTitle size={2}>{i18n.attendances.absenceTitle}</AbsenceTitle>
      <InfoText>
        {absentFrom.length > 1
          ? i18n.attendances.missingFromPlural
          : i18n.attendances.missingFrom}
        :{' '}
        {absentFrom.map((careType) => (
          <div key={careType}>
            {formatCareType(
              careType,
              child.placementType,
              child.entitledToFreeFiveYearsOldDaycare,
              i18n
            )}
          </div>
        ))}
      </InfoText>
    </AbsentFromWrapper>
  )
}
