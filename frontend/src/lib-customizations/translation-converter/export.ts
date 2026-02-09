// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

/* eslint-disable @typescript-eslint/no-unsafe-assignment */
/* eslint-disable @typescript-eslint/no-unsafe-member-access */
/* eslint-disable @typescript-eslint/no-explicit-any */
/* eslint-disable no-console */

import * as fs from 'fs'
import * as path from 'path'
import { fileURLToPath } from 'url'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)

const MOBILE = false

const relPath = MOBILE
  ? '../defaults/employee-mobile-frontend/i18n/fi.ts'
  : '../defaults/employee/i18n/fi.tsx'

function main() {
  // Read the fi.tsx file as text
  const fiPath = path.join(__dirname, relPath)
  const sourceCode = fs.readFileSync(fiPath, 'utf-8')

  // Extract the fi object content
  const fiObjectContent = extractFiObject(sourceCode)
  if (!fiObjectContent) {
    console.error('Failed to extract fi object from source')
    process.exit(1)
  }

  // Parse the object structure
  const fi = parseObject(fiObjectContent)

  const csvLines: string[] = ['Avain;Suomeksi;Ruotsiksi']
  const manualCases: string[] = []

  // Process the translation object
  processObject(fi, '', csvLines, manualCases)

  // Create output directory if it doesn't exist
  const outputDir = path.join(__dirname, 'output')
  if (!fs.existsSync(outputDir)) {
    fs.mkdirSync(outputDir, { recursive: true })
  }

  // Write output.csv
  fs.writeFileSync(path.join(outputDir, 'output.csv'), csvLines.join('\n'), {
    encoding: 'utf-8'
  })

  // Write manual.txt
  fs.writeFileSync(
    path.join(outputDir, 'manual.txt'),
    manualCases.join('\n\n'),
    { encoding: 'utf-8' }
  )

  console.log(
    `✓ Exported ${csvLines.length} simple strings to output/output.csv`
  )

  console.log(
    `✓ Exported ${manualCases.length} functions/arrays to output/manual.txt`
  )

  console.log(
    `\nTotal: ${csvLines.length + manualCases.length} translation entries`
  )
}

function extractFiObject(sourceCode: string): string | null {
  // Find "export const fi = {" and extract everything until the matching closing brace
  const match = /export\s+const\s+fi\s+=\s+\{/.exec(sourceCode)
  if (!match || !match.index) return null

  const startIndex = match.index + match[0].length - 1 // Include the opening brace
  let braceCount = 0
  let endIndex = startIndex
  let inString = false
  let stringChar = ''
  let inTemplate = false

  for (let i = startIndex; i < sourceCode.length; i++) {
    const char = sourceCode[i]
    const prevChar = i > 0 ? sourceCode[i - 1] : ''

    // Handle strings
    if ((char === "'" || char === '"') && prevChar !== '\\') {
      if (!inString) {
        inString = true
        stringChar = char
      } else if (char === stringChar) {
        inString = false
      }
    }

    // Handle template literals
    if (char === '`' && prevChar !== '\\') {
      inTemplate = !inTemplate
    }

    // Count braces only outside of strings
    if (!inString && !inTemplate) {
      if (char === '{') braceCount++
      else if (char === '}') {
        braceCount--
        if (braceCount === 0) {
          endIndex = i + 1
          break
        }
      }
    }
  }

  return sourceCode.substring(startIndex, endIndex)
}

function parseObject(objectString: string): any {
  const result: any = {}

  // Remove outer braces
  objectString = objectString.trim()
  if (objectString.startsWith('{')) {
    objectString = objectString.substring(1, objectString.length - 1)
  }

  // Parse key-value pairs
  let i = 0
  while (i < objectString.length) {
    // Skip whitespace and commas
    while (i < objectString.length && /[\s,]/.test(objectString[i])) {
      i++
    }
    if (i >= objectString.length) break

    // Extract key
    const keyMatch = /^([a-zA-Z_$][a-zA-Z0-9_$]*)\s*:/.exec(
      objectString.substring(i)
    )
    if (!keyMatch) {
      i++
      continue
    }

    const key = keyMatch[1]
    i += keyMatch[0].length

    // Skip whitespace
    while (i < objectString.length && /\s/.test(objectString[i])) {
      i++
    }

    // Extract value
    const valueResult = extractValue(objectString, i)
    if (valueResult) {
      result[key] = valueResult.value
      i = valueResult.endIndex
    } else {
      i++
    }
  }

  return result
}

function extractValue(
  str: string,
  startIndex: number
): { value: any; endIndex: number } | null {
  let i = startIndex

  // Skip whitespace
  while (i < str.length && /\s/.test(str[i])) {
    i++
  }

  const char = str[i]

  // String literal
  if (char === "'" || char === '"' || char === '`') {
    return extractString(str, i)
  }

  // Object
  if (char === '{') {
    return extractNestedObject(str, i)
  }

  // Array
  if (char === '[') {
    return extractArray(str, i)
  }

  // Function (arrow function or regular function)
  if (
    /^(\([^)]*\)\s*=>|[a-zA-Z_$][a-zA-Z0-9_$]*\s*=>|function\s*\()/.exec(
      str.substring(i)
    )
  ) {
    return extractFunction(str, i)
  }

  // JSX (starts with <)
  if (char === '(') {
    // Could be arrow function params or JSX wrapped in parens
    const ahead = str.substring(i)
    if (/^\([^)]*\)\s*=>/.exec(ahead)) {
      return extractFunction(str, i)
    }
  }

  return null
}

