// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useEffect, Fragment, useState } from 'react'
import { RouteComponentProps, useHistory } from 'react-router'
import { Link } from 'react-router-dom'
import styled from 'styled-components'
import ReactSelect from 'react-select'
import { faLink } from 'icon-set'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import LocalDate from '@evaka/lib-common/src/local-date'
import { Container, ContentArea } from '~components/shared/layout/Container'
import Title from '~components/shared/atoms/Title'
import { Gap } from '~components/shared/layout/white-space'
import Button from '~components/shared/atoms/buttons/Button'
import Loader from '~components/shared/atoms/Loader'

import {
  PlacementDraftState,
  PlacementDraftContext,
  UnitsState,
  UnitsContext,
  Unit
} from '~state/placementdraft'
import { useTranslation } from '~state/i18n'
import { UUID } from '~types'
import { Loading, isSuccess, isFailure, isLoading, Result, Success } from '~api'
import { getApplicationUnits } from '~api/daycare'
import { formatName } from '~/utils'
import {
  Period,
  PlacementDraft,
  PlacementDraftPlacement,
  DaycarePlacementPlan
} from '~types/placementdraft'
import UnitCards from './UnitCards'
import PlacementDraftRow from './PlacementDraftRow'
import Placements from './Placements'
import { areIntervalsOverlapping } from 'date-fns'
import { TitleContext, TitleState } from '~state/title'
import { getPlacementDraft, createPlacementPlan } from 'api/applications'

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
  font-weight: 600;
  display: inline-block;
`

const SelectContainer = styled.div`
  max-width: 400px;
