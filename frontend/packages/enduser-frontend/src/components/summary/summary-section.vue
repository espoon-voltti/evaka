<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

<template>
  <section class="summary-section">
    <c-title :size="2">
      {{ title }}
    </c-title>
    <slot />
  </section>
</template>

<script>
  export default {
    name: 'summary-section',
    props: {
      title: String
    }
  }
</script>

<style lang="scss" scoped>
  .summary-section {
    margin-bottom: 1rem;
    margin-top: 1.5rem;
    padding: 0.5rem 2rem;

    @include onMobile() {
      padding: 0 1rem;
    }

    .divider {
      margin: 2rem 0;
    }
  }
</style>
