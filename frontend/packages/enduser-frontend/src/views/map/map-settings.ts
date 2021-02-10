// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import Vue from 'vue'
import { config } from '@/config'

const mapSettings = new Vue({
  data: {
    mapOptions: {
      scrollwheel: true,
      mapTypeControl: false,
      fullscreenControl: false,
      styles: config.maps.styles
    },
    icons: {
      homeMarker: {},
      otherMarkers: {}
    }
    // CLUSTER RELATED CODE

    // clusterStyles: [{
    //   textColor: 'white',
    //   url: '/static/cluster.svg',
    //   height: 40,
    //   width: 40,
    //   textSize: 16,
    //   anchorText: [1, 0]
    // }]
  }
})

export default mapSettings
