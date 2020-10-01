// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import * as _ from 'lodash'

import { useTranslation } from '~state/i18n'
import {
  DaycareGroupPlacementDetailed,
  DaycareGroupWithPlacements,
  Stats,
  Unit
} from '~types/unit'
import '~components/unit/groups/Group.scss'
import {
  Table,
  Td,
  Th,
  Tr,
  Thead,
  Tbody
} from '~components/shared/layout/Table'
import {
  faAngleDown,
  faAngleUp,
  faCalendarAlt,
  faCheck,
  faExchange,
  faPen,
  faTimes,
  faTrash,
  faUndo
} from 'icon-set'
import { deleteGroup, deletePlacement, OccupancyResponse } from '~api/unit'
import { Link } from 'react-router-dom'
import CareTypeLabel, {
  careTypesFromPlacementType
} from '~components/common/CareTypeLabel'
import styled from 'styled-components'
import { EspooColours } from '~utils/colours'
import Tooltip from '~components/common/Tooltip'
import { capitalizeFirstLetter, formatName } from '~utils'
import { StatusIconContainer } from '~components/common/StatusIconContainer'
import { UnitBackupCare } from '~types/child'
import { updateBackupCare } from 'api/child/backup-care'
import { formatPercentage } from 'components/utils'
import { DataList } from 'components/common/DataList'
import { Gap } from '~components/shared/layout/white-space'
import IconButton from '~components/shared/atoms/buttons/IconButton'
import InlineButton from '~components/shared/atoms/buttons/InlineButton'
import { H3 } from '~components/shared/Typography'
import { UnitFilters } from '~utils/UnitFilters'
import { rangesOverlap } from '~utils/date'
import Colors from 'components/shared/Colors'
import { FixedSpaceRow } from '~components/shared/layout/flex-helpers'

interface Props {
  unit: Unit
  filters: UnitFilters
  group: GroupWithDetails
  caretakers?: Stats
  confirmedOccupancy?: OccupancyResponse
  realizedOccupancy?: OccupancyResponse
  canManageGroups: boolean
  canManageChildren: boolean
  onTransferRequested: (
    placement: DaycareGroupPlacementDetailed | UnitBackupCare
  ) => void
  reload: () => void
  open: boolean
  toggleOpen: () => void
}

export interface GroupWithDetails extends DaycareGroupWithPlacements {
  backupCares: UnitBackupCare[]
}

function getMaxOccupancy(
  occupancyResponse?: OccupancyResponse
): string | undefined {
  const maxOccupancy = occupancyResponse?.max?.percentage
  return maxOccupancy !== undefined ? formatPercentage(maxOccupancy) : undefined
}

function getChildMinMaxHeadcounts(
  occupancyResponse?: OccupancyResponse
): { min: number; max: number } | undefined {
  const headcounts = occupancyResponse?.occupancies?.map(
    ({ headcount }) => headcount
  )

  return headcounts !== undefined
    ? { min: Math.min(...headcounts), max: Math.max(...headcounts) }
    : undefined
}

