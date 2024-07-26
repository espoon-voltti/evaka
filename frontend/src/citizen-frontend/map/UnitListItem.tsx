// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

import { PublicUnit } from 'lib-common/generated/api-types/daycare'
import { fontWeights } from 'lib-components/typography'
import { defaultMargins as dM } from 'lib-components/white-space'
import colors from 'lib-customizations/common'

import { useTranslation } from '../localization'

import { formatCareTypes } from './format'

type Props = {
  unit: PublicUnit
  distance: string | null
  onClick: () => void
}

export default React.memo(function UnitListItem({
  unit,
  distance,
  onClick
}: Props) {
  const t = useTranslation()

  const provider = t.common.unit.providerTypes[unit.providerType].toLowerCase()

  return (
    <Wrapper onClick={onClick} data-qa={`map-unit-list-${unit.id}`}>
      <MainRow>
        <UnitName translate="no">{unit.name}</UnitName>
        {distance !== null && <Distance>{distance}</Distance>}
      </MainRow>
      <UnitDetails>
        {provider},{' '}
        {formatCareTypes(t, unit.type)
          .map((text) => text.toLowerCase())
          .join(', ')}
      </UnitDetails>
    </Wrapper>
  )
})

const Wrapper = styled.button`
  background: none;
  border: none;
  display: block;
  width: calc(100% + ${dM.L} + ${dM.L});
  text-align: left;
  margin-bottom: ${dM.s};
  cursor: pointer;

  padding: ${dM.xs} ${dM.L};
  margin: 0 -${dM.L};

  &:hover {
    background-color: ${colors.main.m4};
  }

  &:after {
    content: '';
    width: calc(100% - ${dM.X3L});
    background: ${colors.grayscale.g15};
    height: 1px;
    display: block;
    position: absolute;
    margin-top: ${dM.xs};
    margin-bottom: -${dM.xs};
  }

  :first-child {
    margin-top: 0px;
  }
`

const MainRow = styled.div`
  display: flex;
  justify-content: space-between;
`

const UnitName = styled.div`
  font-weight: ${fontWeights.semibold};
`

const Distance = styled.div`
  margin-left: ${dM.m};
  white-space: nowrap;
`

const UnitDetails = styled.div`
  font-weight: ${fontWeights.semibold};
  font-size: 14px;
  line-height: 21px;
  color: ${colors.grayscale.g70};
`
