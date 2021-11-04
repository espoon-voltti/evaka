// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useEffect, Fragment, useState } from 'react'
import { RouteComponentProps, useHistory } from 'react-router'
import { Link } from 'react-router-dom'
import styled from 'styled-components'
import { faLink } from 'lib-icons'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import LocalDate from 'lib-common/local-date'
import { Container, ContentArea } from 'lib-components/layout/Container'
import Title from 'lib-components/atoms/Title'
import { Gap } from 'lib-components/white-space'
import Loader from 'lib-components/atoms/Loader'
import Combobox from 'lib-components/atoms/dropdowns/Combobox'
import { fontWeights } from 'lib-components/typography'

import {
  PlacementDraftState,
  PlacementDraftContext,
  UnitsState,
  UnitsContext,
  Unit
} from '../../state/placementdraft'
import { useTranslation } from '../../state/i18n'
import { Loading, Result, Success } from 'lib-common/api'
import { getApplicationUnits } from '../../api/daycare'
import { formatName } from '../../utils'
import {
  PlacementDraft,
  PlacementDraftPlacement,
  DaycarePlacementPlan
} from '../../types/placementdraft'
import UnitCards from './UnitCards'
import PlacementDraftRow from './PlacementDraftRow'
import Placements from './Placements'
import { TitleContext, TitleState } from '../../state/title'
import { getPlacementDraft, createPlacementPlan } from '../../api/applications'
import FiniteDateRange from 'lib-common/finite-date-range'
import WarningLabel from '../../components/common/WarningLabel'
import Tooltip from '../../components/common/Tooltip'
import AsyncButton from 'lib-components/atoms/buttons/AsyncButton'
import { UUID } from 'lib-common/types'

const ContainerNarrow = styled(Container)`
  max-width: 990px;
`

const SendButtonContainer = styled.div`
  display: flex;
  justify-content: flex-end;
`

const PlacementDraftInfo = styled.section`
  margin-bottom: 60px;

  h1.title.is-2 {
    margin-bottom: 55px;
  }
`

const ChildName = styled.div`
  display: flex;
  justify-content: space-between;

  a {
    margin-left: 50px;
    min-width: 120px;
    display: flex;
    align-items: center;
  }

  h2.title.is-3 {
    width: 650px;
  }

  svg {
    margin-left: 5px;
  }
`

const ChildDateOfBirth = styled.div``

const DOBTitle = styled.span`
  width: 225px;
  font-weight: ${fontWeights.semibold};
  display: inline-block;
`

const SelectContainer = styled.div`
  max-width: 400px;
`

const FloatRight = styled.div`
  float: right;
`

