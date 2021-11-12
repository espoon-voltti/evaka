// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Dispatch, SetStateAction, useContext, useState } from 'react'
import styled from 'styled-components'
import { faAngleDown, faAngleUp } from 'lib-icons'
import _ from 'lodash'
import { UnitBackupCare } from 'lib-common/generated/api-types/backupcare'
import { useTranslation } from '../../../state/i18n'
import { Gap } from 'lib-components/white-space'
import { H2, Label } from 'lib-components/typography'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import AddButton from 'lib-components/atoms/buttons/AddButton'
import {
  DaycareGroup,
  DaycareGroupPlacementDetailed,
  DaycarePlacement,
  flatMapGroupPlacements,
  Stats,
  Unit
} from '../../../types/unit'
import { OccupancyResponse } from '../../../api/unit'
import Group from '../../../components/unit/tab-groups/groups/Group'
import { UIContext } from '../../../state/ui'
import GroupModal from '../../../components/unit/tab-groups/groups/GroupModal'
import GroupTransferModal from '../../../components/unit/tab-groups/groups/group/GroupTransferModal'
import BackupCareGroupModal from '../../../components/unit/tab-groups/missing-group-placements/BackupCareGroupModal'
import { UnitFilters } from '../../../utils/UnitFilters'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import { Link } from 'react-router-dom'
import { requireRole } from '../../../utils/roles'
import { UserContext } from '../../../state/user'
import UnitDataFilters from '../../../components/unit/UnitDataFilters'
import { UUID } from 'lib-common/types'
import { Action } from 'lib-common/generated/action'

function renderGroups(
  unit: Unit,
  filters: UnitFilters,
  groups: DaycareGroup[],
  groupPermittedActions: Record<UUID, Set<Action.Group>>,
  placements: DaycarePlacement[],
  permittedBackupCareActions: Record<UUID, Set<Action.BackupCare>>,
  permittedGroupPlacementActions: Record<UUID, Set<Action.GroupPlacement>>,
  backupCares: UnitBackupCare[],
  groupCaretakers: Record<string, Stats>,
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
          reload={reload}
          onTransferRequested={onTransferRequested}
          open={!!openGroups[group.id]}
          toggleOpen={() =>
            setOpenGroups((current) => ({
              ...current,
              [group.id]: !current[group.id]
            }))
          }
          permittedActions={groupPermittedActions[group.id] ?? new Set()}
          permittedBackupCareActions={permittedBackupCareActions}
          permittedGroupPlacementActions={permittedGroupPlacementActions}
        />
      ))}
    </div>
  )
}

type Props = {
  unit: Unit
  permittedActions: Set<Action.Unit>
  filters: UnitFilters
  setFilters: Dispatch<SetStateAction<UnitFilters>>
  groups: DaycareGroup[]
  placements: DaycarePlacement[]
  backupCares: UnitBackupCare[]
  groupPermittedActions: Record<UUID, Set<Action.Group>>
  groupCaretakers: Record<string, Stats>
  groupConfirmedOccupancies?: Record<string, OccupancyResponse>
  groupRealizedOccupancies?: Record<string, OccupancyResponse>
  reloadUnitData: () => void
  openGroups: Record<string, boolean>
  setOpenGroups: Dispatch<SetStateAction<Record<string, boolean>>>
  permittedBackupCareActions: Record<UUID, Set<Action.BackupCare>>
  permittedGroupPlacementActions: Record<UUID, Set<Action.GroupPlacement>>
}

export default React.memo(function Groups({
  unit,
  permittedActions,
  filters,
  setFilters,
  groups,
  placements,
  backupCares,
  groupPermittedActions,
  groupCaretakers,
  groupConfirmedOccupancies,
  groupRealizedOccupancies,
  reloadUnitData,
  openGroups,
  setOpenGroups,
  permittedBackupCareActions,
  permittedGroupPlacementActions
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

  const canSeeFamilyContactsReport = requireRole(
    roles,
    'ADMIN',
    'UNIT_SUPERVISOR'
  )

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
            data-qa="toggle-all-groups-collapsible"
          />
        </TitleContainer>
        <FixedSpaceRow spacing="L" alignItems="center">
          {canSeeFamilyContactsReport && (
            <Link to={`/units/${unit.id}/family-contacts`}>
              <InlineButton
                text={i18n.unit.groups.familyContacts}
                onClick={() => undefined}
                data-qa="open-family-contacts-button"
              />
            </Link>
          )}
          {permittedActions.has('CREATE_GROUP') ? (
            <AddButton
              text={i18n.unit.groups.create}
              onClick={() => toggleUiMode('create-new-daycare-group')}
              disabled={uiMode === 'create-new-daycare-group'}
              flipped
            />
          ) : null}
        </FixedSpaceRow>
      </TitleBar>
      <Gap size="s" />
      <FixedSpaceRow alignItems="center">
        <Label>{i18n.unit.filters.title}</Label>
        <UnitDataFilters
          canEdit={requireRole(
            roles,
            'ADMIN',
            'SERVICE_WORKER',
            'UNIT_SUPERVISOR',
            'FINANCE_ADMIN'
          )}
          filters={filters}
          setFilters={setFilters}
        />
      </FixedSpaceRow>
      <Gap size="s" />
      {uiMode === 'create-new-daycare-group' && (
        <GroupModal unitId={unit.id} reload={reloadUnitData} />
      )}
      {uiMode === 'group-transfer' && transferredPlacement && (
        <>
          {'type' in transferredPlacement ? (
            <GroupTransferModal
              placement={transferredPlacement}
              groups={groups}
              reload={reloadUnitData}
            />
          ) : (
            <BackupCareGroupModal
              backupCare={transferredPlacement}
              groups={groups}
              reload={reloadUnitData}
            />
          )}
        </>
      )}
      {renderGroups(
        unit,
        filters,
        groups,
        groupPermittedActions,
        placements,
        permittedBackupCareActions,
        permittedGroupPlacementActions,
        backupCares,
        groupCaretakers,
        reloadUnitData,
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
