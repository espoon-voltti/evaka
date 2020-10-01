// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useState } from 'react'
import { Button, Table } from '~components/shared/alpha'
import { faArrowRight } from '@evaka/icons'
import _ from 'lodash'
import { useTranslation } from '~state/i18n'
import { Title } from '~components/shared/alpha'
import {
  DaycareGroupPlacementDetailed,
  flatMapGroupPlacements,
  DaycareGroup,
  DaycarePlacement
} from '~types/unit'
import GroupPlacementModal from '~components/unit/placements/GroupPlacementModal'
import { UIContext } from '~state/ui'
import { Translations } from '~assets/i18n'
import { Link } from 'react-router-dom'
import CareTypeLabel, {
  careTypesFromPlacementType
} from '~components/common/CareTypeLabel'
import { UnitBackupCare } from '~types/child'
import BackupCareGroupModal from './placements/BackupCareGroupModal'
import { formatName } from '~utils'
import { UnitFilters } from '~utils/UnitFilters'
import { rangesOverlap } from '~utils/date'

function renderMissingPlacementRow(
  missingPlacement: DaycareGroupPlacementDetailed,
  onAddToGroup: () => void,
  i18n: Translations,
  savePosition: () => void,
  canManageChildren: boolean
) {
  const {
    child,
    daycarePlacementStartDate,
    daycarePlacementEndDate,
    startDate,
    endDate,
    type
  } = missingPlacement

  return (
    <Table.Row
      key={`${child.id}:${type}:${daycarePlacementStartDate.formatIso()}`}
      dataQa="missing-placement-row"
    >
      <Table.Td dataQa="child-name">
        <Link to={`/child-information/${child.id}`} onClick={savePosition}>
          {formatName(child.firstName, child.lastName, i18n, true)}
        </Link>
      </Table.Td>
      <Table.Td dataQa="child-dob">{child.dateOfBirth.format()}</Table.Td>
      <Table.Td dataQa="placement-duration">
        {`${daycarePlacementStartDate.format()} - ${daycarePlacementEndDate.format()}`}
      </Table.Td>
      {canManageChildren ? (
        <Table.Td dataQa="group-missing-duration">
          {`${startDate.format()} - ${endDate.format()}`}
        </Table.Td>
      ) : null}
      <Table.Td dataQa="placement-type">
        {careTypesFromPlacementType(type)}
      </Table.Td>
      {canManageChildren ? (
        <Table.Td>
          <Button
            plain
            width={'narrow'}
            onClick={() => onAddToGroup()}
            icon={faArrowRight}
            dataQa="add-to-group-btn"
          >
            {i18n.unit.placements.addToGroup}
          </Button>
        </Table.Td>
      ) : null}
    </Table.Row>
  )
}

function renderBackupCareRow(
  { id, child, period }: UnitBackupCare,
  onAddToGroup: () => void,
  i18n: Translations,
  savePosition: () => void,
  canManageChildren: boolean
) {
  return (
    <Table.Row key={id} dataQa="missing-placement-row">
      <Table.Td dataQa="child-name">
        <Link to={`/child-information/${child.id}`} onClick={savePosition}>
          {formatName(child.firstName, child.lastName, i18n, true)}
        </Link>
      </Table.Td>
      <Table.Td dataQa="child-dob">{child.birthDate.format()}</Table.Td>
      <Table.Td dataQa="placement-duration">
        {`${period.start.format()} - ${period.end.format()}`}
      </Table.Td>
      {canManageChildren ? (
        <Table.Td dataQa="group-missing-duration">
          {`${period.start.format()} - ${period.end.format()}`}
        </Table.Td>
      ) : null}
      <Table.Td dataQa="placement-type">
        <CareTypeLabel type="backup-care" />
      </Table.Td>
      {canManageChildren ? (
        <Table.Td>
          <Button
            plain
            width={'narrow'}
            onClick={() => onAddToGroup()}
            icon={faArrowRight}
            dataQa="add-to-group-btn"
          >
            {i18n.unit.placements.addToGroup}
          </Button>
        </Table.Td>
      ) : null}
    </Table.Row>
  )
}

