// supabase/functions/ai-assistant/index.ts
// Wildlife Whisperer FieldOps AI Assistant - Kimi/Moonshot version
// Keeps your Kimi/Moonshot API key server-side in Supabase Edge Functions.

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

function buildMessages(payload: AiRequest) {
  const mode = payload.mode || "field_plan";

  const system = [
    "You are the Wildlife Whisperer FieldOps AI assistant.",
    "You help a nuisance wildlife removal technician produce practical field notes, estimate guidance, customer messages, and invoice notes.",
    "You are not a lawyer, veterinarian, pesticide label authority, or code-enforcement official.",
    "Do not invent exact legal claims. Give reminders to verify local/state rules, pesticide labels, bat exclusion timing, permits, and protected species requirements.",
    "Prefer concise, job-ready output. Use plain English. Avoid hype.",
    "Pricing should be guidance only and should be framed as a suggested range, not a guaranteed price.",
    "Return ONLY valid JSON. Do not wrap it in markdown. Do not add extra commentary.",
  ].join("\n");

  const user = JSON.stringify(
    {
      requested_mode: mode,
      business_context:
        payload.businessContext ||
        "Small nuisance wildlife removal company. Services include inspection, exclusion, repair, trapping coordination, sanitation, and documentation.",
      job: payload.job || {},
      species: payload.species || payload.job?.species || "",
      observation: safeString(payload.observation || payload.job?.notes || ""),
      services: payload.services || [],
      inspections: payload.inspections || [],
      required_json_shape: {
        mode: "string",
        summary: "string",
        recommended_next_steps: ["string"],
        estimate_guidance: {
          suggested_line_items: [
            {
              service: "string",
              qty: "number",
              unit_price: "number",
              rationale: "string",
            },
          ],
          subtotal_low: "number",
          subtotal_high: "number",
          pricing_notes: "string",
        },
        customer_message: "string",
        invoice_notes: "string",
        safety_flags: ["string"],
        legal_or_permit_reminders: ["string"],
        confidence: "low | medium | high",
      },
    },
    null,
    2,
  );

  return [
    { role: "system", content: system },
    { role: "user", content: user },
  ];
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
    throw new Error("Kimi returned text that was not valid JSON.");
  }
}

async function callKimi(payload: AiRequest) {
  const apiKey = Deno.env.get("MOONSHOT_API_KEY") || Deno.env.get("KIMI_API_KEY");
  if (!apiKey) {
    throw new Error("Missing MOONSHOT_API_KEY Supabase secret.");
  }

  const baseUrl = Deno.env.get("MOONSHOT_BASE_URL") || "https://api.moonshot.ai/v1";
  const model = Deno.env.get("KIMI_MODEL") || Deno.env.get("MOONSHOT_MODEL") || "kimi-k2-0905-preview";

  const res = await fetch(`${baseUrl.replace(/\/$/, "")}/chat/completions`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${apiKey}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      model,
      messages: buildMessages(payload),
      temperature: 0.2,
      max_tokens: 1600,
      response_format: { type: "json_object" },
    }),
  });

  const raw = await res.text();

  if (!res.ok) {
    throw new Error(`Kimi/Moonshot error ${res.status}: ${raw.slice(0, 1000)}`);
  }

  const data = JSON.parse(raw);
  const content = data?.choices?.[0]?.message?.content;

  if (!content) {
    throw new Error("Kimi/Moonshot returned no message content.");
  }

  return extractJson(content);
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
        {
          error: "Send at least one of: job, observation, or species.",
        },
        400,
      );
    }

    const result = await callKimi(payload);

    return jsonResponse({
      ok: true,
      provider: "kimi_moonshot",
      result,
    });
  } catch (err) {
    const message = err instanceof Error ? err.message : String(err);
    console.error("ai-assistant failed:", message);
    return jsonResponse(
      {
        ok: false,
        error: message,
      },
      500,
    );
  }
});
