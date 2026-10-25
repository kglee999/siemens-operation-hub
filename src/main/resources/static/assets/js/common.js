/* Operation Hub 공통 레이아웃 스크립트 (jQuery 없음, Bootstrap 5 bundle 필요) */
(function () {
  'use strict';

  var MOBILE_MAX = 991.98;
  var STORAGE_KEY = 'oh.sidebar';

  function isMobile() {
    return window.matchMedia('(max-width: ' + MOBILE_MAX + 'px)').matches;
  }

  function readPref() {
    try { return localStorage.getItem(STORAGE_KEY); } catch (e) { return null; }
  }

  function writePref(value) {
    try { localStorage.setItem(STORAGE_KEY, value); } catch (e) { /* 저장 불가 시 무시 */ }
  }

  // 사이드바 펼침/접힘: 데스크톱은 nav-md <-> nav-sm, 모바일은 오버레이 열기/닫기
  function initSidebarToggle() {
    var body = document.body;
    if (!body.classList.contains('nav-md') && !body.classList.contains('nav-sm')) return;

    if (readPref() === 'sm') {
      body.classList.replace('nav-md', 'nav-sm');
    }

    var toggle = document.getElementById('menu_toggle');
    if (toggle) {
      toggle.addEventListener('click', function () {
        if (isMobile()) {
          body.classList.toggle('sidebar-open');
          return;
        }
        var collapsed = body.classList.contains('nav-md');
        body.classList.toggle('nav-md', !collapsed);
        body.classList.toggle('nav-sm', collapsed);
        writePref(collapsed ? 'sm' : 'md');
      });
    }

    var backdrop = document.querySelector('.sidebar-backdrop');
    if (backdrop) {
      backdrop.addEventListener('click', function () {
        body.classList.remove('sidebar-open');
      });
    }
  }

  // 현재 URL과 같은 메뉴를 활성화하고 상위 메뉴를 펼침
  function markCurrentMenu() {
    var path = window.location.pathname.replace(/\/+$/, '') || '/';
    var links = document.querySelectorAll('#sidebar-menu a[href]');

    links.forEach(function (link) {
      var href = link.getAttribute('href').replace(/\/+$/, '') || '/';
      if (href !== path) return;

      var childItem = link.closest('.child_menu > li');
      if (childItem) childItem.classList.add('current-page');

      var topItem = link.closest('.side-menu > li');
      if (!topItem) return;
      topItem.classList.add('active');

      var sub = topItem.querySelector('.collapse');
      var trigger = topItem.querySelector('[data-bs-toggle="collapse"]');
      if (sub) sub.classList.add('show');
      if (trigger) trigger.setAttribute('aria-expanded', 'true');
    });
  }

  // 툴팁 자동 초기화
  function initTooltips() {
    if (!window.bootstrap) return;
    document.querySelectorAll('[data-bs-toggle="tooltip"]').forEach(function (el) {
      bootstrap.Tooltip.getOrCreateInstance(el);
    });
  }

  // .table-freeze: 헤더가 2줄일 때 둘째 줄의 고정 위치(top)를 첫 줄 높이에 맞춤
  function initFreezeTables() {
    document.querySelectorAll('.table-freeze').forEach(function (box) {
      var first = box.querySelector('thead tr');
      if (!first) return;
      var update = function () { box.style.setProperty('--oh-freeze-head1', first.getBoundingClientRect().height + 'px'); };
      update();
      if (window.ResizeObserver) new ResizeObserver(update).observe(first);
    });
  }

  // 설명 레이어 (.layer-wrap > .layer-detail-btn + .layer-detail): 버튼을 누르면 열고, 바깥을 누르면 닫음
  document.addEventListener('click', function (e) {
    var btn = e.target.closest('.layer-detail-btn');
    document.querySelectorAll('.layer-wrap.open').forEach(function (w) {
      if (!btn || w !== btn.closest('.layer-wrap')) w.classList.remove('open');
    });
    if (btn) {
      e.preventDefault();
      btn.closest('.layer-wrap').classList.toggle('open');
    }
  });

  document.addEventListener('DOMContentLoaded', function () {
    initSidebarToggle();
    markCurrentMenu();
    initTooltips();
    initFreezeTables();
  });

  // ---------------------------------------------------------------------------
  // 공통 유틸: window.oh  (gb-siemens의 modal_show / $.ajax 대체)
  // ---------------------------------------------------------------------------

  // Spring Security CSRF 메타 태그가 있으면 헤더로 붙임 (보안 꺼진 상태에선 무시)
  function csrfHeaders() {
    var token = document.querySelector('meta[name="_csrf"]');
    var header = document.querySelector('meta[name="_csrf_header"]');
    var h = { 'X-Requested-With': 'XMLHttpRequest' };
    if (token && header) h[header.content] = token.content;
    return h;
  }

  function withParams(url, params) {
    if (!params) return url;
    var qs = new URLSearchParams(params).toString();
    return qs ? url + (url.indexOf('?') < 0 ? '?' : '&') + qs : url;
  }

  var oh = window.oh || {};

  oh.ajax = {
    /** GET 후 텍스트(HTML 조각) 반환 */
    getHtml: function (url, params) {
      return fetch(withParams(url, params), { headers: csrfHeaders() }).then(function (r) {
        if (!r.ok) throw new Error('HTTP ' + r.status);
        return r.text();
      });
    },
    /** 폼을 x-www-form-urlencoded 로 POST 하고 JSON 반환 (4xx 응답의 JSON도 그대로 반환) */
    postForm: function (url, form) {
      return oh.ajax.post(url, new FormData(form));
    },
    /** 객체(또는 FormData)를 x-www-form-urlencoded 로 POST 하고 JSON 반환. 예) oh.ajax.post(url, { seq: 3 }) */
    post: function (url, data) {
      return postBody(url, 'application/x-www-form-urlencoded;charset=UTF-8', new URLSearchParams(data || {}));
    },
    /** 객체/배열을 JSON 본문으로 POST 하고 JSON 반환 (서버는 @RequestBody 로 받음) */
    postJson: function (url, data) {
      return postBody(url, 'application/json;charset=UTF-8', JSON.stringify(data));
    }
  };

  function postBody(url, contentType, body) {
    var headers = csrfHeaders();
    headers['Content-Type'] = contentType;
    return fetch(url, { method: 'POST', headers: headers, body: body }).then(function (r) {
      return r.json().catch(function () { throw new Error('HTTP ' + r.status); });
    });
  }

  /**
   * 확인 창 (window.confirm 대체). Promise<boolean> 반환.
   * 예) oh.confirm('삭제하시겠습니까?').then(function (ok) { if (ok) ... });
   */
  oh.confirm = function (message, options) {
    var opt = options || {};
    return new Promise(function (resolve) {
      var el = document.createElement('div');
      el.className = 'modal fade oh-confirm';
      el.tabIndex = -1;
      el.setAttribute('aria-hidden', 'true');
      el.innerHTML =
        '<div class="modal-dialog modal-dialog-centered modal-sm"><div class="modal-content">' +
        '<div class="modal-body"></div>' +
        '<div class="modal-footer">' +
        '<button type="button" class="btn btn-default" data-bs-dismiss="modal"></button>' +
        '<button type="button" class="btn btn-success" data-ok></button>' +
        '</div></div></div>';
      el.querySelector('.modal-body').textContent = message;
      el.querySelector('[data-bs-dismiss]').textContent = opt.cancelText || '취소';
      el.querySelector('[data-ok]').textContent = opt.okText || '확인';
      document.body.appendChild(el);

      var ok = false;
      var modal = bootstrap.Modal.getOrCreateInstance(el);
      el.querySelector('[data-ok]').addEventListener('click', function () { ok = true; modal.hide(); });
      el.addEventListener('shown.bs.modal', function () { el.querySelector('[data-ok]').focus(); });
      el.addEventListener('hidden.bs.modal', function () { modal.dispose(); el.remove(); resolve(ok); });
      modal.show();
    });
  };

  oh.modal = {
    /**
     * 서버에서 모달 HTML(.modal 요소)을 받아 #modal_area 에 넣고 띄움.
     * 닫히면 DOM에서 제거. Promise<모달 요소> 반환.
     */
    open: function (url, params) {
      return oh.ajax.getHtml(url, params).then(function (html) {
        var area = document.getElementById('modal_area');
        if (!area) {
          area = document.createElement('div');
          area.id = 'modal_area';
          document.body.appendChild(area);
        }
        area.innerHTML = html;
        // innerHTML 로 넣은 <script> 는 실행되지 않으므로 다시 만들어 실행 (jQuery .html() 과 같은 동작)
        area.querySelectorAll('script').forEach(function (old) {
          var s = document.createElement('script');
          Array.prototype.forEach.call(old.attributes, function (a) { s.setAttribute(a.name, a.value); });
          s.textContent = old.textContent;
          old.replaceWith(s);
        });
        var el = area.querySelector('.modal');
        el.addEventListener('hidden.bs.modal', function () {
          bootstrap.Modal.getInstance(el).dispose();
          el.remove();
        });
        bootstrap.Modal.getOrCreateInstance(el).show();
        return el;
      });
    }
  };

  /** 전체 화면 로딩 표시 (CS Webtool siemens.loadingbar). oh.loading.show() / oh.loading.hide() */
  oh.loading = {
    show: function () {
      if (document.getElementById('oh-loading')) return;
      var el = document.createElement('div');
      el.id = 'oh-loading';
      el.className = 'oh-loading';
      el.setAttribute('role', 'status');
      el.innerHTML = '<div class="spinner-border"><span class="visually-hidden">처리 중...</span></div>';
      document.body.appendChild(el);
    },
    hide: function () {
      var el = document.getElementById('oh-loading');
      if (el) el.remove();
    }
  };

  /** 화면 오른쪽 아래 토스트 알림. type: success | danger | warning | info */
  oh.toast = function (message, type) {
    var box = document.getElementById('oh-toast-box');
    if (!box) {
      box = document.createElement('div');
      box.id = 'oh-toast-box';
      box.className = 'toast-container position-fixed bottom-0 end-0 p-3';
      document.body.appendChild(box);
    }
    var el = document.createElement('div');
    el.className = 'toast align-items-center border-0 text-bg-' + (type || 'success');
    el.setAttribute('role', 'status');
    el.setAttribute('aria-live', 'polite');
    el.innerHTML = '<div class="d-flex"><div class="toast-body"></div>' +
      '<button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast" aria-label="닫기"></button></div>';
    el.querySelector('.toast-body').textContent = message;
    box.appendChild(el);
    el.addEventListener('hidden.bs.toast', function () { el.remove(); });
    bootstrap.Toast.getOrCreateInstance(el, { delay: 3000 }).show();
  };

  window.oh = oh;
})();
