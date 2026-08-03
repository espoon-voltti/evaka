// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback } from 'react'

import type { ApplicationFormData } from 'lib-common/application/ApplicationFormData'
import type {
  ApplicationFormDataErrors,
  Term
} from 'lib-common/application/validations'
import type { ApplicationDetails } from 'lib-common/generated/api-types/application'
import type { PersonJSON } from 'lib-common/generated/api-types/pis'
import type LocalDate from 'lib-common/local-date'
import {
  constantQuery,
  useMutationResult,
  useQueryResult
} from 'lib-common/query'
import AdditionalDetailsSection from 'lib-components/application-editor/AdditionalDetailsSection'
import ContactInfoSection from 'lib-components/application-editor/contact-info/ContactInfoSection'
import ServiceNeedSection from 'lib-components/application-editor/service-need/ServiceNeedSection'
import type { ApplicationEditorDeps } from 'lib-components/application-editor/types'
import UnitPreferenceSection from 'lib-components/application-editor/unit-preference/UnitPreferenceSection'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import CollapsibleSection from 'lib-components/molecules/CollapsibleSection'
import FileUpload from 'lib-components/molecules/FileUpload'
import DatePicker from 'lib-components/molecules/date-picker/DatePicker'
import { Gap } from 'lib-components/white-space'
import { faInfo, faUserFriends } from 'lib-icons'

import { useTranslation } from '../../state/i18n'

import ApplicationStatusSection from './ApplicationStatusSection'
import ApplicationTitle from './ApplicationTitle'
import VTJGuardian from './VTJGuardian'

interface Props {
  application: ApplicationDetails
  formData: ApplicationFormData
  setFormData: (
    update: (old: ApplicationFormData) => ApplicationFormData
  ) => void
  errors: ApplicationFormDataErrors
  terms: Term[] | undefined
  guardians: PersonJSON[]
  dueDate: LocalDate | null
  setDueDate: (d: LocalDate | null) => void
  deps: ApplicationEditorDeps
}

export default React.memo(function ApplicationEditView({
  application,
  formData,
  setFormData,
  errors,
  terms,
  guardians,
  dueDate,
  setDueDate,
  deps
}: Props) {
  const { i18n } = useTranslation()
  const {
    featureFlags,
    serviceNeedOptionPublicInfosQuery,
    renderResult,
    lang
  } = deps
  const type = application.type

  const serviceNeedOptions = useQueryResult(
    type === 'DAYCARE' && featureFlags.daycareApplication.serviceNeedOption
      ? serviceNeedOptionPublicInfosQuery({
          placementTypes: ['DAYCARE', 'DAYCARE_PART_TIME']
        })
      : type === 'PRESCHOOL' &&
          featureFlags.preschoolApplication.serviceNeedOption
        ? serviceNeedOptionPublicInfosQuery({
            placementTypes: [
              'PRESCHOOL_DAYCARE',
              ...(application.form.preferences.serviceNeed?.serviceNeedOption
                ?.validPlacementType === 'PRESCHOOL_CLUB'
                ? (['PRESCHOOL_CLUB'] as const)
                : [])
            ]
          })
        : constantQuery([])
  )

  const updateServiceNeed = useCallback(
    (data: Partial<ApplicationFormData['serviceNeed']>) =>
      setFormData((old) => ({
        ...old,
        serviceNeed: { ...old.serviceNeed, ...data }
      })),
    [setFormData]
  )
  const updateUnitPreference = useCallback(
    (
      fn: (
        prev: ApplicationFormData['unitPreference']
      ) => Partial<ApplicationFormData['unitPreference']>
    ) =>
      setFormData((old) => ({
        ...old,
        unitPreference: { ...old.unitPreference, ...fn(old.unitPreference) }
      })),
    [setFormData]
  )
  const updateContactInfo = useCallback(
    (data: Partial<ApplicationFormData['contactInfo']>) =>
      setFormData((old) => ({
        ...old,
        contactInfo: { ...old.contactInfo, ...data }
      })),
    [setFormData]
  )
  const updateAdditionalDetails = useCallback(
    (data: Partial<ApplicationFormData['additionalDetails']>) =>
      setFormData((old) => ({
        ...old,
        additionalDetails: { ...old.additionalDetails, ...data }
      })),
    [setFormData]
  )

  const otherGuardian = guardians.find((g) => g.id !== application.guardianId)
  const fullFamily =
    type === 'DAYCARE' ||
    (type === 'PRESCHOOL' && formData.serviceNeed.connectedDaycare)

  const { mutateAsync: deleteAttachment } = useMutationResult(
    deps.deleteAttachmentMutation
  )

  return (
    <div data-qa="application-edit-view">
      <ApplicationTitle application={application} />
      <Gap />
      {renderResult(serviceNeedOptions, (serviceNeedOptions) => (
        <FixedSpaceColumn $spacing="s">
          <ServiceNeedSection
            deps={deps}
            applicationId={application.id}
            status={application.status}
            isInvalidDate={undefined}
            type={type}
            formData={formData.serviceNeed}
            updateFormData={updateServiceNeed}
            errors={errors.serviceNeed}
            verificationRequested={true}
            terms={terms}
            serviceNeedOptions={serviceNeedOptions}
          />
          <UnitPreferenceSection
            deps={deps}
            formData={formData.unitPreference}
            updateFormData={updateUnitPreference}
            applicationType={type}
            preparatory={
              type === 'PRESCHOOL' && formData.serviceNeed.preparatory
            }
            preferredStartDate={formData.serviceNeed.preferredStartDate}
            errors={errors.unitPreference}
            verificationRequested={true}
            shiftCare={formData.serviceNeed.shiftCare}
          />
          <ContactInfoSection
            deps={deps}
            type={type}
            application={application}
            formData={formData.contactInfo}
            updateFormData={updateContactInfo}
            errors={errors.contactInfo}
            verificationRequested={true}
            fullFamily={fullFamily}
            otherGuardianStatus={
              application.hasOtherGuardian
                ? application.otherGuardianLivesInSameAddress
                  ? 'SAME_ADDRESS'
                  : 'DIFFERENT_ADDRESS'
                : 'NO'
            }
          />
          <AdditionalDetailsSection
            deps={deps}
            formData={formData.additionalDetails}
            updateFormData={updateAdditionalDetails}
            errors={errors.additionalDetails}
            verificationRequested={true}
            applicationType={type}
          />

          <CollapsibleSection
            title={i18n.application.guardians.title}
            icon={faUserFriends}
          >
            <VTJGuardian guardianId={otherGuardian?.id} />
          </CollapsibleSection>

          <CollapsibleSection
            title={
              i18n.application.additionalInfo.serviceWorkerAttachmentsTitle
            }
            icon={faInfo}
          >
            <FileUpload
              uploadHandler={deps.applicationAttachmentUploadHandler(
                application.id,
                'SERVICE_WORKER_ATTACHMENT',
                deleteAttachment
              )}
              getDownloadUrl={deps.getAttachmentUrl}
              files={application.attachments.filter(
                (a) => a.type === 'SERVICE_WORKER_ATTACHMENT'
              )}
              data-qa="file-upload-service-worker"
            />
          </CollapsibleSection>

          <ApplicationStatusSection
            application={application}
            dueDateEditor={
              <DatePicker
                date={dueDate}
                onChange={setDueDate}
                locale={lang}
                data-qa="due-date"
              />
            }
          />
        </FixedSpaceColumn>
      ))}
    </div>
  )
})
