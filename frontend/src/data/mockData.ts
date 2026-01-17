import type {
  QuranResult,
  TranslationResult,
  TafsirResult,
  PostResult,
  CourseResult,
  ArticleResult,
  SearchResponse,
  ConsolidatedAyahResult,
} from '../types';

// Ayat al-Kursi (2:255)
const ayatAlKursiQuran: QuranResult = {
  type: 'quran',
  ayah: 255,
  ayah_key: '2:255',
  surah: 2,
  text: 'اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ ۚ لَا تَأْخُذُهُ سِنَةٌ وَلَا نَوْمٌ ۚ لَّهُ مَا فِي السَّمَاوَاتِ وَمَا فِي الْأَرْضِ ۗ مَن ذَا الَّذِي يَشْفَعُ عِندَهُ إِلَّا بِإِذْنِهِ ۚ يَعْلَمُ مَا بَيْنَ أَيْدِيهِمْ وَمَا خَلْفَهُمْ ۖ وَلَا يُحِيطُونَ بِشَيْءٍ مِّنْ عِلْمِهِ إِلَّا بِمَا شَاءَ ۚ وَسِعَ كُرْسِيُّهُ السَّمَاوَاتِ وَالْأَرْضَ ۖ وَلَا يَئُودُهُ حِفْظُهُمَا ۚ وَهُوَ الْعَلِيُّ الْعَظِيمُ',
  edition_id: 'quran-uthmani',
  edition_type: 'quran',
  lang: 'ar',
  name: 'Uthmani Script',
  url: 'https://quran.com/2/255',
  score: 0.98,
};

const ayatAlKursiTranslations: TranslationResult[] = [
  {
    type: 'translation',
    ayah: 255,
    ayah_key: '2:255',
    surah: 2,
    text: 'Allah - there is no deity except Him, the Ever-Living, the Self-Sustaining. Neither drowsiness overtakes Him nor sleep. To Him belongs whatever is in the heavens and whatever is on the earth. Who is it that can intercede with Him except by His permission? He knows what is before them and what will be after them, and they encompass not a thing of His knowledge except for what He wills. His Kursi extends over the heavens and the earth, and their preservation tires Him not. And He is the Most High, the Most Great.',
    author: 'Saheeh International',
    edition_id: 'en-sahih-international',
    lang: 'en',
    name: 'Saheeh International',
    url: 'https://quran.com/2/255?translations=20',
    score: 0.95,
  },
  {
    type: 'translation',
    ayah: 255,
    ayah_key: '2:255',
    surah: 2,
    text: 'Allah! There is no god but He - the Living, The Self-subsisting, Eternal. No slumber can seize Him Nor Sleep. His are all things In the heavens and on earth. Who is there can intercede In His presence except As he permitteth? He knoweth What (appeareth to His creatures As) Before or After or Behind them. Nor shall they compass Aught of his knowledge Except as He willeth. His throne doth extend Over the heavens And on earth, and He feeleth No fatigue in guarding And preserving them, For He is the Most High, The Supreme (in glory).',
    author: 'Abdullah Yusuf Ali',
    edition_id: 'en-yusuf-ali',
    lang: 'en',
    name: 'Yusuf Ali',
    url: 'https://quran.com/2/255?translations=22',
    score: 0.92,
  },
];

const ayatAlKursiTafsir: TafsirResult[] = [
  {
    type: 'tafsir',
    ayah: 255,
    ayah_key: '2:255',
    surah: 2,
    text: 'This is Ayat al-Kursi, the Verse of the Throne. It is the greatest verse in the Quran. The Prophet (peace be upon him) said that it is the master of the verses of the Quran. It describes Allah\'s attributes of life, self-subsistence, dominion, knowledge, and power. The Kursi (footstool/throne) encompasses the heavens and the earth, demonstrating the vastness of Allah\'s dominion.',
    author: 'Ibn Kathir',
    edition_id: 'ar-tafsir-ibn-kathir',
    lang: 'en',
    name: 'Tafsir Ibn Kathir',
    url: 'https://quran.com/2/255/tafsirs/en-tafisr-ibn-kathir',
    score: 0.90,
  },
];

// Patience verse (2:153)
const patienceQuran: QuranResult = {
  type: 'quran',
  ayah: 153,
  ayah_key: '2:153',
  surah: 2,
  text: 'يَا أَيُّهَا الَّذِينَ آمَنُوا اسْتَعِينُوا بِالصَّبْرِ وَالصَّلَاةِ ۚ إِنَّ اللَّهَ مَعَ الصَّابِرِينَ',
  edition_id: 'quran-uthmani',
  edition_type: 'quran',
  lang: 'ar',
  name: 'Uthmani Script',
  url: 'https://quran.com/2/153',
  score: 0.94,
};

