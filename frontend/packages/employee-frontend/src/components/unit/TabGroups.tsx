import React, { Dispatch, SetStateAction, useContext } from 'react'
import { ContentArea } from '~components/shared/layout/Container'
import { UnitContext } from '~state/unit'
import { isFailure, isLoading } from '~api'
import { SpinnerSegment } from '~components/shared/atoms/state/Spinner'
import ErrorSegment from '~components/shared/atoms/state/ErrorSegment'
import { UserContext } from '~state/user'
import { requireRole } from '~utils/roles'
import { FixedSpaceColumn } from '~components/shared/layout/flex-helpers'
import Groups from '~components/unit/tab-groups/Groups'
import MissingGroupPlacements from '~components/unit/tab-groups/MissingGroupPlacements'

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

  const isManager = requireRole(
    roles,
    'ADMIN',
    'SERVICE_WORKER',
    'UNIT_SUPERVISOR'
  )

  if (isFailure(unitInformation) || isFailure(unitData)) {
    return <ErrorSegment />
  }

  if (isLoading(unitInformation) || isLoading(unitData)) {
    return <SpinnerSegment />
  }

  return (
    <FixedSpaceColumn>
      <ContentArea opaque>
        <MissingGroupPlacements
          canManageChildren={isManager}
          groups={unitData.data.groups}
          missingGroupPlacements={unitData.data.missingGroupPlacements}
          backupCares={unitData.data.backupCares}
          savePosition={savePosition}
          reloadUnitData={reloadUnitData}
        />
      </ContentArea>

      <ContentArea opaque>
        <Groups
          unit={unitInformation.data.daycare}
          canManageGroups={isManager}
          canManageChildren={isManager}
          filters={filters}
          setFilters={setFilters}
          groups={unitData.data.groups}
          placements={unitData.data.placements}
          backupCares={unitData.data.backupCares}
          groupCaretakers={unitData.data.caretakers.groupCaretakers}
          groupConfirmedOccupancies={unitData.data.groupOccupancies?.confirmed}
          groupRealizedOccupancies={unitData.data.groupOccupancies?.realized}
          reloadUnitData={reloadUnitData}
          openGroups={openGroups}
          setOpenGroups={setOpenGroups}
        />
      </ContentArea>
    </FixedSpaceColumn>
  )
}

export default React.memo(TabGroups)
