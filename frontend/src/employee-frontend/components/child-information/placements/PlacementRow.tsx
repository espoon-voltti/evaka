// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import orderBy from 'lodash/orderBy'
import React, { useCallback, useContext, useMemo, useState } from 'react'
import styled from 'styled-components'
import { Link } from 'wouter'

import type { Failure } from 'lib-common/api'
import DateRange from 'lib-common/date-range'
import FiniteDateRange from 'lib-common/finite-date-range'
import type { Action } from 'lib-common/generated/action'
import type {
  DaycareGroupPlacement,
  DaycarePlacementWithDetails
} from 'lib-common/generated/api-types/placement'
import type { ServiceNeedOption } from 'lib-common/generated/api-types/serviceneed'
import LocalDate from 'lib-common/local-date'
import {
  cancelMutation,
  useMutationResult,
  useQueryResult
} from 'lib-common/query'
import type { UUID } from 'lib-common/types'
import UnorderedList from 'lib-components/atoms/UnorderedList'
import { LegacyButton } from 'lib-components/atoms/buttons/LegacyButton'
import { MutateButton } from 'lib-components/atoms/buttons/MutateButton'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import DatePicker from 'lib-components/molecules/date-picker/DatePicker'
import InfoModal from 'lib-components/molecules/modals/InfoModal'
import { fontWeights } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import { faQuestion } from 'lib-icons'

import { useTranslation } from '../../../state/i18n'
import type { UiState } from '../../../state/ui'
import { UIContext } from '../../../state/ui'
import {
  getStatusLabelByDateRange,
  isActiveDateRange
} from '../../../utils/date'
import { InputWarning } from '../../common/InputWarning'
import RetroactiveConfirmation, {
  isChangeRetroactive
} from '../../common/RetroactiveConfirmation'
import Toolbar from '../../common/Toolbar'
import ToolbarAccordion, {
  RestrictedToolbar
} from '../../common/ToolbarAccordion'
import { getPreschoolTermsQuery } from '../../unit/queries'
import {
  backupCaresQuery,
  deletePlacementMutation,
  updatePlacementMutation
} from '../queries'

import ServiceNeeds from './ServiceNeeds'

interface PlacementUpdate {
  startDate: LocalDate | null
  endDate: LocalDate | null
}

