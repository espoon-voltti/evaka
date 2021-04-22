// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useState, Dispatch, SetStateAction } from 'react'
import styled from 'styled-components'
import { faAngleDown, faAngleUp } from 'lib-icons'
import _ from 'lodash'
import { useTranslation } from '../../../state/i18n'
import { Gap } from 'lib-components/white-space'
import { H2 } from 'lib-components/typography'
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
import { UnitBackupCare } from '../../../types/child'
import BackupCareGroupModal from '../../../components/unit/tab-groups/missing-group-placements/BackupCareGroupModal'
import { UnitFilters } from '../../../utils/UnitFilters'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import { Link } from 'react-router-dom'
import { requireRole } from '../../../utils/roles'
import { UserContext } from '../../../state/user'
import { DataList } from '../../../components/common/DataList'
import UnitDataFilters from '../../../components/unit/UnitDataFilters'

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
  setFilters: Dispatch<SetStateAction<UnitFilters>>
  groups: DaycareGroup[]
  placements: DaycarePlacement[]
  backupCares: UnitBackupCare[]
  groupCaretakers: Record<string, Stats>
  groupConfirmedOccupancies?: Record<string, OccupancyResponse>
  groupRealizedOccupancies?: Record<string, OccupancyResponse>
  reloadUnitData: () => void
  openGroups: Record<string, boolean>
  setOpenGroups: Dispatch<SetStateAction<Record<string, boolean>>>
}

export default React.memo(function Groups({
  unit,
  canManageGroups,
  canManageChildren,
  filters,
  setFilters,
  groups,
  placements,
  backupCares,
  groupCaretakers,
  groupConfirmedOccupancies,
  groupRealizedOccupancies,
  reloadUnitData,
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
            data-qa="toggle-all-groups-collapsible"
          />
        </TitleContainer>
        {canManageGroups && (
          <div>
            <Link to={`/units/${unit.id}/family-contacts`}>
              <InlineButton
                text={i18n.unit.groups.familyContacts}
                onClick={() => undefined}
                data-qa="open-family-contacts-button"
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
      <DataList>
        <div>
          <label>{i18n.unit.filters.title}</label>
          <div>
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
          </div>
        </div>
      </DataList>
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
        placements,
        backupCares,
        groupCaretakers,
        canManageGroups,
        canManageChildren,
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
