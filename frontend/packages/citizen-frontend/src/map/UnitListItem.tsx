import React from 'react'
import styled from 'styled-components'
import { defaultMargins } from '@evaka/lib-components/src/white-space'
import { PublicUnit } from '@evaka/lib-common/src/api-types/units/PublicUnit'
import colors from '@evaka/lib-components/src/colors'
import { useTranslation } from '~localization'

type Props = {
  unit: PublicUnit
  distance?: string
}

export default React.memo(function UnitListItem({ unit, distance }: Props) {
  const t = useTranslation()

  const lang = t.common.unit.languages[unit.language]
  const provider = t.common.unit.providerTypes[unit.providerType].toLowerCase()
  const careTypes = unit.type
    .sort((a, b) => {
      if (a === 'CENTRE') return -1
      if (b === 'CENTRE') return 1
      if (a === 'PREPARATORY_EDUCATION') return 1
      if (b === 'PREPARATORY_EDUCATION') return -1
      return 0
    })
    .map((type) => t.common.unit.careTypes[type].toLowerCase())
    .join(', ')

  return (
    <Wrapper>
      <MainRow>
        <UnitName>{unit.name}</UnitName>
        {distance && <Distance>{distance}</Distance>}
      </MainRow>
      <UnitDetails>
        {lang}, {provider}, {careTypes}
      </UnitDetails>
    </Wrapper>
  )
})

const Wrapper = styled.div`
  padding: ${defaultMargins.xs};
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
`

const UnitDetails = styled.div`
  font-weight: 600;
  font-size: 14px;
  line-height: 21px;
  color: ${colors.greyscale.dark};
`
