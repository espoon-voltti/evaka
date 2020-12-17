// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'
import { H1, H2 } from '~components/shared/Typography'
import { defaultMargins } from '@evaka/lib-components/src/white-space'
import { greyscale } from '@evaka/lib-components/src/colors'
import { useTranslation } from '~state/i18n'
import { ApplicationDetails } from '~types/application'
import { formatName } from '~utils'

type Props = {
  application: ApplicationDetails
}

export default React.memo(function ApplicationTitle({ application }: Props) {
  const { i18n } = useTranslation()

  const titleLabels: string[] = []
  if (application.origin === 'PAPER')
    titleLabels.push(i18n.application.origins.PAPER)
  if (application.transferApplication)
    titleLabels.push(i18n.application.transfer)

  return (
    <>
      <TitleRow>
        <H1>{i18n.application.types[application.type]}</H1>
        {titleLabels.length > 0 && (
          <span className="title-labels">({titleLabels.join(', ')})</span>
        )}
      </TitleRow>
      <H2>
        {formatName(
          application.form.child.person.firstName,
          application.form.child.person.lastName,
          i18n,
          true
        )}
      </H2>
    </>
  )
})

const TitleRow = styled.div`
  display: flex;
  align-items: baseline;

  .title-labels {
    color: ${greyscale.medium};
    margin-left: ${defaultMargins.m};
  }
`
