// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { useTranslation } from '~state/i18n'
import { CareTypeLabel } from '~types'

import '~components/common/CareTypeLabel.scss'
import { PlacementType } from '~types/placementdraft'
import styled from 'styled-components'

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

function CareTypeLabel({ type }: { type: CareTypeLabel }) {
  const { i18n } = useTranslation()

  return (
    <div className={`bold care-type-label is-${type}`}>
      <div>{type ? i18n.common.careTypeLabels[type] : type}</div>
    </div>
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