interface Props {
  placement: DaycarePlacementWithDetails
  permittedActions: Action.Placement[]
  permittedServiceNeedActions: Partial<Record<string, Action.ServiceNeed[]>>
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

export default React.memo(function PlacementRow({
  placement,
  permittedActions,
  permittedServiceNeedActions,
  otherPlacementRanges,
  serviceNeedOptions
}: Props) {
  const { i18n } = useTranslation()
  const { setErrorMessage } = useContext<UiState>(UIContext)
  const backupCares = useQueryResult(
    backupCaresQuery({ childId: placement.child.id })
  )

  const preschoolTermsResult = useQueryResult(getPreschoolTermsQuery())

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
  const [preschoolDatesTermWarning, setPreschoolDatesTermWarning] =
    useState(false)

  const retroactive = useMemo(
    () =>
      isChangeRetroactive(
        form.startDate && form.endDate
          ? new DateRange(form.startDate, form.endDate)
          : null,
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
    setPreschoolDatesTermWarning(false)
  }

  const onSuccess = useCallback(() => {
    setEditing(false)
  }, [])

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

  const { mutateAsync: deletePlacement } = useMutationResult(
    deletePlacementMutation
  )

  function submitDelete() {
    void deletePlacement({ placementId: placement.id }).then((res) => {
      if (res.isSuccess) {
        setConfirmingDelete(false)
      }
    })
  }

  const [conflictBackupCare, setConflictBackupCare] = useState(false)

  const dependingBackupCares = useMemo(
    () =>
      backupCares
        .map((backupCareResponse) =>
          backupCareResponse.backupCares.filter(({ backupCare }) =>
            new FiniteDateRange(
              placement.startDate,
              placement.endDate
            ).overlaps(backupCare.period)
          )
        )
        .getOrElse([]),
    [backupCares, placement]
  )

  function validate(startDate: LocalDate | null, endDate: LocalDate | null) {
    if (!startDate || !endDate) return
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
    if (placement.type === 'PRESCHOOL' || placement.type === 'PREPARATORY') {
      preschoolTermsResult.map((preschoolTerms) => {
        const datesAreInsideSomePreschoolTerm = preschoolTerms.some(
          (term) =>
            (term.finnishPreschool.asDateRange().includes(startDate) &&
              term.finnishPreschool.asDateRange().includes(endDate)) ||
            (term.swedishPreschool.asDateRange().includes(startDate) &&
              term.swedishPreschool.asDateRange().includes(endDate))
        )
        setPreschoolDatesTermWarning(!datesAreInsideSomePreschoolTerm)
      })
    }

    if (
      placement.type === 'PRESCHOOL_DAYCARE' ||
      placement.type === 'PRESCHOOL_DAYCARE_ONLY' ||
      placement.type === 'PREPARATORY_DAYCARE'
    ) {
      preschoolTermsResult.map((preschoolTerms) => {
        const datesAreInsideSomeExtendedPreschoolTerm = preschoolTerms.some(
          (term) =>
            term.extendedTerm.asDateRange().includes(startDate) &&
            term.extendedTerm.asDateRange().includes(endDate)
        )
        setPreschoolDatesTermWarning(!datesAreInsideSomeExtendedPreschoolTerm)
      })
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
                <DatePicker
                  date={form.startDate}
                  maxDate={form.endDate ?? undefined}
                  onChange={(startDate) => {
                    setForm({ ...form, startDate })
                    validate(startDate, placement.endDate)
                  }}
                  data-qa="placement-start-date-input"
                  locale="fi"
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
                    <DatePicker
                      date={form.endDate}
                      minDate={form.startDate ?? undefined}
                      onChange={(endDate) => {
                        setForm({ ...form, endDate })
                        validate(placement.startDate, endDate)
                      }}
                      locale="fi"
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
                {preschoolDatesTermWarning && (
                  <div>
                    <WarningContainer>
                      <InputWarning
                        text={
                          i18n.childInformation.placements.createPlacement
                            .preschoolTermNotOpen
                        }
                        iconPosition="after"
                      />
                    </WarningContainer>
                  </div>
                )}
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
        {placement.modifiedAt && (
          <DataRow>
            <DataLabel>{i18n.childInformation.placements.modifiedAt}</DataLabel>
            <DataValue data-qa="placement-modified-at">
              {placement.modifiedAt.format()}
            </DataValue>
          </DataRow>
        )}
        {placement.modifiedBy && (
          <DataRow>
            <DataLabel>{i18n.childInformation.placements.modifiedBy}</DataLabel>
            <DataValue data-qa="placement-modified-by">
              {placement.modifiedBy.name}
            </DataValue>
          </DataRow>
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
              <MutateButton
                primary
                mutation={updatePlacementMutation}
                onClick={() =>
                  form.startDate && form.endDate
                    ? {
                        placementId: placement.id,
                        body: {
                          startDate: form.startDate,
                          endDate: form.endDate
                        }
                      }
                    : cancelMutation
                }
                onSuccess={onSuccess}
                onFailure={onFailure}
                text={i18n.common.save}
                disabled={
                  form.startDate === null ||
                  form.endDate === null ||
                  (retroactive && !confirmedRetroactive)
                }
              />
            </FixedSpaceRow>
          </ActionRow>
        )}

        <Gap size="s" />

        <ServiceNeeds
          placement={placement}
          permittedPlacementActions={permittedActions}
          permittedServiceNeedActions={permittedServiceNeedActions}
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
