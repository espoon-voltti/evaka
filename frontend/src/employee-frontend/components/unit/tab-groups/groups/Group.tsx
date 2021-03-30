// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useEffect, useState } from 'react'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import * as _ from 'lodash'

import { useTranslation } from '../../../../state/i18n'
import {
  DaycareDailyNote,
  DaycareGroupPlacementDetailed,
  DaycareGroupWithPlacements,
  Stats,
  Unit
} from '../../../../types/unit'
import { Table, Td, Th, Tr, Thead, Tbody } from 'lib-components/layout/Table'
import {
  faAngleDown,
  faAngleUp,
  faCalendarAlt,
  faCheck,
  faExchange,
  faPen,
  farStickyNote,
  faStickyNote,
  faTimes,
  faTrash,
  faUndo
} from 'lib-icons'
import {
  deleteGroup,
  deletePlacement,
  getGroupDaycareDailyNotes,
  OccupancyResponse
} from '../../../../api/unit'
import { Link } from 'react-router-dom'
import CareTypeLabel, {
  careTypesFromPlacementType
} from '../../../../components/common/CareTypeLabel'
import styled from 'styled-components'
import colors from 'lib-components/colors'
import { capitalizeFirstLetter, formatName } from '../../../../utils'
import { StatusIconContainer } from '../../../../components/common/StatusIconContainer'
import { UnitBackupCare } from '../../../../types/child'
import { updateBackupCare } from '../../../../api/child/backup-care'
import { formatPercentage } from '../../../../components/utils'
import { DataList } from '../../../../components/common/DataList'
import { Gap } from 'lib-components/white-space'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import { H3 } from 'lib-components/typography'
import { UnitFilters } from '../../../../utils/UnitFilters'
import { rangesOverlap } from '../../../../utils/date'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import PlacementCircle from 'lib-components/atoms/PlacementCircle'
import Tooltip from 'lib-components/atoms/Tooltip'
import { UIContext } from '../../../../state/ui'
import GroupUpdateModal from '../../../../components/unit/tab-groups/groups/group/GroupUpdateModal'
import { isPartDayPlacement } from '../../../../utils/placements'
import { Loading, Result } from 'lib-common/api'
import { SpinnerSegment } from 'lib-components/atoms/state/Spinner'
import ErrorSegment from 'lib-components/atoms/state/ErrorSegment'
import DaycareDailyNoteModal from '../daycare-daily-notes/DaycareDailyNoteModal'
import { useRestApi } from 'lib-common/utils/useRestApi'
import RoundIcon from 'lib-components/atoms/RoundIcon'
import { isNotProduction, isPilotUnit } from '../../../../constants'

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

const IconContainer = styled.div`
  display: flex;
  justify-content: center;
  font-size: 18px;
`

const GroupNoteLinkContainer = styled.div`
  margin-top: 16px;
  margin-left: 8px;
`

const RowActionContainer = styled.div`
  display: flex;
`

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

interface DaycareDailyNoteAndChildInfo {
  daycareDailyNote: DaycareDailyNote | null
  childId: string | null
  groupId: string | null
  childFirstName: string
  childLastName: string
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
  const { uiMode, toggleUiMode, clearUiMode } = useContext(UIContext)
  const [
    selectedDaycareDailyNote,
    setSelectedDaycareDailyNote
  ] = useState<DaycareDailyNoteAndChildInfo>({
    daycareDailyNote: null,
    childId: null,
    groupId: null,
    childFirstName: '',
    childLastName: ''
  })
  const [groupDaycareDailyNotes, setGroupDaycareDailyNotes] = useState<
    Result<DaycareDailyNote[]>
  >(Loading.of())

  const loadGroupDaycareDailyNotes = useRestApi(
    () => getGroupDaycareDailyNotes(group.id),
    setGroupDaycareDailyNotes
  )

  useEffect(loadGroupDaycareDailyNotes, [loadGroupDaycareDailyNotes])

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

  const getChildNote = (childId: string): DaycareDailyNote | undefined =>
    groupDaycareDailyNotes.isSuccess
      ? groupDaycareDailyNotes.value.find((note) => note.childId === childId)
      : undefined

  const getGroupNote = (groupId: string): DaycareDailyNote | undefined =>
    groupDaycareDailyNotes.isSuccess
      ? groupDaycareDailyNotes.value.find((note) => note.groupId === groupId)
      : undefined

