// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useContext, useMemo, useState } from 'react'
import { useHistory, useParams } from 'react-router-dom'
import styled from 'styled-components'
import { combine } from 'lib-common/api'
import RoundIcon from 'lib-components/atoms/RoundIcon'
import AsyncButton from 'lib-components/atoms/buttons/AsyncButton'
import Button from 'lib-components/atoms/buttons/Button'
import { ContentArea } from 'lib-components/layout/Container'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { faArrowLeft, farStickyNote } from 'lib-icons'

import { postFullDayAbsence } from '../../../api/attendances'
import { ChildAttendanceContext } from '../../../state/child-attendance'
import { useTranslation } from '../../../state/i18n'
import { AbsenceType } from '../../../types'
import { renderResult } from '../../async-rendering'
import { TallContentArea } from '../../mobile/components'
import AbsenceSelector from '../AbsenceSelector'
import {
  Actions,
  BackButtonInline,
  CustomTitle,
  DailyNotes
} from '../components'
import DailyNote from '../notes/DailyNote'

export default React.memo(function MarkAbsent() {
  const history = useHistory()
  const { i18n } = useTranslation()

  const { attendanceResponse, reloadAttendances } = useContext(
    ChildAttendanceContext
  )

  const [selectedAbsenceType, setSelectedAbsenceType] = useState<
    AbsenceType | undefined
  >(undefined)

  const { childId, unitId, groupId } = useParams<{
    unitId: string
    groupId: string
    childId: string
  }>()

  const postAbsence = useCallback(
    (absenceType: AbsenceType) =>
      postFullDayAbsence(unitId, childId, absenceType),
    [childId, unitId]
  )

  const child = useMemo(
    () =>
      attendanceResponse.map((attendance) =>
        attendance.children.find((ac) => ac.id === childId)
      ),
    [attendanceResponse, childId]
  )

  const groupNote = useMemo(
    () =>
      attendanceResponse.map((attendance) =>
        attendance.groupNotes.find((g) => g.groupId === groupId)
      ),
    [attendanceResponse, groupId]
  )

  return (
    <TallContentArea
      opaque={false}
      paddingHorizontal="zero"
      paddingVertical="zero"
    >
      {renderResult(combine(child, groupNote), ([child, groupNote]) => (
        <>
          <BackButtonInline
            onClick={() => history.go(-2)}
            icon={faArrowLeft}
            text={
              child ? `${child.firstName} ${child.lastName}` : i18n.common.back
            }
          />
          <ContentArea
            shadow
            opaque={true}
            paddingHorizontal="s"
            paddingVertical="m"
          >
            <AbsenceWrapper>
              <CustomTitle>{i18n.attendances.actions.markAbsent}</CustomTitle>
              <Gap size="m" />
              <FixedSpaceColumn spacing="s">
                <AbsenceSelector
                  selectedAbsenceType={selectedAbsenceType}
                  setSelectedAbsenceType={setSelectedAbsenceType}
                />
              </FixedSpaceColumn>
            </AbsenceWrapper>
            <Gap size="m" />
            <Actions>
              <FixedSpaceRow fullWidth>
                <Button
                  text={i18n.common.cancel}
                  onClick={() => history.goBack()}
                />
                {selectedAbsenceType ? (
                  <AsyncButton
                    primary
                    text={i18n.common.confirm}
                    onClick={() => postAbsence(selectedAbsenceType)}
                    onSuccess={() => {
                      reloadAttendances()
                      history.goBack()
                    }}
                    data-qa="mark-absent-btn"
                  />
                ) : (
                  <Button primary text={i18n.common.confirm} disabled={true} />
                )}
              </FixedSpaceRow>
            </Actions>
          </ContentArea>
          <Gap size="s" />
          <ContentArea
            shadow
            opaque={true}
            paddingHorizontal="s"
            paddingVertical="s"
            blue
          >
            <DailyNotes>
              <span>
                <RoundIcon
                  content={farStickyNote}
                  color={colors.main.dark}
                  size="m"
                />
              </span>
              <DailyNote
                child={child ? child : undefined}
                groupNote={groupNote ? groupNote : undefined}
              />
            </DailyNotes>
          </ContentArea>
        </>
      ))}
    </TallContentArea>
  )
})

const AbsenceWrapper = styled.div`
  display: flex;
  flex-direction: column;
`
