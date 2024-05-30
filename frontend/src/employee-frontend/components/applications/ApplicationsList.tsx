// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React, { useContext, useState } from 'react'
import styled from 'styled-components'

import { wrapResult } from 'lib-common/api'
import { formatDate } from 'lib-common/date'
import {
  ApplicationSortColumn,
  ApplicationSummary,
  PagedApplicationSummaries
} from 'lib-common/generated/api-types/application'
import { UUID } from 'lib-common/types'
import Pagination from 'lib-components/Pagination'
import PlacementCircle from 'lib-components/atoms/PlacementCircle'
import RoundIcon from 'lib-components/atoms/RoundIcon'
import Tooltip from 'lib-components/atoms/Tooltip'
import { IconOnlyButton } from 'lib-components/atoms/buttons/IconOnlyButton'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import TextArea from 'lib-components/atoms/form/TextArea'
import {
  SortableTh,
  Table,
  Tbody,
  Td,
  Th,
  Thead,
  Tr
} from 'lib-components/layout/Table'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { AsyncFormModal } from 'lib-components/molecules/modals/FormModal'
import { Bold, H1, Italic, Light } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import colors, { applicationBasisColors } from 'lib-customizations/common'
import {
  faCheck,
  faCommentAlt,
  faPaperclip,
  fasCommentAltLines,
  faTimes,
  faTrash
} from 'lib-icons'

import ActionBar from '../../components/applications/ActionBar'
import ApplicationActions from '../../components/applications/ApplicationActions'
import { getEmployeeUrlPrefix } from '../../constants'
import { updateServiceWorkerNote } from '../../generated/api-clients/application'
import { ApplicationUIContext } from '../../state/application-ui'
import { useTranslation } from '../../state/i18n'
import { UserContext } from '../../state/user'
import { SearchOrder } from '../../types'
import { formatName } from '../../utils'
import { isPartDayPlacement } from '../../utils/placements'
import { hasRole, RequireRole } from '../../utils/roles'
import { AgeIndicatorChip } from '../common/AgeIndicatorChip'
import { CareTypeChip } from '../common/CareTypeLabel'

import { CircleIconGreen, CircleIconRed } from './CircleIcon'

const updateServiceWorkerNoteResult = wrapResult(updateServiceWorkerNote)

const TitleRowContainer = styled.div`
  display: flex;
  justify-content: space-between;
  padding: ${defaultMargins.m} ${defaultMargins.L};
  position: sticky;
  top: 0;
  z-index: 3;
  background: ${colors.grayscale.g0};
`

interface PaginationWrapperProps {
  paddingVertical?: string
  paddingHorizontal?: string
}

const PaginationWrapper = styled.div<PaginationWrapperProps>`
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  justify-content: center;
  padding: ${(p) =>
    p.paddingVertical && p.paddingHorizontal
      ? `${p.paddingVertical} ${p.paddingHorizontal}`
      : '0px'};
`

const StatusColorTd = styled(Td)<{ color: string }>`
  border-left: solid 7px ${(p) => p.color};
`

const SortableThWithBorder = styled(SortableTh)`
  border-left: solid 7px white;
`

const ApplicationsTableContainer = styled.div`
  table {
    width: auto;
  }

  @media screen and (min-width: 1024px) {
    table {
      max-width: 960px;
    }

    th,
    td {
      padding: ${defaultMargins.s} ${defaultMargins.xs};
    }
    th:last-of-type,
    td:last-of-type {
      padding: ${defaultMargins.s};
    }
  }
  @media screen and (max-width: 1215px) {
    table {
      max-width: 1152px;
    }

    th,
    td {
      padding: ${defaultMargins.s} ${defaultMargins.xs};
    }
    th:last-of-type,
    td:last-of-type {
      padding: ${defaultMargins.s};
    }
  }
  @media screen and (max-width: 1407px) {
    table {
      max-width: 1344px;
    }
  }
  @media screen and (min-width: 1216px) {
    table {
      max-width: 1152px;
    }

    th,
    td {
      padding: ${defaultMargins.s} ${defaultMargins.xs};
    }
    th:last-of-type,
    td:last-of-type {
      padding: ${defaultMargins.s};
    }
  }
  @media screen and (min-width: 1408px) {
    table {
      max-width: 1344px;
    }
  }
`

interface Props {
  applicationsResult: PagedApplicationSummaries
  currentPage: number
  setPage: (page: number) => void
  sortBy: ApplicationSortColumn
  setSortBy: (v: ApplicationSortColumn) => void
  sortDirection: SearchOrder
  setSortDirection: (v: SearchOrder) => void
  reloadApplications: () => void
}

