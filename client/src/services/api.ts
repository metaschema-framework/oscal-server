interface PackageFile {
  name: string;
  size: number;
  lastModified: string;
  mimeType: string;
}

interface ConversionService {
  convertFile: (file: File, format: string) => Promise<any>;
  exportFile: (data: any, format: string) => Promise<Blob>;
  convertPackageDocument:(packageId:string,documentId:string,format:string)=>Promise<string>;
}

export const ConversionService: ConversionService = {

  convertPackageDocument: async (packageId: string, documentId: string,format:string): Promise<any> => {
    const response = await fetch(`/api/convert?document=file://~/.oscal/packages/${packageId}/${documentId}&format=${format}`, {
      method: 'GET',
    });

    if (!response.ok) {
      const errorText = await response.text();
      throw new Error(`Validation failed: ${errorText}`);
    }

    return response.text();
  },
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
  // Validate an OSCAL document
  validateDocument: async (content: string, format: string): Promise<any> => {
    const response = await fetch(`/api/validate?format=${format}`, {
      method: 'POST',
      headers: {
        'Content-Type': getContentTypeFromFormat(format),
      },
      body: content,
    });

    if (!response.ok) {
      const errorText = await response.text();
      throw new Error(`Validation failed: ${errorText}`);
    }

    return response.json();
  },

  validatePackageDocument: async (packageId: string, documentId: string): Promise<any> => {
    const response = await fetch(`/api/validate?document=file://~/.oscal/packages/${packageId}/${documentId}`, {
      method: 'GET',
    });

    if (!response.ok) {
      const errorText = await response.text();
      throw new Error(`Validation failed: ${errorText}`);
    }

    return response.json();
  },

  // Resolve a profile
  resolveProfile: async (content: string, format: string): Promise<string> => {
    const response = await fetch(`/api/resolve-profile?format=${format}`, {
      method: 'POST',
      headers: {
        'Content-Type': getContentTypeFromFormat(format),
      },
      body: content,
    });

    if (!response.ok) {
      const errorText = await response.text();
      throw new Error(`Profile resolution failed: ${errorText}`);
    }

    return response.text();
  },

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

  // Execute a metapath query on a document
  queryDocument: async (content: string, expression: string, format: string, module: string = "https://raw.githubusercontent.com/usnistgov/OSCAL/refs/heads/main/src/metaschema/oscal_complete_metaschema.xml"): Promise<string> => {
    const response = await fetch(`/api/query?expression=${encodeURIComponent(expression)}&module=${encodeURIComponent(module)}`, {
      method: 'POST',
      headers: {
        'Content-Type': getContentTypeFromFormat(format),
      },
      body: content,
    });

    if (!response.ok) {
      const errorText = await response.text();
      throw new Error(`Metapath query failed: ${errorText}`);
    }

    return response.text();
  },

  // Execute a metapath query on a package document
  queryPackageDocument: async (packageId: string, documentId: string, expression: string, module: string = "https://raw.githubusercontent.com/usnistgov/OSCAL/refs/heads/main/src/metaschema/oscal_complete_metaschema.xml"): Promise<string> => {
    const response = await fetch(`/api/query?document=file://~/.oscal/packages/${packageId}/${documentId}&expression=${encodeURIComponent(expression)}&module=${encodeURIComponent(module)}`, {
      method: 'GET',
    });

    if (!response.ok) {
      const errorText = await response.text();
      throw new Error(`Metapath query failed: ${errorText}`);
    }

    return response.text();
  },
};

function getContentTypeFromFormat(format: string): string {
  switch (format.toLowerCase()) {
    case 'json':
      return 'application/json';
    case 'yaml':
    case 'yml':
      return 'text/yaml';
    case 'xml':
      return 'text/xml';
    default:
      return 'application/octet-stream';
  }
}

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
