import React from 'react'
import styled from 'styled-components'
import Colors from '~components/shared/Colors'
import { PlacementType } from '~types/placementdraft'
import Tooltip from '~components/shared/atoms/Tooltip'
import { useTranslation } from '~state/i18n'

const Circle = styled.div`
  width: 34px;
  height: 34px;
  min-width: 34px;
  min-height: 34px;
  max-width: 34px;
  max-height: 34px;
  background-color: ${Colors.accents.green};
  border-radius: 100%;
`
const HalfCircle = styled.div`
  width: 17px;
  height: 34px;
  min-width: 17px;
  min-height: 34px;
  max-width: 17px;
  max-height: 34px;
  background-color: ${Colors.accents.green};
  border-top-left-radius: 17px;
  border-bottom-left-radius: 17px;
`
export interface Props {
  type: PlacementType
}

export default React.memo(function PlacementCircle({ type }: Props) {
  const { i18n } = useTranslation()

  return (
    <Tooltip tooltip={<span>{i18n.placement.type[type]}</span>}>
      {['DAYCARE_PART_TIME', 'PRESCHOOL', 'PREPARATORY'].includes(type) ? (
        <HalfCircle />
      ) : (
        <Circle />
      )}
    </Tooltip>
  )
})
