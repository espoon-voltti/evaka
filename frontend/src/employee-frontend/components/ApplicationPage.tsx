// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useEffect, useState } from 'react'
import { RouteComponentProps } from 'react-router-dom'
import LocalDate from 'lib-common/local-date'
import { Loading, Result, Success } from 'lib-common/api'
import { ApplicationResponse } from '../types/application'
import ApplicationEditView from '../components/application-page/ApplicationEditView'
import ApplicationReadView from '../components/application-page/ApplicationReadView'
import ApplicationActionsBar from '../components/application-page/ApplicationActionsBar'
import {
  getApplication,
  getClubTerms,
  getPreschoolTerms
} from '../api/applications'
import { getApplicationUnits } from '../api/daycare'
import { TitleContext, TitleState } from '../state/title'
import { useTranslation, Translations } from '../state/i18n'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { Gap } from 'lib-components/white-space'
import styled from 'styled-components'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import ApplicationNotes from '../components/application-page/ApplicationNotes'
import { useDebounce } from 'lib-common/utils/useDebounce'
import { isSsnValid, isTimeValid } from '../utils/validation/validations'
import { UserContext } from '../state/user'
import { hasRole, RequireRole } from '../utils/roles'
import { ApplicationDetails } from 'lib-common/api-types/application/ApplicationDetails'
import { PublicUnit } from 'lib-common/api-types/units/PublicUnit'
import { featureFlags } from 'lib-customizations/employee'
import { ServiceNeedOptionPublicInfo } from 'lib-common/api-types/serviceNeed/common'
import { useRestApi } from 'lib-common/utils/useRestApi'
import { getServiceNeedOptionPublicInfos } from '../api/child/service-needs'
import Loader from 'lib-components/atoms/Loader'
import ErrorSegment from 'lib-components/atoms/state/ErrorSegment'
import { renderResult } from './async-rendering'
import { UUID } from 'lib-common/types'

const ApplicationArea = styled(ContentArea)`
  width: 77%;
`

const NotesArea = styled(ContentArea)`
  width: 23%;
  padding: 0;
`

function ApplicationPage({ match }: RouteComponentProps<{ id: UUID }>) {
  const applicationId = match.params.id

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

  const { roles } = useContext(UserContext)
  const enableApplicationActions =
    hasRole(roles, 'SERVICE_WORKER') ||
    hasRole(roles, 'FINANCE_ADMIN') ||
    hasRole(roles, 'ADMIN')

  useEffect(() => {
    if (editing && editedApplication?.type) {
      const applicationType =
        editedApplication.type === 'PRESCHOOL' &&
        editedApplication.form.preferences.preparatory
          ? 'PREPARATORY'
          : editedApplication.type

      void getApplicationUnits(
        applicationType,
        editedApplication.form.preferences.preferredStartDate ??
          LocalDate.today()
      ).then(setUnits)
    }
  }, [
    editing,
    editedApplication?.type,
    editedApplication?.form.preferences.preferredStartDate,
    editedApplication?.form.preferences.preparatory
  ])

  const [terms, setTerms] = useState<Term[]>()
  useEffect(() => {
    switch (
      application.map(({ application: { type } }) => type).getOrElse(undefined)
    ) {
      case 'PRESCHOOL':
        void getPreschoolTerms().then((res) =>
          setTerms(
            res
              .map((terms) => terms.map((term) => term.extendedTerm))
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
  }, [application, setTerms])

  // this is used because text inputs become too sluggish without it
  const debouncedEditedApplication = useDebounce(editedApplication, 50)

  const reloadApplication = () => {
    setPosition(window.scrollY)

    setApplication(Loading.of())
    void getApplication(applicationId).then((result) => {
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
    window.scrollTo({ left: 0, top: position })
  }, [application]) // eslint-disable-line react-hooks/exhaustive-deps

  const [serviceNeedOptions, setServiceNeedOptions] = useState<
    Result<ServiceNeedOptionPublicInfo[]>
  >(Loading.of())

  const loadServiceNeedOptions = useRestApi(
    getServiceNeedOptionPublicInfos,
    setServiceNeedOptions
  )

  const shouldLoadServiceNeedOptions =
    editedApplication !== undefined &&
    editedApplication.type === 'DAYCARE' &&
    editedApplication.form.preferences.serviceNeed !== null &&
    // If service need options are not enabled, backend sets to null
    editedApplication.form.preferences.serviceNeed.serviceNeedOption !== null

  useEffect(() => {
    if (!editing) {
      return
    }
    if (shouldLoadServiceNeedOptions) {
      loadServiceNeedOptions(['DAYCARE', 'DAYCARE_PART_TIME'])
    } else {
      setServiceNeedOptions((prev) => (prev.isLoading ? Success.of([]) : prev))
    }
  }, [
    setServiceNeedOptions,
    loadServiceNeedOptions,
    shouldLoadServiceNeedOptions,
    editing
  ])

  const renderApplication = (applicationData: ApplicationResponse) =>
    editing ? (
      editedApplication ? (
        <>
          {serviceNeedOptions.isLoading && <Loader />}
          {serviceNeedOptions.isFailure && (
            <ErrorSegment
              title={i18n.application.serviceNeed.error.getServiceNeedOptions}
            />
          )}
          {serviceNeedOptions.isSuccess && (
            <ApplicationEditView
              application={editedApplication}
              setApplication={setEditedApplication}
              errors={validationErrors}
              units={units}
              guardians={applicationData.guardians}
              serviceNeedOptions={serviceNeedOptions.value}
            />
          )}
        </>
      ) : null
    ) : (
      <ApplicationReadView
        application={applicationData}
        reloadApplication={reloadApplication}
      />
    )

  return (
    <>
      <Gap size={'L'} />
      <Container>
        <ReturnButton label={i18n.common.goBack} data-qa="close-application" />
        <FixedSpaceRow>
          <ApplicationArea opaque>
            {renderResult(application, renderApplication)}
          </ApplicationArea>
          <RequireRole
            oneOf={['ADMIN', 'SERVICE_WORKER', 'SPECIAL_EDUCATION_TEACHER']}
          >
            <NotesArea opaque={false}>
              <ApplicationNotes applicationId={applicationId} />
            </NotesArea>
          </RequireRole>
        </FixedSpaceRow>
      </Container>

      {application.isSuccess &&
      enableApplicationActions &&
      editedApplication ? (
        <ApplicationActionsBar
          applicationStatus={application.value.application.status}
          editing={editing}
          setEditing={setEditing}
          editedApplication={editedApplication}
          reloadApplication={reloadApplication}
          errors={Object.keys(validationErrors).length > 0}
        />
      ) : null}
    </>
  )
}

interface Term {
  start: LocalDate
  end: LocalDate
}

function validateApplication(
  application: ApplicationDetails,
  units: PublicUnit[],
  terms: Term[] | undefined,
  i18n: Translations
): Record<string, string> {
  const errors = {}

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
    !terms.some((term) => preferredStartDate.isBetween(term.start, term.end))
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
      featureFlags.daycareApplication.dailyTimesEnabled) ||
      application.type === 'PRESCHOOL')
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

export default ApplicationPage
