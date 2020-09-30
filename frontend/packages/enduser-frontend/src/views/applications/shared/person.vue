<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

<template>
  <div class="columns">
    <div class="column is-4">
      <c-input
        :required="required"
        :disabled="disabled"
        :name="[inputName, 'firstName']"
        v-model="inputValue"
        :label="$t(firstNameLabel)"
        :leftIcon="['far', 'id-card']"
      />
    </div>
    <div class="column is-5">
      <c-input
        :required="required"
        :disabled="disabled"
        :name="[inputName, 'lastName']"
        v-model="inputValue"
        :label="$t(lastNameLabel)"
        :leftIcon="['far', 'id-card']"
      />
    </div>
    <div class="column is-3">
      <c-ssn-input
        :required="isSsnRequired"
        :disabled="disabled"
        :name="[inputName, 'socialSecurityNumber']"
        v-model="inputValue"
        :label="$t(ssnLabel)"
        :leftIcon="['far', 'id-card']"
      />
    </div>
  </div>
</template>

<script lang="ts">
  import Vue from 'vue'

  export default Vue.extend({
    props: {
      value: {
        type: Object as () => object,
        required: true
      },
      name: {
        default: ''
      },
      required: {
        type: Boolean as () => boolean,
        default: false
      },
      disabled: {
        type: Boolean as () => boolean,
        default: false
      },
      isChild: {
        type: Boolean as () => boolean,
        default: true
      },
      isOther: {
        type: Boolean as () => boolean,
        default: false
      },
      ssnRequired: {
        type: Boolean as () => boolean,
        default: true
      }
    },
    computed: {
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
      },
      firstNameLabel(): string {
        if (this.isOther) {
          return 'form.daycare-application.personalInfo.otherFirstName'
        } else {
          return `form.daycare-application.personalInfo.${
            this.isChild ? 'child' : 'guardian'
          }.firstName`
        }
      },
      lastNameLabel(): string {
        if (this.isOther) {
          return 'form.daycare-application.personalInfo.otherLastName'
        } else {
          return `form.daycare-application.personalInfo.${
            this.isChild ? 'child' : 'guardian'
          }.lastName`
        }
      },
      ssnLabel(): string {
        if (this.isOther) {
          return 'form.daycare-application.personalInfo.otherSsn'
        } else {
          return `form.daycare-application.personalInfo.${
            this.isChild ? 'child' : 'guardian'
          }.ssn`
        }
      },
      isSsnRequired(): boolean {
        return !this.ssnRequired ? false : this.required
      }
    }
  })
</script>

<style></style>
