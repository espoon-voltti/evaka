// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useContext, useEffect, useState } from 'react'
import styled from 'styled-components'

import { combine, Loading, Result, Success, wrapResult } from 'lib-common/api'
import FiniteDateRange from 'lib-common/finite-date-range'
import {
  ApplicationDetails,
  ApplicationResponse,
  ApplicationType
} from 'lib-common/generated/api-types/application'
import { PublicUnit } from 'lib-common/generated/api-types/daycare'
import { PlacementType } from 'lib-common/generated/api-types/placement'
import { ServiceNeedOptionPublicInfo } from 'lib-common/generated/api-types/serviceneed'
import LocalDate from 'lib-common/local-date'
import useRouteParams from 'lib-common/useRouteParams'
import { scrollToPos } from 'lib-common/utils/scrolling'
import { useDebounce } from 'lib-common/utils/useDebounce'
import { useApiState, useRestApi } from 'lib-common/utils/useRestApi'
import AddButton from 'lib-components/atoms/buttons/AddButton'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { Gap } from 'lib-components/white-space'
import { featureFlags } from 'lib-customizations/employee'
import { faEnvelope } from 'lib-icons'

import ApplicationActionsBar from '../components/application-page/ApplicationActionsBar'
import ApplicationEditView from '../components/application-page/ApplicationEditView'
import ApplicationNotes from '../components/application-page/ApplicationNotes'
import ApplicationReadView from '../components/application-page/ApplicationReadView'
import { getEmployeeUrlPrefix } from '../constants'
import { getApplicationDetails } from '../generated/api-clients/application'
import {
  getApplicationUnits,
  getClubTerms,
  getPreschoolTerms
} from '../generated/api-clients/daycare'
import { getThreadByApplicationId } from '../generated/api-clients/messaging'
import { getServiceNeedOptionPublicInfos } from '../generated/api-clients/serviceneed'
import { Translations, useTranslation } from '../state/i18n'
import { TitleContext, TitleState } from '../state/title'
import { asUnitType } from '../types/daycare'
import { isSsnValid, isTimeValid } from '../utils/validation/validations'

import { renderResult, UnwrapResult } from './async-rendering'

const getServiceNeedOptionPublicInfosResult = wrapResult(
  getServiceNeedOptionPublicInfos
)
const getApplicationResult = wrapResult(getApplicationDetails)
const getClubTermsResult = wrapResult(getClubTerms)
const getPreschoolTermsResult = wrapResult(getPreschoolTerms)
const getApplicationUnitsResult = wrapResult(getApplicationUnits)
const getThreadByApplicationIdResult = wrapResult(getThreadByApplicationId)

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

