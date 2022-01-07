// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { useHistory } from 'react-router'
import styled from 'styled-components'
import { UUID } from 'lib-common/types'
import Button from 'lib-components/atoms/buttons/Button'
import { useTranslation } from '../../../state/i18n'

export const LeaveVasuPageButton = React.memo(function LeaveVasuPageButton({
  childId,
  disabled = false
}: {
  childId: UUID
  disabled?: boolean
}) {
  const history = useHistory()
  const { i18n } = useTranslation()

  return (
    <ExitButtonWrapper>
      <Button
        text={i18n.vasu.leavePage}
        onClick={() => history.push(`/child-information/${childId}`)}
        disabled={disabled}
      />
    </ExitButtonWrapper>
  )
})

const ExitButtonWrapper = styled.div`
  flex: 1;
`
