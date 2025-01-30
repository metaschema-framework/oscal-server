interface PackageFile {
  name: string;
  size: number;
  lastModified: string;
  mimeType: string;
}

interface ConversionService {
  convertFile: (file: File, format: string) => Promise<any>;
  exportFile: (data: any, format: string) => Promise<Blob>;
}

export const ConversionService: ConversionService = {
  convertFile: async (file: File, format: string): Promise<any> => {
    const content = await file.text();
    const contentType = getContentTypeFromFile(file);

    const response = await fetch(`/api/convert?format=${format}`, {
      method: 'POST',
      headers: {
        'Content-Type': contentType,
      },
      body: content,
    });

    if (!response.ok) {
      const errorText = await response.text();
      throw new Error(`Failed to convert file: ${errorText}`);
    }

    return response.text();
  },

  exportFile: async (data: any, format: string): Promise<Blob> => {
    const content = JSON.stringify(data);

    const response = await fetch(`/api/convert?format=${format}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: content,
    });

    if (!response.ok) {
      throw new Error('Failed to convert file');
    }

    return response.blob();
  },
};

interface PackageFile {
  name: string;
  size: number;
  lastModified: string;
  mimeType: string;
}

export const ApiService = {
  // List all files in a package
  listPackageFiles: async (packageId: string): Promise<PackageFile[]> => {
    const response = await fetch(`/api/packages/${packageId}/files`);
    if (!response.ok) {
      throw new Error(`Failed to list package files: ${response.statusText}`);
    }
    return response.json();
  },

  // Get a specific file from a package
  getPackageFile: async (packageId: string, filename: string): Promise<any> => {
    const response = await fetch(`/api/packages/${packageId}/files/${filename}`);
    if (!response.ok) {
      throw new Error(`Failed to get package file: ${response.statusText}`);
    }
    return response.text();
  },

  // Upload a file to a package
  uploadPackageFile: async (packageId: string, file: File): Promise<PackageFile> => {
    const content = await file.text();
    const contentType = getContentTypeFromFile(file);

    const response = await fetch(`/api/packages/${packageId}/files/${file.name}`, {
      method: 'PUT',
      headers: {
        'Content-Type': contentType,
      },
      body: content,
    });

    if (!response.ok) {
      throw new Error(`Failed to upload file: ${response.statusText}`);
    }
    return response.json();
  },

  // Update a file in a package
  updatePackageFile: async (packageId: string, filename: string, file: File): Promise<PackageFile> => {
    const content = await file.text();
    const contentType = getContentTypeFromFile(file);

    const response = await fetch(`/api/packages/${packageId}/files/${filename}`, {
      method: 'PUT',
      headers: {
        'Content-Type': contentType,
      },
      body: content,
    });

    if (!response.ok) {
      throw new Error(`Failed to update file: ${response.statusText}`);
    }
    return response.json();
  },

  // Delete a file from a package
  deletePackageFile: async (packageId: string, filename: string): Promise<void> => {
    const response = await fetch(`/api/packages/${packageId}/files/${filename}`, {
      method: 'DELETE',
    });

    if (!response.ok) {
      throw new Error(`Failed to delete file: ${response.statusText}`);
    }
  },
};

function getContentTypeFromFile(file: File): string {
  const extension = file.name.split('.').pop()?.toLowerCase();
  switch (extension) {
    case 'json':
      return 'application/json';
    case 'yaml':
    case 'yml':
      return 'text/yaml';
    case 'xml':
      return 'text/xml';
    default:
      return file.type || 'application/octet-stream';
  }
}
