import React from 'react'
import styled from 'styled-components'
import { PublicUnit } from '@evaka/lib-common/src/api-types/units/PublicUnit'
import { CareType } from '@evaka/lib-common/src/api-types/units/enums'
import { defaultMargins } from '@evaka/lib-components/src/white-space'
import colors from '@evaka/lib-components/src/colors'
import { useTranslation } from '~localization'

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
  const formatCareType = (type: CareType) => {
    switch (type) {
      case 'CENTRE':
      case 'FAMILY':
      case 'GROUP_FAMILY':
        return t.map.careTypes.DAYCARE.toLowerCase()
      case 'PRESCHOOL':
        return t.common.unit.careTypes.PRESCHOOL.toLowerCase()
      case 'PREPARATORY_EDUCATION':
        return t.common.unit.careTypes.PREPARATORY_EDUCATION.toLowerCase()
      case 'CLUB':
        return t.common.unit.careTypes.CLUB.toLowerCase()
    }
  }
  const careTypes = unit.type
    .sort((a, b) => {
      if (a === 'CENTRE') return -1
      if (b === 'CENTRE') return 1
      if (a === 'PREPARATORY_EDUCATION') return 1
      if (b === 'PREPARATORY_EDUCATION') return -1
      return 0
    })
    .map(formatCareType)
    .join(', ')

  return (
    <Wrapper onClick={onClick}>
      <MainRow>
        <UnitName>{unit.name}</UnitName>
        {distance !== null && <Distance>{distance}</Distance>}
      </MainRow>
      <UnitDetails>
        {provider}, {careTypes}
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
