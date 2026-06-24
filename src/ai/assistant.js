// Example integration file - import and use the modules above
// In your Inspect / AI screen component:

import { startDictation, stopDictation } from './dictation.js';
import { getSuggestionsFromLLM } from './llm.js';

// Example usage in button handlers:
// Dictate button:
// onClick: () => startDictation( (text) => { observations.value = text; }, (err) => showToast(err) )

// Get Suggestions button:
// onClick: async () => {
//   suggestions.value = 'Contacting real LLM...';
//   const result = await getSuggestionsFromLLM(species.value, season.value, observations.value);
//   suggestions.value = result;
// }

// Note: Wire these into your existing state management (src/state.js or your framework)
// Season will now be dynamic from your picker state.