const ApplicationsList = React.memo(function Applications({
  applicationsResult,
  currentPage,
  setPage,
  sortBy,
  setSortBy,
  sortDirection,
  setSortDirection,
  reloadApplications
}: Props) {
  const { data: applications, pages, total } = applicationsResult

  const { i18n } = useTranslation()
  const {
    showCheckboxes,
    checkedIds,
    setCheckedIds,
    applicationSearchFilters
  } = useContext(ApplicationUIContext)

  const { roles } = useContext(UserContext)
  const enableApplicationActions =
    hasRole(roles, 'SERVICE_WORKER') ||
    hasRole(roles, 'FINANCE_ADMIN') ||
    hasRole(roles, 'ADMIN')

  const [editedNote, setEditedNote] = useState<UUID | null>(null)
  const [editedNoteText, setEditedNoteText] = useState<string>('')

  const isSorted = (column: ApplicationSortColumn) =>
    sortBy === column ? sortDirection : undefined

  const toggleSort = (column: ApplicationSortColumn) => () => {
    if (sortBy === column) {
      setSortDirection(sortDirection === 'ASC' ? 'DESC' : 'ASC')
    } else {
      setSortBy(column)
      setSortDirection('ASC')
    }
  }

  const toggleCheckAll = (checked: boolean) => {
    if (checked) {
      setCheckedIds(applications.map((application) => application.id))
    } else {
      setCheckedIds([])
    }
  }

  const isAllChecked = (): boolean => {
    const applicationIdsSorted = applications.map((a) => a.id).sort()
    const checkedIdsSorted = checkedIds.sort()
    if (
      applicationIdsSorted.length === 0 ||
      checkedIdsSorted.length !== applicationIdsSorted.length
    )
      return false

    for (let i = 0; i < applicationIdsSorted.length; i++) {
      if (applicationIdsSorted[i] !== checkedIdsSorted[i]) return false
    }

    return true
  }

  const getAccentColor = (application: ApplicationSummary) => {
    if (
      application.status === 'WAITING_PLACEMENT' &&
      !application.checkedByAdmin
    )
      return colors.status.warning
    if (
      application.status === 'WAITING_UNIT_CONFIRMATION' &&
      application.placementProposalStatus?.unitConfirmationStatus === 'ACCEPTED'
    )
      return colors.status.success
    if (
      application.status === 'WAITING_UNIT_CONFIRMATION' &&
      application.placementProposalStatus?.unitConfirmationStatus === 'REJECTED'
    )
      return colors.status.danger

    return colors.main.m3
  }

  const dateOfBirthInfo = (application: ApplicationSummary) => {
    const startDateOrDueDate =
      application.startDate && application.dueDate
        ? application.startDate.isAfter(application.dueDate)
          ? application.startDate
          : application.dueDate
        : application.startDate
          ? application.startDate
          : application.dueDate
            ? application.dueDate
            : null

    return (
      application.dateOfBirth &&
      startDateOrDueDate && (
        <FixedSpaceRow spacing="xs" alignItems="center">
          <AgeIndicatorChip
            age={startDateOrDueDate.differenceInYears(application.dateOfBirth)}
          />
          <span>
            {application.socialSecurityNumber ||
              formatDate(application.dateOfBirth.toSystemTzDate())}
          </span>
        </FixedSpaceRow>
      )
    )
  }

  const rows = applications.map((application) => (
    <Tr
      key={application.id}
      data-qa="table-application-row"
      data-application-id={application.id}
      onClick={() =>
        window.open(
          `${getEmployeeUrlPrefix()}/employee/applications/${application.id}`,
          '_blank'
        )
      }
    >
      <StatusColorTd color={getAccentColor(application)}>
        <FixedSpaceColumn spacing="xs" alignItems="flex-start">
          <CareTypeChip type={application.placementType} />
          {application.transferApplication && (
            <Light>{i18n.applications.list.transfer}</Light>
          )}
          {application.origin === 'PAPER' && (
            <Light>{i18n.applications.list.paper}</Light>
          )}
        </FixedSpaceColumn>
      </StatusColorTd>
      <Td>
        <PlacementCircle
          type={isPartDayPlacement(application.placementType) ? 'half' : 'full'}
          label={
            application.serviceNeed !== null
              ? application.serviceNeed.nameFi
              : i18n.placement.type[application.placementType]
          }
        />
      </Td>
      <Td>
        <FixedSpaceColumn spacing="xs">
          <Bold>
            {formatName(
              application.firstName,
              application.lastName,
              i18n,
              true
            )}
          </Bold>
          {dateOfBirthInfo(application)}
        </FixedSpaceColumn>
      </Td>
      <Td>
        <Bold>{application.dueDate?.format()}</Bold>
      </Td>
      <Td data-qa="start-date">
        {application.placementPlanStartDate?.format() ??
          application.startDate?.format() ??
          '-'}
      </Td>
      <Td>
        <FixedSpaceRow spacing="xs">
          {application.additionalInfo && (
            <RoundIcon
              content="L"
              color={applicationBasisColors.ADDITIONAL_INFO}
              size="s"
            />
          )}
          {application.siblingBasis && (
            <RoundIcon
              content="S"
              color={applicationBasisColors.SIBLING_BASIS}
              size="s"
            />
          )}
          {application.assistanceNeed && (
            <RoundIcon
              content="T"
              color={applicationBasisColors.ASSISTANCE_NEED}
              size="s"
            />
          )}
          {application.wasOnClubCare && (
            <RoundIcon
              content="K"
              color={applicationBasisColors.CLUB_CARE}
              size="s"
            />
          )}
          {application.wasOnDaycare && (
            <RoundIcon
              content="P"
              color={applicationBasisColors.DAYCARE}
              size="s"
            />
          )}
          {application.extendedCare && (
            <RoundIcon
              content="V"
              color={applicationBasisColors.EXTENDED_CARE}
              size="s"
            />
          )}
          {application.duplicateApplication && (
            <RoundIcon
              content="2"
              color={applicationBasisColors.DUPLICATE_APPLICATION}
              size="s"
            />
          )}
          {application.urgent && (
            <RoundIcon
              content="!"
              color={applicationBasisColors.URGENT}
              size="s"
            />
          )}
          {(application.urgent || application.extendedCare) &&
            application.attachmentCount > 0 && (
              <RoundIcon
                content={faPaperclip}
                color={applicationBasisColors.HAS_ATTACHMENTS}
                size="s"
              />
            )}
        </FixedSpaceRow>
      </Td>
      <Td>
        <span>
          <Tooltip
            tooltip={
              <div>
                {application.preferredUnits.map((unit, i) => (
                  <p key={`unit-pref-${i}`}>{unit.name}</p>
                ))}
                {application.currentPlacementUnit && (
                  <Italic>
                    {i18n.applications.list.currentUnit}{' '}
                    {application.currentPlacementUnit.name}
                  </Italic>
                )}
              </div>
            }
          >
            {application.placementPlanUnitName ||
              application.preferredUnits[0]?.name}
          </Tooltip>
        </span>
      </Td>
      <Td data-qa="application-status">
        <FixedSpaceRow>
          <Bold>{i18n.application.statuses[application.status]}</Bold>
          {application.placementProposalStatus?.unitConfirmationStatus ===
            'ACCEPTED' && (
            <CircleIconGreen>
              <FontAwesomeIcon icon={faCheck} />
            </CircleIconGreen>
          )}
          {application.placementProposalStatus?.unitConfirmationStatus ===
            'REJECTED' && (
            <div data-qa="placement-proposal-status">
              <Tooltip
                tooltip={
                  <p data-qa="placement-proposal-status-tooltip">
                    {application.placementProposalStatus
                      ? application.placementProposalStatus.unitRejectReason ===
                        'OTHER'
                        ? application.placementProposalStatus
                            .unitRejectOtherReason || ''
                        : i18n.unit.placementProposals.rejectReasons[
                            application.placementProposalStatus
                              .unitRejectReason || 'OTHER'
                          ]
                      : ''}
                  </p>
                }
              >
                <CircleIconRed>
                  <FontAwesomeIcon icon={faTimes} />
                </CircleIconRed>
              </Tooltip>
            </div>
          )}
        </FixedSpaceRow>
      </Td>

      <RequireRole oneOf={['SERVICE_WORKER']}>
        <Td>
          <Tooltip
            tooltip={
              application.serviceWorkerNote ? (
                <span>{application.serviceWorkerNote}</span>
              ) : (
                <i>{i18n.applications.list.addNote}</i>
              )
            }
          >
            <IconOnlyButton
              icon={
                application.serviceWorkerNote
                  ? fasCommentAltLines
                  : faCommentAlt
              }
              onClick={(e) => {
                e.stopPropagation()
                setEditedNoteText(application.serviceWorkerNote)
                setEditedNote(application.id)
              }}
              aria-label={
                application.serviceWorkerNote
                  ? i18n.common.edit
                  : i18n.applications.list.addNote
              }
            />
          </Tooltip>
        </Td>
      </RequireRole>
      <Td>
        {enableApplicationActions && (
          <ApplicationActions
            application={application}
            reloadApplications={reloadApplications}
          />
        )}
      </Td>
    </Tr>
  ))

  return (
    <div data-qa="applications-list">
      <TitleRowContainer>
        <H1 fitted noMargin>
          {applicationSearchFilters.status === 'ALL'
            ? i18n.applications.list.title
            : i18n.application.statuses[applicationSearchFilters.status]}
        </H1>
        <PaginationWrapper>
          <div>
            {total > 0
              ? `${i18n.applications.list.resultCount}: ${total}`
              : i18n.applications.list.noResults}
          </div>
          <Pagination
            pages={pages}
            currentPage={currentPage}
            setPage={setPage}
            label={i18n.common.page}
          />
        </PaginationWrapper>
      </TitleRowContainer>

      <ApplicationsTableContainer>
        <Table data-qa="table-of-applications">
          <Thead sticky="90px">
            <Tr>
              <SortableThWithBorder
                sorted={isSorted('APPLICATION_TYPE')}
                onClick={toggleSort('APPLICATION_TYPE')}
              >
                {i18n.applications.list.type}
              </SortableThWithBorder>
              <Th>{i18n.applications.list.subtype}</Th>
              <SortableTh
                sorted={isSorted('CHILD_NAME')}
                onClick={toggleSort('CHILD_NAME')}
              >
                {i18n.applications.list.name}
              </SortableTh>
              <SortableTh
                sorted={isSorted('DUE_DATE')}
                onClick={toggleSort('DUE_DATE')}
              >
                {i18n.applications.list.dueDate}
              </SortableTh>
              <SortableTh
                sorted={isSorted('START_DATE')}
                onClick={toggleSort('START_DATE')}
              >
                {i18n.applications.list.startDate}
              </SortableTh>
              <Th>{i18n.applications.list.basis}</Th>
              <SortableTh
                sorted={isSorted('UNIT_NAME')}
                onClick={toggleSort('UNIT_NAME')}
              >
                {i18n.applications.list.unit}
              </SortableTh>
              <SortableTh
                sorted={isSorted('STATUS')}
                onClick={toggleSort('STATUS')}
              >
                {i18n.applications.list.status}
              </SortableTh>
              <RequireRole oneOf={['SERVICE_WORKER']}>
                <Th>{i18n.applications.list.note}</Th>
              </RequireRole>
              <Th>
                {showCheckboxes ? (
                  <CheckAllContainer>
                    <Checkbox
                      checked={isAllChecked()}
                      label="hidden"
                      hiddenLabel={true}
                      data-qa="toggle-check-all-checkbox"
                      onChange={(checked) => toggleCheckAll(checked)}
                    />
                  </CheckAllContainer>
                ) : null}
              </Th>
            </Tr>
          </Thead>
          <Tbody>{rows}</Tbody>
        </Table>
        <ActionBar reloadApplications={reloadApplications} fullWidth />
      </ApplicationsTableContainer>

      {!!editedNote && (
        <AsyncFormModal
          title={i18n.applications.list.serviceWorkerNote}
          resolveAction={() =>
            updateServiceWorkerNoteResult({
              applicationId: editedNote,
              body: { text: editedNoteText }
            })
          }
          resolveLabel={i18n.common.save}
          onSuccess={() => {
            setEditedNote(null)
            reloadApplications()
          }}
          rejectAction={() => setEditedNote(null)}
          rejectLabel={i18n.common.cancel}
        >
          <AlignRight>
            <InlineButton
              onClick={() => setEditedNoteText('')}
              text={i18n.common.clear}
              icon={faTrash}
              disabled={!editedNoteText}
            />
          </AlignRight>
          <Gap />
          <TextArea value={editedNoteText} onChange={setEditedNoteText} />
        </AsyncFormModal>
      )}
    </div>
  )
})

export default ApplicationsList

const CheckAllContainer = styled.div`
  display: flex;
  justify-content: flex-end;
`

const AlignRight = styled.div`
  display: flex;
  justify-content: flex-end;
  width: 100%;
`
