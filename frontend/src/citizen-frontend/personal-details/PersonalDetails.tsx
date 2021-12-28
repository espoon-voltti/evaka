// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { AuthContext, User } from 'citizen-frontend/auth/state'
import { Result } from 'lib-common/api'
import { email, phone } from 'lib-common/form-validation'
import AsyncButton from 'lib-components/atoms/buttons/AsyncButton'
import Button from 'lib-components/atoms/buttons/Button'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import Select from 'lib-components/atoms/dropdowns/Select'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import InputField from 'lib-components/atoms/form/InputField'
import HorizontalLine from 'lib-components/atoms/HorizontalLine'
import Container, { ContentArea } from 'lib-components/layout/Container'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import ListGrid from 'lib-components/layout/ListGrid'
import {
  ExpandingInfoBox,
  InfoButton
} from 'lib-components/molecules/ExpandingInfo'
import { AlertBox } from 'lib-components/molecules/MessageBoxes'
import { H1, H2, Label } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import { faLockAlt, faPen, fasExclamationTriangle } from 'lib-icons'
import React, { useCallback, useContext, useMemo, useState } from 'react'
import { Redirect } from 'react-router'
import styled, { useTheme } from 'styled-components'
import { renderResult } from '../async-rendering'
import { refreshRedirect } from '../auth/requireAuth'
import Footer from '../Footer'
import { getLoginUri } from '../header/const'
import { Translations, useTranslation } from '../localization'
import { updatePersonalData } from './api'

