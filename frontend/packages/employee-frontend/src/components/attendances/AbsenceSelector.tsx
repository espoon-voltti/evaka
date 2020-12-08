// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Fragment, useContext, useState } from 'react'
import { useHistory } from 'react-router-dom'
import styled from 'styled-components'

import { Result } from '~api'
import { AttendanceResponse, getDaycareAttendances } from '~api/attendances'
import AsyncButton from '~components/shared/atoms/buttons/AsyncButton'
import Button from '~components/shared/atoms/buttons/Button'
import Colors from '~components/shared/Colors'
import { Gap } from '~components/shared/layout/white-space'
import { AttendanceUIContext } from '~state/attendance-ui'
import { useTranslation } from '~state/i18n'
import { UUID } from '~types'
import { AbsenceType, AbsenceTypes } from '~types/absence'
import { CustomButton, Flex } from './components'

const Actions = styled(Flex)`
  width: 100%;
`

interface Props {
  unitId: UUID
  groupId: UUID | 'all'
  selectAbsenceType: (type: AbsenceType) => Promise<Result<AttendanceResponse>>
}

export default function AbsenceSelector({
  unitId,
  groupId: groupIdOrAll,
  selectAbsenceType
}: Props) {
  const history = useHistory()
  const { i18n } = useTranslation()
  const { filterAndSetAttendanceResponse } = useContext(AttendanceUIContext)
  const [selectedAbsenceType, setSelectedAbsenceType] = useState<
    AbsenceType | undefined
  >(undefined)

  return (
    <Fragment>
      <Flex>
        {AbsenceTypes.filter(
          (absenceType) =>
            absenceType !== 'PRESENCE' &&
            absenceType !== 'PARENTLEAVE' &&
            absenceType !== 'FORCE_MAJEURE'
        ).map((absenceType) => (
          <CustomButton
            backgroundColor={
              absenceType === selectedAbsenceType
                ? Colors.blues.medium
                : Colors.blues.light
            }
            borderColor={
              absenceType === selectedAbsenceType
                ? Colors.blues.medium
                : Colors.blues.light
            }
            color={
              absenceType === selectedAbsenceType
                ? Colors.greyscale.white
                : Colors.blues.dark
            }
            key={absenceType}
            text={i18n.absences.absenceTypes[absenceType]}
            onClick={() => setSelectedAbsenceType(absenceType)}
            data-qa={`mark-absent-${absenceType}`}
          />
        ))}
      </Flex>
      {selectedAbsenceType && (
        <Fragment>
          <Gap size={'s'} />
          <Actions>
            <Button
              text={i18n.common.cancel}
              onClick={() => history.goBack()}
            />
            <AsyncButton
              primary
              text={i18n.common.confirm}
              onClick={() => selectAbsenceType(selectedAbsenceType)}
              onSuccess={async () => {
                await getDaycareAttendances(unitId).then((res) =>
                  filterAndSetAttendanceResponse(res, groupIdOrAll)
                )
                history.goBack()
              }}
              data-qa="mark-present"
            />
          </Actions>
        </Fragment>
      )}
    </Fragment>
  )
}
