// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Fragment, useState } from 'react'
import { useHistory } from 'react-router-dom'
import { postChildAbsence } from '~api/absences'

import { childArrives, ChildInGroup } from '~api/unit'
import InputField from '~components/shared/atoms/form/InputField'
import Loader from '~components/shared/atoms/Loader'
import Colors from '~components/shared/Colors'
import { Gap } from '~components/shared/layout/white-space'
import { useTranslation } from '~state/i18n'
import { UUID } from '~types'
import { AbsenceType, AbsenceTypes } from '~types/absence'
import { DaycareGroup } from '~types/unit'
import { FlexLabel } from './AttendanceChildPage'
import { CustomButton, Flex } from './AttendanceGroupSelectorPage'

export const absenceBackgroundColours: { [key in AbsenceType]: string } = {
  UNKNOWN_ABSENCE: Colors.accents.green,
  OTHER_ABSENCE: Colors.blues.dark,
  SICKLEAVE: Colors.accents.violet,
  PLANNED_ABSENCE: Colors.blues.light,
  PARENTLEAVE: Colors.blues.primary,
  FORCE_MAJEURE: Colors.accents.red,
  TEMPORARY_RELOCATION: Colors.accents.orange,
  TEMPORARY_VISITOR: Colors.accents.yellow,
  PRESENCE: Colors.greyscale.white
}

export const absenceBorderColours: { [key in AbsenceType]: string } = {
  UNKNOWN_ABSENCE: Colors.accents.green,
  OTHER_ABSENCE: Colors.blues.dark,
  SICKLEAVE: Colors.accents.violet,
  PLANNED_ABSENCE: Colors.blues.light,
  PARENTLEAVE: Colors.blues.primary,
  FORCE_MAJEURE: Colors.accents.red,
  TEMPORARY_RELOCATION: Colors.accents.orange,
  TEMPORARY_VISITOR: Colors.accents.yellow,
  PRESENCE: Colors.greyscale.white
}

export const absenceColours: { [key in AbsenceType]: string } = {
  UNKNOWN_ABSENCE: Colors.greyscale.darkest,
  OTHER_ABSENCE: Colors.greyscale.white,
  SICKLEAVE: Colors.greyscale.white,
  PLANNED_ABSENCE: Colors.greyscale.darkest,
  PARENTLEAVE: Colors.greyscale.white,
  FORCE_MAJEURE: Colors.greyscale.white,
  TEMPORARY_RELOCATION: Colors.greyscale.white,
  TEMPORARY_VISITOR: Colors.greyscale.white,
  PRESENCE: Colors.greyscale.white
}

interface Props {
  unitId: UUID
  child: ChildInGroup
  group: DaycareGroup
  groupId: UUID | 'all'
}

export default React.memo(function AttendanceChildComing({
  unitId,
  child,
  group,
  groupId
}: Props) {
  const history = useHistory()
  const { i18n } = useTranslation()

  const [time, setTime] = useState<string>(
    new Date().getHours() < 10
      ? new Date().getMinutes() < 10
        ? `0${new Date().getHours()}:0${new Date().getMinutes()}`
        : `0${new Date().getHours()}:${new Date().getMinutes()}`
      : new Date().getMinutes() < 10
      ? `${new Date().getHours()}:0${new Date().getMinutes()}`
      : `${new Date().getHours()}:${new Date().getMinutes()}`
  )
  const [markAbsence, setMarkAbsence] = useState<boolean>(false)

  async function markPresent() {
    const hours = parseInt(time.slice(0, 2))
    const minutes = parseInt(time.slice(3, 5))
    const today = new Date()
    today.setHours(hours)
    today.setMinutes(minutes)
    await childArrives(child.childId, today)
    history.push(`/units/${unitId}/attendance/${groupId}/present`)
  }

  async function selectAbsenceType(absenceType: AbsenceType) {
    const hours = parseInt(time.slice(0, 2))
    const minutes = parseInt(time.slice(3, 5))
    const today = new Date()
    today.setHours(hours)
    today.setMinutes(minutes)
    await postChildAbsence(absenceType, 'PRESCHOOL', child.childId)
    history.push(`/units/${unitId}/attendance/${groupId}/absent`)
  }

  return (
    <Fragment>
      {markAbsence ? (
        child && group ? (
          <Fragment>
            <Gap size={'s'} />
            <Flex>
              {AbsenceTypes.filter(
                (absenceType) => absenceType !== 'PRESENCE'
              ).map((absenceType) => (
                <CustomButton
                  backgroundColor={absenceBackgroundColours[absenceType]}
                  borderColor={absenceBorderColours[absenceType]}
                  color={absenceColours[absenceType]}
                  key={absenceType}
                  text={i18n.absences.absenceTypes[absenceType]}
                  onClick={() => selectAbsenceType(absenceType)}
                />
              ))}
            </Flex>
          </Fragment>
        ) : (
          <Loader />
        )
      ) : (
        <Fragment>
          <Flex>
            <CustomButton
              text={i18n.attendances.actions.markAbsent}
              onClick={() => setMarkAbsence(!markAbsence)}
            />

            <CustomButton
              primary
              text={i18n.attendances.actions.markPresent}
              onClick={markPresent}
            />
          </Flex>
          <FlexLabel>
            <span>{i18n.attendances.timeLabel}</span>
            <InputField onChange={setTime} value={time} width="s" type="time" />
          </FlexLabel>
        </Fragment>
      )}
    </Fragment>
  )
})
