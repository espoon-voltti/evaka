// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { addDays, isAfter, isBefore, subDays } from 'date-fns'
import React, { useCallback, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import styled from 'styled-components'

import { wrapResult } from 'lib-common/api'
import FiniteDateRange from 'lib-common/finite-date-range'
import { AbsenceType } from 'lib-common/generated/api-types/absence'
import LocalDate from 'lib-common/local-date'
import { useQueryResult } from 'lib-common/query'
import { UUID } from 'lib-common/types'
import useRouteParams from 'lib-common/useRouteParams'
import { groupAbsencesByDateRange } from 'lib-common/utils/absences'
import { mockNow } from 'lib-common/utils/helpers'
import { useApiState } from 'lib-common/utils/useRestApi'
import HorizontalLine from 'lib-components/atoms/HorizontalLine'
import Title from 'lib-components/atoms/Title'
import AsyncButton from 'lib-components/atoms/buttons/AsyncButton'
import Button from 'lib-components/atoms/buttons/Button'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import InputField from 'lib-components/atoms/form/InputField'
import { ContentArea } from 'lib-components/layout/Container'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import InfoModal from 'lib-components/molecules/modals/InfoModal'
import { fontWeights, Label, P } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { faExclamation, faTrash } from 'lib-icons'

import { renderResult } from '../../async-rendering'
import ChildNameBackButton from '../../common/ChildNameBackButton'
import { Actions, CustomTitle } from '../../common/components'
import { useTranslation } from '../../common/i18n'
import { futureAbsencesOfChild } from '../../generated/api-clients/absence'
import {
  deleteAbsenceRange,
  postAbsenceRange
} from '../../generated/api-clients/attendance'
import { TallContentArea } from '../../pairing/components'
import { childrenQuery } from '../queries'
import { useChild } from '../utils'

import AbsenceSelector, { AbsenceTypeWithNoAbsence } from './AbsenceSelector'

const postAbsenceRangeResult = wrapResult(postAbsenceRange)
const deleteAbsenceRangeResult = wrapResult(deleteAbsenceRange)
const futureAbsencesByChildResult = wrapResult(futureAbsencesOfChild)

export default React.memo(function MarkAbsentBeforehand({
  unitId
}: {
  unitId: UUID
}) {
  const navigate = useNavigate()
  const { i18n } = useTranslation()

  const { childId } = useRouteParams(['childId'])
  const child = useChild(useQueryResult(childrenQuery(unitId)), childId)

  const [selectedAbsenceType, setSelectedAbsenceType] = useState<
    AbsenceTypeWithNoAbsence | undefined
  >(undefined)
  const [uiMode, setUiMode] = useState<
    'default' | 'confirmDelete' | 'confirmExit'
  >('default')
  const [deleteRange, setDeleteRange] = useState<FiniteDateRange | undefined>(
    undefined
  )

  const [absences, loadFutureAbsences] = useApiState(
    () => futureAbsencesByChildResult({ childId }),
    [childId]
  )

  const [startDate, setStartDate] = useState<string>('')
  const [endDate, setEndDate] = useState<string>('')

  const postAbsence = useCallback(
    (absenceType: AbsenceType) =>
      postAbsenceRangeResult({
        unitId,
        childId,
        body: {
          absenceType,
          range: new FiniteDateRange(
            LocalDate.parseIso(startDate),
            LocalDate.parseIso(endDate)
          )
        }
      }),
    [childId, endDate, startDate, unitId]
  )

  const canSave = useMemo(
    () =>
      isAfter(new Date(startDate), subDays(mockNow() ?? new Date(), 1)) &&
      isBefore(new Date(startDate), addDays(new Date(endDate), 1)),
    [endDate, startDate]
  )

  const createAbsence = useCallback(async () => {
    if (
      selectedAbsenceType !== undefined &&
      selectedAbsenceType !== 'NO_ABSENCE'
    ) {
      await postAbsence(selectedAbsenceType)
      navigate(-1)
    }
  }, [navigate, postAbsence, selectedAbsenceType])

  const deleteAbsences = useCallback(async () => {
    if (deleteRange) {
      await deleteAbsenceRangeResult({
        unitId,
        childId,
        from: deleteRange.start,
        to: deleteRange.end
      })
    }
    setDeleteRange(undefined)
    void loadFutureAbsences()
    setUiMode('default')
  }, [childId, deleteRange, loadFutureAbsences, unitId])

  const cancelDelete = useCallback(() => {
    setDeleteRange(undefined)
    setUiMode('default')
  }, [])

  const goBack = useCallback(() => {
    navigate(-1)
  }, [navigate])

  return (
    <>
      <TallContentArea
        opaque={false}
        paddingHorizontal="zero"
        paddingVertical="zero"
      >
        {renderResult(child, (child) => (
          <>
            <div>
              <ChildNameBackButton
                child={child}
                onClick={() => {
                  if (selectedAbsenceType && canSave) {
                    setUiMode('confirmExit')
                  } else {
                    goBack()
                  }
                }}
              />
            </div>
            <ContentArea
              shadow
              opaque={true}
              paddingHorizontal="s"
              paddingVertical="m"
            >
              <AbsenceWrapper>
                <CustomTitle>
                  {i18n.attendances.actions.markAbsentBeforehand}
                </CustomTitle>
                <Gap size="m" />
                <FixedSpaceRow spacing="xxs" alignItems="flex-end">
                  <TimeInputWrapper>
                    <Label>{i18n.common.starts}</Label>
                    <TimeInput
                      data-qa="start-date-input"
                      value={startDate}
                      type="date"
                      onChange={setStartDate}
                      width="s"
                      info={
                        isBefore(new Date(startDate), mockNow() ?? new Date())
                          ? {
                              text: i18n.absences.chooseStartDate,
                              status: 'warning'
                            }
                          : isBefore(new Date(endDate), new Date(startDate))
                            ? {
                                text: i18n.absences.startBeforeEnd,
                                status: 'warning'
                              }
                            : undefined
                      }
                    />
                  </TimeInputWrapper>
                  <Separator>-</Separator>
                  <TimeInputWrapper>
                    <Label>{i18n.common.ends}</Label>
                    <TimeInput
                      data-qa="end-date-input"
                      value={endDate}
                      type="date"
                      onChange={setEndDate}
                      width="s"
                    />
                  </TimeInputWrapper>
                </FixedSpaceRow>
                <Gap size="L" />
                <FixedSpaceColumn spacing="s">
                  <Label>{i18n.absences.reason}</Label>
                  <AbsenceSelector
                    absenceTypes={[
                      'OTHER_ABSENCE',
                      'SICKLEAVE',
                      'PLANNED_ABSENCE'
                    ]}
                    selectedAbsenceType={selectedAbsenceType}
                    setSelectedAbsenceType={setSelectedAbsenceType}
                  />
                </FixedSpaceColumn>
              </AbsenceWrapper>
              <P>{i18n.absences.fullDayHint}</P>
              <Actions>
                <FixedSpaceRow fullWidth>
                  <Button text={i18n.common.cancel} onClick={goBack} />
                  {selectedAbsenceType !== undefined &&
                  selectedAbsenceType !== 'NO_ABSENCE' &&
                  canSave ? (
                    <AsyncButton
                      primary
                      text={i18n.common.confirm}
                      onClick={() => postAbsence(selectedAbsenceType)}
                      onSuccess={goBack}
                      data-qa="mark-absent-btn"
                    />
                  ) : (
                    <Button
                      primary
                      text={i18n.common.confirm}
                      disabled={true}
                    />
                  )}
                </FixedSpaceRow>
              </Actions>
              <HorizontalLine />

              {renderResult(absences, (absences) =>
                absences.length > 0 ? (
                  <>
                    <Title size={2} smaller primary>
                      {i18n.absences.futureAbsence}
                    </Title>
                    <FixedSpaceColumn>
                      {groupAbsencesByDateRange(absences).map(
                        (absenceRange) => {
                          if (absenceRange.durationInDays() > 1) {
                            return (
                              <AbsenceRow
                                key={absenceRange.start.format()}
                                data-qa="absence-row"
                              >
                                <AbsenceDate>{`${absenceRange.start.format()} - ${absenceRange.end.format()}`}</AbsenceDate>
                                <IconButton
                                  icon={faTrash}
                                  onClick={() => {
                                    setDeleteRange(absenceRange)
                                    setUiMode('confirmDelete')
                                  }}
                                  data-qa="delete-absence-period"
                                  aria-label={i18n.common.remove}
                                />
                              </AbsenceRow>
                            )
                          } else {
                            return (
                              <AbsenceRow
                                key={absenceRange.start.format()}
                                data-qa="absence-row"
                              >
                                <AbsenceDate>
                                  {absenceRange.start.format()}
                                </AbsenceDate>
                                <IconButton
                                  icon={faTrash}
                                  onClick={() => {
                                    setDeleteRange(absenceRange)
                                    setUiMode('confirmDelete')
                                  }}
                                  data-qa="delete-absence-period"
                                  aria-label={i18n.common.remove}
                                />
                              </AbsenceRow>
                            )
                          }
                        }
                      )}
                    </FixedSpaceColumn>
                  </>
                ) : null
              )}
            </ContentArea>
          </>
        ))}
      </TallContentArea>
      {uiMode === `confirmDelete` && (
        <DeleteAbsencesModal
          onConfirm={deleteAbsences}
          onCancel={cancelDelete}
        />
      )}
      {uiMode === `confirmExit` && (
        <ConfirmExitModal
          disabled={!selectedAbsenceType}
          onConfirm={createAbsence}
          onCancel={goBack}
        />
      )}
    </>
  )
})

const DeleteAbsencesModal = React.memo(function DeleteAbsencesModal({
  onConfirm,
  onCancel
}: {
  onConfirm: () => void
  onCancel: () => void
}) {
  const { i18n } = useTranslation()
  return (
    <InfoModal
      type="warning"
      title={i18n.absences.confirmDelete}
      icon={faExclamation}
      reject={{
        action: onCancel,
        label: i18n.common.doNotRemove
      }}
      resolve={{
        action: onConfirm,
        label: i18n.common.remove
      }}
    />
  )
})

const ConfirmExitModal = React.memo(function ConfirmExitModal({
  disabled,
  onConfirm,
  onCancel
}: {
  disabled: boolean
  onConfirm: () => void
  onCancel: () => void
}) {
  const { i18n } = useTranslation()
  return (
    <InfoModal
      type="warning"
      title={i18n.common.saveBeforeClosing}
      icon={faExclamation}
      reject={{
        action: onCancel,
        label: i18n.common.doNotSave
      }}
      resolve={{
        action: onConfirm,
        label: i18n.common.save,
        disabled
      }}
    />
  )
})

const AbsenceRow = styled.div`
  display: flex;
  flex-direction: row;
  justify-content: space-between;
`

const AbsenceDate = styled.span`
  font-weight: ${fontWeights.semibold};
  font-size: 16px;
  margin-bottom: 4px;
`

const AbsenceWrapper = styled.div`
  display: flex;
  flex-direction: column;
`

const TimeInput = styled(InputField)`
  width: calc(50vw - 32px);
  max-width: 180px;
`

const TimeInputWrapper = styled.span`
  div.warning {
    position: fixed;
  }
`

const Separator = styled.span`
  padding-bottom: 5px;
`
