// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'
import '../absences/ColorInfo.scss'
import { AbsenceType } from '~types/absence'
import { useTranslation } from '~state/i18n'
import { absenceColours } from '@evaka/lib-components/src/colors'

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
  background: ${(props: InfoBallProps) => {
    switch (props.type) {
      case 'OTHER_ABSENCE':
        return absenceColours.OTHER_ABSENCE
      case 'SICKLEAVE':
        return absenceColours.SICKLEAVE
      case 'UNKNOWN_ABSENCE':
        return absenceColours.UNKNOWN_ABSENCE
      case 'PLANNED_ABSENCE':
        return absenceColours.PLANNED_ABSENCE
      case 'TEMPORARY_RELOCATION':
        return absenceColours.TEMPORARY_RELOCATION
      case 'TEMPORARY_VISITOR':
        return absenceColours.TEMPORARY_VISITOR
      case 'PARENTLEAVE':
        return absenceColours.PARENTLEAVE
      case 'FORCE_MAJEURE':
        return absenceColours.FORCE_MAJEURE
      case 'PRESENCE':
        return absenceColours.PRESENCE
      default:
        return 'none'
    }
  }};

  @media print {
    -webkit-print-color-adjust: exact;
    color-adjust: exact;
    margin-right: 6px;
    width: 16px;
    height: 16px;
  }
`

interface ColourInfoContainerProps {
  maxWidth?: number
  noMargin?: boolean
}

const ColourInfoContainer = styled.div<ColourInfoContainerProps>`
  display: flex;
  flex: auto;
  max-width: ${(props: ColourInfoContainerProps) =>
    props.maxWidth ? `${props.maxWidth}px` : `130px`};
  margin: ${(props: ColourInfoContainerProps) =>
    props.noMargin ? `` : `0 10px`};
`

interface ColourInfoItemProps {
  type: AbsenceType
  maxWidth?: number
  noMargin?: boolean
}

const ColourInfoItem = ({ type, maxWidth }: ColourInfoItemProps) => {
  const { i18n } = useTranslation()
  return (
    <ColourInfoContainer maxWidth={maxWidth} noMargin>
      <InfoBall type={type} />
      <div>{i18n.absences.absenceTypes[type]}</div>
    </ColourInfoContainer>
  )
}

export default ColourInfoItem
