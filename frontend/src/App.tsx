import { useState, useCallback } from 'react';
import { Header, SearchBox, SearchResults } from './components';
import { mockSearch } from './data/mockData';
import type { SearchResponse } from './types';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '';

function App() {
  const [searchResults, setSearchResults] = useState<SearchResponse | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [hasSearched, setHasSearched] = useState(false);

  const handleSearch = useCallback(async (query: string) => {
    setIsLoading(true);
    setHasSearched(true);

    try {
      const response = await fetch(
        `${API_BASE_URL}/api/search?query=${encodeURIComponent(query)}`
      );
      if (!response.ok) {
        throw new Error(`Search request failed (${response.status})`);
      }
      const results = (await response.json()) as SearchResponse;
      setSearchResults(results);
    } catch (error) {
      console.warn('Search API failed, falling back to mock data', error);
      const results = mockSearch(query);
      setSearchResults(results);
    } finally {
      setIsLoading(false);
    }
  }, []);

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
            <SearchBox onSearch={handleSearch} isLoading={isLoading} />
          </div>
        </section>

        {/* Search results */}
        <section className="px-2 sm:px-4 lg:px-8">
          <SearchResults results={searchResults} isLoading={isLoading} />
        </section>

        {/* Featured content - show only when no search */}
        {!hasSearched && (
          <section className="py-12 px-4 sm:px-6 lg:px-8">
            <div className="max-w-7xl mx-auto">
              <h2 className="text-2xl font-bold text-gray-900 mb-6">
                Try searching for...
              </h2>
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                {[
                  {
                    title: 'ayat al-kursi',
                    description: 'The Verse of the Throne (2:255)',
                  },
                  {
                    title: 'interpretation of ayah 2:255',
                    description: 'Tafsir and scholarly explanations',
                  },
                  {
                    title: 'what does tafsir ibn-kathir say about patience',
                    description: 'Classical tafsir insights',
                  },
                  {
                    title: 'reflections about patience',
                    description: 'Community posts and thoughts',
                  },
                  {
                    title: 'patience with family',
                    description: 'Quranic guidance on family relations',
                  },
                  {
                    title: 'ramadan',
                    description: 'Content about the blessed month',
                  },
                ].map((suggestion) => (
                  <button
                    key={suggestion.title}
                    onClick={() => handleSearch(suggestion.title)}
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
