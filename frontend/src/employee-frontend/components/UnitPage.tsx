// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, {
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState
} from 'react'
import { Redirect, Route, Switch, useParams } from 'react-router-dom'
import { useTranslation } from '../state/i18n'
import { RouteWithTitle } from '../components/RouteWithTitle'
import { Gap } from 'lib-components/white-space'
import Tabs from 'lib-components/molecules/Tabs'
import { UUID } from '../types'
import TabUnitInformation from '../components/unit/TabUnitInformation'
import TabGroups from '../components/unit/TabGroups'
import { TitleContext, TitleState } from '../state/title'
import { getDaycare, getUnitData } from '../api/unit'
import { useRestApi } from 'lib-common/utils/useRestApi'
import Container from 'lib-components/layout/Container'
import { UnitContext } from '../state/unit'
import TabPlacementProposals from '../components/unit/TabPlacementProposals'
import TabWaitingConfirmation from '../components/unit/TabWaitingConfirmation'
import TabApplications from '../components/unit/TabApplications'
import { requireRole } from '../utils/roles'
import { UserContext } from '../state/user'
import { useQuery } from '../utils/useQuery'
import LocalDate from 'lib-common/local-date'

export default React.memo(function UnitPage() {
  const { id } = useParams<{ id: UUID }>()
  const { i18n } = useTranslation()
  const { roles } = useContext(UserContext)
  const { setTitle } = useContext<TitleState>(TitleContext)
  const {
    unitInformation,
    setUnitInformation,
    unitData,
    setUnitData,
    filters,
    setFilters,
    savePosition,
    scrollToPosition
  } = useContext(UnitContext)

  const loadUnitInformation = useRestApi(getDaycare, setUnitInformation)

  const query = useQuery()
  useEffect(() => {
    if (query.has('start')) {
      const queryStart = LocalDate.parseIso(query.get('start') ?? '')
      setFilters(filters.withStartDate(queryStart))
    }
  }, []) // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => loadUnitInformation(id), [id, loadUnitInformation])
  useEffect(() => {
    if (unitInformation.isSuccess) {
      setTitle(unitInformation.value.daycare.name)
    }
  }, [setTitle, unitInformation])

  const loadUnitData = useRestApi(getUnitData, setUnitData)
  const loadUnitDataWithFixedPosition = () => {
    savePosition()
    loadUnitData(id, filters.startDate, filters.endDate)
  }

  useEffect(() => {
    loadUnitDataWithFixedPosition()
  }, [filters]) // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    if (unitData.isSuccess) {
      scrollToPosition()
    }
  }, [scrollToPosition, unitData])

  const [openGroups, setOpenGroups] = useState<Record<string, boolean>>({})

  const tabs = useMemo(
    () => [
      {
        id: 'unit-info',
        link: `/units/${id}/unit-info`,
        label: i18n.unit.tabs.unitInfo
      },
      {
        id: 'groups',
        link: `/units/${id}/groups`,
        label: i18n.unit.tabs.groups,
        counter: unitData
          .map((data) => data.missingGroupPlacements.length)
          .getOrElse(undefined)
      },
      ...(requireRole(
        roles,
        'ADMIN',
        'SERVICE_WORKER',
        'FINANCE_ADMIN',
        'UNIT_SUPERVISOR'
      )
        ? [
            {
              id: 'waiting-confirmation',
              link: `/units/${id}/waiting-confirmation`,
              label: i18n.unit.tabs.waitingConfirmation
            },
            {
              id: 'placement-proposals',
              link: `/units/${id}/placement-proposals`,
              label: i18n.unit.tabs.placementProposals,
              counter: unitData
                .map((data) => data.placementProposals?.length)
                .getOrElse(undefined)
            },
            {
              id: 'applications',
              link: `/units/${id}/applications`,
              label: i18n.unit.tabs.applications
            }
          ]
        : [])
    ],
    [i18n, unitData] // eslint-disable-line react-hooks/exhaustive-deps
  )

  const RedirectToUnitInfo = useCallback(
    () => <Redirect to={`/units/${id}/unit-info`} />,
    [id]
  )

  return (
    <>
      <Gap size="s" />
      <Tabs tabs={tabs} />
      <Gap size="s" />
      <Container>
        <Switch>
          <RouteWithTitle
            exact
            path="/units/:id/unit-info"
            component={TabUnitInformation}
          />
          <RouteWithTitle
            exact
            path="/units/:id/groups"
            component={() => (
              <TabGroups
                reloadUnitData={loadUnitDataWithFixedPosition}
                openGroups={openGroups}
                setOpenGroups={setOpenGroups}
              />
            )}
          />
          <RouteWithTitle
            exact
            path="/units/:id/waiting-confirmation"
            component={TabWaitingConfirmation}
          />
          <RouteWithTitle
            exact
            path="/units/:id/placement-proposals"
            component={() => (
              <TabPlacementProposals
                reloadUnitData={loadUnitDataWithFixedPosition}
              />
            )}
          />
          <RouteWithTitle
            exact
            path="/units/:id/applications"
            component={TabApplications}
          />
          <Route path="/" component={RedirectToUnitInfo} />
        </Switch>
      </Container>
    </>
  )
})
