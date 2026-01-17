import { BookOpen, ChevronDown } from 'lucide-react';

export function Header() {
  return (
    <header className="w-full bg-white border-b border-warm-200">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16">
          {/* Logo */}
          <div className="flex items-center gap-2">
            <BookOpen className="w-8 h-8" style={{ color: '#8B6F5C' }} />
            <span className="text-xl font-semibold text-warm-800">
              Quran.com
            </span>
          </div>

          {/* Navigation */}
          <nav className="hidden md:flex items-center gap-6">
            <button className="flex items-center gap-1 text-sm font-medium text-warm-600 hover:text-warm-800">
              Browse
              <ChevronDown className="w-4 h-4" />
            </button>
            <button className="flex items-center gap-1 text-sm font-medium text-warm-600 hover:text-warm-800">
              About
              <ChevronDown className="w-4 h-4" />
            </button>
            <button className="flex items-center gap-1 text-sm font-medium text-warm-600 hover:text-warm-800">
              Contribute
              <ChevronDown className="w-4 h-4" />
            </button>
          </nav>

          {/* Mobile menu button */}
          <button className="md:hidden p-2 text-warm-500 hover:text-warm-700">
            <svg
              className="w-6 h-6"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M4 6h16M4 12h16M4 18h16"
              />
            </svg>
          </button>
        </div>
      </div>
    </header>
  );
}
