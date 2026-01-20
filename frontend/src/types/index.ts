export type SpaceType = 'quran' | 'translation' | 'tafsir' | 'post' | 'course' | 'article';
export type PostCategory = 'reflection';

export interface QuranResult {
  type: 'quran';
  ayah: number;
  ayah_key: string;
  surah: number;
  text: string;
  edition_id: string;
  edition_type: string;
  lang: string;
  name: string;
  url: string;
  score: number;
}

export interface TranslationResult {
  type: 'translation';
  ayah: number;
  ayah_key: string;
  surah: number;
  text: string;
  author: string;
  edition_id: string;
  lang: string;
  name: string;
  url: string;
  score: number;
}

export interface TafsirResult {
  type: 'tafsir';
  ayah: number;
  ayah_key: string;
  surah: number;
  text: string;
  author: string;
  edition_id: string;
  lang: string;
  name: string;
  url: string;
  score: number;
}

export interface PostResult {
  type: 'post';
  post_id: string;
  reflection_id: string;
  text: string;
  username: string;
  display_name: string;
  ayah_keys: string[];
  surahs: number[];
  category: string;
  likes_count: number;
  created_at: string;
  url: string;
  score: number;
}

export interface CourseResult {
  type: 'course';
  course_id: string;
  course_title: string;
  course_slug: string;
  lesson_id: string;
  lesson_title: string;
  lesson_slug: string;
  text: string;
  lang: string;
  tags: string[];
  url: string;
  score: number;
}

export interface ArticleResult {
  type: 'article';
  title: string;
  slug: string;
  text: string;
  url: string;
  score: number;
}

export type SearchResult =
  | QuranResult
  | TranslationResult
  | TafsirResult
  | PostResult
  | CourseResult
  | ArticleResult;

export interface AiOverview {
  text: string;
}

export interface ConsolidatedAyahResult {
  ayah_key: string;
  surah: number;
  ayah: number;
  quran: QuranResult | null;
  translations: TranslationResult[];
  tafsirs: TafsirResult[];
  posts: PostResult[];
  courses: CourseResult[];
  articles: ArticleResult[];
  topScore: number;
}

export interface SearchResponse {
  query: string;
  aiOverview?: AiOverview | null;
  directHits: (PostResult | CourseResult | ArticleResult)[];
  ayahResults: ConsolidatedAyahResult[];
  totalResults: number;
}

export interface FilterOptions {
  spaces: SpaceType[];
  language: string;
  postCategory?: PostCategory | null;
}
