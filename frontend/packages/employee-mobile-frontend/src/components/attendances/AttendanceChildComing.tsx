// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Fragment, useState } from 'react'

import Loader from '@evaka/lib-components/src/atoms/Loader'
import { FixedSpaceColumn } from '@evaka/lib-components/src/layout/flex-helpers'
import { Gap } from '@evaka/lib-components/src/white-space'

import { useTranslation } from '../../state/i18n'
import { AbsenceType } from '../../types'
import { AttendanceChild, postFullDayAbsence } from '../../api/attendances'
import AbsenceSelector from './AbsenceSelector'
import { WideInlineButton } from './components'
import { WideLinkButton } from '../../components/mobile/components'

interface Props {
  unitId: string
  child: AttendanceChild
  groupIdOrAll: string | 'all'
}

export default React.memo(function AttendanceChildComing({
  unitId,
  child,
  groupIdOrAll
}: Props) {
  const { i18n } = useTranslation()

  const [markAbsence, setMarkAbsence] = useState<boolean>(false)

  async function selectAbsenceType(absenceType: AbsenceType) {
    return postFullDayAbsence(unitId, child.id, absenceType)
  }

  return (
    <Fragment>
      {markAbsence &&
        (child ? (
          <Fragment>
            <Gap size={'s'} />

            <AbsenceSelector selectAbsenceType={selectAbsenceType} />
          </Fragment>
        ) : (
          <Loader />
        ))}

      {!markAbsence && (
        <Fragment>
          <FixedSpaceColumn>
            <WideLinkButton
              data-qa="mark-present"
              to={`/units/${unitId}/groups/${groupIdOrAll}/childattendance/${child.id}/markpresent`}
            >
              {i18n.attendances.actions.markPresent}
            </WideLinkButton>

            <WideInlineButton
              text={i18n.attendances.actions.markAbsent}
              onClick={() => setMarkAbsence(true)}
            />
          </FixedSpaceColumn>
          <Gap size={'L'} />
        </Fragment>
      )}
    </Fragment>
  )
})
