// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useEffect, useState } from 'react'
import { useHistory, useParams } from 'react-router-dom'
import styled from 'styled-components'
import { addDays, isAfter, isBefore, subDays } from 'date-fns'

import { faArrowLeft, faExclamation, faTrash } from 'lib-icons'
import Loader from 'lib-components/atoms/Loader'
import { useRestApi } from 'lib-common/utils/useRestApi'
import { Gap } from 'lib-components/white-space'
import AsyncButton from 'lib-components/atoms/buttons/AsyncButton'
import Button from 'lib-components/atoms/buttons/Button'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import ErrorSegment from 'lib-components/atoms/state/ErrorSegment'
import { ContentArea } from 'lib-components/layout/Container'
import { Absence } from 'lib-common/api-types/child/Absences'
import HorizontalLine from 'lib-components/atoms/HorizontalLine'
import { Label, P } from 'lib-components/typography'
import InputField from 'lib-components/atoms/form/InputField'
import LocalDate from 'lib-common/local-date'
import { Loading, Result } from 'lib-common/api'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import InfoModal from 'lib-components/molecules/modals/InfoModal'

import { TallContentArea } from '../../../components/mobile/components'
import { AttendanceUIContext } from '../../../state/attendance-ui'
import {
  deleteAbsenceRange,
  getDaycareAttendances,
  getFutureAbsencesByChild,
  postAbsenceRange
} from '../../../api/attendances'
import { useTranslation } from '../../../state/i18n'
import { AbsenceType } from '../../../types'
import AbsenceSelector from '../AbsenceSelector'
import { CustomTitle, Actions, BackButtonInline } from '../components'
import FiniteDateRange from 'lib-common/finite-date-range'
import Title from 'lib-components/atoms/Title'
import { groupAbsencesByDateRange } from 'lib-common/utils/absences'

