<!--
SPDX-FileCopyrightText: 2017-2026 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

# Form Framework

## Purpose

Quick reference for eVaka's form framework - a type-safe form validation system that manages form state, validation, and error messages. The framework provides automatic validation, type inference, and components that integrate seamlessly with form state.

**Implementation**: `frontend/src/lib-common/form/`

**When to use:**
- Complex forms with multiple fields and validation rules
- Forms where UX is critical (real-time validation feedback)
- Forms with nested objects, arrays, or conditional fields

**When not necessary:**
- Simple forms with 1-2 fields
- Forms without validation requirements
- Quick prototypes (may add unnecessary complexity)

## Basic Concepts

The framework separates form definition from form usage:

1. **Define form structure** - Describe shape and validation rules
2. **Create form instance** - Use `useForm()` hook with initial state and error messages
3. **Bind to components** - Use F-postfix components (InputFieldF, DatePickerF, etc.)

**F-postfix components**: Components ending in `F` (like `InputFieldF`, `DatePickerF`, `SelectF`) are versions that integrate directly with the form framework. See [Component Library](lib-components.md) for the full list.

## Simple Form Example

```typescript
import { object, required, string, number } from 'lib-common/form/form'
import { useForm, useFormFields } from 'lib-common/form/hooks'
import { InputFieldF } from 'lib-components/atoms/form/InputField'

// 1. Define form structure
const userForm = object({
  name: required(string()),
  age: number()
})

// 2. Use in component
function UserForm() {
  const { i18n } = useTranslation()

  const form = useForm(
    userForm,
    () => ({ name: '', age: 0 }),  // initial state
    i18n.validationErrors  // error messages from i18n
  )

  const { name, age } = useFormFields(form)

  return (
    <div>
      <InputFieldF bind={name} />
      <InputFieldF bind={age} type="number" />
      <button
        disabled={!form.isValid()}
        onClick={() => {
          const data = form.value()  // Throws if invalid
          submitUser(data)  // data is { name: string, age: number }
        }}
      >
        Submit
      </button>
    </div>
  )
}
```

## Form Fields

### Basic Field Types

```typescript
import { string, number, boolean } from 'lib-common/form/form'
import { localDate, localDateRange } from 'lib-common/form/fields'

const basicForm = object({
  name: string(),           // Trims whitespace automatically
  age: number(),
  active: boolean(),
  birthDate: localDate(),   // Validates Finnish date format
  period: localDateRange()  // Start and end dates
})
```

### Required vs Optional Fields

By default, fields are optional (allow empty/undefined). Use `required()` for mandatory fields:

```typescript
import { required } from 'lib-common/form/form'

const form = object({
  requiredName: required(string()),    // Must have value
  optionalEmail: string()              // Can be empty
})
```

### Field with Constraints

```typescript
import { localDate } from 'lib-common/form/fields'
import LocalDate from 'lib-common/local-date'

// Set constraints when updating field state
startDate.set(localDate.fromDate(LocalDate.todayInSystemTz(), {
  minDate: LocalDate.of(2024, 1, 1),
  maxDate: LocalDate.of(2025, 12, 31)
}))
```

## Validation

### Error Messages

Error messages are defined in the shared i18n structure and accessed via `useTranslation()`:

```typescript
const { i18n } = useTranslation()

const form = useForm(
  userForm,
  () => initialState,
  i18n.validationErrors  // Shared error messages
)
```

### Checking Form Validity

```typescript
// Check if entire form is valid before getting value
if (form.isValid()) {
  const data = form.value()  // Safe since we checked isValid()
  submitData(data)
}

// value() throws if form is invalid - always check isValid() first
const data = form.value()  // ❌ Throws error if validation fails

// F-postfix components show errors automatically
<InputFieldF bind={nameField} />
```

## Using Forms with Components

### Complete Form Example

```typescript
import { object, required, boolean } from 'lib-common/form/form'
import { string } from 'lib-common/form/fields'
import { useForm, useFormFields } from 'lib-common/form/hooks'
import { InputFieldF } from 'lib-components/atoms/form/InputField'
import { CheckboxF } from 'lib-components/atoms/form/Checkbox'
import { useMutation } from 'lib-common/query'

const userForm = object({
  firstName: required(string()),
  lastName: required(string()),
  email: string(),
  active: boolean()
})

function UserForm() {
  const { i18n } = useTranslation()
  const { mutate } = useMutation(createUserMutation)

  const form = useForm(
    userForm,
    () => ({ firstName: '', lastName: '', email: '', active: true }),
    i18n.validationErrors
  )

  const { firstName, lastName, email, active } = useFormFields(form)

  const handleSubmit = () => {
    if (form.isValid()) {
      mutate(form.value())
    }
  }

  return (
    <>
      <InputFieldF bind={firstName} />
      <InputFieldF bind={lastName} />
      <InputFieldF bind={email} />
      <CheckboxF bind={active} label="Active" />
      <Button disabled={!form.isValid()} onClick={handleSubmit}>
        Submit
      </Button>
    </>
  )
}
```

