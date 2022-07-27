// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useContext, useState } from 'react'
import { Link } from 'react-router-dom'
import styled from 'styled-components'

import type { Failure } from 'lib-common/api'
import FiniteDateRange from 'lib-common/finite-date-range'
import type { DaycarePlacementWithDetails } from 'lib-common/generated/api-types/placement'
import type { ServiceNeedOption } from 'lib-common/generated/api-types/serviceneed'
import LocalDate from 'lib-common/local-date'
import AsyncButton from 'lib-components/atoms/buttons/AsyncButton'
import Button from 'lib-components/atoms/buttons/Button'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { DatePickerDeprecated } from 'lib-components/molecules/DatePickerDeprecated'
import InfoModal from 'lib-components/molecules/modals/InfoModal'
import { fontWeights } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { faQuestion } from 'lib-icons'

import type { PlacementUpdate } from '../../../api/child/placements'
import { deletePlacement, updatePlacement } from '../../../api/child/placements'
import Toolbar from '../../../components/common/Toolbar'
import ToolbarAccordion, {
  RestrictedToolbar
} from '../../../components/common/ToolbarAccordion'
import { useTranslation } from '../../../state/i18n'
import type { UiState } from '../../../state/ui'
import { UIContext } from '../../../state/ui'
import type { DateRange } from '../../../utils/date'
import {
  getStatusLabelByDateRange,
  isActiveDateRange
} from '../../../utils/date'
import { InputWarning } from '../../common/InputWarning'

import ServiceNeeds from './ServiceNeeds'

interface Props {
  placement: DaycarePlacementWithDetails
  onRefreshNeeded: () => void
  checkOverlaps: (
    range: DateRange,
    placement: DaycarePlacementWithDetails
  ) => boolean | undefined
  serviceNeedOptions: ServiceNeedOption[]
}

const DataRow = styled.div`
  display: flex;
  min-height: 2rem;
`

const DataLabel = styled.div`
  width: 240px;
  padding: 0 40px 0 0;
  margin: 0;
  font-weight: ${fontWeights.semibold};
`

const DataValue = styled.div`
  display: flex;
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
  onRefreshNeeded,
  checkOverlaps,
  serviceNeedOptions
}: Props) {
  const { i18n } = useTranslation()
  const { setErrorMessage } = useContext<UiState>(UIContext)

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

  function startEdit() {
    setToggled(true)
    setForm(initFormData())
    setEditing(true)
    setStartDateWarning(false)
    setEndDateWarning(false)
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
    () => updatePlacement(placement.id, form),
    [placement.id, form]
  )

  function submitDelete() {
    void deletePlacement(placement.id).then((res) => {
      if (res.isSuccess) {
        setConfirmingDelete(false)
        onRefreshNeeded()
      }
    })
  }

  function calculateOverlapWarnings(startDate: LocalDate, endDate: LocalDate) {
    checkOverlaps({ startDate, endDate }, placement)
      ? startDate === placement.startDate
        ? setEndDateWarning(true)
        : setStartDateWarning(true)
      : startDate === placement.startDate
      ? setEndDateWarning(false)
      : setStartDateWarning(false)
  }

  const currentGroupPlacement = placement.groupPlacements.find((gp) =>
    FiniteDateRange.from(gp).includes(LocalDate.todayInSystemTz())
  )

  return placement.isRestrictedFromUser ? (
    <RestrictedToolbar
      title={i18n.childInformation.placements.restrictedName}
      subtitle={`${placement.startDate.format()} - ${placement.endDate.format()}`}
      statusLabel={getStatusLabelByDateRange(placement)}
    />
  ) : (
    <div>
      <ToolbarAccordion
        title={placement.daycare.name}
        subtitle={`${placement.startDate.format()} - ${placement.endDate.format()}`}
        onToggle={() => setToggled((prev) => !prev)}
        open={toggled}
        toolbar={
          <Toolbar
            dateRange={placement}
            onEdit={() => startEdit()}
            dataQaEdit="btn-edit-placement"
            editableFor={[
              'ADMIN',
              'SERVICE_WORKER',
              'FINANCE_ADMIN',
              'UNIT_SUPERVISOR'
            ]}
            onDelete={() => setConfirmingDelete(true)}
            deletableFor={
              placement.startDate.isAfter(LocalDate.todayInSystemTz())
                ? [
                    'ADMIN',
                    'SERVICE_WORKER',
                    'FINANCE_ADMIN',
                    'UNIT_SUPERVISOR'
                  ]
                : ['ADMIN', 'SERVICE_WORKER', 'FINANCE_ADMIN']
            }
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
              <DatepickerContainer>
                <CompactDatePicker
                  date={form.endDate}
                  onChange={(endDate) => {
                    setForm({ ...form, endDate })
                    calculateOverlapWarnings(placement.startDate, endDate)
                  }}
                  type="full-width"
                  data-qa="placement-end-date-input"
                />
                {endDateWarning ? (
                  <WarningContainer>
                    <InputWarning
                      text={i18n.childInformation.placements.warning.overlap}
                      iconPosition="after"
                    />
                  </WarningContainer>
                ) : null}
              </DatepickerContainer>
            ) : (
              placement.endDate.format()
            )}
          </DataValue>
        </DataRow>
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
        {FiniteDateRange.from(placement).includes(
          LocalDate.todayInSystemTz()
        ) && (
          <DataRow>
            <DataLabel>
              {i18n.childInformation.placements.daycareGroup}
            </DataLabel>
            <DataValue data-qa="placement-details-unit">
              {currentGroupPlacement?.groupId &&
              currentGroupPlacement?.groupName ? (
                <Link
                  to={`/units/${placement.daycare.id}/attendances?group=${currentGroupPlacement.groupId}`}
                >
                  {currentGroupPlacement.groupName}
                </Link>
              ) : (
                i18n.childInformation.placements.daycareGroupMissing
              )}
            </DataValue>
          </DataRow>
        )}
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
              <Button
                onClick={() => setEditing(false)}
                text={i18n.common.cancel}
              />
              <AsyncButton
                primary
                onClick={submitUpdate}
                onSuccess={onSuccess}
                onFailure={onFailure}
                text={i18n.common.save}
              />
            </FixedSpaceRow>
          </ActionRow>
        )}

        <Gap size="s" />

        <ServiceNeeds
          placement={placement}
          reload={onRefreshNeeded}
          serviceNeedOptions={serviceNeedOptions}
        />
      </ToolbarAccordion>
      {confirmingDelete && (
        <InfoModal
          title={i18n.childInformation.placements.deletePlacement.confirmTitle}
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
`

const WarningContainer = styled.div`
  margin: 5px 0;
`
