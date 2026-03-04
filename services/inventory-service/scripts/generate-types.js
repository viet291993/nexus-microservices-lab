const fs = require('fs');
const path = require('path');
const yaml = require('yaml');

const asyncapiPath = path.join(__dirname, '../../../shared/event-schemas/asyncapi.yaml');
const outputPath = path.join(__dirname, '../src/inventory/schemas/events.ts');

try {
  const fileContent = fs.readFileSync(asyncapiPath, 'utf8');
  const parsed = yaml.parse(fileContent);
  const schemas = parsed.components.schemas;

  let tsContent = `/**\n * GENERATED CODE - DO NOT MODIFY BY HAND\n * This file was generated from shared/event-schemas/asyncapi.yaml\n */\n\n`;

  for (const [schemaName, schemaDef] of Object.entries(schemas)) {
    if (schemaDef.type === 'object') {
      tsContent += `export class ${schemaName} {\n`;
      const properties = schemaDef.properties || {};
      for (const [propName, propDef] of Object.entries(properties)) {
        let tsType = 'any';
        if (propDef.type === 'string') {
          if (propDef.enum) {
            tsType = propDef.enum.map(e => `'${e}'`).join(' | ');
          } else {
            tsType = 'string';
          }
        } else if (propDef.type === 'integer' || propDef.type === 'number') {
          tsType = 'number';
        }

        const isRequired = (schemaDef.required || []).includes(propName);
        const optionalFlag = isRequired ? '!' : '?';
        
        // Add description as TSDoc if exists
        if (propDef.description) {
          tsContent += `  /**\n   * ${propDef.description}\n   */\n`;
        }
        
        tsContent += `  ${propName}${optionalFlag}: ${tsType};\n`;
      }
      tsContent += `}\n\n`;
    }
  }

  fs.writeFileSync(outputPath, tsContent, 'utf8');
  console.log(`✅ TypeScript interfaces successfully generated at: ${outputPath}`);
} catch (error) {
  console.error('❌ Failed to generate TypeScript interfaces:', error);
  process.exit(1);
}
