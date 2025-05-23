// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import sortBy from 'lodash/sortBy'
import React, { useContext, useMemo, useState } from 'react'
import { Link } from 'wouter'

import FiniteDateRange from 'lib-common/finite-date-range'
import type { Action } from 'lib-common/generated/action'
import type { UnitBackupCare } from 'lib-common/generated/api-types/backupcare'
import type { DaycareGroup } from 'lib-common/generated/api-types/daycare'
import type {
  MissingBackupGroupPlacement,
  MissingGroupPlacement
} from 'lib-common/generated/api-types/placement'
import type { DaycareId } from 'lib-common/generated/api-types/shared'
import LocalDate from 'lib-common/local-date'
import type { UUID } from 'lib-common/types'
import PlacementCircle from 'lib-components/atoms/PlacementCircle'
import Title from 'lib-components/atoms/Title'
import { Button } from 'lib-components/atoms/buttons/Button'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { Light } from 'lib-components/typography'
import type { Translations } from 'lib-customizations/employee'
import { faArrowRight } from 'lib-icons'

import { useTranslation } from '../../../state/i18n'
import { UIContext } from '../../../state/ui'
import { formatName } from '../../../utils'
import { isPartDayPlacement } from '../../../utils/placements'
import { NotificationCounter } from '../../UnitPage'
import { AgeIndicatorChip } from '../../common/AgeIndicatorChip'
import { CareTypeChip } from '../../common/CareTypeLabel'

import GroupPlacementModal from './missing-group-placements/GroupPlacementModal'
import type { MissingPlacement } from './types'
import { toMissingPlacements } from './types'

function renderMissingGroupPlacementRow(
  missingPlacement: MissingPlacement,
  onAddToGroup: () => void,
  i18n: Translations,
  canCreateGroupPlacement: boolean
) {
  const { childId, firstName, lastName, dateOfBirth, placementPeriod, gap } =
    missingPlacement.data

  const careType =
    missingPlacement.type === 'group'
      ? missingPlacement.data.placementType
      : 'backup-care'

  const fromUnits =
    missingPlacement.type === 'backup' ? missingPlacement.data.fromUnits : []

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
            age={LocalDate.todayInHelsinkiTz().differenceInYears(dateOfBirth)}
          />
          <span data-qa="child-dob">{dateOfBirth.format()}</span>
        </FixedSpaceRow>
      </Td>
      <Td data-qa="placement-type">
        <FixedSpaceColumn spacing="xs" alignItems="flex-start">
          <CareTypeChip type={careType} />
          {fromUnits.map((unit) => (
            <Light key={unit}>{unit}</Light>
          ))}
        </FixedSpaceColumn>
      </Td>
      <Td data-qa="placement-subtype">
        {missingPlacement.type === 'group' && (
          <PlacementCircle
            type={
              isPartDayPlacement(missingPlacement.data.placementType)
                ? 'half'
                : 'full'
            }
            label={
              <ServiceNeedTooltipLabel placement={missingPlacement.data} />
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
  const serviceNeeds = placement.serviceNeeds.reduce<
    { range: FiniteDateRange; nameFi: string }[]
  >((prev, sn) => {
    const snRange = new FiniteDateRange(sn.startDate, sn.endDate)
    return placement.gap.overlaps(snRange)
      ? [...prev, { range: snRange, nameFi: sn.nameFi }]
      : prev
  }, [])
  const serviceNeedGaps = placement.gap
    .getGaps(serviceNeeds.map((sn) => sn.range))
    .map((gap) => ({
      range: gap,
      nameFi: placement.defaultServiceNeedOptionNameFi ?? ''
    }))
  return (
    <>
      {serviceNeeds
        .concat(serviceNeedGaps)
        .sort((a, b) => a.range.start.compareTo(b.range.start))
        .map((sn) => (
          <p
            key={`service-need-option-${sn.range.start.formatIso()}`}
            style={{ whiteSpace: 'nowrap' }}
          >
            {sn.nameFi}:
            <br />
            {sn.range.format()}
          </p>
        ))}
    </>
  )
}

type Props = {
  unitId: DaycareId
  groups: DaycareGroup[]
  missingGroupPlacements: MissingGroupPlacement[]
  missingBackupGroupPlacements: MissingBackupGroupPlacement[]
  backupCares: UnitBackupCare[]
  permittedPlacementActions: Partial<Record<UUID, Action.Placement[]>>
  permittedBackupCareActions: Partial<Record<UUID, Action.BackupCare[]>>
}

export default React.memo(function MissingGroupPlacements({
  unitId,
  groups,
  missingGroupPlacements,
  missingBackupGroupPlacements,
  permittedPlacementActions,
  permittedBackupCareActions
}: Props) {
  const { i18n } = useTranslation()
  const { uiMode, toggleUiMode } = useContext(UIContext)
  const [activeMissingPlacement, setActiveMissingPlacement] =
    useState<MissingPlacement | null>(null)

  const addPlacementToGroup = (missingPlacement: MissingPlacement) => {
    setActiveMissingPlacement(missingPlacement)
    if (missingPlacement.type === 'backup') {
      toggleUiMode('backup-care-group')
    } else {
      toggleUiMode('group-placement')
    }
  }

  const sortedRows = useMemo(
    () =>
      sortBy(
        toMissingPlacements(
          missingGroupPlacements,
          missingBackupGroupPlacements
        ),
        [
          (p: MissingPlacement) => p.data.lastName,
          (p: MissingPlacement) => p.data.firstName,
          (p: MissingPlacement) => p.data.placementPeriod.start,
          (p: MissingPlacement) => p.data.gap.start
        ]
      ),
    [missingGroupPlacements, missingBackupGroupPlacements]
  )

  return (
    <>
      <Title size={2}>
        {i18n.unit.placements.title}
        {sortedRows.length > 0 && (
          <NotificationCounter>{sortedRows.length}</NotificationCounter>
        )}
      </Title>
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
              (row.type === 'backup'
                ? permittedBackupCareActions[row.data.backupCareId]?.includes(
                    'UPDATE'
                  )
                : permittedPlacementActions[row.data.placementId]?.includes(
                    'CREATE_GROUP_PLACEMENT'
                  )) ?? false
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
