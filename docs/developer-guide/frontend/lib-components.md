<!--
SPDX-FileCopyrightText: 2017-2026 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

# Component Library (lib-components)

## Purpose

Reference for eVaka's component library - a collection of reusable UI components with consistent styling and behavior. The library includes both standard React components and form-integrated variants (F-postfix components).

**Implementation**: `frontend/src/lib-components/`

## Component Categories

### Atoms
Basic building blocks - buttons, inputs, checkboxes, etc.

### Molecules
Composite components built from atoms - modals, date pickers, etc.

### Form-Integrated Components (F-postfix)
Components ending in `F` integrate directly with the [form framework](forms.md):
- `InputFieldF` - Text input with form binding
- `CheckboxF` - Checkbox with form binding
- `DatePickerF` - Date picker with form binding
- `SelectF` - Dropdown select with form binding
- And more...

These components accept a `bind` prop from the form framework and handle state, validation, and error display automatically.

**TODO: This document needs to be written.**
