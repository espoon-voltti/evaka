// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

import { AbsenceType } from 'lib-common/generated/api-types/absence'
import { absenceColors } from 'lib-customizations/common'

import { useTranslation } from '../../state/i18n'

interface InfoBallProps {
  type: AbsenceType
}

const InfoBall = styled.div<InfoBallProps>`
  vertical-align: top;
  height: 20px;
  width: 20px;
  border-radius: 50%;
  margin-right: 12px;
  margin-top: 2px;
  flex-shrink: 0;
  background: ${(p: InfoBallProps) => absenceColors[p.type]}};

  @media print {
    -webkit-print-color-adjust: exact;
    color-adjust: exact;
    margin-right: 6px;
    width: 16px;
    height: 16px;
  }
`

interface ColorInfoContainerProps {
  maxWidth?: number
  noMargin?: boolean
}

const ColorInfoContainer = styled.div<ColorInfoContainerProps>`
  display: flex;
  flex: auto;
  max-width: ${(props: ColorInfoContainerProps) =>
    props.maxWidth ? `${props.maxWidth}px` : `130px`};
  margin: ${(props: ColorInfoContainerProps) =>
    props.noMargin ? `` : `0 10px`};
`

interface Props {
  type: AbsenceType
  maxWidth?: number
  noMargin?: boolean
}

const ColorInfoItem = ({ type, maxWidth }: Props) => {
  const { i18n } = useTranslation()
  return (
    <ColorInfoContainer maxWidth={maxWidth} noMargin>
      <InfoBall type={type} />
      <div>{i18n.absences.absenceTypes[type]}</div>
    </ColorInfoContainer>
  )
}

export default ColorInfoItem
