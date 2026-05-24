# 📊 Metrics Dashboard

## Overview

The **Metrics** page (`metricsPage()` in `src/main.js`) provides at-a-glance business analytics for Wildlife Whisperer FieldOps. It is accessible from both the side menu and the bottom navigation bar.

## Navigation

- **Side menu**: `📊 Metrics`
- **Bottom nav**: `📊` (5th button, index 4)

## Metrics Displayed

### Summary Cards
| Metric | Description |
|--------|-------------|
| Active Jobs | Jobs where `status !== "Closed"` |
| Closed Jobs | Jobs where `status === "Closed"` |
| Total Jobs | Total count of all jobs |
| Total Revenue | Sum of `grand_total` across all jobs |

### Jobs by Species
- Horizontal bar chart showing species frequency.
- Sorted descending by count.
- Bars are proportional to the most common species.

### Jobs by Status
- Distribution of jobs across all statuses.
- Bars show percentage of total jobs.

### Tech Revenue
- Revenue attributed to each technician (`assigned_tech`).
- Only includes techs already in the `techs` table.
- Sorted descending by revenue.

### Jobs by Town
- Geographic distribution of jobs by town.
- Sorted descending by count.

## Styling

The metrics page reuses existing CSS classes:
- `.card` — container cards
- `.grid` — 4-column summary grid
- `.prog` / `.bar` — horizontal bar charts
- `.stat` — large numbers
- `.tiny` — muted labels

## Adding New Metrics

To add a new chart:

1. Compute the data in `metricsPage()`.
2. Determine `maxValue` for bar scaling.
3. Add a new `.card` block with the same structure as existing charts:

```js
const myCounts = {};
jobs.forEach(j => { if (j.my_field) myCounts[j.my_field] = (myCounts[j.my_field] || 0) + 1; });
const maxMyCount = Math.max(...Object.values(myCounts), 1);

// In shell() template:
`<div class="card">
  <h3>Jobs by My Field</h3>
  ${Object.entries(myCounts).sort((a, b) => b[1] - a[1]).map(([field, count]) => `
    <div style="margin:8px 0;">
      <div style="display:flex;justify-content:space-between;font-size:13px;margin-bottom:4px;">
        <span>${esc(field)}</span>
        <span>${count}</span>
      </div>
      <div class="prog"><div class="bar" style="width:${(count / maxMyCount * 100)}%"></div></div>
    </div>
  `).join("")}
</div>`
```

## Data Sources

All metrics are computed client-side from the local `jobs[]`, `techs[]`, and `services[]` arrays, which are populated via Supabase (`loadData()`). The page updates automatically when `loadData()` is called.
