// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useEffect, useState } from 'react'
import { RouteComponentProps } from 'react-router-dom'
import { UUID } from 'types'
import { isSuccess, Loading, Result } from 'api'
import { ApplicationDetails, ApplicationResponse } from 'types/application'
import ApplicationEditView from 'components/application-page/ApplicationEditView'
import ApplicationReadView from 'components/application-page/ApplicationReadView'
import ApplicationActionsBar from 'components/application-page/ApplicationActionsBar'
import { getApplication } from 'api/applications'
import { renderResult } from 'components/shared/atoms/state/async-rendering'
import { TitleContext, TitleState } from 'state/title'
import { useTranslation, Translations } from 'state/i18n'
import { Container, ContentArea } from 'components/shared/alpha/layout'
import styled from 'styled-components'
import { FixedSpaceRow } from 'components/shared/layout/flex-helpers'
import ReturnButton from 'components/shared/atoms/buttons/ReturnButton'
import ApplicationNotes from 'components/application-page/ApplicationNotes'
import { useDebounce } from 'utils/useDebounce'
import { isSsnValid, isTimeValid } from 'utils/validation/validations'

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
    Loading()
  )

  const creatingNew = window.location.href.includes('create=true')
  const [editing, setEditing] = useState(creatingNew)
  const [editedApplication, setEditedApplication] = useState<
    ApplicationDetails
  >()
  const [validationErrors, setValidationErrors] = useState<
    Record<string, string>
  >({})

  // this is used because text inputs become too sluggish without it
  const debouncedEditedApplication = useDebounce(editedApplication, 50)

  const reloadApplication = () => {
    setApplication(Loading())
    void getApplication(applicationId).then((result) => {
      setApplication(result)
      if (isSuccess(result)) {
        const {
          firstName,
          lastName
        } = result.data.application.form.child.person
        setTitle(
          `${i18n.application.tabTitle} - ${formatTitleName(
            firstName,
            lastName
          )}`
        )
        setEditedApplication(result.data.application)
      }
    })
  }

  useEffect(reloadApplication, [applicationId])

  useEffect(() => {
    if (debouncedEditedApplication) {
      setValidationErrors(validateApplication(debouncedEditedApplication, i18n))
    }
  }, [debouncedEditedApplication])

  const renderApplication = (applicationData: ApplicationResponse) =>
    editing ? (
      editedApplication ? (
        <ApplicationEditView
          application={editedApplication}
          setApplication={setEditedApplication}
          errors={validationErrors}
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
      <Container>
        <ReturnButton dataQa="close-application" />
        <FixedSpaceRow>
          <ApplicationArea opaque>
            {renderResult(application, renderApplication)}
          </ApplicationArea>
          <NotesArea>
            <ApplicationNotes applicationId={applicationId} />
          </NotesArea>
        </FixedSpaceRow>
      </Container>

      {isSuccess(application) ? (
        <ApplicationActionsBar
          applicationStatus={application.data.application.status}
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
