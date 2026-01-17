import {
  MessageSquare,
  GraduationCap,
  FileText,
  ExternalLink,
  Heart,
  Calendar,
  Tag,
} from 'lucide-react';
import type { PostResult, CourseResult, ArticleResult } from '../types';

type DirectHit = PostResult | CourseResult | ArticleResult;

interface DirectHitCardProps {
  result: DirectHit;
}

function formatDate(dateString: string): string {
  const date = new Date(dateString);
  return date.toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  });
}

export function DirectHitCard({ result }: DirectHitCardProps) {
  if (result.type === 'post') {
    return <PostCard post={result} />;
  }

  if (result.type === 'course') {
    return <CourseCard course={result} />;
  }

  return <ArticleCard article={result} />;
}

function PostCard({ post }: { post: PostResult }) {
  return (
    <a
      href={post.url}
      target="_blank"
      rel="noopener noreferrer"
      className="block bg-white rounded-xl shadow-md border border-gray-200 overflow-hidden hover:shadow-lg transition-shadow"
    >
      <div className="p-5">
        <div className="flex items-start gap-4">
          <div className="w-10 h-10 rounded-full bg-blue-100 flex items-center justify-center flex-shrink-0">
            <MessageSquare className="w-5 h-5 text-blue-600" />
          </div>
          <div className="flex-1 min-w-0">
            <div className="flex items-center gap-2 mb-2">
              <span className="text-sm font-semibold text-gray-900">
                {post.display_name}
              </span>
              <span className="text-sm text-gray-500">@{post.username}</span>
              <span className="px-2 py-0.5 text-xs font-medium bg-blue-100 text-blue-700 rounded-full">
                Reflection
              </span>
            </div>
            <p className="text-gray-700 leading-relaxed">{post.text}</p>
            <div className="flex items-center gap-4 mt-3 text-sm text-gray-500">
              <span className="flex items-center gap-1">
                <Heart className="w-4 h-4" />
                {post.likes_count}
              </span>
              <span className="flex items-center gap-1">
                <Calendar className="w-4 h-4" />
                {formatDate(post.created_at)}
              </span>
              {post.ayah_keys.length > 0 && (
                <span className="flex items-center gap-1">
                  <Tag className="w-4 h-4" />
                  {post.ayah_keys.join(', ')}
                </span>
              )}
            </div>
          </div>
          <ExternalLink className="w-4 h-4 text-gray-400 flex-shrink-0" />
        </div>
      </div>
    </a>
  );
}

function CourseCard({ course }: { course: CourseResult }) {
  return (
    <a
      href={course.url}
      target="_blank"
      rel="noopener noreferrer"
      className="block bg-white rounded-xl shadow-md border border-gray-200 overflow-hidden hover:shadow-lg transition-shadow"
    >
      <div className="p-5">
        <div className="flex items-start gap-4">
          <div className="w-10 h-10 rounded-full bg-purple-100 flex items-center justify-center flex-shrink-0">
            <GraduationCap className="w-5 h-5 text-purple-600" />
          </div>
          <div className="flex-1 min-w-0">
            <div className="flex items-center gap-2 mb-1">
              <span className="px-2 py-0.5 text-xs font-medium bg-purple-100 text-purple-700 rounded-full">
                Course
              </span>
            </div>
            <h3 className="text-lg font-semibold text-gray-900 mb-1">
              {course.course_title}
            </h3>
            <p className="text-sm text-[--color-primary] font-medium mb-2">
              Lesson: {course.lesson_title}
            </p>
            <p className="text-gray-600 text-sm leading-relaxed line-clamp-2">
              {course.text}
            </p>
            {course.tags.length > 0 && (
              <div className="flex flex-wrap gap-1.5 mt-3">
                {course.tags.map((tag) => (
                  <span
                    key={tag}
                    className="px-2 py-0.5 text-xs bg-gray-100 text-gray-600 rounded-full"
                  >
                    {tag}
                  </span>
                ))}
              </div>
            )}
          </div>
          <ExternalLink className="w-4 h-4 text-gray-400 flex-shrink-0" />
        </div>
      </div>
    </a>
  );
}

function ArticleCard({ article }: { article: ArticleResult }) {
  return (
    <a
      href={article.url}
      target="_blank"
      rel="noopener noreferrer"
      className="block bg-white rounded-xl shadow-md border border-gray-200 overflow-hidden hover:shadow-lg transition-shadow"
    >
      <div className="p-5">
        <div className="flex items-start gap-4">
          <div className="w-10 h-10 rounded-full bg-green-100 flex items-center justify-center flex-shrink-0">
            <FileText className="w-5 h-5 text-green-600" />
          </div>
          <div className="flex-1 min-w-0">
            <div className="flex items-center gap-2 mb-1">
              <span className="px-2 py-0.5 text-xs font-medium bg-green-100 text-green-700 rounded-full">
                Article
              </span>
            </div>
            <h3 className="text-lg font-semibold text-gray-900 mb-2">
              {article.title}
            </h3>
            <p className="text-gray-600 text-sm leading-relaxed line-clamp-3">
              {article.text}
            </p>
          </div>
          <ExternalLink className="w-4 h-4 text-gray-400 flex-shrink-0" />
        </div>
      </div>
    </a>
  );
}
