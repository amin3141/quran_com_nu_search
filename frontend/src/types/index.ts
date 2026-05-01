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
  surah_name_arabic?: string | null;
  surah_name_transliteration?: string | null;
  surah_type?: string | null;
  surah_total_verses?: number | null;
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

export interface ResultPreview {
  type: SpaceType | string;
  title: string;
  ayahKey?: string | null;
  url: string;
  score: number;
}

export interface AgentToolCall {
  step: number;
  thought?: string | null;
  action: string;
  query: string;
  spaces: string[];
  limit: number;
  resultCount: number;
  newResultCount: number;
  forced: boolean;
  forcedReason?: string | null;
  preview: ResultPreview[];
}

export interface AgentMetadata {
  mode: string;
  plannerModel?: string | null;
  steps: number;
  usedLlmPlanner: boolean;
  usedHeuristicFallback: boolean;
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
  toolCalls?: AgentToolCall[];
  agent?: AgentMetadata | null;
}

export interface FilterOptions {
  spaces: SpaceType[];
  language: string;
  postCategory?: PostCategory | null;
}