function Group({
  unit,
  filters,
  group,
  caretakers,
  confirmedOccupancy,
  realizedOccupancy,
  canManageGroups,
  canManageChildren,
  reload,
  onTransferRequested,
  open,
  toggleOpen
}: Props) {
  const { i18n } = useTranslation()

  const maxOccupancy = getMaxOccupancy(confirmedOccupancy)
  const maxRealizedOccupancy = getMaxOccupancy(realizedOccupancy)
  const headcounts = getChildMinMaxHeadcounts(confirmedOccupancy)

  const onDeleteGroup = () => {
    void deleteGroup(unit.id, group.id).then(reload)
  }

  const onDeletePlacement = (
    row: DaycareGroupPlacementDetailed | UnitBackupCare
  ) => {
    if ('type' in row) {
      if (!row.id) throw Error('deleting placement without id')
      void deletePlacement(row.daycarePlacementId, row.id).then(reload)
    } else {
      const { id, period } = row
      void updateBackupCare(id, { period, groupId: undefined }).then(reload)
    }
  }

  const placements = group.placements.filter((placement) => {
    if (filters.endDate == null) {
      return !placement.endDate.isBefore(filters.startDate)
    } else {
      return rangesOverlap(placement, filters)
    }
  })

  const sortedPlacements = _.sortBy(
    _.concat<DaycareGroupPlacementDetailed | UnitBackupCare>(
      placements,
      group.backupCares
    ),
    [
      (p: DaycareGroupPlacementDetailed | UnitBackupCare) => p.child.lastName,
      (p: DaycareGroupPlacementDetailed | UnitBackupCare) => p.child.firstName,
      (p: DaycareGroupPlacementDetailed | UnitBackupCare) =>
        'type' in p ? p.startDate : p.period.start
    ]
  )

  const renderCaretakerCount = () => {
    if (!caretakers) return <span>0</span>

    if (caretakers.minimum == caretakers.maximum)
      return <span>{caretakers.minimum}</span>
    else
      return (
        <span>
          {caretakers.minimum} - {caretakers.maximum}
        </span>
      )
  }

  const showServiceNeed = !unit.type.includes('CLUB') && canManageChildren

  return (
    <DaycareGroup
      data-qa="daycare-group-collapsible"
      data-status={open ? 'open' : 'closed'}
    >
      <TitleBar>
        <TitleContainer onClick={toggleOpen}>
          <H3 fitted data-qa="group-name">
            {capitalizeFirstLetter(group.name)}
          </H3>
          <Gap size="L" horizontal />
          <IconButton
            icon={open ? faAngleUp : faAngleDown}
            size="L"
            gray
            dataQa="collapsible-trigger"
          />
          {!open ? (
            <>
              <Gap size="L" horizontal />
              <TitleSummary
                items={[
                  {
                    label: i18n.unit.groups.caretakers,
                    value: renderCaretakerCount()
                  },
                  ...(headcounts
                    ? [
                        {
                          label: i18n.unit.groups.childrenLabel,
                          value:
                            headcounts.min === headcounts.max
                              ? headcounts.min
                              : `${headcounts.min}-${headcounts.max}`
                        }
                      ]
                    : [])
                ]}
              />
            </>
          ) : null}
        </TitleContainer>
        <Gap size="L" horizontal />
        <Toolbar>
          {canManageGroups ? (
            <>
              <InlineButton
                icon={faTrash}
                text={i18n.unit.groups.deleteGroup}
                onClick={() => onDeleteGroup()}
                disabled={sortedPlacements.length > 0 || !group.deletable}
                dataQa="btn-remove-service-need"
              />
              <Gap size="s" horizontal />
            </>
          ) : null}
          <Link to={`/absences/${group.id}`}>
            <InlineButton
              icon={faCalendarAlt}
              text={i18n.unit.groups.diaryButton}
              onClick={() => undefined}
              dataQa="open-absence-diary-button"
            />
          </Link>
        </Toolbar>
      </TitleBar>
      {open ? (
        <>
          <Gap size="m" />
          <DataList labelWidth="150px" marginBottom="20px">
            <div>
              <label>{i18n.unit.groups.startDate}</label>{' '}
              <span data-qa="group-founded">{group.startDate.format()}</span>
            </div>
            {group.endDate && (
              <div>
                <label>{i18n.unit.groups.endDate}</label>{' '}
                <span>{group.endDate.format()}</span>
              </div>
            )}
            <div>
              <label>{i18n.unit.groups.caretakers}</label>
              <div>
                {renderCaretakerCount()}
                {canManageGroups ? (
                  <>
                    <Gap size="s" horizontal />
                    <Link
                      to={`/units/${unit.id}/groups/${group.id}/caretakers`}
                    >
                      <InlineButton
                        icon={faPen}
                        text={i18n.common.edit}
                        onClick={() => undefined}
                      />
                    </Link>
                  </>
                ) : null}
              </div>
            </div>
            {headcounts && (
              <div>
                <label>{i18n.unit.groups.childrenLabel}</label>
                <span>
                  {headcounts.min === headcounts.max
                    ? `${headcounts.min} ${
                        headcounts.min === 1
                          ? i18n.unit.groups.childrenValue.single
                          : i18n.unit.groups.childrenValue.plural
                      }`
                    : `${headcounts.min}-${headcounts.max} ${i18n.unit.groups.childrenValue.plural}`}
                </span>
              </div>
            )}
            {maxOccupancy && (
              <div>
                <label>{i18n.unit.groups.maxOccupancy}</label>{' '}
                <span>{maxOccupancy}</span>
              </div>
            )}
            {maxRealizedOccupancy && (
              <div>
                <label>{i18n.unit.groups.maxRealizedOccupancy}</label>{' '}
                <span>{maxRealizedOccupancy}</span>
              </div>
            )}
          </DataList>
          {sortedPlacements.length > 0 ? (
            <div className="table-of-group-placements">
              <Table data-qa="table-of-group-placements" className="compact">
                <Thead>
                  <Tr>
                    <Th>{i18n.unit.groups.name}</Th>
                    <Th>{i18n.unit.groups.birthday}</Th>
                    <Th>{i18n.unit.groups.placementDuration}</Th>
                    {showServiceNeed ? (
                      <Th>{i18n.unit.groups.serviceNeed}</Th>
                    ) : null}
                    <Th>{i18n.unit.groups.placementType}</Th>
                    {canManageChildren ? <Th /> : null}
                  </Tr>
                </Thead>
                <Tbody>
                  {sortedPlacements.map((placement, ind) => {
                    const missingServiceNeedDays =
                      'type' in placement
                        ? placement.daycarePlacementMissingServiceNeedDays
                        : placement.missingServiceNeedDays
                    return (
                      <Tr
                        key={placement.id || ''}
                        data-qa="group-placement-row"
                      >
                        <Td data-qa="child-name">
                          <Link to={`/child-information/${placement.child.id}`}>
                            {formatName(
                              placement.child.firstName,
                              placement.child.lastName,
                              i18n,
                              true
                            )}
                          </Link>
                        </Td>
                        <Td data-qa="child-dob">
                          {'type' in placement
                            ? placement.child.dateOfBirth.format()
                            : placement.child.birthDate.format()}
                        </Td>
                        <Td data-qa="placement-duration">
                          {'type' in placement
                            ? `${placement.startDate.format()} - ${placement.endDate.format()}`
                            : `${placement.period.start.format()} - ${placement.period.end.format()}`}
                        </Td>
                        {showServiceNeed ? (
                          <Td data-qa="service-need">
                            {missingServiceNeedDays > 0 ? (
                              <Tooltip
                                tooltipId={`service-need-tooltip-${ind}`}
                                tooltipText={`${i18n.unit.groups.serviceNeedMissing1} ${missingServiceNeedDays} ${i18n.unit.groups.serviceNeedMissing2}`}
                                place={'right'}
                              >
                                <StatusIconContainer
                                  color={EspooColours.orange}
                                >
                                  <FontAwesomeIcon icon={faTimes} inverse />
                                </StatusIconContainer>
                              </Tooltip>
                            ) : (
                              <Tooltip
                                tooltipId={`service-need-tooltip-${ind}`}
                                tooltipText={
                                  i18n.unit.groups.serviceNeedChecked
                                }
                                place={'right'}
                              >
                                <StatusIconContainer color={EspooColours.green}>
                                  <FontAwesomeIcon icon={faCheck} inverse />
                                </StatusIconContainer>
                              </Tooltip>
                            )}
                          </Td>
                        ) : null}
                        <Td data-qa="placement-type">
                          {'type' in placement ? (
                            careTypesFromPlacementType(placement.type)
                          ) : (
                            <CareTypeLabel type="backup-care" />
                          )}
                        </Td>
                        {canManageChildren ? (
                          <Td className="row-buttons">
                            <FixedSpaceRow>
                              <InlineButton
                                onClick={() => onTransferRequested(placement)}
                                dataQa="transfer-btn"
                                icon={faExchange}
                                text={i18n.unit.groups.transferBtn}
                              />
                              <InlineButton
                                onClick={() => onDeletePlacement(placement)}
                                dataQa="remove-btn"
                                icon={faUndo}
                                text={i18n.unit.groups.returnBtn}
                              />
                            </FixedSpaceRow>
                          </Td>
                        ) : null}
                      </Tr>
                    )
                  })}
                </Tbody>
              </Table>
            </div>
          ) : (
            <p data-qa="no-children-placeholder">
              {i18n.unit.groups.noChildren}
            </p>
          )}
        </>
      ) : null}
    </DaycareGroup>
  )
}

const TitleBar = styled.div`
  display: flex;
  flex-direction: row;
`

const TitleContainer = styled.div`
  display: flex;
  flex-direction: row;
  align-items: center;
  flex-grow: 1;
`

const Toolbar = styled.div`
  display: flex;
  flex-direction: row;
  align-items: center;
  flex-wrap: nowrap;
`

const DaycareGroup = styled.div`
  border: ${Colors.greyscale.medium} solid 1px;
  padding: 16px;
  margin-bottom: 16px;
`

const TitleSummary = React.memo(function TitleSummary({
  items
}: {
  items: { label: string; value: React.ReactNode }[]
}) {
  return (
    <>
      {items.map(({ label, value }, index) => (
        <React.Fragment key={label}>
          {index !== 0 ? (
            <>
              <Gap size="xs" horizontal />|<Gap size="xs" horizontal />
            </>
          ) : null}
          <Label>{label}</Label>
          <Gap size="xxs" horizontal />
          {value}
        </React.Fragment>
      ))}
    </>
  )
})

const Label = styled.label`
  font-weight: 600;
`

export default Group
