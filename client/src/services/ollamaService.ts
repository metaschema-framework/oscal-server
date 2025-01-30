import { useState, useEffect } from 'react';

// Types for Ollama API responses
interface OllamaResponse {
  response: string;
  context?: number[];
  done: boolean;
}

interface ModelStatus {
  name: string;
  status: 'downloading' | 'ready' | 'error';
  progress?: number;
}

// Constants
const GRANITE_MODEL = 'ibm-granite';
const OLLAMA_BASE_URL = 'http://localhost:11434'; // Default Ollama API port

/**
 * Check if the Ollama service is available
 */
async function checkOllamaService(): Promise<boolean> {
  try {
    const response = await fetch(`${OLLAMA_BASE_URL}/api/tags`);
    return response.ok;
  } catch (error) {
    // console.error('Ollama service check failed:', error);
    return false;
  }
}

/**
 * Check if the IBM Granite model is available locally
 */
async function isModelAvailable(): Promise<boolean> {
  try {
    const response = await fetch(`${OLLAMA_BASE_URL}/api/tags`);
    const data = await response.json();
    return data.models?.some((model: any) => model.name === GRANITE_MODEL) ?? false;
  } catch (error) {
    console.error('Model availability check failed:', error);
    return false;
  }
}

/**
 * Download the IBM Granite model if not already present
 */
export async function downloadGraniteModelIfNeeded(): Promise<ModelStatus> {
  if (await isModelAvailable()) {
    return { name: GRANITE_MODEL, status: 'ready' };
  }

  try {
    const response = await fetch(`${OLLAMA_BASE_URL}/api/pull`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ name: GRANITE_MODEL }),
    });

    if (!response.ok) {
      throw new Error(`Failed to download model: ${response.statusText}`);
    }

    return { name: GRANITE_MODEL, status: 'downloading', progress: 0 };
  } catch (error) {
    console.error('Model download failed:', error);
    return { name: GRANITE_MODEL, status: 'error' };
  }
}

/**
 * Generate a response using the IBM Granite model
 */
export async function generateOllamaResponse(
  prompt: string,
  options: { temperature?: number; maxTokens?: number } = {}
): Promise<string> {
  const serviceAvailable = await checkOllamaService();
  if (!serviceAvailable) {
    return 'The Ollama service is currently unavailable. Please try again later.';
  }

  const modelAvailable = await isModelAvailable();
  if (!modelAvailable) {
    return 'The IBM Granite model is not available. Please ensure it is downloaded first.';
  }

  try {
    const response = await fetch(`${OLLAMA_BASE_URL}/api/generate`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        model: GRANITE_MODEL,
        prompt,
        temperature: options.temperature ?? 0.7,
        max_tokens: options.maxTokens ?? 500,
        stream: false,
      }),
    });

    if (!response.ok) {
      throw new Error(`Generation failed: ${response.statusText}`);
    }

    const data: OllamaResponse = await response.json();
    return data.response;
  } catch (error) {
    console.error('Response generation failed:', error);
    return 'Failed to generate response. Please try again.';
  }
}

/**
 * React hook for managing Ollama model status
 */
export function useOllamaStatus() {
  const [status, setStatus] = useState<ModelStatus>({ 
    name: GRANITE_MODEL, 
    status: 'downloading' 
  });

  useEffect(() => {
    const checkStatus = async () => {
      const modelAvailable = await isModelAvailable();
      setStatus({ 
        name: GRANITE_MODEL, 
        status: modelAvailable ? 'ready' : 'error' 
      });
    };

    checkStatus();
  }, []);

  return status;
}