type Props = {
  canManageChildren: boolean
  filters: UnitFilters
  groups: DaycareGroup[]
  placements: DaycarePlacement[]
  backupCares: UnitBackupCare[]
  savePosition: () => void
  loadUnitData: () => void
}

export default React.memo(function Placements({
  canManageChildren,
  filters,
  groups,
  placements,
  backupCares,
  savePosition,
  loadUnitData
}: Props) {
  const { i18n } = useTranslation()
  const { uiMode, toggleUiMode } = useContext(UIContext)
  const [
    activeMissingPlacement,
    setActiveMissingPlacement
  ] = useState<DaycareGroupPlacementDetailed | null>(null)
  const [
    activeBackupCare,
    setActiveBackupCare
  ] = useState<UnitBackupCare | null>(null)

  const addPlacementToGroup = (
    missingPlacement: DaycareGroupPlacementDetailed
  ) => {
    setActiveMissingPlacement(missingPlacement)
    toggleUiMode('group-placement')
  }

  const addBackupCareToGroup = (backupCare: UnitBackupCare) => {
    setActiveBackupCare(backupCare)
    toggleUiMode('backup-care-group')
  }

  const missingPlacements: DaycareGroupPlacementDetailed[] = flatMapGroupPlacements(
    placements
  )
    .filter(({ groupId }) => !groupId)
    .filter((missingPlacement) => {
      if (filters.endDate == null) {
        return !missingPlacement.endDate.isBefore(filters.startDate)
      } else {
        return rangesOverlap(missingPlacement, filters)
      }
    })

  const incompleteBackupCare: Array<
    DaycareGroupPlacementDetailed | UnitBackupCare
  > = backupCares.filter((backupCare) => !backupCare.group)

  const sortedRows = _.sortBy(incompleteBackupCare.concat(missingPlacements), [
    (p: DaycareGroupPlacementDetailed | UnitBackupCare) => p.child.lastName,
    (p: DaycareGroupPlacementDetailed | UnitBackupCare) => p.child.firstName,
    (p: DaycareGroupPlacementDetailed | UnitBackupCare) =>
      'type' in p ? p.daycarePlacementStartDate : p.period.start,
    (p: DaycareGroupPlacementDetailed | UnitBackupCare) =>
      'type' in p ? p.startDate : p.period.start
  ])

  return (
    <>
      <Title size={2}>{i18n.unit.placements.title}</Title>
      <div
        className="table-of-missing-groups"
        data-qa="table-of-missing-groups"
      >
        <Table.Table dataQa="table-of-missing-groups" className="compact">
          <Table.Head>
            <Table.Row>
              <Table.Th>{i18n.unit.placements.name}</Table.Th>
              <Table.Th>{i18n.unit.placements.birthday}</Table.Th>
              <Table.Th>{i18n.unit.placements.placementDuration}</Table.Th>
              {canManageChildren ? (
                <Table.Th>{i18n.unit.placements.missingGroup}</Table.Th>
              ) : null}
              <Table.Th>{i18n.unit.placements.type}</Table.Th>
              {canManageChildren ? <Table.Th /> : null}
            </Table.Row>
          </Table.Head>
          <Table.Body>
            {sortedRows.map((row) =>
              'type' in row
                ? renderMissingPlacementRow(
                    row,
                    () => addPlacementToGroup(row),
                    i18n,
                    savePosition,
                    canManageChildren
                  )
                : renderBackupCareRow(
                    row,
                    () => addBackupCareToGroup(row),
                    i18n,
                    savePosition,
                    canManageChildren
                  )
            )}
          </Table.Body>
        </Table.Table>
      </div>
      {uiMode == 'group-placement' && activeMissingPlacement && (
        <GroupPlacementModal
          groups={groups}
          missingPlacement={activeMissingPlacement}
          reload={loadUnitData}
        />
      )}
      {uiMode == 'backup-care-group' && activeBackupCare && (
        <BackupCareGroupModal
          backupCare={activeBackupCare}
          groups={groups}
          reload={loadUnitData}
        />
      )}
    </>
  )
})
