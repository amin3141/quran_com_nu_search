import { useState, useCallback, useMemo, useEffect } from 'react';
import { Header, SearchBox, SearchResults } from './components';
import { mockSearch } from './data/mockData';
import type { PostCategory, SearchResponse, SpaceType } from './types';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '';
const OPTIONAL_SPACES: SpaceType[] = [
  'translation',
  'tafsir',
  'post',
  'course',
  'article',
];

type SearchSuggestion = {
  title: string;
  description: string;
  spaces?: SpaceType[];
  postCategory?: PostCategory | null;
};

type SearchOptions = {
  spaces?: SpaceType[];
  postCategory?: PostCategory | null;
  pushHistory?: boolean;
};

const SEARCH_SUGGESTIONS: SearchSuggestion[] = [
  {
    title: 'ayat al-kursi',
    description: 'The Verse of the Throne (2:255)',
  },
  {
    title: 'interpretation of ayah 2:255',
    description: 'Tafsir and scholarly explanations',
    spaces: ['tafsir'],
  },
  {
    title: 'what does tafsir ibn-kathir say about patience',
    description: 'Classical tafsir insights',
    spaces: ['tafsir'],
  },
  {
    title: 'reflections about patience',
    description: 'Community posts and thoughts',
    spaces: ['post'],
    postCategory: 'reflection',
  },
  {
    title: 'patience with family',
    description: 'Quranic guidance on family relations',
  },
  {
    title: 'ramadan',
    description: 'Content about the blessed month',
  },
];

