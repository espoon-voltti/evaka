// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React, { useCallback, useContext, useEffect } from 'react'
import { Navigate } from 'react-router-dom'
import styled, { useTheme } from 'styled-components'

import { AuthContext, User } from 'citizen-frontend/auth/state'
import { Result } from 'lib-common/api'
import { boolean, string } from 'lib-common/form/fields'
import {
  chained,
  object,
  oneOf,
  required,
  validated
} from 'lib-common/form/form'
import { useBoolean, useForm, useFormField } from 'lib-common/form/hooks'
import { StateOf, ValidationSuccess } from 'lib-common/form/types'
import {
  requiredPhoneNumber,
  requiredEmail,
  optionalPhoneNumber
} from 'lib-common/form/validators'
import HorizontalLine from 'lib-components/atoms/HorizontalLine'
import Main from 'lib-components/atoms/Main'
import AsyncButton from 'lib-components/atoms/buttons/AsyncButton'
import Button from 'lib-components/atoms/buttons/Button'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import { SelectF } from 'lib-components/atoms/dropdowns/Select'
import { CheckboxF } from 'lib-components/atoms/form/Checkbox'
import { InputFieldF } from 'lib-components/atoms/form/InputField'
import Container, { ContentArea } from 'lib-components/layout/Container'
import ListGrid from 'lib-components/layout/ListGrid'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import {
  ExpandingInfoBox,
  InfoButton
} from 'lib-components/molecules/ExpandingInfo'
import { AlertBox } from 'lib-components/molecules/MessageBoxes'
import { H1, H2, Label, Light } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import { faLockAlt, faPen, fasExclamationTriangle } from 'lib-icons'

import Footer from '../Footer'
import { renderResult } from '../async-rendering'
import { useTranslation } from '../localization'
import { getStrongLoginUri } from '../navigation/const'

import { updatePersonalData } from './api'

