// supabase/functions/compliance-engine/index.ts
// Wildlife FieldOps - Compliance & Permit Engine + Review Automation
// Handles: NWCO license tracking, expiration alerts, species permit reminders, audit export, review requests

 Deno.serve(async (req) => {
  const corsHeaders = {
    "Access-Control-Allow-Origin": "*",
    "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
    "Access-Control-Allow-Methods": "POST, GET, OPTIONS",
  };

  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  try {
    const supabase = createClient(
      Deno.env.get("SUPABASE_URL")!,
      Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!,
    );

    const url = new URL(req.url);
    const action = url.searchParams.get("action") || "status";

    if (action === "license_status") {
      // Get tech licenses nearing expiration
      const { data, error } = await supabase
        .from("techs")
        .select("id, name, license_number, license_expiry, license_type, notes")
        .not("license_expiry", "is", null)
        .lte("license_expiry", new Date(Date.now() + 60 * 24 * 60 * 60 * 1000).toISOString()); // 60 days

      if (error) throw error;
      return new Response(JSON.stringify({ ok: true, licenses: data }), { headers: { ...corsHeaders, "Content-Type": "application/json" } });
    }

    if (action === "permit_reminders") {
      // Species-specific permit reminders (example logic)
      const { data: jobs } = await supabase
        .from("jobs")
        .select("id, species, scheduled_start, address")
        .gte("scheduled_start", new Date().toISOString())
        .lte("scheduled_start", new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString());

      const reminders = jobs?.map(j => ({
        job_id: j.id,
        species: j.species,
        message: `Check ${j.species} permit requirements for job at ${j.address}`,
      })) || [];

      return new Response(JSON.stringify({ ok: true, reminders }), { headers: { ...corsHeaders, "Content-Type": "application/json" } });
    }

    if (action === "export_audit") {
      const { data, error } = await supabase
        .from("audit_log")
        .select("*")
        .order("created_at", { ascending: false })
        .limit(500);

      if (error) throw error;
      return new Response(JSON.stringify({ ok: true, audit_logs: data }), { headers: { ...corsHeaders, "Content-Type": "application/json" } });
    }

    if (action === "trigger_review") {
      const body = await req.json();
      const { job_id, customer_email, customer_phone } = body;

      // In production: integrate with email/SMS provider (SendGrid, Twilio, or Supabase Edge + Resend)
      // For now: log and return template
      const reviewTemplate = {
        subject: "How was your wildlife removal experience?",
        body: `Hi, thanks for choosing Wildlife Whisperer. Please leave a review: [link]. We appreciate your feedback!`,
        sms: `Thanks for choosing us for your ${body.species || 'wildlife'} job. Quick review? [link]`
      };

      // TODO: Actually send via provider
      console.log("Review triggered for job", job_id);

      return new Response(JSON.stringify({ ok: true, review_template: reviewTemplate }), { headers: { ...corsHeaders, "Content-Type": "application/json" } });
    }

    return new Response(JSON.stringify({ ok: true, message: "Compliance engine ready. Actions: license_status, permit_reminders, export_audit, trigger_review" }), { headers: { ...corsHeaders, "Content-Type": "application/json" } });
  } catch (err) {
    return new Response(JSON.stringify({ ok: false, error: String(err) }), { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } });
  }
});

// Helper (add at top if needed)
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";