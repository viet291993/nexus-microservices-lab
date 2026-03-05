import { TypeScriptGenerator } from '@asyncapi/modelina';
import * as fs from 'fs';
import * as path from 'path';

const classValidatorPreset = {
  class: {
    property(args: any) {
      const property = args.property;
      const content = args.content;

      const isRequired = property.required;
      const type = property.type || property.property?.type;
      
      let decorators = [];
      if (!isRequired) {
        decorators.push('  @IsOptional()');
      } else {
        decorators.push('  @IsDefined()');
      }

      if (type === 'string') decorators.push('  @IsString()');
      if (type === 'number' || type === 'integer') decorators.push('  @IsNumber()');
      if (type === 'boolean') decorators.push('  @IsBoolean()');

      return `${decorators.join('\n')}\n${content}`;
    }
  }
};

const generator = new TypeScriptGenerator({
  modelType: 'class',
  presets: [
    classValidatorPreset as any
  ]
});

async function main() {
  const yamlPath = path.resolve(__dirname, '../../../shared/event-schemas/asyncapi.yaml');
  const yaml = fs.readFileSync(yamlPath, 'utf-8');
  
  const models = await generator.generate(yaml);
  
  const outputDir = path.resolve(__dirname, '../src/shared/events/models');
  if (!fs.existsSync(outputDir)) {
      fs.mkdirSync(outputDir, { recursive: true });
  }

  const importStatement = `import { IsOptional, IsDefined, IsString, IsNumber, IsBoolean } from 'class-validator';\n`;
  const allModelNames = models.map(m => m.modelName);

  for (const model of models) {
    let finalCode = model.result;
    
    // Nếu là file Class, tự động thêm import Class-Validator
    if (finalCode.includes('class ' + model.modelName)) {
      finalCode = importStatement + finalCode;
    }

    // Heuristic dependency detection: if this model's code contains the name of another model, add the import
    const dependencies = allModelNames.filter(name => name !== model.modelName && finalCode.includes(name));
    if (dependencies.length > 0) {
      const depImports = dependencies.map(d => `import ${d} from './${d}';`).join('\n');
      finalCode = `${depImports}\n${finalCode}`;
    }

    // Fix export
    if (!finalCode.includes('export default') && finalCode.includes('class ' + model.modelName)) {
       finalCode = finalCode.replace('class ' + model.modelName, 'export default class ' + model.modelName);
    }
    if (!finalCode.includes('export default') && finalCode.includes('enum ' + model.modelName)) {
       finalCode = finalCode.replace('export default enum', 'export enum'); // Revert if already added manually
       finalCode = finalCode.replace('enum ' + model.modelName, 'export enum ' + model.modelName);
       finalCode += `\nexport default ${model.modelName};\n`;
    } else if (finalCode.includes('enum ' + model.modelName) && !finalCode.includes('export enum')) {
       finalCode = finalCode.replace('enum ' + model.modelName, 'export enum ' + model.modelName);
       finalCode += `\nexport default ${model.modelName};\n`;
    }

    fs.writeFileSync(
      path.resolve(outputDir, `${model.modelName}.ts`),
      finalCode
    );
  }
  console.log("Custom Schema with class-validator decorators generated successfully!");
}

main().catch(console.error);
