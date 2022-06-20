// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import sortBy from 'lodash/sortBy'
import React, { useContext, useState } from 'react'
import { Link } from 'react-router-dom'

import FiniteDateRange from 'lib-common/finite-date-range'
import { Action } from 'lib-common/generated/action'
import { UnitBackupCare } from 'lib-common/generated/api-types/backupcare'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'
import PlacementCircle from 'lib-components/atoms/PlacementCircle'
import Title from 'lib-components/atoms/Title'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { featureFlags, Translations } from 'lib-customizations/employee'
import { faArrowRight } from 'lib-icons'

import { MissingGroupPlacement } from '../../../api/unit'
import GroupPlacementModal from '../../../components/unit/tab-groups/missing-group-placements/GroupPlacementModal'
import { useTranslation } from '../../../state/i18n'
import { UIContext } from '../../../state/ui'
import { DaycareGroup } from '../../../types/unit'
import { formatName } from '../../../utils'
import { isPartDayPlacement } from '../../../utils/placements'
import { AgeIndicatorIcon } from '../../common/AgeIndicatorIcon'
import { CareTypeChip } from '../../common/CareTypeLabel'

function renderMissingGroupPlacementRow(
  missingPlacement: MissingGroupPlacement,
  onAddToGroup: () => void,
  i18n: Translations,
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

  const childIsUnder3 = placementPeriod.start.differenceInYears(dateOfBirth) < 3

  return (
    <Tr
      key={`${childId}:${gap.start.formatIso()}`}
      data-qa="missing-placement-row"
    >
      <Td data-qa="child-name">
        <Link to={`/child-information/${childId}`}>
          {formatName(firstName, lastName, i18n, true)}
        </Link>
      </Td>
      <Td>
        <FixedSpaceRow spacing="xs">
          <AgeIndicatorIcon
            isUnder3={childIsUnder3}
            tooltipText="placement-start"
          />
          <span data-qa="child-dob">{dateOfBirth.format()}</span>
        </FixedSpaceRow>
      </Td>
      <Td data-qa="placement-type">
        <CareTypeChip type={placementType ?? 'backup-care'} />
      </Td>
      <Td data-qa="placement-subtype">
        {placementType && (
          <PlacementCircle
            type={isPartDayPlacement(placementType) ? 'half' : 'full'}
            label={
              featureFlags.groupsTableServiceNeedsEnabled
                ? serviceNeeds
                    .filter((sn) =>
                      new FiniteDateRange(sn.startDate, sn.endDate).includes(
                        LocalDate.today()
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
  reloadUnitData: () => void
  permittedPlacementActions: Record<UUID, Set<Action.Placement>>
  permittedBackupCareActions: Record<UUID, Set<Action.BackupCare>>
}

export default React.memo(function MissingGroupPlacements({
  groups,
  missingGroupPlacements,
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

  const sortedRows = sortBy(missingGroupPlacements, [
    (p: MissingGroupPlacement) => p.lastName,
    (p: MissingGroupPlacement) => p.firstName,
    (p: MissingGroupPlacement) => p.placementPeriod.start,
    (p: MissingGroupPlacement) => p.gap.start
  ])

  return (
    <>
      <Title size={2}>{i18n.unit.placements.title}</Title>
      <Table data-qa="table-of-missing-placements">
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
              row.backup
                ? permittedBackupCareActions[row.placementId]?.has('UPDATE')
                : permittedPlacementActions[row.placementId]?.has(
                    'CREATE_GROUP_PLACEMENT'
                  )
            )
          )}
        </Tbody>
      </Table>
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
