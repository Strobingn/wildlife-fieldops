/**
 * AIModal.js — AI Assistant page
 * Species selector, season selector, observation textarea, suggestions, loading state
 */

import { SPECIES, SPECIES_HINTS } from "../constants.js";

function E(s) {
  return String(s || "").replace(/[&<>"']/g, (m) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", "\"": "&quot;", "'": "&#39;" }[m]));
}

const SEASONS = ["Spring", "Summer", "Fall", "Winter"];

export const AIModal = {
  _listeners: [],
  _loading: false,
  _result: null,
  _species: SPECIES[0],
  _season: "Spring",
  _observations: "",

  render(state) {
    const currentMonth = new Date().getMonth();
    const currentSeason = currentMonth < 2 ? "Winter" : currentMonth < 5 ? "Spring" : currentMonth < 8 ? "Summer" : "Fall";
    if (!this._season) this._season = currentSeason;

    return /* html */ `
      <div class="card stack">
        <h2>🧠 AI Field Assistant</h2>
        <p class="tiny">Describe what you're seeing and get species-specific guidance.</p>
      </div>

      <!-- Input Form -->
      <div class="card">
        <div class="section-title" style="margin-top:0;">Observation</div>

        <label for="aiSpecies">Species</label>
        <select id="aiSpecies">
          ${SPECIES.map((s) => `<option value="${E(s)}" ${this._species === s ? "selected" : ""}>${E(s)}</option>`).join("")}
        </select>

        <label for="aiSeason">Season</label>
        <select id="aiSeason">
          ${SEASONS.map((s) => `<option value="${E(s)}" ${this._season === s ? "selected" : ""}>${E(s)} ${s === currentSeason ? "(current)" : ""}</option>`).join("")}
        </select>

        <label for="aiObservations">What are you observing?</label>
        <textarea
          id="aiObservations"
          rows="5"
          placeholder="Describe the situation: noises, damage, droppings, time of day, location on property, entry points..."
        >${E(this._observations)}</textarea>

        <button class="action" data-action="get-suggestions" ${this._loading ? "disabled" : ""}>
          ${this._loading ? "🔄 Analyzing..." : "🔍 Get Suggestions"}
        </button>
      </div>

      <!-- Species Hint -->
      <div class="card">
        <div class="section-title" style="margin-top:0;">Species Quick Reference</div>
        <div style="font-size:14px;line-height:1.6;">
          <b>${E(this._species)}</b>
          <p style="color:var(--muted);margin-top:4px;">${E(SPECIES_HINTS[this._species] || "Track behavior, seasonality, and recurrence patterns.")}</p>
        </div>
      </div>

      <!-- Loading State -->
      ${this._loading
        ? `<div class="card" style="text-align:center;padding:32px;">
            <div class="splash-spinner" style="margin:0 auto 16px;"></div>
            <div style="font-weight:600;">Analyzing your observation...</div>
            <div class="tiny">Checking species patterns, seasonal behavior, and structural indicators.</div>
           </div>`
        : ""
      }

      <!-- Results -->
      ${this._result
        ? `<div class="card">
            <div class="section-title" style="margin-top:0;">AI Analysis</div>
            <div class="ai-result">${this._result}</div>
           </div>
           <button class="action dark" data-action="clear-result" style="margin-bottom:12px;">🔄 New Analysis</button>`
        : ""
      }

      <!-- Common Scenarios -->
      <div class="section-title">Common Scenarios</div>
      <div class="card">
        <div style="font-size:14px;line-height:1.8;">
          <details style="margin-bottom:8px;">
            <summary style="cursor:pointer;font-weight:600;">🦝 Raccoon — Attic noise at night</summary>
            <div style="padding:8px 0;color:var(--muted);">
              Check roof vents, soffit returns, and chimney caps. Look for latrine sites (concentrated droppings).
              Raccoons need ~4" opening. Set one-way door after confirming no juveniles present.
            </div>
          </details>
          <details style="margin-bottom:8px;">
            <summary style="cursor:pointer;font-weight:600;">🐿️ Squirrel — Chewing sounds in ceiling</summary>
            <div style="padding:8px 0;color:var(--muted);">
              Inspect fascia boards, gable vents, and roof-to-wall joints. Grey squirrels can fit through 1.5" gaps.
              Look for chewed entry points and nesting materials in attic.
            </div>
          </details>
          <details style="margin-bottom:8px;">
            <summary style="cursor:pointer;font-weight:600;">🦇 Bat — Droppings in attic, dusk activity</summary>
            <div style="padding:8px 0;color:var(--muted);">
              Identify entry/exit points by watching at dusk. Check for guano accumulation and urine stains.
              Note: Maternal season restrictions apply (May-Aug). Use one-way exclusion devices.
            </div>
          </details>
          <details style="margin-bottom:8px;">
            <summary style="cursor:pointer;font-weight:600;">🦨 Skunk — Odor under deck/porch</summary>
            <div style="padding:8px 0;color:var(--muted);">
              Locate den entrance. Skunks burrow under structures. Install trench-and-screen exclusion.
              Use positive-set traps near entrance. Be alert for defensive spray warning signs.
            </div>
          </details>
          <details>
            <summary style="cursor:pointer;font-weight:600;">🐦 Bird — Nesting in vent/soffit</summary>
            <div style="padding:8px 0;color:var(--muted);">
              Check dryer vents, bathroom vents, and soffit gaps. Remove nesting material and install vent guards.
              Watch for mites. European starlings and house sparrows are not protected.
            </div>
          </details>
        </div>
      </div>

      <!-- Seasonal Tips -->
      <div class="section-title">Seasonal Tips</div>
      <div class="card">
        <div style="font-size:14px;">
          ${this._renderSeasonalTips(this._season)}
        </div>
      </div>
    `;
  },

  afterRender(state) {
    // Track input changes
    const speciesEl = document.getElementById("aiSpecies");
    const seasonEl = document.getElementById("aiSeason");
    const obsEl = document.getElementById("aiObservations");

    if (speciesEl) {
      const handler = () => { this._species = speciesEl.value; state.rerender?.(); };
      speciesEl.addEventListener("change", handler);
      this._listeners.push({ el: speciesEl, type: "change", fn: handler });
    }
    if (seasonEl) {
      const handler = () => { this._season = seasonEl.value; state.rerender?.(); };
      seasonEl.addEventListener("change", handler);
      this._listeners.push({ el: seasonEl, type: "change", fn: handler });
    }
    if (obsEl) {
      const handler = () => { this._observations = obsEl.value; };
      obsEl.addEventListener("input", handler);
      this._listeners.push({ el: obsEl, type: "input", fn: handler });
    }

    // Action buttons
    document.querySelectorAll("[data-action]").forEach((btn) => {
      const handler = () => {
        const action = btn.dataset.action;

        if (action === "get-suggestions") {
          this._getSuggestions(state);
        }

        if (action === "clear-result") {
          this._result = null;
          this._observations = "";
          state.rerender?.();
        }
      };
      btn.addEventListener("click", handler);
      this._listeners.push({ el: btn, type: "click", fn: handler });
    });
  },

  unmount() {
    this._listeners.forEach(({ el, type, fn }) => el.removeEventListener(type, fn));
    this._listeners = [];
    this._loading = false;
    this._result = null;
  },

  _getSuggestions(state) {
    this._loading = true;
    this._result = null;
    state.rerender?.();

    // Simulate async AI call
    setTimeout(() => {
      this._loading = false;
      this._result = this._generateAnalysis();
      state.rerender?.();
    }, 1500);
  },

  _generateAnalysis() {
    const observations = this._observations.toLowerCase();
    const species = this._species;
    const season = this._season;
    const hints = SPECIES_HINTS[species] || "Track behavior, seasonality, and recurrence.";

    let tips = [];

    // Species-specific tips
    tips.push(`<b>Species Profile — ${species}</b>`);
    tips.push(hints);

    // Seasonal context
    tips.push("");
    tips.push(`<b>Seasonal Context — ${season}</b>`);
    if (season === "Spring") {
      tips.push("Breeding season for most mammals. Check for juveniles before exclusion. Mothers will be aggressive. Look for nesting materials.");
    } else if (season === "Summer") {
      tips.push("Juveniles mobile. Full exclusion possible. High activity period. Watch for secondary entry points created by young.");
    } else if (season === "Fall") {
      tips.push("Pre-winter entry surge. Animals seeking overwinter shelter. Inspect all potential entry points thoroughly.");
    } else if (season === "Winter") {
      tips.push("Dormancy for some species, but bats may still roost. Rodents seeking indoor warmth. Ice dams can create new entry points.");
    }

    // Keyword-based observations
    const keywords = [];
    if (observations.includes("night") || observations.includes("evening")) {
      keywords.push("<b>Night Activity</b> — Points to raccoon, flying squirrel, bat, or mice depending on sound type. Scratching = squirrels. Chattering = raccoons. Fluttering = bats.");
    }
    if (observations.includes("soffit") || observations.includes("fascia") || observations.includes("roof")) {
      keywords.push("<b>Roofline Entry</b> — Inspect soffit returns, fascia corners, roof-to-wall joints, and gable vents. Look for chew marks, rub marks, and staining.");
    }
    if (observations.includes("attic")) {
      keywords.push("<b>Attic Infestation</b> — Check insulation trails, nesting zones, rub marks, urine staining, and secondary exits. Use UV light to detect urine.");
    }
    if (observations.includes("droppings") || observations.includes("guano") || observations.includes("scat")) {
      keywords.push("<b>Droppings Found</b> — Document size, shape, and location. Raccoon = large tubular. Squirrel = small round. Bat = dry pellets that crumble to dust.");
    }
    if (observations.includes("noise") || observations.includes("sound") || observations.includes("scratching")) {
      keywords.push("<b>Noise Analysis</b> — Fast scratching = squirrel. Slow heavy movement = raccoon. Fluttering = bat. Squeaking = mice/birds. Timing matters: day vs night.");
    }
    if (observations.includes("odor") || observations.includes("smell")) {
      keywords.push("<b>Odor Present</b> — Strong musky = skunk or raccoon latrine. Ammonia-like = bat guano/urine. Musty = dead animal or mold from urine.");
    }
    if (observations.includes("deck") || observations.includes("porch") || observations.includes("shed")) {
      keywords.push("<b>Ground Structure</b> — Check for burrowing under decks/sheds. Look for dig marks, hair, and worn paths. Groundhogs and skunks are common.");
    }
    if (observations.includes("chimney")) {
      keywords.push("<b>Chimney Entry</b> — Install chimney cap with proper mesh size. Check for nesting material. Birds, squirrels, and raccoons are common chimney invaders.");
    }
    if (observations.includes("baby") || observations.includes("juvenile") || observations.includes("young")) {
      keywords.push("<b>Juveniles Present</b> — Do NOT exclude yet. Juveniles must be mobile or removed first. Delay exclusion until juveniles can exit independently.");
    }

    if (keywords.length) {
      tips.push("");
      tips.push("<b>Observation Analysis</b>");
      tips.push(...keywords);
    }

    // General recommendations
    tips.push("");
    tips.push("<b>Recommended Next Steps</b>");
    tips.push("1. Document all evidence with photos (entry points, damage, droppings)");
    tips.push("2. Conduct full perimeter inspection — walk the property clockwise");
    tips.push("3. Identify all potential entry points (primary and secondary)");
    tips.push("4. Check for juveniles before any exclusion work");
    tips.push("5. Present findings to customer with photo documentation");

    return tips.join("\n");
  },

  _renderSeasonalTips(season) {
    const tips = {
      "Spring": [
        "🐣 Breeding season — check for babies before exclusion",
        "🌧️ Rain can expose entry points through water damage",
        "🌳 Tree trimming season — remove access bridges",
        "Busy season begins — book follow-ups early",
      ],
      "Summer": [
        "☀️ Juveniles now mobile — full exclusion safe",
        "🔥 Check attic temperatures — heat stress on animals",
        "🦟 Mosquito-heavy — wear repellent on exterior work",
        "Peak demand — prioritize warranty callbacks",
      ],
      "Fall": [
        "🍂 Pre-winter entry surge — animals seeking shelter",
        "🌡️ Temperature drops trigger migration indoors",
        "📋 Best time for prevention inspections",
        "Gutter cleaning season — check for entry points",
      ],
      "Winter": [
        "❄️ Reduced activity for some species",
        "🦇 Bats may still roost — check warm attic areas",
        "🐁 Rodent calls increase — indoor warmth seeking",
        "Good time for equipment maintenance and training",
      ],
    };

    return (tips[season] || tips["Spring"]).map((t) => `<div style="padding:6px 0;border-bottom:1px solid var(--border);">${t}</div>`).join("");
  },
};
