# Finnish to Swedish Translation Workflow

Complete workflow for translating the Finnish i18n file to Swedish.

## Overview

1. **Extract** Finnish translations to CSV/Excel
2. **Translate** - Give CSV to translator to fill Swedish column
3. **Generate** Swedish TypeScript file from completed CSV

---

## Step 1: Extract Finnish Translations

Extract all translations from `fi.tsx` to a CSV file:

```bash
node extract-for-translation.js > translations.csv
```

This creates a CSV with 3 columns:
- `path` - Full dot notation path (e.g., `titles.defaultTitle`)
- `fi` - Finnish text
- `sv` - Empty column for Swedish translation

**Output**: `translations.csv` with ~3,239 rows

### Open in Excel

The CSV can be opened directly in Excel:
1. Open Excel
2. File → Open → Select `translations.csv`
3. Excel will import it with proper columns

---

## Step 2: Translation

Give `translations.csv` to the translator with instructions:
1. Fill in the `sv` column with Swedish translations
2. Leave `path` and `fi` columns unchanged
3. If a translation is not needed, leave `sv` empty (will use Finnish as fallback)
4. Save as CSV when done

**Important for Translator**:
- Don't modify the `path` or `fi` columns
- Keep special characters like `{variable}` or `\n` intact
- `[FUNCTION]` entries are code functions - they can be skipped or translated as text templates

---

## Step 3: Generate Swedish TypeScript File

Once translation is complete, generate the `sv.tsx` file:

```bash
node generate-sv-from-csv.js translations.csv > sv.tsx
```

This creates a TypeScript file with the same structure as `fi.tsx`:

```typescript
export const sv = {
  titles: {
    defaultTitle: 'Förskola',
    login: 'Logga in',
    // ... all other translations
  },
  common: {
    yes: 'Ja',
    no: 'Nej',
    // ...
  }
}
```

### Place the Generated File

Copy the generated file to the appropriate location:

```bash
# For employee frontend
cp sv.tsx frontend/src/lib-customizations/defaults/employee/i18n/sv.tsx

# Or for citizen frontend (if applicable)
cp sv.tsx frontend/src/lib-customizations/defaults/citizen/i18n/sv.tsx
```

---

## Test Example

A complete round-trip example:

```bash
# 1. Extract
node extract-for-translation.js > translations.csv

# 2. Manually edit translations.csv (or give to translator)
# Add Swedish translations in the 'sv' column

# 3. Generate sv.tsx
node generate-sv-from-csv.js translations.csv > sv.tsx

# 4. Verify the output looks correct
head -50 sv.tsx
```

---

## File Locations

- **Source**: `frontend/src/lib-customizations/defaults/employee/i18n/fi.tsx`
- **CSV for translation**: `translations.csv` (3,239 rows)
- **Generated output**: `sv.tsx`
- **Final destination**: `frontend/src/lib-customizations/defaults/employee/i18n/sv.tsx`

---

## Handling Special Cases

### Functions
Rows with `[FUNCTION]` are dynamic functions. Example:
```csv
common.resultCount,[FUNCTION],{count} resultat
```
The translator should provide a template string, and you'll need to manually convert it back to a function.

### JSX Elements
Rows with `[JSX Element]` contain React components. These may need manual handling after generation.

### Special Characters
- Quotes in text are automatically escaped
- Newlines are preserved
- Variables like `${count}` should be kept as-is

---

## Troubleshooting

### Missing translations
If some translations are missing in the CSV:
- Re-run `extract-for-translation.js` to get a fresh export
- The script extracts only leaf nodes (actual text values)

### Generated file has errors
- Check CSV format (proper commas, quoted fields)
- Ensure no extra columns or malformed rows
- Test with a small subset first

### Special characters broken
- Make sure CSV is saved as UTF-8
- Excel might need "Save As → CSV UTF-8"

---

## Created Files

1. `extract-for-translation.js` - Extracts fi.tsx → CSV
2. `generate-sv-from-csv.js` - Converts CSV → sv.tsx
3. `translations.csv` - The extraction output (ready for translation)
4. `test-translations.csv` - Example with a few Swedish translations

---

## Next Steps After Translation

Once `sv.tsx` is generated and placed in the correct location:

1. **Import it** in the appropriate i18n index file
2. **Test** the application with Swedish locale
3. **Commit** the new translation file to git
4. **Deploy** to make Swedish available to users
