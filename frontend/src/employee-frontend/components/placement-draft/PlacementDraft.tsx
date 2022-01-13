// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, {
  Dispatch,
  SetStateAction,
  useCallback,
  useContext,
  useEffect,
  useState
} from 'react'
import { RouteComponentProps, useHistory } from 'react-router'
import styled from 'styled-components'
import { isLoading, Loading, Result, Success } from 'lib-common/api'
import FiniteDateRange from 'lib-common/finite-date-range'
import { PublicUnit } from 'lib-common/generated/api-types/daycare'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'
import { InternalLink } from 'lib-components/atoms/InternalLink'
import AsyncButton from 'lib-components/atoms/buttons/AsyncButton'
import Combobox from 'lib-components/atoms/dropdowns/Combobox'
import { Container, ContentArea } from 'lib-components/layout/Container'
import ListGrid from 'lib-components/layout/ListGrid'
import { H1, H2, Label } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { createPlacementPlan, getPlacementDraft } from '../../api/applications'
import { getApplicationUnits } from '../../api/daycare'
import Tooltip from '../../components/common/Tooltip'
import WarningLabel from '../../components/common/WarningLabel'
import { useTranslation } from '../../state/i18n'
import { TitleContext, TitleState } from '../../state/title'
import {
  DaycarePlacementPlan,
  PlacementDraft,
  PlacementDraftPlacement
} from '../../types/placementdraft'
import { formatName } from '../../utils'
import { renderResult } from '../async-rendering'
import PlacementDraftRow from './PlacementDraftRow'
import Placements from './Placements'
import UnitCards from './UnitCards'

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

function calculateOverLaps(
  placementDraft: Result<PlacementDraft>,
  setPlacementDraft: Dispatch<SetStateAction<Result<PlacementDraft>>>
) {
  if (placementDraft.isSuccess) {
    const placements = placementDraft.value.placements.map(
      (placement: PlacementDraftPlacement) => {
        return {
          ...placement,
          overlap: hasOverlap(placement, placementDraft)
        }
      }
    )
    setPlacementDraft(placementDraft.map((draft) => ({ ...draft, placements })))
  }
}

function hasOverlap(
  placement: PlacementDraftPlacement,
  { value: { period, preschoolDaycarePeriod } }: Success<PlacementDraft>
) {
  const placementDateRange = new FiniteDateRange(
    placement.startDate,
    placement.endDate
  )
  return (
    period.overlaps(placementDateRange) ||
    (preschoolDaycarePeriod?.overlaps(placementDateRange) ?? false)
  )
}

export default React.memo(function PlacementDraft({
  match
}: RouteComponentProps<{ id: UUID }>) {
  const { id: applicationId } = match.params
  const { i18n } = useTranslation()
  const history = useHistory()
  const [placementDraft, setPlacementDraft] = useState<Result<PlacementDraft>>(
    Loading.of()
  )
  const [units, setUnits] = useState<Result<PublicUnit[]>>(Loading.of())

  const [placement, setPlacement] = useState<DaycarePlacementPlan>({
    unitId: '',
    period: undefined
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
    placementDraft: Result<PlacementDraft>
  ): Result<PlacementDraft> {
    return placementDraft.map((draft) => ({
      ...draft,
      placements: draft.placements.filter(
        (p) => !p.endDate.isBefore(LocalDate.today())
      )
    }))
  }

  const redirectToMainPage = useCallback(
    () => history.push('/applications'),
    [history]
  )

  useEffect(() => {
    setPlacementDraft(Loading.of())
    void getPlacementDraft(applicationId).then((placementDraft) => {
      const withoutOldPlacements = removeOldPlacements(placementDraft)
      setPlacementDraft(withoutOldPlacements)
      if (withoutOldPlacements.isSuccess) {
        setPlacement({
          unitId: undefined,
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
    })
  }, [applicationId, redirectToMainPage])

  useEffect(() => {
    if (placementDraft.isSuccess) {
      void getApplicationUnits(
        placementDraft.value.type,
        placement.period?.start ?? placementDraft.value.period.start
      ).then(setUnits)
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
                      tooltipId="tooltip_warning"
                      tooltipText={i18n.placementDraft.restrictedDetailsTooltip}
                      place="top"
                      delayShow={750}
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
                  text={i18n.childInformation.title}
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
                />
              </SelectContainer>
              <SendButtonContainer>
                <AsyncButton
                  primary
                  disabled={
                    placement.unitId === undefined || selectedUnitIsGhostUnit
                  }
                  data-qa="send-placement-button"
                  onClick={() => createPlacementPlan(applicationId, placement)}
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