  const renderDaycareDailyNote = (
    placement: DaycareGroupPlacementDetailed | UnitBackupCare
  ) => {
    const childNote = getChildNote(placement.child.id)
    return (
      <>
        {groupDaycareDailyNotes.isLoading &&
          (isNotProduction() || isPilotUnit(unit.id)) && <SpinnerSegment />}
        {groupDaycareDailyNotes.isFailure &&
          (isNotProduction() || isPilotUnit(unit.id)) && (
            <ErrorSegment title={i18n.common.loadingFailed} compact />
          )}
        {groupDaycareDailyNotes.isSuccess &&
          (isNotProduction() || isPilotUnit(unit.id)) && (
            <Tooltip
              dataQa={`daycare-daily-note-hover-${placement.child.id}`}
              up
              tooltip={
                !childNote ? (
                  <span>{i18n.unit.groups.daycareDailyNote.edit}</span>
                ) : (
                  <div>
                    <h4>{i18n.unit.groups.daycareDailyNote.header}</h4>
                    <h5>{i18n.unit.groups.daycareDailyNote.notesHeader}</h5>
                    <p>{childNote.note}</p>
                    <h5>{i18n.unit.groups.daycareDailyNote.feedingHeader}</h5>
                    <p>
                      {childNote.feedingNote
                        ? i18n.unit.groups.daycareDailyNote.level[
                            childNote.feedingNote
                          ]
                        : ''}
                    </p>
                    <h5>{i18n.unit.groups.daycareDailyNote.sleepingHeader}</h5>
                    <p>
                      {childNote.sleepingNote
                        ? i18n.unit.groups.daycareDailyNote.level[
                            childNote.sleepingNote
                          ]
                        : ''}
                    </p>
                    <h5>{i18n.unit.groups.daycareDailyNote.reminderHeader}</h5>
                    <p>
                      {childNote.reminders
                        .map(
                          (reminder) =>
                            i18n.unit.groups.daycareDailyNote.reminderType[
                              reminder
                            ]
                        )
                        .join(',')}
                    </p>
                    <h5>
                      {
                        i18n.unit.groups.daycareDailyNote
                          .otherThingsToRememberHeader
                      }
                    </h5>
                    <p>{childNote.reminderNote}</p>
                  </div>
                )
              }
            >
              <RoundIcon
                active={childNote != null}
                dataQa={`daycare-daily-note-icon-${placement.child.id}`}
                content={faStickyNote}
                color={colors.blues.primary}
                size="m"
                onClick={() => {
                  setSelectedDaycareDailyNote({
                    daycareDailyNote: childNote || null,
                    groupId: null,
                    childId: placement.child.id,
                    childFirstName: placement.child.firstName || '',
                    childLastName: placement.child.lastName || ''
                  })
                  toggleUiMode(`daycare-daily-note-edit-${group.id}`)
                }}
              />
            </Tooltip>
          )}
      </>
    )
  }