export default React.memo(function PersonalDetails() {
  const t = useTranslation()
  const { user, refreshAuthStatus } = useContext(AuthContext)

  const [editing, setEditing] = useBoolean(false)
  const [emailInfo, setEmailInfo] = useBoolean(false)
  const form = useForm(
    personForm,
    () => initialFormState(user),
    t.validationErrors
  )
  const preferredNameState = useFormField(form, 'preferredName')
  const phoneState = useFormField(form, 'phone')
  const backupPhoneState = useFormField(form, 'backupPhone')

  const emailObjectState = useFormField(form, 'email')
  const emailState = useFormField(emailObjectState, 'email')
  const noEmailState = useFormField(emailObjectState, 'noEmail')

  const set = form.set
  useEffect(() => {
    if (user.isSuccess) {
      set(initialFormState(user))
    }
  }, [set, user])

  const navigateToLogin = useCallback(
    () => window.location.replace(getStrongLoginUri()),
    []
  )

  const save = useCallback(async () => {
    return await updatePersonalData(form.value())
  }, [form])

  const formWrapper = (children: React.ReactNode) =>
    editing ? (
      <form>
        {children}
        <FixedSpaceRow justifyContent="flex-end">
          <Button
            type="button"
            text={t.common.cancel}
            onClick={() => {
              setEditing.off()
              form.set(initialFormState(user))
            }}
          />
          <AsyncButton
            primary
            type="submit"
            text={t.common.save}
            onClick={save}
            onSuccess={() => {
              refreshAuthStatus()
              setEditing.off()
            }}
            disabled={!form.isValid()}
            data-qa="save"
          />
        </FixedSpaceRow>
      </form>
    ) : (
      <>
        {children}
        <Gap size="XL" />
      </>
    )

  const getAlertMessage = (
    email: string | null,
    phone: string | null
  ): string => {
    const alerts = []
    if (email === null) alerts.push(t.personalDetails.noEmailAlert)
    if (!phone) alerts.push(t.personalDetails.noPhoneAlert)
    return alerts.join('. ').concat('.')
  }

  return (
    <>
      <Main>
        <Container>
          <Gap size="L" />
          <ContentArea opaque paddingVertical="L">
            <H1 noMargin>{t.personalDetails.title}</H1>
            {t.personalDetails.description}
            {renderResult(user, (personalData) => {
              if (personalData === undefined) {
                return <Navigate replace to="/" />
              }

              const {
                firstName,
                lastName,
                preferredName,
                streetAddress,
                postalCode,
                postOffice,
                phone,
                backupPhone,
                email,
                authLevel
              } = personalData

              const canEdit = authLevel === 'STRONG'

              return (
                <>
                  {(email === null || !phone) && (
                    <AlertBox
                      message={getAlertMessage(email, phone)}
                      data-qa="missing-email-or-phone-box"
                    />
                  )}
                  <HorizontalLine />
                  <EditButtonRow>
                    <InlineButton
                      text={t.common.edit}
                      icon={canEdit ? faPen : faLockAlt}
                      onClick={canEdit ? setEditing.on : navigateToLogin}
                      disabled={editing}
                      data-qa={
                        canEdit ? 'start-editing' : 'start-editing-login'
                      }
                    />
                  </EditButtonRow>
                  {formWrapper(
                    <>
                      <ListGrid
                        rowGap="s"
                        columnGap="L"
                        labelWidth="max-content"
                      >
                        <H2 noMargin>{t.personalDetails.personalInfo}</H2>
                        <div />
                        <Label>{t.personalDetails.name}</Label>
                        <div>
                          {firstName} {lastName}
                        </div>
                        <Label>{t.personalDetails.preferredName}</Label>
                        {editing ? (
                          <SelectF
                            bind={preferredNameState}
                            data-qa="preferred-name"
                          />
                        ) : (
                          <div data-qa="preferred-name">{preferredName}</div>
                        )}
                        <H2 noMargin>{t.personalDetails.contactInfo}</H2>
                        <div />
                        <Label>{t.personalDetails.address}</Label>
                        <div>
                          {!!streetAddress &&
                            `${streetAddress}, ${postalCode} ${postOffice}`}
                        </div>
                        <Label>{t.personalDetails.phone}</Label>
                        <div data-qa="phone">
                          {editing ? (
                            <InputFieldF
                              type="text"
                              width="m"
                              bind={phoneState}
                              hideErrorsBeforeTouched
                              placeholder="0401234567"
                            />
                          ) : (
                            <div>
                              {phone ? (
                                phone
                              ) : (
                                <MandatoryValueMissingWarning
                                  text={t.personalDetails.phoneMissing}
                                />
                              )}
                            </div>
                          )}
                        </div>
                        <Label>{t.personalDetails.backupPhone}</Label>
                        <div data-qa="backup-phone">
                          {editing ? (
                            <InputFieldF
                              type="text"
                              width="m"
                              bind={backupPhoneState}
                              hideErrorsBeforeTouched
                              placeholder={
                                t.personalDetails.backupPhonePlaceholder
                              }
                            />
                          ) : (
                            backupPhone
                          )}
                        </div>
                        <Label>{t.personalDetails.email}</Label>
                        {editing ? (
                          <FixedSpaceColumn spacing="xs">
                            <div data-qa="email">
                              <InputFieldF
                                type="text"
                                width="m"
                                bind={emailState}
                                readonly={noEmailState.state}
                                hideErrorsBeforeTouched
                                placeholder={t.personalDetails.email}
                              />
                            </div>
                            <FixedSpaceRow alignItems="center">
                              <CheckboxF
                                label={t.personalDetails.noEmail}
                                bind={noEmailState}
                                data-qa="no-email"
                              />
                              <InfoButton
                                onClick={setEmailInfo.toggle}
                                aria-label={t.common.openExpandingInfo}
                              />
                            </FixedSpaceRow>
                          </FixedSpaceColumn>
                        ) : (
                          <div data-qa="email">
                            {email ? (
                              email
                            ) : (
                              <MandatoryValueMissingWarning
                                text={t.personalDetails.emailMissing}
                              />
                            )}
                          </div>
                        )}
                      </ListGrid>
                      {emailInfo && (
                        <>
                          <ExpandingInfoBox
                            info={t.personalDetails.emailInfo}
                            close={setEmailInfo.off}
                            closeLabel={t.common.close}
                          />
                          <Gap size="s" />
                        </>
                      )}
                    </>
                  )}
                </>
              )
            })}
          </ContentArea>
        </Container>
      </Main>
      <Footer />
    </>
  )
})

const personForm = object({
  preferredName: required(oneOf<string>()),
  phone: validated(string, requiredPhoneNumber),
  backupPhone: validated(string, optionalPhoneNumber),
  email: chained(
    object({
      email: validated(string, requiredEmail),
      noEmail: boolean
    }),
    (form, state) => {
      if (state.noEmail) {
        return ValidationSuccess.of('')
      }
      return form.shape.email.validate(state.email)
    }
  )
})

function initialFormState(
  userResult: Result<User | undefined>
): StateOf<typeof personForm> {
  if (!userResult.isSuccess || userResult.value === undefined) {
    throw new Error('User not loaded')
  }
  const user = userResult.value
  const firstNames = user.firstName.split(' ')

  const preferredNameOptions = firstNames.map((name) => ({
    value: name,
    label: name,
    domValue: name
  }))

  return {
    preferredName: {
      options: preferredNameOptions,
      domValue: user.preferredName || (firstNames[0] ?? '')
    },
    phone: user.phone,
    backupPhone: user.backupPhone,
    email: {
      email: user.email ?? '',
      noEmail: false
    }
  }
}

const EditButtonRow = styled.div`
  display: flex;
  flex-direction: row;
  justify-content: flex-end;
  margin-top: -20px;
`

const MandatoryValueMissingWarning = styled(
  React.memo(function EmailMissing({
    text,
    className
  }: {
    text: string
    className?: string
  }) {
    const { colors } = useTheme()
    return (
      <Light className={className}>
        {text}
        <FontAwesomeIcon
          icon={fasExclamationTriangle}
          color={colors.status.warning}
        />
      </Light>
    )
  })
)`
  svg {
    margin-left: ${defaultMargins.xs};
  }
`