export default React.memo(function MarkAbsentBeforehand() {
  const history = useHistory()
  const { i18n } = useTranslation()

  const { attendanceResponse, setAttendanceResponse } = useContext(
    AttendanceUIContext
  )

  const [selectedAbsenceType, setSelectedAbsenceType] = useState<
    AbsenceType | undefined
  >(undefined)
  const [absences, setAbsences] = useState<Result<Absence[]>>(Loading.of())
  const [uiMode, setUiMode] = useState<
    'default' | 'confirmDelete' | 'confirmExit'
  >('default')
  const [deleteRange, setDeleteRange] = useState<FiniteDateRange | undefined>(
    undefined
  )

  const { childId, unitId } = useParams<{
    unitId: string
    childId: string
  }>()

  const [startDate, setStartDate] = useState<string>('')
  const [endDate, setEndDate] = useState<string>('')

  const loadDaycareAttendances = useRestApi(
    getDaycareAttendances,
    setAttendanceResponse
  )

  const loadFutureAbsences = useRestApi(getFutureAbsencesByChild, setAbsences)

  useEffect(() => {
    loadDaycareAttendances(unitId)
  }, [loadDaycareAttendances, unitId])

  useEffect(() => {
    loadFutureAbsences(childId)
  }, [loadFutureAbsences, childId])

  async function postAbsence(absenceType: AbsenceType) {
    return postAbsenceRange(
      unitId,
      childId,
      absenceType,
      LocalDate.parseIso(startDate),
      LocalDate.parseIso(endDate)
    )
  }

  async function deleteAbsences() {
    if (deleteRange) {
      await deleteAbsenceRange(unitId, childId, deleteRange)
    }
    setDeleteRange(undefined)
    loadFutureAbsences(childId)
    setUiMode('default')
  }

  function DeleteAbsencesModal() {
    return (
      <InfoModal
        iconColour={'orange'}
        title={i18n.absences.confirmDelete}
        icon={faExclamation}
        reject={{
          action: () => {
            setDeleteRange(undefined)
            setUiMode('default')
          },
          label: i18n.common.doNotRemove
        }}
        resolve={{
          action: () => {
            void deleteAbsences()
          },
          label: i18n.common.remove
        }}
      />
    )
  }

  function ConfirmExitModal() {
    return (
      <InfoModal
        iconColour={'orange'}
        title={i18n.common.saveBeforeClosing}
        icon={faExclamation}
        reject={{
          action: () => {
            history.goBack()
          },
          label: i18n.common.doNotSave
        }}
        resolve={
          selectedAbsenceType && {
            action: () => {
              void postAbsence(selectedAbsenceType).then(() => {
                history.goBack()
              })
            },
            label: i18n.common.save
          }
        }
      />
    )
  }

  function canSave() {
    return (
      isAfter(new Date(startDate), subDays(new Date(), 1)) &&
      isBefore(new Date(startDate), addDays(new Date(endDate), 1))
    )
  }

  const child =
    attendanceResponse.isSuccess &&
    attendanceResponse.value.children.find((ac) => ac.id === childId)

  return (
    <>
      {attendanceResponse.isLoading && <Loader />}
      {attendanceResponse.isFailure && <ErrorSegment />}
      {attendanceResponse.isSuccess && (
        <TallContentArea
          opaque={false}
          paddingHorizontal={'zero'}
          paddingVertical={'zero'}
        >
          <div>
            <BackButtonInline
              onClick={() => {
                if (selectedAbsenceType && canSave()) {
                  setUiMode('confirmExit')
                } else {
                  history.goBack()
                }
              }}
              icon={faArrowLeft}
              text={
                child
                  ? `${child.firstName} ${child.lastName}`
                  : i18n.common.back
              }
            />
          </div>
          <ContentArea
            shadow
            opaque={true}
            paddingHorizontal={'s'}
            paddingVertical={'m'}
          >
            <AbsenceWrapper>
              <CustomTitle>
                {i18n.attendances.actions.markAbsentBeforehand}
              </CustomTitle>
              <Gap size={'m'} />
              <FixedSpaceRow spacing={'xxs'} alignItems={'flex-end'}>
                <TimeInputWrapper>
                  <Label>{i18n.common.starts}</Label>
                  <TimeInput
                    data-qa="start-date-input"
                    value={startDate}
                    type="date"
                    onChange={setStartDate}
                    width={'s'}
                    info={
                      isBefore(new Date(startDate), new Date())
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
                    width={'s'}
                  />
                </TimeInputWrapper>
              </FixedSpaceRow>
              <Gap size={'L'} />
              <FixedSpaceColumn spacing={'s'}>
                <Label>{i18n.absences.reason}</Label>
                <AbsenceSelector
                  selectedAbsenceType={selectedAbsenceType}
                  setSelectedAbsenceType={setSelectedAbsenceType}
                  noUnknownAbsences
                />
              </FixedSpaceColumn>
            </AbsenceWrapper>
            <P>{i18n.absences.fullDayHint}</P>
            <Actions>
              <FixedSpaceRow fullWidth>
                <Button
                  text={i18n.common.cancel}
                  onClick={() => history.goBack()}
                />
                {selectedAbsenceType && canSave() ? (
                  <AsyncButton
                    primary
                    text={i18n.common.confirm}
                    onClick={() => postAbsence(selectedAbsenceType)}
                    onSuccess={() => history.goBack()}
                    data-qa="mark-absent-btn"
                  />
                ) : (
                  <Button primary text={i18n.common.confirm} disabled={true} />
                )}
              </FixedSpaceRow>
            </Actions>
            <HorizontalLine />

            {absences.isSuccess && absences.value.length > 0 && (
              <>
                <Title size={2} smaller primary>
                  {i18n.absences.futureAbsence}
                </Title>
                <FixedSpaceColumn>
                  {groupAbsencesByDateRange(absences.value).map(
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
                            />
                          </AbsenceRow>
                        )
                      } else {
                        return (
                          <AbsenceRow
                            key={absenceRange.start.format()}
                            data-qa="absence-row"
                          >
                            <AbsenceDate>{`${absenceRange.start.format()}`}</AbsenceDate>
                            <IconButton
                              icon={faTrash}
                              onClick={() => {
                                setDeleteRange(absenceRange)
                                setUiMode('confirmDelete')
                              }}
                              data-qa="delete-absence-period"
                            />
                          </AbsenceRow>
                        )
                      }
                    }
                  )}
                </FixedSpaceColumn>
              </>
            )}
          </ContentArea>
        </TallContentArea>
      )}
      {uiMode === `confirmDelete` && <DeleteAbsencesModal />}
      {uiMode === `confirmExit` && <ConfirmExitModal />}
    </>
  )
})

const AbsenceRow = styled.div`
  display: flex;
  flex-direction: row;
  justify-content: space-between;
`

const AbsenceDate = styled.span`
  font-weight: 600;
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
