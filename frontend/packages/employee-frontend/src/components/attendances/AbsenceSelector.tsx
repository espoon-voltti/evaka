// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Fragment, useState } from 'react'
import { useHistory } from 'react-router-dom'
import styled from 'styled-components'

import { Result } from '@evaka/lib-common/src/api'
import { AttendanceResponse } from '~api/attendances'
import AsyncButton from '@evaka/lib-components/src/atoms/buttons/AsyncButton'
import Button from '@evaka/lib-components/src/atoms/buttons/Button'
import colors from '@evaka/lib-components/src/colors'
import { useTranslation } from '~state/i18n'
import { AbsenceType, AbsenceTypes } from '~types/absence'
import { CustomButton, Flex } from './components'

const Actions = styled(Flex)`
  width: 100%;
`

interface Props {
  selectAbsenceType: (type: AbsenceType) => Promise<Result<AttendanceResponse>>
}

export default function AbsenceSelector({ selectAbsenceType }: Props) {
  const history = useHistory()
  const { i18n } = useTranslation()
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
                ? colors.blues.medium
                : colors.blues.light
            }
            borderColor={
              absenceType === selectedAbsenceType
                ? colors.blues.medium
                : colors.blues.light
            }
            color={
              absenceType === selectedAbsenceType
                ? colors.greyscale.white
                : colors.blues.dark
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
          <Actions>
            <Button
              text={i18n.common.cancel}
              onClick={() => history.goBack()}
            />
            <AsyncButton
              primary
              text={i18n.common.confirm}
              onClick={() =>
                selectAbsenceType(selectedAbsenceType).then((result) => {
                  if (result.isFailure) throw Error(result.message)
                  return undefined
                })
              }
              onSuccess={() => history.goBack()}
              data-qa="mark-present"
            />
          </Actions>
        </Fragment>
      )}
    </Fragment>
  )
}
