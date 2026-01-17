import { BookOpen, Search, MessageSquare, Library } from 'lucide-react';

interface QuickActionsProps {
  onAction: (query: string) => void;
}

const actions = [
  { label: 'Explain Verse', icon: BookOpen, query: 'interpretation of ayah' },
  { label: 'Find Hadith', icon: Search, query: 'hadith about' },
  { label: 'Meaning Elaboration', icon: MessageSquare, query: 'explain meaning of' },
  { label: 'Explore Books', icon: Library, query: 'tafsir ibn kathir' },
];

export function QuickActions({ onAction }: QuickActionsProps) {
  return (
    <div className="flex flex-wrap justify-center gap-3 mt-6">
      {actions.map((action) => (
        <button
          key={action.label}
          onClick={() => onAction(action.query)}
          className="flex items-center gap-2 px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-200 rounded-lg hover:bg-gray-50 hover:border-gray-300 transition-colors shadow-sm"
        >
          <action.icon className="w-4 h-4 text-gray-500" />
          {action.label}
        </button>
      ))}
    </div>
  );
}
