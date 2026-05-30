export default {
  async fetch(request, env, ctx) {

    const corsHeaders = {
      "Access-Control-Allow-Origin": "*",
      "Access-Control-Allow-Methods": "GET, POST, OPTIONS",
      "Access-Control-Allow-Headers": "Content-Type, Authorization"
    };

    // OPTIONS
    if (request.method === "OPTIONS") {
      return new Response(null, {
        headers: corsHeaders
      });
    }

    // GET TEST
    if (request.method === "GET") {
      return new Response(
        JSON.stringify({
          ok: true,
          service: "Wildlife Whisperer FieldOps API",
          status: "online",
          timestamp: new Date().toISOString()
        }),
        {
          status: 200,
          headers: {
            ...corsHeaders,
            "Content-Type": "application/json"
          }
        }
      );
    }

    // ONLY POST AFTER THIS
    if (request.method !== "POST") {
      return new Response(
        JSON.stringify({
          error: "Method Not Allowed"
        }),
        {
          status: 405,
          headers: {
            ...corsHeaders,
            "Content-Type": "application/json"
          }
        }
      );
    }

    try {

      const body = await request.json();

      // JOB ESTIMATE AI
      if (body.type === "estimate") {

        const species = body.species || "Other";
        const severity = body.severity || "Medium";
        const stories = Number(body.stories || 1);
        const trips = Number(body.trips || 2);
        const taxRate = Number(body.taxRate || 0);

        const basePricing = {
          "Bat": 950,
          "Raccoon": 650,
          "Grey Squirrel": 550,
          "Red Squirrel": 575,
          "Flying Squirrel": 750,
          "Skunk": 450,
          "Groundhog": 450,
          "Rat": 350,
          "Mouse": 325,
          "Carpenter Bee": 350,
          "Bird": 500,
          "Snake": 300,
          "Opossum": 425,
          "Other": 500
        };

        const severityMultiplier = {
          "Low": 1,
          "Medium": 1.35,
          "High": 1.8,
          "Critical": 2.4
        };

        const base = basePricing[species] || 500;
        const mult = severityMultiplier[severity] || 1.35;

        const subtotal =
          Math.round(
            (
              base * mult
            ) +
            (stories * 125) +
            (trips * 85)
          );

        const taxAmount =
          Math.round(subtotal * (taxRate / 100));

        const total =
          subtotal + taxAmount;

        return new Response(
          JSON.stringify({
            ok: true,
            estimate: {
              species,
              severity,
              subtotal,
              taxRate,
              taxAmount,
              total
            }
          }),
          {
            status: 200,
            headers: {
              ...corsHeaders,
              "Content-Type": "application/json"
            }
          }
        );
      }

      // AI INSPECTION ENGINE
      if (body.type === "inspection-ai") {

        const species = body.species || "Other";

        const ai = {
          "Bat":
            "Inspect ridge vents, gable vents, soffit returns, staining, guano accumulation, and legal exclusion timing.",

          "Raccoon":
            "Inspect roof intersections, chimney caps, soffits, attic compression trails, and latrine contamination.",

          "Grey Squirrel":
            "Inspect fascia corners, gable vents, attic trails, and roof edge chew points.",

          "Red Squirrel":
            "Inspect aggressive chew zones, repeated entry points, wiring exposure, and cone cache areas.",

          "Flying Squirrel":
            "Inspect upper soffits, colony nesting areas, wall void movement, and secondary entry holes.",

          "Carpenter Bee":
            "Inspect fascia boards, deck rails, trim returns, and recurring seasonal drill locations."
        };

        return new Response(
          JSON.stringify({
            ok: true,
            recommendation:
              ai[species] ||
              "Inspect all vulnerable entry points, evidence trails, and recurrence risks."
          }),
          {
            status: 200,
            headers: {
              ...corsHeaders,
              "Content-Type": "application/json"
            }
          }
        );
      }

      // PDF SAVE PLACEHOLDER
      if (body.type === "save-pdf") {

        return new Response(
          JSON.stringify({
            ok: true,
            message: "PDF save endpoint ready"
          }),
          {
            status: 200,
            headers: {
              ...corsHeaders,
              "Content-Type": "application/json"
            }
          }
        );
      }

      // FALLBACK
      return new Response(
        JSON.stringify({
          ok: true,
          message: "Worker online"
        }),
        {
          status: 200,
          headers: {
            ...corsHeaders,
            "Content-Type": "application/json"
          }
        }
      );

    } catch (err) {

      return new Response(
        JSON.stringify({
          ok: false,
          error: err.message
        }),
        {
          status: 500,
          headers: {
            ...corsHeaders,
            "Content-Type": "application/json"
          }
        }
      );
    }
  }
};
