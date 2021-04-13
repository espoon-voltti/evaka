// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'
import { Gap } from 'lib-components/white-space'
import StickyFooter from 'lib-components/layout/StickyFooter'
import AsyncButton from 'lib-components/atoms/buttons/AsyncButton'
import Button from 'lib-components/atoms/buttons/Button'
import { useTranslation } from '../../state/i18n'
import { sendApplication, updateApplication } from '../../api/applications'
import { ApplicationStatus } from 'lib-common/api-types/application/enums'
import { ApplicationDetails } from 'lib-common/api-types/application/ApplicationDetails'

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
          dataQa="edit-application"
        />
      )
    },
    {
      id: 'cancel-editing',
      enabled: editing,
      component: (
        <Button onClick={() => setEditing(false)} text={i18n.common.cancel} />
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
            updateApplication(editedApplication).then((res) =>
              editedApplication.status === 'CREATED'
                ? sendApplication(editedApplication.id)
                : res
            )
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