`

const periodsOverlap = (period1: Period, period2: Period) =>
  areIntervalsOverlapping(
    {
      start: period1.start.toSystemTzDate(),
      end: period1.end.toSystemTzDate()
    },
    { start: period2.start.toSystemTzDate(), end: period2.end.toSystemTzDate() }
  )

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

  function hasOverlap(
    placement: PlacementDraftPlacement,
    placementDraft: Success<PlacementDraft>
  ) {
    const periodOverlap = periodsOverlap(
      { start: placement.startDate, end: placement.endDate },
      placementDraft.data.period
    )

    let preschoolDaycarePeriod = false
    if (placementDraft.data.preschoolDaycarePeriod) {
      preschoolDaycarePeriod = periodsOverlap(
        { start: placement.startDate, end: placement.endDate },
        placementDraft.data.preschoolDaycarePeriod
      )
    }

    return periodOverlap || preschoolDaycarePeriod
  }

  function calculateOverLaps(placementDraft: Success<PlacementDraft>) {
    if (isSuccess(placementDraft)) {
      const placements = placementDraft.data.placements.map(
        (placement: PlacementDraftPlacement) => {
          return {
            ...placement,
            overlap: hasOverlap(placement, placementDraft)
          }
        }
      )
      setPlacementDraft({
        ...placementDraft,
        data: {
          ...placementDraft.data,
          placements: placements
        }
      })
    }
  }

  function removeOldPlacements(placementDraft: Result<PlacementDraft>) {
    if (isSuccess(placementDraft)) {
      placementDraft = {
        ...placementDraft,
        data: {
          ...placementDraft.data,
          placements: placementDraft.data.placements.filter((p) => {
            return !p.endDate.isBefore(LocalDate.today())
          })
        }
      }
    }
  }

  useEffect(() => {
    setPlacementDraft(Loading())
    void getPlacementDraft(id).then((placementDraft) => {
      removeOldPlacements(placementDraft)
      setPlacementDraft(placementDraft)
      if (isSuccess(placementDraft)) {
        setPlacement({
          unitId: undefined,
          period: placementDraft.data.period,
          preschoolDaycarePeriod: placementDraft.data.preschoolDaycarePeriod
        })
        calculateOverLaps(placementDraft)

        void getApplicationUnits(
          placementDraft.data.type,
          placementDraft.data.period.start
        ).then(setUnits)
      }
    })
  }, [id])

  useEffect(() => {
    if (isSuccess(placementDraft)) {
      const name = formatTitleName(
        placementDraft.data.child.firstName,
        placementDraft.data.child.lastName
      )
      setTitle(`${name} | ${i18n.titles.placementDraft}`)
    }
  }, [placementDraft])

  function fixNullLengthPeriods(
    periodType: 'period' | 'preschoolDaycarePeriod',
    dateType: 'start' | 'end',
    date: LocalDate
  ) {
    if (isSuccess(placementDraft)) {
      const period = placementDraft.data[periodType]
      if (dateType === 'start') {
        if (period?.end && period.end.isBefore(date)) {
          const nextDay = date.addDays(1)
          period.start = date
          period.end = nextDay
        } else {
          if (period?.start) period.start = date
        }
      } else if (period?.start && date.isBefore(period.start)) {
        const previousDay = date.subDays(1)
        period.end = date
        period.start = previousDay
      } else {
        if (period?.end) period.end = date
      }
      return period
    }
    return null
  }

  const updatePlacementDate = (
    periodType: 'period' | 'preschoolDaycarePeriod',
    dateType: 'start' | 'end'
  ) => (date: LocalDate) => {
    const fixedPeriod = fixNullLengthPeriods(periodType, dateType, date)
    setPlacement({
      ...placement,
      [periodType]: fixedPeriod
    })
    if (isSuccess(placementDraft)) {
      const updatedPlacementDraft = {
        ...placementDraft,
        data: {
          ...placementDraft.data,
          [periodType]: fixedPeriod
        }
      }
      setPlacementDraft(updatedPlacementDraft)
      calculateOverLaps(updatedPlacementDraft)
    }
  }

  function redirectToMainPage() {
    history.push('/applications')
    return null
  }

  function addUnit(unitId: UUID) {
    return (
      isSuccess(units) &&
      isSuccess(placementDraft) &&
      setAdditionalUnits((prevUnits) => {
        if (
          placementDraft.data.preferredUnits
            .map((unit) => unit.id)
            .includes(unitId)
        ) {
          return prevUnits
        }
        const withOut = prevUnits.filter((unit) => unit.id !== unitId)
        const newUnit = units.data.find((unit) => unit.id === unitId)
        if (!newUnit) throw new Error(`Unit not found ${unitId}`)
        return [...withOut, newUnit]
      })
    )
  }

  return (
    <ContainerNarrow>
      <Gap size={'L'} />
      <ContentArea opaque>
        {isFailure(placementDraft) && <p>{i18n.common.loadingFailed}</p>}
        {isLoading(placementDraft) && <Loader />}
        {isSuccess(placementDraft) && placement.period && (
          <Fragment>
            <PlacementDraftInfo>
              <Title size={2}>{i18n.placementDraft.createPlacementDraft}</Title>
              <ChildName>
                <Title size={3}>
                  {formatName(
                    placementDraft.data.child.firstName,
                    placementDraft.data.child.lastName,
                    i18n
                  )}
                </Title>
                <Link to={`/child-information/${placementDraft.data.child.id}`}>
                  {i18n.childInformation.title}
                  <FontAwesomeIcon icon={faLink} />
                </Link>
              </ChildName>
              <ChildDateOfBirth>
                <DOBTitle>{i18n.placementDraft.dateOfBirth}</DOBTitle>
                {placementDraft.data.child.dob.format()}
              </ChildDateOfBirth>
            </PlacementDraftInfo>
            <PlacementDraftRow
              placementDraft={placementDraft.data}
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
            {placementDraft.data.placements && <Placements />}
            <SelectContainer>
              <ReactSelect
                placeholder={i18n.filters.unitPlaceholder}
                value={null}
                options={
                  isSuccess(units)
                    ? units.data
                        .map(({ id, name }) => ({ label: name, value: id }))
                        .sort((a, b) => (a.label < b.label ? -1 : 1))
                    : []
                }
                onChange={(option) =>
                  option && 'value' in option
                    ? addUnit(option.value)
                    : undefined
                }
                isLoading={isLoading(units)}
                loadingMessage={() => i18n.common.loading}
                noOptionsMessage={() => i18n.common.loadingFailed}
              />
            </SelectContainer>
            <UnitCards
              additionalUnits={additionalUnits}
              setAdditionalUnits={setAdditionalUnits}
              placement={placement}
              setPlacement={setPlacement}
              placementDraft={placementDraft.data}
            />
            <SendButtonContainer>
              <Button
                primary
                disabled={placement.unitId === undefined}
                dataQa="send-placement-button"
                onClick={() => {
                  void createPlacementPlan(id, placement).then(() =>
                    redirectToMainPage()
                  )
                }}
                text={i18n.common.send}
              />
            </SendButtonContainer>
          </Fragment>
        )}
      </ContentArea>
    </ContainerNarrow>
  )
}

export default PlacementDraft
