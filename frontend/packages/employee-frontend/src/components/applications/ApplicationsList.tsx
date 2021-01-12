// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext } from 'react'
import styled from 'styled-components'
import { faCheck, fasArrowDown, fasArrowUp, faTimes } from '@evaka/lib-icons'
import {
  Table,
  Tr,
  Th,
  Td,
  Thead,
  Tbody,
  SortableTh
} from '@evaka/lib-components/src/layout/Table'
import { useTranslation } from 'state/i18n'
import {
  ApplicationListSummary,
  ApplicationsSearchResponse
} from 'types/application'
import { SearchOrder } from '~types'
import Pagination from '@evaka/lib-components/src/Pagination'
import colors, { primaryColors } from '@evaka/lib-components/src/colors'
import { SortByApplications } from '~types/application'
import { formatName } from '~utils'
import RoundIcon from '@evaka/lib-components/src/atoms/RoundIcon'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from '@evaka/lib-components/src/layout/flex-helpers'
import { H1 } from '@evaka/lib-components/src/typography'
import { defaultMargins } from '@evaka/lib-components/src/white-space'
import { getEmployeeUrlPrefix } from '~constants'
import { formatDate } from '~utils/date'
import ApplicationActions from '~components/applications/ApplicationActions'
import Checkbox from '@evaka/lib-components/src/atoms/form/Checkbox'
import { ApplicationUIContext } from '~state/application-ui'
import ActionBar from '~components/applications/ActionBar'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import Tooltip from '@evaka/lib-components/src/atoms/Tooltip'
import { careTypesFromPlacementType } from '~components/common/CareTypeLabel'
import PlacementCircle from '@evaka/lib-components/src/atoms/PlacementCircle'
import { UserContext } from '~state/user'
import { hasRole } from '~utils/roles'
import { isPartDayPlacement } from '~utils/placements'

const CircleIcon = styled.div`
  display: flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  min-width: 34px;
  min-height: 34px;
  font-size: 24px !important;
  border-radius: 100%;
  color: ${colors.greyscale.white};
`

const CircleIconGreen = styled(CircleIcon)`
  background-color: ${colors.accents.green};
`

const CircleIconRed = styled(CircleIcon)`
  background-color: ${colors.accents.red};
`

const TitleRowContainer = styled.div`
  display: flex;
  justify-content: space-between;
  padding: ${defaultMargins.m} ${defaultMargins.L};
  position: sticky;
  top: 0;
  z-index: 3;
  background: ${colors.greyscale.white};
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

const Bold = styled.span`
  font-weight: 600;
`

const Light = styled.span`
  font-style: italic;
  color: ${colors.greyscale.medium};
`

const StatusColorTd = styled(Td)<{ color: string }>`
  border-left-color: ${(p) => p.color};
  border-left-width: 7px;