function App() {
  const [searchResults, setSearchResults] = useState<SearchResponse | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [hasSearched, setHasSearched] = useState(false);
  const [selectedSpaces, setSelectedSpaces] = useState<SpaceType[]>(OPTIONAL_SPACES);
  const [postCategory, setPostCategory] = useState<PostCategory | null>(null);
  const [query, setQuery] = useState('');

  const requestedSpaces = useMemo<SpaceType[]>(
    () => ['quran', ...selectedSpaces],
    [selectedSpaces]
  );

  const handleToggleSpace = useCallback((space: SpaceType) => {
    setSelectedSpaces((prev) => {
      const next = new Set(prev);
      if (next.has(space)) {
        next.delete(space);
      } else {
        next.add(space);
      }
      if (space === 'post' && !next.has('post')) {
        setPostCategory(null);
      }
      return OPTIONAL_SPACES.filter((value) => next.has(value));
    });
  }, []);

  const handleToggleReflection = useCallback(() => {
    setPostCategory((prev) => (prev === 'reflection' ? null : 'reflection'));
  }, []);

  const parseSpaces = useCallback((value: string | null) => {
    if (!value || !value.trim()) {
      return OPTIONAL_SPACES;
    }
    const parts = value
      .split(',')
      .map((entry) => entry.trim())
      .filter(Boolean) as SpaceType[];
    const normalized = OPTIONAL_SPACES.filter((space) => parts.includes(space));
    return normalized.length > 0 ? normalized : OPTIONAL_SPACES;
  }, []);

  const parsePostCategory = useCallback((value: string | null): PostCategory | null => {
    if (value === 'reflection') {
      return 'reflection';
    }
    return null;
  }, []);

  const resetToHome = useCallback(() => {
    setQuery('');
    setSearchResults(null);
    setIsLoading(false);
    setHasSearched(false);
    setSelectedSpaces(OPTIONAL_SPACES);
    setPostCategory(null);
  }, []);

  const updateHistory = useCallback(
    (nextQuery: string, spaces: SpaceType[], category: PostCategory | null) => {
      const params = new URLSearchParams();
      params.set('q', nextQuery);
      params.set('spaces', spaces.join(','));
      if (category) {
        params.set('postCategory', category);
      }
      const nextUrl = `${window.location.pathname}?${params.toString()}`;
      window.history.pushState({ q: nextQuery, spaces, postCategory: category }, '', nextUrl);
    },
    []
  );

  const performSearch = useCallback(async (rawQuery: string, options?: SearchOptions) => {
    const normalizedQuery = rawQuery.trim();
    if (!normalizedQuery) {
      return;
    }

    setQuery(normalizedQuery);
    const resolvedSpaces = options?.spaces ?? selectedSpaces;
    const resolvedPostCategory =
      typeof options?.postCategory !== 'undefined' ? options?.postCategory ?? null : postCategory;
    const resolvedRequestedSpaces: SpaceType[] = ['quran', ...resolvedSpaces];
    const shouldPushHistory = options?.pushHistory !== false;
    if (shouldPushHistory) {
      updateHistory(normalizedQuery, resolvedSpaces, resolvedPostCategory);
    }

    setIsLoading(true);
    setHasSearched(true);

    try {
      const response = await fetch(
        `${API_BASE_URL}/api/search?query=${encodeURIComponent(normalizedQuery)}&spaces=${encodeURIComponent(resolvedRequestedSpaces.join(','))}`
      );
      if (!response.ok) {
        throw new Error(`Search request failed (${response.status})`);
      }
      const results = (await response.json()) as SearchResponse;
      setSearchResults(results);
    } catch (error) {
      console.warn('Search API failed, falling back to mock data', error);
      const results = mockSearch(normalizedQuery, resolvedRequestedSpaces, resolvedPostCategory);
      setSearchResults(results);
    } finally {
      setIsLoading(false);
    }
  }, [postCategory, selectedSpaces, updateHistory]);

  const handleSearch = useCallback((query: string) => {
    performSearch(query);
  }, [performSearch]);

  const handleSuggestionClick = useCallback((suggestion: SearchSuggestion) => {
    const hasSpaceOverride = typeof suggestion.spaces !== 'undefined';
    const nextSpaces = suggestion.spaces ?? selectedSpaces;
    const normalizedSpaces = OPTIONAL_SPACES.filter((space) => nextSpaces.includes(space));
    const normalizedQuery = suggestion.title.trim();
    const nextPostCategory =
      typeof suggestion.postCategory !== 'undefined'
        ? suggestion.postCategory ?? null
        : hasSpaceOverride
          ? null
          : postCategory;

    if (hasSpaceOverride) {
      setSelectedSpaces(normalizedSpaces);
    }
    if (hasSpaceOverride || typeof suggestion.postCategory !== 'undefined') {
      setPostCategory(nextPostCategory);
    }
    setQuery(normalizedQuery);

    performSearch(normalizedQuery, {
      spaces: normalizedSpaces,
      postCategory: nextPostCategory,
    });
  }, [performSearch, postCategory, selectedSpaces]);

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const initialQuery = params.get('q');
    if (initialQuery) {
      const spaces = parseSpaces(params.get('spaces'));
      const category = parsePostCategory(params.get('postCategory'));
      setSelectedSpaces(spaces);
      setPostCategory(category);
      performSearch(initialQuery, {
        spaces,
        postCategory: category,
        pushHistory: false,
      });
    } else {
      window.history.replaceState({ type: 'home' }, '', window.location.pathname);
    }

    const handlePopState = () => {
      const nextParams = new URLSearchParams(window.location.search);
      const nextQuery = nextParams.get('q');
      if (!nextQuery) {
        resetToHome();
        return;
      }
      const spaces = parseSpaces(nextParams.get('spaces'));
      const category = parsePostCategory(nextParams.get('postCategory'));
      setSelectedSpaces(spaces);
      setPostCategory(category);
      performSearch(nextQuery, {
        spaces,
        postCategory: category,
        pushHistory: false,
      });
    };

    window.addEventListener('popstate', handlePopState);
    return () => {
      window.removeEventListener('popstate', handlePopState);
    };
  // eslint-disable-next-line react-hooks/exhaustive-deps -- performSearch intentionally excluded to prevent infinite loops
  }, [parsePostCategory, parseSpaces, resetToHome]);

  return (
    <div className="min-h-screen bg-gray-50">
      <Header />

      <main>
        {/* Hero section */}
        <section
          className={`transition-all duration-500 ${
            hasSearched ? 'py-8' : 'py-16 md:py-24'
          }`}
        >
          <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
            {/* Title - hide after search */}
            {!hasSearched && (
              <div className="text-center mb-8">
                <h1 className="text-3xl md:text-4xl lg:text-5xl font-bold text-gray-900 mb-4">
                  AI-Powered Islamic Research
                </h1>
                <p className="text-lg text-gray-600 max-w-2xl mx-auto">
                  Explore, search, and analyze over 340,000 texts including Quran,
                  translations, tafsir, reflections, and courses.
                </p>
              </div>
            )}

            {/* Search box */}
            <SearchBox
              onSearch={handleSearch}
              isLoading={isLoading}
              query={query}
              onQueryChange={setQuery}
              selectedSpaces={selectedSpaces}
              onToggleSpace={handleToggleSpace}
              postCategory={postCategory}
              onToggleReflection={handleToggleReflection}
            />
          </div>
        </section>

        {/* Search results */}
        <section className="px-2 sm:px-4 lg:px-8">
          <SearchResults
            results={searchResults}
            isLoading={isLoading}
            activeSpaces={requestedSpaces}
            postCategory={postCategory}
          />
        </section>

        {/* Featured content - show only when no search */}
        {!hasSearched && (
          <section className="py-12 px-4 sm:px-6 lg:px-8">
            <div className="max-w-7xl mx-auto">
              <h2 className="text-2xl font-bold text-gray-900 mb-6">
                Try searching for...
              </h2>
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                {SEARCH_SUGGESTIONS.map((suggestion) => (
                  <button
                    key={suggestion.title}
                    onClick={() => handleSuggestionClick(suggestion)}
                    className="text-left p-4 bg-white rounded-xl border border-gray-200 hover:border-[--color-primary] hover:shadow-md transition-all"
                  >
                    <p className="font-medium text-gray-900">{suggestion.title}</p>
                    <p className="text-sm text-gray-500 mt-1">
                      {suggestion.description}
                    </p>
                  </button>
                ))}
              </div>
            </div>
          </section>
        )}
      </main>

      {/* Footer */}
      <footer className="py-8 border-t border-gray-200 mt-auto">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
          <p className="text-sm text-gray-500">
            Quran.com Omni Search PoC — Powered by GoodMem
          </p>
        </div>
      </footer>
    </div>
  );
}

export default App;
