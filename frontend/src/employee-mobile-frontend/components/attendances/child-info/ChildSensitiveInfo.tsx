import React from 'react'
import styled from 'styled-components'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'

import { P, Label } from 'lib-components/typography'
import Title from 'lib-components/atoms/Title'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import CollapsibleSection from 'lib-components/molecules/CollapsibleSection'
import { faPhone } from 'lib-icons'

import { Child } from '../../../api/attendances'
import { useTranslation } from '../../../state/i18n'
import { ContentAreaWithShadow } from '../../mobile/components'
import { PlacementType } from '../../../types'

interface Props {
  child: Child | null
}

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
      <Label>{label}</Label>
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
          <ContentAreaWithShadow
            opaque={true}
            paddingHorizontal={'s'}
            paddingVertical={'m'}
          >
            <FixedSpaceColumn alignItems={'center'} spacing={'m'}>
              <Title>{i18n.attendances.childInfo.header}</Title>
            </FixedSpaceColumn>
          </ContentAreaWithShadow>

          <ContentAreaWithShadow opaque={true}>
            <CollapsibleSection
              title={i18n.attendances.childInfo.personalInfoHeader}
            >
              <FixedSpaceColumn>
                <KeyValue>
                  <Label>{i18n.attendances.childInfo.childName}</Label>
                  <P
                    fitted
                    data-qa={'child-info-name'}
                  >{`${child.firstName} ${child.lastName}`}</P>
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

                {child.placementType && child.placementType.length > 0 && (
                  <KeyValue>
                    <Label>{i18n.attendances.childInfo.type}</Label>
                    <P fitted>{placementTypesToText(child.placementType)}</P>
                  </KeyValue>
                )}
              </FixedSpaceColumn>
            </CollapsibleSection>
          </ContentAreaWithShadow>

          <ContentAreaWithShadow opaque={true}>
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
          </ContentAreaWithShadow>

          <ContentAreaWithShadow opaque={true}>
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
                      <Label>{i18n.attendances.childInfo.name}</Label>
                      <P
                        fitted
                        data-qa={`child-info-contact${index + 1}-name`}
                      >{`${contact.firstName || ''} ${
                        contact.lastName || ''
                      }`}</P>
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
                      `child-info-backup-pickup${index + 1}-phone`
                    )}

                    <Divider />
                  </FixedSpaceColumn>
                </div>
              ))}
            </CollapsibleSection>
          </ContentAreaWithShadow>
        </FixedSpaceColumn>
      )}
    </>
  )
})
