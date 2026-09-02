// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, {
  Suspense,
  useCallback,
  useEffect,
  useMemo,
  useState
} from 'react'
import styled from 'styled-components'
import { useSearchParams } from 'wouter'

import { combine } from 'lib-common/api'
import type { ApplicationFormData } from 'lib-common/application/ApplicationFormData'
import {
  apiDataToFormData,
  formDataToApiData
} from 'lib-common/application/ApplicationFormData'
import {
  applicationHasErrors,
  toApplicationTerms,
  validateApplication
} from 'lib-common/application/validations'
import type {
  ApplicationResponse,
  ApplicationUpdate
} from 'lib-common/generated/api-types/application'
import type { ThreadByApplicationResponse } from 'lib-common/generated/api-types/messaging'
import type { ApplicationId } from 'lib-common/generated/api-types/shared'
import type LocalDate from 'lib-common/local-date'
import { formatPersonName } from 'lib-common/names'
import {
  constantQuery,
  useChainedQuery,
  useQueryResult
} from 'lib-common/query'
import { useIdRouteParam } from 'lib-common/useRouteParams'
import AddButton from 'lib-components/atoms/buttons/AddButton'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { Gap } from 'lib-components/white-space'
import { featureFlags } from 'lib-customizations/employee'
import { faEnvelope } from 'lib-icons'

import { getEmployeeUrlPrefix } from '../../constants'
import type { Translations } from '../../state/i18n'
import { useTranslation } from '../../state/i18n'
import { useTitle } from '../../utils/useTitle'
import MetadataSection from '../archive-metadata/MetadataSection'
import { renderResult } from '../async-rendering'

import ApplicationActionsBar from './ApplicationActionsBar'
import ApplicationNotes from './ApplicationNotes'
import ApplicationReadView from './ApplicationReadView'
import {
  applicationDetailsQuery,
  applicationMetadataQuery,
  clubTermsQuery,
  preschoolTermsQuery,
  threadByApplicationIdQuery
} from './queries'

const ApplicationArea = styled(ContentArea)<{ $fullWidth: boolean }>`
  width: ${(p) => (p.$fullWidth ? '100%' : '77%')};
`

const SidebarArea = styled(ContentArea)`
  width: 23%;
  padding: 0;
`

const getMessageSubject = (
  i18n: Translations,
  applicationData: ApplicationResponse
) =>
  i18n.application.messageSubject(
    applicationData.application.sentDate?.format() ?? '',
    formatPersonName(
      applicationData.application.form.child.person,
      'First Last'
    )
  )

const ApplicationMetadataSection = React.memo(
  function ApplicationMetadataSection({
    applicationId
  }: {
    applicationId: ApplicationId
  }) {
    const result = useQueryResult(applicationMetadataQuery({ applicationId }))
    return <MetadataSection metadataResult={result} />
  }
)

// Lazily loaded: it is the employee frontend's only entry point into the
// citizen translation bundle, which would otherwise ship on every page.
const ApplicationEditView = React.lazy(() => import('./ApplicationEditView'))

