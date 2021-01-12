// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

import { useTranslation } from '~state/i18n'
import { CareTypeLabel } from '~types'
import { PlacementType } from '~types/placementdraft'
import colors from '@evaka/lib-components/src/colors'

export interface Props {
  type?: CareTypeLabel
}

const CareTypeLabelsContainer = styled.div`
  display: flex;

  > * {
    margin-left: 10px;
  }

  > *:first-child {
    margin-left: 0;
  }
`

interface CareTypeLabelContainerProps {
  type: CareTypeLabel
}

const CareTypeLabelContainer = styled.div<CareTypeLabelContainerProps>`
  font-weight: 600;
  border-radius: 12px;
  height: 25px;
  padding: 0 10px;
  text-align: center;
  line-height: 25px;
  background: ${(props: CareTypeLabelContainerProps) => {
    switch (props.type) {
      case 'daycare':
        return colors.accents.green
      case 'preschool':
        return colors.primaryColors.dark
      case 'preparatory':
        return colors.accents.water
      case 'backup-care':
        return colors.accents.yellow
      case 'club':
        return colors.greyscale.lighter
      default:
        return colors.greyscale.white
    }
  }};
  color: ${(props: CareTypeLabelContainerProps) => {
    switch (props.type) {
      case 'daycare':
        return colors.greyscale.dark
      case 'preschool':
        return colors.greyscale.white
      case 'preparatory':
        return colors.greyscale.dark
      case 'backup-care':
        return colors.greyscale.dark
      case 'club':
        return colors.greyscale.dark
      default:
        return 'initial'
    }
  }};
`

function CareTypeLabel({ type }: { type: CareTypeLabel }) {
  const { i18n } = useTranslation()

  return (
    <CareTypeLabelContainer type={type}>
      <div>{type ? i18n.common.careTypeLabels[type] : type}</div>
    </CareTypeLabelContainer>
  )
}

export function careTypesFromPlacementType(type: PlacementType) {
  return (
    <CareTypeLabelsContainer>
      {type === 'CLUB' && <CareTypeLabel type="club" />}
      {(type === 'DAYCARE' || type === 'DAYCARE_PART_TIME') && (
        <CareTypeLabel type="daycare" />
      )}
      {(type === 'PRESCHOOL' || type === 'PRESCHOOL_DAYCARE') && (
        <CareTypeLabel type="preschool" />
      )}
      {(type === 'PREPARATORY' || type === 'PREPARATORY_DAYCARE') && (
        <CareTypeLabel type="preparatory" />
      )}
    </CareTypeLabelsContainer>
  )
}

export default CareTypeLabel
