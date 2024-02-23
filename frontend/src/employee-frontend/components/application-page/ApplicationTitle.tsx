// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

import { ApplicationDetails } from 'lib-common/generated/api-types/application'
import { H1, H2 } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import { colors } from 'lib-customizations/common'

import { useTranslation } from '../../state/i18n'
import { formatName } from '../../utils'
import CircularLabel from '../common/CircularLabel'

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
    <div data-qa="application-title">
      <TitleRow>
        <TitleWrapper>
          <H1 noMargin>{i18n.application.types[application.type]}</H1>
          {titleLabels.length > 0 && (
            <span className="title-labels">({titleLabels.join(', ')})</span>
          )}
        </TitleWrapper>
        {application.guardianDateOfDeath ? (
          <CircularLabel
            color="white"
            background="black"
            text={`${
              i18n.application.person.applicantDead
            } ${application.guardianDateOfDeath.format()}`}
            data-qa="applicant-dead"
          />
        ) : null}
      </TitleRow>
      <H2>
        {formatName(
          application.form.child.person.firstName,
          application.form.child.person.lastName,
          i18n,
          true
        )}
      </H2>
    </div>
  )
})

const TitleRow = styled.div`
  margin: ${defaultMargins.m} 0;
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  align-items: center;

  .title-labels {
    color: ${colors.grayscale.g35};
    margin-left: ${defaultMargins.m};
    white-space: nowrap;
  }
`

const TitleWrapper = styled.div`
  display: flex;
  flex-wrap: wrap;
  flex-direction: row;
  align-items: baseline;
  justify-content: flex-start;
`
