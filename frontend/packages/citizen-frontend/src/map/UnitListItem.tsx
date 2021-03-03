import React from 'react'
import styled from 'styled-components'
import { PublicUnit } from '@evaka/lib-common/src/api-types/units/PublicUnit'
import { defaultMargins } from '@evaka/lib-components/src/white-space'
import colors from '@evaka/lib-components/src/colors'
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
  margin-bottom: ${defaultMargins.s};
  cursor: pointer;

  padding: ${defaultMargins.xs} ${defaultMargins.s} ${defaultMargins.xs}
    ${defaultMargins.L};
  margin: -${defaultMargins.xs} -${defaultMargins.s} -${defaultMargins.xs} -${defaultMargins.L};

  &:hover {
    background-color: ${colors.blues.lighter};
  }
`

const MainRow = styled.div`
  display: flex;
  justify-content: space-between;
`

const UnitName = styled.div`
  font-weight: 600;
  text-wrap: normal;
`

const Distance = styled.div`
  margin-left: ${defaultMargins.m};
  white-space: nowrap;
`

const UnitDetails = styled.div`
  font-weight: 600;
  font-size: 14px;
  line-height: 21px;
  color: ${colors.greyscale.dark};
`
