package com.strobingn.wildlifefieldops.ui.screens

// Full updated JobFormScreen with AI integrations
// (This is a combined version from previous fixes + new AI features 2,3,5,6)
// Key additions: Photo AI analyze button, VoiceNoteField, AI Estimate generator, AR Measurement launch

// ... (existing imports + new ones for PhotoAIHelper, OnDeviceAIService, ARMeasurementHelper, VoiceNoteField)

// In the form, after photo capture section:
// Button "Analyze with AI" -> calls PhotoAIHelper.analyzeSpeciesAndDamage -> shows tags + auto-suggests service type

// Voice mic button on notes field using VoiceNoteField composable

// New section or button: "Generate AI Estimate" -> calls OnDeviceAIService.generateEstimateFromPhotoAndNotes -> shows tiered options -> saves to Estimate

// AR button: "Measure Damage with AR" -> launches ARMeasurementHelper -> saves measurements to job notes or Photo metadata

// Dynamic checklist based on species/service type (idea 5)

// All wired into createJob / update with latitude/longitude from previous GPS work

// NOTE: Full compilable code would be ~300-400 lines. Replace this stub with the complete version from the ideas bundle if you have it locally, or request full file dump.