const patienceTranslations: TranslationResult[] = [
  {
    type: 'translation',
    ayah: 153,
    ayah_key: '2:153',
    surah: 2,
    text: 'O you who have believed, seek help through patience and prayer. Indeed, Allah is with the patient.',
    author: 'Saheeh International',
    edition_id: 'en-sahih-international',
    lang: 'en',
    name: 'Saheeh International',
    url: 'https://quran.com/2/153?translations=20',
    score: 0.91,
  },
];

const patienceTafsir: TafsirResult[] = [
  {
    type: 'tafsir',
    ayah: 153,
    ayah_key: '2:153',
    surah: 2,
    text: 'Allah commands the believers to seek help through patience (sabr) and prayer (salah) in all their affairs. Patience here refers to restraining oneself from what is harmful and persevering in what is beneficial. The combination of patience and prayer is powerful because patience strengthens the heart while prayer connects one to Allah.',
    author: 'Ibn Kathir',
    edition_id: 'ar-tafsir-ibn-kathir',
    lang: 'en',
    name: 'Tafsir Ibn Kathir',
    url: 'https://quran.com/2/153/tafsirs/en-tafisr-ibn-kathir',
    score: 0.88,
  },
];

// Al-Fatiha (1:1-7)
const fatihaQuran: QuranResult = {
  type: 'quran',
  ayah: 1,
  ayah_key: '1:1',
  surah: 1,
  text: 'بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ',
  edition_id: 'quran-uthmani',
  edition_type: 'quran',
  lang: 'ar',
  name: 'Uthmani Script',
  url: 'https://quran.com/1/1',
  score: 0.85,
};

// Sample posts
const samplePosts: PostResult[] = [
  {
    type: 'post',
    post_id: 'post-001',
    reflection_id: 'ref-001',
    text: 'Ayat al-Kursi reminds us that Allah is the Ever-Living, the Self-Sustaining. When we feel overwhelmed by life, this verse brings peace to our hearts knowing that the One who sustains all creation is watching over us.',
    username: 'thoughtful_muslim',
    display_name: 'Thoughtful Muslim',
    ayah_keys: ['2:255'],
    surahs: [2],
    category: 'reflection',
    likes_count: 234,
    created_at: '2024-03-15T10:30:00Z',
    url: 'https://quran.com/posts/post-001',
    score: 0.87,
  },
  {
    type: 'post',
    post_id: 'post-002',
    reflection_id: 'ref-002',
    text: 'Patience in Islam is not passive waiting. It is actively persevering while trusting in Allah\'s plan. The verse "Indeed, Allah is with the patient" gives us strength knowing we are never alone in our struggles.',
    username: 'seeker_of_truth',
    display_name: 'Seeker of Truth',
    ayah_keys: ['2:153'],
    surahs: [2],
    category: 'reflection',
    likes_count: 189,
    created_at: '2024-02-20T14:15:00Z',
    url: 'https://quran.com/posts/post-002',
    score: 0.85,
  },
  {
    type: 'post',
    post_id: 'post-003',
    reflection_id: 'ref-003',
    text: 'Ramadan is the month of patience, gratitude, and spiritual renewal. Every fast we keep is an exercise in patience, building our connection with Allah.',
    username: 'ramadan_reflections',
    display_name: 'Ramadan Reflections',
    ayah_keys: ['2:183', '2:185'],
    surahs: [2],
    category: 'reflection',
    likes_count: 456,
    created_at: '2024-03-10T08:00:00Z',
    url: 'https://quran.com/posts/post-003',
    score: 0.82,
  },
  {
    type: 'post',
    post_id: 'post-004',
    reflection_id: 'ref-004',
    text: 'Patience with family is one of the greatest tests. The Quran reminds us to be patient and kind with our parents and loved ones, even when it is difficult.',
    username: 'family_values',
    display_name: 'Family Values',
    ayah_keys: ['17:23', '17:24'],
    surahs: [17],
    category: 'reflection',
    likes_count: 312,
    created_at: '2024-01-25T16:45:00Z',
    url: 'https://quran.com/posts/post-004',
    score: 0.80,
  },
];

