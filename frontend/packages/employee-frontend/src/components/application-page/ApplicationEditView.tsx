// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import styled from 'styled-components'
import { set } from 'lodash/fp'
import ReactSelect from 'react-select'
import {
  faChild,
  faFileAlt,
  faInfo,
  faMapMarkerAlt,
  faTimes,
  faUserFriends,
  faUsers
} from '@evaka/icons'
import LocalDate from '@evaka/lib-common/src/local-date'
import { Loading, Result, isSuccess, isLoading } from 'api'
import { getApplicationUnits } from 'api/daycare'
import { H4, Label } from 'components/shared/Typography'
import CollapsibleSection from 'components/shared/molecules/CollapsibleSection'
import { Gap } from 'components/shared/layout/white-space'
import { FixedSpaceColumn } from 'components/shared/layout/flex-helpers'
import ListGrid from 'components/shared/layout/ListGrid'
import InlineButton from 'components/shared/atoms/buttons/InlineButton'
import AddButton from 'components/shared/atoms/buttons/AddButton'
import InputField from 'components/shared/atoms/form/InputField'
import Radio from 'components/shared/atoms/form/Radio'
import Checkbox from 'components/shared/atoms/form/Checkbox'
import { TextArea } from 'components/shared/alpha/elements/form/textarea'
import { DatePicker } from 'components/common/DatePicker'
import ApplicationTitle from 'components/application-page/ApplicationTitle'
import VTJGuardian from 'components/application-page/VTJGuardian'
import ApplicationStatusSection from 'components/application-page/ApplicationStatusSection'
import { useTranslation, Translations } from 'state/i18n'
import {
  Address,
  ApplicationDetails,
  FutureAddress,
  PersonBasics,
  PreferredUnit
} from 'types/application'
import { formatName } from 'utils'
import { InputWarning } from '~components/common/InputWarning'

interface PreschoolApplicationProps {
  application: ApplicationDetails
  setApplication: React.Dispatch<
    React.SetStateAction<ApplicationDetails | undefined>
  >
  errors: Record<string, string>
}

