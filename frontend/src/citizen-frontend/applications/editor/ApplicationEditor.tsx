// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useEffect, useState } from 'react'
import { useHistory, useParams } from 'react-router-dom'
import { Loading, Result } from 'lib-common/api'
import { useRestApi } from 'lib-common/utils/useRestApi'
import Container from 'lib-components/layout/Container'
import { SpinnerSegment } from 'lib-components/atoms/state/Spinner'
import ErrorSegment from 'lib-components/atoms/state/ErrorSegment'
import {
  getApplication,
  saveApplicationDraft,
  sendApplication,
  updateApplication
} from '../../applications/api'
import { ApplicationDetails } from 'lib-common/api-types/application/ApplicationDetails'
import { useTranslation } from '../../localization'
import { useUser } from '../../auth'
import { OverlayContext } from '../../overlay/state'
import {
  apiDataToFormData,
  ApplicationFormData,
  formDataToApiData
} from '../../applications/editor/ApplicationFormData'
import {
  ApplicationFormDataErrors,
  applicationHasErrors,
  validateApplication
} from '../../applications/editor/validations'
import { faAngleLeft, faCheck, faExclamation } from 'lib-icons'
import { defaultMargins, Gap } from 'lib-components/white-space'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import ActionRow from 'lib-components/layout/ActionRow'
import Button from 'lib-components/atoms/buttons/Button'
import ReturnButton, {
  ReturnButtonWrapper
} from 'lib-components/atoms/buttons/ReturnButton'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import ApplicationFormDaycare from '../../applications/editor/ApplicationFormDaycare'
import ApplicationVerificationView from '../../applications/editor/verification/ApplicationVerificationView'
import ApplicationFormClub from '../../applications/editor/ApplicationFormClub'
import ApplicationFormPreschool from '../../applications/editor/ApplicationFormPreschool'
import Footer from '../../Footer'
import useTitle from '../../useTitle'

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
}

const ApplicationEditorContent = React.memo(function DaycareApplicationEditor({
  apiData
}: ApplicationEditorContentProps) {
  const t = useTranslation()
  const history = useHistory()
  const user = useUser()

  const { setErrorMessage, setInfoMessage, clearInfoMessage } = useContext(
    OverlayContext
  )

  const [formData, setFormData] = useState<ApplicationFormData>(
    apiDataToFormData(apiData, user)
  )
  const [verificationRequested, setVerificationRequested] = useState<boolean>(
    false
  )
  const [verifying, setVerifying] = useState<boolean>(false)
  const [verified, setVerified] = useState<boolean>(false)
  const [submitting, setSubmitting] = useState<boolean>(false)

  const [errors, setErrors] = useState<ApplicationFormDataErrors>(
    validateApplication(apiData, formData)
  )
  useEffect(() => {
    setErrors(validateApplication(apiData, formData))
  }, [formData])

  const onVerify = () => {
    setErrors(validateApplication(apiData, formData))
    setVerificationRequested(true)

    if (!applicationHasErrors(errors)) {
      setVerified(false)
      setVerifying(true)
    }

    setTimeout(
      () => window.scrollTo({ left: 0, top: 0, behavior: 'smooth' }),
      50
    )
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
          iconColour: 'green',
          icon: faExclamation,
          resolve: {
            action: () => {
              history.push('/applications')
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
              iconColour: 'green',
              icon: faCheck,
              resolve: {
                action: () => {
                  history.push('/applications')
                  clearInfoMessage()
                },
                label: t.applications.editor.sentInfo.ok
              },
              'data-qa': 'info-message-application-sent'
            })

            history.push('/applications')
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
          iconColour: 'green',
          icon: faCheck,
          resolve: {
            action: () => {
              history.push('/applications')
              clearInfoMessage()
            },
            label: t.applications.editor.updateInfo.ok
          },
          'data-qa': 'info-message-application-sent'
        })

        history.push('/applications')
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
            dataQa="verify-checkbox"
          />
          <Gap size="s" />
        </div>
      )}

      <ActionRow breakpoint="660px">
        {!verifying && (
          <Button
            text={t.applications.editor.actions.cancel}
            onClick={() => history.goBack()}
            disabled={submitting}
          />
        )}

        <div className="expander" />

        {apiData.status === 'CREATED' && !verifying && (
          <Button
            text={t.applications.editor.actions.saveDraft}
            onClick={onSaveDraft}
            disabled={submitting}
            dataQa="save-as-draft-btn"
          />
        )}
        {verifying ? (
          <>
            <Button
              text={t.applications.editor.actions.returnToEditBtn}
              onClick={() => setVerifying(false)}
              disabled={submitting}
              dataQa="return-to-edit-btn"
            />
            {apiData.status === 'CREATED' ? (
              <Button
                text={t.applications.editor.actions.send}
                onClick={onSend}
                disabled={submitting || !verified}
                primary
                dataQa="send-btn"
              />
            ) : (
              <Button
                text={t.applications.editor.actions.update}
                onClick={onUpdate}
                disabled={submitting || !verified}
                primary
                dataQa="save-btn"
              />
            )}
          </>
        ) : (
          <Button
            text={t.applications.editor.actions.verify}
            onClick={onVerify}
            disabled={submitting}
            primary
            dataQa="verify-btn"
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
  const { applicationId } = useParams<{ applicationId: string }>()
  const t = useTranslation()
  const [apiData, setApiData] = useState<Result<ApplicationDetails>>(
    Loading.of()
  )

  const loadApplication = useRestApi(getApplication, setApiData)
  useEffect(() => {
    loadApplication(applicationId)
  }, [applicationId])

  useTitle(
    t,
    apiData
      .map(({ type }) => t.applications.editor.heading.title[type])
      .getOrElse(''),
    [apiData]
  )

  return (
    <>
      <Container>
        {apiData.isLoading && <SpinnerSegment />}
        {apiData.isFailure && <ErrorSegment />}
        {apiData.isSuccess && (
          <ApplicationEditorContent apiData={apiData.value} />
        )}
      </Container>
      <Footer />
    </>
  )
})
