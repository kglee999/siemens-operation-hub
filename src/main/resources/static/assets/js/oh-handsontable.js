/*
 * Handsontable 6.2.2 공통 기본값 (CS Webtool 과 같은 버전, MIT 라이선스 마지막 버전, jQuery 없음).
 * 7.0 부터는 상용 라이선스가 필요하므로 버전을 올리지 말 것.
 *
 * 로드 순서 (페이지 layout:fragment="script"):
 *   vendor/handsontable-6.2.2/handsontable.full.min.js
 *   vendor/handsontable-6.2.2/languages/ko-KR.min.js   (선택: 우클릭 메뉴 한글)
 *   assets/js/oh-handsontable.js
 * CSS: vendor/handsontable-6.2.2/handsontable.full.min.css (페이지 <head>)
 * 모양은 style.css 의 #HANDSONTABLE 영역에서 jambo_table 과 같게 맞춤.
 *
 * 사용: const hot = oh.grid('#grid', { colHeaders: [...], columns: [...], data: [...] });
 *   - 읽기 전용 열: columns 에 readOnly: true  → 빨간 글자 (not-editable-cell)
 *   - 빈 줄 노란 배경: cells: oh.grid.emptyRowCells()
 */
(function () {
  'use strict';
  if (!window.Handsontable) return;

  var DEFAULTS = {
    rowHeaders: true,
    className: 'htCenter htMiddle',
    stretchH: 'all',
    rowHeights: 30,
    contextMenu: ['row_above', 'row_below'],
    readOnlyCellClassName: 'not-editable-cell',
    outsideClickDeselects: false
  };
  if (Handsontable.languages && Handsontable.languages.getLanguageDictionary && Handsontable.languages.getLanguageDictionary('ko-KR')) {
    DEFAULTS.language = 'ko-KR';
  }

  window.oh = window.oh || {};

  /** 그리드를 만들고 Handsontable 인스턴스 반환. el 은 요소 또는 선택자 */
  window.oh.grid = function (el, options) {
    var target = typeof el === 'string' ? document.querySelector(el) : el;
    target.classList.add('oh-grid');
    var hot = new Handsontable(target, Object.assign({}, DEFAULTS, options || {}));
    // cells 콜백이 인스턴스를 참조하는 경우(emptyRowCells) 처음 그릴 땐 인스턴스가 없으므로 한 번 더 그림
    if (options && options.cells) hot.render();
    return hot;
  };

  /**
   * cells 옵션용: 읽기 전용이 아닌 칸이 모두 비어 있는 줄에 노란 배경(hot-empty-row).
   * 예) oh.grid('#grid', { ..., cells: oh.grid.emptyRowCells() })
   */
  window.oh.grid.emptyRowCells = function () {
    return function (row) {
      var hot = this.instance;   // cells 콜백의 this 는 칸 설정 객체이고 instance 를 가짐
      if (!hot) return {};
      var values = hot.getDataAtRow(row) || [];
      var settings = hot.getSettings().columns;
      var empty = values.every(function (v, col) {
        var ro = Array.isArray(settings) && settings[col] && settings[col].readOnly;
        return ro || v === null || v === undefined || v === '';
      });
      return empty ? { className: 'htCenter htMiddle hot-empty-row' } : {};
    };
  };
})();
