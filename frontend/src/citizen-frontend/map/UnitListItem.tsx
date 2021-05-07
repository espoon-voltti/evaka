// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'
import { PublicUnit } from 'lib-common/api-types/units/PublicUnit'
import { defaultMargins as dM } from 'lib-components/white-space'
import colors from 'lib-components/colors'
import { useTranslation } from '../localization'
import { formatCareTypes } from '../map/format'

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
        <UnitName>{unit.name}</UnitName>
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

const Wrapper = styled.div`
  margin-bottom: ${dM.s};
  cursor: pointer;

  padding: ${dM.xs} ${dM.s} ${dM.xs} ${dM.L};
  margin: ${dM.xs} -${dM.s} -${dM.xs} -${dM.L};

  &:hover {
    background-color: ${colors.brandEspoo.espooTurquoiseLight};
  }

  &:after {
    content: '';
    width: calc(100% - ${dM.XXL});
    background: ${colors.greyscale.lighter};
    height: 1px;
    display: block;
    position: absolute;
    margin-top: ${dM.xs};
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
  font-weight: 600;
`

const Distance = styled.div`
  margin-left: ${dM.m};
  white-space: nowrap;
`

const UnitDetails = styled.div`
  font-weight: 600;
  font-size: 14px;
  line-height: 21px;
  color: ${colors.greyscale.dark};
`
