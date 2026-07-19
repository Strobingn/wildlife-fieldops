// tools/patch-main-js-ai.js
const fs = require("fs");
const path = require("path");

const file = path.join(process.cwd(), "src", "main.js");

if (!fs.existsSync(file)) {
  console.error("Could not find src/main.js. Run this from the repo root.");
  process.exit(1);
}

let src = fs.readFileSync(file, "utf8");

if (!src.includes('import { runFieldAI, formatFieldAIResult } from "./ai/fieldAssistant.js";')) {
  src = src.replace(
    'import { supabase } from "./auth/supabaseClient.js";',
    'import { supabase } from "./auth/supabaseClient.js";\nimport { runFieldAI, formatFieldAIResult } from "./ai/fieldAssistant.js";',
  );
}

const newAiPage = `function aiPage() { shell(\`
## Kimi AI Field Assistant

<div class="card">
  <label>AI Mode</label>
  <select id="aiMode">
    <option value="field_plan">Field Plan</option>
    <option value="job_notes">Job Notes</option>
    <option value="estimate">Estimate Guidance</option>
    <option value="customer_message">Customer Message</option>
    <option value="invoice_notes">Invoice Notes</option>
    <option value="risk_check">Risk / Safety Check</option>
  </select>

  <label>Species</label>
  <select id="aiSpecies">
    \${SPECIES.map(s => \`<option>\${s}</option>\`).join("")}
  </select>

  <label>Field Observations</label>
  <textarea id="aiObs" rows="8" placeholder="Example: raccoon entry near soffit, attic droppings, customer hears noise at night..."></textarea>

  <div class="row">
    <button onclick="aiSuggest()">Generate Kimi AI Plan</button>
    <button onclick="dictate(aiObs)">🎙️ Dictate</button>
  </div>
</div>

<div class="card">
  <label>Kimi AI Output</label>
  <textarea id="aiOut" rows="18" placeholder="AI output appears here..."></textarea>
</div>
\`); }`;

const newAiSuggest = `window.aiSuggest = async function () {
  const mode = document.getElementById("aiMode")?.value || "field_plan";
  const species = document.getElementById("aiSpecies")?.value || selectedJob?.species || "";
  const observation = document.getElementById("aiObs")?.value || selectedJob?.notes || "";

  const jobServices = selectedJob ? services.filter(s => s.job_id === selectedJob.id) : [];

  showLoading("Running Kimi AI assistant…");

  try {
    const result = await runFieldAI(supabase, {
      mode,
      species,
      observation,
      job: selectedJob || { species, notes: observation },
      services: jobServices,
      businessContext: "Wildlife Whisperer nuisance wildlife removal field app. Prioritize inspection notes, exclusion planning, estimate clarity, customer communication, safety reminders, and professional documentation."
    });

    const formatted = formatFieldAIResult(result);
    const out = document.getElementById("aiOut");
    if (out) out.value = formatted;

    if (selectedJob?.id) {
      await supabase.from("jobs").update({
        ai_notes: formatted,
        ai_customer_message: result.customer_message || null,
        ai_invoice_notes: result.invoice_notes || null,
        ai_last_run_at: new Date().toISOString()
      }).eq("id", selectedJob.id);

      await supabase.from("ai_runs").insert({
        job_id: selectedJob.id,
        mode,
        input: { species, observation, services: jobServices },
        output: result,
        provider: "kimi_moonshot"
      });
    }

    showToast("Kimi AI assistant complete");
  } catch (err) {
    console.error(err);
    showToast(err.message || "Kimi AI assistant failed", "error", 6000);
  } finally {
    hideLoading();
  }
};`;

const aiPageRegex = /function aiPage\(\) \{ shell\(`[\s\S]*?`\); \}/;
if (aiPageRegex.test(src)) {
  src = src.replace(aiPageRegex, newAiPage);
} else {
  console.warn("Could not find aiPage() block. Add the AI page manually.");
}

const aiSuggestRegex = /window\.aiSuggest = (?:async )?function \(\) \{[\s\S]*?\}; window\.saveGps/;
if (aiSuggestRegex.test(src)) {
  src = src.replace(aiSuggestRegex, newAiSuggest + " window.saveGps");
} else {
  console.warn("Could not find old window.aiSuggest block. Add aiSuggest manually.");
}

fs.writeFileSync(file, src);
console.log("Patched src/main.js with Kimi AI assistant integration.");
