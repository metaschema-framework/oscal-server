import Ajv, { ValidateFunction } from 'ajv';
import addFormats from "ajv-formats";
import { create } from 'zustand';
import oscalSchema from '../schema.json';
import { StorageService } from '../services/storage';
import { EntityType, OscalPackage, RootElementType, RootElementTypes, SecurityImpactLevel, SingleElementType } from '../types';
import { PackageId } from 'typescript';

let ajv: Ajv | null = null;

function getAjv(): Ajv {
  if (!ajv) {
    ajv = new Ajv({
      allErrors: true,
      verbose: true,
      schemas:[oscalSchema]
    });
    addFormats(ajv);
  }
  return ajv;
}

// Cache for validation functions
const validatorCache = new Map<string, ValidateFunction>();

type OscalEntities = {
  [K in EntityType]: Record<string, any>
}

interface OscalState {
  packageId: string;
  documentId?: string;
  insert: (type: EntityType, entity: any) => Promise<void>;
  saveDocument: (file:File) => Promise<void>;
  read: (type: EntityType, id: string) => Promise<any>;
  all: (type: EntityType) => Promise<Record<string, any>>;
  packages: () => Promise<string[]>;
  documents: () => Promise<string[]>;
  destroy: (type: EntityType, id: string) => Promise<void>;
  validateEntity: (type: EntityType, entity: any) => boolean;
  setPackage: (packageId: string) => boolean;
  setDocumentId: (documentId: string) => boolean;
}

export const useOscal = create<OscalState>((set, get) => {
  function getValidator(type: EntityType): ValidateFunction {
    if (!validatorCache.has(type)) {
      const ajv = getAjv();
      const validate = ajv.getSchema(`#/definitions/${type}`);
      if (!validate) {
        throw new Error(`No validator found for type: ${type}`);
      }
      validatorCache.set(type, validate);
    }
    return validatorCache.get(type)!;
  }

  return {
    packageId: 'workspace',
    documentId: 'ssp.json',
    setPackage: (packageId: string) => {
      set((state) => ({ ...state, packageId }));
      return true;
    },
    setDocumentId: (documentId: string) => {
      set((state) => ({ ...state, documentId }));
      return true;
    },

    insert: async (type: EntityType, entity: any) => {
      const documentId=get().documentId;
      if(!documentId){
        throw("needs a document context");
      }
      try {
        if (RootElementTypes.includes(type)) {
          throw("Use SaveDocument");
        } else {
          await StorageService.save(get().packageId,documentId, type, entity);
        }
      } catch (error) {
        console.error('Failed to create entity:', error);
        throw error;
      }
    },

    saveDocument: async (file: File) => {
        await StorageService.saveDocument(get().packageId,file);
    },

    read: async (type: EntityType, id: string) => {
      const documentId=get().documentId;
      if(!documentId){
        throw("needs a document context");
      }
      return await StorageService.get(get().packageId,documentId, type, id);
    },

    all: async (type: EntityType) => {
      const documentId=get().documentId;
      if(!documentId){
        throw("needs a document context");
      }
      return await StorageService.all(get().packageId,documentId, type);
    },

    packages: async () => {
      return await StorageService.packages();
    },

    documents: async () => {
      return await StorageService.documents(get().packageId);
    },

    destroy: async (type: EntityType, id: string) => {
      const documentId=get().documentId;
      if(!documentId){
        throw("needs a document context");
      }
      try {
        await StorageService.delete(get().packageId,documentId, type, id);
      } catch (error) {
        console.error('Failed to delete entity:', error);
        throw error;
      }
    },

    validateEntity: (type: EntityType, entity: any): boolean => {
      try {
        const validate = getValidator(type);
        const valid = validate(entity);
        if (!valid) {
          console.error(`Validation errors for ${type}:`, validate.errors);
        }
        return valid;
      } catch (error) {
        console.error('Validation failed:', error);
        return false;
      }
    },

    validateDefinition: (type: EntityType, schema: any) => {
      try {
        const ajv = getAjv();
        ajv.addSchema(schema, `#/definitions/${type}`);
        validatorCache.delete(type);
      } catch (error) {
        console.error('Failed to validate definition:', error);
        throw error;
      }
    }
  };
});
