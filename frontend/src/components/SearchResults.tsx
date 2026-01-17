import { AyahCard } from './AyahCard';
import { DirectHitCard } from './DirectHitCard';
import type { SearchResponse } from '../types';
import { BookOpen, Sparkles } from 'lucide-react';

interface SearchResultsProps {
  results: SearchResponse | null;
  isLoading: boolean;
}

export function SearchResults({ results, isLoading }: SearchResultsProps) {
  if (isLoading) {
    return (
      <div className="w-full max-w-4xl mx-auto mt-8">
        <div className="flex items-center justify-center py-12">
          <div className="flex items-center gap-3 text-gray-500">
            <div className="w-5 h-5 border-2 border-gray-300 border-t-[--color-primary] rounded-full animate-spin" />
            <span>Searching...</span>
          </div>
        </div>
      </div>
    );
  }

  if (!results) {
    return null;
  }

  if (results.totalResults === 0) {
    return (
      <div className="w-full max-w-4xl mx-auto mt-8">
        <div className="text-center py-12">
          <BookOpen className="w-12 h-12 text-gray-300 mx-auto mb-4" />
          <h3 className="text-lg font-medium text-gray-900 mb-2">
            No results found
          </h3>
          <p className="text-gray-500">
            Try adjusting your search terms or filters
          </p>
        </div>
      </div>
    );
  }

  return (
    <div className="w-full max-w-4xl mx-auto mt-8 pb-12">
      {/* Results header */}
      <div className="flex items-center justify-between mb-6">
        <div className="flex items-center gap-2">
          <Sparkles className="w-5 h-5 text-[--color-primary]" />
          <h2 className="text-lg font-semibold text-gray-900">
            Results for "{results.query}"
          </h2>
        </div>
        <span className="text-sm text-gray-500">
          {results.totalResults} {results.totalResults === 1 ? 'result' : 'results'}
        </span>
      </div>

      {/* Ayah-centric results */}
      {results.ayahResults.length > 0 && (
        <div className="mb-8">
          <h3 className="text-sm font-semibold text-gray-500 uppercase tracking-wide mb-4">
            Quranic Verses
          </h3>
          <div className="space-y-4">
            {results.ayahResults.map((ayah) => (
              <AyahCard key={ayah.ayah_key} result={ayah} />
            ))}
          </div>
        </div>
      )}

      {/* Direct content hits */}
      {results.directHits.length > 0 && (
        <div>
          <h3 className="text-sm font-semibold text-gray-500 uppercase tracking-wide mb-4">
            Related Content
          </h3>
          <div className="space-y-4">
            {results.directHits.map((hit) => {
              const key =
                hit.type === 'post'
                  ? hit.post_id
                  : hit.type === 'course'
                    ? hit.course_id
                    : hit.slug;
              return <DirectHitCard key={key} result={hit} />;
            })}
          </div>
        </div>
      )}
    </div>
  );
}
