// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'

import { ApplicationDetails } from 'lib-common/api-types/application/ApplicationDetails'
import {
  apiDataToFormData,
  ApplicationFormData,
  formDataToApiData
} from 'lib-common/api-types/application/ApplicationFormData'
import FiniteDateRange from 'lib-common/finite-date-range'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'
import useBetterParams from 'lib-common/useNonNullableParams'
import { scrollToTop } from 'lib-common/utils/scrolling'
import { useApiState } from 'lib-common/utils/useRestApi'
import Button from 'lib-components/atoms/buttons/Button'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import ReturnButton, {
  ReturnButtonWrapper
} from 'lib-components/atoms/buttons/ReturnButton'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import ActionRow from 'lib-components/layout/ActionRow'
import Container from 'lib-components/layout/Container'
import { defaultMargins, Gap } from 'lib-components/white-space'
import { faAngleLeft, faCheck, faExclamation } from 'lib-icons'

import Footer from '../../Footer'
import ApplicationFormClub from '../../applications/editor/ApplicationFormClub'
import ApplicationFormDaycare from '../../applications/editor/ApplicationFormDaycare'
import ApplicationFormPreschool from '../../applications/editor/ApplicationFormPreschool'
import ApplicationVerificationView from '../../applications/editor/verification/ApplicationVerificationView'
import { renderResult } from '../../async-rendering'
import { useUser } from '../../auth/state'
import { useTranslation } from '../../localization'
import { OverlayContext } from '../../overlay/state'
import useTitle from '../../useTitle'
import {
  getApplication,
  getClubTerms,
  getPreschoolTerms,
  saveApplicationDraft,
  sendApplication,
  updateApplication
} from '../api'

import {
  ApplicationFormDataErrors,
  applicationHasErrors,
  validateApplication
} from './validations'

type ApplicationEditorContentProps = {
  apiData: ApplicationDetails
}

export type ApplicationFormProps = {
  apiData: ApplicationDetails
  formData: ApplicationFormData
  setFormData: (
    update: (old: ApplicationFormData) => ApplicationFormData
  ) => void
  errors: ApplicationFormDataErrors
  verificationRequested: boolean
  terms?: FiniteDateRange[]
}

