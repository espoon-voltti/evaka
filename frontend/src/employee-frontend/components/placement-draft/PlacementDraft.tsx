// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, {
  Dispatch,
  SetStateAction,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState
} from 'react'
import { useNavigate } from 'react-router-dom'
import styled from 'styled-components'

import { isLoading, Loading, Result, Success, wrapResult } from 'lib-common/api'
import FiniteDateRange from 'lib-common/finite-date-range'
import { DaycarePlacementPlan } from 'lib-common/generated/api-types/application'
import { PublicUnit } from 'lib-common/generated/api-types/daycare'
import {
  PlacementPlanDraft,
  PlacementSummary
} from 'lib-common/generated/api-types/placement'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'
import useRouteParams from 'lib-common/useRouteParams'
import { InternalLink } from 'lib-components/atoms/InternalLink'
import Tooltip from 'lib-components/atoms/Tooltip'
import { AsyncButton } from 'lib-components/atoms/buttons/AsyncButton'
import Combobox from 'lib-components/atoms/dropdowns/Combobox'
import { Container, ContentArea } from 'lib-components/layout/Container'
import ListGrid from 'lib-components/layout/ListGrid'
import { H1, H2, Label } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'

import WarningLabel from '../../components/common/WarningLabel'
import {
  createPlacementPlan,
  getPlacementPlanDraft
} from '../../generated/api-clients/application'
import { getApplicationUnits } from '../../generated/api-clients/daycare'
import { useTranslation } from '../../state/i18n'
import { TitleContext, TitleState } from '../../state/title'
import { asUnitType } from '../../types/daycare'
import { formatName } from '../../utils'
import { renderResult } from '../async-rendering'

import PlacementDraftRow from './PlacementDraftRow'
import Placements from './Placements'
import UnitCards from './UnitCards'

const getPlacementPlanDraftResult = wrapResult(getPlacementPlanDraft)
const createPlacementPlanResult = wrapResult(createPlacementPlan)
const getApplicationUnitsResult = wrapResult(getApplicationUnits)

const SendButtonContainer = styled.div`
  display: flex;
  justify-content: flex-end;
`

const SelectContainer = styled.div`
  max-width: 400px;
`

const FloatRight = styled.div`
  float: right;
`

export interface PlacementSummaryWithOverlaps extends PlacementSummary {
  overlap?: boolean
}

interface PlacementPlanDraftWithOverlaps extends PlacementPlanDraft {
  placements: PlacementSummaryWithOverlaps[]
}

function calculateOverLaps(
  placementDraft: Result<PlacementPlanDraft>,
  setPlacementDraft: Dispatch<
    SetStateAction<Result<PlacementPlanDraftWithOverlaps>>
  >
) {
  if (placementDraft.isSuccess) {
    const placements = placementDraft.value.placements.map(
      (placement: PlacementSummary) => ({
        ...placement,
        overlap: hasOverlap(placement, placementDraft)
      })
    )
    setPlacementDraft(placementDraft.map((draft) => ({ ...draft, placements })))
  }
}

function hasOverlap(
  placement: PlacementSummary,
  { value: { period, preschoolDaycarePeriod } }: Success<PlacementPlanDraft>
): boolean {
  const placementDateRange = new FiniteDateRange(
    placement.startDate,
    placement.endDate
  )
  return (
    period.overlaps(placementDateRange) ||
    (preschoolDaycarePeriod?.overlaps(placementDateRange) ?? false)
  )
}

export interface DaycarePlacementPlanForm {
  unitId: UUID | null
  period: FiniteDateRange | null
  preschoolDaycarePeriod: FiniteDateRange | null
}

