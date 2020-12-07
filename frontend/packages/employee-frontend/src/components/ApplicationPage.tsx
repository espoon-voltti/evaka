// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useEffect, useState } from 'react'
import { RouteComponentProps } from 'react-router-dom'
import LocalDate from '@evaka/lib-common/src/local-date'
import { UUID } from 'types'
import { Loading, Result } from 'api'
import {
  ApplicationDetails,
  ApplicationResponse,
  PreferredUnit
} from 'types/application'
import ApplicationEditView from 'components/application-page/ApplicationEditView'
import ApplicationReadView from 'components/application-page/ApplicationReadView'
import ApplicationActionsBar from 'components/application-page/ApplicationActionsBar'
import { getApplication } from 'api/applications'
import { getApplicationUnits } from 'api/daycare'
import { renderResult } from 'components/shared/atoms/state/async-rendering'
import { TitleContext, TitleState } from 'state/title'
import { useTranslation, Translations } from 'state/i18n'
import { Container, ContentArea } from '~components/shared/layout/Container'
import { Gap } from 'components/shared/layout/white-space'
import styled from 'styled-components'
import { FixedSpaceRow } from 'components/shared/layout/flex-helpers'
import ReturnButton from 'components/shared/atoms/buttons/ReturnButton'
import ApplicationNotes from 'components/application-page/ApplicationNotes'
import { useDebounce } from 'utils/useDebounce'
import { isSsnValid, isTimeValid } from 'utils/validation/validations'
import { UserContext } from '~state/user'
import { hasRole } from '~utils/roles'

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
  const [application, setApplication] = useState<Result<ApplicationResponse>>(
    Loading.of()
  )
  const creatingNew = window.location.href.includes('create=true')
  const [editing, setEditing] = useState(creatingNew)
  const [editedApplication, setEditedApplication] = useState<
    ApplicationDetails
  >()
  const [validationErrors, setValidationErrors] = useState<
    Record<string, string>
  >({})
  const [units, setUnits] = useState<Result<PreferredUnit[]>>(Loading.of())

  const { roles } = useContext(UserContext)
  const enableApplicationActions =
    hasRole(roles, 'SERVICE_WORKER') ||
    hasRole(roles, 'FINANCE_ADMIN') ||
    hasRole(roles, 'ADMIN')

  useEffect(() => {
    if (editing && editedApplication) {
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
    editedApplication?.form.preferences.preferredStartDate
  ])

  // this is used because text inputs become too sluggish without it
  const debouncedEditedApplication = useDebounce(editedApplication, 50)

  const reloadApplication = () => {
    setApplication(Loading.of())
    void getApplication(applicationId).then((result) => {
      setApplication(result)
      if (result.isSuccess) {
        const {
          firstName,
          lastName
        } = result.value.application.form.child.person
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

  useEffect(reloadApplication, [applicationId])

  useEffect(() => {
    if (debouncedEditedApplication && units.isSuccess) {
      setValidationErrors(
        validateApplication(debouncedEditedApplication, units.value, i18n)
      )
    }
  }, [debouncedEditedApplication])

  const renderApplication = (applicationData: ApplicationResponse) =>
    editing ? (
      editedApplication ? (
        <ApplicationEditView
          application={editedApplication}
          setApplication={setEditedApplication}
          errors={validationErrors}
          units={units}
          guardians={applicationData.guardians}
        />
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
        <ReturnButton dataQa="close-application" />
        <FixedSpaceRow>
          <ApplicationArea opaque>
            {renderResult(application, renderApplication)}
          </ApplicationArea>
          <NotesArea opaque={false}>
            <ApplicationNotes applicationId={applicationId} />
          </NotesArea>
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

function validateApplication(
  application: ApplicationDetails,
  units: PreferredUnit[],
  i18n: Translations
): Record<string, string> {
  const errors = {}

  const {
    form: { preferences, otherPartner, otherChildren }
  } = application

  if (!preferences.preferredStartDate) {
    errors['form.preferences.preferredStartDate'] =
      i18n.validationError.mandatoryField
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

  if (preferences.serviceNeed) {
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

  return errors
}

export default ApplicationPage