const ApplicationEditorContent = React.memo(function DaycareApplicationEditor({
  apiData
}: ApplicationEditorContentProps) {
  const t = useTranslation()
  const navigate = useNavigate()
  const user = useUser()

  const { setErrorMessage, setInfoMessage, clearInfoMessage } =
    useContext(OverlayContext)

  const [terms, setTerms] = useState<FiniteDateRange[]>()
  useEffect(() => {
    switch (apiData.type) {
      case 'PRESCHOOL':
        void getPreschoolTerms().then((res) =>
          setTerms(
            res
              .map((terms) =>
                terms
                  .filter(({ applicationPeriod, extendedTerm }) => {
                    const today = LocalDate.todayInSystemTz()
                    return (
                      applicationPeriod.start.isEqualOrBefore(today) &&
                      extendedTerm.end.isEqualOrAfter(today)
                    )
                  })
                  .map((term) => term.extendedTerm)
              )
              .getOrElse([])
          )
        )
        break
      case 'CLUB':
        void getClubTerms().then((res) =>
          setTerms(
            res.map((terms) => terms.map(({ term }) => term)).getOrElse([])
          )
        )
        break
    }
  }, [apiData.type, setTerms])

  const [formData, setFormData] = useState<ApplicationFormData>(
    apiDataToFormData(apiData, user)
  )
  const [verificationRequested, setVerificationRequested] =
    useState<boolean>(false)
  const [verifying, setVerifying] = useState<boolean>(false)
  const [verified, setVerified] = useState<boolean>(false)
  const [submitting, setSubmitting] = useState<boolean>(false)

  const [errors, setErrors] = useState<ApplicationFormDataErrors>(
    validateApplication(apiData, formData)
  )
  useEffect(() => {
    setErrors(validateApplication(apiData, formData, terms))
  }, [apiData, formData, terms])

  const onVerify = () => {
    setErrors(validateApplication(apiData, formData, terms))
    setVerificationRequested(true)

    if (!applicationHasErrors(errors)) {
      setVerified(false)
      setVerifying(true)
    }

    scrollToTop(50)
  }

  const onSaveDraft = () => {
    const reqBody = formDataToApiData(apiData, formData, true)
    setSubmitting(true)
    void saveApplicationDraft(apiData.id, reqBody).then((res) => {
      setSubmitting(false)
      if (res.isFailure) {
        setErrorMessage({
          title: t.applications.editor.actions.updateError,
          type: 'error',
          resolveLabel: t.common.ok
        })
      } else if (res.isSuccess) {
        setInfoMessage({
          title: t.applications.editor.draftPolicyInfo.title,
          text: t.applications.editor.draftPolicyInfo.text,
          type: 'success',
          icon: faExclamation,
          resolve: {
            action: () => {
              navigate('/applying/applications')
              clearInfoMessage()
            },
            label: t.applications.editor.draftPolicyInfo.ok
          },
          'data-qa': 'info-message-draft-saved'
        })
      }
    })
  }

  const onSend = () => {
    const reqBody = formDataToApiData(apiData, formData)
    setSubmitting(true)
    void updateApplication(apiData.id, reqBody).then((res) => {
      if (res.isFailure) {
        setSubmitting(false)
        setErrorMessage({
          title: t.applications.editor.actions.sendError,
          type: 'error',
          resolveLabel: t.common.ok
        })
      } else if (res.isSuccess) {
        void sendApplication(apiData.id).then((res2) => {
          setSubmitting(false)
          if (res2.isFailure) {
            setErrorMessage({
              title: t.applications.editor.actions.sendError,
              type: 'error'
            })
          } else {
            setInfoMessage({
              title: t.applications.editor.sentInfo.title,
              text: t.applications.editor.sentInfo.text,
              type: 'success',
              icon: faCheck,
              resolve: {
                action: () => {
                  navigate('/applying/applications')
                  clearInfoMessage()
                },
                label: t.applications.editor.sentInfo.ok
              },
              'data-qa': 'info-message-application-sent'
            })

            navigate('/applying/applications')
          }
        })
      }
    })
  }

  const onUpdate = () => {
    const reqBody = formDataToApiData(apiData, formData)
    setSubmitting(true)
    void updateApplication(apiData.id, reqBody).then((res) => {
      if (res.isFailure) {
        setSubmitting(false)
        setErrorMessage({
          title: t.applications.editor.actions.updateError,
          type: 'error',
          resolveLabel: t.common.ok
        })
      } else if (res.isSuccess) {
        setInfoMessage({
          title: t.applications.editor.updateInfo.title,
          text: t.applications.editor.updateInfo.text,
          type: 'success',
          icon: faCheck,
          resolve: {
            action: () => {
              navigate('/applying/applications')
              clearInfoMessage()
            },
            label: t.applications.editor.updateInfo.ok
          },
          'data-qa': 'info-message-application-sent'
        })

        navigate('/applying/applications')
      }
    })
  }

  const renderEditor = () => {
    switch (apiData.type) {
      case 'DAYCARE':
        return (
          <ApplicationFormDaycare
            apiData={apiData}
            formData={formData}
            setFormData={setFormData}
            errors={errors}
            verificationRequested={verificationRequested}
          />
        )
      case 'PRESCHOOL':
        return (
          <ApplicationFormPreschool
            apiData={apiData}
            formData={formData}
            setFormData={setFormData}
            errors={errors}
            verificationRequested={verificationRequested}
            terms={terms}
          />
        )
      case 'CLUB':
        return (
          <ApplicationFormClub
            apiData={apiData}
            formData={formData}
            setFormData={setFormData}
            errors={errors}
            verificationRequested={verificationRequested}
            terms={terms}
          />
        )
    }
  }

  const renderVerificationView = () => {
    switch (apiData.type) {
      case 'DAYCARE':
        return (
          <ApplicationVerificationView
            application={apiData}
            formData={formData}
            type="DAYCARE"
            closeVerification={() => setVerifying(false)}
          />
        )
      case 'PRESCHOOL':
        return (
          <ApplicationVerificationView
            application={apiData}
            formData={formData}
            type="PRESCHOOL"
            closeVerification={() => setVerifying(false)}
          />
        )
      case 'CLUB':
        return (
          <ApplicationVerificationView
            application={apiData}
            formData={formData}
            type="CLUB"
            closeVerification={() => setVerifying(false)}
          />
        )
    }
  }

  const renderActions = () => (
    <Container>
      {verifying && (
        <div style={{ marginLeft: defaultMargins.s }}>
          <Checkbox
            label={t.applications.editor.actions.hasVerified}
            checked={verified}
            onChange={setVerified}
            data-qa="verify-checkbox"
          />
          <Gap size="s" />
        </div>
      )}

      <ActionRow breakpoint="660px">
        {!verifying && (
          <Button
            data-qa="cancel-application-button"
            text={t.applications.editor.actions.cancel}
            onClick={() => navigate(-1)}
            disabled={submitting}
          />
        )}

        <div className="expander" />

        {apiData.status === 'CREATED' && !verifying && (
          <Button
            text={t.applications.editor.actions.saveDraft}
            onClick={onSaveDraft}
            disabled={submitting}
            data-qa="save-as-draft-btn"
          />
        )}
        {verifying ? (
          <>
            <Button
              text={t.applications.editor.actions.returnToEditBtn}
              onClick={() => setVerifying(false)}
              disabled={submitting}
              data-qa="return-to-edit-btn"
            />
            {apiData.status === 'CREATED' ? (
              <Button
                text={t.applications.editor.actions.send}
                onClick={onSend}
                disabled={submitting || !verified}
                primary
                data-qa="send-btn"
              />
            ) : (
              <Button
                text={t.applications.editor.actions.update}
                onClick={onUpdate}
                disabled={submitting || !verified}
                primary
                data-qa="save-btn"
              />
            )}
          </>
        ) : (
          <Button
            text={t.applications.editor.actions.verify}
            onClick={onVerify}
            disabled={submitting}
            primary
            data-qa="verify-btn"
          />
        )}
      </ActionRow>
    </Container>
  )

  return (
    <Container>
      {verifying ? (
        <ReturnButtonWrapper>
          <InlineButton
            icon={faAngleLeft}
            text={t.applications.editor.actions.returnToEdit}
            onClick={() => setVerifying(false)}
          />
        </ReturnButtonWrapper>
      ) : (
        <ReturnButton label={t.common.return} />
      )}

      {verifying ? renderVerificationView() : renderEditor()}

      <Gap size="m" />

      {renderActions()}
    </Container>
  )
})

export default React.memo(function ApplicationEditor() {
  const { applicationId } = useBetterParams<{ applicationId: UUID }>()
  const t = useTranslation()
  const [apiData] = useApiState(
    () => getApplication(applicationId),
    [applicationId]
  )

  useTitle(
    t,
    apiData
      .map(({ type }) => t.applications.editor.heading.title[type])
      .getOrElse('')
  )

  return (
    <>
      <Container>
        {renderResult(apiData, (value) => (
          <ApplicationEditorContent apiData={value} />
        ))}
      </Container>
      <Footer />
    </>
  )
})
