// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, {
  Dispatch,
  SetStateAction,
  useCallback,
  useContext,
  useMemo
} from 'react'

import { getUnitGroupDetails } from 'employee-frontend/api/unit'
import { combine, Result } from 'lib-common/api'
import { Action } from 'lib-common/generated/action'
import { useApiState } from 'lib-common/utils/useRestApi'
import { ContentArea } from 'lib-components/layout/Container'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'

import Groups from '../../components/unit/tab-groups/Groups'
import MissingGroupPlacements from '../../components/unit/tab-groups/MissingGroupPlacements'
import { UnitContext } from '../../state/unit'
import { renderResult } from '../async-rendering'

import TerminatedPlacements from './tab-groups/TerminatedPlacements'

interface Props {
  openGroups: Record<string, boolean>
  setOpenGroups: Dispatch<SetStateAction<Record<string, boolean>>>
}

export default React.memo(function TabGroups({
  openGroups,
  setOpenGroups
}: Props) {
  const {
    unitId,
    unitInformation,
    reloadUnitNotifications,
    filters,
    setFilters
  } = useContext(UnitContext)
  const [groupData, reloadGroupData] = useApiState(
    () => getUnitGroupDetails(unitId, filters.startDate, filters.endDate),
    [unitId, filters.startDate, filters.endDate]
  )
  const reloadData = useCallback(() => {
    void reloadGroupData()
    reloadUnitNotifications()
  }, [reloadGroupData, reloadUnitNotifications])

  const groupPermittedActions: Result<
    Record<string, Set<Action.Group> | undefined>
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
              groups={groupData.groups}
              missingGroupPlacements={groupData.missingGroupPlacements}
              backupCares={groupData.backupCares}
              reloadGroupData={reloadData}
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
            reloadGroupData={reloadData}
            openGroups={openGroups}
            setOpenGroups={setOpenGroups}
          />
        </ContentArea>
      </FixedSpaceColumn>
    )
  )
})
