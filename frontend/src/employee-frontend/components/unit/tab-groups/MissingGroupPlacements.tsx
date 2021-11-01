// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useState } from 'react'
import _ from 'lodash'
import { Table, Td, Th, Tr, Thead, Tbody } from 'lib-components/layout/Table'
import Title from 'lib-components/atoms/Title'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import { faArrowRight } from 'lib-icons'
import { useTranslation } from '../../../state/i18n'
import { DaycareGroup } from '../../../types/unit'
import GroupPlacementModal from '../../../components/unit/tab-groups/missing-group-placements/GroupPlacementModal'
import { UIContext } from '../../../state/ui'
import { featureFlags, Translations } from 'lib-customizations/employee'
import { Link } from 'react-router-dom'
import CareTypeLabel, {
  careTypesFromPlacementType
} from '../../../components/common/CareTypeLabel'
import { UnitBackupCare } from '../../../types/child'
import { formatName } from '../../../utils'
import PlacementCircle from 'lib-components/atoms/PlacementCircle'
import { MissingGroupPlacement } from '../../../api/unit'
import { isPartDayPlacement } from '../../../utils/placements'
import FiniteDateRange from 'lib-common/finite-date-range'
import { UUID } from 'lib-common/types'
import { Action } from 'lib-common/generated/action'

function renderMissingGroupPlacementRow(
  missingPlacement: MissingGroupPlacement,
  onAddToGroup: () => void,
  i18n: Translations,
  savePosition: () => void,
  canCreateGroupPlacement: boolean
) {
  const {
    childId,
    firstName,
    lastName,
    dateOfBirth,
    placementPeriod,
    gap,
    serviceNeeds,
    placementType
  } = missingPlacement

  return (
    <Tr
      key={`${childId}:${gap.start.formatIso()}`}
      data-qa="missing-placement-row"
    >
      <Td data-qa="child-name">
        <Link to={`/child-information/${childId}`} onClick={savePosition}>
          {formatName(firstName, lastName, i18n, true)}
        </Link>
      </Td>
      <Td data-qa="child-dob">{dateOfBirth.format()}</Td>
      <Td data-qa="placement-type">
        {placementType ? (
          careTypesFromPlacementType(placementType)
        ) : (
          <CareTypeLabel type="backup-care" />
        )}
      </Td>
      <Td data-qa="placement-subtype">
        {placementType && (
          <PlacementCircle
            type={isPartDayPlacement(placementType) ? 'half' : 'full'}
            label={
              featureFlags.groupsTableServiceNeedsEnabled
                ? serviceNeeds
                    .filter((sn) =>
                      gap.overlaps(
                        new FiniteDateRange(sn.startDate, sn.endDate)
                      )
                    )
                    .map((sn) => sn.option.nameFi)
                    .join(' / ')
                : i18n.placement.type[placementType]
            }
          />
        )}
      </Td>
      <Td data-qa="placement-duration">
        {`${placementPeriod.start.format()} - ${placementPeriod.end.format()}`}
      </Td>
      <Td data-qa="group-missing-duration">
        {`${gap.start.format()} - ${gap.end.format()}`}
      </Td>
      <Td>
        {canCreateGroupPlacement && (
          <InlineButton
            onClick={() => onAddToGroup()}
            icon={faArrowRight}
            data-qa="add-to-group-btn"
            text={i18n.unit.placements.addToGroup}
          />
        )}
      </Td>
    </Tr>
  )
}

type Props = {
  groups: DaycareGroup[]
  missingGroupPlacements: MissingGroupPlacement[]
  backupCares: UnitBackupCare[]
  savePosition: () => void
  reloadUnitData: () => void
  permittedPlacementActions: Record<UUID, Set<Action.Placement>>
  permittedBackupCareActions: Record<UUID, Set<Action.BackupCare>>
}

export default React.memo(function MissingGroupPlacements({
  groups,
  missingGroupPlacements,
  savePosition,
  reloadUnitData,
  permittedPlacementActions,
  permittedBackupCareActions
}: Props) {
  const { i18n } = useTranslation()
  const { uiMode, toggleUiMode } = useContext(UIContext)
  const [activeMissingPlacement, setActiveMissingPlacement] =
    useState<MissingGroupPlacement | null>(null)

  const addPlacementToGroup = (missingPlacement: MissingGroupPlacement) => {
    setActiveMissingPlacement(missingPlacement)
    if (missingPlacement.backup) {
      toggleUiMode('backup-care-group')
    } else {
      toggleUiMode('group-placement')
    }
  }

  const sortedRows = _.sortBy(missingGroupPlacements, [
    (p: MissingGroupPlacement) => p.lastName,
    (p: MissingGroupPlacement) => p.firstName,
    (p: MissingGroupPlacement) => p.placementPeriod.start,
    (p: MissingGroupPlacement) => p.gap.start
  ])

  return (
    <>
      <Title size={2}>{i18n.unit.placements.title}</Title>
      <div
        className="table-of-missing-groups"
        data-qa="table-of-missing-groups"
      >
        <Table data-qa="table-of-missing-groups">
          <Thead>
            <Tr>
              <Th>{i18n.unit.placements.name}</Th>
              <Th>{i18n.unit.placements.birthday}</Th>
              <Th>{i18n.unit.placements.type}</Th>
              <Th>{i18n.unit.placements.subtype}</Th>
              <Th>{i18n.unit.placements.placementDuration}</Th>
              <Th>{i18n.unit.placements.missingGroup}</Th>
              <Th />
            </Tr>
          </Thead>
          <Tbody>
            {sortedRows.map((row) =>
              renderMissingGroupPlacementRow(
                row,
                () => addPlacementToGroup(row),
                i18n,
                savePosition,
                row.backup
                  ? permittedBackupCareActions[row.placementId]?.has('UPDATE')
                  : permittedPlacementActions[row.placementId]?.has(
                      'CREATE_GROUP_PLACEMENT'
                    )
              )
            )}
          </Tbody>
        </Table>
      </div>
      {['group-placement', 'backup-care-group'].includes(uiMode) &&
        activeMissingPlacement && (
          <GroupPlacementModal
            groups={groups}
            missingPlacement={activeMissingPlacement}
            reload={reloadUnitData}
          />
        )}
    </>
  )
})
