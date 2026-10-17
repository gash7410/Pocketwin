const test = require('node:test');
const assert = require('node:assert/strict');
const ideas = require('../js/ideas.js');

// Ντετερμινιστικό rng, ώστε τα τεστ να είναι επαναλήψιμα.
function seeded(seed) {
  let state = seed;
  return () => {
    state = (state * 1664525 + 1013904223) % 4294967296;
    return state / 4294967296;
  };
}

test('generateIdeas returns the requested number of ideas', () => {
  const list = ideas.generateIdeas({ topic: 'καφές', platform: 'instagram', count: 6, rng: seeded(1) });
  assert.equal(list.length, 6);
});

test('generateIdeas caps count at the number of available angles', () => {
  const list = ideas.generateIdeas({ topic: 'καφές', platform: 'instagram', count: 999, rng: seeded(1) });
  assert.equal(list.length, ideas.ANGLES.length);
});

test('generateIdeas never repeats an angle within one batch', () => {
  const list = ideas.generateIdeas({ topic: 'καφές', platform: 'instagram', count: 12, rng: seeded(7) });
  const angles = new Set(list.map(idea => idea.angle));
  assert.equal(angles.size, list.length);
});

test('generateIdeas rejects an empty topic', () => {
  assert.throws(() => ideas.generateIdeas({ topic: '   ', platform: 'tiktok' }), /θέμα/);
});

test('generateIdeas rejects an unknown platform', () => {
  assert.throws(() => ideas.generateIdeas({ topic: 'καφές', platform: 'myspace' }), /πλατφόρμα/);
});

test('generateIdeas only uses formats that the platform supports', () => {
  const list = ideas.generateIdeas({ topic: 'καφές', platform: 'linkedin', count: 12, rng: seeded(3) });
  const allowed = ideas.PLATFORMS.linkedin.formats;
  list.forEach(idea => assert.ok(allowed.includes(idea.format), idea.format));
});

test('a specific format is honored when the platform supports it', () => {
  const list = ideas.generateIdeas({ topic: 'καφές', platform: 'instagram', format: 'story', count: 4, rng: seeded(5) });
  list.forEach(idea => assert.equal(idea.format, 'story'));
});

test('an unsupported format falls back to the platform formats', () => {
  const list = ideas.generateIdeas({ topic: 'καφές', platform: 'tiktok', format: 'carousel', count: 3, rng: seeded(5) });
  list.forEach(idea => assert.equal(idea.format, 'reel'));
});

test('topic placeholders are fully replaced', () => {
  const list = ideas.generateIdeas({ topic: 'καφές', platform: 'instagram', count: 12, rng: seeded(9) });
  list.forEach(idea => {
    const text = [idea.title, idea.hook, ...idea.outline].join(' ');
    assert.ok(!text.includes('{t}'), text);
  });
});

test('tone prefix is applied and the hook is lowercased after it', () => {
  const list = ideas.generateIdeas({ topic: 'καφές', platform: 'instagram', tone: 'funny', count: 1, rng: seeded(2) });
  assert.ok(list[0].hook.startsWith('Να το πω χωρίς ντροπή: '), list[0].hook);
});

test('friendly tone leaves the hook untouched', () => {
  const list = ideas.generateIdeas({ topic: 'καφές', platform: 'instagram', tone: 'friendly', count: 12, rng: seeded(2) });
  list.forEach(idea => assert.match(idea.hook, /^[^a-zα-ω]/u, idea.hook));
});

test('the call to action follows the chosen goal', () => {
  const list = ideas.generateIdeas({ topic: 'καφές', platform: 'instagram', goal: 'sales', count: 2, rng: seeded(4) });
  list.forEach(idea => assert.equal(idea.cta, ideas.GOALS.sales.cta));
});

test('hashtags respect the platform limit and carry the topic', () => {
  const list = ideas.generateIdeas({ topic: 'ψητό καφέ σπιτιού', platform: 'x', count: 1, rng: seeded(6) });
  assert.ok(list[0].hashtags.length <= ideas.PLATFORMS.x.maxHashtags);
  assert.equal(list[0].hashtags[0], '#ψητόκαφέσπιτιού');
});

test('buildHashtags drops short words and duplicates', () => {
  assert.deepEqual(ideas.buildHashtags('ο καφές', 5), ['#καφές']);
  assert.deepEqual(ideas.buildHashtags('καφές', 5), ['#καφές']);
  assert.deepEqual(ideas.buildHashtags('fitness', 5), ['#fitness']);
});

test('ids are stable for the same angle, platform, format and topic', () => {
  const a = ideas.generateIdeas({ topic: 'Καφές', platform: 'tiktok', count: 1, rng: seeded(8) })[0];
  const b = ideas.generateIdeas({ topic: 'καφές', platform: 'tiktok', count: 1, rng: seeded(8) })[0];
  assert.equal(a.id, b.id);
});

test('saved list adds once and removes by id', () => {
  const idea = ideas.generateIdeas({ topic: 'καφές', platform: 'tiktok', count: 1, rng: seeded(8) })[0];
  let list = ideas.addSaved([], idea);
  list = ideas.addSaved(list, idea);
  assert.equal(list.length, 1);
  assert.equal(ideas.isSaved(list, idea.id), true);
  list = ideas.removeSaved(list, idea.id);
  assert.equal(list.length, 0);
});

test('toPlainText contains the title, hook, outline, cta and hashtags', () => {
  const idea = ideas.generateIdeas({ topic: 'καφές', platform: 'instagram', count: 1, rng: seeded(8) })[0];
  const text = ideas.toPlainText(idea);
  assert.ok(text.includes(idea.title));
  assert.ok(text.includes(idea.hook));
  assert.ok(text.includes(idea.outline[0]));
  assert.ok(text.includes(idea.cta));
  assert.ok(text.includes(idea.hashtags[0]));
});
