// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useState } from 'react'
import { Placement } from '~types/child'
import ToolbarAccordion, {
  RestrictedToolbar
} from '~components/common/ToolbarAccordion'
import {
  DateRange,
  getStatusLabelByDateRange,
  isActiveDateRange
} from '~utils/date'
import { useTranslation } from '~state/i18n'
import { faQuestion } from '@evaka/lib-icons'
import styled from 'styled-components'
import { Gap } from '@evaka/lib-components/src/white-space'
import Button from '@evaka/lib-components/src/atoms/buttons/Button'
import InfoModal from '@evaka/lib-components/src/molecules/modals/InfoModal'
import { DatePicker } from '@evaka/lib-components/src/molecules/DatePicker'
import { UIContext, UiState } from '~state/ui'
import { Link } from 'react-router-dom'
import { AlertBox } from '@evaka/lib-components/src/molecules/MessageBoxes'
import Toolbar from '~components/common/Toolbar'
import {
  deletePlacement,
  PlacementUpdate,
  updatePlacement
} from 'api/child/placements'
import { InputWarning } from '~components/common/InputWarning'
import LocalDate from '@evaka/lib-common/src/local-date'
import { FixedSpaceRow } from '@evaka/lib-components/src/layout/flex-helpers'

interface Props {
  placement: Placement
  onRefreshNeeded: () => void | undefined
  checkOverlaps: (range: DateRange, placement: Placement) => boolean | undefined
}

const DataRow = styled.div`
  display: flex;
  min-height: 2rem;
`

const DataLabel = styled.div`
  width: 41.6666667%;
  padding: 0 40px 0 0;
  margin: 0;
  font-weight: 600;
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

const CompactDatePicker = styled(DatePicker)`
  .input {
    font-size: 1rem;
    padding: 0;
    height: unset;
    min-height: unset;
  }
`

function PlacementRow({ placement, onRefreshNeeded, checkOverlaps }: Props) {
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

  function submitUpdate() {
    void updatePlacement(placement.id, form).then((res) => {
      if (res.isSuccess) {
        setEditing(false)
        onRefreshNeeded()
      } else if (res.isFailure) {
        const message =
          res.statusCode === 403
            ? i18n.common.error.forbidden
            : res.statusCode === 409
            ? i18n.childInformation.placements.error.conflict.title
            : i18n.common.error.unknown
        const text =
          res.statusCode === 409
            ? i18n.childInformation.placements.error.conflict.text
            : ''
        setErrorMessage({
          type: 'error',
          title: message,
          text: text
        })
      }
    })
  }

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

  const showServiceNeedWarning =
    placement.type !== 'CLUB' &&
    !editing &&
    placement.missingServiceNeedDays > 0

  return placement.isRestrictedFromUser ? (
    <RestrictedToolbar
      title={i18n.childInformation.placements.restrictedName}
      subtitle={`${placement.startDate.format()} - ${placement.endDate.format()}`}
      statusLabel={getStatusLabelByDateRange(placement)}
    />
  ) : (
    <>
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
            deletableFor={['ADMIN', 'SERVICE_WORKER', 'FINANCE_ADMIN']}
            dataQaDelete="btn-remove-placement"
            warning={
              showServiceNeedWarning
                ? {
                    text: `${i18n.childInformation.placements.serviceNeedMissingTooltip1} ${placement.missingServiceNeedDays} ${i18n.childInformation.placements.serviceNeedMissingTooltip2}`,
                    tooltipId: `tooltip_missing-service-need_${placement.id}`
                  }
                : undefined
            }
          />
        }
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
                  dataQa="placement-start-date-input"
                />
                {startDateWarning ? (
                  <WarningContainer>
                    <InputWarning
                      text={i18n.childInformation.placements.warning.overlap}
                      iconPosition={'after'}
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
                  dataQa="placement-end-date-input"
                />
                {endDateWarning ? (
                  <WarningContainer>
                    <InputWarning
                      text={i18n.childInformation.placements.warning.overlap}
                      iconPosition={'after'}
                    />
                  </WarningContainer>
                ) : null}
              </DatepickerContainer>
            ) : (
              placement.endDate.format()
            )}
          </DataValue>
        </DataRow>
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
          <DataLabel>{i18n.childInformation.placements.type}</DataLabel>
          <DataValue data-qa="placement-details-type">
            {i18n.decisionDraft.types[placement.type]}
          </DataValue>
        </DataRow>
        <Gap size="s" />
        {showServiceNeedWarning && (
          <AlertBox
            message={`${i18n.childInformation.placements.serviceNeedMissing1} ${placement.missingServiceNeedDays} ${i18n.childInformation.placements.serviceNeedMissing2}`}
            thin
            wide
          />
        )}
        {editing && (
          <ActionRow>
            <FixedSpaceRow>
              <Button
                onClick={() => setEditing(false)}
                text={i18n.common.cancel}
              />
              <Button
                primary
                onClick={() => submitUpdate()}
                text={i18n.common.save}
              />
            </FixedSpaceRow>
          </ActionRow>
        )}
      </ToolbarAccordion>
      {confirmingDelete && (
        <InfoModal
          title={i18n.childInformation.placements.deletePlacement.confirmTitle}
          iconColour={'orange'}
          icon={faQuestion}
          resolveLabel={i18n.childInformation.placements.deletePlacement.btn}
          resolve={submitDelete}
          rejectLabel={i18n.common.cancel}
          reject={() => setConfirmingDelete(false)}
        />
      )}
    </>
  )
}

export default PlacementRow

const DatepickerContainer = styled.div`
  display: flex;
  flex-direction: column;
`

const WarningContainer = styled.div`
  margin: 5px 0;
`
