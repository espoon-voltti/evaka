// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useContext, useEffect, useState } from 'react'
import { useSearchParams } from 'react-router'
import styled from 'styled-components'

import { combine } from 'lib-common/api'
import type FiniteDateRange from 'lib-common/finite-date-range'
import type {
  ApplicationDetails,
  ApplicationResponse,
  ApplicationType
} from 'lib-common/generated/api-types/application'
import type { PublicUnit } from 'lib-common/generated/api-types/daycare'
import type { PlacementType } from 'lib-common/generated/api-types/placement'
import type { ApplicationId } from 'lib-common/generated/api-types/shared'
import LocalDate from 'lib-common/local-date'
import {
  constantQuery,
  pendingQuery,
  useChainedQuery,
  useQueryResult
} from 'lib-common/query'
import { useIdRouteParam } from 'lib-common/useRouteParams'
import { useDebounce } from 'lib-common/utils/useDebounce'
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
import type { TitleState } from '../../state/title'
import { TitleContext } from '../../state/title'
import { asUnitType } from '../../types/daycare'
import { isSsnValid, isTimeValid } from '../../utils/validation/validations'
import { serviceNeedPublicInfosQuery } from '../applications/queries'
import MetadataSection from '../archive-metadata/MetadataSection'
import { renderResult } from '../async-rendering'

import ApplicationActionsBar from './ApplicationActionsBar'
import ApplicationEditView from './ApplicationEditView'
import ApplicationNotes from './ApplicationNotes'
import ApplicationReadView from './ApplicationReadView'
import {
  applicationDetailsQuery,
  applicationMetadataQuery,
  applicationUnitsQuery,
  clubTermsQuery,
  preschoolTermsQuery,
  threadByApplicationIdQuery
} from './queries'

const ApplicationArea = styled(ContentArea)`
  width: 77%;
`

const SidebarArea = styled(ContentArea)`
  width: 23%;
  padding: 0;
`

const placementTypeFilters: Record<ApplicationType, PlacementType[]> = {
  DAYCARE: ['DAYCARE', 'DAYCARE_PART_TIME'],
  PRESCHOOL: ['PRESCHOOL_DAYCARE', 'PRESCHOOL_CLUB'],
  CLUB: []
}