export default React.memo(function ApplicationPageShared() {
  const applicationId = useIdRouteParam<ApplicationId>('id')
  const { i18n } = useTranslation()
  const [searchParams] = useSearchParams()
  const creatingNew = searchParams.get('create') === 'true'
  const [editing, setEditing] = useState(creatingNew)
  const [formData, setFormData] = useState<ApplicationFormData>()
  const [dueDate, setDueDate] = useState<LocalDate | null>(null)

  const updateFormData = useCallback(
    (update: (old: ApplicationFormData) => ApplicationFormData) =>
      setFormData((prev) => (prev === undefined ? prev : update(prev))),
    []
  )

  const application = useQueryResult(applicationDetailsQuery({ applicationId }))

  const formDataInitialized = formData !== undefined
  useEffect(() => {
    if (application.isSuccess && !formDataInitialized) {
      // oxlint-disable-next-line react/set-state-in-effect
      setFormData(apiDataToFormData(application.value.application, []))
      setDueDate(application.value.application.dueDate)
    }
  }, [application, formDataInitialized])

  useTitle(
    application.map(
      (value) =>
        `${i18n.application.tabTitle} - ${formatPersonName(value.application.form.child.person, 'Last First')}`
    )
  )

  const messageThread = useChainedQuery(
    application.map((a) =>
      a.permittedActions.includes('READ_SERVICE_WORKER_ACCOUNT_MESSAGES')
        ? threadByApplicationIdQuery({ applicationId })
        : constantQuery<ThreadByApplicationResponse>({ thread: null })
    )
  )

  const preschoolTerms = useChainedQuery(
    application.map((a) =>
      a.application.type === 'PRESCHOOL'
        ? preschoolTermsQuery()
        : constantQuery([])
    )
  )

  const clubTerms = useChainedQuery(
    application.map((a) =>
      a.application.type === 'CLUB' ? clubTermsQuery() : constantQuery([])
    )
  )

  const terms = useMemo(
    () =>
      combine(application, preschoolTerms, clubTerms)
        .map(([applicationData, preschoolTerms, clubTerms]) => {
          const applicationTerms = (onlyOpenForApplications: boolean) =>
            toApplicationTerms(
              applicationData.application.type,
              preschoolTerms,
              clubTerms,
              onlyOpenForApplications
            )
          return {
            // Employees record paper applications against any term, so
            // validation must accept them all.
            validation: applicationTerms(false),
            // The term list rendered in the form is guardian-facing guidance,
            // so it lists the same terms open for applications as the citizen
            // editor does.
            display: applicationTerms(true)
          }
        })
        .getOrElse({ validation: undefined, display: undefined }),
    [application, preschoolTerms, clubTerms]
  )

  const applicationDetails = application.isSuccess
    ? application.value.application
    : undefined

  // Validation only gates the Save button, which exists only while editing —
  // running it in read view would traverse the whole form for nothing.
  const errors = useMemo(
    () =>
      editing && applicationDetails !== undefined && formData !== undefined
        ? validateApplication(
            applicationDetails,
            formData,
            featureFlags,
            'employee',
            terms.validation
          )
        : undefined,
    [editing, applicationDetails, formData, terms]
  )
  const hasErrors = errors !== undefined && applicationHasErrors(errors)

  const applicationUpdate = useMemo(
    (): ApplicationUpdate | null =>
      applicationDetails !== undefined && formData !== undefined
        ? {
            form: formDataToApiData(applicationDetails, formData, {
              actor: 'employee',
              dailyTimes: featureFlags.daycareApplication.dailyTimes
            }),
            dueDate
          }
        : null,
    [applicationDetails, formData, dueDate]
  )

  const getSendMessageUrl = useCallback(
    (applicationData: ApplicationResponse) => {
      if (
        messageThread.isSuccess &&
        messageThread.value?.thread !== null &&
        messageThread.value.thread.messages.length > 0
      ) {
        return `${getEmployeeUrlPrefix()}/employee/messages/?applicationId=${
          applicationData.application.id
        }&messageBox=thread&threadId=${messageThread.value.thread.id}&reply=true`
      }
      return `${getEmployeeUrlPrefix()}/employee/messages/send?recipient=${
        applicationData.application.guardianId
      }&title=${getMessageSubject(i18n, applicationData)}&applicationId=${
        applicationData.application.id
      }`
    },
    [i18n, messageThread]
  )

  return (
    <>
      <Container>
        <ReturnButton label={i18n.common.goBack} data-qa="close-application" />
        {renderResult(application, (applicationData) => (
          <FixedSpaceRow>
            <ApplicationArea $opaque $fullWidth={editing}>
              {editing ? (
                formData !== undefined && errors !== undefined ? (
                  <Suspense fallback={null}>
                    <ApplicationEditView
                      application={applicationData.application}
                      formData={formData}
                      setFormData={updateFormData}
                      errors={errors}
                      terms={terms.display}
                      guardians={applicationData.guardians}
                      dueDate={dueDate}
                      setDueDate={setDueDate}
                    />
                  </Suspense>
                ) : null
              ) : (
                <ApplicationReadView application={applicationData} />
              )}
            </ApplicationArea>
            {!editing &&
              (applicationData.permittedActions.includes('READ_NOTES') ||
                applicationData.permittedActions.includes(
                  'READ_SPECIAL_EDUCATION_TEACHER_NOTES'
                )) && (
                <SidebarArea $opaque={false}>
                  <ApplicationNotes
                    applicationId={applicationId}
                    allowCreate={applicationData.permittedActions.includes(
                      'CREATE_NOTE'
                    )}
                  />
                  <Gap $size="m" />
                  {application.isSuccess &&
                    application.value.permittedActions.includes(
                      'READ_SERVICE_WORKER_ACCOUNT_MESSAGES'
                    ) && (
                      <AddButton
                        onClick={() =>
                          window.open(
                            getSendMessageUrl(applicationData),
                            '_blank'
                          )
                        }
                        text={i18n.application.messaging.sendMessage}
                        darker
                        icon={faEnvelope}
                        data-qa="send-message-button"
                      />
                    )}
                </SidebarArea>
              )}
          </FixedSpaceRow>
        ))}
        {!editing &&
          application.isSuccess &&
          application.value.permittedActions.includes('READ_METADATA') && (
            <>
              <Gap />
              <Container>
                <ApplicationMetadataSection applicationId={applicationId} />
              </Container>
            </>
          )}
      </Container>
      <Gap />
      {application.isSuccess &&
        application.value.permittedActions.includes('UPDATE') &&
        applicationUpdate !== null && (
          <ApplicationActionsBar
            applicationStatus={application.value.application.status}
            editing={editing}
            setEditing={setEditing}
            application={application.value.application}
            applicationUpdate={applicationUpdate}
            errors={hasErrors}
          />
        )}
    </>
  )
})