export default React.memo(function ApplicationPage() {
  const { id: applicationId } = useRouteParams(['id'])

  const { i18n } = useTranslation()
  const { setTitle, formatTitleName } = useContext<TitleState>(TitleContext)
  const [position, setPosition] = useState(0)
  const [application, setApplication] = useState<Result<ApplicationResponse>>(
    Loading.of()
  )
  const creatingNew = window.location.href.includes('create=true')
  const [editing, setEditing] = useState(creatingNew)
  const [editedApplication, setEditedApplication] =
    useState<ApplicationDetails>()
  const [validationErrors, setValidationErrors] = useState<
    Record<string, string>
  >({})
  const [units, setUnits] = useState<Result<PublicUnit[]>>(Loading.of())

  const [messageThread] = useApiState(
    () => getThreadByApplicationIdResult({ applicationId }),
    [applicationId]
  )

  useEffect(() => {
    if (editing && editedApplication?.type) {
      const applicationType =
        editedApplication.type === 'PRESCHOOL' &&
        editedApplication.form.preferences.preparatory
          ? 'PREPARATORY'
          : editedApplication.type

      void getApplicationUnitsResult({
        type: asUnitType(applicationType),
        date:
          editedApplication.form.preferences.preferredStartDate ??
          LocalDate.todayInSystemTz(),
        shiftCare: null
      }).then(setUnits)
    }
  }, [
    editing,
    editedApplication?.type,
    editedApplication?.form.preferences.preferredStartDate,
    editedApplication?.form.preferences.preparatory
  ])

  const [terms, setTerms] = useState<FiniteDateRange[]>()
  useEffect(() => {
    switch (
      application.map(({ application: { type } }) => type).getOrElse(undefined)
    ) {
      case 'PRESCHOOL':
        void getPreschoolTermsResult().then((res) =>
          setTerms(
            res
              .map((terms) => terms.map((term) => term.extendedTerm))
              .getOrElse([])
          )
        )
        break
      case 'CLUB':
        void getClubTermsResult().then((res) =>
          setTerms(
            res.map((terms) => terms.map(({ term }) => term)).getOrElse([])
          )
        )
        break
    }
  }, [application, setTerms])

  // this is used because text inputs become too sluggish without it
  const debouncedEditedApplication = useDebounce(editedApplication, 50)

  const reloadApplication = () => {
    setPosition(window.scrollY)

    setApplication(Loading.of())
    void getApplicationResult({ applicationId }).then((result) => {
      setApplication(result)
      if (result.isSuccess) {
        const { firstName, lastName } =
          result.value.application.form.child.person
        setTitle(
          `${i18n.application.tabTitle} - ${formatTitleName(
            firstName,
            lastName
          )}`
        )
        setEditedApplication(result.value.application)
      }
    })
  }

  useEffect(reloadApplication, [applicationId]) // eslint-disable-line react-hooks/exhaustive-deps

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

  useEffect(() => {
    scrollToPos({ left: 0, top: position })
  }, [application]) // eslint-disable-line react-hooks/exhaustive-deps

  const [serviceNeedOptions, setServiceNeedOptions] = useState<
    Result<ServiceNeedOptionPublicInfo[]>
  >(Loading.of())

  const loadServiceNeedOptions = useRestApi(
    getServiceNeedOptionPublicInfosResult,
    setServiceNeedOptions
  )

  const shouldLoadServiceNeedOptions =
    editedApplication !== undefined &&
    ((editedApplication.type === 'DAYCARE' &&
      editedApplication.form.preferences.serviceNeed !== null &&
      // If service need options are not enabled, backend sets to null
      editedApplication.form.preferences.serviceNeed.serviceNeedOption !==
        null) ||
      (editedApplication.type === 'PRESCHOOL' &&
        featureFlags.preschoolApplication.serviceNeedOption))

  useEffect(() => {
    if (shouldLoadServiceNeedOptions) {
      void loadServiceNeedOptions({
        placementTypes: placementTypeFilters[editedApplication.type]
      })
    } else {
      setServiceNeedOptions((prev) => (prev.isLoading ? Success.of([]) : prev))
    }
  }, [
    setServiceNeedOptions,
    loadServiceNeedOptions,
    shouldLoadServiceNeedOptions,
    editedApplication?.type
  ])

  const getSendMessageUrl = useCallback(
    (applicationData: ApplicationResponse) => {
      if (
        messageThread.isSuccess &&
        messageThread.value !== null &&
        messageThread.value.messages.length > 0
      ) {
        return `${getEmployeeUrlPrefix()}/employee/messages/?applicationId=${
          applicationData.application.id
        }&messageBox=thread&threadId=${messageThread.value.id}&reply=true`
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
                  <ApplicationReadView
                    application={applicationData}
                    reloadApplication={reloadApplication}
                  />
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
      </Container>

      <UnwrapResult
        result={application}
        loading={() => null}
        failure={() => null}
      >
        {(application) =>
          application.permittedActions.includes('UPDATE') &&
          editedApplication ? (
            <ApplicationActionsBar
              applicationStatus={application.application.status}
              editing={editing}
              setEditing={setEditing}
              editedApplication={editedApplication}
              reloadApplication={reloadApplication}
              errors={Object.keys(validationErrors).length > 0}
            />
          ) : null
        }
      </UnwrapResult>
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
