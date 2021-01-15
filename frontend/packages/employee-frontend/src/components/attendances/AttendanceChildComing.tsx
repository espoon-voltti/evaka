// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Fragment, useContext, useEffect, useState } from 'react'
import { useHistory } from 'react-router-dom'

import {
  AttendanceChild,
  childArrivesPOST,
  getDaycareAttendances,
  Group,
  postFullDayAbsence
} from '~api/attendances'
import InputField from '@evaka/lib-components/src/atoms/form/InputField'
import Loader from '@evaka/lib-components/src/atoms/Loader'
import { FixedSpaceColumn } from '@evaka/lib-components/src/layout/flex-helpers'
import { Gap } from '@evaka/lib-components/src/white-space'
import { AttendanceUIContext } from '~state/attendance-ui'
import { useTranslation } from '~state/i18n'
import { UUID } from '~types'
import { AbsenceType } from '~types/absence'
import AbsenceSelector from './AbsenceSelector'
import { getCurrentTime } from './AttendanceChildPage'
import {
  BigWideButton,
  BigWideInlineButton,
  WideAsyncButton,
  FlexLabel
} from './components'

interface Props {
  unitId: UUID
  child: AttendanceChild
  group: Group
  groupId: UUID | 'all'
}

export default React.memo(function AttendanceChildComing({
  unitId,
  child,
  group,
  groupId: groupIdOrAll
}: Props) {
  const history = useHistory()
  const { i18n } = useTranslation()

  const [time, setTime] = useState<string>(getCurrentTime())
  const [markAbsence, setMarkAbsence] = useState<boolean>(false)
  const [markPresent, setMarkPresent] = useState<boolean>(false)

  const { filterAndSetAttendanceResponse } = useContext(AttendanceUIContext)

  useEffect(() => {
    void getDaycareAttendances(unitId).then((res) =>
      filterAndSetAttendanceResponse(res, groupIdOrAll)
    )
  }, [])

  async function selectAbsenceType(absenceType: AbsenceType) {
    return postFullDayAbsence(unitId, child.id, absenceType)
  }

  function childArrives() {
    return childArrivesPOST(unitId, child.id, time)
  }

  return (
    <Fragment>
      {markAbsence &&
        (child && group ? (
          <Fragment>
            <Gap size={'s'} />

            <AbsenceSelector selectAbsenceType={selectAbsenceType} />
          </Fragment>
        ) : (
          <Loader />
        ))}

      {!markAbsence && !markPresent && (
        <Fragment>
          <FixedSpaceColumn>
            <BigWideButton
              primary
              text={i18n.attendances.actions.markPresent}
              onClick={() => {
                setTime(getCurrentTime())
                setMarkPresent(true)
              }}
              data-qa="mark-present"
            />

            <BigWideInlineButton
              text={i18n.attendances.actions.markAbsent}
              onClick={() => setMarkAbsence(true)}
            />
          </FixedSpaceColumn>
        </Fragment>
      )}

      {markPresent && (
        <Fragment>
          <FixedSpaceColumn>
            <FlexLabel>
              <span>{i18n.attendances.arrivalTime}</span>
              <InputField
                onChange={setTime}
                value={time}
                width="s"
                type="time"
                data-qa="set-time"
              />
            </FlexLabel>

            <WideAsyncButton
              primary
              text={i18n.common.confirm}
              onClick={() => childArrives()}
              onSuccess={async () => history.goBack()}
              data-qa="mark-present"
            />
          </FixedSpaceColumn>
        </Fragment>
      )}
    </Fragment>
  )
})
