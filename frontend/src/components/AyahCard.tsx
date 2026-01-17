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

// Verses that are Huruf Muqatta'at (mysterious letters)
const HURUF_MUQATTAAT_VERSES = new Set([
  '2:1', '3:1', '7:1', '10:1', '11:1', '12:1', '13:1', '14:1', '15:1',
  '19:1', '20:1', '26:1', '27:1', '28:1', '29:1', '30:1', '31:1', '32:1',
  '36:1', '38:1', '40:1', '41:1', '42:1', '43:1', '44:1', '45:1', '46:1',
  '50:1', '68:1',
]);

// Convert Western numerals to Arabic-Indic numerals
function toArabicNumerals(num: number): string {
  const arabicDigits = ['٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩'];
  return num
    .toString()
    .split('')
    .map((digit) => arabicDigits[parseInt(digit, 10)])
    .join('');
}

export function AyahCard({ result }: AyahCardProps) {
  const [expanded, setExpanded] = useState(false);

  const surahInfo = surahNames[result.surah] || {
    arabic: '',
    english: `Surah ${result.surah}`,
    transliteration: `Surah ${result.surah}`,
  };

  const isHurufMuqattaat = HURUF_MUQATTAAT_VERSES.has(result.ayah_key);

  // Count only items shown in expandable section (not tafsirs - they're shown separately)
  // Extra translations (beyond the first) are also shown in expanded section
  const extraTranslations = Math.max(0, result.translations.length - 1);
  const totalRelated =
    result.posts.length +
    result.courses.length +
    result.articles.length +
    extraTranslations;

  // End of ayah marker with Arabic numerals
  const ayahMarker = `\u06DD${toArabicNumerals(result.ayah)}`; // ۝ + number

  return (
    <div className="bg-white rounded-xl shadow-md border border-warm-200 overflow-hidden">
      {/* Header with prominent Surah:Ayah info */}
      <div
        className="px-5 py-3 text-white"
        style={{ background: 'linear-gradient(to right, #8B6F5C, #6B5548)' }}
      >
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            {/* Prominent verse badge */}
            <div className="flex items-center gap-2 bg-white/20 rounded-lg px-3 py-1.5">
              <span className="text-xl font-bold">{result.surah}:{result.ayah}</span>
            </div>
            <div className="flex flex-col">
              <span className="text-sm font-medium">
                {surahInfo.transliteration}
              </span>
              <span className="text-xs opacity-80">
                {surahInfo.english}
              </span>
            </div>
          </div>
          <a
            href={result.quran?.url || `https://quran.com/${result.surah}/${result.ayah}`}
            target="_blank"
            rel="noopener noreferrer"
            className="flex items-center gap-1 px-2 py-1 text-sm bg-white/20 rounded hover:bg-white/30 transition-colors"
          >
            View on Quran.com
            <ExternalLink className="w-3.5 h-3.5" />
          </a>
        </div>
      </div>

      {/* Quran text with end of ayah marker */}
      {result.quran && (
        <div className="px-5 py-6 border-b border-warm-200" style={{ background: 'linear-gradient(to bottom, #FAF9F7, white)' }}>
          <p className="arabic-text text-2xl md:text-3xl text-warm-800 text-right leading-[2.5]">
            {result.quran.text}
            <span className="mr-2" style={{ color: '#8B6F5C' }}>{ayahMarker}</span>
          </p>
        </div>
      )}

      {/* Verse reference bar */}
      <div className="px-5 py-2 bg-warm-100 border-b border-warm-200 flex items-center justify-between">
        <div className="flex items-center gap-4 text-sm text-warm-600">
          <span className="font-medium">
            Surah {surahInfo.transliteration} ({result.surah}), Ayah {result.ayah}
          </span>
          {surahInfo.arabic && (
            <span className="arabic-text text-base">{surahInfo.arabic}</span>
          )}
        </div>
        <span className="text-xs text-warm-500">
          {result.quran?.name || 'Quran'}
        </span>
      </div>

      {/* Best translation */}
      {result.translations.length > 0 ? (
        <div className="px-5 py-4 border-b border-warm-200 bg-warm-50">
          <div className="flex items-start gap-3">
            <BookOpen className="w-5 h-5 flex-shrink-0 mt-0.5" style={{ color: '#8B6F5C' }} />
            <div className="flex-1">
              <p className="text-warm-700 leading-relaxed">
                {result.translations[0].text}
              </p>
              <p className="text-sm text-warm-500 mt-2">
                — {result.translations[0].author}
              </p>
            </div>
          </div>
        </div>
      ) : (
        <div className="px-5 py-3 border-b border-warm-200 bg-warm-50">
          <div className="flex items-start gap-3">
            <BookOpen className="w-5 h-5 text-warm-400 flex-shrink-0 mt-0.5" />
            <p className="text-sm text-warm-500 italic">
              {isHurufMuqattaat
                ? 'These letters (Huruf Muqatta\'at) appear at the beginning of certain surahs. Their meaning is known only to Allah.'
                : 'No translation available for this verse.'}
            </p>
          </div>
        </div>
      )}

      {/* Tafsir excerpt (if available) */}
      {result.tafsirs.length > 0 && (
        <div className="px-5 py-4 border-b border-warm-200">
          <div className="flex items-start gap-3">
            <MessageSquare className="w-5 h-5 flex-shrink-0 mt-0.5" style={{ color: '#4A7C6F' }} />
            <div className="flex-1">
              <p className="text-sm font-medium text-warm-700 mb-1">
                Tafsir: {result.tafsirs[0].name}
              </p>
              <p className="text-warm-600 text-sm leading-relaxed line-clamp-3">
                {result.tafsirs[0].text}
              </p>
            </div>
          </div>
        </div>
      )}

      {/* Related content summary */}
      {totalRelated > 0 && (
        <div className="px-5 py-3 bg-warm-50">
          <button
            onClick={() => setExpanded(!expanded)}
            className="flex items-center justify-between w-full text-left"
          >
            <span className="text-sm font-medium text-warm-700">
              {totalRelated} related{' '}
              {totalRelated === 1 ? 'item' : 'items'}
              <span className="text-warm-500 font-normal ml-2">
                ({[
                  result.posts.length > 0 && `${result.posts.length} posts`,
                  result.courses.length > 0 && `${result.courses.length} courses`,
                  result.articles.length > 0 && `${result.articles.length} articles`,
                  extraTranslations > 0 && `${extraTranslations} more translations`,
                ].filter(Boolean).join(', ')})
              </span>
            </span>
            {expanded ? (
              <ChevronUp className="w-5 h-5 text-warm-400" />
            ) : (
              <ChevronDown className="w-5 h-5 text-warm-400" />
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
                  className="block p-3 bg-white rounded-lg border border-warm-200 hover:border-warm-300 transition-colors"
                >
                  <div className="flex items-start gap-3">
                    <MessageSquare className="w-4 h-4 flex-shrink-0 mt-0.5" style={{ color: '#8B6F5C' }} />
                    <div className="flex-1 min-w-0">
                      <p className="text-sm text-warm-700 line-clamp-2">
                        {post.text}
                      </p>
                      <p className="text-xs text-warm-500 mt-1">
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
                  className="block p-3 bg-white rounded-lg border border-warm-200 hover:border-warm-300 transition-colors"
                >
                  <div className="flex items-start gap-3">
                    <GraduationCap className="w-4 h-4 flex-shrink-0 mt-0.5" style={{ color: '#4A7C6F' }} />
                    <div className="flex-1 min-w-0">
                      <p className="text-sm font-medium text-warm-800">
                        {course.course_title}
                      </p>
                      <p className="text-sm text-warm-600 line-clamp-2">
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
                  className="block p-3 bg-white rounded-lg border border-warm-200 hover:border-warm-300 transition-colors"
                >
                  <div className="flex items-start gap-3">
                    <FileText className="w-4 h-4 flex-shrink-0 mt-0.5" style={{ color: '#C9A86C' }} />
                    <div className="flex-1 min-w-0">
                      <p className="text-sm font-medium text-warm-800">
                        {article.title}
                      </p>
                      <p className="text-sm text-warm-600 line-clamp-2">
                        {article.text}
                      </p>
                    </div>
                  </div>
                </a>
              ))}

              {/* Additional translations */}
              {result.translations.length > 1 && (
                <div className="pt-2 border-t border-warm-200">
                  <p className="text-xs font-medium text-warm-500 uppercase tracking-wide mb-2">
                    Other Translations
                  </p>
                  {result.translations.slice(1).map((translation, idx) => (
                    <a
                      key={idx}
                      href={translation.url}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="block p-3 bg-white rounded-lg border border-warm-200 hover:border-warm-300 transition-colors mb-2"
                    >
                      <p className="text-sm text-warm-700 line-clamp-2">
                        {translation.text}
                      </p>
                      <p className="text-xs text-warm-500 mt-1">
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
