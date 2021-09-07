import React from 'react'
import styled from 'styled-components'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import RoundIcon from 'lib-components/atoms/RoundIcon'
import { faThermometer } from 'lib-icons'
import colors from 'lib-customizations/common'
import { AbsenceType } from 'lib-common/generated/enums'

interface Props {
  type: AbsenceType
}

export default React.memo(function AbsenceDay({ type }: Props) {
  if (type === 'SICKLEAVE')
    return (
      <AbsenceCell>
        <FixedSpaceRow spacing="xs" alignItems="center">
          <RoundIcon
            content={faThermometer}
            color={colors.accents.violet}
            size="m"
          />
          <div>Sairaus</div>
        </FixedSpaceRow>
      </AbsenceCell>
    )

  return (
    <AbsenceCell>
      <FixedSpaceRow spacing="xs" alignItems="center">
        <RoundIcon content="–" color={colors.primary} size="m" />
        <div>Muu syy</div>
      </FixedSpaceRow>
    </AbsenceCell>
  )
})

const AbsenceCell = styled.div`
  display: flex;
  justify-content: center;
  align-items: center;
  font-style: italic;
`
