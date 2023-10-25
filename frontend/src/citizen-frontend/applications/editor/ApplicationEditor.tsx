// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import maxBy from 'lodash/maxBy'
import minBy from 'lodash/minBy'
import React, { useContext, useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'

import { combine } from 'lib-common/api'
import { ApplicationDetails } from 'lib-common/api-types/application/ApplicationDetails'
import {
  apiDataToFormData,
  ApplicationFormData,
  formDataToApiData
} from 'lib-common/api-types/application/ApplicationFormData'
import FiniteDateRange from 'lib-common/finite-date-range'
import { CitizenChildren } from 'lib-common/generated/api-types/application'
import LocalDate from 'lib-common/local-date'
import { useMutation, useQuery, useQueryResult } from 'lib-common/query'
import { UUID } from 'lib-common/types'
import useBetterParams from 'lib-common/useNonNullableParams'
import { scrollToTop } from 'lib-common/utils/scrolling'
import Main from 'lib-components/atoms/Main'
import Button from 'lib-components/atoms/buttons/Button'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import ReturnButton, {
  ReturnButtonWrapper
} from 'lib-components/atoms/buttons/ReturnButton'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import ActionRow from 'lib-components/layout/ActionRow'
import Container from 'lib-components/layout/Container'
import { ExpandingInfoGroup } from 'lib-components/molecules/ExpandingInfo'
import { defaultMargins, Gap } from 'lib-components/white-space'
import { faAngleLeft, faCheck, faExclamation } from 'lib-icons'

import Footer from '../../Footer'
import ApplicationFormClub from '../../applications/editor/ApplicationFormClub'
import ApplicationFormDaycare from '../../applications/editor/ApplicationFormDaycare'
import ApplicationFormPreschool from '../../applications/editor/ApplicationFormPreschool'
import ApplicationVerificationView from '../../applications/editor/verification/ApplicationVerificationView'
import { renderResult } from '../../async-rendering'
import { useTranslation } from '../../localization'
import { OverlayContext } from '../../overlay/state'
import useTitle from '../../useTitle'
import {
  applicationChildrenQuery,
  applicationQuery,
  clubTermsQuery,
  preschoolTermsQuery,
  saveApplicationDraftMutation,
  sendApplicationMutation,
  updateApplicationMutation
} from '../queries'

import {
  ApplicationFormDataErrors,
  applicationHasErrors,
  maxPreferredStartDate,
  minPreferredStartDate,
  validateApplication
} from './validations'

type ApplicationEditorContentProps = {
  application: ApplicationDetails
  citizenChildren: CitizenChildren[]
}

export type ApplicationFormProps = {
  application: ApplicationDetails
  formData: ApplicationFormData
  setFormData: (
    update: (old: ApplicationFormData) => ApplicationFormData
  ) => void
  errors: ApplicationFormDataErrors
  verificationRequested: boolean
  originalPreferredStartDate: LocalDate | null
  minDate: LocalDate
  maxDate: LocalDate
  terms?: FiniteDateRange[]
}

const ApplicationEditorContent = React.memo(function DaycareApplicationEditor({
  application,
  citizenChildren
}: ApplicationEditorContentProps) {
  const t = useTranslation()
  const navigate = useNavigate()

  const { setErrorMessage, setInfoMessage, clearInfoMessage } =
    useContext(OverlayContext)

  const { data: preschoolTerms } = useQuery(preschoolTermsQuery(), {
    enabled: application.type === 'PRESCHOOL'
  })
  const { data: clubTerms } = useQuery(clubTermsQuery(), {
    enabled: application.type === 'CLUB'
  })
  const terms = useMemo(() => {
    switch (application.type) {
      case 'PRESCHOOL':
        return (preschoolTerms ?? [])
          .filter(({ applicationPeriod, extendedTerm }) => {
            const today = LocalDate.todayInSystemTz()
            return (
              applicationPeriod.start.isEqualOrBefore(today) &&
              extendedTerm.end.isEqualOrAfter(today)
            )
          })
          .map((term) => term.extendedTerm)
      case 'CLUB':
        return (clubTerms ?? [])
          .filter(({ term }) =>
            term.end.isEqualOrAfter(LocalDate.todayInHelsinkiTz())
          )
          .map(({ term }) => term)
      default:
        return undefined
    }
  }, [application.type, clubTerms, preschoolTerms])

  const [formData, setFormData] = useState<ApplicationFormData>(
    apiDataToFormData(application, citizenChildren)
  )
  const [verificationRequested, setVerificationRequested] =
    useState<boolean>(false)
  const [verifying, setVerifying] = useState<boolean>(false)
  const [verified, setVerified] = useState<boolean>(false)
  const [allowOtherGuardianAccess, setAllowOtherGuardianAccess] =
    useState<boolean>(application.allowOtherGuardianAccess)

  const [errors, setErrors] = useState<ApplicationFormDataErrors>(
    validateApplication(application, formData)
  )
  useEffect(() => {
    setErrors(validateApplication(application, formData, terms))
  }, [application, formData, terms])

  const originalPreferredStartDate =
    application.status !== 'CREATED'
      ? application.form.preferences.preferredStartDate
      : null

  const minDate = useMemo(() => {
    const minPreferred = minPreferredStartDate(originalPreferredStartDate)
    const minTermDate = terms && minBy(terms, (term) => term.start)?.start

    return minTermDate?.isAfter(minPreferred) ? minTermDate : minPreferred
  }, [terms, originalPreferredStartDate])

  const maxDate = useMemo(() => {
    const maxPreferred = maxPreferredStartDate()
    const maxTermDate = terms && maxBy(terms, (term) => term.end)?.end

    return maxTermDate?.isBefore(maxPreferred) ? maxTermDate : maxPreferred
  }, [terms])

  const hasOtherGuardian = !!application.otherGuardianId

  const { mutateAsync: saveApplicationDraft, isPending: savingDraft } =
    useMutation(saveApplicationDraftMutation)
  const { mutateAsync: updateApplication, isPending: updatingApplication } =
    useMutation(updateApplicationMutation)
  const { mutateAsync: sendApplication, isPending: sendingApplication } =
    useMutation(sendApplicationMutation)
  const submitting = savingDraft || updatingApplication || sendingApplication

  const onVerify = () => {
    setErrors(validateApplication(application, formData, terms))
    setVerificationRequested(true)

    if (!applicationHasErrors(errors)) {
      setVerified(false)
      setVerifying(true)
    }

    scrollToTop(50)
  }

  const onSaveDraft = () => {
    const body = formDataToApiData(application, formData, true)
    void saveApplicationDraft({ applicationId: application.id, body })
      .then(() => {
        setInfoMessage({
          title: t.applications.editor.draftPolicyInfo.title,
          text: t.applications.editor.draftPolicyInfo.text,
          type: 'success',
          icon: faExclamation,
          resolve: {
            action: () => {
              navigate('/applications')
              clearInfoMessage()
            },
            label: t.applications.editor.draftPolicyInfo.ok
          },
          'data-qa': 'info-message-draft-saved'
        })
      })
      .catch(() => {
        setErrorMessage({
          title: t.applications.editor.actions.updateError,
          type: 'error',
          resolveLabel: t.common.ok
        })
      })
  }

  const onSend = () => {
    const body = {
      form: formDataToApiData(application, formData),
      allowOtherGuardianAccess
    }
    updateApplication({ applicationId: application.id, body })
      .then(() => sendApplication(application.id))
      .then(() => {
        setInfoMessage({
          title: t.applications.editor.sentInfo.title,
          text: t.applications.editor.sentInfo.text,
          type: 'success',
          icon: faCheck,
          resolve: {
            action: () => {
              navigate('/applications')
              clearInfoMessage()
            },
            label: t.applications.editor.sentInfo.ok
          },
          'data-qa': 'info-message-application-sent'
        })
        navigate('/applications')
      })
      .catch(() => {
        setErrorMessage({
          title: t.applications.editor.actions.sendError,
          type: 'error',
          resolveLabel: t.common.ok
        })
      })
  }

  const onUpdate = () => {
    const body = {
      form: formDataToApiData(application, formData),
      allowOtherGuardianAccess
    }
    updateApplication({ applicationId: application.id, body })
      .then(() => {
        setInfoMessage({
          title: t.applications.editor.updateInfo.title,
          text: t.applications.editor.updateInfo.text,
          type: 'success',
          icon: faCheck,
          resolve: {
            action: () => {
              navigate('/applications')
              clearInfoMessage()
            },
            label: t.applications.editor.updateInfo.ok
          },
          'data-qa': 'info-message-application-sent'
        })
        navigate('/applications')
      })
      .catch(() => {
        setErrorMessage({
          title: t.applications.editor.actions.updateError,
          type: 'error',
          resolveLabel: t.common.ok
        })
      })
  }

  const renderEditor = () => {
    switch (application.type) {
      case 'DAYCARE':
        return (
          <ApplicationFormDaycare
            application={application}
            formData={formData}
            setFormData={setFormData}
            errors={errors}
            verificationRequested={verificationRequested}
            originalPreferredStartDate={originalPreferredStartDate}
            minDate={minDate}
            maxDate={maxDate}
          />
        )
      case 'PRESCHOOL':
        return (
          <ApplicationFormPreschool
            application={application}
            formData={formData}
            setFormData={setFormData}
            errors={errors}
            verificationRequested={verificationRequested}
            terms={terms}
            originalPreferredStartDate={originalPreferredStartDate}
            minDate={minDate}
            maxDate={maxDate}
          />
        )
      case 'CLUB':
        return (
          <ApplicationFormClub
            application={application}
            formData={formData}
            setFormData={setFormData}
            errors={errors}
            verificationRequested={verificationRequested}
            terms={terms}
            originalPreferredStartDate={originalPreferredStartDate}
            minDate={minDate}
            maxDate={maxDate}
          />
        )
    }
  }

  const renderVerificationView = () => {
    switch (application.type) {
      case 'DAYCARE':
        return (
          <ApplicationVerificationView
            application={application}
            formData={formData}
            type="DAYCARE"
            closeVerification={() => setVerifying(false)}
          />
        )
      case 'PRESCHOOL':
        return (
          <ApplicationVerificationView
            application={application}
            formData={formData}
            type="PRESCHOOL"
            closeVerification={() => setVerifying(false)}
          />
        )
      case 'CLUB':
        return (
          <ApplicationVerificationView
            application={application}
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
          {hasOtherGuardian && (
            <>
              <Checkbox
                label={t.applications.editor.actions.allowOtherGuardianAccess}
                checked={allowOtherGuardianAccess}
                onChange={setAllowOtherGuardianAccess}
                data-qa="allow-other-guardian-access"
              />
              <Gap size="s" />
            </>
          )}
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

        {application.status === 'CREATED' && !verifying && (
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
            {application.status === 'CREATED' ? (
              <Button
                text={t.applications.editor.actions.send}
                onClick={onSend}
                disabled={
                  submitting ||
                  !verified ||
                  (hasOtherGuardian && !allowOtherGuardianAccess)
                }
                primary
                data-qa="send-btn"
              />
            ) : (
              <Button
                text={t.applications.editor.actions.update}
                onClick={onUpdate}
                disabled={
                  submitting ||
                  !verified ||
                  (hasOtherGuardian && !allowOtherGuardianAccess)
                }
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

      <Main>
        <ExpandingInfoGroup>
          {verifying ? renderVerificationView() : renderEditor()}

          <Gap size="m" />

          {renderActions()}
        </ExpandingInfoGroup>
      </Main>
    </Container>
  )
})

export default React.memo(function ApplicationEditor() {
  const { applicationId } = useBetterParams<{ applicationId: UUID }>()
  const t = useTranslation()
  const application = useQueryResult(applicationQuery(applicationId))
  const children = useQueryResult(applicationChildrenQuery())

  useTitle(
    t,
    application
      .map(({ type }) => t.applications.editor.heading.title[type])
      .getOrElse('')
  )

  return (
    <>
      <Container>
        {renderResult(
          combine(application, children),
          ([application, children]) => (
            <ApplicationEditorContent
              application={application}
              citizenChildren={children}
            />
          )
        )}
      </Container>
      <Footer />
    </>
  )
})