function PlacementDraft({ match }: RouteComponentProps<{ id: UUID }>) {
  const { id } = match.params
  const { i18n } = useTranslation()
  const history = useHistory()
  const { placementDraft, setPlacementDraft } = useContext<PlacementDraftState>(
    PlacementDraftContext
  )

  const { units, setUnits } = useContext<UnitsState>(UnitsContext)

  const [placement, setPlacement] = useState<DaycarePlacementPlan>({
    unitId: '',
    period: undefined
  })

  const { setTitle, formatTitleName } = useContext<TitleState>(TitleContext)

  const [additionalUnits, setAdditionalUnits] = useState<Unit[]>([])
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
  }, [placement]) // eslint-disable-line react-hooks/exhaustive-deps

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

  function calculateOverLaps(placementDraft: Result<PlacementDraft>) {
    if (placementDraft.isSuccess) {
      const placements = placementDraft.value.placements.map(
        (placement: PlacementDraftPlacement) => {
          return {
            ...placement,
            overlap: hasOverlap(placement, placementDraft)
          }
        }
      )
      setPlacementDraft(
        placementDraft.map((draft) => ({ ...draft, placements }))
      )
    }
  }

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

  function redirectToMainPage() {
    history.push('/applications')
    return null
  }

  useEffect(() => {
    setPlacementDraft(Loading.of())
    void getPlacementDraft(id).then((placementDraft) => {
      const withoutOldPlacements = removeOldPlacements(placementDraft)
      setPlacementDraft(withoutOldPlacements)
      if (withoutOldPlacements.isSuccess) {
        setPlacement({
          unitId: undefined,
          period: withoutOldPlacements.value.period,
          preschoolDaycarePeriod:
            withoutOldPlacements.value.preschoolDaycarePeriod
        })
        calculateOverLaps(withoutOldPlacements)
      }

      // Application has already changed its status
      if (placementDraft.isFailure && placementDraft.statusCode === 409) {
        redirectToMainPage()
      }
    })
  }, [id]) // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    if (placementDraft.isSuccess) {
      void getApplicationUnits(
        placementDraft.value.type,
        placement.period?.start ?? placementDraft.value.period.start
      ).then(setUnits)
    }
  }, [placementDraft, placement.period?.start]) // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    if (placementDraft.isSuccess) {
      const name = formatTitleName(
        placementDraft.value.child.firstName,
        placementDraft.value.child.lastName
      )
      setTitle(`${name} | ${i18n.titles.placementDraft}`)
    }
  }, [placementDraft]) // eslint-disable-line react-hooks/exhaustive-deps

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
        calculateOverLaps(updatedPlacementDraft)
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
    <ContainerNarrow>
      <Gap size={'L'} />
      <ContentArea opaque>
        {placementDraft.isFailure && <p>{i18n.common.loadingFailed}</p>}
        {placementDraft.isLoading && <Loader />}
        {placementDraft.isSuccess && placement.period && (
          <Fragment>
            <PlacementDraftInfo>
              {placementDraft.value.guardianHasRestrictedDetails && (
                <FloatRight>
                  <Tooltip
                    tooltipId={`tooltip_warning`}
                    tooltipText={i18n.placementDraft.restrictedDetailsTooltip}
                    place={'top'}
                    delayShow={750}
                  >
                    <WarningLabel
                      text={i18n.placementDraft.restrictedDetails}
                      data-qa={`restricted-details-warning`}
                    />
                  </Tooltip>
                </FloatRight>
              )}
              <Title size={1}>{i18n.placementDraft.createPlacementDraft}</Title>
              <ChildName>
                <Title size={3}>
                  {formatName(
                    placementDraft.value.child.firstName,
                    placementDraft.value.child.lastName,
                    i18n
                  )}
                </Title>
                <Link
                  to={`/child-information/${placementDraft.value.child.id}`}
                >
                  {i18n.childInformation.title}
                  <FontAwesomeIcon icon={faLink} />
                </Link>
              </ChildName>
              <ChildDateOfBirth>
                <DOBTitle>{i18n.placementDraft.dateOfBirth}</DOBTitle>
                {placementDraft.value.child.dob.format()}
              </ChildDateOfBirth>
            </PlacementDraftInfo>
            <PlacementDraftRow
              placementDraft={placementDraft.value}
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
            {placementDraft.value.placements && <Placements />}
            <SelectContainer>
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
              />
            </SelectContainer>
            <UnitCards
              additionalUnits={additionalUnits}
              setAdditionalUnits={setAdditionalUnits}
              placement={placement}
              setPlacement={setPlacement}
              placementDraft={placementDraft.value}
              selectedUnitIsGhostUnit={selectedUnitIsGhostUnit}
            />
            <SendButtonContainer>
              <AsyncButton
                primary
                disabled={
                  placement.unitId === undefined || selectedUnitIsGhostUnit
                }
                data-qa="send-placement-button"
                onClick={() => createPlacementPlan(id, placement)}
                onSuccess={redirectToMainPage}
                text={i18n.placementDraft.createPlacementDraft}
              />
            </SendButtonContainer>
          </Fragment>
        )}
      </ContentArea>
    </ContainerNarrow>
  )
}

export default PlacementDraft
