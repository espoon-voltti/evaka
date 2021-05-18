// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

export type ApplicationType = 'CLUB' | 'DAYCARE' | 'PRESCHOOL'

export type ApplicationStatus =
  | 'CREATED'
  | 'SENT'
  | 'WAITING_PLACEMENT'
  | 'WAITING_UNIT_CONFIRMATION'
  | 'WAITING_DECISION'
  | 'WAITING_MAILING'
  | 'WAITING_CONFIRMATION'
  | 'REJECTED'
  | 'ACTIVE'
  | 'CANCELLED'

export type ApplicationOrigin = 'ELECTRONIC' | 'PAPER'

export type ApplicationGuardianAgreementStatus =
  | 'AGREED'
  | 'NOT_AGREED'
  | 'RIGHT_TO_GET_NOTIFIED'

export type AttachmentType = 'URGENCY' | 'EXTENDED_CARE'

export type PlacementType = 
 | 'DAYCARE'
 | 'DAYCARE_PART_TIME'
