// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Dispatch, SetStateAction, useCallback, useMemo } from 'react'
import styled from 'styled-components'

import { isLoading, Result } from 'lib-common/api'
import FiniteDateRange from 'lib-common/finite-date-range'
import { PublicUnit } from 'lib-common/generated/api-types/daycare'
import { OccupancyResponseSpeculated } from 'lib-common/generated/api-types/occupancy'
import LocalDate from 'lib-common/local-date'
import { useQueryResult } from 'lib-common/query'
import { UUID } from 'lib-common/types'
import { SelectionChip } from 'lib-components/atoms/Chip'
import { InternalLink } from 'lib-components/atoms/InternalLink'
import CrossIconButton from 'lib-components/atoms/buttons/CrossIconButton'
import { Bold, H1, InformationText, Title } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'

import { useTranslation } from '../../state/i18n'
import { renderResult } from '../async-rendering'
import WarningLabel from '../common/WarningLabel'
import { unitSpeculatedOccupancyRatesQuery } from '../unit/queries'

import { DaycarePlacementPlanForm } from './PlacementDraft'

const Numbers = styled.div`
  display: flex;
  gap: ${defaultMargins.s};
  justify-content: space-evenly;
`

const Number = styled(H1)`
  margin: 0;
  color: ${colors.main.m2};
`
const formatPercentage = (num?: number | null) =>
  num ? `${num.toFixed(1).replace('.', ',')} %` : '–'

interface OccupancyNumbersProps {
  title: string
  num3: number | null | undefined
  num6: number | null | undefined
  'data-qa'?: string
}

function OccupancyNumbers({
  title,
  num3,
  num6,
  'data-qa': dataQa
}: OccupancyNumbersProps) {
  const { i18n } = useTranslation()
  return (
    <>
      <Bold>{title}</Bold>
      <Gap size="xs" />
      <Numbers data-qa={dataQa}>
        <div>
          <InformationText>
            3 {i18n.common.datetime.monthShort.toLowerCase()}
          </InformationText>
          <Number data-qa="3months">{formatPercentage(num3)}</Number>
        </div>
        <div>
          <InformationText>
            6 {i18n.common.datetime.monthShort.toLowerCase()}
          </InformationText>
          <Number data-qa="6months">{formatPercentage(num6)}</Number>
        </div>
      </Numbers>
    </>
  )
}

const Card = styled.div<{ selected: boolean }>`
  display: flex;
  flex-direction: column;
  align-items: center;

  border-color: ${(p) => (p.selected ? colors.main.m2 : colors.grayscale.g35)};
  border-width: 2px;
  border-style: solid;
  border-radius: 4px;
  background: ${colors.grayscale.g0};
  box-shadow: 0 4px 4px 0 ${colors.grayscale.g100}26; // 26 = 15 % opacity
  padding: ${defaultMargins.L};
  flex: 1 0 30%;
  max-width: calc(33% - 1.5rem);
  position: relative;
  text-align: center;
`

const RemoveBtn = styled.div`
  position: absolute;
  top: 0;
  right: 0;
`

const OccupancyContainer = styled.div`
  display: flex;
  flex-direction: column;
  justify-content: center;
`

function useSpeculatedOccupancies(
  applicationId: UUID,
  unitId: UUID,
  period: FiniteDateRange,
  preschoolDaycarePeriod: FiniteDateRange | undefined
): Result<OccupancyResponseSpeculated> {
  const { start, end } = useMemo(() => {
    const start = period.start.isBefore(LocalDate.todayInSystemTz())
      ? LocalDate.todayInSystemTz()
      : period.start
    const maxDate = start.addYears(1)
    const end = period.end.isAfter(maxDate) ? maxDate : period.end
    return { start, end }
  }, [period.end, period.start])
  return useQueryResult(
    unitSpeculatedOccupancyRatesQuery(
      applicationId,
      unitId,
      start,
      end,
      preschoolDaycarePeriod?.start,
      preschoolDaycarePeriod?.end
    )
  )
}

interface Props {
  applicationId: UUID
  unitId: string
  unitName: string
  period: FiniteDateRange
  preschoolDaycarePeriod?: FiniteDateRange
  additionalUnits: PublicUnit[]
  setAdditionalUnits: Dispatch<SetStateAction<PublicUnit[]>>
  setPlacement: Dispatch<SetStateAction<DaycarePlacementPlanForm>>
  isSelectedUnit: boolean
  displayGhostUnitWarning: boolean
}

export default React.memo(function UnitCard({
  applicationId,
  unitId,
  unitName,
  period,
  preschoolDaycarePeriod,
  additionalUnits,
  setAdditionalUnits,
  setPlacement,
  isSelectedUnit,
  displayGhostUnitWarning
}: Props) {
  const { i18n } = useTranslation()

  const occupancies = useSpeculatedOccupancies(
    applicationId,
    unitId,
    period,
    preschoolDaycarePeriod
  )

  const isRemovable = additionalUnits.some((item) => item.id === unitId)

  const removeUnit = useCallback(() => {
    setPlacement((prev) =>
      prev.unitId === unitId ? { ...prev, unitId: '' } : prev
    )
    setAdditionalUnits((prev) => prev.filter((unit) => unit.id !== unitId))
  }, [setAdditionalUnits, setPlacement, unitId])

  const selectUnit = useCallback(
    (unitId: UUID) => setPlacement((prev) => ({ ...prev, unitId })),
    [setPlacement]
  )

  return (
    <Card
      data-qa={`placement-item-${unitId}`}
      data-isloading={isLoading(occupancies)}
      selected={isSelectedUnit}
    >
      {isRemovable && (
        <RemoveBtn role="button">
          <CrossIconButton active={false} onClick={removeUnit} />
        </RemoveBtn>
      )}
      <InternalLink
        to={`/units/${unitId}/unit-info?start=${period.start.formatIso()}`}
        text={<Title primary>{unitName}</Title>}
        newTab
      />
      <Gap size="m" />
      <OccupancyContainer>
        {renderResult(occupancies, (occupancies) => {
          if (!occupancies.max3Months || !occupancies.max6Months) {
            return <span>{i18n.unit.occupancy.noValidValues}</span>
          }
          return (
            <div>
              <OccupancyNumbers
                title={i18n.placementDraft.card.title}
                num3={occupancies.max3Months.percentage}
                num6={occupancies.max6Months.percentage}
                data-qa="current-occupancies"
              />
              <Gap size="m" />
              <OccupancyNumbers
                title={i18n.placementDraft.card.titleSpeculated}
                num3={occupancies.max3MonthsSpeculated?.percentage}
                num6={occupancies.max6MonthsSpeculated?.percentage}
                data-qa="speculated-occupancies"
              />
            </div>
          )
        })}
      </OccupancyContainer>
      <Gap size="XL" />
      {displayGhostUnitWarning && (
        <>
          <WarningLabel
            text={i18n.childInformation.placements.warning.ghostUnit}
          />
          <Gap size="s" />
        </>
      )}
      <SelectionChip
        data-qa="select-placement-unit"
        onChange={(checked) => selectUnit(checked ? unitId : '')}
        selected={isSelectedUnit}
        text={
          isSelectedUnit
            ? i18n.placementDraft.selectedUnit
            : i18n.placementDraft.selectUnit
        }
      />
    </Card>
  )
})
