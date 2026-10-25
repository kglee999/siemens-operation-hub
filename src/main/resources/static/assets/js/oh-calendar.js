/*
 * FullCalendar 7 공통 기본값 (jQuery 없음). 아래 순서로 로드한 뒤 사용:
 *   vendor/fullcalendar-7.1.3/fullcalendar.global.js
 *   vendor/fullcalendar-7.1.3/themes/classic/classic.global.js
 *   vendor/fullcalendar-7.1.3/locales/ko.global.js
 *   assets/js/oh-calendar.js
 * CSS: skeleton.css, themes/classic/theme.css, themes/classic/palette.css (페이지 <head>)
 * 모양은 style.css 의 #CALENDAR 영역 (.oh-calendar) 에서 CS Webtool(FullCalendar 3) 에 맞춤.
 *
 * 사용: const cal = oh.calendar('#calendar', { events: [...] });   // 화면별 옵션만 넘기면 됨
 * 일정별 색: v7 에서는 color / contrastColor 를 쓴다 (v3 의 backgroundColor / textColor 는 무시됨).
 */
(function () {
  'use strict';
  if (!window.FullCalendar) return;

  var DEFAULTS = {
    locale: 'ko',
    initialView: 'dayGridMonth',
    // CS Webtool: 왼쪽 이전/다음/오늘, 가운데 제목, 오른쪽 월/주/일
    headerToolbar: { left: 'prev,next today', center: 'title', right: 'dayGridMonth,timeGridWeek,timeGridDay' },
    buttonText: { today: '오늘', month: '월', week: '주', day: '일', list: '목록' },
    height: 'auto',
    dayMaxEvents: true,
    fixedWeekCount: false,
    eventDisplay: 'block'
  };

  window.oh = window.oh || {};

  /** 캘린더를 만들고 바로 그림. el 은 요소 또는 선택자. Calendar 객체 반환 */
  window.oh.calendar = function (el, options) {
    var target = typeof el === 'string' ? document.querySelector(el) : el;
    target.classList.add('oh-calendar');
    var cal = new FullCalendar.Calendar(target, Object.assign({}, DEFAULTS, options || {}));
    cal.render();
    return cal;
  };
})();
