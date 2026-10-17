// Η λογική παραγωγής ιδεών. Δεν αγγίζει το DOM, ώστε να ελέγχεται με Node.
(function (root) {
  'use strict';

  const PLATFORMS = {
    instagram: {
      name: 'Instagram',
      formats: ['reel', 'carousel', 'story', 'post'],
      maxHashtags: 5,
      tip: 'Η πρώτη εικόνα και η πρώτη γραμμή της λεζάντας κρίνουν αν θα σταματήσει κάποιος να σκρολάρει.'
    },
    tiktok: {
      name: 'TikTok',
      formats: ['reel'],
      maxHashtags: 4,
      tip: 'Βάλε τον γάντζο στα πρώτα 2 δευτερόλεπτα και κράτα ρυθμό μέχρι το τέλος.'
    },
    youtube: {
      name: 'YouTube',
      formats: ['reel', 'video'],
      maxHashtags: 3,
      tip: 'Απάντησε από την αρχή και κάνε τον τίτλο να προκαλεί περιέργεια χωρίς clickbait.'
    },
    linkedin: {
      name: 'LinkedIn',
      formats: ['post', 'carousel'],
      maxHashtags: 3,
      tip: 'Μοιράσου προσωπική εμπειρία ή ένα μάθημα. Η διαφήμιση μειώνει την εμβέλεια.'
    },
    facebook: {
      name: 'Facebook',
      formats: ['post', 'reel', 'story'],
      maxHashtags: 3,
      tip: 'Οι ερωτήσεις και οι ιστορίες παράγουν σχόλια, και τα σχόλια αυξάνουν την εμβέλεια.'
    },
    x: {
      name: 'X',
      formats: ['thread', 'post'],
      maxHashtags: 2,
      tip: 'Σύντομες, καθαρές προτάσεις. Το thread δίνει χώρο για βάθος.'
    }
  };

  const FORMAT_LABELS = {
    reel: 'Σύντομο βίντεο (Reel / Short / TikTok)',
    video: 'Βίντεο μεγαλύτερης διάρκειας',
    carousel: 'Carousel (πολλαπλές διαφάνειες)',
    story: 'Story',
    post: 'Στατική ανάρτηση',
    thread: 'Thread'
  };

  const GOALS = {
    growth: { name: 'Ανάπτυξη ακολούθων', cta: 'Ακολούθησε για περισσότερα τέτοια.' },
    engagement: { name: 'Αλληλεπίδραση', cta: 'Γράψε στα σχόλια τη δική σου εμπειρία.' },
    sales: { name: 'Πωλήσεις', cta: 'Δες τον σύνδεσμο στο προφίλ για να το δοκιμάσεις.' },
    awareness: { name: 'Γνωστοποίηση', cta: 'Στείλε το σε κάποιον που θα το χρειαστεί.' }
  };

  const TONES = {
    friendly: { name: 'Φιλικός', prefix: '' },
    funny: { name: 'Χιουμοριστικός', prefix: 'Να το πω χωρίς ντροπή: ' },
    educational: { name: 'Εκπαιδευτικός', prefix: 'Σύντομο μάθημα: ' },
    inspiring: { name: 'Εμπνευστικός', prefix: 'Θυμήσου: ' }
  };

  // Κάθε γωνία είναι ένας τύπος περιεχομένου. Το {t} αντικαθίσταται με το θέμα του χρήστη.
  const ANGLES = [
    {
      id: 'mistakes',
      title: 'Τα 3 συχνότερα λάθη: {t}',
      hook: 'Αν κάνεις αυτά τα λάθη στο «{t}», χάνεις χρόνο και αποτελέσματα.',
      outline: ['Λάθος #1 και γιατί συμβαίνει', 'Λάθος #2 και πώς το διορθώνεις', 'Λάθος #3 και η σωστή προσέγγιση']
    },
    {
      id: 'howto',
      title: 'Οδηγός 3 βημάτων: {t}',
      hook: 'Το «{t}» φαίνεται δύσκολο, αλλά με 3 βήματα μπορείς να ξεκινήσεις σήμερα.',
      outline: ['Βήμα 1: το βασικό που χρειάζεσαι', 'Βήμα 2: εφάρμοσέ το σε μία πραγματική περίπτωση', 'Βήμα 3: μέτρα το αποτέλεσμα και επανάλαβε']
    },
    {
      id: 'myth',
      title: 'Μύθος ή αλήθεια; {t}',
      hook: 'Το ακούς συνέχεια για το «{t}». Ισχύει πραγματικά;',
      outline: ['Ο μύθος που ακούγεται περισσότερο', 'Τι ισχύει στην πράξη', 'Τι να κάνεις στη θέση του']
    },
    {
      id: 'beforeafter',
      title: 'Πριν και μετά: {t}',
      hook: 'Αυτό ήταν πριν. Αυτό είναι μετά από μία εβδομάδα με το «{t}».',
      outline: ['Το σημείο εκκίνησης, ειλικρινά', 'Τι άλλαξα και γιατί', 'Το αποτέλεσμα και τι θα έκανα διαφορετικά']
    },
    {
      id: 'tool',
      title: 'Ένα εργαλείο που βοηθά: {t}',
      hook: 'Αν δουλεύεις με το «{t}», αυτό το εργαλείο θα σου γλιτώσει ώρες.',
      outline: ['Το πρόβλημα που λύνει', 'Πώς το χρησιμοποιείς σε 60 δευτερόλεπτα', 'Για ποιον αξίζει και για ποιον όχι']
    },
    {
      id: 'faq',
      title: 'Οι 3 ερωτήσεις που ακούω συχνότερα: {t}',
      hook: 'Απαντάω στις ερωτήσεις που μου στέλνετε για το «{t}».',
      outline: ['Ερώτηση #1 με σύντομη απάντηση', 'Ερώτηση #2 με παράδειγμα', 'Ερώτηση #3 με τη δική μου γνώμη']
    },
    {
      id: 'behind',
      title: 'Πίσω από τις κάμερες: {t}',
      hook: 'Δείχνω πώς είναι στην πράξη μια μέρα με το «{t}», χωρίς φίλτρα.',
      outline: ['Το πρωί: η ρουτίνα', 'Το μέσα της μέρας: το δύσκολο κομμάτι', 'Το βράδυ: τι κέρδισα']
    },
    {
      id: 'challenge',
      title: 'Challenge 7 ημερών: {t}',
      hook: 'Για 7 μέρες θα κάνω κάτι καθημερινά με το «{t}». Ακολούθησέ με για το αποτέλεσμα.',
      outline: ['Μέρα 1: το ξεκίνημα', 'Μέρες 2 έως 6: η πρόοδος', 'Μέρα 7: ο απολογισμός']
    },
    {
      id: 'question',
      title: 'Ερώτηση στο κοινό: {t}',
      hook: 'Ψηφίστε στα σχόλια: ποιο είναι το δυσκολότερο κομμάτι του «{t}» για εσάς;',
      outline: ['Παρουσίασε δύο επιλογές', 'Δώσε τη δική σου γνώμη', 'Ζήτα από το κοινό να εξηγήσει την επιλογή του']
    },
    {
      id: 'story',
      title: 'Η ιστορία που με έμαθε: {t}',
      hook: 'Η πιο χρήσιμη μου μάθηση για το «{t}» ήρθε από ένα λάθος.',
      outline: ['Το λάθος και η στιγμή που το κατάλαβα', 'Η αντίδρασή μου', 'Το μάθημα που κρατάς']
    },
    {
      id: 'lessons',
      title: 'Τα 5 πράγματα που θα ήθελα να ξέρω: {t}',
      hook: 'Αν ξεκινούσα από το μηδέν με το «{t}», αυτά θα έκανα διαφορετικά.',
      outline: ['Πράγμα #1 και #2', 'Πράγμα #3 και #4', 'Πράγμα #5, το πιο σημαντικό']
    },
    {
      id: 'comparison',
      title: 'Αρχάριος ή επαγγελματίας: {t}',
      hook: 'Η διαφορά στο «{t}» δεν είναι το ταλέντο. Είναι αυτά τα 3 πράγματα.',
      outline: ['Τι κάνει ο αρχάριος', 'Τι κάνει ο επαγγελματίας', 'Πώς περνάς από το ένα στο άλλο']
    }
  ];

  const DEFAULT_COUNT = 6;

  function fill(template, topic) {
    return template.split('{t}').join(topic);
  }

  function lowerFirst(text) {
    return text.charAt(0).toLowerCase() + text.slice(1);
  }

  function slugify(text) {
    return text.toLowerCase().split(/[^\p{L}\p{N}]+/u).filter(Boolean).join('-');
  }

  function unique(items) {
    return items.filter((item, index) => items.indexOf(item) === index);
  }

  function shuffle(items, rng) {
    const copy = items.slice();
    for (let i = copy.length - 1; i > 0; i -= 1) {
      const j = Math.floor(rng() * (i + 1));
      [copy[i], copy[j]] = [copy[j], copy[i]];
    }
    return copy;
  }

  // Hashtags από το θέμα: η ένωση των λέξεων και κάθε λέξη χωριστά.
  function buildHashtags(topic, maxCount) {
    const words = topic
      .toLowerCase()
      .split(/[^\p{L}\p{N}]+/u)
      .filter(word => word.length > 2);
    const joined = words.join('');
    const tags = joined ? ['#' + joined] : [];
    words.forEach(word => tags.push('#' + word));
    return unique(tags).slice(0, maxCount);
  }

  function generateIdeas(options) {
    const opts = options || {};
    const topic = String(opts.topic || '').trim().replace(/\s+/g, ' ');
    if (!topic) {
      throw new Error('Γράψε ένα θέμα ή κλάδο για να βρούμε ιδέες.');
    }

    const platform = PLATFORMS[opts.platform];
    if (!platform) {
      throw new Error('Επίλεξε πλατφόρμα.');
    }

    const goal = GOALS[opts.goal] || GOALS.growth;
    const tone = TONES[opts.tone] || TONES.friendly;
    const allowedFormat = platform.formats.includes(opts.format);
    const formats = allowedFormat ? [opts.format] : platform.formats;
    const count = Math.max(1, Math.min(opts.count || DEFAULT_COUNT, ANGLES.length));
    const rng = opts.rng || Math.random;

    const angles = shuffle(ANGLES, rng).slice(0, count);

    return angles.map((angle, index) => {
      const format = formats[index % formats.length];
      const hook = fill(angle.hook, topic);
      return {
        id: [angle.id, opts.platform, format, slugify(topic)].join('|'),
        topic,
        platform: opts.platform,
        platformName: platform.name,
        format,
        formatLabel: FORMAT_LABELS[format],
        angle: angle.id,
        title: fill(angle.title, topic),
        hook: tone.prefix ? tone.prefix + lowerFirst(hook) : hook,
        outline: angle.outline.map(line => fill(line, topic)),
        cta: goal.cta,
        hashtags: buildHashtags(topic, platform.maxHashtags),
        tip: platform.tip
      };
    });
  }

  function toPlainText(idea) {
    return [
      idea.title,
      '',
      'Γάντζος: ' + idea.hook,
      '',
      ...idea.outline.map((line, index) => (index + 1) + '. ' + line),
      '',
      'Κάλεσμα για δράση: ' + idea.cta,
      idea.hashtags.join(' ')
    ].join('\n');
  }

  function isSaved(list, id) {
    return list.some(idea => idea.id === id);
  }

  function addSaved(list, idea) {
    return isSaved(list, idea.id) ? list : [idea].concat(list);
  }

  function removeSaved(list, id) {
    return list.filter(idea => idea.id !== id);
  }

  const api = {
    PLATFORMS,
    FORMAT_LABELS,
    GOALS,
    TONES,
    ANGLES,
    generateIdeas,
    buildHashtags,
    toPlainText,
    isSaved,
    addSaved,
    removeSaved
  };

  if (typeof module !== 'undefined' && module.exports) {
    module.exports = api;
  }
  root.ContentIdeas = api;
})(typeof window !== 'undefined' ? window : globalThis);
