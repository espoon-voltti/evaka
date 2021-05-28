{
  /*
SPDX-FileCopyrightText: 2017-2021 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
*/
}

import React from 'react'
import styled from 'styled-components'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'

import Title from 'lib-components/atoms/Title'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import CollapsibleSection from 'lib-components/molecules/CollapsibleSection'
import { ContentArea } from 'lib-components/layout/Container'
import { faPhone } from 'lib-icons'

import { Child } from '../../../api/attendances'
import { useTranslation } from '../../../state/i18n'
import { PlacementType } from 'lib-customizations/types'

interface Props {
  child: Child | null
}

const Key = styled.span`
  font-weight: 600;
  font-size: 16px;
  margin-bottom: 4px;
`

const KeyValue = styled.div`
  display: flex;
  flex-direction: column;
`

const Divider = styled.div`
  border-bottom: 1px solid #d8d8d8;
`

const Phone = styled.div`
  display: flex;
  justify-content: space-between;
`

const renderKeyValue = (
  label: string,
  value: string | null,
  dataQa: string,
  phone?: boolean
) => {
  return value ? (
    <KeyValue>
      <Key>{label}</Key>
      {phone ? (
        <Phone>
          <span data-qa={dataQa}>{value}</span>
          <a href={`tel:${value}`}>
            <FontAwesomeIcon icon={faPhone} />
          </a>
        </Phone>
      ) : (
        <span data-qa={dataQa}>{value}</span>
      )}
    </KeyValue>
  ) : null
}

export default React.memo(function ChildSensitiveInfo({ child }: Props) {
  const { i18n } = useTranslation()

  const placementTypesToText = (types: PlacementType[]): string => {
    return types.map((type) => i18n.common.placement[type]).join(',')
  }

  return (
    <>
      {child && (
        <FixedSpaceColumn spacing={'m'}>
          <ContentArea
            shadow
            opaque
            paddingHorizontal={'s'}
            paddingVertical={'m'}
          >
            <FixedSpaceColumn alignItems={'center'} spacing={'m'}>
              <Title>{i18n.attendances.childInfo.header}</Title>
            </FixedSpaceColumn>
          </ContentArea>

          <ContentArea shadow opaque>
            <CollapsibleSection
              title={i18n.attendances.childInfo.personalInfoHeader}
            >
              <FixedSpaceColumn>
                <KeyValue>
                  <Key>{i18n.attendances.childInfo.childName}</Key>
                  <span
                    data-qa={'child-info-name'}
                  >{`${child.firstName} ${child.lastName}`}</span>
                </KeyValue>

                {renderKeyValue(
                  i18n.attendances.childInfo.preferredName,
                  child.preferredName,
                  'child-info-preferred-name'
                )}

                {renderKeyValue(
                  i18n.attendances.childInfo.address,
                  child.childAddress,
                  'child-info-child-address'
                )}

                {child.placementTypes && child.placementTypes.length > 0 && (
                  <KeyValue>
                    <Key>{i18n.attendances.childInfo.type}</Key>
                    <span>{placementTypesToText(child.placementTypes)}</span>
                  </KeyValue>
                )}
              </FixedSpaceColumn>
            </CollapsibleSection>
          </ContentArea>

          <ContentArea shadow opaque>
            <CollapsibleSection
              title={i18n.attendances.childInfo.allergiesHeader}
            >
              <FixedSpaceColumn>
                {renderKeyValue(
                  i18n.attendances.childInfo.allergies,
                  child.allergies,
                  'child-info-allergies'
                )}

                {renderKeyValue(
                  i18n.attendances.childInfo.diet,
                  child.diet,
                  'child-info-diet'
                )}

                {renderKeyValue(
                  i18n.attendances.childInfo.medication,
                  child.medication,
                  'child-info-medication'
                )}
              </FixedSpaceColumn>
            </CollapsibleSection>
          </ContentArea>

          <ContentArea shadow opaque>
            <CollapsibleSection
              title={i18n.attendances.childInfo.contactInfoHeader}
            >
              {child.contacts?.map((contact, index) => (
                <div key={contact.id}>
                  <Title size={4}>{`${i18n.attendances.childInfo.contact} ${
                    index + 1
                  }`}</Title>
                  <FixedSpaceColumn>
                    <KeyValue>
                      <Key>{i18n.attendances.childInfo.name}</Key>
                      <span data-qa={`child-info-contact${index + 1}-name`}>{`${
                        contact.firstName || ''
                      } ${contact.lastName || ''}`}</span>
                    </KeyValue>

                    {renderKeyValue(
                      i18n.attendances.childInfo.phone,
                      contact.phone,
                      `child-info-contact${index + 1}-phone`,
                      true
                    )}

                    {renderKeyValue(
                      i18n.attendances.childInfo.backupPhone,
                      contact.backupPhone,
                      `child-info-contact${index + 1}-backup-phone`,
                      true
                    )}

                    {renderKeyValue(
                      i18n.attendances.childInfo.email,
                      contact.email,
                      `child-info-contact${index + 1}-email`
                    )}

                    <Divider />
                  </FixedSpaceColumn>
                </div>
              ))}

              {child.backupPickups?.map((backupPickup, index) => (
                <div key={backupPickup.id}>
                  <Title size={4}>
                    {`${i18n.attendances.childInfo.backupPickup} ${index + 1}`}
                  </Title>
                  <FixedSpaceColumn>
                    {renderKeyValue(
                      i18n.attendances.childInfo.backupPickupName,
                      backupPickup.firstName,
                      `child-info-backup-pickup${index + 1}-name`
                    )}

                    {renderKeyValue(
                      i18n.attendances.childInfo.phone,
                      backupPickup.phone,
                      `child-info-backup-pickup${index + 1}-phone`,
                      true
                    )}

                    <Divider />
                  </FixedSpaceColumn>
                </div>
              ))}
            </CollapsibleSection>
          </ContentArea>
        </FixedSpaceColumn>
      )}
    </>
  )
})
