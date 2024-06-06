// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { useNavigate } from 'react-router-dom'
import styled from 'styled-components'

import { Success, wrapResult } from 'lib-common/api'
import {
  ApplicationDetails,
  ApplicationStatus
} from 'lib-common/generated/api-types/application'
import AsyncButton from 'lib-components/atoms/buttons/AsyncButton'
import { Button } from 'lib-components/atoms/buttons/Button'
import StickyFooter from 'lib-components/layout/StickyFooter'
import { Gap } from 'lib-components/white-space'

import {
  sendApplication,
  updateApplication
} from '../../generated/api-clients/application'
import { useTranslation } from '../../state/i18n'

const updateApplicationResult = wrapResult(updateApplication)
const sendApplicationResult = wrapResult(sendApplication)

type Props = {
  applicationStatus: ApplicationStatus
  editing: boolean
  setEditing: (v: boolean) => void
  editedApplication: ApplicationDetails
  reloadApplication: () => void
  errors: boolean
}

export default React.memo(function ApplicationActionsBar({
  applicationStatus,
  editing,
  setEditing,
  editedApplication,
  reloadApplication,
  errors
}: Props) {
  const navigate = useNavigate()
  const { i18n } = useTranslation()

  const actions = [
    {
      id: 'start-editing',
      enabled:
        !editing &&
        (applicationStatus === 'CREATED' || applicationStatus === 'SENT'),
      component: (
        <Button
          onClick={() => setEditing(true)}
          text={i18n.common.edit}
          primary
          data-qa="edit-application"
        />
      )
    },
    {
      id: 'cancel-editing',
      enabled: editing && applicationStatus !== 'CREATED',
      component: (
        <Button onClick={() => setEditing(false)} text={i18n.common.cancel} />
      )
    },
    {
      id: 'cancel-new-application',
      enabled: editing && applicationStatus === 'CREATED',
      component: (
        <Button onClick={() => navigate(-1)} text={i18n.common.cancel} />
      )
    },
    {
      id: 'save-application',
      enabled: editing,
      component: (
        <AsyncButton
          text={i18n.common.save}
          textInProgress={i18n.common.saving}
          textDone={i18n.common.saved}
          disabled={!editedApplication || errors}
          onClick={() =>
            updateApplicationResult({
              applicationId: editedApplication.id,
              body: editedApplication
            }).then((result) => {
              if (result.isSuccess) {
                return editedApplication.status === 'CREATED'
                  ? sendApplicationResult({
                      applicationId: editedApplication.id
                    })
                  : Success.of()
              } else {
                return result
              }
            })
          }
          onSuccess={() => {
            setEditing(false)
            reloadApplication()
          }}
          primary
          data-qa="save-application"
        />
      )
    }
  ].filter(({ enabled }) => enabled)

  return actions.length > 0 ? (
    <StickyFooter>
      <ContentContainer>
        {actions.map(({ id, component }, index) => (
          <React.Fragment key={id}>
            {index === 0 ? null : <Gap size="s" horizontal />}
            {component}
          </React.Fragment>
        ))}
      </ContentContainer>
    </StickyFooter>
  ) : null
})

const ContentContainer = styled.div`
  display: flex;
  flex-direction: row;
  justify-content: flex-end;
`