**F-postfix components** (InputFieldF, CheckboxF, DatePickerF, SelectF, etc.) integrate with the form via the `bind` prop, which provides current state, update functions, and automatic error display.

### Accessing and Updating Fields

```typescript
// Get all fields
const { name, email, age } = useFormFields(form)

// Get single field
const name = useFormField(form, 'name')

// Update field
nameField.set('Alice')
nameField.update(prev => prev.toUpperCase())

// Update entire form
form.update(prev => ({ ...prev, name: 'Bob', age: 30 }))
```

## Advanced Features

The form framework supports additional patterns for complex use cases:

### Arrays

Dynamic lists of form elements:

```typescript
import { array } from 'lib-common/form/form'

const form = object({
  members: array(object({
    name: string(),
    role: string()
  }))
})

// Access array elements
const members = useFormElems(membersField)
members.map((member, i) => <MemberForm key={i} bind={member} />)
```

### Nested Objects

Forms within forms:

```typescript
const form = object({
  user: object({
    name: string(),
    address: object({
      street: string(),
      city: string()
    })
  })
})

// Access nested fields
const { user } = useFormFields(form)
const { name, address } = useFormFields(user)
```

### Union Types

Conditional form branches for mutually exclusive options:

```typescript
import { union, object, required, string, number } from 'lib-common/form/form'
import { useForm, useFormUnion, useFormFields } from 'lib-common/form/hooks'
import type { BoundForm } from 'lib-common/form/hooks'
import { InputFieldF } from 'lib-components/atoms/form/InputField'
import Radio from 'lib-components/atoms/form/Radio'

// Define branches separately for typing
const presentBranch = object({ hours: required(number()) })
const absentBranch = object({ reason: required(string()) })

// Define union form
const attendanceForm = union({
  present: presentBranch,
  absent: absentBranch
})

// Main component
function AttendanceForm() {
  const { i18n } = useTranslation()

  const form = useForm(
    attendanceForm,
    () => ({
      branch: 'present',
      state: { hours: 0 }
    }),
    i18n.validationErrors
  )

  const { branch, form: boundForm } = useFormUnion(form)

  const handleSubmit = () => {
    if (form.isValid()) {
      const data = form.value()
      if (data.branch === 'absent') {
        submitAbsence({ reason: data.value.reason })
      } else {
        submitAttendance({ hours: data.value.hours })
      }
    }
  }

  return (
    <FormContainer>
      <RadioGroup>
        <Radio
          checked={branch === 'present'}
          label="Present"
          onChange={() => form.update(() => ({
            branch: 'present',
            state: { hours: 0 }
          }))}
        />
        <Radio
          checked={branch === 'absent'}
          label="Absent"
          onChange={() => form.update(() => ({
            branch: 'absent',
            state: { reason: '' }
          }))}
        />
      </RadioGroup>

      {branch === 'absent' ? (
        <AbsentInput bind={boundForm} />
      ) : (
        <PresentInput bind={boundForm} />
      )}

      <Button disabled={!form.isValid()} onClick={handleSubmit}>
        Submit
      </Button>
    </FormContainer>
  )
}

// Child components with proper typing
function AbsentInput({ bind }: { bind: BoundForm<typeof absentBranch> }) {
  const { reason } = useFormFields(bind)
  return <InputFieldF bind={reason} label="Absence reason" />
}

function PresentInput({ bind }: { bind: BoundForm<typeof presentBranch> }) {
  const { hours } = useFormFields(bind)
  return <InputFieldF bind={hours} type="number" label="Hours present" />
}
```

**Key pattern:**
1. Define branches separately for proper typing
2. Call `useFormUnion()` to get active branch and bound form
3. Radio buttons switch branches by updating form state
4. Conditionally render child components based on branch
5. Child components typed with `BoundForm<typeof branchDefinition>`

**Use case**: Forms where user chooses between mutually exclusive options with different fields.

### Custom Validation

Transform and validate with `mapped()` and `transformed()`:

```typescript
import { mapped, transformed } from 'lib-common/form/form'
import { ValidationError, ValidationSuccess } from 'lib-common/form/types'

const uppercaseString = mapped(string(), (s) => s.toUpperCase())

const customField = transformed(string(), (value) => {
  if (value.length < 3) return ValidationError.of('tooShort')
  return ValidationSuccess.of(value)
})
```

## See Also

- Form Types & Validators (`frontend/src/lib-common/form/types.ts`, `validators.ts`)
- [Component Library](lib-components.md) - Full list of F-postfix components
