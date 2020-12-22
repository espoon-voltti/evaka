// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'
import { AttendanceChild } from '~api/attendances'

import AsyncButton from '@evaka/lib-components/src/atoms/buttons/AsyncButton'
import Button from '@evaka/lib-components/src/atoms/buttons/Button'
import InlineButton from '@evaka/lib-components/src/atoms/buttons/InlineButton'
import RoundIcon from '@evaka/lib-components/src/atoms/RoundIcon'
import colors from '@evaka/lib-components/src/colors'
import { defaultMargins } from '@evaka/lib-components/src/white-space'
import { Label } from '@evaka/lib-components/src/typography'
import { faExclamation } from '@evaka/lib-icons'
import { useTranslation } from '~state/i18n'
import { CareType, formatCareType } from '~types/absence'

// TODO: Refactor all of these to actual components that show up in storybook
export const WideButton = styled(Button)`
  width: 100%;
`

export const BigWideButton = styled(WideButton)`
  white-space: normal;
  height: 64px;
`

export const BigWideInlineButton = styled(InlineButton)`
  white-space: normal;
  height: 64px;
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

const AbsentFromWrapper = styled.div`
  display: flex;
`

const InfoText = styled.div`
  margin-left: ${defaultMargins.s};
`

interface AbsentFromProps {
  child: AttendanceChild
  absentFrom: CareType[]
}

export function AbsentFrom({ child, absentFrom }: AbsentFromProps) {
  const { i18n } = useTranslation()

  return (
    <AbsentFromWrapper>
      <RoundIcon
        color={colors.brandEspoo.espooTurquoise}
        size={'s'}
        content={faExclamation}
      />
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
