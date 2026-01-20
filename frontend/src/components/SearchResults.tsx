import { AyahCard } from './AyahCard';
import { DirectHitCard } from './DirectHitCard';
import type { PostCategory, SearchResponse, SpaceType } from '../types';
import { BookOpen, Sparkles } from 'lucide-react';

interface SearchResultsProps {
  results: SearchResponse | null;
  isLoading: boolean;
  activeSpaces: SpaceType[];
  postCategory: PostCategory | null;
}

export function SearchResults({
  results,
  isLoading,
  activeSpaces,
  postCategory,
}: SearchResultsProps) {
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

  const spaceSet = new Set(activeSpaces);
  const showQuran = spaceSet.has('quran');
  const showPosts = spaceSet.has('post');
  const showCourses = spaceSet.has('course');
  const showArticles = spaceSet.has('article');
  const activePostCategory = showPosts ? postCategory : null;

  const filteredDirectHits = results.directHits.filter((hit) => {
    if (hit.type === 'course') return showCourses;
    if (hit.type === 'post') {
      if (!showPosts) return false;
      if (activePostCategory === 'reflection') {
        return hit.category === 'reflection';
      }
      return true;
    }
    if (hit.type === 'article') return showArticles;
    return true;
  });

  const ayahResults = showQuran ? results.ayahResults : [];
  const displayTotal = filteredDirectHits.length + ayahResults.length;

  if (displayTotal === 0) {
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

  const courses = filteredDirectHits.filter((hit) => hit.type === 'course');
  const otherHits = filteredDirectHits.filter((hit) => hit.type !== 'course');
  const hasCourses = courses.length > 0 && showCourses;

  return (
    <div className={`w-full mx-auto mt-8 pb-12 ${hasCourses ? 'max-w-6xl' : 'max-w-4xl'}`}>
      {/* Results header */}
      <div className="flex items-center justify-between mb-6">
        <div className="flex items-center gap-2">
          <Sparkles className="w-5 h-5" style={{ color: '#8B6F5C' }} />
          <h2 className="text-lg font-semibold text-warm-800">
            Results for "{results.query}"
          </h2>
        </div>
        <span className="text-sm text-warm-500">
          {displayTotal} {displayTotal === 1 ? 'result' : 'results'}
        </span>
      </div>

      {/* Two-column layout: main content + courses sidebar */}
      <div className={`flex flex-col ${hasCourses ? 'lg:flex-row lg:gap-6' : ''}`}>
        {/* Main content column */}
        <div className={hasCourses ? 'lg:flex-1 lg:min-w-0' : 'w-full'}>
          {/* Ayah-centric results */}
          {ayahResults.length > 0 && (
            <div className="mb-8">
              <h3 className="text-sm font-semibold text-warm-500 uppercase tracking-wide mb-4">
                Quranic Verses
              </h3>
              <div className="space-y-4">
                {ayahResults.map((ayah) => (
                  <AyahCard
                    key={ayah.ayah_key}
                    result={ayah}
                    visibleSpaces={activeSpaces}
                    postCategory={activePostCategory}
                  />
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
