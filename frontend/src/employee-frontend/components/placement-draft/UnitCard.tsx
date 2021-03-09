// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, {
  useEffect,
  useState,
  memo,
  Dispatch,
  SetStateAction
} from 'react'
import { Link } from 'react-router-dom'
import styled from 'styled-components'
import LocalDate from '@evaka/lib-common/local-date'
import InlineButton from '@evaka/lib-components/atoms/buttons/InlineButton'
import Title from '@evaka/lib-components/atoms/Title'
import Loader from '@evaka/lib-components/atoms/Loader'

import { useTranslation } from '../../state/i18n'
import { Loading, Result } from '@evaka/lib-common/api'
import { getOccupancyRates, OccupancyResponse } from '../../api/unit'

import colors from '@evaka/lib-components/colors'
import UnderRowStatusIcon from '@evaka/lib-components/atoms/StatusIcon'
import { faCheck } from '@evaka/lib-icons'

import { formatPercentage } from '../utils'
import { Occupancy } from '../../types/unit'
import { DaycarePlacementPlan } from '../../types/placementdraft'
import { Unit } from '../../state/placementdraft'
import { UUID } from '../../types'

const MarginBox = styled.div`
  margin: 1rem;
  text-align: center;
`

const Card = styled.div<{ active: boolean }>`
  margin: 2rem 1rem;
  border: 1px solid gainsboro;
  border-top: 5px solid gainsboro;
  background: white;
  padding: 2rem;
  flex: 1 0 30%;
  max-width: calc(33% - 2rem);
  position: relative;
  text-align: center;
  ${({ active }) =>
    active &&
    `
    box-shadow: 0px 0px 6px 4px lightgray;
    border-top: 5px solid ${colors.brandEspoo.espooBlue};
    border-width: 5px 0 0 0;
  `}
`

const CardTitle = styled(Title)`
  min-height: 50px;
`

const RemoveBtn = styled.a`
  position: absolute;
  right: 0;
  top: -2rem;
`

const FailText = styled.div`
  color: ${colors.accents.red};
`

const Values = styled.div`
  margin: 1rem 0;
  display: flex;
  flex-direction: column;
  align-items: center;
`

const ValueHeading = styled.span`
  font-weight: 600;
`

const ValuePercentage = styled.span`
  color: ${colors.greyscale.dark};
  font-size: 3rem;
`

const OccupancyContainer = styled.div`
  height: 135px;
  display: flex;
  flex-direction: column;
  justify-content: center;
`

const Warning = styled.div`
  color: ${colors.accents.orangeDark};
  font-size: 0.75em;
  display: flex;
  flex-direction: row;
  flex-wrap: nowrap;
  justify-content: center;
  align-items: center;
`

interface OccupancyValueProps {
  type: 'confirmed' | 'planned'
  heading: string
  occupancy: Occupancy
}

function OccupancyValue({ type, heading, occupancy }: OccupancyValueProps) {
  return (
    <Values data-qa={`occupancies-maximum-${type}`}>
      <ValueHeading>{heading}</ValueHeading>
      <ValuePercentage>
        {formatPercentage(occupancy.percentage)}
      </ValuePercentage>
    </Values>
  )
}

interface Props {
  unitId: string
  unitName: string
  startDate: LocalDate
  endDate: LocalDate
  additionalUnits: Unit[]
  setAdditionalUnits: Dispatch<SetStateAction<Unit[]>>
  setPlacement: Dispatch<SetStateAction<DaycarePlacementPlan>>
  isSelectedUnit: boolean
  displayGhostUnitWarning: boolean
}

const MemoizedCard = memo(function UnitCard({
  unitId,
  unitName,
  startDate,
  endDate,
  additionalUnits,
  setAdditionalUnits,
  setPlacement,
  isSelectedUnit,
  displayGhostUnitWarning
}: Props) {
  const { i18n } = useTranslation()

  const [occupancies, setOccupancies] = useState<Result<OccupancyResponse>>(
    Loading.of()
  )

  const isRemovable = additionalUnits.map((item) => item.id).includes(unitId)

  useEffect(() => {
    if (unitId) {
      setOccupancies(Loading.of())
      const occupancyStartDate = startDate.isBefore(LocalDate.today())
        ? LocalDate.today()
        : startDate
      const maxDate = occupancyStartDate.addYears(1)
      const occupancyEndDate = endDate.isAfter(maxDate) ? maxDate : endDate
      void getOccupancyRates(
        unitId,
        occupancyStartDate,
        occupancyEndDate,
        'PLANNED'
      ).then(setOccupancies)
    }

    return () => {
      setOccupancies(Loading.of())
    }
  }, [unitId, startDate, endDate])

  function removeUnit(id: UUID) {
    setPlacement((prevPlacement) =>
      prevPlacement.unitId === id
        ? { ...prevPlacement, unitId: '' }
        : prevPlacement
    )
    return setAdditionalUnits((prevUnits) => {
      return prevUnits.filter((unit) => unit.id !== id)
    })
  }

  function selectUnit(unitId: UUID) {
    setPlacement((prevPlacement) => ({
      ...prevPlacement,
      unitId
    }))
  }

  return (
    <Card active={isSelectedUnit} data-qa="placement-item">
      {isRemovable && (
        <RemoveBtn role="button" onClick={() => removeUnit(unitId)}>
          {i18n.placementDraft.card.remove}
        </RemoveBtn>
      )}
      <CardTitle size={3}>{unitName}</CardTitle>
      <MarginBox>
        <Link
          to={`/units/${unitId}/unit-info?start=${startDate.formatIso()}`}
          target="_blank"
        >
          {i18n.placementDraft.card.unitLink}
        </Link>
      </MarginBox>
      <OccupancyContainer>
        {occupancies.isLoading ? (
          <Loader />
        ) : occupancies.isFailure ? (
          <FailText>{i18n.unit.occupancy.fail}</FailText>
        ) : occupancies.value.max ? (
          <OccupancyValue
            type="planned"
            heading={i18n.placementDraft.card.title}
            occupancy={occupancies.value.max}
          />
        ) : (
          <span>{i18n.unit.occupancy.noValidValues}</span>
        )}
      </OccupancyContainer>
      {displayGhostUnitWarning ? (
        <Warning>
          {i18n.childInformation.placements.warning.ghostUnit}
          <UnderRowStatusIcon status="warning" />
        </Warning>
      ) : null}
      <MarginBox>
        <InlineButton
          disabled={false}
          dataQa="select-placement-unit"
          onClick={() => selectUnit(unitId)}
          icon={(isSelectedUnit && faCheck) || undefined}
          text={
            isSelectedUnit
              ? i18n.placementDraft.selectedUnit
              : i18n.common.select
          }
        />
      </MarginBox>
    </Card>
  )
})

export default MemoizedCard
