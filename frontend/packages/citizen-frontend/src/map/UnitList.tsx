import React from 'react'
import styled from 'styled-components'
import { ContentArea } from '@evaka/lib-components/src/layout/Container'
import { Result } from '@evaka/lib-common/src/api'
import { PublicUnit } from '@evaka/lib-common/src/api-types/units/PublicUnit'
import { SpinnerSegment } from '@evaka/lib-components/src/atoms/state/Spinner'
import ErrorSegment from '@evaka/lib-components/src/atoms/state/ErrorSegment'
import { useTranslation } from '~localization'
import UnitListItem from '~map/UnitListItem'

type Props = {
  filteredUnits: Result<PublicUnit[]>
}

export default React.memo(function UnitList({ filteredUnits }: Props) {
  const t = useTranslation()

  return (
    <Wrapper opaque className="unit-list">
      {filteredUnits.isLoading && <SpinnerSegment />}
      {filteredUnits.isFailure && (
        <ErrorSegment title={t.common.errors.genericGetError} />
      )}
      {filteredUnits.isSuccess &&
        filteredUnits.value.map((unit) => (
          <UnitListItem key={unit.id} unit={unit} />
        ))}
    </Wrapper>
  )
})

const Wrapper = styled(ContentArea)`
  box-sizing: border-box;
  width: 100%;
  overflow-y: auto;
  flex-grow: 1;
`
