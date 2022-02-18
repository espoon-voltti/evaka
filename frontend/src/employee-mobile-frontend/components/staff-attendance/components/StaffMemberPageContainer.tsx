// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { useHistory } from 'react-router-dom'
import styled from 'styled-components'

import colors from 'lib-customizations/common'
import { faArrowLeft } from 'lib-icons'

import { BackButton, TallContentArea } from '../../mobile/components'

const TallContentAreaNoOverflow = styled(TallContentArea)`
  overflow-x: hidden;
`
const BackButtonMargin = styled(BackButton)`
  margin-left: 8px;
  margin-top: 8px;
  z-index: 2;
`

const Shadow = styled.div`
  box-shadow: 0 4px 4px 0 ${colors.grayscale.g15};
  z-index: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  min-height: calc(100vh - 74px);
`

export function StaffMemberPageContainer({
  children
}: React.PropsWithChildren<unknown>) {
  const history = useHistory()
  return (
    <TallContentAreaNoOverflow
      opaque
      paddingHorizontal="0px"
      paddingVertical="0px"
      shadow
    >
      <BackButtonMargin
        onClick={() => history.goBack()}
        icon={faArrowLeft}
        data-qa="back-btn"
      />
      <Shadow>{children}</Shadow>
    </TallContentAreaNoOverflow>
  )
}
