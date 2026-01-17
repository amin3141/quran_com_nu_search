import { useState, useCallback } from 'react';
import { Search, SlidersHorizontal, ArrowUp } from 'lucide-react';

interface SearchBoxProps {
  onSearch: (query: string) => void;
  isLoading?: boolean;
}

export function SearchBox({ onSearch, isLoading = false }: SearchBoxProps) {
  const [query, setQuery] = useState('');
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
        <p className="arabic-text text-2xl md:text-3xl text-warm-800 leading-relaxed">
          ذَٰلِكَ الْكِتَابُ لَا رَيْبَ ۛ فِيهِ ۛ هُدًى لِّلْمُتَّقِينَ
        </p>
      </div>

      {/* Search form */}
      <form onSubmit={handleSubmit} className="relative">
        <div className="bg-white rounded-2xl shadow-lg border border-warm-200 overflow-hidden">
          {/* Input area */}
          <div className="flex items-center px-4 py-3">
            <Search className="w-5 h-5 text-warm-400 flex-shrink-0" />
            <input
              type="text"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              onKeyDown={handleKeyDown}
              placeholder="Search the Quran, translations, tafsir, and more..."
              className="flex-1 px-3 py-2 text-warm-800 placeholder-warm-500 bg-transparent border-none outline-none text-base"
              disabled={isLoading}
            />
          </div>

          {/* Bottom bar with buttons */}
          <div className="flex items-center justify-end px-4 py-2 bg-warm-100 border-t border-warm-200">
            <div className="flex items-center gap-2">
              <button
                type="button"
                onClick={() => setShowFilters(!showFilters)}
                className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-sm font-medium transition-colors"
                style={showFilters ? { backgroundColor: '#8B6F5C', color: 'white' } : { backgroundColor: 'white', color: '#5C564F', border: '1px solid #E8E4DE' }}
              >
                <SlidersHorizontal className="w-4 h-4" />
                Filters
              </button>
              <button
                type="submit"
                disabled={!query.trim() || isLoading}
                className="flex items-center justify-center w-10 h-10 rounded-full text-white disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                style={{ backgroundColor: '#8B6F5C' }}
              >
                <ArrowUp className="w-5 h-5" />
              </button>
            </div>
          </div>
        </div>
      </form>

      {/* Filters panel */}
      {showFilters && (
        <div className="mt-3 p-4 bg-white rounded-xl shadow-lg border border-warm-200">
          <h4 className="text-sm font-semibold text-warm-700 mb-3">
            Filter by content type
          </h4>
          <div className="flex flex-wrap gap-2">
            {['Quran', 'Translation', 'Tafsir', 'Posts', 'Courses', 'Articles'].map(
              (filter) => (
                <button
                  key={filter}
                  type="button"
                  className="px-3 py-1.5 text-sm font-medium rounded-full border border-warm-200 text-warm-700 hover:bg-warm-100 hover:border-warm-300 transition-colors"
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
