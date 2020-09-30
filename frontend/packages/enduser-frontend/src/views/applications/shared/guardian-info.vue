<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

<template>
  <div>
    <Person
      :required="required"
      v-model="model"
      :name="inputName"
      :disabled="disablePersonFields"
      :isChild="false"
      v-if="showPersonFields"
      :ssnRequired="!isGuardian2"
    />

    <div class="columns" v-if="showPersonFields">
      <div class="column is-4">
        <c-tel-input
          :required="requireTel"
          v-model="model"
          :name="[inputName, 'phoneNumber']"
          :label="$t('general.input.tel')"
          :leftIcon="['far', 'mobile-alt']"
        />
      </div>
      <div class="column is-4">
        <c-email-input
          v-model="model"
          :name="[inputName, 'email']"
          :label="$t('general.input.email')"
          :leftIcon="['far', 'at']"
          :required="false"
        />
      </div>
    </div>

    <Address
      v-model="model"
      :name="[inputName, 'address']"
      :disabled="disablePersonFields"
      v-if="showAddressFields"
    />

    <c-checkbox
      v-if="showAddressFields"
      :class="{ correctingAddress: !addressChanging }"
      v-model="model"
      :name="[inputName, 'hasCorrectingAddress']"
      :label="$t('form.persons.new-addr')"
    >
      <c-instructions :instruction="$t('form.persons.new-addr-info')" />
    </c-checkbox>

    <c-content :secondary="true" v-if="addressChanging && showAddressFields">
      <Address
        v-model="model"
        :required="addressChanging"
        :name="[inputName, 'correctingAddress']"
        :showMovingDate="addressChanging"
        :movingDateKey="movingDateKey"
        movingDateField="guardianMovingDate"
      />
    </c-content>

    <c-info-box bold v-if="isGuardian2 && showPersonFields">
      {{ $t('form.persons.guardian2.optional-second-guardian-information') }}
    </c-info-box>
  </div>
</template>

<script lang="ts">
  import Vue from 'vue'
  import { mapGetters } from 'vuex'
  import { get } from 'lodash'
  import Person from './person.vue'
  import Address from './address.vue'

  export default Vue.extend({
    components: {
      Person,
      Address
    },
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
      disablePersonFields: {
        type: Boolean as () => boolean,
        default: false
      },
      showPersonFields: {
        type: Boolean as () => boolean,
        default: true
      },
      showAddressFields: {
        type: Boolean as () => boolean,
        default: true
      },
      movingDateKey: {
        type: String,
        default: 'guardian'
      },
      requireTel: {
        type: Boolean as () => boolean,
        default: true
      }
    },
    computed: {
      ...mapGetters(['countries', 'languages']),
      isGuardian2() {
        return this.movingDateKey === 'guardian2'
      },
      inputName(): string {
        const name = this.name as any
        return name instanceof Array ? name.filter((n) => n).join('.') : name
      },
      model: {
        get(): object {
          return this.value
        },

        set(value: object) {
          this.$emit('input', value)
        }
      },
      addressChanging() {
        return get(this.model, `${this.inputName}.hasCorrectingAddress`)
      }
    }
  })
</script>

<style lang="scss" scoped>
  .correctingAddress {
    margin-bottom: 2rem;
  }

  .optional-second-guardian-information {
    color: $black;
    font-size: 0.9rem;
    font-weight: bold;
    padding-bottom: 1rem;
  }
</style>
