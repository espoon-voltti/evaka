// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { AuthContext } from 'citizen-frontend/auth/state'
import HorizontalLine from 'lib-components/atoms/HorizontalLine'
import Container, { ContentArea } from 'lib-components/layout/Container'
import ListGrid from 'lib-components/layout/ListGrid'
import { H1, H2, Label } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import { fasExclamationTriangle } from 'lib-icons'
import React, { useContext } from 'react'
import { Redirect } from 'react-router'
import styled, { useTheme } from 'styled-components'
import { renderResult } from '../async-rendering'
import Footer from '../Footer'
import { useTranslation } from '../localization'

export default React.memo(function PersonalDetails() {
  const t = useTranslation()
  const { user } = useContext(AuthContext)

  return (
    <>
      <Container>
        <Gap size="L" />
        <ContentArea opaque paddingVertical="L">
          <H1 noMargin>{t.personalDetails.title}</H1>
          {t.personalDetails.description}
          {renderResult(user, (personalData) => {
            if (personalData === undefined) {
              return <Redirect to="/" />
            }

            const {
              firstName,
              lastName,
              streetAddress,
              postalCode,
              postOffice,
              phone,
              backupPhone,
              email
            } = personalData

            return (
              <>
                <HorizontalLine />
                <ListGrid rowGap="s" columnGap="L" labelWidth="max-content">
                  <H2 noMargin>{t.personalDetails.personalInfo}</H2>
                  <div />
                  <Label>{t.personalDetails.name}</Label>
                  <div>
                    {firstName} {lastName}
                  </div>
                  <H2 noMargin>{t.personalDetails.contactInfo}</H2>
                  <div />
                  <Label>{t.personalDetails.address}</Label>
                  <div>
                    {streetAddress &&
                      `${streetAddress}, ${postalCode} ${postOffice}`}
                  </div>
                  <Label>{t.personalDetails.phone}</Label>
                  <div>{phone}</div>
                  <Label>{t.personalDetails.backupPhone}</Label>
                  <div>{backupPhone}</div>
                  <Label>{t.personalDetails.email}</Label>
                  <div>{email ? email : <EmailMissing />}</div>
                </ListGrid>
              </>
            )
          })}
          <Gap size="XXL" />
        </ContentArea>
      </Container>
      <Footer />
    </>
  )
})

const EmailMissing = styled(
  React.memo(function EmailMissing({ className }: { className?: string }) {
    const t = useTranslation()
    const { colors } = useTheme()
    return (
      <span className={className}>
        {t.personalDetails.emailMissing}
        <FontAwesomeIcon
          icon={fasExclamationTriangle}
          color={colors.accents.orange}
        />
      </span>
    )
  })
)`
  color: ${({ theme }) => theme.colors.greyscale.dark};
  font-style: italic;

  svg {
    margin-left: ${defaultMargins.xs};
  }
`
