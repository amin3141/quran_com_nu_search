import { useState, useCallback } from 'react';
import { Search, SlidersHorizontal, ArrowUp } from 'lucide-react';
import type { PostCategory, SpaceType } from '../types';

interface SearchBoxProps {
  onSearch: (query: string) => void;
  isLoading?: boolean;
  query: string;
  onQueryChange: (value: string) => void;
  selectedSpaces: SpaceType[];
  onToggleSpace: (space: SpaceType) => void;
  postCategory: PostCategory | null;
  onToggleReflection: () => void;
}

const FILTER_OPTIONS: { label: string; value: SpaceType; alwaysOn?: boolean }[] = [
  { label: 'Quran (always)', value: 'quran', alwaysOn: true },
  { label: 'Translation', value: 'translation' },
  { label: 'Tafsir', value: 'tafsir' },
  { label: 'Posts', value: 'post' },
  { label: 'Courses', value: 'course' },
  { label: 'Articles', value: 'article' },
];

export function SearchBox({
  onSearch,
  isLoading = false,
  query,
  onQueryChange,
  selectedSpaces,
  onToggleSpace,
  postCategory,
  onToggleReflection,
}: SearchBoxProps) {
  const [showFilters, setShowFilters] = useState(false);
  const hasPosts = selectedSpaces.includes('post');
  const isReflectionOnly = postCategory === 'reflection';

  const handleSubmit = useCallback(
    (e: React.FormEvent) => {
      e.preventDefault();
      const normalizedQuery = query.trim();
      if (normalizedQuery) {
        onSearch(normalizedQuery);
      }
    },
    [query, onSearch]
  );

  const handleKeyDown = useCallback(
    (e: React.KeyboardEvent) => {
      if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault();
        const normalizedQuery = query.trim();
        if (normalizedQuery) {
          onSearch(normalizedQuery);
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
              onChange={(e) => onQueryChange(e.target.value)}
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
            {FILTER_OPTIONS.map((filter) => {
              const isActive =
                filter.alwaysOn || selectedSpaces.includes(filter.value);
              const isDisabled = Boolean(filter.alwaysOn);
              return (
                <button
                  key={filter.value}
                  type="button"
                  aria-pressed={isActive}
                  aria-disabled={isDisabled}
                  onClick={() => {
                    if (!isDisabled) {
                      onToggleSpace(filter.value);
                    }
                  }}
                  className={`px-3 py-1.5 text-sm font-medium rounded-full border transition-colors ${
                    isActive ? 'text-white' : 'text-warm-700 hover:bg-warm-100 hover:border-warm-300'
                  } ${isDisabled ? 'cursor-default opacity-80' : ''}`}
                  style={
                    isActive
                      ? { backgroundColor: '#8B6F5C', borderColor: '#8B6F5C' }
                      : { backgroundColor: 'white', borderColor: '#E8E4DE' }
                  }
                >
                  {filter.label}
                </button>
              );
            })}
          </div>
          <div className="mt-4">
            <p className="text-xs font-semibold text-warm-500 uppercase tracking-wide mb-2">
              Post category
            </p>
            <button
              type="button"
              aria-pressed={isReflectionOnly}
              aria-disabled={!hasPosts}
              onClick={() => {
                if (hasPosts) {
                  onToggleReflection();
                }
              }}
              className={`px-3 py-1.5 text-sm font-medium rounded-full border transition-colors ${
                isReflectionOnly
                  ? 'text-white'
                  : 'text-warm-700 hover:bg-warm-100 hover:border-warm-300'
              } ${!hasPosts ? 'cursor-not-allowed opacity-60' : ''}`}
              style={
                isReflectionOnly
                  ? { backgroundColor: '#8B6F5C', borderColor: '#8B6F5C' }
                  : { backgroundColor: 'white', borderColor: '#E8E4DE' }
              }
            >
              Reflections
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
