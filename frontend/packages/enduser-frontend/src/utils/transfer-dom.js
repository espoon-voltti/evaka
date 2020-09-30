// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import Vue from 'vue'

const transferDom = {
  inserted: function(el, bindings) {
    // default append to <body>
    const container = bindings.arg
      ? document.getElementById(bindings.arg)
      : document.body
    if (container) {
      bindings.modifiers.prepend && container.firstChild
        ? container.insertBefore(el, container.firstChild)
        : container.appendChild(el)
    } else {
      console.warn(
        'v-' + name + ' target element id "' + bindings.arg + '" not found.'
      )
    }
  },
  unbind: function(el) {
    if (el.parentNode) {
      el.parentNode.removeChild(el)
    }
  }
}

Vue.directive('transferDom', transferDom)