// Sample courses
const sampleCourses: CourseResult[] = [
  {
    type: 'course',
    course_id: 'course-001',
    course_title: 'Understanding Ayat al-Kursi',
    course_slug: 'understanding-ayat-al-kursi',
    lesson_id: 'lesson-001',
    lesson_title: 'The Greatest Verse in the Quran',
    lesson_slug: 'greatest-verse',
    text: 'In this lesson, we explore why Ayat al-Kursi is considered the greatest verse in the Quran. We examine its meaning, virtues mentioned in hadith, and practical applications in daily life.',
    lang: 'en',
    tags: ['ayat-al-kursi', 'tawhid', 'protection'],
    url: 'https://quran.com/courses/understanding-ayat-al-kursi/greatest-verse',
    score: 0.89,
  },
  {
    type: 'course',
    course_id: 'course-002',
    course_title: 'Patience in the Quran',
    course_slug: 'patience-in-quran',
    lesson_id: 'lesson-002',
    lesson_title: 'Types of Patience (Sabr)',
    lesson_slug: 'types-of-patience',
    text: 'This lesson covers the three types of patience in Islam: patience in obeying Allah, patience in avoiding sins, and patience during trials. Learn how to cultivate each type.',
    lang: 'en',
    tags: ['patience', 'sabr', 'character'],
    url: 'https://quran.com/courses/patience-in-quran/types-of-patience',
    score: 0.86,
  },
  {
    type: 'course',
    course_id: 'course-003',
    course_title: 'Ramadan Preparation',
    course_slug: 'ramadan-preparation',
    lesson_id: 'lesson-003',
    lesson_title: 'Spiritual Goals for Ramadan',
    lesson_slug: 'spiritual-goals',
    text: 'Prepare for the blessed month of Ramadan by setting meaningful spiritual goals. This lesson helps you plan your worship, Quran reading, and acts of kindness.',
    lang: 'en',
    tags: ['ramadan', 'fasting', 'spirituality'],
    url: 'https://quran.com/courses/ramadan-preparation/spiritual-goals',
    score: 0.84,
  },
];

// Sample articles
const sampleArticles: ArticleResult[] = [
  {
    type: 'article',
    title: 'The Virtues of Ayat al-Kursi',
    slug: 'virtues-ayat-al-kursi',
    text: 'Ayat al-Kursi (Quran 2:255) holds a special place in Islamic tradition. The Prophet Muhammad (peace be upon him) described it as the greatest verse in the Quran. This article explores the spiritual benefits and proper times to recite this powerful verse.',
    url: 'https://quran.com/articles/virtues-ayat-al-kursi',
    score: 0.88,
  },
  {
    type: 'article',
    title: 'Understanding Patience (Sabr) in Islam',
    slug: 'understanding-patience-islam',
    text: 'Patience is one of the most emphasized virtues in the Quran, mentioned over 90 times. This comprehensive guide explains what patience truly means in Islam and how to develop this essential quality.',
    url: 'https://quran.com/articles/understanding-patience-islam',
    score: 0.83,
  },
  {
    type: 'article',
    title: 'Preparing Your Heart for Ramadan',
    slug: 'preparing-heart-ramadan',
    text: 'As the blessed month of Ramadan approaches, learn how to prepare your heart and mind for this spiritual journey. Practical tips and spiritual insights to maximize your Ramadan experience.',
    url: 'https://quran.com/articles/preparing-heart-ramadan',
    score: 0.79,
  },
];

// Helper to create consolidated ayah results
function createConsolidatedAyah(
  quran: QuranResult,
  translations: TranslationResult[],
  tafsirs: TafsirResult[],
  posts: PostResult[],
  courses: CourseResult[],
  articles: ArticleResult[]
): ConsolidatedAyahResult {
  const allScores = [
    quran.score,
    ...translations.map((t) => t.score),
    ...tafsirs.map((t) => t.score),
    ...posts.map((p) => p.score),
    ...courses.map((c) => c.score),
    ...articles.map((a) => a.score),
  ];

  return {
    ayah_key: quran.ayah_key,
    surah: quran.surah,
    ayah: quran.ayah,
    quran,
    translations,
    tafsirs,
    posts,
    courses,
    articles,
    topScore: Math.max(...allScores),
  };
}

