// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useMemo, useState } from 'react'
import styled from 'styled-components'
import { Loading, Result } from 'lib-common/api'
import { UUID } from 'lib-common/types'
import InputField from 'lib-components/atoms/form/InputField'
import InfoModal from 'lib-components/molecules/modals/InfoModal'
import { Bold, fontWeights, P } from 'lib-components/typography'
import colors from 'lib-customizations/common'
import { faPlus } from 'lib-icons'
import {
  getPairingStatus,
  PairingResponse,
  postPairing,
  postPairingResponse,
  putMobileDeviceName
} from '../api/unit'
import { useTranslation } from '../state/i18n'
import { renderResult } from './async-rendering'

type IdProps = { unitId: UUID } | { employeeId: UUID }

type Props = IdProps & { closeModal: () => void }

const Flex = styled.div`
  display: flex;
  align-items: center;
  justify-content: center;
`

const ResponseKey = styled.div`
  font-family: Montserrat, sans-serif;
  font-style: normal;
  font-weight: ${fontWeights.semibold};
  font-size: 30px;
  line-height: 30px;
  text-align: center;
  letter-spacing: 0.08em;
  color: ${colors.grayscale.g70};
`

export default React.memo(function MobilePairingModal({
  closeModal,
  ...idProps
}: Props) {
  const { i18n } = useTranslation()

  const [phase, setPhase] = useState<1 | 2 | 3>(1)
  const [responseKey, setResponseKey] = useState<string>('')
  const [deviceName, setDeviceName] = useState<string>('')
  const [pairingResponse, setPairingResponse] = useState<
    Result<PairingResponse>
  >(Loading.of())

  useEffect(() => {
    if (phase === 1) {
      void postPairing(idProps).then(setPairingResponse)
    }
  }, [phase]) // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    if (responseKey.length === 10) {
      if (pairingResponse.isSuccess) {
        void postPairingResponse(
          pairingResponse.value.id,
          pairingResponse.value.challengeKey,
          responseKey
        ).then((res) => {
          if (res.isSuccess) {
            setPairingResponse(res)
            if (res.value.status === 'READY') {
              setPhase(3)
            }
          }
        })
      }
    }
  }, [responseKey]) // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    const polling = setInterval(() => {
      if (pairingResponse.isSuccess) {
        if (pairingResponse.value.status === 'WAITING_CHALLENGE') {
          void getPairingStatus(pairingResponse.value.id).then((status) => {
            if (status.isSuccess) {
              if (status.value.status === 'WAITING_RESPONSE') {
                clearInterval(polling)
                setPhase(2)
              }
            }
          })
        }
      }
    }, 1000)
    return () => clearInterval(polling)
  }, [pairingResponse])

  const actions = useMemo(() => {
    if (phase !== 3) {
      return { resolve: { action: closeModal, label: i18n.common.cancel } }
    }

    const saveDeviceName = () => {
      if (pairingResponse.isSuccess && pairingResponse.value.mobileDeviceId) {
        void putMobileDeviceName(
          pairingResponse.value.mobileDeviceId,
          deviceName
        ).then(closeModal)
      }
    }

    return {
      reject: { action: closeModal, label: i18n.common.cancel },
      resolve: { action: saveDeviceName, label: i18n.common.ready }
    }
  }, [i18n, phase, closeModal, pairingResponse, deviceName])

  return (
    <InfoModal
      title={
        'unitId' in idProps
          ? i18n.mobilePairingModal.sharedDeviceModalTitle
          : i18n.mobilePairingModal.personalDeviceModalTitle
      }
      icon={faPlus}
      data-qa={`mobile-pairing-modal-phase-${phase}`}
      iconColor={phase === 3 ? 'green' : 'blue'}
      {...actions}
    >
      {phase === 1 && (
        <>
          <P centered>
            {i18n.mobilePairingModal.modalText1}
            <br />
            <Bold>{`${window.location.hostname}/employee/mobile`}</Bold>
            <br />
            {i18n.mobilePairingModal.modalText2}
          </P>
          {renderResult(pairingResponse, (pairingResponse) => (
            <ResponseKey data-qa="challenge-key">
              {pairingResponse.challengeKey}
            </ResponseKey>
          ))}
        </>
      )}

      {phase === 2 && (
        <>
          <P centered>{i18n.mobilePairingModal.modalText3}</P>
          {renderResult(pairingResponse, () => (
            <Flex>
              <InputField
                value={responseKey}
                onChange={setResponseKey}
                placeholder={i18n.common.code}
                width="m"
                data-qa="response-key-input"
              />
            </Flex>
          ))}
        </>
      )}

      {phase === 3 && (
        <>
          <P centered>{i18n.mobilePairingModal.modalText4}</P>
          {renderResult(pairingResponse, (pairingResponse) => (
            <Flex>
              {pairingResponse.mobileDeviceId ? (
                <InputField
                  value={deviceName}
                  onChange={setDeviceName}
                  placeholder={i18n.mobilePairingModal.namePlaceholder}
                  width="m"
                  data-qa="mobile-device-name-input"
                  id={pairingResponse.mobileDeviceId}
                />
              ) : (
                <div>{i18n.common.loadingFailed}</div>
              )}
            </Flex>
          ))}
        </>
      )}
    </InfoModal>
  )
})
