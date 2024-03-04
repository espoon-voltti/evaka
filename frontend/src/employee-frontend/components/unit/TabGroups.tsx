// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Dispatch, SetStateAction, useContext, useMemo } from 'react'

import { combine, Result } from 'lib-common/api'
import { Action } from 'lib-common/generated/action'
import { useQueryResult } from 'lib-common/query'
import { ContentArea } from 'lib-components/layout/Container'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'

import Groups from '../../components/unit/tab-groups/Groups'
import MissingGroupPlacements from '../../components/unit/tab-groups/MissingGroupPlacements'
import { UnitContext } from '../../state/unit'
import { renderResult } from '../async-rendering'

import { unitGroupDetailsQuery } from './queries'
import TerminatedPlacements from './tab-groups/TerminatedPlacements'

interface Props {
  openGroups: Record<string, boolean>
  setOpenGroups: Dispatch<SetStateAction<Record<string, boolean>>>
}

export default React.memo(function TabGroups({
  openGroups,
  setOpenGroups
}: Props) {
  const { unitId, unitInformation, filters, setFilters } =
    useContext(UnitContext)
  const groupData = useQueryResult(
    unitGroupDetailsQuery({
      unitId,
      from: filters.startDate,
      to: filters.endDate
    })
  )

  const groupPermittedActions: Result<
    Record<string, Action.Group[] | undefined>
  > = useMemo(
    () =>
      unitInformation.map((unitInformation) =>
        Object.fromEntries(
          unitInformation.groups.map(({ id, permittedActions }) => [
            id,
            permittedActions
          ])
        )
      ),
    [unitInformation]
  )

  return renderResult(
    combine(unitInformation, groupData, groupPermittedActions),
    ([unitInformation, groupData, groupPermittedActions], isReloading) => (
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

        {groupData.missingGroupPlacements.length > 0 && (
          <ContentArea opaque data-qa="missing-placements-section">
            <MissingGroupPlacements
              unitId={unitId}
              groups={groupData.groups}
              missingGroupPlacements={groupData.missingGroupPlacements}
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
          />
        </ContentArea>
      </FixedSpaceColumn>
    )
  )
})
