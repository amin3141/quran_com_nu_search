import { Sparkles } from 'lucide-react';
import type { AiOverview as AiOverviewType } from '../types';

interface AiOverviewProps {
  overview: AiOverviewType;
}

export function AiOverview({ overview }: AiOverviewProps) {
  return (
    <div className="mb-8 w-full max-w-[72ch] mx-auto ai-overview-container">
      <div className="ai-overview-inner">
        <div className="px-6 pt-5">
          <div className="ai-badge ai-badge-shimmer inline-flex items-center gap-2 rounded-full px-3 py-1.5 text-[11px] font-semibold uppercase tracking-[0.22em]" style={{ color: '#6B5A7A' }}>
            <Sparkles className="sparkle-icon h-3.5 w-3.5" style={{ color: '#8B6F9C' }} />
            <span className="relative z-10">AI Overview</span>
          </div>
        </div>
        <div className="px-6 pt-4 pb-6">
          <p className="text-[15px] leading-7 text-warm-800 md:text-[17px] whitespace-pre-line text-pretty">
            {overview.text}
          </p>
          <p className="mt-4 text-xs italic" style={{ color: '#9A8A9E' }}>
            Generated from retrieved sources. Verify details in the results below.
          </p>
        </div>
      </div>
    </div>
  );
}
