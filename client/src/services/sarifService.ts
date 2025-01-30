import { OscalPackage } from '../types';

interface SarifResult {
  ruleId: string;
  message: {
    text: string;
  };
}

interface SarifRun {
  results?: SarifResult[];
}

interface SarifLog {
  runs: SarifRun[];
}

export function parseSarif(sarifJson: SarifLog): string[] {
  try {
    return sarifJson.runs[0]?.results?.map(
      (result: SarifResult) => `${result.ruleId}: ${result.message.text}`
    ) || [];
  } catch (error) {
    console.error('SARIF parsing error:', error);
    return ['Unable to parse validation results'];
  }
}

export async function checkValidationService(): Promise<boolean> {
  try {
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 5000);

    try {
      const response = await fetch('/api/validate', {
        method: 'HEAD',
        signal: controller.signal
      });
      clearTimeout(timeoutId);
      return response.ok;
    } catch (error) {
      if (error instanceof Error && error.name === 'AbortError') {
        console.warn('Validation service check timed out');
      }
      throw error;
    }
  } catch (error) {
    console.warn('Validation service not available:', error);
    return false;
  }
}

export async function validateDocument(document: OscalPackage): Promise<string[]> {
  try {
    const response = await fetch('/api/validate', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(document)
    });
    
    if (!response.ok) {
      throw new Error(`Validation service returned ${response.status}`);
    }
    
    const sarifData = await response.json() as SarifLog;
    return parseSarif(sarifData);
  } catch (error) {
    console.error('Document validation failed:', error);
    return ['Validation service is currently unavailable'];
  }
}
