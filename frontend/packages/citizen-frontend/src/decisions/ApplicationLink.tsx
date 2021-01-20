// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { Link } from 'react-router-dom'
import { noop } from 'lodash'
import InlineButton from '@evaka/lib-components/src/atoms/buttons/InlineButton'
import { faFileAlt } from '@evaka/lib-icons'
import { useTranslation } from '~localization'

export const ApplicationLink = React.memo(function ApplicationLink({
  applicationId
}: {
  applicationId: string
}) {
  const t = useTranslation()

  return (
    <Link to={`/applications/${applicationId}/edit`}>
      <InlineButton
        icon={faFileAlt}
        text={t.applicationsList.openApplicationLink}
        onClick={noop}
        dataQa={`button-open-application-${applicationId}`}
      />
    </Link>
  )
})
