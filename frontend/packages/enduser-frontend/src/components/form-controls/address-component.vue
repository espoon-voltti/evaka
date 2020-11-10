<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

<template>
  <div class="columns is-multiline">
    <div class="column is-two-thirds-tablet">
      <text-field
        name="street"
        :label="labelStreet || $t('form.persons.address.street-label')"
        :placeholder="
          placeholderStreet || $t('form.persons.address.street-label')
        "
        :value="value.street"
        :icon="icon"
        @input="streetChanged"
        :required="isRequired"
        :disabled="disabled"
      >
      </text-field>
    </div>
    <div class="column is-half-tablet is-5-desktop">
      <postal-code-field
        name="postalCode"
        :label="labelPostalCode || $t('form.persons.address.postal-code-label')"
        :placeholder="
          placeholderPostalCode || $t('form.persons.address.postal-code-label')
        "
        :value="value.postalCode"
        @input="postalCodeChanged"
        :icon="icon"
        :required="isRequired"
        :disabled="disabled"
      >
      </postal-code-field>
    </div>

    <div class="column is-half-tablet is-3-desktop">
      <text-field
        name="city"
        :label="labelCity || $t('form.persons.address.city-label')"
        :placeholder="placeholderCity || $t('form.persons.address.city-label')"
        :value="value.postOffice"
        @input="postOfficeChanged"
        :icon="icon"
        :required="isRequired"
        :disabled="disabled"
      >
      </text-field>
    </div>
  </div>
</template>

<script>
  import PostalCodeField from '@/components/form-controls/postal-code-field.vue'
  import TextField from '@/components/form-controls/text-field.vue'

  export default {
    components: {
      TextField,
      PostalCodeField
    },
    props: {
      value: Object,
      labelStreet: String,
      labelPostalCode: String,
      labelCity: String,
      placeholderStreet: String,
      placeholderPostalCode: String,
      placeholderCity: String,
      isRequired: Boolean,
      icon: String,
      disabled: Boolean
    },
    computed: {
      required() {
        return this.isRequired ? 'required' : ''
      }
    },
    methods: {
      streetChanged(value) {
        this.updateAddress({ street: value })
      },
      postalCodeChanged(value) {
        this.updateAddress({ postalCode: value })
      },
      postOfficeChanged(value) {
        this.updateAddress({ postOffice: value })
      },
      updateAddress(value) {
        this.$emit('input', Object.assign({}, this.value, value))
      }
    }
  }
</script>