export default React.memo(function ApplicationEditView({
  application,
  setApplication,
  errors
}: PreschoolApplicationProps) {
  const { i18n } = useTranslation()
  const [units, setUnits] = useState<Result<PreferredUnit[]>>(Loading())

  const applicationType =
    application.type === 'PRESCHOOL' && application.form.preferences.preparatory
      ? 'PREPARATORY'
      : application.type

  useEffect(() => {
    void getApplicationUnits(
      applicationType,
      application.form.preferences.preferredStartDate ?? LocalDate.today()
    ).then(setUnits)
  }, [applicationType])

  const {
    type,
    form: {
      child,
      guardian,
      secondGuardian,
      otherPartner,
      otherChildren,
      preferences: {
        preferredUnits,
        preferredStartDate,
        urgent,
        serviceNeed,
        siblingBasis,
        preparatory
      },
      otherInfo,
      maxFeeAccepted,
      clubDetails
    },
    childId,
    guardianId,
    otherGuardianId,
    childRestricted,
    guardianRestricted,
    otherGuardianLivesInSameAddress
  } = application

  const connectedDaycare = type === 'PRESCHOOL' && serviceNeed !== null
  const paid = type === 'DAYCARE' || connectedDaycare

  const formatPersonName = (person: PersonBasics) =>
    formatName(person.firstName, person.lastName, i18n, true)
  const formatAddress = (a: Address) =>
    `${a.street}, ${a.postalCode} ${a.postOffice}`

  return (
    <div data-qa="application-edit-view">
      <ApplicationTitle application={application} />
      <Gap />

      <CollapsibleSection
        title={i18n.application.serviceNeed.title}
        icon={faFileAlt}
      >
        <ListGrid>
          <Label>{i18n.application.serviceNeed.startDate}</Label>
          <div data-qa="datepicker-start-date">
            <HorizontalContainer>
              <DatePicker
                type="short"
                date={preferredStartDate ?? undefined}
                onChange={(value) => {
                  setApplication(
                    set('form.preferences.preferredStartDate', value)
                  )
                }}
                dataQa="datepicker-start-date"
              />
              {errors['form.preferences.preferredStartDate'] ? (
                <>
                  <Gap size="s" horizontal />
                  <InputWarning
                    text={errors['form.preferences.preferredStartDate']}
                  />
                </>
              ) : null}
            </HorizontalContainer>
          </div>

          {type === 'DAYCARE' && (
            <>
              <Label>{i18n.application.serviceNeed.urgentLabel}</Label>
              <Checkbox
                label={i18n.application.serviceNeed.urgentValue}
                checked={urgent}
                onChange={(value) =>
                  setApplication(set('form.preferences.urgent', value))
                }
                dataQa="checkbox-urgent"
              />
            </>
          )}

          {type === 'PRESCHOOL' && (
            <>
              <Label>{i18n.application.serviceNeed.connectedLabel}</Label>
              <Checkbox
                label={i18n.application.serviceNeed.connectedValue}
                checked={serviceNeed !== null}
                onChange={(value) =>
                  value
                    ? setApplication(
                        set('form.preferences.serviceNeed', {
                          startTime: '',
                          endTime: '',
                          shiftCare: false,
                          partTime: false
                        })
                      )
                    : setApplication(set('form.preferences.serviceNeed', null))
                }
                dataQa="checkbox-service-need-connected"
              />
            </>
          )}

          {serviceNeed !== null && (
            <>
              <Label>{i18n.application.serviceNeed.dailyTime}</Label>
              <div>
                <HorizontalContainer>
                  <InputField
                    width="m"
                    placeholder={
                      i18n.application.serviceNeed.startTimePlaceholder
                    }
                    value={serviceNeed.startTime}
                    onChange={(value) =>
                      setApplication(
                        set('form.preferences.serviceNeed.startTime', value)
                      )
                    }
                    info={
                      errors['form.preferences.serviceNeed.startTime']
                        ? {
                            text:
                              errors['form.preferences.serviceNeed.startTime'],
                            status: 'warning'
                          }
                        : undefined
                    }
                    dataQa="start-time"
                  />
                  <Gap size="s" horizontal />
                  <InputField
                    width="m"
                    placeholder={
                      i18n.application.serviceNeed.endTimePlaceholder
                    }
                    value={serviceNeed.endTime}
                    onChange={(value) =>
                      setApplication(
                        set('form.preferences.serviceNeed.endTime', value)
                      )
                    }
                    info={
                      errors['form.preferences.serviceNeed.endTime']
                        ? {
                            text:
                              errors['form.preferences.serviceNeed.endTime'],
                            status: 'warning'
                          }
                        : undefined
                    }
                    dataQa="end-time"
                  />
                </HorizontalContainer>
                <Gap size="m" />
              </div>

              <Label>{i18n.application.serviceNeed.shiftCareLabel}</Label>
              <Checkbox
                label={i18n.application.serviceNeed.shiftCareValue}
                checked={serviceNeed.shiftCare}
                onChange={(value) =>
                  setApplication(
                    set('form.preferences.serviceNeed.shiftCare', value)
                  )
                }
                dataQa="checkbox-service-need-shift-care"
              />
            </>
          )}

          {type === 'CLUB' && (
            <>
              <Label>{i18n.application.clubDetails.wasOnClubCareLabel}</Label>
              <Checkbox
                label={i18n.application.clubDetails.wasOnClubCareValue}
                checked={!!clubDetails?.wasOnClubCare}
                onChange={(value) =>
                  setApplication(set('form.clubDetails.wasOnClubCare', value))
                }
                dataQa="checkbox-club-details-club-care"
              />
              <Label>{i18n.application.clubDetails.wasOnDaycareLabel}</Label>
              <Checkbox
                label={i18n.application.clubDetails.wasOnDaycareValue}
                checked={!!clubDetails?.wasOnDaycare}
                onChange={(value) =>
                  setApplication(set('form.clubDetails.wasOnDaycare', value))
                }
                dataQa="checkbox-club-details-day-care"
              />
            </>
          )}

          {type === 'PRESCHOOL' && (
            <>
              <Label>{i18n.application.serviceNeed.preparatoryLabel}</Label>
              <Checkbox
                label={i18n.application.serviceNeed.preparatoryValue}
                checked={preparatory}
                onChange={(value) =>
                  setApplication(set('form.preferences.preparatory', value))
                }
                dataQa="checkbox-service-need-preparatory"
              />
            </>
          )}

          <Label>{i18n.application.serviceNeed.assistanceLabel}</Label>
          <Checkbox
            label={i18n.application.serviceNeed.assistanceValue}
            checked={child.assistanceNeeded}
            onChange={(value) =>
              setApplication(set('form.child.assistanceNeeded', value))
            }
            dataQa="checkbox-service-need-assistance-needed"
          />

          {child.assistanceNeeded && (
            <>
              <Label>{i18n.application.serviceNeed.assistanceDesc}</Label>
              <TextArea
                value={child.assistanceDescription}
                onChange={(e) => {
                  const value = e.target.value
                  setApplication(set('form.child.assistanceDescription', value))
                }}
              />
            </>
          )}
        </ListGrid>
      </CollapsibleSection>

      <CollapsibleSection
        title={i18n.application.preferences.title}
        icon={faMapMarkerAlt}
      >
        <ListGrid>
          <Label>{i18n.application.preferences.preferredUnits}</Label>
          <VerticalContainer data-qa="preferred-unit">
            <ReactSelect
              placeholder={i18n.common.search}
              isLoading={isLoading(units)}
              value={null}
              options={
                isSuccess(units)
                  ? units.data
                      .filter(
                        ({ id }) =>
                          !preferredUnits.some((unit) => unit.id === id)
                      )
                      .map(({ id, name }) => ({
                        value: id,
                        label: name
                      }))
                  : []
              }
              isDisabled={preferredUnits.length >= 3}
              onChange={(option) => {
                if (option && 'value' in option) {
                  setApplication(
                    set('form.preferences.preferredUnits', [
                      ...preferredUnits,
                      { id: option.value, name: option.label }
                    ])
                  )
                }
              }}
              loadingMessage={() => i18n.common.loading}
              noOptionsMessage={() => i18n.common.noResults}
              dataQa="select-preferred-unit"
            />
            <Gap size="s" />
            {preferredUnits.length === 0 ? (
              <InputWarning
                text={i18n.application.preferences.missingPreferredUnits}
              />
            ) : null}
            {preferredUnits.map((unit, i) => (
              <HorizontalContainer key={unit.id}>
                <Link to={`/units/${unit.id}`}>{`${i + 1}. ${unit.name}`}</Link>
                <Gap size="s" horizontal />
                <InlineButton
                  icon={faTimes}
                  text={i18n.common.remove}
                  onClick={() =>
                    setApplication(
                      set(
                        'form.preferences.preferredUnits',
                        preferredUnits.filter((_, index) => i !== index)
                      )
                    )
                  }
                  dataQa="button-select-preferred-unit"
                />
              </HorizontalContainer>
            ))}
          </VerticalContainer>
        </ListGrid>
        <Gap size="s" />
        <ListGrid>
          <Label>{i18n.application.preferences.siblingBasisLabel}</Label>
          <Checkbox
            label={i18n.application.preferences.siblingBasisValue}
            checked={siblingBasis !== null}
            onChange={(value) =>
              value
                ? setApplication(
                    set('form.preferences.siblingBasis', {
                      siblingName: '',
                      siblingSsn: ''
                    })
                  )
                : setApplication(set('form.preferences.siblingBasis', null))
            }
            dataQa="checkbox-sibling-basis"
          />
          {siblingBasis !== null && (
            <>
              <Label>{i18n.application.preferences.siblingName}</Label>
              <InputField
                width="L"
                value={siblingBasis.siblingName}
                onChange={(value) =>
                  setApplication(
                    set('form.preferences.siblingBasis.siblingName', value)
                  )
                }
                dataQa="input-sibling-name"
              />
              <Label>{i18n.application.preferences.siblingSsn}</Label>
              <InputField
                width="L"
                value={siblingBasis.siblingSsn}
                onChange={(value) =>
                  setApplication(
                    set('form.preferences.siblingBasis.siblingSsn', value)
                  )
                }
                dataQa="input-sibling-ssn"
              />
            </>
          )}
        </ListGrid>
      </CollapsibleSection>

      <CollapsibleSection title={i18n.application.child.title} icon={faChild}>
        <ListGrid>
          <Label>{i18n.application.person.name}</Label>
          <Link to={`/child-information/${childId}`}>
            <span data-qa="link-child-name">
              {formatPersonName(child.person)}
            </span>
          </Link>

          <Label>{i18n.application.person.ssn}</Label>
          <span data-qa="child-ssn">{child.person.socialSecurityNumber}</span>

          {!child.person.socialSecurityNumber && (
            <>
              <Label>{i18n.application.person.dob}</Label>
              <span data-qa="child-dob">{child.dateOfBirth?.format()}</span>
            </>
          )}

          {childRestricted ? (
            <>
              <Label>{i18n.application.person.address}</Label>
              <span data-qa="child-restricted">
                {i18n.application.person.restricted}
              </span>
            </>
          ) : (
            <>
              <Label>{i18n.application.person.address}</Label>
              <div>
                <span data-qa="child-address">
                  {child.address && formatAddress(child.address)}
                </span>
                <Gap size="xs" />
                <Checkbox
                  label={i18n.application.person.hasFutureAddress}
                  checked={child.futureAddress !== null}
                  onChange={(value) => {
                    if (value) {
                      setApplication(
                        set('form.child.futureAddress', {
                          street: '',
                          postalCode: '',
                          postOffice: '',
                          movingDate: LocalDate.today()
                        })
                      )
                    } else {
                      setApplication(set('form.child.futureAddress', null))
                    }
                  }}
                />
              </div>
              {child.futureAddress && (
                <>
                  <Label>{i18n.application.person.futureAddress}</Label>
                  <FutureAddressInputs
                    futureAddress={child.futureAddress}
                    setApplication={setApplication}
                    i18n={i18n}
                    path={'child'}
                  />
                  <Label>{i18n.application.person.movingDate}</Label>
                  <DatePicker
                    type="short"
                    date={child.futureAddress.movingDate ?? undefined}
                    onChange={(value) => {
                      setApplication(
                        set('form.child.futureAddress.movingDate', value)
                      )
                    }}
                  />
                </>
              )}
            </>
          )}

          <Label>{i18n.application.person.nationality}</Label>
          <span data-qa="child-nationality">{child.nationality}</span>
          <Label>{i18n.application.person.language}</Label>
          <span data-qa="child-language">{child.language}</span>
        </ListGrid>
      </CollapsibleSection>

      <CollapsibleSection
        title={i18n.application.guardians.title}
        icon={faUserFriends}
      >
        <FixedSpaceColumn spacing="L">
          <div>
            <H4>{i18n.application.guardians.appliedGuardian}</H4>
            <ListGrid>
              <Label>{i18n.application.person.name}</Label>
              <Link to={`/profile/${guardianId}`}>
                <span data-qa="guardian-name">
                  {formatPersonName(guardian.person)}
                </span>
              </Link>
              <Label>{i18n.application.person.ssn}</Label>
              <span data-qa="guardian-ssn">
                {guardian.person.socialSecurityNumber}
              </span>

              {guardianRestricted ? (
                <>
                  <Label>{i18n.application.person.address}</Label>
                  <span data-qa="guardian-restricted">
                    {i18n.application.person.restricted}
                  </span>
                </>
              ) : (
                <>
                  <Label>{i18n.application.person.address}</Label>
                  <div>
                    <span data-qa="guardian-address">
                      {guardian.address && formatAddress(guardian.address)}
                    </span>
                    <Gap size="xs" />
                    <Checkbox
                      label="Väestorekisterissä oleva osoite on muuttunut / muuttumassa"
                      checked={guardian.futureAddress !== null}
                      onChange={(value) => {
                        if (value) {
                          setApplication(
                            set('form.guardian.futureAddress', {
                              street: '',
                              postalCode: '',
                              postOffice: '',
                              movingDate: LocalDate.today()
                            })
                          )
                        } else {
                          setApplication(
                            set('form.guardian.futureAddress', null)
                          )
                        }
                      }}
                    />
                  </div>
                  {guardian.futureAddress && (
                    <>
                      <Label>{i18n.application.person.futureAddress}</Label>
                      <FutureAddressInputs
                        futureAddress={guardian.futureAddress}
                        setApplication={setApplication}
                        i18n={i18n}
                        path={'guardian'}
                      />
                      <Label>{i18n.application.person.movingDate}</Label>
                      <DatePicker
                        type="short"
                        date={guardian.futureAddress.movingDate ?? undefined}
                        onChange={(value) => {
                          setApplication(
                            set('form.guardian.futureAddress.movingDate', value)
                          )
                        }}
                      />
                    </>
                  )}
                </>
              )}

              <Label>{i18n.application.person.phone}</Label>
              <InputField
                width="m"
                value={guardian.phoneNumber}
                onChange={(value) => {
                  setApplication(set('form.guardian.phoneNumber', value))
                }}
                dataQa="application-person-phone"
              />
              <Label>{i18n.application.person.email}</Label>
              <InputField
                width="m"
                value={guardian.email}
                onChange={(value) => {
                  setApplication(set('form.guardian.email', value))
                }}
                dataQa="application-person-email"
              />
            </ListGrid>
          </div>

          {type !== 'CLUB' && (
            <div>
              <H4>{i18n.application.guardians.secondGuardian.title}</H4>
              <Checkbox
                checked={!!secondGuardian}
                label={i18n.application.guardians.secondGuardian.checkboxLabel}
                onChange={(value) =>
                  setApplication(
                    set(
                      'form.secondGuardian',
                      value
                        ? {
                            phoneNumber: '',
                            email: '',
                            agreementStatus: null
                          }
                        : undefined
                    )
                  )
                }
                dataQa="application-second-guardian-toggle"
              />
              {secondGuardian && (
                <>
                  <Gap size="s" />
                  <ListGrid>
                    <Label>{i18n.application.person.phone}</Label>
                    <InputField
                      width="m"
                      value={secondGuardian?.phoneNumber ?? ''}
                      onChange={(value) => {
                        setApplication(
                          set('form.secondGuardian.phoneNumber', value)
                        )
                      }}
                      dataQa="application-second-guardian-phone"
                    />
                    <Label>{i18n.application.person.email}</Label>
                    <InputField
                      width="m"
                      value={secondGuardian?.email ?? ''}
                      onChange={(value) => {
                        setApplication(set('form.secondGuardian.email', value))
                      }}
                      dataQa="application-second-guardian-email"
                    />
                    <Label data-qa="agreement-status-label">
                      {i18n.application.person.agreementStatus}
                    </Label>
                    <div>
                      {([
                        'AGREED',
                        'NOT_AGREED',
                        'RIGHT_TO_GET_NOTIFIED',
                        null
                      ] as const).map((id, index) => (
                        <React.Fragment key={id ?? 'NOT_SET'}>
                          {index !== 0 ? <Gap size="xxs" /> : null}
                          <Radio
                            label={
                              i18n.application.person
                                .otherGuardianAgreementStatuses[id ?? 'NOT_SET']
                            }
                            checked={
                              id === (secondGuardian?.agreementStatus ?? null)
                            }
                            onChange={() => {
                              setApplication(
                                set('form.secondGuardian.agreementStatus', id)
                              )
                            }}
                            dataQa={`radio-other-guardian-agreement-status-${
                              id ?? 'null'
                            }`}
                          />
                        </React.Fragment>
                      ))}
                    </div>
                  </ListGrid>
                </>
              )}
            </div>
          )}

          <VTJGuardian
            guardianId={otherGuardianId}
            otherGuardianLivesInSameAddress={otherGuardianLivesInSameAddress}
          />
        </FixedSpaceColumn>
      </CollapsibleSection>

      {paid && (
        <>
          <CollapsibleSection
            title={i18n.application.otherPeople.title}
            icon={faUsers}
          >
            <FixedSpaceColumn spacing="L">
              {(!otherGuardianId || !otherGuardianLivesInSameAddress) && (
                <div>
                  <H4>{i18n.application.otherPeople.adult}</H4>
                  <Checkbox
                    label={i18n.application.otherPeople.spouse}
                    checked={otherPartner !== null}
                    onChange={(value) => {
                      if (value) {
                        setApplication(
                          set('form.otherPartner', {
                            firstName: '',
                            lastName: '',
                            socialSecurityNumber: ''
                          })
                        )
                      } else {
                        setApplication(set('form.otherPartner', null))
                      }
                    }}
                  />
                  {otherPartner && (
                    <>
                      <Gap size="s" />
                      <HorizontalContainer>
                        <WithLabel label={i18n.common.form.firstName}>
                          <InputField
                            width="m"
                            value={otherPartner.firstName}
                            onChange={(value) => {
                              setApplication(
                                set('form.otherPartner.firstName', value)
                              )
                            }}
                          />
                        </WithLabel>
                        <Gap size="s" horizontal />
                        <WithLabel label={i18n.common.form.lastName}>
                          <InputField
                            width="m"
                            value={otherPartner.lastName}
                            onChange={(value) => {
                              setApplication(
                                set('form.otherPartner.lastName', value)
                              )
                            }}
                          />
                        </WithLabel>
                        <Gap size="s" horizontal />
                        <WithLabel label={i18n.application.person.ssn}>
                          <InputField
                            width="m"
                            value={otherPartner.socialSecurityNumber}
                            onChange={(value) => {
                              setApplication(
                                set(
                                  'form.otherPartner.socialSecurityNumber',
                                  value
                                )
                              )
                            }}
                            info={
                              errors['form.otherPartner.socialSecurityNumber']
                                ? {
                                    text:
                                      errors[
                                        'form.otherPartner.socialSecurityNumber'
                                      ],
                                    status: 'warning'
                                  }
                                : undefined
                            }
                          />
                        </WithLabel>
                      </HorizontalContainer>
                    </>
                  )}
                </div>
              )}

              <div>
                <H4>{i18n.application.otherPeople.children}</H4>
                <AddButton
                  text={i18n.application.otherPeople.addChild}
                  onClick={() => {
                    setApplication(
                      set('form.otherChildren', [
                        ...otherChildren,
                        {
                          firstName: '',
                          lastName: '',
                          socialSecurityNumber: ''
                        }
                      ])
                    )
                  }}
                />
                {otherChildren.map(
                  ({ firstName, lastName, socialSecurityNumber }, index) => (
                    <React.Fragment key={index}>
                      <Gap size="s" />
                      <HorizontalContainer>
                        <WithLabel label={i18n.common.form.firstName}>
                          <InputField
                            width="m"
                            value={firstName}
                            onChange={(value) => {
                              setApplication(
                                set(
                                  ['form', 'otherChildren', index, 'firstName'],
                                  value
                                )
                              )
                            }}
                          />
                        </WithLabel>
                        <Gap size="s" horizontal />
                        <WithLabel label={i18n.common.form.lastName}>
                          <InputField
                            width="m"
                            value={lastName}
                            onChange={(value) => {
                              setApplication(
                                set(
                                  ['form', 'otherChildren', index, 'lastName'],
                                  value
                                )
                              )
                            }}
                          />
                        </WithLabel>
                        <Gap size="s" horizontal />
                        <WithLabel label={i18n.application.person.ssn}>
                          <InputField
                            width="m"
                            value={socialSecurityNumber}
                            onChange={(value) => {
                              setApplication(
                                set(
                                  [
                                    'form',
                                    'otherChildren',
                                    index,
                                    'socialSecurityNumber'
                                  ],
                                  value
                                )
                              )
                            }}
                            info={
                              errors[
                                `form.otherChildren.${index}.socialSecurityNumber`
                              ]
                                ? {
                                    text:
                                      errors[
                                        `form.otherChildren.${index}.socialSecurityNumber`
                                      ],
                                    status: 'warning'
                                  }
                                : undefined
                            }
                          />
                        </WithLabel>
                        <Gap size="s" horizontal />
                        <InlineButton
                          icon={faTimes}
                          text={i18n.common.remove}
                          onClick={() =>
                            setApplication(
                              set(
                                'form.otherChildren',
                                otherChildren.filter((c, i) => i !== index)
                              )
                            )
                          }
                        />
                      </HorizontalContainer>
                    </React.Fragment>
                  )
                )}
              </div>
            </FixedSpaceColumn>
          </CollapsibleSection>
        </>
      )}

      <CollapsibleSection
        title={i18n.application.additionalInfo.title}
        icon={faInfo}
      >
        <ListGrid>
          <Label>{i18n.application.additionalInfo.applicationInfo}</Label>
          <TextArea
            value={otherInfo}
            onChange={(e) => {
              const value = e.target.value
              setApplication(set('form.otherInfo', value))
            }}
          />

          {type !== 'CLUB' && (
            <>
              <Label>{i18n.application.additionalInfo.allergies}</Label>
              <TextArea
                value={child.allergies}
                onChange={(e) => {
                  const value = e.target.value
                  setApplication(set('form.child.allergies', value))
                }}
              />

              <Label>{i18n.application.additionalInfo.diet}</Label>
              <TextArea
                value={application.form.child.diet}
                onChange={(e) => {
                  const value = e.target.value
                  setApplication(set('form.child.diet', value))
                }}
              />
            </>
          )}
        </ListGrid>

        {type !== 'CLUB' && (
          <>
            <Gap />
            <Checkbox
              label={i18n.application.additionalInfo.maxFeeAccepted}
              checked={maxFeeAccepted}
              onChange={(value) =>
                setApplication(set('form.maxFeeAccepted', value))
              }
              dataQa="checkbox-maxFeeAccepted"
              disabled={application.origin !== 'PAPER'}
            />
          </>
        )}
      </CollapsibleSection>

      <ApplicationStatusSection application={application} />
    </div>
  )
})

