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
          <div className="flex items-center gap-3 text-warm-500">
            <div className="w-5 h-5 border-2 border-warm-300 rounded-full animate-spin" style={{ borderTopColor: '#8B6F5C' }} />
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
          <BookOpen className="w-12 h-12 text-warm-300 mx-auto mb-4" />
          <h3 className="text-lg font-medium text-warm-800 mb-2">
            No results found
          </h3>
          <p className="text-warm-500">
            Try adjusting your search terms or filters
          </p>
        </div>
      </div>
    );
  }

  const courses = results.directHits.filter((hit) => hit.type === 'course');
  const otherHits = results.directHits.filter((hit) => hit.type !== 'course');
  const hasCourses = courses.length > 0;

  return (
    <div className="w-full max-w-6xl mx-auto mt-8 pb-12">
      {/* Results header */}
      <div className="flex items-center justify-between mb-6">
        <div className="flex items-center gap-2">
          <Sparkles className="w-5 h-5" style={{ color: '#8B6F5C' }} />
          <h2 className="text-lg font-semibold text-warm-800">
            Results for "{results.query}"
          </h2>
        </div>
        <span className="text-sm text-warm-500">
          {results.totalResults} {results.totalResults === 1 ? 'result' : 'results'}
        </span>
      </div>

      {/* Two-column layout: main content + courses sidebar */}
      <div className={`flex flex-col ${hasCourses ? 'lg:flex-row lg:gap-6' : ''}`}>
        {/* Main content column */}
        <div className={hasCourses ? 'lg:flex-1 lg:min-w-0' : 'w-full max-w-4xl'}>
          {/* Ayah-centric results */}
          {results.ayahResults.length > 0 && (
            <div className="mb-8">
              <h3 className="text-sm font-semibold text-warm-500 uppercase tracking-wide mb-4">
                Quranic Verses
              </h3>
              <div className="space-y-4">
                {results.ayahResults.map((ayah) => (
                  <AyahCard key={ayah.ayah_key} result={ayah} />
                ))}
              </div>
            </div>
          )}

          {/* Posts and articles */}
          {otherHits.length > 0 && (
            <div className="mb-8 lg:mb-0">
              <h3 className="text-sm font-semibold text-warm-500 uppercase tracking-wide mb-4">
                Related Content
              </h3>
              <div className="space-y-4">
                {otherHits.map((hit) => {
                  const key = hit.type === 'post' ? hit.post_id : hit.slug;
                  return <DirectHitCard key={key} result={hit} />;
                })}
              </div>
            </div>
          )}
        </div>

        {/* Courses sidebar */}
        {hasCourses && (
          <div className="lg:w-80 flex-shrink-0">
            <h3 className="text-sm font-semibold text-warm-500 uppercase tracking-wide mb-4">
              Courses
            </h3>
            <div className="space-y-4">
              {courses.map((hit) => (
                <DirectHitCard key={hit.course_id} result={hit} />
              ))}
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
