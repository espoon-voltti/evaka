// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import InfoModal from 'lib-components/molecules/modals/InfoModal'
import { faExclamation } from 'lib-icons'

import { useTranslation } from '../localization'

interface Props {
  onClose: () => void
}

export default React.memo(function NonReservableDaysWarningModal({
  onClose
}: Props) {
  const i18n = useTranslation()
  return (
    <InfoModal
      icon={faExclamation}
      type="warning"
      title={i18n.calendar.nonReservableDaysWarningModal.title}
      text={i18n.calendar.nonReservableDaysWarningModal.text}
      resolve={{
        action: onClose,
        label: i18n.calendar.nonReservableDaysWarningModal.ok
      }}
      closeLabel={i18n.common.close}
      close={onClose}
      data-qa="non-reservable-days-warning-modal"
    />
  )
})
