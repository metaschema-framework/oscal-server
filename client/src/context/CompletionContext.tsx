import { create } from 'zustand';
import { useOscal} from './OscalContext';

interface CompletionState {
  stepCompletion: Record<string, number>;
  calculateCompletions: () => void;
}

export const useCompletion = create<CompletionState>((set, get) => ({
  stepCompletion: {},

  calculateCompletions: () => {}
}));