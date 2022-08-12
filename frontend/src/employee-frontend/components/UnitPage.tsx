// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, {
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState
} from 'react'
import {
  Navigate,
  Route,
  Routes,
  useNavigate,
  useSearchParams
} from 'react-router-dom'
import styled from 'styled-components'

import { UnitResponse } from 'employee-frontend/api/unit'
import { isLoading } from 'lib-common/api'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'
import useNonNullableParams from 'lib-common/useNonNullableParams'
import { useQuery } from 'lib-common/utils/useQuery'
import Spinner from 'lib-components/atoms/state/Spinner'
import Container from 'lib-components/layout/Container'
import Tabs from 'lib-components/molecules/Tabs'
import { fontWeights } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'

import TabGroups from '../components/unit/TabGroups'
import TabUnitInformation from '../components/unit/TabUnitInformation'
import { useTranslation } from '../state/i18n'
import { TitleContext, TitleState } from '../state/title'
import { UnitContext, UnitContextProvider } from '../state/unit'

import TabApplicationProcess from './unit/TabApplicationProcess'
import TabAttendances from './unit/TabAttendances'

const defaultTab = (unit: UnitResponse) => {
  if (unit.permittedActions.has('READ_ATTENDANCES')) return 'attendances'
  return 'groups'
}

const UnitPage = React.memo(function UnitPage({ id }: { id: UUID }) {
  const { i18n } = useTranslation()
  const { setTitle } = useContext<TitleState>(TitleContext)
  const { unitInformation, unitData, reloadUnitData, filters, setFilters } =
    useContext(UnitContext)

  const query = useQuery()
  useEffect(() => {
    if (query.has('start')) {
      const queryStart = LocalDate.parseIso(query.get('start') ?? '')
      setFilters(filters.withStartDate(queryStart))
    }
  }, []) // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    if (unitInformation.isSuccess) {
      setTitle(unitInformation.value.daycare.name)
    }
  }, [setTitle, unitInformation])

  const [openGroups, setOpenGroups] = useState<Record<string, boolean>>({})

  const navigate = useNavigate()
  const queryParams = useCallback(
    () => new URLSearchParams(location.search),
    []
  )

  const getOpenGroupsFromQueryParams = useCallback((): Record<
    string,
    boolean
  > => {
    const qp = queryParams().get('open_groups')
    if (qp != null) {
      return qp
        .split(',')
        .reduce(
          (prev, cur) => (cur ? Object.assign(prev, { [cur]: true }) : prev),
          {}
        )
    } else {
      return {}
    }
  }, [queryParams])

  useEffect(() => {
    setOpenGroups(getOpenGroupsFromQueryParams())
  }, [getOpenGroupsFromQueryParams])

  const openGroupsToStringList = (
    openGroups: Record<string, boolean>
  ): string => {
    return Object.keys(openGroups)
      .reduce(
        (prev: string[], cur: string) =>
          openGroups[cur] == true ? prev.concat(cur) : prev,
        []
      )
      .join(',')
  }

  const [searchParams, setSearchParams] = useSearchParams()

  useEffect(() => {
    const openList = openGroupsToStringList(openGroups)
    if (openList.length > 0) {
      searchParams.set('open_groups', openList)
      setSearchParams(searchParams, {
        replace: true
      })
    }
  }, [openGroups, navigate, setSearchParams, searchParams])

  const tabs = useMemo(
    () => [
      ...(unitInformation.isSuccess &&
      unitInformation.value.permittedActions.has('READ_ATTENDANCES')
        ? [
            {
              id: 'attendances',
              link: `/units/${id}/attendances`,
              label: i18n.unit.tabs.attendances
            }
          ]
        : []),
      {
        id: 'groups',
        link: `/units/${id}/groups`,
        label: i18n.unit.tabs.groups,
        counter: unitData
          .map((data) => data.missingGroupPlacements.length)
          .getOrElse(undefined)
      },
      ...(unitInformation.isSuccess &&
      unitInformation.value.permittedActions.has('READ_DETAILED')
        ? [
            {
              id: 'application-process',
              link: `/units/${id}/application-process`,
              label: i18n.unit.tabs.applicationProcess,
              counter: unitData
                .map(
                  (data) =>
                    (data.placementPlans?.filter((p) => !p.rejectedByCitizen)
                      ?.length ?? 0) + (data.placementProposals?.length ?? 0)
                )
                .getOrElse(0)
            }
          ]
        : []),
      {
        id: 'unit-info',
        link: `/units/${id}/unit-info`,
        label: i18n.unit.tabs.unitInfo
      }
    ],
    [id, i18n, unitData, unitInformation]
  )

  if (unitInformation.isLoading) {
    return (
      <Container>
        <Spinner />
      </Container>
    )
  }
  if (unitInformation.isFailure) {
    return (
      <Container>
        <p>{i18n.common.loadingFailed}</p>
      </Container>
    )
  }

  return (
    <>
      <Tabs tabs={tabs} />
      <Gap size="s" />
      <Container>
        <Routes>
          <Route path="unit-info" element={<TabUnitInformation />} />
          <Route
            path="groups"
            element={
              <TabGroups
                reloadUnitData={reloadUnitData}
                openGroups={openGroups}
                setOpenGroups={setOpenGroups}
              />
            }
          />
          <Route path="attendances" element={<TabAttendances />} />
          <Route
            path="application-process"
            element={
              <TabApplicationProcess
                isLoading={isLoading(unitData)}
                reloadUnitData={reloadUnitData}
              />
            }
          />
          <Route
            index
            element={
              <Navigate replace to={defaultTab(unitInformation.value)} />
            }
          />
        </Routes>
      </Container>
    </>
  )
})

export default React.memo(function UnitPageWrapper() {
  const { id } = useNonNullableParams<{ id: UUID }>()
  return (
    <UnitContextProvider id={id}>
      <UnitPage id={id} />
    </UnitContextProvider>
  )
})

export const NotificationCounter = styled.div`
  width: 1.25em;
  height: 1.25em;
  display: inline-flex;
  justify-content: center;
  align-items: center;

  border-radius: 50%;
  background-color: ${(p) => p.theme.colors.status.warning};
  color: ${(p) => p.theme.colors.grayscale.g0};
  margin-left: ${defaultMargins.xs};
  font-weight: ${fontWeights.bold};
  font-size: 0.75em;
`