`

interface Props {
  applicationsResult: ApplicationsSearchResponse
  currentPage: number
  setPage: (page: number) => void
  sortBy: SortByApplications
  setSortBy: (v: SortByApplications) => void
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
  const { data: applications, pages, totalCount } = applicationsResult

  const { i18n } = useTranslation()
  const { showCheckboxes, checkedIds, setCheckedIds, status } = useContext(
    ApplicationUIContext
  )

  const { roles } = useContext(UserContext)
  const enableApplicationActions =
    hasRole(roles, 'SERVICE_WORKER') ||
    hasRole(roles, 'FINANCE_ADMIN') ||
    hasRole(roles, 'ADMIN')

  const isSorted = (column: SortByApplications) =>
    sortBy === column ? sortDirection : undefined

  const toggleSort = (column: SortByApplications) => () => {
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

  const getAccentColor = (application: ApplicationListSummary) => {
    if (
      application.status === 'WAITING_PLACEMENT' &&
      !application.checkedByAdmin
    )
      return colors.accents.orange
    if (
      application.status === 'WAITING_UNIT_CONFIRMATION' &&
      application.placementProposalStatus?.unitConfirmationStatus === 'ACCEPTED'
    )
      return colors.accents.green
    if (
      application.status === 'WAITING_UNIT_CONFIRMATION' &&
      application.placementProposalStatus?.unitConfirmationStatus === 'REJECTED'
    )
      return colors.accents.red

    return colors.accents.water
  }

  const dateOfBirthInfo = (application: ApplicationListSummary) => {
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
        <FixedSpaceRow spacing="xs">
          <Tooltip
            tooltip={
              <span>
                {startDateOrDueDate.differenceInYears(application.dateOfBirth) <
                3
                  ? i18n.applications.list.lessthan3
                  : i18n.applications.list.morethan3}
              </span>
            }
          >
            <RoundIcon
              content={
                startDateOrDueDate.differenceInYears(application.dateOfBirth) <
                3
                  ? fasArrowDown
                  : fasArrowUp
              }
              color={
                startDateOrDueDate.differenceInYears(application.dateOfBirth) <
                3
                  ? colors.accents.green
                  : colors.primaryColors.medium
              }
              size="s"
            />
          </Tooltip>
          <span>
            {application.socialSecurityNumber ||
              formatDate(application.dateOfBirth.toSystemTzDate())}
          </span>
        </FixedSpaceRow>
      )
    )
  }

  // todo: check new logic for status color border
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
        <FixedSpaceColumn spacing="xs">
          {careTypesFromPlacementType(application.placementType)}
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
          label={i18n.placement.type[application.placementType]}
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
      <Td>{application.startDate?.format() ?? '-'}</Td>
      <Td>
        <FixedSpaceRow spacing="xs">
          {application.additionalInfo && (
            <RoundIcon content="L" color={primaryColors.dark} size="s" />
          )}
          {application.siblingBasis && (
            <RoundIcon content="S" color={colors.accents.green} size="s" />
          )}
          {application.assistanceNeed && (
            <RoundIcon content="T" color={colors.accents.water} size="s" />
          )}
          {application.wasOnClubCare && (
            <RoundIcon content="K" color={colors.accents.red} size="s" />
          )}
          {application.wasOnDaycare && (
            <RoundIcon content="P" color={colors.accents.orange} size="s" />
          )}
          {application.extendedCare && (
            <RoundIcon content="V" color={colors.accents.petrol} size="s" />
          )}
          {application.duplicateApplication && (
            <RoundIcon content="2" color={colors.accents.emerald} size="s" />
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
              </div>
            }
          >
            {application.placementProposalUnitName ||
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
            <div>
              <Tooltip
                tooltip={
                  <p>
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
        <H1 fitted>
          {status === 'ALL'
            ? i18n.applications.list.title
            : i18n.application.statuses[status]}
        </H1>
        <PaginationWrapper>
          <div>
            {totalCount > 0
              ? `${i18n.applications.list.resultCount}: ${totalCount}`
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

      <div>
        <Table data-qa="table-of-applications">
          <Thead>
            <Tr>
              <SortableTh
                sticky
                top={'106px'}
                sorted={isSorted('APPLICATION_TYPE')}
                onClick={toggleSort('APPLICATION_TYPE')}
              >
                {i18n.applications.list.type}
              </SortableTh>
              <Th sticky top={'106px'}>
                {i18n.applications.list.subtype}
              </Th>
              <SortableTh
                sticky
                top={'106px'}
                sorted={isSorted('CHILD_NAME')}
                onClick={toggleSort('CHILD_NAME')}
              >
                {i18n.applications.list.name}
              </SortableTh>
              <SortableTh
                sticky
                top={'106px'}
                sorted={isSorted('DUE_DATE')}
                onClick={toggleSort('DUE_DATE')}
              >
                {i18n.applications.list.dueDate}
              </SortableTh>
              <SortableTh
                sticky
                top={'106px'}
                sorted={isSorted('START_DATE')}
                onClick={toggleSort('START_DATE')}
              >
                {i18n.applications.list.startDate}
              </SortableTh>
              <Th sticky top={'106px'}>
                {i18n.applications.list.basis}
              </Th>
              <Th sticky top={'106px'}>
                {i18n.applications.list.unit}
              </Th>
              <SortableTh
                sticky
                top={'106px'}
                sorted={isSorted('STATUS')}
                onClick={toggleSort('STATUS')}
              >
                {i18n.applications.list.status}
              </SortableTh>
              <Th>
                {showCheckboxes ? (
                  <CheckAllContainer>
                    <Checkbox
                      checked={isAllChecked()}
                      label={'hidden'}
                      hiddenLabel={true}
                      dataQa={'toggle-check-all-checkbox'}
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
      </div>
    </div>
  )
})

export default ApplicationsList

const CheckAllContainer = styled.div`
  display: flex;
  justify-content: flex-end;
`
