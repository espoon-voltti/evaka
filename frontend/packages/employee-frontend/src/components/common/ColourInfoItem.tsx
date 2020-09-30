// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'
import '../absences/ColorInfo.scss'
import { AbsenceColours } from '~utils/colours'
import { AbsenceType } from '~types/absence'
import { useTranslation } from '~state/i18n'

interface InfoBallProps {
  type: AbsenceType
}

const InfoBall = styled.div<InfoBallProps>`
  vertical-align: top;
  height: 20px;
  width: 20px;
  border-radius: 100%;
  margin-right: 12px;
  margin-top: 2px;
  flex-shrink: 0;
  background: ${(props: InfoBallProps) => {
    switch (props.type) {
      case 'OTHER_ABSENCE':
        return AbsenceColours.Other
      case 'SICKLEAVE':
        return AbsenceColours.Sick
      case 'UNKNOWN_ABSENCE':
        return AbsenceColours.Unknown
      case 'PLANNED_ABSENCE':
        return AbsenceColours.Planned
      case 'TEMPORARY_RELOCATION':
        return AbsenceColours.Relocated
      case 'TEMPORARY_VISITOR':
        return AbsenceColours.Visitor
      case 'PARENTLEAVE':
        return AbsenceColours.Parentleave
      case 'FORCE_MAJEURE':
        return AbsenceColours.Uncharged
      case 'PRESENCE':
        return AbsenceColours.Presence
      default:
        return 'none'
    }
  }};
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
