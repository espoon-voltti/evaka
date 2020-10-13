// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useState, Dispatch, SetStateAction } from 'react'
import styled from 'styled-components'
import { faAngleDown, faAngleUp } from 'icon-set'
import _ from 'lodash'
import { useTranslation } from '~state/i18n'
import { Gap } from '~components/shared/layout/white-space'
import { H2 } from '~components/shared/Typography'
import IconButton from '~components/shared/atoms/buttons/IconButton'
import AddButton from '~components/shared/atoms/buttons/AddButton'
import {
  DaycareGroup,
  DaycareGroupPlacementDetailed,
  DaycarePlacement,
  flatMapGroupPlacements,
  Stats,
  Unit
} from '~types/unit'
import { OccupancyResponse } from '~api/unit'
import Group from '~components/unit/groups/Group'
import { UIContext } from '~state/ui'
import GroupModal from '~components/unit/groups/GroupModal'
import GroupTransferModal from '~components/unit/groups/group/GroupTransferModal'
import { UnitBackupCare } from '~types/child'
import BackupCareGroupModal from '~components/unit/placements/BackupCareGroupModal'
import { UnitFilters } from '~utils/UnitFilters'
import InlineButton from '~components/shared/atoms/buttons/InlineButton'
import { Link } from 'react-router-dom'
import { requireRole } from '~utils/roles'
import { UserContext } from '~state/user'

function renderGroups(
  unit: Unit,
  filters: UnitFilters,
  groups: DaycareGroup[],
  placements: DaycarePlacement[],
  backupCares: UnitBackupCare[],
  groupCaretakers: Record<string, Stats>,
  canManageGroups: boolean,
  canManageChildren: boolean,
  reload: () => void,
  onTransferRequested: (
    placement: DaycareGroupPlacementDetailed | UnitBackupCare
  ) => void,
  openGroups: { [k: string]: boolean },
  setOpenGroups: React.Dispatch<React.SetStateAction<{ [k: string]: boolean }>>,
  confirmedOccupancies?: Record<string, OccupancyResponse>,
  realizedOccupancies?: Record<string, OccupancyResponse>
) {
  const groupsWithPlacements = groups.map((group) => ({
    ...group,
    placements: flatMapGroupPlacements(placements).filter(
      (it) => it.groupId == group.id
    ),
    backupCares: backupCares.filter((it) => it.group?.id === group.id)
  }))
  const sortedGroups = _.sortBy(groupsWithPlacements, [
    (g) => g.name.toLowerCase()
  ])

  return (
    <div data-qa="daycare-groups-list">
      {sortedGroups.map((group) => (
        <Group
          unit={unit}
          filters={filters}
          group={group}
          caretakers={groupCaretakers[group.id]}
          confirmedOccupancy={confirmedOccupancies?.[group.id]}
          realizedOccupancy={realizedOccupancies?.[group.id]}
          key={group.id}
          canManageGroups={canManageGroups}
          canManageChildren={canManageChildren}
          reload={reload}
          onTransferRequested={onTransferRequested}
          open={!!openGroups[group.id]}
          toggleOpen={() =>
            setOpenGroups((current) => ({
              ...current,
              [group.id]: !current[group.id]
            }))
          }
        />
      ))}
    </div>
  )
}

type Props = {
  unit: Unit
  canManageGroups: boolean
  canManageChildren: boolean
  filters: UnitFilters
  groups: DaycareGroup[]
  placements: DaycarePlacement[]
  backupCares: UnitBackupCare[]
  groupCaretakers: Record<string, Stats>
  groupConfirmedOccupancies?: Record<string, OccupancyResponse>
  groupRealizedOccupancies?: Record<string, OccupancyResponse>
  loadUnitData: () => void
  openGroups: Record<string, boolean>
  setOpenGroups: Dispatch<SetStateAction<Record<string, boolean>>>
}

export default React.memo(function Groups({
  unit,
  canManageGroups,
  canManageChildren,
  filters,
  groups,
  placements,
  backupCares,
  groupCaretakers,
  groupConfirmedOccupancies,
  groupRealizedOccupancies,
  loadUnitData,
  openGroups,
  setOpenGroups
}: Props) {
  const { i18n } = useTranslation()
  const { uiMode, toggleUiMode } = useContext(UIContext)
  const { roles } = useContext(UserContext)
  const [transferredPlacement, setTransferredPlacement] = useState<
    DaycareGroupPlacementDetailed | UnitBackupCare | null
  >(null)

  const allGroupsAreOpen = groups.every(({ id }) => openGroups[id])

  const toggleAllGroups = () => {
    if (groups) {
      if (allGroupsAreOpen) {
        setOpenGroups({})
      } else {
        setOpenGroups(Object.fromEntries(groups.map(({ id }) => [id, true])))
      }
    }
  }

  return (
    <>
      <TitleBar
        data-qa="groups-title-bar"
        data-status={allGroupsAreOpen ? 'open' : 'closed'}
      >
        <TitleContainer onClick={toggleAllGroups}>
          <H2 fitted>{i18n.unit.groups.title}</H2>
          <Gap size="XL" horizontal />
          <IconButton
            icon={allGroupsAreOpen ? faAngleUp : faAngleDown}
            size="L"
            gray
            dataQa="toggle-all-groups-collapsible"
          />
        </TitleContainer>
        {requireRole(
          roles,
          'ADMIN',
          'SERVICE_WORKER',
          'FINANCE_ADMIN',
          'UNIT_SUPERVISOR'
        ) && (
          <div>
            <Link to={`/units/${unit.id}/family-contacts`}>
              <InlineButton
                text={i18n.unit.groups.familyContacts}
                onClick={() => undefined}
                dataQa="open-family-contacts-button"
              />
            </Link>
          </div>
        )}
        {canManageGroups ? (
          <>
            <Gap size="L" horizontal />
            <AddButton
              text={i18n.unit.groups.create}
              onClick={() => toggleUiMode('create-new-daycare-group')}
              disabled={uiMode === 'create-new-daycare-group'}
              flipped
            />
          </>
        ) : null}
      </TitleBar>
      <Gap size="s" />
      {uiMode === 'create-new-daycare-group' && (
        <GroupModal unitId={unit.id} reload={loadUnitData} />
      )}
      {uiMode === 'group-transfer' && transferredPlacement && (
        <>
          {'type' in transferredPlacement ? (
            <GroupTransferModal
              placement={transferredPlacement}
              groups={groups}
              reload={loadUnitData}
            />
          ) : (
            <BackupCareGroupModal
              backupCare={transferredPlacement}
              groups={groups}
              reload={loadUnitData}
            />
          )}
        </>
      )}
      {renderGroups(
        unit,
        filters,
        groups,
        placements,
        backupCares,
        groupCaretakers,
        canManageGroups,
        canManageChildren,
        loadUnitData,
        (placement) => {
          setTransferredPlacement(placement)
          toggleUiMode('group-transfer')
        },
        openGroups,
        setOpenGroups,
        groupConfirmedOccupancies,
        groupRealizedOccupancies
      )}
    </>
  )
})

const TitleBar = styled.div`
  display: flex;
  flex-direction: row;
  align-items: center;
`

const TitleContainer = styled.div`
  display: flex;
  flex-direction: row;
  align-items: center;
  flex-grow: 1;
`
