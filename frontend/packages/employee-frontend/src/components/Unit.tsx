// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, {
  useContext,
  useEffect,
  useState,
  Dispatch,
  SetStateAction
} from 'react'
import LocalDate from '@evaka/lib-common/src/local-date'
import { UUID } from '~types'
import { Container, ContentArea, Title } from '~components/shared/alpha'
import UnitInformation from './unit/UnitInformation'
import Placements from './unit/Placements'
import '~components/Unit.scss'
import Groups from '~components/unit/Groups'
import { UnitContext, UnitState } from '~state/unit'
import { getDaycare, getUnitData, UnitData, UnitResponse } from '~api/unit'
import { isFailure, isLoading, isSuccess, Loading, Result } from '~api'
import { SpinnerSegment } from 'components/shared/atoms/state/Spinner'
import ErrorSegment from 'components/shared/atoms/state/ErrorSegment'
import UnitDataFilters from '~components/unit/UnitDataFilters'
import Occupancy from '~components/unit/unit-data/Occupancy'
import PlacementPlans from '~components/unit/PlacementPlans'
import styled from 'styled-components'
import { TitleContext, TitleState } from '~state/title'
import UnitAccessControl from '~components/unit/UnitAccessControl'
import { UnitFilters } from 'utils/UnitFilters'
import { requireRole, RequireRole } from '~utils/roles'
import { useParams } from 'react-router-dom'
import { useRestApi } from '~utils/useRestApi'
import { useQuery, ReadOnlySearchParams } from '~utils/useQuery'
import { ApplyAclRoles, UserContext } from '~state/user'
import { Unit } from '~types/unit'
import PlacementProposals from 'components/unit/PlacementProposals'

const UnitBasicsSection = styled.div`
  display: flex;
`

export default React.memo(function Unit(): JSX.Element {
  const { id } = useParams<{ id: UUID }>()

  const { setTitle } = useContext<TitleState>(TitleContext)

  const [response, setResponse] = useState<Result<UnitResponse>>(Loading())
  const reloadUnit = useRestApi(getDaycare, setResponse)

  useEffect(() => reloadUnit(id), [id])
  useEffect(() => {
    if (isSuccess(response)) {
      setTitle(response.data.daycare.name)
    }
  }, [response])

  const { scrollToPosition, savePosition } = useContext<UnitState>(UnitContext)

  const query = useQuery()
  const [filters, setFilters] = useState(
    new UnitFilters(getFilterStartDate(query), '3 months')
  )

  const [unitData, setUnitData] = useState<Result<UnitData>>(Loading())
  const reloadUnitData = useRestApi(getUnitData, setUnitData)

  const loadUnitData = () => {
    savePosition()
    reloadUnitData(id, filters.startDate, filters.endDate)
  }

  useEffect(() => {
    loadUnitData()
  }, [filters])

  useEffect(() => {
    if (isSuccess(unitData)) {
      scrollToPosition()
    }
  }, [unitData])

  const [openGroups, setOpenGroups] = useState<Record<string, boolean>>({})

  useEffect(() => {
    if (isSuccess(unitData)) {
      setOpenGroups(
        Object.fromEntries(
          Object.entries(openGroups).filter(([id]) =>
            unitData.data.groups.some((group) => group.id === id)
          )
        )
      )
    }
  }, [unitData])

  if (isFailure(response) || isFailure(unitData)) {
    return <ErrorSegment />
  }

  if (isLoading(response) || isLoading(unitData)) {
    return <SpinnerSegment />
  }

  return (
    <ApplyAclRoles roles={response.data.currentUserRoles}>
      <UnitDetails
        unit={response.data.daycare}
        unitData={unitData.data}
        loadUnitData={loadUnitData}
        filters={filters}
        setFilters={setFilters}
        savePosition={savePosition}
        openGroups={openGroups}
        setOpenGroups={setOpenGroups}
      />
    </ApplyAclRoles>
  )
})

function getFilterStartDate(query: ReadOnlySearchParams): LocalDate {
  const queryStart = query.get('start')
  return queryStart ? LocalDate.parseIso(queryStart) : LocalDate.today()
}

const UnitDetails = React.memo(function UnitDetails({
  unit,
  unitData,
  loadUnitData,
  filters,
  setFilters,
  savePosition,
  openGroups,
  setOpenGroups
}: {
  unit: Unit
  unitData: UnitData
  loadUnitData: () => void
  filters: UnitFilters
  setFilters: Dispatch<SetStateAction<UnitFilters>>
  savePosition: () => void
  openGroups: Record<string, boolean>
  setOpenGroups: Dispatch<SetStateAction<Record<string, boolean>>>
}) {
  const { roles } = useContext(UserContext)

  const isSupervisor = requireRole(
    roles,
    'ADMIN',
    'SERVICE_WORKER',
    'UNIT_SUPERVISOR'
  )

  const isFinanceAdmin = requireRole(roles, 'FINANCE_ADMIN')

  return (
    <Container>
      <ContentArea opaque>
        <UnitBasicsSection>
          <div className="column is-8-desktop is-full-tablet">
            <Title size={2} dataQa="unit-name">
              {unit.name}
            </Title>
            <UnitDataFilters
              canEdit={isSupervisor || isFinanceAdmin}
              filters={filters}
              setFilters={setFilters}
            />
            <UnitInformation
              unit={unit}
              caretakers={unitData.caretakers.unitCaretakers}
            />
          </div>
          {unitData.unitOccupancies ? (
            <div className="column is-4-desktop is-6-tablet">
              <Occupancy
                filters={filters}
                occupancies={unitData.unitOccupancies}
              />
            </div>
          ) : null}
        </UnitBasicsSection>
      </ContentArea>

      {unitData.placementProposals ? (
        <ContentArea opaque>
          <PlacementProposals
            unitId={unit.id}
            placementPlans={unitData.placementProposals}
            loadUnitData={loadUnitData}
          />
        </ContentArea>
      ) : null}

      {unitData.placementPlans ? (
        <ContentArea opaque>
          <PlacementPlans placementPlans={unitData.placementPlans} />
        </ContentArea>
      ) : null}

      {isSupervisor && (
        <ContentArea opaque>
          <Placements
            canManageChildren={isSupervisor}
            filters={filters}
            groups={unitData.groups}
            placements={unitData.placements}
            backupCares={unitData.backupCares}
            savePosition={savePosition}
            loadUnitData={loadUnitData}
          />
        </ContentArea>
      )}

      <ContentArea opaque>
        <Groups
          unit={unit}
          canManageGroups={isSupervisor}
          canManageChildren={isSupervisor}
          filters={filters}
          groups={unitData.groups}
          placements={unitData.placements}
          backupCares={unitData.backupCares}
          groupCaretakers={unitData.caretakers.groupCaretakers}
          groupConfirmedOccupancies={unitData.groupOccupancies?.confirmed}
          groupRealizedOccupancies={unitData.groupOccupancies?.realized}
          loadUnitData={loadUnitData}
          openGroups={openGroups}
          setOpenGroups={setOpenGroups}
        />
      </ContentArea>

      <RequireRole oneOf={['ADMIN', 'UNIT_SUPERVISOR']}>
        <UnitAccessControl unitId={unit.id} />
      </RequireRole>
    </Container>
  )
})
