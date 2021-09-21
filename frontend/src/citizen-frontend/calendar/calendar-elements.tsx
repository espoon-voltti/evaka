import { useTranslation } from 'citizen-frontend/localization'
import React from 'react'
import styled from 'styled-components'

export const Holiday = React.memo(function Holiday() {
  const i18n = useTranslation()
  return <HolidayNote>{i18n.calendar.holiday}</HolidayNote>
})

const HolidayNote = styled.div`
  font-style: italic;
  color: ${({ theme }) => theme.colors.greyscale.dark};
`

export const NoReservation = React.memo(function NoReservation() {
  const i18n = useTranslation()
  return <NoReservationNote>{i18n.calendar.noReservation}</NoReservationNote>
})

const NoReservationNote = styled.span`
  color: ${({ theme }) => theme.colors.accents.orangeDark};
`
