# Full App Audit — Bug Report

## CRITICAL — App Crashing Bugs

1. **snapshotQueue undefined** (main.js:1986, 2032)
   - `snapshotQueue.push(...)` in handleSaveInspection and handleConvertInspection
   - `snapshotQueue` was never declared/defined — will throw ReferenceError
   - Fix: Remove these lines (sync queue is handled by the store setState)

2. **navigateTo() wrong signature in handleConvertInspection** (main.js:2037)
   - `navigateTo('job-detail', job.id)` should be `navigateTo('job-detail', { selectedJobId: job.id })`
   - This will cause job detail page to show "No job selected"

## HIGH — Broken Navigation / UI

3. **Bottom nav active state map is wrong** (main.js:210)
   - Current: `{ dashboard: 0, jobs: 1, gps: 2, ai: 3 }`
   - Actual bottom nav: dashboard(0), jobs(1), inspections(2), schedule(3), gps(4)
   - Fix: Update map to match BOTTOM_NAV constant

4. **Missing page labels** (main.js:222)
   - No labels for: `inspections`, `inspection-form`, `schedule`
   - These show "Wildlife Whisperer" fallback instead of proper title

5. **Modal backdrop CSS missing**
   - GPS help modal and manual GPS modal use `.modal-backdrop`, `.modal-content` classes
   - These CSS classes don't exist in styles.css — modals will look broken

6. **Inspection status badge CSS missing**
   - `INSPECTION_STATUS_STYLES` maps to: `pending`, `scheduled`, `completed`, `cancelled`
   - Need to verify these CSS classes exist for status badges

## MEDIUM — Functional Issues

7. **handleSyncNow declared after it's referenced**
   - Used in SettingsPage (line 1719) but declared at line 2614
   - Works due to function hoisting, but `handleCaptureGPS` references it at line 2852
   - Actually this is fine because function declarations are hoisted
   - But at line 2985 it's called in init() which is also fine

8. **Bottom nav "Inspect" label truncated**
   - BOTTOM_NAV uses label "Inspect" which should be "Inspections" for clarity

## VERIFIED WORKING
- All handler functions exist (handleSyncNow, handleExportData, handleImportData, handleRecoverData, handleWipeData)
- Router routes are all registered correctly
- State has inspections[] and selectedInspectionId
- CSS has cal-has-jobs and cal-has-items styles
- InspectionList navigation fixed (points to inspection-form)
- GPS handler with help dialog and manual entry
