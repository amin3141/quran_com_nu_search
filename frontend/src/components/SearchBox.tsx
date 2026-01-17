import { useState, useCallback } from 'react';
import { Search, Sparkles, SlidersHorizontal, ArrowUp } from 'lucide-react';
import type { SearchMode } from '../types';

interface SearchBoxProps {
  onSearch: (query: string) => void;
  isLoading?: boolean;
}

export function SearchBox({ onSearch, isLoading = false }: SearchBoxProps) {
  const [query, setQuery] = useState('');
  const [mode, setMode] = useState<SearchMode>('search');
  const [showFilters, setShowFilters] = useState(false);

  const handleSubmit = useCallback(
    (e: React.FormEvent) => {
      e.preventDefault();
      if (query.trim()) {
        onSearch(query.trim());
      }
    },
    [query, onSearch]
  );

  const handleKeyDown = useCallback(
    (e: React.KeyboardEvent) => {
      if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault();
        if (query.trim()) {
          onSearch(query.trim());
        }
      }
    },
    [query, onSearch]
  );

  return (
    <div className="w-full max-w-3xl mx-auto">
      {/* Arabic verse display */}
      <div className="mb-4 text-center">
        <p className="arabic-text text-2xl md:text-3xl text-gray-800 leading-relaxed">
          ذَٰلِكَ الْكِتَابُ لَا رَيْبَ ۛ فِيهِ ۛ هُدًى لِّلْمُتَّقِينَ
        </p>
      </div>

      {/* Search form */}
      <form onSubmit={handleSubmit} className="relative">
        <div className="bg-white rounded-2xl shadow-lg border border-gray-200 overflow-hidden">
          {/* Input area */}
          <div className="flex items-center px-4 py-3">
            <Search className="w-5 h-5 text-gray-400 flex-shrink-0" />
            <input
              type="text"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              onKeyDown={handleKeyDown}
              placeholder="Search the Quran, translations, tafsir, and more..."
              className="flex-1 px-3 py-2 text-gray-900 placeholder-gray-500 bg-transparent border-none outline-none text-base"
              disabled={isLoading}
            />
          </div>

          {/* Bottom bar with toggle and buttons */}
          <div className="flex items-center justify-between px-4 py-2 bg-gray-50 border-t border-gray-100">
            {/* Mode toggle */}
            <div className="flex items-center gap-1 bg-white rounded-lg border border-gray-200 p-1">
              <button
                type="button"
                onClick={() => setMode('ai-chat')}
                className={`flex items-center gap-1.5 px-3 py-1.5 rounded-md text-sm font-medium transition-colors ${
                  mode === 'ai-chat'
                    ? 'bg-[--color-primary] text-white'
                    : 'text-gray-600 hover:bg-gray-100'
                }`}
              >
                <Sparkles className="w-4 h-4" />
                AI Chat
              </button>
              <button
                type="button"
                onClick={() => setMode('search')}
                className={`flex items-center gap-1.5 px-3 py-1.5 rounded-md text-sm font-medium transition-colors ${
                  mode === 'search'
                    ? 'bg-[--color-primary] text-white'
                    : 'text-gray-600 hover:bg-gray-100'
                }`}
              >
                <Search className="w-4 h-4" />
                Search
              </button>
            </div>

            {/* Right side buttons */}
            <div className="flex items-center gap-2">
              <button
                type="button"
                onClick={() => setShowFilters(!showFilters)}
                className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-sm font-medium transition-colors ${
                  showFilters
                    ? 'bg-[--color-primary] text-white'
                    : 'text-gray-600 hover:bg-gray-200 bg-white border border-gray-200'
                }`}
              >
                <SlidersHorizontal className="w-4 h-4" />
                Filters
              </button>
              <button
                type="submit"
                disabled={!query.trim() || isLoading}
                className="flex items-center justify-center w-10 h-10 rounded-full bg-[--color-primary] text-white hover:bg-[--color-primary-dark] disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
              >
                <ArrowUp className="w-5 h-5" />
              </button>
            </div>
          </div>
        </div>
      </form>

      {/* Filters panel */}
      {showFilters && (
        <div className="mt-3 p-4 bg-white rounded-xl shadow-lg border border-gray-200">
          <h4 className="text-sm font-semibold text-gray-700 mb-3">
            Filter by content type
          </h4>
          <div className="flex flex-wrap gap-2">
            {['Quran', 'Translation', 'Tafsir', 'Posts', 'Courses', 'Articles'].map(
              (filter) => (
                <button
                  key={filter}
                  type="button"
                  className="px-3 py-1.5 text-sm font-medium rounded-full border border-gray-200 text-gray-700 hover:bg-gray-100 hover:border-gray-300 transition-colors"
                >
                  {filter}
                </button>
              )
            )}
          </div>
        </div>
      )}
    </div>
  );
}
