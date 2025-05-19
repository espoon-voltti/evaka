// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import concat from 'lodash/concat'
import sortBy from 'lodash/sortBy'
import React, { useContext, useState } from 'react'
import { Link } from 'react-router'
import styled from 'styled-components'

import type { Result } from 'lib-common/api'
import { Loading, wrapResult } from 'lib-common/api'
import FiniteDateRange from 'lib-common/finite-date-range'
import { useBoolean } from 'lib-common/form/hooks'
import type { Action } from 'lib-common/generated/action'
import type { UnitBackupCare } from 'lib-common/generated/api-types/backupcare'
import type {
  Caretakers,
  Daycare
} from 'lib-common/generated/api-types/daycare'
import type { NekkuUnitNumber } from 'lib-common/generated/api-types/nekku'
import type {
  ChildDailyNote,
  NotesByGroupResponse
} from 'lib-common/generated/api-types/note'
import type { OccupancyResponse } from 'lib-common/generated/api-types/occupancy'
import type {
  DaycareId,
  GroupId,
  PersonId
} from 'lib-common/generated/api-types/shared'
import { first, second, useSelectMutation } from 'lib-common/query'
import { capitalizeFirstLetter } from 'lib-common/string'
import type { UUID } from 'lib-common/types'
import { formatPercentage } from 'lib-common/utils/number'
import { useApiState } from 'lib-common/utils/useRestApi'
import PlacementCircle from 'lib-components/atoms/PlacementCircle'
import RoundIcon from 'lib-components/atoms/RoundIcon'
import Tooltip from 'lib-components/atoms/Tooltip'
import { Button } from 'lib-components/atoms/buttons/Button'
import { IconOnlyButton } from 'lib-components/atoms/buttons/IconOnlyButton'
import {
  MutateButton,
  cancelMutation
} from 'lib-components/atoms/buttons/MutateButton'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { H3, Label, Light } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { featureFlags } from 'lib-customizations/employee'
import {
  faAngleDown,
  faAngleUp,
  faCalendarAlt,
  faCheck,
  faExchange,
  faFile,
  faPen,
  farStickyNote,
  faStickyNote,
  faTimes,
  faTrash,
  faUndo
} from 'lib-icons'

import { getNotesByGroup } from '../../../../generated/api-clients/note'
import type { Translations } from '../../../../state/i18n'
import { useTranslation } from '../../../../state/i18n'
import { UIContext } from '../../../../state/ui'
import { UserContext } from '../../../../state/user'
import type {
  DaycareGroupPlacementDetailed,
  DaycareGroupWithPlacements,
  UnitChildrenCapacityFactors
} from '../../../../types/unit'
import { formatPersonName } from '../../../../utils'
import type { UnitFilters } from '../../../../utils/UnitFilters'
import { rangesOverlap } from '../../../../utils/date'
import { isPartDayPlacement } from '../../../../utils/placements'
import { requireRole } from '../../../../utils/roles'
import { renderResult } from '../../../async-rendering'
import { updateBackupCareMutation } from '../../../child-information/queries'
import { AgeIndicatorChip } from '../../../common/AgeIndicatorChip'
import { CareTypeChip } from '../../../common/CareTypeLabel'
import { DataList } from '../../../common/DataList'
import { StatusIconContainer } from '../../../common/StatusIconContainer'
import {
  deleteGroupMutation,
  deleteGroupPlacementMutation
} from '../../queries'
import { CreateChildDocumentsModal } from '../child-documents/CreateChildDocumentsModal'
import NotesModal from '../notes/NotesModal'

import GroupUpdateModal from './group/GroupUpdateModal'

const getNotesByGroupResult = wrapResult(getNotesByGroup)

