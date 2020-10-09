// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { RouteComponentProps, withRouter } from 'react-router-dom'
import styled from 'styled-components'
import { faAngleLeft } from 'icon-set'
import { useTranslation } from '~state/i18n'
import InlineButton from 'components/shared/atoms/buttons/InlineButton'

const ReturnButtonWrapper = styled.div`
  margin-top: 32px;
  margin-bottom: 8px;
  display: inline-block;

  button {
    padding-left: 0;
    margin-left: 0;
    justify-content: flex-start;
  }
`

interface Props {
  dataQa?: string
}

function ReturnButtonPlain({ history, dataQa }: Props & RouteComponentProps) {
  const { i18n } = useTranslation()

  if (history.length > 1) {
    return (
      <ReturnButtonWrapper>
        <InlineButton
          icon={faAngleLeft}
          text={i18n.common.goBack}
          onClick={() => history.goBack()}
          dataQa={dataQa}
        />
      </ReturnButtonWrapper>
    )
  } else return null
}

const ReturnButton = withRouter(ReturnButtonPlain)

export default ReturnButton