function WithLabel({
  label,
  children
}: {
  label: string
  children: React.ReactNode
}) {
  return (
    <div>
      <Label>{label}</Label>
      {children}
    </div>
  )
}

function FutureAddressInputs({
  futureAddress,
  setApplication,
  i18n,
  path
}: {
  futureAddress: FutureAddress
  setApplication: React.Dispatch<
    React.SetStateAction<ApplicationDetails | undefined>
  >
  i18n: Translations
  path: 'child' | 'guardian'
}) {
  return (
    <div>
      <InputField
        width="L"
        placeholder={i18n.common.form.streetAddress}
        value={futureAddress.street}
        onChange={(value) => {
          setApplication(set(['form', path, 'futureAddress', 'street'], value))
        }}
      />
      <Gap size="xs" />
      <HorizontalContainer>
        <InputField
          width="s"
          placeholder={i18n.common.form.postalCode}
          value={futureAddress.postalCode}
          onChange={(value) => {
            setApplication(
              set(['form', path, 'futureAddress', 'postalCode'], value)
            )
          }}
        />
        <Gap size="xs" horizontal />
        <InputField
          width="m"
          placeholder={i18n.common.form.postOffice}
          value={futureAddress.postOffice}
          onChange={(value) => {
            setApplication(
              set(['form', path, 'futureAddress', 'postOffice'], value)
            )
          }}
        />
      </HorizontalContainer>
    </div>
  )
}

const HorizontalContainer = styled.div`
  display: flex;
  flex-direction: row;
  align-items: center;
`

const VerticalContainer = styled.div`
  display: flex;
  flex-direction: column;
`
