import { useState } from 'react';
import {
  ChevronDown,
  ChevronUp,
  BookOpen,
  MessageSquare,
  GraduationCap,
  FileText,
  ExternalLink,
} from 'lucide-react';
import type { ConsolidatedAyahResult } from '../types';
import { surahNames } from '../data/mockData';

interface AyahCardProps {
  result: ConsolidatedAyahResult;
}

export function AyahCard({ result }: AyahCardProps) {
  const [expanded, setExpanded] = useState(false);

  const surahInfo = surahNames[result.surah] || {
    arabic: '',
    english: `Surah ${result.surah}`,
    transliteration: `Surah ${result.surah}`,
  };

  const totalRelated =
    result.tafsirs.length +
    result.posts.length +
    result.courses.length +
    result.articles.length;

  return (
    <div className="bg-white rounded-xl shadow-md border border-gray-200 overflow-hidden">
      {/* Header with Surah info */}
      <div className="px-5 py-3 bg-gradient-to-r from-[--color-primary] to-[--color-primary-dark] text-white">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <span className="text-lg font-semibold">{result.ayah_key}</span>
            <span className="text-sm opacity-90">
              {surahInfo.transliteration} ({surahInfo.english})
            </span>
          </div>
          <a
            href={result.quran?.url || '#'}
            target="_blank"
            rel="noopener noreferrer"
            className="flex items-center gap-1 text-sm opacity-90 hover:opacity-100 transition-opacity"
          >
            View on Quran.com
            <ExternalLink className="w-3.5 h-3.5" />
          </a>
        </div>
      </div>

      {/* Quran text */}
      {result.quran && (
        <div className="px-5 py-4 border-b border-gray-100">
          <p className="arabic-text text-xl md:text-2xl text-gray-900 text-right leading-loose">
            {result.quran.text}
          </p>
        </div>
      )}

      {/* Best translation */}
      {result.translations.length > 0 && (
        <div className="px-5 py-4 border-b border-gray-100 bg-gray-50">
          <div className="flex items-start gap-3">
            <BookOpen className="w-5 h-5 text-[--color-primary] flex-shrink-0 mt-0.5" />
            <div className="flex-1">
              <p className="text-gray-800 leading-relaxed">
                {result.translations[0].text}
              </p>
              <p className="text-sm text-gray-500 mt-2">
                — {result.translations[0].author}
              </p>
            </div>
          </div>
        </div>
      )}

      {/* Tafsir excerpt (if available) */}
      {result.tafsirs.length > 0 && (
        <div className="px-5 py-4 border-b border-gray-100">
          <div className="flex items-start gap-3">
            <MessageSquare className="w-5 h-5 text-[--color-secondary] flex-shrink-0 mt-0.5" />
            <div className="flex-1">
              <p className="text-sm font-medium text-gray-700 mb-1">
                Tafsir: {result.tafsirs[0].name}
              </p>
              <p className="text-gray-600 text-sm leading-relaxed line-clamp-3">
                {result.tafsirs[0].text}
              </p>
            </div>
          </div>
        </div>
      )}

      {/* Related content summary */}
      {totalRelated > 0 && (
        <div className="px-5 py-3 bg-gray-50">
          <button
            onClick={() => setExpanded(!expanded)}
            className="flex items-center justify-between w-full text-left"
          >
            <span className="text-sm font-medium text-gray-700">
              {totalRelated} related{' '}
              {totalRelated === 1 ? 'item' : 'items'}
              <span className="text-gray-500 font-normal ml-2">
                ({result.posts.length > 0 && `${result.posts.length} posts`}
                {result.posts.length > 0 && result.courses.length > 0 && ', '}
                {result.courses.length > 0 && `${result.courses.length} courses`}
                {(result.posts.length > 0 || result.courses.length > 0) &&
                  result.articles.length > 0 &&
                  ', '}
                {result.articles.length > 0 && `${result.articles.length} articles`})
              </span>
            </span>
            {expanded ? (
              <ChevronUp className="w-5 h-5 text-gray-400" />
            ) : (
              <ChevronDown className="w-5 h-5 text-gray-400" />
            )}
          </button>

          {/* Expanded related content */}
          {expanded && (
            <div className="mt-4 space-y-3">
              {/* Posts */}
              {result.posts.map((post) => (
                <a
                  key={post.post_id}
                  href={post.url}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="block p-3 bg-white rounded-lg border border-gray-200 hover:border-gray-300 transition-colors"
                >
                  <div className="flex items-start gap-3">
                    <MessageSquare className="w-4 h-4 text-blue-500 flex-shrink-0 mt-0.5" />
                    <div className="flex-1 min-w-0">
                      <p className="text-sm text-gray-700 line-clamp-2">
                        {post.text}
                      </p>
                      <p className="text-xs text-gray-500 mt-1">
                        by {post.display_name} · {post.likes_count} likes
                      </p>
                    </div>
                  </div>
                </a>
              ))}

              {/* Courses */}
              {result.courses.map((course) => (
                <a
                  key={course.course_id}
                  href={course.url}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="block p-3 bg-white rounded-lg border border-gray-200 hover:border-gray-300 transition-colors"
                >
                  <div className="flex items-start gap-3">
                    <GraduationCap className="w-4 h-4 text-purple-500 flex-shrink-0 mt-0.5" />
                    <div className="flex-1 min-w-0">
                      <p className="text-sm font-medium text-gray-900">
                        {course.course_title}
                      </p>
                      <p className="text-sm text-gray-600 line-clamp-2">
                        {course.lesson_title}
                      </p>
                    </div>
                  </div>
                </a>
              ))}

              {/* Articles */}
              {result.articles.map((article) => (
                <a
                  key={article.slug}
                  href={article.url}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="block p-3 bg-white rounded-lg border border-gray-200 hover:border-gray-300 transition-colors"
                >
                  <div className="flex items-start gap-3">
                    <FileText className="w-4 h-4 text-green-500 flex-shrink-0 mt-0.5" />
                    <div className="flex-1 min-w-0">
                      <p className="text-sm font-medium text-gray-900">
                        {article.title}
                      </p>
                      <p className="text-sm text-gray-600 line-clamp-2">
                        {article.text}
                      </p>
                    </div>
                  </div>
                </a>
              ))}

              {/* Additional translations */}
              {result.translations.length > 1 && (
                <div className="pt-2 border-t border-gray-200">
                  <p className="text-xs font-medium text-gray-500 uppercase tracking-wide mb-2">
                    Other Translations
                  </p>
                  {result.translations.slice(1).map((translation, idx) => (
                    <a
                      key={idx}
                      href={translation.url}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="block p-3 bg-white rounded-lg border border-gray-200 hover:border-gray-300 transition-colors mb-2"
                    >
                      <p className="text-sm text-gray-700 line-clamp-2">
                        {translation.text}
                      </p>
                      <p className="text-xs text-gray-500 mt-1">
                        — {translation.author}
                      </p>
                    </a>
                  ))}
                </div>
              )}
            </div>
          )}
        </div>
      )}
    </div>
  );
}
