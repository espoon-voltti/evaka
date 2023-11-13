// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { useNavigate } from 'react-router-dom'
import styled from 'styled-components'

import { useTranslation } from 'employee-mobile-frontend/common/i18n'
import colors from 'lib-customizations/common'
import { faArrowLeft } from 'lib-icons'

import { BackButton, TallContentArea } from '../../pairing/components'

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
  back,
  children
}: React.PropsWithChildren & { back?: string }) {
  const { i18n } = useTranslation()
  const navigate = useNavigate()

  return (
    <TallContentAreaNoOverflow
      opaque
      paddingHorizontal="0px"
      paddingVertical="0px"
      shadow
    >
      <BackButtonMargin
        onClick={() => (back !== undefined ? navigate(back) : navigate(-1))}
        icon={faArrowLeft}
        data-qa="back-btn"
        aria-label={i18n.common.close}
      />
      <Shadow>{children}</Shadow>
    </TallContentAreaNoOverflow>
  )
}
