// YuhanCon 화면 동작. CSP(script-src 'self') 때문에 인라인 스크립트 없이 이 파일 하나로 처리한다.
// 스크립트가 없어도 폼 제출·링크는 동작하고, 여기서는 편의 기능만 더한다.
(function () {
  'use strict';
  const $ = (s, el) => (el || document).querySelector(s);
  const $$ = (s, el) => Array.from((el || document).querySelectorAll(s));

  // 모바일 메뉴 열기/닫기
  $$('.nav-toggle').forEach(btn => btn.addEventListener('click', () => {
    const nav = btn.closest('.nav');
    const open = nav.classList.toggle('open');
    btn.setAttribute('aria-expanded', String(open));
    btn.setAttribute('aria-label', open ? '메뉴 닫기' : '메뉴 열기');
  }));

  // 삭제·취소처럼 되돌릴 수 없는 폼은 한 번 더 묻는다
  $$('form[data-confirm]').forEach(f => f.addEventListener('submit', e => {
    if (!window.confirm(f.dataset.confirm)) e.preventDefault();
  }));

  // 변경 폼은 제출 버튼을 한 번만 누를 수 있게(두 번 제출 방지). 확인창에서 취소한 경우는 그대로 둔다.
  $$('form[method="post"]').forEach(f => f.addEventListener('submit', e => {
    // 다른 검사(아이디 확인, 좌석 선택)가 제출을 막았는지는 모든 처리기가 끝난 뒤에 본다
    setTimeout(() => { if (!e.defaultPrevented) $$('button[type="submit"]', f).forEach(b => b.disabled = true); }, 0);
  }));

  // 정렬을 바꾸면 바로 적용
  $$('select[data-autosubmit]').forEach(s => s.addEventListener('change', () => s.form.submit()));

  // 홈 슬라이드: 5초마다 다음 장면. 진행 바를 누르면 그 장면으로 가고 5초를 처음부터 다시 센다.
  const stage = $('.stage');
  if (stage) {
    const slides = $$('.slide', stage), bars = $$('.bar', stage), MS = 5000;
    const reduce = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    let cur = 0, timer = null;
    const show = n => {
      cur = (n + slides.length) % slides.length;
      slides.forEach((s, i) => {
        const on = i === cur;
        s.classList.toggle('on', on);
        s.setAttribute('aria-hidden', String(!on));
        $$('a', s).forEach(a => a.tabIndex = on ? 0 : -1);
      });
      bars.forEach((b, i) => {
        b.classList.remove('on', 'done');
        void b.offsetWidth; // 진행 애니메이션을 처음부터 다시 시작
        if (i < cur) b.classList.add('done');
        if (i === cur) b.classList.add('on');
        b.setAttribute('aria-current', String(i === cur));
      });
      schedule();
    };
    const schedule = () => {
      clearTimeout(timer);
      if (slides.length > 1 && !reduce && !stage.classList.contains('paused')) timer = setTimeout(() => show(cur + 1), MS);
    };
    bars.forEach((b, i) => b.addEventListener('click', () => show(i)));
    // 마우스를 올려도 계속 넘어간다. 키보드(Tab)로 안에 들어온 동안만 멈춘다(마우스 클릭으로 생긴 포커스는 제외).
    const pause = on => { stage.classList.toggle('paused', on); on ? clearTimeout(timer) : show(cur); };
    stage.addEventListener('focusin', e => { if (e.target.matches(':focus-visible')) pause(true); });
    stage.addEventListener('focusout', e => { if (stage.classList.contains('paused') && !stage.contains(e.relatedTarget)) pause(false); });
    document.addEventListener('visibilitychange', () => document.hidden ? clearTimeout(timer) : schedule());
    stage.style.setProperty('--slide-ms', MS + 'ms');
    if (reduce) bars.forEach(b => b.classList.remove('on'));
    schedule();
  }

  // 홈 분류 칩: 미리 받아 둔 분류별 묶음(최대 4개) 중 하나를 보여 준다
  $$('[data-group-tabs]').forEach(tabs => {
    const panels = $$('[data-group-panel]', tabs.parentElement);
    $$('[data-group]', tabs).forEach(chip => chip.addEventListener('click', () => {
      $$('[data-group]', tabs).forEach(c => c.setAttribute('aria-pressed', String(c === chip)));
      panels.forEach(p => { p.hidden = p.dataset.groupPanel !== chip.dataset.group; });
    }));
  });

  // 좌석표: 최대 매수를 넘지 않게 막고, 고른 좌석을 보여 준다
  $$('.seatmap').forEach(map => {
    const max = Number(map.dataset.max || 6);
    const form = map.closest('form');
    const summary = form && $('.seat-summary', form);
    const boxes = $$('input[name="seats"]', map);
    const update = () => {
      const picked = boxes.filter(b => b.checked).map(b => b.value);
      boxes.forEach(b => { if (!b.closest('.taken')) b.disabled = !b.checked && picked.length >= max; });
      if (summary) summary.textContent = picked.length ? picked.join(', ') + ' · ' + picked.length + '석' : summary.dataset.empty;
    };
    boxes.forEach(b => b.addEventListener('change', update));
    if (form) form.addEventListener('submit', e => {
      if (!boxes.some(b => b.checked)) { e.preventDefault(); summary && (summary.textContent = '좌석을 하나 이상 골라 주세요'); }
    });
    update();
  });

  // 공연 등록: 분류 '기타'면 이름 칸, 좌석 지정이면 열 편집기, 아니면 총 인원 칸
  const etcField = $('[data-etc-field]');
  if (etcField) {
    const radios = $$('input[data-etc-toggle]');
    const sync = () => { etcField.hidden = !radios.some(r => r.checked && r.value === 'ETC'); };
    radios.forEach(r => r.addEventListener('change', sync));
    sync();
  }
  const editor = $('[data-seat-editor]');
  if (editor) {
    const free = $('[data-free-seats]'), rows = $('[data-rows]', editor), tpl = $('#row-tpl');
    const toggles = $$('input[data-seat-toggle]');
    const totalEl = $('[data-seat-total]', editor);
    const total = () => {
      totalEl.textContent = $$('input[name="rowCounts"]', rows).reduce((s, i) => s + (Number(i.value) || 0), 0);
    };
    const nextName = () => {
      const used = $$('input[name="rowNames"]', rows).map(i => i.value.toUpperCase());
      for (let c = 65; c <= 90; c++) { const n = String.fromCharCode(c); if (!used.includes(n)) return n; }
      return '';
    };
    const addRow = () => {
      const node = tpl.content.firstElementChild.cloneNode(true);
      $('input[name="rowNames"]', node).value = nextName();
      $('input[name="rowCounts"]', node).value = 10;
      rows.appendChild(node);
      total();
    };
    const sync = () => {
      const seated = toggles.some(t => t.checked && t.value === 'true');
      editor.hidden = !seated;
      free.hidden = seated;
      // 숨긴 칸은 제출하지 않는다
      $$('input', editor).forEach(i => i.disabled = !seated);
      $$('input', free).forEach(i => i.disabled = seated);
      if (seated && !rows.children.length) addRow();
    };
    $('[data-row-add]', editor).addEventListener('click', addRow);
    rows.addEventListener('click', e => {
      if (e.target.matches('[data-row-remove]')) { e.target.closest('.row-input').remove(); total(); }
    });
    rows.addEventListener('input', total);
    toggles.forEach(t => t.addEventListener('change', sync));
    sync();
    total();
  }

  // 회원가입: 아이디 중복 확인
  const signup = $('form[data-signup]');
  if (signup) {
    const input = $('#loginId', signup), btn = $('[data-check-id]', signup), msg = $('#loginId-check', signup);
    let checked = null;
    const say = (text, ok) => { msg.textContent = text; msg.className = 'check-msg ' + (ok ? 'ok' : 'bad'); };
    btn.addEventListener('click', async () => {
      const id = input.value.trim();
      if (!id) { say('아이디를 입력하세요.', false); input.focus(); return; }
      try {
        const res = await fetch('/signup/check-id?loginId=' + encodeURIComponent(id), { headers: { Accept: 'application/json' } });
        const data = await res.json();
        say(data.message, data.available);
        checked = data.available ? id.toLowerCase() : null;
        input.setAttribute('aria-invalid', String(!data.available));
      } catch (e) {
        say('확인하지 못했습니다. 잠시 후 다시 눌러 주세요.', false);
      }
    });
    input.addEventListener('input', () => { checked = null; msg.textContent = ''; });
    signup.addEventListener('submit', e => {
      if (checked !== input.value.trim().toLowerCase()) {
        e.preventDefault();
        say('아이디 중복 확인을 눌러 주세요.', false);
        btn.focus();
      }
    });
  }

  // 관리자: 전체 선택, 슬라이드 최대 개수
  $$('[data-check-all]').forEach(all => {
    const form = all.closest('form');
    const boxes = $$('input[name="' + all.dataset.checkAll + '"]', form).filter(b => b !== all);
    all.addEventListener('change', () => boxes.forEach(b => b.checked = all.checked));
    boxes.forEach(b => b.addEventListener('change', () => { all.checked = boxes.every(x => x.checked); }));
  });
  $$('form[data-max-check]').forEach(form => {
    const max = Number(form.dataset.max), boxes = $$('input[name="' + form.dataset.maxCheck + '"]', form), count = $('[data-max-count]', form);
    const update = () => {
      const n = boxes.filter(b => b.checked).length;
      boxes.forEach(b => b.disabled = !b.checked && n >= max);
      if (count) count.textContent = n;
    };
    boxes.forEach(b => b.addEventListener('change', update));
    update();

    // 홈 슬라이드 순서: 고른 순서대로 쌓고 ↑ ↓로 바꾼다. 제출할 때 이 순서로 ids를 보낸다.
    const orderBox = $('[data-slide-order]', form);
    if (!orderBox) return;
    const list = $('ol', orderBox), empty = $('.order-empty', orderBox);
    let order = (orderBox.dataset.initial || '').split(',').filter(id => boxes.some(b => b.value === id && b.checked));
    boxes.forEach(b => { if (b.checked && !order.includes(b.value)) order.push(b.value); });
    const titleOf = id => boxes.find(b => b.value === id).dataset.title;
    const moveBtn = (label, move, i, disabled) => {
      const btn = document.createElement('button');
      btn.type = 'button';
      btn.className = 'order-btn';
      // 화살표 글자는 Windows에서 이모지로 보여서 SVG로 그린다
      const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
      svg.setAttribute('viewBox', '0 0 24 24');
      svg.setAttribute('aria-hidden', 'true');
      const path = document.createElementNS('http://www.w3.org/2000/svg', 'path');
      path.setAttribute('d', move < 0 ? 'M6 15l6-6 6 6' : 'M6 9l6 6 6-6');
      svg.appendChild(path);
      btn.appendChild(svg);
      btn.dataset.move = move;
      btn.dataset.index = i;
      btn.disabled = disabled;
      btn.setAttribute('aria-label', titleOf(order[i]) + (move < 0 ? ' 위로' : ' 아래로'));
      return btn;
    };
    const render = focus => {
      list.replaceChildren(...order.map((id, i) => {
        const li = document.createElement('li');
        const name = document.createElement('span');
        name.textContent = titleOf(id); // 제목은 글자로만 넣는다
        li.append(name, moveBtn('위로', -1, i, i === 0), moveBtn('아래로', 1, i, i === order.length - 1));
        return li;
      }));
      empty.hidden = order.length > 0;
      if (focus) { const b = list.querySelector('button[data-index="' + focus.i + '"][data-move="' + focus.move + '"]'); (b && !b.disabled ? b : list.querySelector('button:not(:disabled)'))?.focus(); }
    };
    boxes.forEach(b => b.addEventListener('change', () => {
      order = b.checked ? order.concat(b.value) : order.filter(id => id !== b.value);
      render();
    }));
    list.addEventListener('click', e => {
      const btn = e.target.closest('button[data-move]');
      if (!btn) return;
      const i = Number(btn.dataset.index), move = Number(btn.dataset.move), j = i + move;
      if (j < 0 || j >= order.length) return;
      [order[i], order[j]] = [order[j], order[i]];
      render({ i: j, move });
    });
    form.addEventListener('submit', () => {
      boxes.forEach(b => b.removeAttribute('name')); // 체크박스 대신 순서 목록대로 보낸다
      order.forEach(id => {
        const h = document.createElement('input');
        h.type = 'hidden'; h.name = 'ids'; h.value = id;
        form.appendChild(h);
      });
    });
    render();
  });
})();
