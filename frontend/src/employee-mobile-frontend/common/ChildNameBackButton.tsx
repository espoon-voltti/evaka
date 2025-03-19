// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import { AttendanceChild } from 'lib-common/generated/api-types/attendance'
import { faArrowLeft } from 'lib-icons'

import { BackButtonInline } from './components'

interface Props {
  child: AttendanceChild
  onClick: () => void
}

export default React.memo(function ChildNameBackButton({
  child,
  onClick
}: Props) {
  const text =
    `${child.firstName} ${child.lastName}` +
    (child.preferredName ? ` (${child.preferredName})` : '')

  return (
    <BackButtonInline
      onClick={onClick}
      icon={faArrowLeft}
      text={text}
      data-qa="child-name-back-button"
    />
  )
})