function extractString(
  str: string,
  startIndex: number
): { value: string; endIndex: number } | null {
  const quote = str[startIndex]
  let i = startIndex + 1
  let value = ''
  let escaped = false

  while (i < str.length) {
    const char = str[i]

    if (escaped) {
      value += char
      escaped = false
    } else if (char === '\\') {
      escaped = true
    } else if (char === quote) {
      return { value, endIndex: i + 1 }
    } else {
      value += char
    }

    i++
  }

  return null
}

function extractNestedObject(
  str: string,
  startIndex: number
): { value: any; endIndex: number } | null {
  let braceCount = 0
  let i = startIndex
  let inString = false
  let stringChar = ''
  let inTemplate = false

  while (i < str.length) {
    const char = str[i]
    const prevChar = i > 0 ? str[i - 1] : ''

    // Handle strings
    if ((char === "'" || char === '"') && prevChar !== '\\') {
      if (!inString) {
        inString = true
        stringChar = char
      } else if (char === stringChar) {
        inString = false
      }
    }

    // Handle template literals
    if (char === '`' && prevChar !== '\\') {
      inTemplate = !inTemplate
    }

    // Count braces only outside strings
    if (!inString && !inTemplate) {
      if (char === '{') braceCount++
      else if (char === '}') {
        braceCount--
        if (braceCount === 0) {
          const objectString = str.substring(startIndex, i + 1)
          const parsed = parseObject(objectString)
          return { value: parsed, endIndex: i + 1 }
        }
      }
    }
    i++
  }

  return null
}

function extractArray(
  str: string,
  startIndex: number
): { value: string; endIndex: number } | null {
  let bracketCount = 0
  let i = startIndex
  let inString = false
  let stringChar = ''
  let inTemplate = false

  while (i < str.length) {
    const char = str[i]
    const prevChar = i > 0 ? str[i - 1] : ''

    // Handle strings
    if ((char === "'" || char === '"') && prevChar !== '\\') {
      if (!inString) {
        inString = true
        stringChar = char
      } else if (char === stringChar) {
        inString = false
      }
    }

    // Handle template literals
    if (char === '`' && prevChar !== '\\') {
      inTemplate = !inTemplate
    }

    // Count brackets only outside strings
    if (!inString && !inTemplate) {
      if (char === '[') bracketCount++
      else if (char === ']') {
        bracketCount--
        if (bracketCount === 0) {
          const arrayString = str.substring(startIndex, i + 1)
          return { value: `ARRAY:${arrayString}`, endIndex: i + 1 }
        }
      }
    }
    i++
  }

  return null
}

