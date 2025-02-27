// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import { ChildImageId } from 'lib-common/generated/api-types/shared'
import { Uri } from 'lib-common/uri'
import { client } from '../../api-client'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.childimages.ChildImageController.getImageCitizen
*/
export function getImageCitizen(
  request: {
    imageId: ChildImageId
  }
): { url: Uri } {
  return {
    url: uri`${client.defaults.baseURL ?? ''}/citizen/child-images/${request.imageId}`
  }
}
