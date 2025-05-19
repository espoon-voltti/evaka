// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import type { Dispatch, SetStateAction } from 'react'
import React, {
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState
} from 'react'
import { Link, useNavigate } from 'react-router'
import styled from 'styled-components'

import type { Result, Success } from 'lib-common/api'
import { isLoading, Loading, wrapResult } from 'lib-common/api'
import FiniteDateRange from 'lib-common/finite-date-range'
import type { DaycarePlacementPlan } from 'lib-common/generated/api-types/application'
import type { PublicUnit } from 'lib-common/generated/api-types/daycare'
import type {
  PlacementPlanDraft,
  PlacementSummary
} from 'lib-common/generated/api-types/placement'
import type {
  ApplicationId,
  DaycareId
} from 'lib-common/generated/api-types/shared'
import LocalDate from 'lib-common/local-date'
import { useIdRouteParam } from 'lib-common/useRouteParams'
import Tooltip from 'lib-components/atoms/Tooltip'
import { AsyncButton } from 'lib-components/atoms/buttons/AsyncButton'
import Combobox from 'lib-components/atoms/dropdowns/Combobox'
import { Container, ContentArea } from 'lib-components/layout/Container'
import ListGrid from 'lib-components/layout/ListGrid'
import { Bold, H1, H2, Label } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { faLink } from 'lib-icons'

import WarningLabel from '../../components/common/WarningLabel'
import {
  createPlacementPlan,
  getPlacementPlanDraft
} from '../../generated/api-clients/application'
import { getApplicationUnits } from '../../generated/api-clients/daycare'
import { useTranslation } from '../../state/i18n'
import type { TitleState } from '../../state/title'
import { TitleContext } from '../../state/title'
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

export type DaycarePlacementPlanForm =
  | {
      unitId: DaycareId | null
      period: FiniteDateRange | null
      hasPreschoolDaycarePeriod: false
    }
  | {
      unitId: DaycareId | null
      period: FiniteDateRange | null
      hasPreschoolDaycarePeriod: true
      preschoolDaycarePeriod: FiniteDateRange | null
    }

export default React.memo(function PlacementDraft() {
  const applicationId = useIdRouteParam<ApplicationId>('id')
  const { i18n } = useTranslation()
  const navigate = useNavigate()
  const [placementDraft, setPlacementDraft] = useState<
    Result<PlacementPlanDraftWithOverlaps>
  >(Loading.of())
  const [units, setUnits] = useState<Result<PublicUnit[]>>(Loading.of())

  const [formState, setFormState] = useState<DaycarePlacementPlanForm>({
    unitId: null,
    period: null,
    hasPreschoolDaycarePeriod: false
  })

  const { setTitle, formatTitleName } = useContext<TitleState>(TitleContext)

  const [additionalUnits, setAdditionalUnits] = useState<PublicUnit[]>([])
  const [selectedUnitIsGhostUnit, setSelectedUnitIsGhostUnit] =
    useState<boolean>(false)

  useEffect(() => {
    if (units.isSuccess && formState.unitId) {
      setSelectedUnitIsGhostUnit(
        units.value
          .filter((unit) => unit.id === formState.unitId)
          .map((unit) => unit.ghostUnit)
          .includes(true)
      )
    }
  }, [formState, units])

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
    () => void navigate('/applications'),
    [navigate]
  )

  useEffect(() => {
    setPlacementDraft(Loading.of())
    void getPlacementPlanDraftResult({ applicationId }).then(
      (placementDraft) => {
        const withoutOldPlacements = removeOldPlacements(placementDraft)
        setPlacementDraft(withoutOldPlacements)
        if (withoutOldPlacements.isSuccess) {
          if (withoutOldPlacements.value.preschoolDaycarePeriod !== null) {
            setFormState({
              unitId: null,
              period: withoutOldPlacements.value.period,
              hasPreschoolDaycarePeriod: true,
              preschoolDaycarePeriod:
                withoutOldPlacements.value.preschoolDaycarePeriod
            })
          } else {
            setFormState({
              unitId: null,
              period: withoutOldPlacements.value.period,
              hasPreschoolDaycarePeriod: false
            })
          }
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
        date: formState.period?.start ?? placementDraft.value.period.start,
        shiftCare: null
      }).then(setUnits)
    }
  }, [placementDraft, formState.period?.start])

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
    date: LocalDate | null
  ) {
    if (placementDraft.isSuccess) {
      if (!date) return null
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
    (date: LocalDate | null) => {
      const fixedPeriod = fixNullLengthPeriods(periodType, dateType, date)
      if (
        periodType === 'preschoolDaycarePeriod' &&
        !formState.hasPreschoolDaycarePeriod
      ) {
        throw new Error('BUG: preschoolDaycarePeriod should not be set')
      }
      setFormState({
        ...formState,
        [periodType]: fixedPeriod
      })
      if (fixedPeriod !== null && placementDraft.isSuccess) {
        const updatedPlacementDraft = placementDraft.map((draft) => ({
          ...draft,
          [periodType]: fixedPeriod
        }))
        setPlacementDraft(updatedPlacementDraft)
        calculateOverLaps(updatedPlacementDraft, setPlacementDraft)
      }
    }

  function addUnit(unitId: DaycareId) {
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
    const { unitId, period, hasPreschoolDaycarePeriod } = formState
    if (!unitId || !period || selectedUnitIsGhostUnit) {
      return null
    } else {
      return {
        unitId,
        period,
        preschoolDaycarePeriod: hasPreschoolDaycarePeriod
          ? formState.preschoolDaycarePeriod
          : null
      }
    }
  }, [formState, selectedUnitIsGhostUnit])

  return (
    <Container
      data-qa="placement-draft-page"
      data-isloading={isLoading(placementDraft)}
    >
      <ContentArea opaque>
        <Gap size="xs" />
        {renderResult(placementDraft, (placementDraft) => (
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
              <Link
                to={`/child-information/${placementDraft.child.id}`}
                target="_blank"
              >
                <Bold>
                  {i18n.titles.childInformation}{' '}
                  <FontAwesomeIcon icon={faLink} />
                </Bold>
              </Link>
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
              formState={formState}
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
              formState={formState}
              setFormState={setFormState}
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
                onChange={(option) => (option ? addUnit(option.id) : undefined)}
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
        ))}
      </ContentArea>
    </Container>
  )
})
