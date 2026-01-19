-- 初始词库数据
USE newlingoflow;

INSERT INTO
    words (
        word,
        phonetic,
        meaning_cn,
        meaning_en,
        example_sentence,
        difficulty
    )
VALUES (
        'ephemeral',
        '/ɪˈfemərəl/',
        '短暂的，转瞬即逝的',
        'lasting for a very short time',
        'Fame is often ephemeral.',
        'hard'
    ),
    (
        'ubiquitous',
        '/juːˈbɪkwɪtəs/',
        '无处不在的',
        'present everywhere',
        'Smartphones have become ubiquitous.',
        'hard'
    ),
    (
        'serendipity',
        '/ˌserənˈdɪpəti/',
        '意外发现',
        'the occurrence of events by chance',
        'Finding that book was pure serendipity.',
        'hard'
    ),
    (
        'eloquent',
        '/ˈeləkwənt/',
        '雄辩的，有说服力的',
        'fluent or persuasive in speaking',
        'She gave an eloquent speech.',
        'medium'
    ),
    (
        'resilient',
        '/rɪˈzɪliənt/',
        '有弹性的，能恢复的',
        'able to recover quickly',
        'Children are remarkably resilient.',
        'medium'
    ),
    (
        'meticulous',
        '/məˈtɪkjələs/',
        '一丝不苟的',
        'showing great attention to detail',
        'He is meticulous about his work.',
        'medium'
    ),
    (
        'profound',
        '/prəˈfaʊnd/',
        '深刻的，意义深远的',
        'very great or intense',
        'The book had a profound impact on me.',
        'medium'
    ),
    (
        'ambiguous',
        '/æmˈbɪɡjuəs/',
        '模棱两可的',
        'open to more than one interpretation',
        'His answer was deliberately ambiguous.',
        'medium'
    ),
    (
        'pragmatic',
        '/præɡˈmætɪk/',
        '务实的，实用主义的',
        'dealing with things sensibly',
        'We need a pragmatic approach.',
        'medium'
    ),
    (
        'innovative',
        '/ˈɪnəveɪtɪv/',
        '创新的',
        'featuring new methods',
        'They developed innovative solutions.',
        'easy'
    ),
    (
        'collaborate',
        '/kəˈlæbəreɪt/',
        '合作，协作',
        'work jointly on an activity',
        'We collaborate with other teams.',
        'easy'
    ),
    (
        'enhance',
        '/ɪnˈhɑːns/',
        '增强，提高',
        'intensify, increase, or further improve',
        'This will enhance your skills.',
        'easy'
    ),
    (
        'implement',
        '/ˈɪmplɪment/',
        '实施，执行',
        'put into effect',
        'We will implement the plan next week.',
        'easy'
    ),
    (
        'integrate',
        '/ˈɪntɪɡreɪt/',
        '整合，使成为一体',
        'combine one thing with another',
        'We need to integrate these systems.',
        'easy'
    ),
    (
        'sustainable',
        '/səˈsteɪnəbl/',
        '可持续的',
        'able to be maintained',
        'We need sustainable development.',
        'medium'
    ),
    (
        'comprehensive',
        '/ˌkɒmprɪˈhensɪv/',
        '全面的，综合的',
        'complete, including all elements',
        'This is a comprehensive guide.',
        'medium'
    ),
    (
        'contemporary',
        '/kənˈtempərəri/',
        '当代的，同时代的',
        'living or occurring at the same time',
        'Contemporary art is fascinating.',
        'medium'
    ),
    (
        'substantial',
        '/səbˈstænʃl/',
        '大量的，实质的',
        'of considerable importance or size',
        'We made substantial progress.',
        'medium'
    ),
    (
        'versatile',
        '/ˈvɜːsətaɪl/',
        '多才多艺的，多功能的',
        'able to adapt to many functions',
        'She is a versatile performer.',
        'medium'
    ),
    (
        'anticipate',
        '/ænˈtɪsɪpeɪt/',
        '预期，期望',
        'regard as probable; expect',
        'We anticipate strong demand.',
        'easy'
    ),
    (
        'demonstrate',
        '/ˈdemənstreɪt/',
        '证明，演示',
        'clearly show the existence of',
        'Let me demonstrate how it works.',
        'easy'
    ),
    (
        'elaborate',
        '/ɪˈlæbərət/',
        '详尽的，精心制作的',
        'involving many carefully arranged parts',
        'She gave an elaborate explanation.',
        'medium'
    ),
    (
        'facilitate',
        '/fəˈsɪlɪteɪt/',
        '促进，使便利',
        'make an action easier',
        'This will facilitate communication.',
        'medium'
    ),
    (
        'inevitable',
        '/ɪnˈevɪtəbl/',
        '不可避免的',
        'certain to happen; unavoidable',
        'Change is inevitable.',
        'medium'
    ),
    (
        'legitimate',
        '/lɪˈdʒɪtɪmət/',
        '合法的，正当的',
        'conforming to the law or rules',
        'This is a legitimate concern.',
        'medium'
    ),
    (
        'predominant',
        '/prɪˈdɒmɪnənt/',
        '主要的，占优势的',
        'present as the strongest element',
        'Blue is the predominant color.',
        'hard'
    ),
    (
        'spontaneous',
        '/spɒnˈteɪniəs/',
        '自发的，自然的',
        'performed without being planned',
        'It was a spontaneous decision.',
        'medium'
    ),
    (
        'subsequent',
        '/ˈsʌbsɪkwənt/',
        '随后的，后来的',
        'coming after something in time',
        'Subsequent events proved him right.',
        'medium'
    ),
    (
        'tremendous',
        '/trɪˈmendəs/',
        '巨大的，惊人的',
        'very great in amount or intensity',
        'We achieved tremendous success.',
        'easy'
    );

-- ========================================
-- Phase 6: 词典系统初始数据
-- ========================================
INSERT INTO
    dictionaries (
        name,
        description,
        total_words
    )
VALUES ('CET-4', '大学英语四级词汇', 4500),
    ('CET-6', '大学英语六级词汇', 6000),
    ('IELTS', '雅思词汇', 8000),
    ('TOEFL', '托福词汇', 10000)
ON DUPLICATE KEY UPDATE
    description = VALUES(description),
    total_words = VALUES(total_words);