{
  /*
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
*/
}

import React, { Dispatch, SetStateAction, useContext } from 'react'
import { ContentArea } from '@evaka/lib-components/src/layout/Container'
import { UnitContext } from '../../state/unit'
import { SpinnerSegment } from '@evaka/lib-components/src/atoms/state/Spinner'
import ErrorSegment from '@evaka/lib-components/src/atoms/state/ErrorSegment'
import { UserContext } from '../../state/user'
import { requireRole } from '../../utils/roles'
import { FixedSpaceColumn } from '@evaka/lib-components/src/layout/flex-helpers'
import Groups from '../../components/unit/tab-groups/Groups'
import MissingGroupPlacements from '../../components/unit/tab-groups/MissingGroupPlacements'

interface Props {
  reloadUnitData: () => void
  openGroups: Record<string, boolean>
  setOpenGroups: Dispatch<SetStateAction<Record<string, boolean>>>
}

function TabGroups({ reloadUnitData, openGroups, setOpenGroups }: Props) {
  const { roles } = useContext(UserContext)
  const {
    unitInformation,
    unitData,
    filters,
    setFilters,
    savePosition
  } = useContext(UnitContext)

  const isUnitSupervisor = requireRole(roles, 'ADMIN', 'UNIT_SUPERVISOR')

  if (unitInformation.isFailure || unitData.isFailure) {
    return <ErrorSegment />
  }

  if (unitInformation.isLoading || unitData.isLoading) {
    return <SpinnerSegment />
  }

  return (
    <FixedSpaceColumn>
      <ContentArea opaque>
        <MissingGroupPlacements
          canManageChildren={isUnitSupervisor}
          groups={unitData.value.groups}
          missingGroupPlacements={unitData.value.missingGroupPlacements}
          backupCares={unitData.value.backupCares}
          savePosition={savePosition}
          reloadUnitData={reloadUnitData}
        />
      </ContentArea>

      <ContentArea opaque>
        <Groups
          unit={unitInformation.value.daycare}
          canManageGroups={isUnitSupervisor}
          canManageChildren={isUnitSupervisor}
          filters={filters}
          setFilters={setFilters}
          groups={unitData.value.groups}
          placements={unitData.value.placements}
          backupCares={unitData.value.backupCares}
          groupCaretakers={unitData.value.caretakers.groupCaretakers}
          groupConfirmedOccupancies={unitData.value.groupOccupancies?.confirmed}
          groupRealizedOccupancies={unitData.value.groupOccupancies?.realized}
          reloadUnitData={reloadUnitData}
          openGroups={openGroups}
          setOpenGroups={setOpenGroups}
        />
      </ContentArea>
    </FixedSpaceColumn>
  )
}

export default React.memo(TabGroups)