export default React.memo(function PlacementDraft() {
  const { id: applicationId } = useRouteParams(['id'])
  const { i18n } = useTranslation()
  const navigate = useNavigate()
  const [placementDraft, setPlacementDraft] = useState<
    Result<PlacementPlanDraftWithOverlaps>
  >(Loading.of())
  const [units, setUnits] = useState<Result<PublicUnit[]>>(Loading.of())

  const [placement, setPlacement] = useState<DaycarePlacementPlanForm>({
    unitId: null,
    period: null,
    preschoolDaycarePeriod: null
  })

  const { setTitle, formatTitleName } = useContext<TitleState>(TitleContext)

  const [additionalUnits, setAdditionalUnits] = useState<PublicUnit[]>([])
  const [selectedUnitIsGhostUnit, setSelectedUnitIsGhostUnit] =
    useState<boolean>(false)

  useEffect(() => {
    units.isSuccess &&
      placement.unitId &&
      setSelectedUnitIsGhostUnit(
        units.value
          .filter((unit) => unit.id === placement.unitId)
          .map((unit) => unit.ghostUnit)
          .includes(true)
      )
  }, [placement, units])

  function removeOldPlacements(
    placementDraft: Result<PlacementPlanDraft>
  ): Result<PlacementPlanDraft> {
    return placementDraft.map((draft) => ({
      ...draft,
      placements: draft.placements.filter(
        (p) => !p.endDate.isBefore(LocalDate.todayInSystemTz())
      )
    }))
  }

  const redirectToMainPage = useCallback(
    () => navigate('/applications'),
    [navigate]
  )

  useEffect(() => {
    setPlacementDraft(Loading.of())
    void getPlacementPlanDraftResult({ applicationId }).then(
      (placementDraft) => {
        const withoutOldPlacements = removeOldPlacements(placementDraft)
        setPlacementDraft(withoutOldPlacements)
        if (withoutOldPlacements.isSuccess) {
          setPlacement({
            unitId: null,
            period: withoutOldPlacements.value.period,
            preschoolDaycarePeriod:
              withoutOldPlacements.value.preschoolDaycarePeriod
          })
          calculateOverLaps(withoutOldPlacements, setPlacementDraft)
        }

        // Application has already changed its status
        if (placementDraft.isFailure && placementDraft.statusCode === 409) {
          redirectToMainPage()
        }
      }
    )
  }, [applicationId, redirectToMainPage])

  useEffect(() => {
    if (placementDraft.isSuccess) {
      void getApplicationUnitsResult({
        type: asUnitType(placementDraft.value.type),
        date: placement.period?.start ?? placementDraft.value.period.start,
        shiftCare: null
      }).then(setUnits)
    }
  }, [placementDraft, placement.period?.start])

  useEffect(() => {
    if (placementDraft.isSuccess) {
      const name = formatTitleName(
        placementDraft.value.child.firstName,
        placementDraft.value.child.lastName
      )
      setTitle(`${name} | ${i18n.titles.placementDraft}`)
    }
  }, [formatTitleName, i18n.titles.placementDraft, placementDraft, setTitle])

  function fixNullLengthPeriods(
    periodType: 'period' | 'preschoolDaycarePeriod',
    dateType: 'start' | 'end',
    date: LocalDate
  ) {
    if (placementDraft.isSuccess) {
      const period = placementDraft.value[periodType]
      if (!period) return period
      if (dateType === 'start') {
        if (period.end && period.end.isBefore(date)) {
          const nextDay = date.addDays(1)
          return new FiniteDateRange(date, nextDay)
        } else {
          return new FiniteDateRange(date, period.end)
        }
      } else if (period.start && date.isBefore(period.start)) {
        const previousDay = date.subDays(1)
        return new FiniteDateRange(previousDay, date)
      } else {
        return new FiniteDateRange(period.start, date)
      }
    }
    return null
  }

  const updatePlacementDate =
    (
      periodType: 'period' | 'preschoolDaycarePeriod',
      dateType: 'start' | 'end'
    ) =>
    (date: LocalDate) => {
      const fixedPeriod = fixNullLengthPeriods(periodType, dateType, date)
      setPlacement({
        ...placement,
        [periodType]: fixedPeriod
      })
      if (placementDraft.isSuccess) {
        const updatedPlacementDraft = placementDraft.map((draft) => ({
          ...draft,
          [periodType]: fixedPeriod
        }))
        setPlacementDraft(updatedPlacementDraft)
        calculateOverLaps(updatedPlacementDraft, setPlacementDraft)
      }
    }

  function addUnit(unitId: UUID) {
    return (
      units.isSuccess &&
      placementDraft.isSuccess &&
      setAdditionalUnits((prevUnits) => {
        if (
          placementDraft.value.preferredUnits
            .map((unit) => unit.id)
            .includes(unitId)
        ) {
          return prevUnits
        }
        const withOut = prevUnits.filter((unit) => unit.id !== unitId)
        const newUnit = units.value.find((unit) => unit.id === unitId)
        if (!newUnit) throw new Error(`Unit not found ${unitId}`)
        return [...withOut, newUnit]
      })
    )
  }

  const validPlan: DaycarePlacementPlan | null = useMemo(() => {
    const { unitId, period, preschoolDaycarePeriod } = placement
    if (unitId && period && !selectedUnitIsGhostUnit) {
      return { unitId, period, preschoolDaycarePeriod }
    } else {
      return null
    }
  }, [placement, selectedUnitIsGhostUnit])

  return (
    <Container
      data-qa="placement-draft-page"
      data-isloading={isLoading(placementDraft)}
    >
      <ContentArea opaque>
        <Gap size="xs" />
        {renderResult(placementDraft, (placementDraft) =>
          placement.period ? (
            <>
              <section>
                {placementDraft.guardianHasRestrictedDetails && (
                  <FloatRight>
                    <Tooltip
                      tooltip={i18n.placementDraft.restrictedDetailsTooltip}
                      position="top"
                    >
                      <WarningLabel
                        text={i18n.placementDraft.restrictedDetails}
                        data-qa="restricted-details-warning"
                      />
                    </Tooltip>
                  </FloatRight>
                )}
                <H1 noMargin>{i18n.placementDraft.createPlacementDraft}</H1>
                <Gap size="xs" />
                <H2 noMargin>
                  {formatName(
                    placementDraft.child.firstName,
                    placementDraft.child.lastName,
                    i18n
                  )}
                </H2>
                <Gap size="L" />
                <ListGrid>
                  <Label>{i18n.placementDraft.dateOfBirth}</Label>
                  <span>{placementDraft.child.dob.format()}</span>
                </ListGrid>
                <Gap size="s" />
                <InternalLink
                  to={`/child-information/${placementDraft.child.id}`}
                  text={i18n.titles.childInformation}
                  newTab
                />
              </section>
              {placementDraft.placements && (
                <>
                  <Gap size="XL" />
                  <Placements placements={placementDraft.placements} />
                </>
              )}
              <Gap size="XL" />
              <PlacementDraftRow
                placementDraft={placementDraft}
                placement={placement}
                updateStart={updatePlacementDate('period', 'start')}
                updateEnd={updatePlacementDate('period', 'end')}
                updatePreschoolStart={updatePlacementDate(
                  'preschoolDaycarePeriod',
                  'start'
                )}
                updatePreschoolEnd={updatePlacementDate(
                  'preschoolDaycarePeriod',
                  'end'
                )}
              />
              <Gap size="L" />
              <UnitCards
                additionalUnits={additionalUnits}
                setAdditionalUnits={setAdditionalUnits}
                applicationId={applicationId}
                placement={placement}
                setPlacement={setPlacement}
                placementDraft={placementDraft}
                selectedUnitIsGhostUnit={selectedUnitIsGhostUnit}
              />
              <Gap size="XL" />
              <SelectContainer>
                <Label>{i18n.placementDraft.addOtherUnit}</Label>
                <Combobox
                  placeholder={i18n.filters.unitPlaceholder}
                  selectedItem={null}
                  items={units
                    .map((us) =>
                      us
                        .map(({ id, name }) => ({ name, id }))
                        .sort((a, b) => (a.name < b.name ? -1 : 1))
                    )
                    .getOrElse([])}
                  getItemLabel={({ name }) => name}
                  onChange={(option) =>
                    option ? addUnit(option.id) : undefined
                  }
                  isLoading={units.isLoading}
                  menuEmptyLabel={i18n.common.noResults}
                  data-qa="add-other-unit"
                />
              </SelectContainer>
              <SendButtonContainer>
                <AsyncButton
                  primary
                  disabled={validPlan === null}
                  data-qa="send-placement-button"
                  onClick={() =>
                    createPlacementPlanResult({
                      applicationId,
                      body: validPlan!
                    })
                  }
                  onSuccess={redirectToMainPage}
                  text={i18n.placementDraft.createPlacementDraft}
                />
              </SendButtonContainer>
            </>
          ) : null
        )}
      </ContentArea>
    </Container>
  )
})
