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

## Future Integration

This PoC uses mock data. For production, integrate with the GoodMem API:

- REST API: `https://omni-dev.quran.ai:8080`
- gRPC API: `https://omni-dev.quran.ai:9090`

The mock data structure matches the expected GoodMem response format for easy integration.
