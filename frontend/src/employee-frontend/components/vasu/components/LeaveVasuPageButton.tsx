// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { useNavigate } from 'react-router-dom'
import styled from 'styled-components'

import { UUID } from 'lib-common/types'
import { Button } from 'lib-components/atoms/buttons/Button'

import { useTranslation } from '../../../state/i18n'

export const LeaveVasuPageButton = React.memo(function LeaveVasuPageButton({
  childId,
  disabled = false
}: {
  childId: UUID
  disabled?: boolean
}) {
  const navigate = useNavigate()
  const { i18n } = useTranslation()

  return (
    <ExitButtonWrapper>
      <Button
        text={i18n.vasu.leavePage}
        onClick={() => navigate(`/child-information/${childId}`)}
        disabled={disabled}
        data-qa="back-button"
      />
    </ExitButtonWrapper>
  )
})

const ExitButtonWrapper = styled.div`
  flex: 1;
`
