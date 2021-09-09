import React from 'react'
import styled from 'styled-components'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faCalendarPlus, faUserMinus } from 'lib-icons'
import ModalBackground from 'lib-components/molecules/modals/ModalBackground'
import Button from 'lib-components/atoms/buttons/Button'
import { defaultMargins, Gap } from 'lib-components/white-space'
import { useTranslation } from 'citizen-frontend/localization'

interface Props {
  close: () => void
  openReservations: () => void
  openAbsences: () => void
}

export default React.memo(function ActionPickerModal({
  close,
  openReservations,
  openAbsences
}: Props) {
  const i18n = useTranslation()

  return (
    <ModalBackground onClick={close}>
      <Container>
        <Action onClick={openAbsences} data-qa="calendar-action-absences">
          {i18n.calendar.newAbsence}
          <IconBackground>
            <FontAwesomeIcon icon={faUserMinus} size="1x" />
          </IconBackground>
        </Action>
        <Gap size="s" />
        <Action
          onClick={openReservations}
          data-qa="calendar-action-reservations"
        >
          {i18n.calendar.newReservationBtn}
          <IconBackground>
            <FontAwesomeIcon icon={faCalendarPlus} size="1x" />
          </IconBackground>
        </Action>
      </Container>
    </ModalBackground>
  )
})

const Container = styled.div`
  position: fixed;
  bottom: calc(46px + ${defaultMargins.L});
  right: ${defaultMargins.s};
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  align-items: flex-end;
`

const Action = styled(Button)`
  border: none;
  background: none;
  color: ${(p) => p.theme.colors.greyscale.white};
  padding: 0;
  min-height: 0;
`

const IconBackground = styled.div`
  display: inline-flex;
  justify-content: center;
  align-items: center;
  width: 34px;
  height: 34px;
  color: ${(p) => p.theme.colors.main.primary};
  background: ${(p) => p.theme.colors.greyscale.white};
  margin-left: ${defaultMargins.s};
  border-radius: 50%;
`
