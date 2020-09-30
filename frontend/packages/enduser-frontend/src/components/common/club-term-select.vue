<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

<template>
  <div>
    <div v-for="term in terms" :key="term.id" class="club-term-select">
      <radio
        :value="value"
        :inputValue="term.id"
        :label="termLabels[term.id]"
        class="radio-wrapper"
        @change="(evt) => $emit('change', term)"
        :aria-label="$t('form.care.club-term') + ` ${termLabels[term.id]}`"
      ></radio>
    </div>
  </div>
</template>

<script>
  import Radio from '@/components/styleguide/input-radio'

  export default {
    name: 'ClubTermSelector',
    components: {
      Radio
    },
    props: {
      terms: {
        type: Array,
        required: true
      },
      value: {
        type: String,
        required: true
      }
    },
    computed: {
      termLabels() {
        const { date } = this.$options.filters
        return this.terms.reduce(
          (labels, term) => ({
            ...labels,
            [term.id]: `${date(term.start)} - ${date(term.end)}`
          }),
          {}
        )
      }
    }
  }
</script>

<style lang="scss" scoped>
  .club-term-select {
    .radio-wrapper {
      margin-bottom: 0;
    }
  }
</style>
