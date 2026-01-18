interface QuickActionsProps {
  onAction: (query: string) => void;
}

const queries = [
  'patience in adversity',
  'ayat al-kursi',
  'kindness to parents',
  'stories of prophets',
];

export function QuickActions({ onAction }: QuickActionsProps) {
  return (
    <div className="flex flex-wrap justify-center gap-2 mt-6">
      {queries.map((query) => (
        <button
          key={query}
          onClick={() => onAction(query)}
          className="px-4 py-2 text-sm font-medium text-warm-600 bg-white border border-warm-200 rounded-full hover:bg-warm-50 hover:border-warm-300 transition-colors"
        >
          {query}
        </button>
      ))}
    </div>
  );
}