const getMessageSubject = (
  i18n: Translations,
  applicationData: ApplicationResponse
) =>
  i18n.application.messageSubject(
    applicationData.application.sentDate?.format() ?? '',
    `${applicationData.application.form.child.person.firstName} ${applicationData.application.form.child.person.lastName}`
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

export default React.memo(function ApplicationPage() {
  const applicationId = useIdRouteParam<ApplicationId>('id')

  const { i18n } = useTranslation()
  const { setTitle, formatTitleName } = useContext<TitleState>(TitleContext)

  const [searchParams] = useSearchParams()
  const creatingNew = searchParams.get('create') === 'true'
  const [editing, setEditing] = useState(creatingNew)
  const [editedApplication, setEditedApplication] =
    useState<ApplicationDetails>()
  const [validationErrors, setValidationErrors] = useState<
    Record<string, string>
  >({})

  const application = useQueryResult(applicationDetailsQuery({ applicationId }))

  const editedApplicationInitialized = editedApplication !== undefined
  useEffect(() => {
    if (application.isSuccess) {
      const { firstName, lastName } =
        application.value.application.form.child.person
      setTitle(
        `${i18n.application.tabTitle} - ${formatTitleName(firstName, lastName)}`
      )
      if (!editedApplicationInitialized) {
        setEditedApplication(application.value.application)
      }
    }
  }, [
    application,
    formatTitleName,
    i18n.application.tabTitle,
    setTitle,
    editedApplicationInitialized
  ])

  const messageThread = useQueryResult(
    threadByApplicationIdQuery({ applicationId })
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

  const terms = combine(application, preschoolTerms, clubTerms)
    .map(([application, preschoolTerms, clubTerms]) =>
      application.application.type === 'PRESCHOOL'
        ? preschoolTerms.map((term) => term.extendedTerm)
        : application.application.type === 'CLUB'
          ? clubTerms.map(({ term }) => term)
          : undefined
    )
    .getOrElse(undefined)

  const units = useQueryResult(
    editing && editedApplication
      ? applicationUnitsQuery({
          type: asUnitType(
            editedApplication.type === 'PRESCHOOL' &&
              editedApplication.form.preferences.preparatory
              ? 'PREPARATORY'
              : editedApplication.type
          ),
          date:
            editedApplication.form.preferences.preferredStartDate ??
            LocalDate.todayInSystemTz(),
          shiftCare: null
        })
      : pendingQuery<PublicUnit[]>()
  )

  // this is used because text inputs become too sluggish without it
  const debouncedEditedApplication = useDebounce(editedApplication, 50)

  useEffect(() => {
    if (debouncedEditedApplication && units.isSuccess) {
      setValidationErrors(
        validateApplication(
          debouncedEditedApplication,
          units.value,
          terms,
          i18n
        )
      )
    }
  }, [debouncedEditedApplication]) // eslint-disable-line react-hooks/exhaustive-deps

  const shouldLoadServiceNeedOptions =
    editedApplication !== undefined &&
    ((editedApplication.type === 'DAYCARE' &&
      featureFlags.daycareApplication.serviceNeedOption) ||
      (editedApplication.type === 'PRESCHOOL' &&
        featureFlags.preschoolApplication.serviceNeedOption))

  const serviceNeedOptions = useQueryResult(
    shouldLoadServiceNeedOptions
      ? serviceNeedPublicInfosQuery({
          placementTypes: placementTypeFilters[editedApplication.type]
        })
      : constantQuery([])
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
        {renderResult(
          combine(application, serviceNeedOptions),
          ([applicationData, serviceNeedOptions]) => (
            <FixedSpaceRow>
              <ApplicationArea opaque>
                {editing ? (
                  editedApplication ? (
                    <ApplicationEditView
                      application={editedApplication}
                      setApplication={setEditedApplication}
                      errors={validationErrors}
                      units={units}
                      guardians={applicationData.guardians}
                      serviceNeedOptions={serviceNeedOptions}
                    />
                  ) : null
                ) : (
                  <ApplicationReadView application={applicationData} />
                )}
              </ApplicationArea>
              {(applicationData.permittedActions.includes('READ_NOTES') ||
                applicationData.permittedActions.includes(
                  'READ_SPECIAL_EDUCATION_TEACHER_NOTES'
                )) && (
                <SidebarArea opaque={false}>
                  <ApplicationNotes
                    applicationId={applicationId}
                    allowCreate={applicationData.permittedActions.includes(
                      'CREATE_NOTE'
                    )}
                  />
                  <Gap size="m" />
                  <AddButton
                    onClick={() =>
                      window.open(getSendMessageUrl(applicationData), '_blank')
                    }
                    text={i18n.application.messaging.sendMessage}
                    darker
                    icon={faEnvelope}
                    data-qa="send-message-button"
                  />
                </SidebarArea>
              )}
            </FixedSpaceRow>
          )
        )}
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
        editedApplication && (
          <ApplicationActionsBar
            applicationStatus={application.value.application.status}
            editing={editing}
            setEditing={setEditing}
            editedApplication={editedApplication}
            errors={Object.keys(validationErrors).length > 0}
          />
        )}
    </>
  )
})

function validateApplication(
  application: ApplicationDetails,
  units: PublicUnit[],
  terms: FiniteDateRange[] | undefined,
  i18n: Translations
): Record<string, string> {
  const errors: Record<string, string> = {}

  const {
    form: { child, preferences, otherPartner, otherChildren }
  } = application

  const preferredStartDate = preferences.preferredStartDate
  if (!preferredStartDate) {
    errors['form.preferences.preferredStartDate'] =
      i18n.validationError.mandatoryField
  }

  if (
    terms &&
    preferredStartDate &&
    !terms.some((term) => term.includes(preferredStartDate))
  ) {
    errors['form.preferences.preferredStartDate'] =
      i18n.validationError.startDateNotOnTerm
  }

  if (preferences.preferredUnits.length === 0) {
    errors['form.preferences.preferredUnits'] =
      i18n.application.preferences.missingPreferredUnits
  }

  if (
    preferences.preferredUnits.some(
      ({ id }) => units.find((unit) => unit.id === id) === undefined
    )
  ) {
    errors['form.preferences.preferredUnits'] =
      i18n.application.preferences.unitMismatch
  }

  if (
    preferences.serviceNeed !== null &&
    ((application.type === 'DAYCARE' &&
      featureFlags.daycareApplication.dailyTimes) ||
      (application.type === 'PRESCHOOL' &&
        !featureFlags.preschoolApplication.serviceNeedOption))
  ) {
    if (!preferences.serviceNeed.startTime) {
      errors['form.preferences.serviceNeed.startTime'] =
        i18n.validationError.mandatoryField
    } else if (!isTimeValid(preferences.serviceNeed.startTime)) {
      errors['form.preferences.serviceNeed.startTime'] =
        i18n.validationError.time
    }

    if (!preferences.serviceNeed.endTime) {
      errors['form.preferences.serviceNeed.endTime'] =
        i18n.validationError.mandatoryField
    } else if (!isTimeValid(preferences.serviceNeed.endTime)) {
      errors['form.preferences.serviceNeed.endTime'] = i18n.validationError.time
    }
  }

  if (
    application.type === 'PRESCHOOL' &&
    preferences.serviceNeed !== null &&
    featureFlags.preschoolApplication.connectedDaycarePreferredStartDate
  ) {
    const connectedDaycarePreferredStartDate =
      preferences.connectedDaycarePreferredStartDate
    if (!connectedDaycarePreferredStartDate) {
      errors['form.preferences.connectedDaycarePreferredStartDate'] =
        i18n.validationError.mandatoryField
    }
  }

  if (
    application.type === 'PRESCHOOL' &&
    preferences.serviceNeed !== null &&
    featureFlags.preschoolApplication.serviceNeedOption
  ) {
    if (!preferences.serviceNeed?.serviceNeedOption) {
      errors['form.preferences.serviceNeed.serviceNeedOption'] =
        i18n.validationError.mandatoryField
    }
  }

  if (
    otherPartner &&
    otherPartner.socialSecurityNumber &&
    !isSsnValid(otherPartner.socialSecurityNumber)
  ) {
    errors['form.otherPartner.socialSecurityNumber'] = i18n.validationError.ssn
  }

  otherChildren.forEach(({ socialSecurityNumber }, index) => {
    if (socialSecurityNumber && !isSsnValid(socialSecurityNumber)) {
      errors[`form.otherChildren.${index}.socialSecurityNumber`] =
        i18n.validationError.ssn
    }
  })

  if (
    child.assistanceNeeded &&
    child.assistanceDescription.trim().length === 0
  ) {
    errors['form.child.assistanceDescription'] =
      i18n.validationError.mandatoryField
  }

  return errors
}
