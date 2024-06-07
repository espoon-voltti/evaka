// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import orderBy from 'lodash/orderBy'
import React, { useCallback, useContext, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import styled from 'styled-components'

import { ChildContext } from 'employee-frontend/state'
import { Failure, wrapResult } from 'lib-common/api'
import DateRange from 'lib-common/date-range'
import FiniteDateRange from 'lib-common/finite-date-range'
import { Action } from 'lib-common/generated/action'
import {
  DaycareGroupPlacement,
  DaycarePlacementWithDetails
} from 'lib-common/generated/api-types/placement'
import { ServiceNeedOption } from 'lib-common/generated/api-types/serviceneed'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'
import UnorderedList from 'lib-components/atoms/UnorderedList'
import { LegacyAsyncButton } from 'lib-components/atoms/buttons/LegacyAsyncButton'
import { LegacyButton } from 'lib-components/atoms/buttons/LegacyButton'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { DatePickerDeprecated } from 'lib-components/molecules/DatePickerDeprecated'
import InfoModal from 'lib-components/molecules/modals/InfoModal'
import { fontWeights } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import { faQuestion } from 'lib-icons'

import Toolbar from '../../../components/common/Toolbar'
import ToolbarAccordion, {
  RestrictedToolbar
} from '../../../components/common/ToolbarAccordion'
import {
  deletePlacement,
  updatePlacementById
} from '../../../generated/api-clients/placement'
import { useTranslation } from '../../../state/i18n'
import { UIContext, UiState } from '../../../state/ui'
import {
  getStatusLabelByDateRange,
  isActiveDateRange
} from '../../../utils/date'
import { InputWarning } from '../../common/InputWarning'
import RetroactiveConfirmation, {
  isChangeRetroactive
} from '../../common/RetroactiveConfirmation'

import ServiceNeeds from './ServiceNeeds'

const updatePlacementResult = wrapResult(updatePlacementById)
const deletePlacementResult = wrapResult(deletePlacement)

interface PlacementUpdate {
  startDate: LocalDate
  endDate: LocalDate
}

interface Props {
  placement: DaycarePlacementWithDetails
  permittedActions: Action.Placement[]
  permittedServiceNeedActions: Record<string, Action.ServiceNeed[]>
  onRefreshNeeded: () => void
  otherPlacementRanges: FiniteDateRange[]
  serviceNeedOptions: ServiceNeedOption[]
}

const DataRow = styled.div`
  display: flex;
  min-height: 2rem;
`

const WarningRow = styled.div`
  width: 600px;
  display: flex;
  flex-direction: column;
  margin-top: ${defaultMargins.s};
  margin-bottom: ${defaultMargins.m};
`

const DataLabel = styled.div`
  width: 240px;
  padding: 0 40px 0 0;
  margin: 0;
  font-weight: ${fontWeights.semibold};
`

const DataValue = styled.div<{ marginBottom?: string }>`
  display: flex;
  ${(p) => (p.marginBottom ? `margin-bottom: ${p.marginBottom};` : '')}
`

const ActionRow = styled.div`
  display: flex;
  justify-content: flex-end;
  margin-right: 20px;
  flex-grow: 1;

  > * {
    margin-left: 10px;
  }
`

const CompactDatePicker = styled(DatePickerDeprecated)`
  .input {
    font-size: 1rem;
    padding: 0;
    height: unset;
    min-height: unset;
  }
`

export default React.memo(function PlacementRow({
  placement,
  permittedActions,
  permittedServiceNeedActions,
  onRefreshNeeded,
  otherPlacementRanges,
  serviceNeedOptions
}: Props) {
  const { i18n } = useTranslation()
  const { setErrorMessage } = useContext<UiState>(UIContext)
  const { backupCares, loadBackupCares } = useContext(ChildContext)

  const expandedAtStart = isActiveDateRange(
    placement.startDate,
    placement.endDate
  )
  const [toggled, setToggled] = useState(expandedAtStart)

  const initFormData = () => ({
    startDate: placement.startDate,
    endDate: placement.endDate
  })
  const [form, setForm] = useState<PlacementUpdate>(initFormData())
  const [editing, setEditing] = useState<boolean>(false)
  const [confirmingDelete, setConfirmingDelete] = useState<boolean>(false)
  const [startDateWarning, setStartDateWarning] = useState(false)
  const [endDateWarning, setEndDateWarning] = useState(false)
  const retroactive = useMemo(
    () =>
      isChangeRetroactive(
        new DateRange(form.startDate, form.endDate),
        new DateRange(placement.startDate, placement.endDate),
        false, // only range can be edited
        LocalDate.todayInHelsinkiTz()
      ),
    [form, placement]
  )
  const [confirmedRetroactive, setConfirmedRetroactive] = useState(false)

  function startEdit() {
    setToggled(true)
    setForm(initFormData())
    setEditing(true)
    setStartDateWarning(false)
    setEndDateWarning(false)
    setConfirmedRetroactive(false)
  }

  const onSuccess = useCallback(() => {
    setEditing(false)
    onRefreshNeeded()
  }, [onRefreshNeeded])

  const onFailure = useCallback(
    (res: Failure<unknown>) => {
      const message =
        res.statusCode === 403
          ? i18n.common.error.forbidden
          : res?.statusCode === 409
            ? i18n.childInformation.placements.error.conflict.title
            : i18n.common.error.unknown
      const text =
        res?.statusCode === 409
          ? i18n.childInformation.placements.error.conflict.text
          : ''
      setErrorMessage({
        type: 'error',
        title: message,
        text: text,
        resolveLabel: i18n.common.ok
      })
    },
    [i18n, setErrorMessage]
  )

  const submitUpdate = useCallback(
    () =>
      updatePlacementResult({ placementId: placement.id, body: form }).then(
        (res) => {
          if (res.isSuccess) {
            return loadBackupCares()
          }
          return res
        }
      ),
    [placement.id, form, loadBackupCares]
  )

  function submitDelete() {
    void deletePlacementResult({ placementId: placement.id }).then((res) => {
      if (res.isSuccess) {
        setConfirmingDelete(false)
        onRefreshNeeded()
      }
    })
  }

  const [conflictBackupCare, setConflictBackupCare] = useState(false)

  const dependingBackupCares = useMemo(
    () =>
      backupCares
        .map((backups) =>
          backups.filter(({ backupCare }) =>
            new FiniteDateRange(
              placement.startDate,
              placement.endDate
            ).overlaps(backupCare.period)
          )
        )
        .getOrElse([]),
    [backupCares, placement]
  )

  function calculateOverlapWarnings(startDate: LocalDate, endDate: LocalDate) {
    if (
      otherPlacementRanges.some((range) =>
        range.overlaps(new FiniteDateRange(startDate, endDate))
      )
    ) {
      if (startDate === placement.startDate) {
        setEndDateWarning(true)
      } else {
        setStartDateWarning(true)
      }
    } else {
      if (startDate === placement.startDate) {
        setEndDateWarning(false)
      } else {
        setStartDateWarning(false)
      }
    }

    const range = new FiniteDateRange(startDate, endDate)

    if (
      dependingBackupCares.some(({ backupCare }) =>
        backupCare.period.contains(range)
      )
    ) {
      // a depending backup care has this placement in the middle, so it cannot be modified
      setConflictBackupCare(true)
    } else if (
      dependingBackupCares.some(
        ({ backupCare }) =>
          placement.startDate <= backupCare.period.start &&
          startDate > backupCare.period.start
      )
    ) {
      // the start date was moved from before a backup care to after its start
      setConflictBackupCare(true)
    } else if (
      dependingBackupCares.some(
        ({ backupCare }) =>
          placement.endDate >= backupCare.period.end &&
          endDate < backupCare.period.end
      )
    ) {
      // the end date was moved from after a backup care to before its end
      setConflictBackupCare(true)
    } else {
      setConflictBackupCare(false)
    }
  }

  return placement.isRestrictedFromUser ? (
    <RestrictedToolbar
      title={placement.daycare.name}
      subtitle={`${placement.startDate.format()} - ${placement.endDate.format()}`}
      statusLabel={getStatusLabelByDateRange(placement)}
    />
  ) : (
    <div data-qa="placement-row">
      <ToolbarAccordion
        title={placement.daycare.name}
        subtitle={`${placement.startDate.format()} - ${placement.endDate.format()}`}
        onToggle={() => setToggled((prev) => !prev)}
        open={toggled}
        toolbar={
          <Toolbar
            dateRange={placement}
            guarantee={
              placement.placeGuarantee &&
              placement.startDate.isAfter(LocalDate.todayInHelsinkiTz())
            }
            dataQa="placement-toolbar"
            onEdit={() => startEdit()}
            dataQaEdit="btn-edit-placement"
            editable={permittedActions.includes('UPDATE')}
            onDelete={() => setConfirmingDelete(true)}
            deletable={permittedActions.includes('DELETE')}
            dataQaDelete="btn-remove-placement"
            warning={
              placement.missingServiceNeedDays > 0
                ? `${i18n.childInformation.placements.serviceNeedMissingTooltip1} ${placement.missingServiceNeedDays} ${i18n.childInformation.placements.serviceNeedMissingTooltip2}`
                : undefined
            }
          />
        }
        data-qa={`placement-${placement.id}`}
      >
        <DataRow>
          <DataLabel>{i18n.childInformation.placements.startDate}</DataLabel>
          <DataValue data-qa="placement-details-start-date">
            {editing ? (
              <DatepickerContainer>
                <CompactDatePicker
                  date={form.startDate}
                  maxDate={form.endDate}
                  onChange={(startDate) => {
                    setForm({ ...form, startDate })
                    calculateOverlapWarnings(startDate, placement.endDate)
                  }}
                  type="full-width"
                  data-qa="placement-start-date-input"
                />
                {startDateWarning ? (
                  <WarningContainer>
                    <InputWarning
                      text={i18n.childInformation.placements.warning.overlap}
                      iconPosition="after"
                    />
                  </WarningContainer>
                ) : null}
              </DatepickerContainer>
            ) : (
              placement.startDate.format()
            )}
          </DataValue>
        </DataRow>
        <DataRow>
          <DataLabel>{i18n.childInformation.placements.endDate}</DataLabel>
          <DataValue data-qa="placement-details-end-date">
            {editing ? (
              <div>
                <div>
                  <DatepickerContainer>
                    <CompactDatePicker
                      date={form.endDate}
                      minDate={form.startDate}
                      onChange={(endDate) => {
                        setForm({ ...form, endDate })
                        calculateOverlapWarnings(placement.startDate, endDate)
                      }}
                      type="full-width"
                      data-qa="placement-end-date-input"
                      aria-labelledby="placement-details-end-date"
                    />
                    {endDateWarning ? (
                      <WarningContainer>
                        <InputWarning
                          text={
                            i18n.childInformation.placements.warning.overlap
                          }
                          iconPosition="after"
                        />
                      </WarningContainer>
                    ) : null}
                  </DatepickerContainer>
                </div>
                <div>
                  {conflictBackupCare && (
                    <WarningContainer>
                      <InputWarning
                        text={
                          i18n.childInformation.placements.warning
                            .backupCareDepends
                        }
                        iconPosition="after"
                      />
                    </WarningContainer>
                  )}
                </div>
              </div>
            ) : (
              placement.endDate.format()
            )}
          </DataValue>
        </DataRow>
        {editing && retroactive && (
          <WarningRow>
            <RetroactiveConfirmation
              confirmed={confirmedRetroactive}
              setConfirmed={setConfirmedRetroactive}
            />
          </WarningRow>
        )}
        {placement.terminationRequestedDate && (
          <DataRow>
            <DataLabel>
              {i18n.childInformation.placements.terminatedByGuardian}
            </DataLabel>
            <DataValue data-qa="placement-terminated">
              {`${
                i18n.childInformation.placements.terminated
              } ${placement.terminationRequestedDate.format()}`}
            </DataValue>
          </DataRow>
        )}
        <DataRow>
          <DataLabel>{i18n.childInformation.placements.area}</DataLabel>
          <DataValue data-qa="placement-details-area">
            {placement.daycare.area}
          </DataValue>
        </DataRow>
        <DataRow>
          <DataLabel>{i18n.childInformation.placements.daycareUnit}</DataLabel>
          <DataValue data-qa="placement-details-unit">
            <Link to={`/units/${placement.daycare.id}`}>
              {placement.daycare.name}
            </Link>
          </DataValue>
        </DataRow>
        <DataRow>
          <DataLabel>
            {i18n.childInformation.placements.daycareGroups}
          </DataLabel>
          <DataValue
            data-qa="placement-details-unit"
            marginBottom={defaultMargins.s}
          >
            <UnorderedList>
              {orderBy(
                placement.groupPlacements,
                (groupPlacement) => groupPlacement.startDate,
                'desc'
              ).map((groupPlacement) => (
                <li key={groupPlacement.startDate.formatIso()}>
                  <GroupPlacement
                    unitId={placement.daycare.id}
                    groupPlacement={groupPlacement}
                  />
                </li>
              ))}
            </UnorderedList>
          </DataValue>
        </DataRow>
        <DataRow>
          <DataLabel>{i18n.childInformation.placements.type}</DataLabel>
          <DataValue data-qa="placement-details-type">
            {i18n.placement.type[placement.type]}
          </DataValue>
        </DataRow>
        <DataRow>
          <DataLabel>{i18n.childInformation.placements.providerType}</DataLabel>
          <DataValue data-qa="placement-details-provider-type">
            {i18n.common.providerType[placement.daycare.providerType]}
          </DataValue>
        </DataRow>
        <Gap size="s" />
        {editing && (
          <ActionRow>
            <FixedSpaceRow>
              <LegacyButton
                onClick={() => setEditing(false)}
                text={i18n.common.cancel}
              />
              <LegacyAsyncButton
                primary
                onClick={submitUpdate}
                onSuccess={onSuccess}
                onFailure={onFailure}
                text={i18n.common.save}
                disabled={retroactive && !confirmedRetroactive}
              />
            </FixedSpaceRow>
          </ActionRow>
        )}

        <Gap size="s" />

        <ServiceNeeds
          placement={placement}
          permittedPlacementActions={permittedActions}
          permittedServiceNeedActions={permittedServiceNeedActions}
          reload={onRefreshNeeded}
          serviceNeedOptions={serviceNeedOptions}
        />
      </ToolbarAccordion>
      {confirmingDelete && (
        <InfoModal
          title={i18n.childInformation.placements.deletePlacement.confirmTitle}
          text={
            dependingBackupCares.length > 0
              ? i18n.childInformation.placements.deletePlacement
                  .hasDependingBackupCares
              : undefined
          }
          type="warning"
          icon={faQuestion}
          resolve={{
            action: submitDelete,
            label: i18n.childInformation.placements.deletePlacement.btn
          }}
          reject={{
            action: () => setConfirmingDelete(false),
            label: i18n.common.cancel
          }}
        />
      )}
    </div>
  )
})

const DatepickerContainer = styled.div`
  display: flex;
  flex-direction: column;
  width: fit-content;
`

const WarningContainer = styled.div`
  margin: 5px 0;
`

const GroupPlacement = ({
  unitId,
  groupPlacement
}: {
  unitId: UUID
  groupPlacement: DaycareGroupPlacement
}) => {
  const { i18n } = useTranslation()

  const range = groupPlacement.startDate.isEqual(groupPlacement.endDate)
    ? groupPlacement.startDate.format()
    : `${groupPlacement.startDate.format()} – ${groupPlacement.endDate.format()}`

  if (groupPlacement.groupId === null) {
    return (
      <>
        {i18n.childInformation.placements.daycareGroupMissing}, {range}
      </>
    )
  }

  return (
    <Link
      key={groupPlacement.startDate.formatIso()}
      to={`/units/${unitId}/groups?open_groups=${groupPlacement.groupId}`}
    >
      {groupPlacement.groupName}, {range}
    </Link>
  )
}
