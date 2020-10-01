// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

import { useTranslation } from '~state/i18n'
import { CareTypeLabel } from '~types'
import { PlacementType } from '~types/placementdraft'
import Colors from 'components/shared/Colors'

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
        return Colors.accents.green
      case 'connected':
        return Colors.accents.orange
      case 'preschool':
        return Colors.accents.water
      case 'preparatory':
        return Colors.accents.water
      case 'backup-care':
        return Colors.accents.yellow
      case 'club':
        return Colors.greyscale.lighter
      default:
        return Colors.greyscale.white
    }
  }};
  color: ${(props: CareTypeLabelContainerProps) => {
    switch (props.type) {
      case 'daycare':
        return Colors.greyscale.dark
      case 'connected':
        return Colors.greyscale.dark
      case 'preschool':
        return Colors.greyscale.dark
      case 'preparatory':
        return Colors.greyscale.dark
      case 'backup-care':
        return Colors.greyscale.dark
      case 'club':
        return Colors.greyscale.dark
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
      {(type === 'PRESCHOOL_DAYCARE' || type === 'PREPARATORY_DAYCARE') && (
        <CareTypeLabel type="connected" />
      )}
    </CareTypeLabelsContainer>
  )
}

export default CareTypeLabel
