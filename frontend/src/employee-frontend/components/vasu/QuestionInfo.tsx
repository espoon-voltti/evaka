// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import ExpandingInfo from 'lib-components/molecules/ExpandingInfo'
import { useTranslation } from '../../state/i18n'

interface Props {
  children: React.ReactNode
  info: string | null
}

export default React.memo(function QuestionInfo({ children, info }: Props) {
  const { i18n } = useTranslation()
  if (info) {
    return (
      <ExpandingInfo
        info={<div>{info}</div>}
        ariaLabel={i18n.common.openExpandingInfo}
      >
        {children}
      </ExpandingInfo>
    )
  }

  return <>{children}</>
})