  return (
    <DaycareGroup
      className={'daycare-group-collapsible'}
      data-qa={`daycare-group-collapsible-${group.id}`}
      data-status={open ? 'open' : 'closed'}
    >
      {uiMode === `update-group-${group.id}` && (
        <GroupUpdateModal group={group} reload={reload} />
      )}
      {uiMode === `daycare-daily-note-edit-${group.id}` && (
        <DaycareDailyNoteModal
          note={selectedDaycareDailyNote.daycareDailyNote}
          childId={selectedDaycareDailyNote.childId}
          groupId={selectedDaycareDailyNote.groupId}
          childFirstName={selectedDaycareDailyNote.childFirstName}
          childLastName={selectedDaycareDailyNote.childLastName}
          groupNote={getGroupNote(group.id)?.note ?? null}
          onClose={() => clearUiMode()}
          reload={() => loadGroupDaycareDailyNotes()}
        />
      )}
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
                icon={faPen}
                text={i18n.unit.groups.update}
                onClick={() => toggleUiMode(`update-group-${group.id}`)}
                dataQa="btn-update-group"
              />
              <Gap size="s" horizontal />
              <InlineButton
                icon={faTrash}
                text={i18n.unit.groups.deleteGroup}
                onClick={() => onDeleteGroup()}
                disabled={sortedPlacements.length > 0 || !group.deletable}
                dataQa="btn-remove-group"
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
              <span data-qa="group-start-date">{group.startDate.format()}</span>
            </div>
            {group.endDate && (
              <div>
                <label>{i18n.unit.groups.endDate}</label>{' '}
                <span data-qa="group-end-date">{group.endDate.format()}</span>
              </div>
            )}
            <div>
              <label>{i18n.unit.groups.caretakers}</label>
              <FixedSpaceRow>
                {renderCaretakerCount()}
                {canManageGroups ? (
                  <Link to={`/units/${unit.id}/groups/${group.id}/caretakers`}>
                    <InlineButton
                      icon={faPen}
                      text={i18n.common.edit}
                      onClick={() => undefined}
                    />
                  </Link>
                ) : null}
              </FixedSpaceRow>
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
              <Table data-qa="table-of-group-placements">
                <Thead>
                  <Tr>
                    {(isNotProduction() || isPilotUnit(unit.id)) && (
                      <Th>
                        <IconContainer>
                          <Tooltip
                            up
                            tooltip={
                              <span>
                                {i18n.unit.groups.daycareDailyNote.header}
                              </span>
                            }
                          >
                            <FontAwesomeIcon
                              icon={faStickyNote}
                              color={colors.greyscale.dark}
                            />
                          </Tooltip>
                        </IconContainer>
                      </Th>
                    )}
                    <Th>{i18n.unit.groups.name}</Th>
                    <Th>{i18n.unit.groups.birthday}</Th>
                    <Th>{i18n.unit.groups.placementType}</Th>
                    <Th>{i18n.unit.groups.placementSubtype}</Th>
                    {showServiceNeed ? (
                      <Th>{i18n.unit.groups.serviceNeed}</Th>
                    ) : null}
                    <Th>{i18n.unit.groups.placementDuration}</Th>
                    {canManageChildren ? <Th /> : null}
                  </Tr>
                </Thead>
                <Tbody>
                  {sortedPlacements.map((placement) => {
                    const missingServiceNeedDays =
                      'type' in placement
                        ? placement.daycarePlacementMissingServiceNeedDays
                        : placement.missingServiceNeedDays
                    return (
                      <Tr
                        key={placement.id || ''}
                        className={'group-placement-row'}
                        data-qa={`group-placement-row-${placement.child.id}`}
                      >
                        {(isNotProduction() || isPilotUnit(unit.id)) && (
                          <Td data-qa="daily-note">
                            {renderDaycareDailyNote(placement)}
                          </Td>
                        )}
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
                        <Td data-qa="placement-type">
                          {'type' in placement ? (
                            careTypesFromPlacementType(placement.type)
                          ) : (
                            <CareTypeLabel type="backup-care" />
                          )}
                        </Td>
                        <Td data-qa="placement-subtype">
                          {'type' in placement ? (
                            <PlacementCircle
                              type={
                                isPartDayPlacement(placement.type)
                                  ? 'half'
                                  : 'full'
                              }
                              label={i18n.placement.type[placement.type]}
                            />
                          ) : null}
                        </Td>
                        {showServiceNeed ? (
                          <Td data-qa="service-need">
                            {missingServiceNeedDays > 0 ? (
                              <Tooltip
                                tooltip={
                                  <span>{`${i18n.unit.groups.serviceNeedMissing1} ${missingServiceNeedDays} ${i18n.unit.groups.serviceNeedMissing2}`}</span>
                                }
                              >
                                <StatusIconContainer
                                  color={colors.accents.orange}
                                >
                                  <FontAwesomeIcon icon={faTimes} inverse />
                                </StatusIconContainer>
                              </Tooltip>
                            ) : (
                              <Tooltip
                                tooltip={
                                  <span>
                                    {i18n.unit.groups.serviceNeedChecked}
                                  </span>
                                }
                              >
                                <StatusIconContainer
                                  color={colors.accents.green}
                                >
                                  <FontAwesomeIcon icon={faCheck} inverse />
                                </StatusIconContainer>
                              </Tooltip>
                            )}
                          </Td>
                        ) : null}
                        <Td data-qa="placement-duration">
                          {'type' in placement
                            ? `${placement.startDate.format()}- ${placement.endDate.format()}`
                            : `${placement.period.start.format()}- ${placement.period.end.format()}`}
                        </Td>
                        {canManageChildren ? (
                          <Td align="right">
                            <RowActionContainer>
                              <InlineButton
                                onClick={() => onTransferRequested(placement)}
                                dataQa="transfer-btn"
                                icon={faExchange}
                                text={i18n.unit.groups.transferBtn}
                              />
                              <Gap size="s" horizontal />
                              <InlineButton
                                onClick={() => onDeletePlacement(placement)}
                                dataQa="remove-btn"
                                icon={faUndo}
                                text={i18n.unit.groups.returnBtn}
                              />
                            </RowActionContainer>
                          </Td>
                        ) : null}
                      </Tr>
                    )
                  })}
                </Tbody>
              </Table>
              <GroupNoteLinkContainer>
                <InlineButton
                  icon={getGroupNote(group.id) ? farStickyNote : faStickyNote}
                  text={
                    getGroupNote(group.id)
                      ? i18n.unit.groups.daycareDailyNote
                          .groupNoteModalModifyLink
                      : i18n.unit.groups.daycareDailyNote.groupNoteModalAddLink
                  }
                  onClick={() => {
                    setSelectedDaycareDailyNote({
                      daycareDailyNote: getGroupNote(group.id) ?? null,
                      groupId: group.id,
                      childId: null,
                      childFirstName: '',
                      childLastName: ''
                    })
                    toggleUiMode(`daycare-daily-note-edit-${group.id}`)
                  }}
                  dataQa="btn-create-group-note"
                />
              </GroupNoteLinkContainer>
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
  border: ${colors.greyscale.medium} solid 1px;
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