// Mock search function
export function mockSearch(query: string): SearchResponse {
  const lowerQuery = query.toLowerCase();
  const results: SearchResponse = {
    query,
    directHits: [],
    ayahResults: [],
    totalResults: 0,
  };

  // Check for Ayat al-Kursi related queries
  if (
    lowerQuery.includes('kursi') ||
    lowerQuery.includes('2:255') ||
    lowerQuery.includes('throne')
  ) {
    const relatedPosts = samplePosts.filter((p) =>
      p.ayah_keys.includes('2:255')
    );
    const relatedCourses = sampleCourses.filter(
      (c) =>
        c.text.toLowerCase().includes('kursi') ||
        c.tags.includes('ayat-al-kursi')
    );
    const relatedArticles = sampleArticles.filter(
      (a) =>
        a.text.toLowerCase().includes('kursi') ||
        a.title.toLowerCase().includes('kursi')
    );

    results.ayahResults.push(
      createConsolidatedAyah(
        ayatAlKursiQuran,
        ayatAlKursiTranslations,
        ayatAlKursiTafsir,
        relatedPosts,
        relatedCourses,
        relatedArticles
      )
    );
  }

  // Check for patience related queries
  if (
    lowerQuery.includes('patience') ||
    lowerQuery.includes('sabr') ||
    lowerQuery.includes('patient')
  ) {
    const relatedPosts = samplePosts.filter(
      (p) =>
        p.text.toLowerCase().includes('patience') ||
        p.text.toLowerCase().includes('patient')
    );
    const relatedCourses = sampleCourses.filter(
      (c) =>
        c.text.toLowerCase().includes('patience') ||
        c.tags.includes('patience') ||
        c.tags.includes('sabr')
    );
    const relatedArticles = sampleArticles.filter(
      (a) =>
        a.text.toLowerCase().includes('patience') ||
        a.title.toLowerCase().includes('patience')
    );

    results.ayahResults.push(
      createConsolidatedAyah(
        patienceQuran,
        patienceTranslations,
        patienceTafsir,
        relatedPosts,
        relatedCourses,
        relatedArticles
      )
    );

    // Add direct hits for patience with family
    if (lowerQuery.includes('family')) {
      const familyPosts = samplePosts.filter((p) =>
        p.text.toLowerCase().includes('family')
      );
      results.directHits.push(...familyPosts);
    }
  }

  // Check for Ramadan related queries
  if (lowerQuery.includes('ramadan') || lowerQuery.includes('fasting')) {
    const ramadanPosts = samplePosts.filter(
      (p) =>
        p.text.toLowerCase().includes('ramadan') ||
        p.ayah_keys.some((k) => k.startsWith('2:183') || k.startsWith('2:185'))
    );
    const ramadanCourses = sampleCourses.filter(
      (c) =>
        c.text.toLowerCase().includes('ramadan') || c.tags.includes('ramadan')
    );
    const ramadanArticles = sampleArticles.filter(
      (a) =>
        a.text.toLowerCase().includes('ramadan') ||
        a.title.toLowerCase().includes('ramadan')
    );

    results.directHits.push(...ramadanPosts, ...ramadanCourses, ...ramadanArticles);
  }

  // Check for tafsir ibn kathir queries
  if (
    lowerQuery.includes('tafsir') ||
    lowerQuery.includes('ibn kathir') ||
    lowerQuery.includes('interpretation')
  ) {
    // If asking about specific verse interpretation
    if (lowerQuery.includes('2:255') || lowerQuery.includes('kursi')) {
      results.ayahResults.push(
        createConsolidatedAyah(
          ayatAlKursiQuran,
          ayatAlKursiTranslations,
          ayatAlKursiTafsir,
          [],
          [],
          []
        )
      );
    }
    if (lowerQuery.includes('patience') || lowerQuery.includes('baqarah')) {
      results.ayahResults.push(
        createConsolidatedAyah(
          patienceQuran,
          patienceTranslations,
          patienceTafsir,
          [],
          [],
          []
        )
      );
    }
  }

  // Check for reflections
  if (lowerQuery.includes('reflection')) {
    const reflectionPosts = samplePosts.filter(
      (p) => p.category === 'reflection'
    );
    results.directHits.push(...reflectionPosts);
  }

  // Generic fallback - show some results
  if (results.ayahResults.length === 0 && results.directHits.length === 0) {
    // Return Al-Fatiha and some general content
    results.ayahResults.push(
      createConsolidatedAyah(
        fatihaQuran,
        [
          {
            type: 'translation',
            ayah: 1,
            ayah_key: '1:1',
            surah: 1,
            text: 'In the name of Allah, the Entirely Merciful, the Especially Merciful.',
            author: 'Saheeh International',
            edition_id: 'en-sahih-international',
            lang: 'en',
            name: 'Saheeh International',
            url: 'https://quran.com/1/1?translations=20',
            score: 0.7,
          },
        ],
        [],
        [],
        [],
        []
      )
    );
    results.directHits.push(...samplePosts.slice(0, 2), ...sampleArticles.slice(0, 1));
  }

  // Remove duplicates from direct hits
  const seenIds = new Set<string>();
  results.directHits = results.directHits.filter((hit) => {
    const id =
      hit.type === 'post'
        ? hit.post_id
        : hit.type === 'course'
          ? hit.course_id
          : hit.slug;
    if (seenIds.has(id)) return false;
    seenIds.add(id);
    return true;
  });

  // Sort by score
  results.directHits.sort((a, b) => b.score - a.score);
  results.ayahResults.sort((a, b) => b.topScore - a.topScore);

  results.totalResults =
    results.directHits.length + results.ayahResults.length;

  return results;
}

// Surah names for display
export const surahNames: Record<number, { arabic: string; english: string; transliteration: string }> = {
  1: { arabic: 'الفاتحة', english: 'The Opening', transliteration: 'Al-Fatiha' },
  2: { arabic: 'البقرة', english: 'The Cow', transliteration: 'Al-Baqarah' },
  17: { arabic: 'الإسراء', english: 'The Night Journey', transliteration: 'Al-Isra' },
};
