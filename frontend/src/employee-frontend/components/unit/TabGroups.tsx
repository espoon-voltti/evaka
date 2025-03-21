// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Dispatch, SetStateAction, useContext, useMemo } from 'react'

import { combine } from 'lib-common/api'
import { Action } from 'lib-common/generated/action'
import { DaycareResponse } from 'lib-common/generated/api-types/daycare'
import { constantQuery, useQueryResult } from 'lib-common/query'
import { ContentArea } from 'lib-components/layout/Container'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { featureFlags } from 'lib-customizations/employee'

import Groups from '../../components/unit/tab-groups/Groups'
import MissingGroupPlacements from '../../components/unit/tab-groups/MissingGroupPlacements'
import { UnitContext } from '../../state/unit'
import { renderResult } from '../async-rendering'

import { nekkuUnitNumbersQuery, unitGroupDetailsQuery } from './queries'
import TerminatedPlacements from './tab-groups/TerminatedPlacements'

interface Props {
  unitInformation: DaycareResponse
  openGroups: Record<string, boolean>
  setOpenGroups: Dispatch<SetStateAction<Record<string, boolean>>>
}

export default React.memo(function TabGroups({
  unitInformation,
  openGroups,
  setOpenGroups
}: Props) {
  const unitId = unitInformation.daycare.id
  const { filters, setFilters } = useContext(UnitContext)

  const groupPermittedActions: Record<string, Action.Group[] | undefined> =
    useMemo(
      () =>
        Object.fromEntries(
          unitInformation.groups.map(({ id, permittedActions }) => [
            id,
            permittedActions
          ])
        ),
      [unitInformation]
    )

  const groupData = useQueryResult(
    unitGroupDetailsQuery({
      unitId,
      from: filters.startDate,
      to: filters.endDate
    })
  )

  const nekkuData = useQueryResult(
    featureFlags.nekkuIntegration ? nekkuUnitNumbersQuery() : constantQuery([])
  )

  return renderResult(
    combine(groupData, nekkuData),
    ([groupData, nekkuData], isReloading) => (
      <FixedSpaceColumn data-qa="unit-groups-page" data-loading={isReloading}>
        {groupData.recentlyTerminatedPlacements.length > 0 && (
          <ContentArea opaque data-qa="terminated-placements-section">
            <TerminatedPlacements
              recentlyTerminatedPlacements={
                groupData.recentlyTerminatedPlacements
              }
            />
          </ContentArea>
        )}

        {groupData.missingGroupPlacements.length +
          groupData.missingBackupGroupPlacements.length >
          0 && (
          <ContentArea opaque data-qa="missing-placements-section">
            <MissingGroupPlacements
              unitId={unitId}
              groups={groupData.groups}
              missingGroupPlacements={groupData.missingGroupPlacements}
              missingBackupGroupPlacements={
                groupData.missingBackupGroupPlacements
              }
              backupCares={groupData.backupCares}
              permittedPlacementActions={groupData.permittedPlacementActions}
              permittedBackupCareActions={groupData.permittedBackupCareActions}
            />
          </ContentArea>
        )}

        <ContentArea opaque>
          <Groups
            unit={unitInformation.daycare}
            permittedActions={unitInformation.permittedActions}
            filters={filters}
            setFilters={setFilters}
            groups={groupData.groups}
            placements={groupData.placements}
            backupCares={groupData.backupCares}
            groupPermittedActions={groupPermittedActions}
            groupCaretakers={groupData.caretakers}
            groupConfirmedOccupancies={groupData.groupOccupancies?.confirmed}
            groupRealizedOccupancies={groupData.groupOccupancies?.realized}
            permittedBackupCareActions={groupData.permittedBackupCareActions}
            permittedGroupPlacementActions={
              groupData.permittedGroupPlacementActions
            }
            unitChildrenCapacityFactors={groupData.unitChildrenCapacityFactors}
            openGroups={openGroups}
            setOpenGroups={setOpenGroups}
            nekkuUnits={nekkuData}
          />
        </ContentArea>
      </FixedSpaceColumn>
    )
  )
})
