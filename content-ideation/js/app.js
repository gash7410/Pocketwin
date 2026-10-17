(function () {
  'use strict';

  const I = window.ContentIdeas;
  const STORAGE_KEY = 'contentIdeas.saved.v1';

  const els = {
    form: document.getElementById('idea-form'),
    topic: document.getElementById('topic'),
    platform: document.getElementById('platform'),
    format: document.getElementById('format'),
    goal: document.getElementById('goal'),
    tone: document.getElementById('tone'),
    error: document.getElementById('form-error'),
    tip: document.getElementById('platform-tip'),
    regenerate: document.getElementById('regenerate'),
    results: document.getElementById('results'),
    saved: document.getElementById('saved'),
    savedCount: document.getElementById('saved-count'),
    toast: document.getElementById('toast'),
    tabs: document.querySelectorAll('[role="tab"]'),
    panels: {
      ideas: document.getElementById('panel-ideas'),
      saved: document.getElementById('panel-saved')
    }
  };

  const state = {
    options: null,
    ideas: [],
    saved: loadSaved()
  };

  // Το localStorage μπορεί να λείπει ή να μπλοκάρεται, οπότε η εφαρμογή λειτουργεί και χωρίς αυτό.
  function loadSaved() {
    try {
      const parsed = JSON.parse(localStorage.getItem(STORAGE_KEY) || '[]');
      return Array.isArray(parsed) ? parsed : [];
    } catch (error) {
      return [];
    }
  }

  function persistSaved() {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(state.saved));
    } catch (error) {
      showToast('Δεν ήταν δυνατή η αποθήκευση στη συσκευή.');
    }
  }

  function el(tag, className, text) {
    const node = document.createElement(tag);
    if (className) node.className = className;
    if (text !== undefined) node.textContent = text;
    return node;
  }

  function fillSelect(select, entries, selectedValue) {
    select.replaceChildren(...entries.map(([value, label]) => {
      const option = el('option', null, label);
      option.value = value;
      option.selected = value === selectedValue;
      return option;
    }));
  }

  function renderFormats(platformKey, selected) {
    const platform = I.PLATFORMS[platformKey];
    const entries = [['all', 'Όλες οι μορφές']].concat(
      platform.formats.map(format => [format, I.FORMAT_LABELS[format]])
    );
    fillSelect(els.format, entries, selected || 'all');
    els.tip.textContent = platform.tip;
  }

  function initForm() {
    fillSelect(
      els.platform,
      Object.entries(I.PLATFORMS).map(([key, value]) => [key, value.name]),
      'instagram'
    );
    renderFormats('instagram');

    fillSelect(
      els.goal,
      Object.entries(I.GOALS).map(([key, value]) => [key, value.name]),
      'growth'
    );
    fillSelect(
      els.tone,
      Object.entries(I.TONES).map(([key, value]) => [key, value.name]),
      'friendly'
    );

    els.platform.addEventListener('change', () => renderFormats(els.platform.value));
  }

  function showError(message) {
    els.error.textContent = message;
    els.error.hidden = !message;
  }

  function readOptions() {
    return {
      topic: els.topic.value,
      platform: els.platform.value,
      format: els.format.value,
      goal: els.goal.value,
      tone: els.tone.value,
      count: 6
    };
  }

  function generate() {
    showError('');
    const options = readOptions();
    try {
      state.options = options;
      state.ideas = I.generateIdeas(options);
    } catch (error) {
      showError(error.message);
      return;
    }
    els.regenerate.hidden = false;
    renderIdeas();
  }

  function renderIdeas() {
    if (!state.ideas.length) {
      els.results.replaceChildren(el('p', 'empty', 'Δεν βρέθηκαν ιδέες. Δοκίμασε άλλο θέμα.'));
      return;
    }
    els.results.replaceChildren(...state.ideas.map(idea => renderCard(idea, {
      onSave: () => toggleSaved(idea),
      isSaved: () => I.isSaved(state.saved, idea.id)
    })));
  }

  function renderSaved() {
    els.savedCount.textContent = String(state.saved.length);
    if (!state.saved.length) {
      els.saved.replaceChildren(el('p', 'empty', 'Δεν έχεις αποθηκευμένες ιδέες ακόμη.'));
      return;
    }
    els.saved.replaceChildren(...state.saved.map(idea => renderCard(idea, {
      onSave: () => toggleSaved(idea),
      isSaved: () => true
    })));
  }

  function renderCard(idea, handlers) {
    const card = el('article', 'idea');

    const head = el('div', 'idea-head');
    head.append(el('span', 'chip', idea.platformName), el('span', 'chip', idea.formatLabel));

    const actions = el('div', 'idea-actions');
    const copyButton = el('button', 'small', 'Αντιγραφή');
    copyButton.type = 'button';
    copyButton.addEventListener('click', () => copyIdea(idea));

    const saveButton = el('button', 'small');
    saveButton.type = 'button';
    const refreshSaveLabel = () => {
      const saved = handlers.isSaved();
      saveButton.textContent = saved ? 'Αφαίρεση από αποθηκευμένες' : 'Αποθήκευση';
      saveButton.setAttribute('aria-pressed', String(saved));
    };
    saveButton.addEventListener('click', () => {
      handlers.onSave();
      refreshSaveLabel();
    });
    refreshSaveLabel();

    actions.append(copyButton, saveButton);

    const hook = el('p', 'hook');
    hook.append(el('strong', null, 'Γάντζος: '), document.createTextNode(idea.hook));

    const outline = el('ol');
    idea.outline.forEach(line => outline.append(el('li', null, line)));

    card.append(
      head,
      el('h2', null, idea.title),
      hook,
      outline,
      el('p', 'cta', 'Κάλεσμα για δράση: ' + idea.cta),
      el('p', 'tags', idea.hashtags.join(' ')),
      el('p', 'tip', idea.tip),
      actions
    );
    return card;
  }

  function toggleSaved(idea) {
    state.saved = I.isSaved(state.saved, idea.id)
      ? I.removeSaved(state.saved, idea.id)
      : I.addSaved(state.saved, idea);
    persistSaved();
    renderSaved();
  }

  async function copyIdea(idea) {
    const text = I.toPlainText(idea);
    try {
      await navigator.clipboard.writeText(text);
      showToast('Η ιδέα αντιγράφηκε.');
    } catch (error) {
      showToast('Δεν ήταν δυνατή η αντιγραφή.');
    }
  }

  let toastTimer = null;
  function showToast(message) {
    els.toast.textContent = message;
    els.toast.hidden = false;
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => {
      els.toast.hidden = true;
    }, 2200);
  }

  function selectView(view) {
    els.tabs.forEach(tab => {
      const active = tab.dataset.view === view;
      tab.setAttribute('aria-selected', String(active));
    });
    Object.entries(els.panels).forEach(([key, panel]) => {
      panel.hidden = key !== view;
    });
    if (view === 'saved') renderSaved();
  }

  els.form.addEventListener('submit', event => {
    event.preventDefault();
    generate();
  });

  els.regenerate.addEventListener('click', () => {
    if (!state.options) return;
    showError('');
    state.ideas = I.generateIdeas(state.options);
    renderIdeas();
  });

  els.tabs.forEach(tab => {
    tab.addEventListener('click', () => selectView(tab.dataset.view));
  });

  initForm();
  renderSaved();
})();
