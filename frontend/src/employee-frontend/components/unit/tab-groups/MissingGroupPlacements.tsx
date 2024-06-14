// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import sortBy from 'lodash/sortBy'
import React, { useContext, useState } from 'react'
import { Link } from 'react-router-dom'

import FiniteDateRange from 'lib-common/finite-date-range'
import { Action } from 'lib-common/generated/action'
import { UnitBackupCare } from 'lib-common/generated/api-types/backupcare'
import { DaycareGroup } from 'lib-common/generated/api-types/daycare'
import { MissingGroupPlacement } from 'lib-common/generated/api-types/placement'
import { UUID } from 'lib-common/types'
import PlacementCircle from 'lib-components/atoms/PlacementCircle'
import Title from 'lib-components/atoms/Title'
import { Button } from 'lib-components/atoms/buttons/Button'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { Light } from 'lib-components/typography'
import { Translations } from 'lib-customizations/employee'
import { faArrowRight } from 'lib-icons'

import GroupPlacementModal from '../../../components/unit/tab-groups/missing-group-placements/GroupPlacementModal'
import { useTranslation } from '../../../state/i18n'
import { UIContext } from '../../../state/ui'
import { formatName } from '../../../utils'
import { isPartDayPlacement } from '../../../utils/placements'
import { AgeIndicatorChip } from '../../common/AgeIndicatorChip'
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
    placementType
  } = missingPlacement

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
        <FixedSpaceRow spacing="xs" alignItems="center">
          <AgeIndicatorChip
            age={placementPeriod.start.differenceInYears(dateOfBirth)}
          />
          <span data-qa="child-dob">{dateOfBirth.format()}</span>
        </FixedSpaceRow>
      </Td>
      <Td data-qa="placement-type">
        <FixedSpaceColumn spacing="xs" alignItems="flex-start">
          <CareTypeChip type={placementType ?? 'backup-care'} />
          {missingPlacement.fromUnits.map((unit) => (
            <Light key={unit}>{unit}</Light>
          ))}
        </FixedSpaceColumn>
      </Td>
      <Td data-qa="placement-subtype">
        {placementType && (
          <PlacementCircle
            type={isPartDayPlacement(placementType) ? 'half' : 'full'}
            label={<ServiceNeedTooltipLabel placement={missingPlacement} />}
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
          <Button
            appearance="inline"
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

const ServiceNeedTooltipLabel = ({
  placement
}: {
  placement: MissingGroupPlacement
}) => {
  const serviceNeeds = placement.serviceNeeds.filter((sn) =>
    placement.gap.overlaps(new FiniteDateRange(sn.startDate, sn.endDate))
  )
  return (
    <>
      {serviceNeeds
        .sort((a, b) => a.startDate.compareTo(b.startDate))
        .map((sn, index) => (
          <p
            key={`service-need-option-${sn.nameFi}-${index}`}
            style={{ whiteSpace: 'nowrap' }}
          >
            {sn.nameFi}:
            <br />
            {sn.startDate.format()} - {sn.endDate.format()}
          </p>
        ))}
    </>
  )
}

type Props = {
  unitId: UUID
  groups: DaycareGroup[]
  missingGroupPlacements: MissingGroupPlacement[]
  backupCares: UnitBackupCare[]
  permittedPlacementActions: Record<UUID, Action.Placement[]>
  permittedBackupCareActions: Record<UUID, Action.BackupCare[]>
}

export default React.memo(function MissingGroupPlacements({
  unitId,
  groups,
  missingGroupPlacements,
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
                ? permittedBackupCareActions[row.placementId]?.includes(
                    'UPDATE'
                  )
                : permittedPlacementActions[row.placementId]?.includes(
                    'CREATE_GROUP_PLACEMENT'
                  )
            )
          )}
        </Tbody>
      </Table>
      {['group-placement', 'backup-care-group'].includes(uiMode) &&
        activeMissingPlacement && (
          <GroupPlacementModal
            unitId={unitId}
            groups={groups}
            missingPlacement={activeMissingPlacement}
          />
        )}
    </>
  )
})