function extractFunction(
  str: string,
  startIndex: number
): { value: string; endIndex: number } | null {
  let i = startIndex
  let depth = 0
  let inString = false
  let stringChar = ''
  const templateStack: number[] = [] // Stack to track nested templates and their expression brace depths
  let foundArrow = false

  // Scan forward to find the function body
  while (i < str.length) {
    const char = str[i]
    const prevChar = i > 0 ? str[i - 1] : ''
    const nextTwo = str.substring(i, i + 2)

    const inTemplate = templateStack.length > 0
    const inTemplateExpr =
      inTemplate && templateStack[templateStack.length - 1] > 0

    // Handle strings (only when not in template or when in template expression)
    if ((char === "'" || char === '"') && prevChar !== '\\') {
      // In template string part (not expression), ignore quotes
      if (!inTemplate || inTemplateExpr) {
        if (!inString) {
          inString = true
          stringChar = char
        } else if (char === stringChar) {
          inString = false
        }
      }
    }

    // Handle template literal start/end
    if (char === '`' && prevChar !== '\\' && !inString) {
      // Check if we're ending a template or starting a new one
      if (inTemplate && templateStack[templateStack.length - 1] === 0) {
        // Ending a template literal (no open expressions)
        templateStack.pop()
      } else {
        // Starting a new template literal
        templateStack.push(0)
      }
      i++
      continue
    }

    // Handle template expression start ${
    if (inTemplate && nextTwo === '${' && prevChar !== '\\' && !inString) {
      templateStack[templateStack.length - 1]++
      depth++ // Count the opening brace
      i += 2
      continue
    }

    // Track braces/parens/brackets
    if (!inString && (!inTemplate || inTemplateExpr)) {
      if (char === '(' || char === '{' || char === '[') {
        depth++
      } else if (char === ')' || char === '}' || char === ']') {
        // Check if this closes a template expression
        if (inTemplateExpr && char === '}') {
          templateStack[templateStack.length - 1]--
        }

        depth--
        // If we're back to depth 0 and we found an arrow, we're done
        // But not if we just closed a template expression (still in template)
        const stillInTemplate = templateStack.length > 0
        if (
          depth === 0 &&
          foundArrow &&
          (char === '}' || char === ')') &&
          !stillInTemplate
        ) {
          return {
            value: `FUNCTION:${str.substring(startIndex, i + 1)}`,
            endIndex: i + 1
          }
        }
      }

      // Look for arrow
      if (nextTwo === '=>' && !foundArrow) {
        foundArrow = true
        i += 2
        continue
      }

      // If we found arrow and we're at a comma at depth 0, we're done (expression body)
      if (foundArrow && char === ',' && depth === 0) {
        return {
          value: `FUNCTION:${str.substring(startIndex, i)}`,
          endIndex: i
        }
      }
    }

    i++
  }

  // Reached end of string
  if (foundArrow) {
    return { value: `FUNCTION:${str.substring(startIndex, i)}`, endIndex: i }
  }

  return null
}

function processObject(
  obj: any,
  path: string,
  csvLines: string[],
  manualCases: string[]
) {
  for (const key in obj) {
    if (!Object.prototype.hasOwnProperty.call(obj, key)) continue

    const currentPath = path ? `${path}.${key}` : key
    const value = obj[key]

    if (typeof value === 'string') {
      // Check if it's a special marker
      if (value.startsWith('FUNCTION:')) {
        const functionCode = value.substring(9)
        manualCases.push(formatManualEntry(currentPath, functionCode))
      } else if (value.startsWith('ARRAY:')) {
        const arrayCode = value.substring(6)
        manualCases.push(formatManualEntry(currentPath, arrayCode))
      } else {
        // Simple string → CSV
        csvLines.push(`${escapeCsv(currentPath)};${escapeCsv(value)}`)
      }
    } else if (typeof value === 'object' && value !== null) {
      // Nested object → recurse
      processObject(value, currentPath, csvLines, manualCases)
    }
  }
}

function formatManualEntry(key: string, code: string): string {
  return `================================================================
KEY: ${key}
----------------------------------------------------------------

${code.trim()}


`
}

function escapeCsv(value: string): string {
  // Escape double quotes by doubling them and wrap in quotes if contains semicolon, quote, or newline
  if (
    value.includes(';') ||
    value.includes('"') ||
    value.includes('\n') ||
    value.includes('\r')
  ) {
    return `"${value.replace(/"/g, '""')}"`
  }
  return value
}

main()
