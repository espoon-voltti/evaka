// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

export default {
  methods: {
    getFieldValue(form, field) {
      return this.$store.getters.fieldValue(form, field)
    },
    setFieldValue(form, field, value) {
      this.$store.dispatch('updateForm', {
        form,
        field,
        value
      })
    }
  }
}

export function bind(form, field) {
  return {
    get() {
      return this.getFieldValue(form, field)
    },
    set(value) {
      this.setFieldValue(form, field, value)
    }
  }
}
