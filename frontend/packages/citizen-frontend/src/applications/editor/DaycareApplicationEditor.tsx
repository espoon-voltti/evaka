// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useContext, useState } from 'react'
import { Gap } from '@evaka/lib-components/src/white-space'
import Container from '@evaka/lib-components/src/layout/Container'
import Heading from '~applications/editor/Heading'
import ServiceNeedSection from '~applications/editor/ServiceNeedSection'
import ContactInfoSection from '~applications/editor/ContactInfoSection'
import UnitPreferenceSection from '~applications/editor/UnitPreferenceSection'
import FeeSection from '~applications/editor/FeeSection'
import AdditionalDetailsSection from '~applications/editor/AdditionalDetailsSection'
import {
  AdditionalDetailsFormData,
  apiDataToFormData,
  ApplicationFormData,
  FeeFormData,
  formDataToApiData
} from '~applications/editor/ApplicationFormData'
import Button from '@evaka/lib-components/src/atoms/buttons/Button'
import {
  saveApplicationDraft,
  sendApplication,
  updateApplication
} from '~applications/api'
import { OverlayContext } from '~overlay/state'
import { useTranslation } from '~localization'
import { useHistory } from 'react-router-dom'
import { FixedSpaceRow } from '@evaka/lib-components/src/layout/flex-helpers'
import ReturnButton, {
  ReturnButtonWrapper
} from '@evaka/lib-components/src/atoms/buttons/ReturnButton'
import DaycareApplicationVerificationView from '~applications/editor/verification/DaycareApplicationVerificationView'
import InlineButton from '@evaka/lib-components/src/atoms/buttons/InlineButton'
import { faAngleLeft, faCheck } from '@evaka/lib-icons'
import Checkbox from '@evaka/lib-components/src/atoms/form/Checkbox'
import { faExclamation } from '@evaka/lib-icons'
import { ApplicationDetails } from '@evaka/lib-common/src/api-types/application/ApplicationDetails'

type DaycareApplicationEditorProps = {
  apiData: ApplicationDetails
}

const applicationType = 'DAYCARE'

export default React.memo(function DaycareApplicationEditor({
  apiData
}: DaycareApplicationEditorProps) {
  const t = useTranslation()
  const [formData, setFormData] = useState<ApplicationFormData>(
    apiDataToFormData(apiData)
  )
  const [submitting, setSubmitting] = useState<boolean>(false)
  const [verifying, setVerifying] = useState<boolean>(false)
  const [verified, setVerified] = useState<boolean>(false)

  const history = useHistory()
  const { setErrorMessage, setInfoMessage, clearInfoMessage } = useContext(
    OverlayContext
  )

  const updateFeeFormData = useCallback(
    (feeData: FeeFormData) =>
      setFormData((previousState) => ({
        ...previousState,
        fee: feeData
      })),
    [setFormData]
  )

  const updateAdditionalDetailsFormData = useCallback(
    (additionalDetails: AdditionalDetailsFormData) =>
      setFormData((previousState) => ({
        ...previousState,
        additionalDetails: additionalDetails
      })),
    [setFormData]
  )

  const onVerify = () => {
    // todo: validation should happen here
    setVerified(false)
    setVerifying(true)
  }

  const onSaveDraft = () => {
    const reqBody = formDataToApiData(formData)
    setSubmitting(true)
    void saveApplicationDraft(apiData.id, reqBody).then((res) => {
      setSubmitting(false)
      if (res.isFailure) {
        setErrorMessage({
          title: t.applications.editor.actions.saveDraftError,
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
    const reqBody = formDataToApiData(formData)
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

  const renderEditor = () => (
    <>
      <Heading type={applicationType} />
      <Gap size="s" />
      <ServiceNeedSection
        formData={formData.serviceNeed}
        updateFormData={(data) =>
          setFormData((old) =>
            old
              ? {
                  ...old,
                  serviceNeed: {
                    ...old?.serviceNeed,
                    ...data
                  }
                }
              : old
          )
        }
      />
      <Gap size="s" />
      <UnitPreferenceSection
        formData={formData.unitPreference}
        updateFormData={(data) =>
          setFormData((old) =>
            old
              ? {
                  ...old,
                  unitPreference: {
                    ...old?.unitPreference,
                    ...data
                  }
                }
              : old
          )
        }
        applicationType={applicationType}
        preparatory={false}
        preferredStartDate={formData.serviceNeed.preferredStartDate}
      />
      <Gap size="s" />
      <ContactInfoSection
        formData={formData.contactInfo}
        updateFormData={(data) =>
          setFormData((old) =>
            old
              ? {
                  ...old,
                  contactInfo: {
                    ...old?.contactInfo,
                    ...data
                  }
                }
              : old
          )
        }
      />
      <Gap size="s" />
      <FeeSection formData={formData.fee} updateFormData={updateFeeFormData} />
      <Gap size="s" />
      <AdditionalDetailsSection
        formData={formData.additionalDetails}
        updateFormData={updateAdditionalDetailsFormData}
        applicationType={applicationType}
      />
    </>
  )

  const renderActionBar = () => (
    <Container>
      {verifying && (
        <>
          <Checkbox
            label={t.applications.editor.actions.hasVerified}
            checked={verified}
            onChange={setVerified}
          />
          <Gap size="s" />
        </>
      )}
      <FixedSpaceRow justifyContent="space-between">
        {verifying ? (
          <div />
        ) : (
          <Button
            text={t.applications.editor.actions.cancel}
            onClick={() => history.goBack()}
            disabled={submitting}
          />
        )}

        <FixedSpaceRow>
          {apiData.status === 'CREATED' && !verifying && (
            <Button
              text={t.applications.editor.actions.saveDraft}
              onClick={onSaveDraft}
              disabled={submitting}
            />
          )}
          {verifying ? (
            <>
              <Button
                text={t.applications.editor.actions.returnToEditBtn}
                onClick={() => setVerifying(false)}
                disabled={submitting}
              />
              <Button
                text={t.applications.editor.actions.send}
                onClick={onSend}
                disabled={submitting || !verified}
                primary
              />
            </>
          ) : (
            <Button
              text={t.applications.editor.actions.verify}
              onClick={onVerify}
              disabled={submitting}
              primary
            />
          )}
        </FixedSpaceRow>
      </FixedSpaceRow>
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

      {verifying ? (
        <DaycareApplicationVerificationView
          application={apiData}
          formData={formData}
        />
      ) : (
        renderEditor()
      )}
      <Gap size="m" />
      {renderActionBar()}
    </Container>
  )
})
