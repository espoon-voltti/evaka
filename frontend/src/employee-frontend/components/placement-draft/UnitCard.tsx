// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { isLoading, Result } from 'lib-common/api'
import { PublicUnit } from 'lib-common/generated/api-types/daycare'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'
import { formatPercentage } from 'lib-common/utils/number'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import UnderRowStatusIcon from 'lib-components/atoms/StatusIcon'
import Title from 'lib-components/atoms/Title'
import { fontWeights } from 'lib-components/typography'
import colors from 'lib-customizations/common'
import { faCheck } from 'lib-icons'
import React, { Dispatch, SetStateAction } from 'react'
import { Link } from 'react-router-dom'
import styled from 'styled-components'
import { getOccupancyRates, OccupancyResponse } from '../../api/unit'
import { useTranslation } from '../../state/i18n'
import { DaycarePlacementPlan } from '../../types/placementdraft'
import { Occupancy } from '../../types/unit'
import { useApiState } from 'lib-common/utils/useRestApi'
import { renderResult } from '../async-rendering'

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
    border-top: 5px solid ${colors.main.dark};
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

const Values = styled.div`
  margin: 1rem 0;
  display: flex;
  flex-direction: column;
  align-items: center;
`

const ValueHeading = styled.span`
  font-weight: ${fontWeights.semibold};
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

async function getUnitOccupancies(
  unitId: UUID,
  startDate: LocalDate,
  endDate: LocalDate
): Promise<Result<OccupancyResponse>> {
  const occupancyStartDate = startDate.isBefore(LocalDate.today())
    ? LocalDate.today()
    : startDate
  const maxDate = occupancyStartDate.addYears(1)
  const occupancyEndDate = endDate.isAfter(maxDate) ? maxDate : endDate
  return getOccupancyRates(
    unitId,
    occupancyStartDate,
    occupancyEndDate,
    'PLANNED'
  )
}

interface Props {
  unitId: string
  unitName: string
  startDate: LocalDate
  endDate: LocalDate
  additionalUnits: PublicUnit[]
  setAdditionalUnits: Dispatch<SetStateAction<PublicUnit[]>>
  setPlacement: Dispatch<SetStateAction<DaycarePlacementPlan>>
  isSelectedUnit: boolean
  displayGhostUnitWarning: boolean
}

export default React.memo(function UnitCard({
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

  const [occupancies] = useApiState(
    () => getUnitOccupancies(unitId, startDate, endDate),
    [unitId, startDate, endDate]
  )

  const isRemovable = additionalUnits.map((item) => item.id).includes(unitId)

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
    <Card
      active={isSelectedUnit}
      data-qa="placement-item"
      data-isloading={isLoading(occupancies)}
    >
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
        {renderResult(occupancies, (occupancies) =>
          occupancies.max ? (
            <OccupancyValue
              type="planned"
              heading={i18n.placementDraft.card.title}
              occupancy={occupancies.max}
            />
          ) : (
            <span>{i18n.unit.occupancy.noValidValues}</span>
          )
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
          data-qa="select-placement-unit"
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
