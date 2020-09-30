// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

export type Status =
  | 'SENT'
  | 'VERIFIED'
  | 'WAITING_DECISION'
  | 'WAITING_CONFIRMATION'
  | 'ACCEPTED'
  | 'REJECTED'
  | 'CANCELLED'
  | 'WAITING_MAILING'

export enum APPLICATION_TYPE {
  DAYCARE = 'daycare',
  PRESCHOOL = 'preschool',
  CLUB = 'club',
  ALL = 'all'
}

export enum APPLICATION_STATUS {
  CREATED = 'LUONNOS',
  SENT = 'LÄHETETTY'
}

export enum GROUNDS {
  ADDITIONAL_INFO = 'ADDITIONAL_INFO',
  SIBLING_BASIS = 'SIBLING_BASIS',
  ASSISTANCE_NEEDED = 'ASSISTANCE_NEEDED',
  WAS_ON_CLUB_CARE = 'WAS_ON_CLUB_CARE',
  WAS_ON_DAYCARE = 'WAS_ON_DAYCARE',
  EXTENDED_CARE = 'EXTENDED_CARE'
}

// TODO : This is currently the only reliable way to select an area, as their id's (UUID) are different in each
// environment. It would be nice to have custom id's for these areas; e.g. Espoonlahti <-> areaId=3, LEPPÄVAARA_LÄNSI <-> areaId=4
// The current implementation will not work if these names are changed in the database, that's why e.g. numerical values would be very useful
export enum AREA {
  ESPOON_KESKUS_POHJOINEN = 'Espoon keskus (pohjoinen)',
  ESPOON_KESKUS_ETELÄINEN = 'Espoon keskus (eteläinen)',
  ESPOONLAHTI = 'Espoonlahti',
  LEPPÄVAARA_LÄNSI = 'Leppävaara (länsi)',
  LEPPÄVAARA_ITÄ = 'Leppävaara (itä)',
  MATINKYLÄ_OLARI = 'Matinkylä-Olari',
  TAPIOLA = 'Tapiola',
  SVENSKA_BILDININGSTJÄNSTER = 'Svenska bildningstjänster'
}
