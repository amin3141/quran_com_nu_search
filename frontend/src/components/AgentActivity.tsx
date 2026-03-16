import { Bot, Search, Sparkles } from 'lucide-react';
import type { AgentToolCall } from '../types';

interface AgentActivityProps {
  isLoading: boolean;
  statusLines: string[];
  toolCalls: AgentToolCall[];
}

export function AgentActivity({
  isLoading,
  statusLines,
  toolCalls,
}: AgentActivityProps) {
  if (!isLoading && statusLines.length === 0 && toolCalls.length === 0) {
    return null;
  }

  const latestStatus = statusLines[0];

  return (
    <div className="w-full max-w-4xl mx-auto mt-6">
      <div className="bg-white rounded-2xl shadow-md border border-warm-200 overflow-hidden">
        <div className="px-4 py-3 border-b border-warm-200 bg-warm-50 flex items-center gap-2">
          <Bot className="w-4 h-4 text-warm-600" />
          <span className="text-sm font-semibold text-warm-700">Search Agent</span>
          {isLoading && (
            <span className="ml-auto inline-flex items-center gap-2 text-xs text-warm-500">
              <span className="w-2 h-2 rounded-full bg-[--color-primary] animate-pulse" />
              Active
            </span>
          )}
        </div>

        <div className="px-4 py-4 space-y-4">
          {latestStatus && (
            <div className="flex items-start gap-3">
              <Sparkles className="w-4 h-4 mt-0.5 text-warm-500" />
              <p className="text-sm text-warm-700">{latestStatus}</p>
            </div>
          )}

          {toolCalls.length > 0 && (
            <div className="space-y-3">
              {toolCalls.slice().reverse().map((toolCall) => (
                <div
                  key={`${toolCall.step}-${toolCall.action}-${toolCall.query}`}
                  className="rounded-xl border border-warm-200 bg-warm-50/70 p-3"
                >
                  <div className="flex items-center gap-2 text-sm text-warm-700">
                    <Search className="w-4 h-4 text-warm-500" />
                    <span className="font-medium">Step {toolCall.step}</span>
                    <span className="text-warm-500">{toolCall.action}</span>
                    <span className="ml-auto text-xs text-warm-500">
                      {toolCall.resultCount} hits, {toolCall.newResultCount} new
                    </span>
                  </div>
                  <p className="mt-2 text-sm text-warm-600">
                    Searching {toolCall.spaces.join(', ')} for "{toolCall.query}"
                  </p>
                  {toolCall.forcedReason && (
                    <p className="mt-1 text-xs text-warm-500 italic">
                      {toolCall.forcedReason}
                    </p>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
