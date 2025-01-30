import { EntityType, RootElementType, RootElementTypes } from "../types";
import { ApiService } from "./api";

export const StorageService = {
  getEntityId: (packageId: string, documentId:string, type: string, entity: any): string => {
    if (RootElementTypes.includes(type)) {
      return `${packageId}/${documentId}/${type}/${(Object.values(entity)[0] as any).uuid}`;
    } else {
      return `${packageId}/${documentId}/${type}/${entity.uuid || entity.id}`;
    }
  },

  get: async (packageId: string, documentId:string,type: EntityType, id: string): Promise<any> => {
    return await ApiService.getPackageFile(packageId, documentId);
  },

  all: async (packageId: string,documentId:string, type: EntityType): Promise<Record<string, any>> => {
    return await ApiService.getPackageFile(packageId, documentId);
  },

  documents: async (packageId: string): Promise<string[]> => {
    const files = await ApiService.listPackageFiles(packageId);
    return files.map(file => file.name);
  },

  packages: async (): Promise<string[]> => {
    // This would need to be implemented on the server side
    // For now, return an empty array or handle it differently
    return [];
  },

  save: async (packageId: string, documentId:string,type: EntityType, entity: any): Promise<void> => {
    const file = new File([JSON.stringify(entity, null, 2)], documentId, {
      type: 'application/json',
    });
    await ApiService.uploadPackageFile(packageId, file);
  },

  delete: async (packageId: string,documentId:string, type: EntityType, id: string): Promise<void> => {
    await ApiService.deletePackageFile(packageId, documentId);
  },

  saveDocument: async (packageId: string,file:File): Promise<void> => {
    await ApiService.uploadPackageFile(packageId, file);
  },

  clearStorage: async (packageId: string): Promise<void> => {
    const files = await ApiService.listPackageFiles(packageId);
    for (const file of files) {
      await ApiService.deletePackageFile(packageId, file.name);
    }
  }
};
