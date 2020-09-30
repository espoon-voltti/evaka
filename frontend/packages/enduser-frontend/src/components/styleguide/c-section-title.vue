<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

<script>
  import Title from './c-title'

  export default {
    extends: Title,
    name: 'SectionTitle',
    props: {
      icon: {
        type: [String, Array],
        default: ''
      },
      size: {
        type: [Number, String],
        default: 3
      },
      customSize: {
        type: Boolean,
        default: true
      }
    },

    render(createElement) {
      return createElement(
        `h${this.size}`,
        {
          class: [
            `title is-${this.size} section-title ${
              this.customSize ? 'custom-size' : ''
            }`
          ]
        },
        [
          this.icon
            ? createElement('font-awesome-icon', {
                props: {
                  icon: this.icon
                }
              })
            : createElement('div'),
          this.$slots.default
        ]
      )
    }
  }
</script>

<style lang="scss" scoped>
  .section-title {
    color: $grey-dark;

    svg {
      margin-right: 8px;
    }
  }

  .custom-size {
    font-size: 22px;
  }
</style>
