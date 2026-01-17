# Quran.com Omni Search PoC

A proof-of-concept implementation of an "omni search box" for Quran.com that can search across multiple content types and present results in an intelligent, consolidated manner.

## Features

- **Unified Search Interface**: Single search box with AI Chat/Search toggle
- **Multiple Content Types**: Searches across Quran, translations, tafsir, posts, courses, and articles
- **Ayah-Centric Results**: Consolidates results by verse, showing:
  - Arabic Quran text
  - Best-matching translation
  - Relevant tafsir excerpts
  - Related posts, courses, and articles
  - Expandable drill-down for full result set
- **Direct Content Hits**: Links to articles, courses, and posts with excerpts
- **Clean UI**: Inspired by modern Islamic research platforms

## Getting Started

### Prerequisites

- Node.js 18+
- npm or yarn

### Installation

```bash
cd frontend
npm install
```

### Development

```bash
npm run dev
```

The app will be available at `http://localhost:5173`

### Build

```bash
npm run build
```

## Architecture

```
frontend/
├── src/
│   ├── components/     # React components
│   │   ├── Header.tsx
│   │   ├── SearchBox.tsx
│   │   ├── QuickActions.tsx
│   │   ├── SearchResults.tsx
│   │   ├── AyahCard.tsx
│   │   └── DirectHitCard.tsx
│   ├── data/
│   │   └── mockData.ts # Mock search responses
│   ├── types/
│   │   └── index.ts    # TypeScript interfaces
│   ├── App.tsx
│   └── main.tsx
└── ...
```

## Backend (Java/Javalin)

The server lives in `server/` and proxies GoodMem into the response format the UI expects.

### Run

```bash
cd server
export GOODMEM_API_KEY=gm_***
export GOODMEM_INSECURE_SSL=true
gradle run
```

The server defaults to `http://localhost:7070` and exposes `GET /api/search`.

### Environment

- `GOODMEM_BASE_URL` (default `https://omni-dev.quran.ai:8080`)
- `GOODMEM_API_KEY` (required)
- `GOODMEM_INSECURE_SSL` (default `true` for self-signed TLS)
- `GOODMEM_SPACE_IDS` (optional, `quran=...,translation=...` etc)
- `SEARCH_DEFAULT_LANGUAGE` (default `en`)
- `SEARCH_LIMIT_QURAN`, `SEARCH_LIMIT_TRANSLATION`, `SEARCH_LIMIT_TAFSIR`, `SEARCH_LIMIT_POST`, `SEARCH_LIMIT_COURSE`, `SEARCH_LIMIT_ARTICLE`

## Example Queries

Try these searches to see different result types:

- `ayat al-kursi` - Shows consolidated ayah view with translations, tafsir, and related content
- `interpretation of ayah 2:255` - Tafsir-focused results
- `patience` - Returns multiple ayahs about patience with related reflections
- `ramadan` - Direct hits for posts, courses, and articles about Ramadan
- `patience with family` - Mix of ayah results and direct content hits

## Technology Stack

- React 18 with TypeScript
- Vite
- Tailwind CSS v4
- Lucide React (icons)

## GoodMem Integration

The frontend attempts to call `/api/search` and falls back to mock data if the backend is not running. The backend proxies GoodMem:

- REST API: `https://omni-dev.quran.ai:8080`
- gRPC API: `https://omni-dev.quran.ai:9090`

The response structure matches the UI types so the mock data and live search stay interchangeable.
