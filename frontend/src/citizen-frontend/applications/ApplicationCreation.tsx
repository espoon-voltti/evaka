// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useMemo, useState } from 'react'
import { Redirect, useHistory, useParams } from 'react-router-dom'
import styled from 'styled-components'
import { Loading, Result } from 'lib-common/api'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { H1, H2 } from 'lib-components/typography'
import { Gap, defaultMargins } from 'lib-components/white-space'
import ButtonContainer from 'lib-components/layout/ButtonContainer'
import AsyncButton from 'lib-components/atoms/buttons/AsyncButton'
import Button from 'lib-components/atoms/buttons/Button'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import Radio from 'lib-components/atoms/form/Radio'
import { AlertBox, InfoBox } from 'lib-components/molecules/MessageBoxes'
import { useUser } from '../auth'
import { useTranslation } from '../localization'
import {
  createApplication,
  getActivePlacementsByApplicationType,
  getDuplicateApplications
} from '../applications/api'
import { ApplicationType } from 'lib-common/api-types/application/enums'
import ExpandingInfo from 'lib-components/molecules/ExpandingInfo'
import Footer from '../Footer'
import useTitle from '../useTitle'
import { featureFlags } from 'lib-customizations/citizen'

export default React.memo(function ApplicationCreation() {
  const history = useHistory()
  const { childId } = useParams<{ childId: string }>()
  const t = useTranslation()
  const user = useUser()
  useTitle(t, t.applications.creation.title)
  const child = useMemo(
    () => user?.children.find(({ id }) => childId === id),
    [childId, user]
  )
  const [selectedType, setSelectedType] = useState<ApplicationType>()

  const [duplicates, setDuplicates] = useState<
    Result<Record<ApplicationType, boolean>>
  >(Loading.of())
  useEffect(() => {
    void getDuplicateApplications(childId).then(setDuplicates)
  }, [childId])

  const duplicateExists =
    selectedType !== undefined &&
    duplicates.map((ds) => ds[selectedType] === true).getOrElse(false)

  const [transferApplicationTypes, setTransferApplicationTypes] = useState<
    Result<Record<ApplicationType, boolean>>
  >(Loading.of())
  useEffect(() => {
    void getActivePlacementsByApplicationType(childId).then(
      setTransferApplicationTypes
    )
  }, [childId])

  const shouldUseTransferApplication =
    (selectedType === 'DAYCARE' || selectedType === 'PRESCHOOL') &&
    transferApplicationTypes
      .map((ts) => ts[selectedType] === true)
      .getOrElse(false)

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
            margin="xs"
          >
            <Radio
              checked={selectedType === 'DAYCARE'}
              onChange={() => setSelectedType('DAYCARE')}
              label={t.applications.creation.daycareLabel}
              data-qa="type-radio-DAYCARE"
            />
          </ExpandingInfo>
          <Gap size="s" />
          {featureFlags.preschoolEnabled && (
            <>
              <ExpandingInfo
                info={t.applications.creation.preschoolInfo}
                ariaLabel={t.common.openExpandingInfo}
                margin="xs"
              >
                <Radio
                  checked={selectedType === 'PRESCHOOL'}
                  onChange={() => setSelectedType('PRESCHOOL')}
                  label={t.applications.creation.preschoolLabel}
                  data-qa="type-radio-PRESCHOOL"
                />
              </ExpandingInfo>
              <PreschoolDaycareInfo>
                {t.applications.creation.preschoolDaycareInfo}
              </PreschoolDaycareInfo>
              <Gap size="s" />
            </>
          )}
          <ExpandingInfo
            info={t.applications.creation.clubInfo}
            ariaLabel={t.common.openExpandingInfo}
            margin="xs"
          >
            <Radio
              checked={selectedType === 'CLUB'}
              onChange={() => setSelectedType('CLUB')}
              label={t.applications.creation.clubLabel}
              data-qa="type-radio-CLUB"
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
                  ? createApplication(childId, selectedType).then((result) => {
                      if (result.isSuccess) {
                        history.push(`/applications/${result.value}/edit`)
                      }
                    })
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

const PreschoolDaycareInfo = styled.p`
  margin: 0;
  margin-left: calc(
    36px + ${defaultMargins.s}
  ); // width of the radio input's icon + the margin on label
  font-weight: 600;
  font-size: 0.875em;
`
