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
    <div className="bg-white rounded-xl shadow-md border border-warm-200 overflow-hidden">
      <div className="p-5">
        <div className="flex items-start gap-4">
          <div className="w-10 h-10 rounded-full flex items-center justify-center flex-shrink-0" style={{ backgroundColor: '#E8E4DE' }}>
            <MessageSquare className="w-5 h-5" style={{ color: '#8B6F5C' }} />
          </div>
          <div className="flex-1 min-w-0">
            <div className="flex items-center gap-2 mb-2">
              <span className="text-sm font-semibold text-warm-800">
                {post.display_name}
              </span>
              <span className="text-sm text-warm-500">@{post.username}</span>
              <span className="px-2 py-0.5 text-xs font-medium rounded-full" style={{ backgroundColor: '#F5F3F0', color: '#8B6F5C' }}>
                Reflection
              </span>
            </div>
            <p className="text-warm-700 leading-relaxed select-text">{post.text}</p>
            <div className="flex items-center gap-4 mt-3 text-sm text-warm-500">
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
          <a
            href={post.url}
            target="_blank"
            rel="noopener noreferrer"
            className="p-2 rounded-lg hover:bg-warm-100 transition-colors flex-shrink-0"
            title="Open in new tab"
          >
            <ExternalLink className="w-4 h-4 text-warm-400" />
          </a>
        </div>
      </div>
    </div>
  );
}

function CourseCard({ course }: { course: CourseResult }) {
  return (
    <div className="bg-white rounded-xl shadow-md border border-warm-200 overflow-hidden h-full flex flex-col">
      <div className="p-3 sm:p-4 flex-1">
        <div className="flex items-center gap-2 mb-2">
          <span className="inline-flex items-center gap-1 px-2 py-0.5 text-xs font-medium rounded-full" style={{ backgroundColor: '#E8F5F1', color: '#4A7C6F' }}>
            <GraduationCap className="w-3 h-3" />
            Course
          </span>
          <div className="flex-1" />
          <a
            href={course.url}
            target="_blank"
            rel="noopener noreferrer"
            className="p-1.5 rounded-lg hover:bg-warm-100 transition-colors flex-shrink-0"
            title="Open in new tab"
          >
            <ExternalLink className="w-4 h-4 text-warm-400" />
          </a>
        </div>
        <a
          href={course.url}
          target="_blank"
          rel="noopener noreferrer"
          className="text-base font-semibold text-warm-800 mb-1 hover:underline block"
        >
          {course.course_title}
        </a>
        <p className="text-sm font-medium mb-2" style={{ color: '#8B6F5C' }}>
          Lesson: {course.lesson_title}
        </p>
        <p className="text-warm-600 text-sm leading-relaxed select-text">
          {course.text}
        </p>
        {course.tags.length > 0 && (
          <div className="flex flex-wrap gap-1.5 mt-3">
            {course.tags.map((tag) => (
              <span
                key={tag}
                className="px-2 py-0.5 text-xs bg-warm-100 text-warm-600 rounded-full"
              >
                {tag}
              </span>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

function ArticleCard({ article }: { article: ArticleResult }) {
  return (
    <div className="bg-white rounded-xl shadow-md border border-warm-200 overflow-hidden">
      <div className="p-5">
        <div className="flex items-start gap-4">
          <div className="w-10 h-10 rounded-full flex items-center justify-center flex-shrink-0" style={{ backgroundColor: '#FDF8E8' }}>
            <FileText className="w-5 h-5" style={{ color: '#C9A86C' }} />
          </div>
          <div className="flex-1 min-w-0">
            <div className="flex items-center gap-2 mb-1">
              <span className="px-2 py-0.5 text-xs font-medium rounded-full" style={{ backgroundColor: '#FDF8E8', color: '#A68B4B' }}>
                Article
              </span>
            </div>
            <a
              href={article.url}
              target="_blank"
              rel="noopener noreferrer"
              className="text-lg font-semibold text-warm-800 mb-2 hover:underline block"
            >
              {article.title}
            </a>
            <p className="text-warm-600 text-sm leading-relaxed select-text">
              {article.text}
            </p>
          </div>
          <a
            href={article.url}
            target="_blank"
            rel="noopener noreferrer"
            className="p-2 rounded-lg hover:bg-warm-100 transition-colors flex-shrink-0"
            title="Open in new tab"
          >
            <ExternalLink className="w-4 h-4 text-warm-400" />
          </a>
        </div>
      </div>
    </div>
  );
}
