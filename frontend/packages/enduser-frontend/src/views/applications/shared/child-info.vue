<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

<template>
  <div>
    <Person
      v-model="model"
      :required="required"
      :name="inputName"
      :disabled="disablePersonFields"
    />

    <Address
      v-model="model"
      :name="[inputName, 'address']"
      :disabled="disablePersonFields"
    />

    <c-checkbox
      :class="{ correctingAddress: !addressChanging }"
      v-model="model"
      :name="[inputName, 'hasCorrectingAddress']"
      :label="$t('form.persons.new-addr')"
    >
      <c-instructions :instruction="$t('form.persons.new-addr-info')" />
    </c-checkbox>
    <c-content :secondary="true" v-if="addressChanging">
      <Address
        v-model="model"
        :required="addressChanging"
        :name="[inputName, 'correctingAddress']"
        :showMovingDate="addressChanging"
        movingDateKey="child"
        movingDateField="childMovingDate"
      />
    </c-content>
  </div>
</template>

<script lang="ts">
  import Vue from 'vue'
  import { mapGetters } from 'vuex'

  import { get } from 'lodash'

  import Person from './person.vue'
  import Address from './address.vue'
  import { ChildInfoType } from './types'

  export default Vue.extend({
    components: {
      Person,
      Address
    },

    props: {
      name: {
        required: true
      },
      value: {
        type: Object as () => ChildInfoType,
        required: true
      },
      required: {
        type: Boolean as () => boolean,
        default: false
      },
      disablePersonFields: {
        type: Boolean as () => boolean,
        default: false
      }
    },

    computed: {
      ...mapGetters(['countries', 'languages']),

      inputName(): string {
        const name = this.name as any
        return name instanceof Array ? name.filter((n) => n).join('.') : name
      },

      model: {
        get(): ChildInfoType {
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
<style scoped>
  .correctingAddress {
    margin-bottom: 2rem;
  }
</style>