export default React.memo(function PersonalDetails() {
  const t = useTranslation()
  const { user, refreshAuthStatus } = useContext(AuthContext)
  const {
    editing,
    startEditing,
    cancelEditing,
    editorState,
    errors,
    errorCount,
    updateState,
    toggleEmailInfo,
    save,
    onSaveSuccess
  } = useEditState(t, user, refreshAuthStatus)

  const preferredNameOptions = useMemo(
    () => getPreferredNameOptions(user),
    [user]
  )

  const navigateToLogin = useCallback(() => refreshRedirect(getLoginUri()), [])

  const formWrapper = (children: React.ReactNode) =>
    editing ? (
      <form onSubmit={(e) => e.preventDefault()}>
        {children}
        <FixedSpaceRow justifyContent="flex-end">
          <Button
            type="button"
            text={t.common.cancel}
            onClick={cancelEditing}
          />
          <AsyncButton
            primary
            type="submit"
            text={t.common.save}
            onClick={save}
            onSuccess={onSaveSuccess}
            disabled={errorCount > 0}
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
              preferredName,
              streetAddress,
              postalCode,
              postOffice,
              phone,
              backupPhone,
              email,
              userType
            } = personalData

            const canEdit = userType === 'ENDUSER'

            return (
              <>
                {email === null && (
                  <AlertBox
                    message={t.personalDetails.noEmailAlert}
                    data-qa="missing-email-box"
                  />
                )}
                <HorizontalLine />
                <EditButtonRow>
                  <InlineButton
                    text={t.common.edit}
                    icon={canEdit ? faPen : faLockAlt}
                    onClick={canEdit ? startEditing : navigateToLogin}
                    disabled={editing}
                    data-qa={canEdit ? 'start-editing' : 'start-editing-login'}
                  />
                </EditButtonRow>
                {formWrapper(
                  <>
                    <ListGrid rowGap="s" columnGap="L" labelWidth="max-content">
                      <H2 noMargin>{t.personalDetails.personalInfo}</H2>
                      <div />
                      <Label>{t.personalDetails.name}</Label>
                      <div>
                        {firstName} {lastName}
                      </div>
                      <Label>{t.personalDetails.preferredName}</Label>
                      {editing ? (
                        <Select
                          items={preferredNameOptions}
                          selectedItem={editorState.preferredName}
                          onChange={updateState.preferredName}
                          data-qa="preferred-name"
                        />
                      ) : (
                        <div data-qa="preferred-name">{preferredName}</div>
                      )}
                      <H2 noMargin>{t.personalDetails.contactInfo}</H2>
                      <div />
                      <Label>{t.personalDetails.address}</Label>
                      <div>
                        {streetAddress &&
                          `${streetAddress}, ${postalCode} ${postOffice}`}
                      </div>
                      <Label>{t.personalDetails.phone}</Label>
                      <div data-qa="phone">
                        {editing ? (
                          <InputField
                            type="text"
                            width="m"
                            value={editorState.phone}
                            onChange={updateState.phone}
                            info={errors.phone}
                            hideErrorsBeforeTouched
                            placeholder="0401234567"
                          />
                        ) : (
                          phone
                        )}
                      </div>
                      <Label>{t.personalDetails.backupPhone}</Label>
                      <div data-qa="backup-phone">
                        {editing ? (
                          <InputField
                            type="text"
                            width="m"
                            value={editorState.backupPhone}
                            onChange={updateState.backupPhone}
                            info={errors.backupPhone}
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
                            <InputField
                              type="text"
                              width="m"
                              value={editorState.email}
                              onChange={updateState.email}
                              readonly={editorState.noEmail}
                              info={errors.email}
                              hideErrorsBeforeTouched
                              placeholder={t.personalDetails.email}
                            />
                          </div>
                          <FixedSpaceRow alignItems="center">
                            <Checkbox
                              label={t.personalDetails.noEmail}
                              checked={editorState.noEmail}
                              onChange={updateState.noEmail}
                              data-qa="no-email"
                            />
                            <InfoButton
                              onClick={toggleEmailInfo}
                              aria-label={t.common.openExpandingInfo}
                            />
                          </FixedSpaceRow>
                        </FixedSpaceColumn>
                      ) : (
                        <div data-qa="email">
                          {email ? email : <EmailMissing />}
                        </div>
                      )}
                    </ListGrid>
                    {editorState.showEmailInfo && (
                      <>
                        <ExpandingInfoBox
                          info={t.personalDetails.emailInfo}
                          close={toggleEmailInfo}
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
      <Footer />
    </>
  )
})

function getPreferredNameOptions(user: Result<User | undefined>): string[] {
  return user.map((user) => user?.firstName.split(' ') ?? []).getOrElse([])
}

function useEditState(
  t: Translations,
  user: Result<User | undefined>,
  reloadUser: () => void
) {
  const [editing, setEditing] = useState(false)
  const [editorState, setEditorState] = useState(() => initialEditorState(user))

  const errors = useMemo(() => {
    const errors: Partial<
      Record<
        'phone' | 'backupPhone' | 'email',
        { status: 'warning'; text: string }
      >
    > = {}

    const phoneError = editorState.phone ? phone(editorState.phone) : 'required'
    if (phoneError) {
      errors['phone'] = {
        status: 'warning',
        text: t.validationErrors[phoneError]
      }
    }

    const backupPhoneError =
      editorState.backupPhone && phone(editorState.backupPhone)
    if (backupPhoneError) {
      errors['backupPhone'] = {
        status: 'warning',
        text: t.validationErrors[backupPhoneError]
      }
    }

    const emailError = editorState.email
      ? email(editorState.email)
      : !editorState.noEmail && 'required'
    if (emailError) {
      errors['email'] = {
        status: 'warning',
        text: t.validationErrors[emailError]
      }
    }

    return errors
  }, [t, editorState])

  const errorCount = Object.keys(errors).length

  const updateState = useMemo(() => {
    const updateFn =
      (field: 'preferredName' | 'phone' | 'backupPhone' | 'email') =>
      (value: string) =>
        setEditorState((state) => ({ ...state, [field]: value }))

    return {
      preferredName: (value: string | null) =>
        setEditorState((state) => ({ ...state, preferredName: value ?? '' })),
      phone: updateFn('phone'),
      backupPhone: updateFn('backupPhone'),
      email: updateFn('email'),
      noEmail: (value: boolean) =>
        setEditorState((state) => ({ ...state, noEmail: value }))
    }
  }, [])

  const toggleEmailInfo = useCallback(
    () =>
      setEditorState((state) => ({
        ...state,
        showEmailInfo: !state.showEmailInfo
      })),
    []
  )

  const startEditing = useCallback(() => {
    setEditorState(initialEditorState(user))
    setEditing(true)
  }, [user])

  const cancelEditing = useCallback(() => setEditing(false), [])

  const save = useCallback(
    () =>
      updatePersonalData({
        ...editorState,
        email: editorState.noEmail ? '' : editorState.email
      }),
    [editorState]
  )

  const onSaveSuccess = useCallback(() => {
    reloadUser()
    cancelEditing()
  }, [reloadUser, cancelEditing])

  return {
    editing,
    startEditing,
    cancelEditing,
    editorState,
    errors,
    errorCount,
    updateState,
    toggleEmailInfo,
    save,
    onSaveSuccess
  }
}

function initialEditorState(userResult: Result<User | undefined>) {
  const noEmail = false
  const showEmailInfo = false
  const preferredNameOptions = getPreferredNameOptions(userResult)

  return (
    userResult
      .map((user) =>
        user
          ? {
              preferredName:
                user.preferredName || (preferredNameOptions[0] ?? ''),
              phone: user.phone,
              backupPhone: user.backupPhone,
              email: user.email ?? '',
              noEmail,
              showEmailInfo
            }
          : undefined
      )
      .getOrElse(undefined) ?? {
      preferredName: '',
      phone: '',
      backupPhone: '',
      email: '',
      noEmail,
      showEmailInfo
    }
  )
}

const EditButtonRow = styled.div`
  display: flex;
  flex-direction: row;
  justify-content: flex-end;
  margin-top: -20px;
`

const EmailMissing = styled(
  React.memo(function EmailMissing({ className }: { className?: string }) {
    const t = useTranslation()
    const { colors } = useTheme()
    return (
      <span className={className}>
        {t.personalDetails.emailMissing}
        <FontAwesomeIcon
          icon={fasExclamationTriangle}
          color={colors.accents.warningOrange}
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
