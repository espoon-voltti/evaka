#!/usr/bin/env node

/**
 * Extract all leaf nodes from fi.tsx with their values
 * Creates a CSV file ready for translation
 * 
 * Usage: node extract-for-translation.js > translations.csv
 */

const fs = require('fs');

function extractLeafNodes() {
  const content = fs.readFileSync('frontend/src/lib-customizations/defaults/employee/i18n/fi.tsx', 'utf8');
  
  // Find the fi object start
  const startIdx = content.indexOf('export const fi = {');
  if (startIdx === -1) {
    console.error('Could not find export const fi');
    process.exit(1);
  }
  
  const lines = content.substring(startIdx).split('\n');
  const results = [];
  const keyStack = [];
  let braceCount = 0;
  
  for (let line of lines) {
    const trimmed = line.trim();
    
    // Skip comments, empty lines, imports
    if (!trimmed || trimmed.startsWith('//') || trimmed.startsWith('/*') || trimmed.startsWith('*')) continue;
    
    // Count braces to track nesting
    const openBraces = (line.match(/{/g) || []).length;
    const closeBraces = (line.match(/}/g) || []).length;
    
    // Match key: value patterns
    // Handle: key: 'value', 'key': 'value', key: "value"
    const stringMatch = trimmed.match(/^(['"]?)([a-zA-Z_][a-zA-Z0-9_-]*)(\1)\s*:\s*(['"`])((?:[^'"`\\]|\\.)*)(\4)/);
    
    if (stringMatch) {
      const key = stringMatch[2];
      const value = stringMatch[5];
      
      // It's a leaf node with a string value
      const fullPath = keyStack.length > 0 
        ? keyStack.join('.') + '.' + key 
        : key;
      
      // Unescape the value
      const unescapedValue = value
        .replace(/\\'/g, "'")
        .replace(/\\"/g, '"')
        .replace(/\\n/g, ' ')
        .replace(/\\\\/g, '\\');
      
      results.push({ path: fullPath, value: unescapedValue });
      
    } else {
      // Check if it's opening a nested object
      const keyMatch = trimmed.match(/^(['"]?)([a-zA-Z_][a-zA-Z0-9_-]*)(\1)\s*:\s*{/);
      if (keyMatch) {
        const key = keyMatch[2];
        keyStack.push(key);
        braceCount += openBraces - closeBraces;
      } else {
        // Check for arrow functions - these are leaf nodes too
        const funcMatch = trimmed.match(/^(['"]?)([a-zA-Z_][a-zA-Z0-9_-]*)(\1)\s*:\s*\(/);
        if (funcMatch) {
          const key = funcMatch[2];
          const fullPath = keyStack.length > 0 
            ? keyStack.join('.') + '.' + key 
            : key;
          results.push({ path: fullPath, value: '[FUNCTION]' });
        } else {
          // Just closing braces
          if (closeBraces > openBraces) {
            const braceDiff = closeBraces - openBraces;
            for (let i = 0; i < braceDiff && keyStack.length > 0; i++) {
              keyStack.pop();
              braceCount--;
            }
          }
        }
      }
    }
  }
  
  return results;
}

function escapeCSV(value) {
  // Escape double quotes and wrap in quotes if contains comma, quote, or newline
  if (value.includes('"') || value.includes(',') || value.includes('\n')) {
    return '"' + value.replace(/"/g, '""') + '"';
  }
  return value;
}

function main() {
  console.error('Extracting leaf nodes from fi.tsx...\n');
  
  const leafNodes = extractLeafNodes();
  
  console.error(`Found ${leafNodes.length} leaf nodes\n`);
  console.error('Generating CSV...\n');
  
  // Output CSV header
  console.log('path,fi,sv');
  
  // Output each leaf node
  for (const node of leafNodes) {
    const path = escapeCSV(node.path);
    const fi = escapeCSV(node.value);
    const sv = ''; // Empty for translator to fill
    
    console.log(`${path},${fi},${sv}`);
  }
  
  console.error('Done! CSV output to stdout');
  console.error(`Total rows: ${leafNodes.length}`);
}

main();
