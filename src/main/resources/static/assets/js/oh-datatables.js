/*
 * DataTables 공통 기본값 (jQuery 없음). dataTables.min.js / dataTables.bootstrap5.min.js 다음에 로드.
 * 모양은 style.css 의 #DATATABLES 영역에서 jambo_table 과 같게 맞춤.
 *
 * 사용: new DataTable('#tableId', { order: [[0, 'asc']] });  // 화면별 옵션만 넘기면 됨
 */
(function () {
  'use strict';
  if (!window.DataTable) return;

  // 검색창/건수 선택을 작은 크기(-sm) 대신 기본 입력 크기(34px)로
  DataTable.util.object.assignDeep(DataTable.ext.classes, {
    search: { input: 'form-control' },
    length: { select: 'form-select' }
  });

  DataTable.util.object.assignDeep(DataTable.defaults, {
    pageLength: 10,
    lengthMenu: [10, 25, 50, 100],
    autoWidth: false,
    // 위: 건수 선택 + 건수 정보(왼쪽) / 검색(오른쪽), 아래: 페이징(가운데) - CS Webtool 목록처럼
    layout: {
      topStart: ['pageLength', 'info'],
      topEnd: 'search',
      bottomStart: null,
      bottomEnd: null,
      bottom: 'paging'
    },
    language: {
      search: '검색',
      searchPlaceholder: '검색어 입력',
      lengthMenu: '_MENU_ 건씩 보기',
      info: '전체 _TOTAL_건 중 _START_ - _END_',
      infoEmpty: '0건',
      infoFiltered: '(전체 _MAX_건에서 검색)',
      zeroRecords: '검색 결과가 없습니다.',
      emptyTable: '데이터가 없습니다.',
      loadingRecords: '불러오는 중...',
      processing: '처리 중...',
      paginate: { first: '«', previous: '‹', next: '›', last: '»' },
      aria: { sortAscending: ': 오름차순 정렬', sortDescending: ': 내림차순 정렬' }
    }
  });
})();
