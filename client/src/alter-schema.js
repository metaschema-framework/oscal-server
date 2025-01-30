import fs from 'fs';

function generateUniqueDefinitionName(baseName, definitions) {
  let counter = 1;
  let name = baseName;

  while (definitions[name]) {
    name = `${baseName}${counter}`;
    counter++;
  }

  return name;
}

function flattenSchema(schema) {
  const definitions = { ...schema.definitions };
  const processedDefs = new Set();

  function processSchema(schema, path) {
    const stack = [{ schema, path, visited: new Set() }];

    while (stack.length > 0) {
      const { schema, path, visited } = stack.pop();

      if (
        !schema ||
        typeof schema !== 'object' ||
        processedDefs.has(schema) ||
        visited.has(schema)
      ) {
        continue;
      }

      visited.add(schema);
      processedDefs.add(schema);

      if (schema.$ref) {
        const refName = schema.$ref.split('/').pop();
        if (definitions[refName] && !processedDefs.has(definitions[refName])) {
          stack.push({ schema: definitions[refName], path: `definitions/${refName}`, visited });
        }
        continue;
      }

      if (schema.type === 'object') {
        const defName = generateUniqueDefinitionName(path.split('/').pop().replace(/[^a-zA-Z0-9]/g, '_'), definitions);
        definitions[defName] = { ...schema }; // Create a copy of the schema
        schema.$ref = `#/definitions/${defName}`;
        delete schema.type;
        delete schema.properties;
        continue;
      }

      if (schema.type === 'array' && schema.items) {
        stack.push({ schema: schema.items, path: `${path}/items`, visited });
        continue;
      }

      if (schema.properties) {
        for (const [key, value] of Object.entries(schema.properties)) {
          stack.push({ schema: value, path: `${path}/properties/${key}`, visited });
        }
      }

      ['anyOf', 'allOf', 'oneOf'].forEach((key) => {
        if (schema[key]) {
          schema[key].forEach((item, index) => {
            stack.push({ schema: item, path: `${path}/${key}/${index}`, visited });
          });
        }
      });
    }

    return schema;
  }

  // Process the main schema
  const processedSchema = processSchema(schema, '/');

  // Process all definitions
  for (const [key, value] of Object.entries(definitions)) {
    definitions[key] = processSchema(value, `/definitions/${key}`);
  }

  // Construct the flattened schema
  const flattenedSchema = {
    ...processedSchema,
    definitions,
  };

  return flattenedSchema;
}

const schema = JSON.parse(fs.readFileSync('./schema.json', 'utf8'));
const flattenedSchema = flattenSchema(schema);
fs.writeFileSync('output2.json', JSON.stringify(flattenedSchema, null, 2));
