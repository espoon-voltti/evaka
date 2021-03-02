// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useMemo, useState } from 'react'
import { Redirect, useHistory, useParams } from 'react-router-dom'
import styled from 'styled-components'
import {
  Container,
  ContentArea
} from '@evaka/lib-components/src/layout/Container'
import { H1, H2 } from '@evaka/lib-components/src/typography'
import { Gap, defaultMargins } from '@evaka/lib-components/src/white-space'
import ButtonContainer from '@evaka/lib-components/src/layout/ButtonContainer'
import AsyncButton from '@evaka/lib-components/src/atoms/buttons/AsyncButton'
import Button from '@evaka/lib-components/src/atoms/buttons/Button'
import ReturnButton from '@evaka/lib-components/src/atoms/buttons/ReturnButton'
import Radio from '@evaka/lib-components/src/atoms/form/Radio'
import {
  AlertBox,
  InfoBox
} from '@evaka/lib-components/src/molecules/MessageBoxes'
import { useUser } from '../auth'
import { useTranslation } from '../localization'
import {
  createApplication,
  getActivePlacementsByApplicationType,
  getDuplicateApplications
} from '../applications/api'
import { ApplicationType } from '@evaka/lib-common/src/api-types/application/enums'
import ExpandingInfo from '@evaka/lib-components/src/molecules/ExpandingInfo'
import Footer from '../Footer'
import useTitle from '../useTitle'

export default React.memo(function ApplicationCreation() {
  const history = useHistory()
  const { childId } = useParams<{ childId: string }>()
  const t = useTranslation()
  const user = useUser()
  useTitle(t, t.applications.creation.title)
  const child = useMemo(() => user?.children.find(({ id }) => childId === id), [
    childId,
    user
  ])
  const [selectedType, setSelectedType] = useState<ApplicationType>()

  const [duplicates, setDuplicates] = useState<
    Record<ApplicationType, boolean>
  >(duplicatesDefault)
  useEffect(() => {
    void getDuplicateApplications(childId).then(setDuplicates)
  }, [])

  const duplicateExists =
    selectedType !== undefined && duplicates[selectedType] === true

  const [transferApplicationTypes, setTransferApplicationTypes] = useState<
    Record<ApplicationType, boolean>
  >(duplicatesDefault)
  useEffect(() => {
    void getActivePlacementsByApplicationType(childId).then(
      setTransferApplicationTypes
    )
  }, [])

  const shouldUseTransferApplication =
    (selectedType === 'DAYCARE' || selectedType === 'PRESCHOOL') &&
    transferApplicationTypes[selectedType] === true

  if (child === undefined) {
    return <Redirect to="/applications" />
  }

  return (
    <>
      <Container>
        <ReturnButton label={t.common.return} />
        <ContentArea opaque paddingVertical="L">
          <H1 noMargin>{t.applications.creation.title}</H1>
          <Gap size="m" />
          <H2 noMargin>
            {child.firstName} {child.lastName}
          </H2>
          <Gap size="XL" />
          <ExpandingInfo
            info={t.applications.creation.daycareInfo}
            ariaLabel={t.common.openExpandingInfo}
          >
            <Radio
              checked={selectedType === 'DAYCARE'}
              onChange={() => setSelectedType('DAYCARE')}
              label={t.applications.creation.daycareLabel}
              dataQa="type-radio-DAYCARE"
            />
          </ExpandingInfo>
          <Gap size="s" />
          <ExpandingInfo
            info={t.applications.creation.preschoolInfo}
            ariaLabel={t.common.openExpandingInfo}
          >
            <Radio
              checked={selectedType === 'PRESCHOOL'}
              onChange={() => setSelectedType('PRESCHOOL')}
              label={t.applications.creation.preschoolLabel}
              dataQa="type-radio-PRESCHOOL"
            />
          </ExpandingInfo>
          <PreschoolDaycareInfo>
            {t.applications.creation.preschoolDaycareInfo}
          </PreschoolDaycareInfo>
          <Gap size="s" />
          <ExpandingInfo
            info={t.applications.creation.clubInfo}
            ariaLabel={t.common.openExpandingInfo}
          >
            <Radio
              checked={selectedType === 'CLUB'}
              onChange={() => setSelectedType('CLUB')}
              label={t.applications.creation.clubLabel}
              dataQa="type-radio-CLUB"
            />
          </ExpandingInfo>
          {duplicateExists ? (
            <>
              <Gap size="L" />
              <AlertBox
                thin
                data-qa={'duplicate-application-notification'}
                message={t.applications.creation.duplicateWarning}
              />
            </>
          ) : null}
          {!duplicateExists && shouldUseTransferApplication && (
            <>
              <Gap size="L" />
              <InfoBox
                thin
                data-qa={'transfer-application-notification'}
                message={
                  t.applications.creation.transferApplicationInfo[
                    selectedType === 'DAYCARE' ? 'DAYCARE' : 'PRESCHOOL'
                  ]
                }
              />
            </>
          )}
          <Gap size="s" />
          {t.applications.creation.applicationInfo()}
        </ContentArea>
        <ContentArea opaque={false} paddingVertical="L">
          <ButtonContainer justify="center">
            <AsyncButton
              primary
              text={t.applications.creation.create}
              disabled={selectedType === undefined || duplicateExists}
              onClick={() =>
                selectedType !== undefined
                  ? createApplication(
                      childId,
                      selectedType
                    ).then((applicationId) =>
                      history.push(`/applications/${applicationId}/edit`)
                    )
                  : Promise.resolve()
              }
              onSuccess={() => undefined}
              data-qa="submit"
            />
            <Button
              text={t.common.cancel}
              onClick={() => history.push('/applications')}
            />
          </ButtonContainer>
        </ContentArea>
      </Container>
      <Footer />
    </>
  )
})

const duplicatesDefault: Record<ApplicationType, boolean> = {
  CLUB: false,
  DAYCARE: false,
  PRESCHOOL: false
}

const PreschoolDaycareInfo = styled.p`
  margin: 0;
  margin-left: calc(
    36px + ${defaultMargins.s}
  ); // width of the radio input's icon + the margin on label
  font-weight: 600;
  font-size: 0.875em;
`
