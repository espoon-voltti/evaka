// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useEffect, useState } from 'react'
import { useHistory, useParams } from 'react-router-dom'
import styled from 'styled-components'

import { faArrowLeft } from 'lib-icons'
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

import { TallContentArea } from '../../../components/mobile/components'
import { AttendanceUIContext } from '../../../state/attendance-ui'
import {
  getDaycareAttendances,
  getFutureAbsencesByChild,
  postAbsenceRange
} from '../../../api/attendances'
import { useTranslation } from '../../../state/i18n'
import { AbsenceType } from '../../../types'
import AbsenceSelector from '../AbsenceSelector'
import { CustomTitle, Actions, BackButtonInline } from '../components'
import HorizontalLine from 'lib-components/atoms/HorizontalLine'
import { Label, P } from 'lib-components/typography'
import InputField from 'lib-components/atoms/form/InputField'
import LocalDate from 'lib-common/local-date'
import { addDays, isAfter, isBefore, subDays } from 'date-fns'
import { Loading, Result } from 'lib-common/api'

export default React.memo(function MarkAbsentBeforehand() {
  const history = useHistory()
  const { i18n } = useTranslation()

  const { attendanceResponse, setAttendanceResponse } = useContext(
    AttendanceUIContext
  )

  const [selectedAbsenceType, setSelectedAbsenceType] = useState<
    AbsenceType | undefined
  >(undefined)
  const [, setAbsences] = useState<Result<Absence[]>>(Loading.of())

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
          <BackButtonInline
            onClick={() => history.goBack()}
            icon={faArrowLeft}
            text={
              child ? `${child.firstName} ${child.lastName}` : i18n.common.back
            }
          />
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
                {selectedAbsenceType &&
                isAfter(new Date(startDate), subDays(new Date(), 1)) &&
                isBefore(new Date(startDate), addDays(new Date(endDate), 1)) ? (
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
          </ContentArea>
        </TallContentArea>
      )}
    </>
  )
})

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
