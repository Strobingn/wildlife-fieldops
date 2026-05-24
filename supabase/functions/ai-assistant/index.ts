// supabase/functions/ai-assistant/index.ts
// Wildlife Whisperer FieldOps AI Assistant — Gemini only
// Add GEMINI_API_KEY to Supabase Edge Function Secrets

type AiMode =
  | "field_plan"
  | "job_notes"
  | "estimate"
  | "customer_message"
  | "invoice_notes"
  | "risk_check";

type AiRequest = {
  mode?: AiMode;
  job?: Record<string, unknown>;
  observation?: string;
  species?: string;
  services?: Array<Record<string, unknown>>;
  inspections?: Array<Record<string, unknown>>;
  businessContext?: string;
};

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

function jsonResponse(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: {
      ...corsHeaders,
      "Content-Type": "application/json",
    },
  });
}

function safeString(value: unknown, max = 6000) {
  return String(value ?? "").slice(0, max);
}

function buildGeminiPrompt(payload: AiRequest) {
  const mode = payload.mode || "field_plan";

  return [
    "You are the Wildlife Whisperer FieldOps AI assistant.",
    "You help a nuisance wildlife removal technician produce practical field notes, estimate guidance, customer messages, and invoice notes.",
    "Return ONLY valid JSON. Do not wrap it in markdown. Do not add extra commentary.",
    "",
    `Requested mode: ${mode}`,
    `Species: ${payload.species || payload.job?.species || ""}`,
    `Observation: ${safeString(payload.observation || payload.job?.notes || "")}`,
    `Business context: ${payload.businessContext || "Small nuisance wildlife removal company."}`,
    "",
    "Return JSON with this shape:",
    '{"summary":"string","recommended_next_steps":["string"],"estimate_guidance":{"suggested_line_items":[{"service":"string","qty":number,"unit_price":number,"rationale":"string"}],"subtotal_low":number,"subtotal_high":number,"pricing_notes":"string"},"customer_message":"string","invoice_notes":"string","safety_flags":["string"],"legal_or_permit_reminders":["string"],"confidence":"low|medium|high"}',
  ].join("\n");
}

function extractJson(text: string) {
  const cleaned = text
    .trim()
    .replace(/^```json\s*/i, "")
    .replace(/^```\s*/i, "")
    .replace(/```$/i, "")
    .trim();

  try {
    return JSON.parse(cleaned);
  } catch {
    const start = cleaned.indexOf("{");
    const end = cleaned.lastIndexOf("}");
    if (start >= 0 && end > start) {
      return JSON.parse(cleaned.slice(start, end + 1));
    }
    throw new Error("AI returned text that was not valid JSON.");
  }
}

function getDemoResult(payload: AiRequest) {
  const species = payload.species || payload.job?.species || "Wildlife";
  const observation = payload.observation || payload.job?.notes || "";

  return {
    mode: payload.mode || "field_plan",
    summary: `Demo mode: ${species} inspection noted. ${observation.slice(0, 60)}...`,
    recommended_next_steps: [
      "Photograph all entry points and damage.",
      "Write detailed inspection notes before pricing.",
      "Check for secondary access points.",
      "Document warranty boundaries with customer.",
      "Schedule follow-up within 48 hours.",
    ],
    estimate_guidance: {
      suggested_line_items: [
        { service: "Inspection", qty: 1, unit_price: 125, rationale: "Required for all jobs" },
        { service: "Exclusion repair", qty: 1, unit_price: 150, rationale: "Seal entry points" },
      ],
      subtotal_low: 275,
      subtotal_high: 450,
      pricing_notes: "Demo estimate. Add a real GEMINI_API_KEY for live AI.",
    },
    customer_message: `Hi, we inspected your property for ${species} activity. We found evidence and recommend exclusion work. We'll send a detailed estimate shortly.`,
    invoice_notes: `Demo invoice notes. Inspection and exclusion work for ${species}.`,
    safety_flags: ["Wear respirator when handling droppings.", "Check for electrical hazards in attic."],
    legal_or_permit_reminders: ["Verify local wildlife regulations.", "Bat exclusions may have seasonal restrictions."],
    confidence: "medium",
  };
}

async function callGemini(payload: AiRequest) {
  const apiKey = Deno.env.get("GEMINI_API_KEY");
  if (!apiKey) {
    console.log("No GEMINI_API_KEY found. Returning demo response.");
    return getDemoResult(payload);
  }

  const model = Deno.env.get("GEMINI_MODEL") || "gemini-1.5-flash";
  const url = `https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent?key=${apiKey}`;

  const res = await fetch(url, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      contents: [{ parts: [{ text: buildGeminiPrompt(payload) }] }],
      generationConfig: {
        temperature: 0.2,
        maxOutputTokens: 1600,
        responseMimeType: "application/json",
      },
    }),
  });

  const raw = await res.text();

  if (!res.ok) {
    throw new Error(`Gemini error ${res.status}: ${raw.slice(0, 1000)}`);
  }

  const data = JSON.parse(raw);
  const text = data?.candidates?.[0]?.content?.parts?.[0]?.text;

  if (!text) {
    throw new Error("Gemini returned no text content.");
  }

  return extractJson(text);
}

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  if (req.method !== "POST") {
    return jsonResponse({ error: "Method not allowed. Use POST." }, 405);
  }

  try {
    const payload = (await req.json()) as AiRequest;

    if (!payload.mode) payload.mode = "field_plan";
    if (!payload.job && !payload.observation && !payload.species) {
      return jsonResponse(
        { error: "Send at least one of: job, observation, or species." },
        400,
      );
    }

    const result = await callGemini(payload);
    const hasKey = !!Deno.env.get("GEMINI_API_KEY");

    return jsonResponse({
      ok: true,
      provider: hasKey ? "gemini" : "demo",
      result,
    });
  } catch (err) {
    const message = err instanceof Error ? err.message : String(err);
    console.error("ai-assistant failed:", message);
    return jsonResponse({ ok: false, error: message }, 500);
  }
});
