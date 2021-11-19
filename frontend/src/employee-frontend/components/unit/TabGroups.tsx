{
  /*
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
*/
}

import React, { Dispatch, SetStateAction, useContext } from 'react'
import { ContentArea } from 'lib-components/layout/Container'
import { UnitContext } from '../../state/unit'
import { SpinnerSegment } from 'lib-components/atoms/state/Spinner'
import ErrorSegment from 'lib-components/atoms/state/ErrorSegment'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import Groups from '../../components/unit/tab-groups/Groups'
import MissingGroupPlacements from '../../components/unit/tab-groups/MissingGroupPlacements'

interface Props {
  reloadUnitData: () => void
  openGroups: Record<string, boolean>
  setOpenGroups: Dispatch<SetStateAction<Record<string, boolean>>>
}

function TabGroups({ reloadUnitData, openGroups, setOpenGroups }: Props) {
  const { unitInformation, unitData, filters, setFilters } =
    useContext(UnitContext)

  if (unitInformation.isFailure || unitData.isFailure) {
    return <ErrorSegment />
  }

  if (unitInformation.isLoading || unitData.isLoading) {
    return <SpinnerSegment />
  }

  const groupPermittedActions = Object.fromEntries(
    unitInformation.value.groups.map(({ id, permittedActions }) => [
      id,
      permittedActions
    ])
  )

  return (
    <FixedSpaceColumn>
      <ContentArea opaque>
        <MissingGroupPlacements
          groups={unitData.value.groups}
          missingGroupPlacements={unitData.value.missingGroupPlacements}
          backupCares={unitData.value.backupCares}
          reloadUnitData={reloadUnitData}
          permittedPlacementActions={unitData.value.permittedPlacementActions}
          permittedBackupCareActions={unitData.value.permittedBackupCareActions}
        />
      </ContentArea>

      <ContentArea opaque>
        <Groups
          unit={unitInformation.value.daycare}
          permittedActions={unitInformation.value.permittedActions}
          filters={filters}
          setFilters={setFilters}
          groups={unitData.value.groups}
          placements={unitData.value.placements}
          backupCares={unitData.value.backupCares}
          groupPermittedActions={groupPermittedActions}
          groupCaretakers={unitData.value.caretakers.groupCaretakers}
          groupConfirmedOccupancies={unitData.value.groupOccupancies?.confirmed}
          groupRealizedOccupancies={unitData.value.groupOccupancies?.realized}
          permittedBackupCareActions={unitData.value.permittedBackupCareActions}
          permittedGroupPlacementActions={
            unitData.value.permittedGroupPlacementActions
          }
          reloadUnitData={reloadUnitData}
          openGroups={openGroups}
          setOpenGroups={setOpenGroups}
        />
      </ContentArea>
    </FixedSpaceColumn>
  )
}

export default React.memo(TabGroups)