interface Props {
  unit: Daycare
  filters: UnitFilters
  group: GroupWithDetails
  caretakers?: Caretakers
  confirmedOccupancy?: OccupancyResponse
  realizedOccupancy?: OccupancyResponse
  unitChildrenCapacityFactors: UnitChildrenCapacityFactors[]
  onTransferRequested: (
    placement: DaycareGroupPlacementDetailed | UnitBackupCare
  ) => void
  open: boolean
  toggleOpen: () => void
  permittedActions: Action.Group[]
  permittedBackupCareActions: Partial<Record<UUID, Action.BackupCare[]>>
  permittedGroupPlacementActions: Partial<Record<UUID, Action.Placement[]>>
  nekkuUnits: NekkuUnitNumber[]
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

const ActionsContainer = styled(FixedSpaceRow)`
  margin-top: 16px;
  margin-left: 8px;
`

const RowActionContainer = styled.div`
  display: flex;
`

const ChildCapacityFactorList = styled.div`
  list-style: none;
  white-space: nowrap;
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

type IdAndName<T extends string> = { id: T; name: string }

export default React.memo(function Group({
  unit,
  filters,
  group,
  caretakers,
  confirmedOccupancy,
  realizedOccupancy,
  onTransferRequested,
  open,
  toggleOpen,
  permittedActions,
  permittedBackupCareActions,
  permittedGroupPlacementActions,
  unitChildrenCapacityFactors,
  nekkuUnits
}: Props) {
  const mobileEnabled = unit.enabledPilotFeatures.includes('MOBILE')
  const { i18n } = useTranslation()
  const { roles } = useContext(UserContext)
  const { uiMode, toggleUiMode } = useContext(UIContext)

  const hasNotesPermission = permittedActions.includes('READ_NOTES')
  const [notesResponse, loadNotes] = useApiState(
    async (): Promise<Result<NotesByGroupResponse>> =>
      hasNotesPermission
        ? await getNotesByGroupResult({ groupId: group.id })
        : Loading.of(),
    [hasNotesPermission, group.id]
  )

  const [notesModal, setNotesModal] = useState<{
    child?: IdAndName<PersonId>
    group: IdAndName<GroupId>
  }>()
  const [
    createChildDocumentsModalVisible,
    { on: createChildDocumentsModalOn, off: createChildDocumentsModalOff }
  ] = useBoolean(false)

  const maxOccupancy = getMaxOccupancy(confirmedOccupancy)
  const maxRealizedOccupancy = getMaxOccupancy(realizedOccupancy)
  const headcounts = getChildMinMaxHeadcounts(confirmedOccupancy)

  const placements = group.placements.filter((placement) => {
    if (filters.endDate == null) {
      return !placement.endDate.isBefore(filters.startDate)
    } else {
      return rangesOverlap(placement, filters)
    }
  })

  const sortedPlacements = sortBy(
    concat<DaycareGroupPlacementDetailed | UnitBackupCare>(
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

    if (caretakers.minimum === caretakers.maximum)
      return <span>{caretakers.minimum}</span>
    else
      return (
        <span>
          {caretakers.minimum} - {caretakers.maximum}
        </span>
      )
  }
  const showServiceNeed =
    !unit.type.includes('CLUB') &&
    requireRole(roles, 'ADMIN', 'UNIT_SUPERVISOR')

  const showChildCapacityFactor =
    !unit.type.includes('CLUB') &&
    unitChildrenCapacityFactors &&
    unitChildrenCapacityFactors.length > 0

  const canManageCaretakers =
    permittedActions.includes('UPDATE_CARETAKERS') ||
    permittedActions.includes('CREATE_CARETAKERS') ||
    permittedActions.includes('DELETE_CARETAKERS')

  return (
    <DaycareGroup
      className="daycare-group-collapsible"
      data-qa={`daycare-group-collapsible-${group.id}`}
      data-status={open ? 'open' : 'closed'}
    >
      {uiMode === `update-group-${group.id}` && (
        <GroupUpdateModal group={group} nekkuUnits={nekkuUnits} />
      )}
      {notesModal && (
        <NotesModal
          {...notesModal}
          notesByGroup={notesResponse}
          reload={loadNotes}
          onClose={() => setNotesModal(undefined)}
        />
      )}
      {createChildDocumentsModalVisible && (
        <CreateChildDocumentsModal
          groupId={group.id}
          placements={placements}
          onClose={createChildDocumentsModalOff}
        />
      )}
      <TitleBar>
        <TitleContainer onClick={toggleOpen}>
          <H3 fitted data-qa="group-name">
            {capitalizeFirstLetter(group.name)}
          </H3>
          <Gap size="L" horizontal />
          <IconOnlyButton
            icon={open ? faAngleUp : faAngleDown}
            size="L"
            color="gray"
            data-qa="collapsible-trigger"
            aria-label={open ? i18n.common.close : i18n.common.open}
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
          {permittedActions.includes('UPDATE') && (
            <>
              <Button
                appearance="inline"
                icon={faPen}
                text={i18n.unit.groups.update}
                onClick={() => toggleUiMode(`update-group-${group.id}`)}
                data-qa="btn-update-group"
              />
              <Gap size="s" horizontal />
            </>
          )}
          {permittedActions.includes('DELETE') && (
            <>
              <MutateButton
                appearance="inline"
                icon={faTrash}
                text={i18n.unit.groups.deleteGroup}
                mutation={deleteGroupMutation}
                onClick={() => ({ daycareId: unit.id, groupId: group.id })}
                disabled={sortedPlacements.length > 0 || !group.deletable}
                data-qa="btn-remove-group"
              />
              <Gap size="s" horizontal />
            </>
          )}
          {permittedActions.includes('READ_ABSENCES') && (
            <Link to={`/units/${unit.id}/calendar?group=${group.id}`}>
              <Button
                appearance="inline"
                icon={faCalendarAlt}
                text={i18n.unit.groups.diaryButton}
                onClick={() => undefined}
                data-qa="open-month-calendar-button"
              />
            </Link>
          )}
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
                {canManageCaretakers ? (
                  <Link to={`/units/${unit.id}/groups/${group.id}/caretakers`}>
                    <Button
                      appearance="inline"
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
            {!!maxOccupancy && (
              <div>
                <label>{i18n.unit.groups.maxOccupancy}</label>{' '}
                <span>{maxOccupancy}</span>
              </div>
            )}
            {!!maxRealizedOccupancy && (
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
                    {mobileEnabled &&
                      permittedActions.includes('READ_NOTES') && (
                        <Th>
                          <IconContainer>
                            <Tooltip
                              position="top"
                              tooltip={
                                <span>
                                  {i18n.unit.groups.daycareDailyNote.dailyNote}
                                </span>
                              }
                            >
                              <FontAwesomeIcon
                                icon={faStickyNote}
                                color={colors.grayscale.g70}
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
                    {showChildCapacityFactor && (
                      <Th data-qa="child-capacity-factor-heading">
                        {i18n.unit.groups.factor}
                      </Th>
                    )}
                    <Th>{i18n.unit.groups.placementDuration}</Th>
                    <Th />
                  </Tr>
                </Thead>
                <Tbody>
                  {sortedPlacements.map((placement) => (
                    <GroupPlacementRow
                      key={placement.id}
                      placement={placement}
                      unitId={unit.id}
                      filters={filters}
                      mobileEnabled={mobileEnabled}
                      showServiceNeed={showServiceNeed}
                      showChildCapacityFactor={showChildCapacityFactor}
                      notesResponse={notesResponse}
                      permittedActions={permittedActions}
                      permittedGroupPlacementActions={
                        permittedGroupPlacementActions
                      }
                      permittedBackupCareActions={permittedBackupCareActions}
                      unitChildrenCapacityFactors={unitChildrenCapacityFactors}
                      onShowNote={(child) =>
                        setNotesModal({
                          child,
                          group: { id: group.id, name: group.name }
                        })
                      }
                      onTransferRequested={onTransferRequested}
                    />
                  ))}
                </Tbody>
              </Table>
              <ActionsContainer>
                {mobileEnabled &&
                  permittedActions.includes('READ_NOTES') &&
                  notesResponse.isSuccess && (
                    <Button
                      appearance="inline"
                      icon={
                        notesResponse.value.groupNotes.length > 0
                          ? farStickyNote
                          : faStickyNote
                      }
                      text={
                        i18n.unit.groups.daycareDailyNote.groupNoteModalLink
                      }
                      onClick={() =>
                        setNotesModal({
                          group: { id: group.id, name: group.name }
                        })
                      }
                      data-qa="btn-create-group-note"
                    />
                  )}
                {featureFlags.citizenChildDocumentTypes &&
                  permittedActions.includes('CREATE_CHILD_DOCUMENTS') && (
                    <Button
                      appearance="inline"
                      icon={faFile}
                      text={i18n.unit.groups.childDocuments.createModalLink}
                      onClick={createChildDocumentsModalOn}
                      data-qa="btn-create-child-documents"
                    />
                  )}
              </ActionsContainer>
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
})

interface GroupPlacementRowProps {
  placement: DaycareGroupPlacementDetailed | UnitBackupCare
  unitId: DaycareId
  filters: UnitFilters
  mobileEnabled: boolean
  showServiceNeed: boolean
  showChildCapacityFactor: boolean
  notesResponse: Result<NotesByGroupResponse>
  permittedActions: Action.Group[]
  permittedGroupPlacementActions: Partial<Record<UUID, Action.Placement[]>>
  permittedBackupCareActions: Partial<Record<UUID, Action.BackupCare[]>>
  unitChildrenCapacityFactors: UnitChildrenCapacityFactors[]
  onShowNote: (child?: IdAndName<PersonId>) => void
  onTransferRequested: (
    placement: DaycareGroupPlacementDetailed | UnitBackupCare
  ) => void
}

const GroupPlacementRow = React.memo(function GroupPlacementRow({
  placement,
  unitId,
  filters,
  mobileEnabled,
  showServiceNeed,
  showChildCapacityFactor,
  notesResponse,
  permittedActions,
  permittedGroupPlacementActions,
  permittedBackupCareActions,
  unitChildrenCapacityFactors,
  onShowNote,
  onTransferRequested
}: GroupPlacementRowProps) {
  const { i18n } = useTranslation()

  const missingServiceNeedDays =
    'type' in placement
      ? placement.daycarePlacementMissingServiceNeedDays
      : placement.missingServiceNeedDays
  const canTransfer = !!(
    placement.id &&
    ('type' in placement
      ? permittedGroupPlacementActions[placement.id]?.includes('UPDATE')
      : permittedBackupCareActions[placement.id]?.includes('UPDATE'))
  )
  const canDelete = !!(
    placement.id &&
    ('type' in placement
      ? permittedGroupPlacementActions[placement.id]?.includes('DELETE')
      : permittedBackupCareActions[placement.id]?.includes('DELETE'))
  )

  const dateOfBirth =
    'type' in placement
      ? placement.child.dateOfBirth
      : placement.child.birthDate

  const { assistanceNeedFactor, serviceNeedFactor } =
    unitChildrenCapacityFactors.find(
      (item) => item.childId === placement.child.id
    ) ?? {}

  const multipliedCapacityFactor =
    serviceNeedFactor && assistanceNeedFactor
      ? serviceNeedFactor * assistanceNeedFactor
      : undefined

  const [deleteMutation, onClick] = useSelectMutation(
    () => ('type' in placement ? first(placement) : second(placement)),
    [
      deleteGroupPlacementMutation,
      (placement) =>
        placement.id
          ? { unitId, groupPlacementId: placement.id }
          : cancelMutation
    ],
    [
      updateBackupCareMutation,
      (placement) => ({
        id: placement.id,
        unitId,
        body: {
          period: placement.period,
          groupId: null
        }
      })
    ]
  )

  return (
    <Tr
      key={placement.id || ''}
      className="group-placement-row"
      data-qa={`group-placement-row-${placement.child.id}`}
    >
      {mobileEnabled && permittedActions.includes('READ_NOTES') && (
        <Td data-qa="daily-note">
          <DailyNote
            placement={placement}
            notesResponse={notesResponse}
            onOpen={onShowNote}
          />
        </Td>
      )}
      <Td data-qa="child-name">
        <Link to={`/child-information/${placement.child.id}`}>
          {formatPersonName(placement.child, i18n, true)}
        </Link>
      </Td>
      <Td>
        <FixedSpaceRow spacing="xs" alignItems="center">
          <AgeIndicatorChip
            age={filters.startDate.differenceInYears(dateOfBirth)}
          />
          <span data-qa="child-dob">{dateOfBirth.format()}</span>
        </FixedSpaceRow>
      </Td>
      <Td data-qa="placement-type">
        <FixedSpaceColumn spacing="xs" alignItems="flex-start">
          <CareTypeChip
            type={'type' in placement ? placement.type : 'backup-care'}
          />
          {'fromUnits' in placement &&
            placement.fromUnits.map((unit) => <Light key={unit}>{unit}</Light>)}
        </FixedSpaceColumn>
      </Td>
      <Td data-qa="placement-subtype">
        {'type' in placement ? (
          <PlacementCircle
            type={isPartDayPlacement(placement.type) ? 'half' : 'full'}
            label={
              <ServiceNeedTooltipLabel
                placement={placement}
                filters={filters}
              />
            }
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
              <StatusIconContainer color={colors.status.warning}>
                <FontAwesomeIcon icon={faTimes} inverse />
              </StatusIconContainer>
            </Tooltip>
          ) : (
            <Tooltip
              tooltip={<span>{i18n.unit.groups.serviceNeedChecked}</span>}
            >
              <StatusIconContainer color={colors.status.success}>
                <FontAwesomeIcon icon={faCheck} inverse />
              </StatusIconContainer>
            </Tooltip>
          )}
        </Td>
      ) : null}
      {showChildCapacityFactor && (
        <Td data-qa="child-capacity-factor-column">
          <Tooltip
            tooltip={
              <ChildCapacityFactorList>
                <li>
                  {i18n.unit.groups.childServiceNeedFactor}:{' '}
                  {serviceNeedFactor === 1 ? '—' : serviceNeedFactor}
                </li>
                <li>
                  {i18n.unit.groups.childAssistanceNeedFactor}:{' '}
                  {assistanceNeedFactor === 1 ? '—' : assistanceNeedFactor}
                </li>
              </ChildCapacityFactorList>
            }
          >
            <span data-qa={`child-capacity-factor-${placement.child.id}`}>
              {multipliedCapacityFactor
                ? multipliedCapacityFactor === 1
                  ? '—'
                  : multipliedCapacityFactor.toFixed(2)
                : ''}
            </span>
          </Tooltip>
        </Td>
      )}
      <Td data-qa="placement-duration">
        {'type' in placement
          ? `${placement.startDate.format()}- ${placement.endDate.format()}`
          : `${placement.period.start.format()}- ${placement.period.end.format()}`}
      </Td>
      {canTransfer || canDelete ? (
        <Td align="right">
          <RowActionContainer>
            {canTransfer && (
              <>
                <Button
                  appearance="inline"
                  onClick={() => onTransferRequested(placement)}
                  data-qa="transfer-btn"
                  icon={faExchange}
                  text={i18n.unit.groups.transferBtn}
                />
                <Gap size="s" horizontal />
              </>
            )}
            {canDelete && (
              <MutateButton
                appearance="inline"
                mutation={deleteMutation}
                onClick={onClick}
                data-qa="remove-btn"
                icon={faUndo}
                text={i18n.unit.groups.returnBtn}
              />
            )}
          </RowActionContainer>
        </Td>
      ) : null}
    </Tr>
  )
})

interface DaycareDailyNoteProps {
  placement: DaycareGroupPlacementDetailed | UnitBackupCare
  notesResponse: Result<NotesByGroupResponse>
  onOpen: (child: IdAndName<PersonId>) => void
}

const DailyNote = React.memo(function DaycareDailyNote({
  placement,
  notesResponse,
  onOpen
}: DaycareDailyNoteProps) {
  const { i18n } = useTranslation()

  return renderResult(notesResponse, (notes) => {
    const childNote = notes.childDailyNotes.find(
      (note) => note.childId === placement.child.id
    )
    return (
      <Tooltip
        data-qa={`daycare-daily-note-hover-${placement.child.id}`}
        position="top"
        tooltip={
          childNote ? (
            <div>
              <h4>{i18n.unit.groups.daycareDailyNote.header}</h4>
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
              <p>{formatSleepingTooltipText(childNote, i18n)}</p>
              <h5>{i18n.unit.groups.daycareDailyNote.reminderHeader}</h5>
              <p>
                {childNote.reminders
                  .map(
                    (reminder) =>
                      i18n.unit.groups.daycareDailyNote.reminderType[reminder]
                  )
                  .join(',')}
              </p>
              <h5>
                {i18n.unit.groups.daycareDailyNote.otherThingsToRememberHeader}
              </h5>
              <p>{childNote.reminderNote}</p>
              {notes.childStickyNotes.length > 0 && (
                <>
                  <h5>{i18n.unit.groups.daycareDailyNote.stickyNotesHeader}</h5>
                  {notes.childStickyNotes.map((stickyNote) => (
                    <p key={stickyNote.id}>{stickyNote.note}</p>
                  ))}
                </>
              )}
              {notes.groupNotes.length > 0 && (
                <>
                  <h5>{i18n.unit.groups.daycareDailyNote.groupNotesHeader}</h5>
                  {notes.groupNotes.map((groupNote) => (
                    <p key={groupNote.id}>{groupNote.note}</p>
                  ))}
                </>
              )}
            </div>
          ) : (
            <span>{i18n.unit.groups.daycareDailyNote.edit}</span>
          )
        }
      >
        <RoundIcon
          active={childNote != null || notes.childStickyNotes.length > 0}
          data-qa={`daycare-daily-note-icon-${placement.child.id}`}
          content={faStickyNote}
          color={colors.main.m2}
          size="m"
          onClick={() =>
            onOpen({
              id: placement.child.id,
              name: formatPersonName(placement.child, i18n)
            })
          }
        />
      </Tooltip>
    )
  })
})

const formatSleepingTooltipText = (
  childNote: ChildDailyNote,
  i18n: Translations
) =>
  [
    childNote.sleepingNote
      ? i18n.unit.groups.daycareDailyNote.level[childNote.sleepingNote]
      : null,
    childNote.sleepingMinutes
      ? Number(childNote.sleepingMinutes) / 60 >= 1
        ? `${Math.floor(Number(childNote.sleepingMinutes) / 60)}h ${Math.round(
            Number(childNote.sleepingMinutes) % 60
          )}min`
        : `${Math.floor(Number(childNote.sleepingMinutes) / 60)}min`
      : null
  ]
    .filter((s) => s !== null)
    .join(',')

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
  border: ${colors.grayscale.g35} solid 1px;
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

const ServiceNeedTooltipLabel = ({
  placement,
  filters
}: {
  placement: DaycareGroupPlacementDetailed
  filters: UnitFilters
}) => {
  const placementRange = new FiniteDateRange(
    placement.startDate,
    placement.endDate
  )
  const filterRange = new FiniteDateRange(filters.startDate, filters.endDate)
  const serviceNeeds = placement.serviceNeeds.reduce<
    { range: FiniteDateRange; nameFi: string }[]
  >((prev, sn) => {
    const snRange = new FiniteDateRange(sn.startDate, sn.endDate)
    return snRange.overlaps(placementRange) && snRange.overlaps(filterRange)
      ? [...prev, { range: snRange, nameFi: sn.option.nameFi }]
      : prev
  }, [])
  const serviceNeedGaps = placementRange
    .getGaps(serviceNeeds.map((sn) => sn.range))
    .filter((gap) => gap.overlaps(filterRange))
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
            key={`service-need-${sn.range.start.formatIso()}`}
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
