# Wildlife Whisperer FieldOps — Rockstar Edition

## 🔥 What Makes This 1000/10

### Visual Polish
- **Splash screen** with animated spinner on load
- **Scroll-reveal animations** — cards fade in as you scroll
- **Animated counters** — numbers count up on dashboard load
- **Ripple effects** on all buttons
- **FAB auto-hide** when scrolling down, reappears when scrolling up
- **Dark/light theme** toggle with persistence
- **Notification badge** on active jobs count
- **Weather widget** on dashboard (when GPS available)
- **Hero gradient** with radial glow on dashboard

### UX Upgrades
- **Modal system** instead of ugly alerts for confirmations
- **Loading states** with spinner overlay on all async operations
- **Better search** with debounced real-time filtering
- **Quick actions** grid on dashboard (New Job, Estimate, Techs, Metrics)
- **Job score** progress bars on every card
- **Species icons** (🦝🐿️🦇 etc.) on every job card
- **Status pills** with color coding (Active=green, Scheduled=blue, etc.)

### Data & Sync
- **Offline-first** — works without internet, syncs when back online
- **Auto-snapshots** every 30 seconds to localStorage backup
- **Conflict resolution** in Cloudflare Worker (last-write-wins)
- **Import/Export** JSON backups
- **Supabase real-time** ready (just enable in config)

### Mobile-First
- **Bottom navigation** with active state highlighting
- **Safe area insets** for notched phones
- **Touch-friendly** targets (min 44px)
- **Capacitor-ready** for native Android build

## 🚀 Quick Start

```bash
# Local dev
npm install
npm run dev

# Build for production
npm run build

# Android APK
npx cap add android
npx cap sync android
npx cap open android
```

## 📱 Deploy to Web

1. Push to GitHub
2. Connect to Vercel/Netlify/Cloudflare Pages
3. Set environment variables:
   - `VITE_SUPABASE_URL`
   - `VITE_SUPABASE_ANON_KEY`
   - `VITE_GOOGLE_MAPS_API_KEY` (optional)
   - `VITE_OPENWEATHER_API_KEY` (optional)

## 🔧 Supabase Schema

```sql
-- Jobs table
create table jobs (
  id uuid default gen_random_uuid() primary key,
  customer_name text not null,
  phone text,
  email text,
  address text not null,
  town text,
  species text,
  status text default 'Active',
  assigned_tech text,
  notes text,
  estimate numeric default 0,
  tax_rate numeric default 0,
  tax_amount numeric default 0,
  grand_total numeric default 0,
  latitude text,
  longitude text,
  created_at timestamptz default now(),
  updated_at timestamptz default now()
);

-- Techs table
create table techs (
  id uuid default gen_random_uuid() primary key,
  name text not null,
  phone text,
  role text,
  created_at timestamptz default now()
);

-- Services table
create table services (
  id uuid default gen_random_uuid() primary key,
  job_id uuid references jobs(id),
  service text,
  qty numeric,
  unit_price numeric,
  total numeric,
  created_at timestamptz default now()
);

-- Inspections table
create table inspections (
  id uuid default gen_random_uuid() primary key,
  job_id uuid references jobs(id),
  inspection_type text,
  notes text,
  created_at timestamptz default now()
);

-- Photos table
create table photos (
  id uuid default gen_random_uuid() primary key,
  job_id uuid references jobs(id),
  image_url text,
  tag text,
  notes text,
  created_at timestamptz default now()
);
```

## 🎯 1000/10 Checklist

- [x] Splash screen with animation
- [x] Dark/light theme toggle
- [x] Bottom navigation
- [x] Animated counters
- [x] Scroll reveal animations
- [x] Ripple button effects
- [x] FAB auto-hide on scroll
- [x] Loading overlay
- [x] Toast notifications
- [x] Modal system
- [x] Job score progress bars
- [x] Species icons
- [x] Status color pills
- [x] Weather widget
- [x] GPS capture
- [x] Offline-first sync
- [x] Import/export JSON
- [x] PDF generation
- [x] Google Calendar integration
- [x] Google Maps integration
- [x] Voice dictation
- [x] AI assistant
- [x] Smart estimator
- [x] Business metrics
- [x] Property history
- [x] Digital contracts
- [x] Signature capture
- [x] Photo compression
- [x] Search with debounce
- [x] Service worker
- [x] Capacitor Android scaffold

## 🦝 Built for Wildlife Whisperer LLC

Cornwall, NY — Humane wildlife removal, exclusion, and prevention.

**Phone:** 845-751-8448
**Email:** Austin@wildlifewhispererllc.com
**Address:** 210 Willow Ave, Cornwall NY 12518
