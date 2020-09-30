<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

<template>
  <div class="columns is-multiline">
    <c-datepicker
      v-if="showMovingDate"
      :label="$t('form.daycare-application.personalInfo.movingDate')"
      v-model="inputValue"
      :name="[movingDateKey, movingDateField]"
      :required="true"
      :iconRight="true"
      :border="true"
      :locale="locale"
      :placeholder="$t('general.input.moving-date')"
      class="column is-12 moving-date-datepicker"
    >
    </c-datepicker>
    <div class="column is-12">
      <c-input
        :required="required"
        :disabled="disabled"
        :name="[inputName, 'street']"
        :label="$t('form.daycare-application.personalInfo.street')"
        v-model="inputValue"
        :leftIcon="['far', 'home']"
      />
    </div>
    <div class="column is-6">
      <c-postal-code-input
        :required="required"
        :disabled="disabled"
        :name="[inputName, 'postalCode']"
        :label="$t('form.daycare-application.personalInfo.postalCode')"
        v-model="inputValue"
        :leftIcon="['far', 'home']"
      />
    </div>
    <div class="column is-6">
      <c-input
        :required="required"
        :disabled="disabled"
        :name="[inputName, 'city']"
        :label="$t('form.daycare-application.personalInfo.city')"
        v-model="inputValue"
        :leftIcon="['far', 'home']"
      />
    </div>
  </div>
</template>

<script lang="ts">
  import Vue from 'vue'
  import { AddressType } from './types'

  export default Vue.extend({
    props: {
      value: {
        type: Object as () => AddressType,
        required: true
      },
      name: {
        required: true
      },
      required: {
        type: Boolean as () => boolean,
        default: false
      },
      disabled: {
        type: Boolean as () => boolean,
        default: false
      },
      showMovingDate: {
        type: Boolean as () => boolean,
        default: false
      },
      movingDateKey: {
        type: String,
        default: ''
      },
      movingDateField: {
        type: String,
        default: ''
      }
    },
    computed: {
      locale(): string {
        return this.$i18n.locale
      },
      inputName(): string {
        const name = this.name as any
        return name instanceof Array ? name.filter((n) => n).join('.') : name
      },
      inputValue: {
        get(): object {
          return this.value
        },
        set(value: object) {
          this.$emit('input', value)
        }
      }
    }
  })
</script>

<style scoped>
  .moving-date-datepicker {
    width: 13rem;
  }
</style>
