package com.operation.hub.sample.controller;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.regex.Pattern;

import com.operation.hub.sample.dto.SampleDutyStore;
import com.operation.hub.sample.dto.SampleUser;
import org.springframework.core.io.ResourceLoader;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

/**
 * 디자인/퍼블리싱 테스트 화면 모음. 개발 전 디자인 가이드용이며 URL은 /sample 아래에 둔다.
 * 실제 홈 화면이 생기기 전까지 / 는 /sample/home 으로 보낸다.
 * 템플릿: templates/sample/*.html
 */
@Controller
public class SampleController {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final String[] TEAMS = {"운영1팀", "운영2팀", "지원팀"};
    private static final String[] NAMES = {"홍길동", "김철수", "이영희", "박민수", "최지원", "정하늘", "강서준", "윤지우", "임도윤", "한예린"};

    /** 모달 등록 샘플용 메모리 저장소 (서버 재시작 시 초기화) */
    private final Map<String, SampleUser> modalUsers = new ConcurrentSkipListMap<>();

    /** 당직 관리 샘플용 메모리 저장소 */
    private final SampleDutyStore dutyStore = new SampleDutyStore();

    private final ResourceLoader resourceLoader;

    public SampleController(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
        sampleUsers(5).forEach(u -> modalUsers.put(u.emplNo(), u));
    }

    /** 실제 홈 화면이 생기기 전까지 샘플 홈으로 보낸다. */
    @GetMapping("/")
    public String root() {
        return "redirect:/sample/home";
    }

    /** 홈 (타일 메뉴) */
    @GetMapping({"/sample", "/sample/", "/sample/home"})
    public String home() {
        return "sample/home";
    }

    /** 로그인 화면 */
    @GetMapping("/sample/login")
    public String login() {
        return "sample/login";
    }

    /** 로그인 제출: 인증 없이 홈으로 이동 */
    @PostMapping("/sample/login")
    public String loginSubmit() {
        return "redirect:/sample/home";
    }

    /** 목록 화면 (검색 + 테이블 + 페이징) */
    @GetMapping("/sample/list")
    public String list() {
        return "sample/list";
    }

    /** 등록/수정 화면 */
    @GetMapping("/sample/form")
    public String form() {
        return "sample/form";
    }

    /** 등록/수정 저장: 목록으로 이동 */
    @PostMapping("/sample/form")
    public String formSubmit() {
        return "redirect:/sample/list";
    }

    /** UI 컴포넌트 모음 */
    @GetMapping("/sample/components")
    public String components() {
        return "sample/components";
    }

    // ---------------------------------------------------------------------
    // 모달 등록 화면
    // ---------------------------------------------------------------------

    /** 목록 + 모달 등록/수정 */
    @GetMapping("/sample/modal")
    public String modal(Model model) {
        model.addAttribute("users", modalUsers.values());
        return "sample/modal";
    }

    /** 목록 tbody만 다시 그림 (저장 후 갱신용) */
    @GetMapping("/sample/modal/rows")
    public String modalRows(Model model) {
        model.addAttribute("users", modalUsers.values());
        return "sample/modal :: rows";
    }

    /** 모달 내용 (등록: mode=add, 수정: mode=modify&emplNo=...) */
    @GetMapping("/sample/modal/form")
    public String modalForm(@RequestParam(defaultValue = "add") String mode,
                            @RequestParam(required = false) String emplNo,
                            Model model) {
        SampleUser user = "modify".equals(mode) && emplNo != null ? modalUsers.get(emplNo) : null;
        model.addAttribute("mode", user == null ? "add" : "modify");
        model.addAttribute("user", user);
        model.addAttribute("teams", TEAMS);
        return "sample/modal-form :: modal";
    }

    /** 모달 저장 (AJAX). 결과는 JSON: {result: OK|FAIL, message, field?} */
    @PostMapping("/sample/modal/save")
    @ResponseBody
    public ResponseEntity<Map<String, String>> modalSave(@RequestParam String mode,
                                                         @RequestParam(required = false) String emplNo,
                                                         @RequestParam(required = false) String name,
                                                         @RequestParam(required = false) String team,
                                                         @RequestParam(defaultValue = "1") String grade,
                                                         @RequestParam(required = false) String phone,
                                                         @RequestParam(required = false) String email,
                                                         @RequestParam(defaultValue = "false") boolean useYn) {
        if (!StringUtils.hasText(emplNo)) return fail("emplNo", "사용자 NO를 입력하세요.");
        if (!StringUtils.hasText(name)) return fail("name", "사용자명을 입력하세요.");
        if (!StringUtils.hasText(team)) return fail("team", "조직을 선택하세요.");

        boolean add = !"modify".equals(mode);
        if (add && modalUsers.containsKey(emplNo)) return fail("emplNo", "이미 등록된 사용자 NO입니다.");
        if (!add && !modalUsers.containsKey(emplNo)) return fail("emplNo", "수정할 사용자를 찾을 수 없습니다.");

        String lastLogin = add ? "-" : modalUsers.get(emplNo).lastLogin();
        modalUsers.put(emplNo, new SampleUser(emplNo.trim(), name.trim(), team, grade, phone, email, useYn, lastLogin));

        Map<String, String> body = new LinkedHashMap<>();
        body.put("result", "OK");
        body.put("message", add ? "등록되었습니다." : "수정되었습니다.");
        return ResponseEntity.ok(body);
    }

    private static ResponseEntity<Map<String, String>> fail(String field, String message) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("result", "FAIL");
        body.put("field", field);
        body.put("message", message);
        return ResponseEntity.badRequest().body(body);
    }

    // ---------------------------------------------------------------------
    // DataTables (바닐라 JS, jQuery 없음)
    // ---------------------------------------------------------------------

    /** DataTables 3 샘플: 정렬 / 검색 / 페이지 크기 / 페이징을 브라우저에서 처리 */
    @GetMapping("/sample/datatable")
    public String datatable(Model model) {
        model.addAttribute("users", sampleUsers(57));
        return "sample/datatable";
    }

    // ---------------------------------------------------------------------
    // CS Webtool 화면 퍼블 (정적 샘플). 템플릿 파일만 추가하면 열린다.
    //   화면: /sample/cswt/{module}/{page}         -> templates/sample/cswt/{module}/{page}.html
    //   모달: /sample/cswt/{module}/modal/{name}   -> templates/sample/cswt/{module}/_{name}.html 의 th:fragment="modal"
    // ---------------------------------------------------------------------

    private static final Pattern NAME = Pattern.compile("[a-z0-9][a-z0-9-]{0,60}");

    @GetMapping("/sample/cswt/{module}/{page}")
    public String cswtPage(@PathVariable String module, @PathVariable String page) {
        return cswtTemplate(module, page);
    }

    @GetMapping("/sample/cswt/{module}/modal/{name}")
    public String cswtModal(@PathVariable String module, @PathVariable String name) {
        return cswtTemplate(module, "_" + name) + " :: modal";
    }

    private String cswtTemplate(String module, String file) {
        String bare = file.startsWith("_") ? file.substring(1) : file;
        if (!NAME.matcher(module).matches() || !NAME.matcher(bare).matches()) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        String view = "sample/cswt/" + module + "/" + file;
        if (!resourceLoader.getResource("classpath:/templates/" + view + ".html").exists()) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return view;
    }

    // ---------------------------------------------------------------------
    // 당직 관리 (CS Webtool "724 당직 관리 (DI)" 화면)
    // ---------------------------------------------------------------------

    /** 당직 관리 화면. dutyDate 가 없으면 오늘 */
    @GetMapping("/sample/duty")
    public String duty(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dutyDate,
                       Model model) {
        LocalDate date = dutyDate != null ? dutyDate : LocalDate.now();
        model.addAttribute("dutyDate", date);
        model.addAttribute("dutyTime", dutyTime(date));
        model.addAttribute("areas", dutyStore.board(date));
        return "sample/duty";
    }

    /** 당직표 영역만 다시 그림 (추가/삭제 후 갱신용) */
    @GetMapping("/sample/duty/board")
    public String dutyBoard(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dutyDate, Model model) {
        model.addAttribute("dutyDate", dutyDate);
        model.addAttribute("areas", dutyStore.board(dutyDate));
        return "sample/duty :: board";
    }

    /** 당직 추가 모달 */
    @GetMapping("/sample/duty/add")
    public String dutyAdd(@RequestParam String branchCd, @RequestParam String gg,
                          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dutyDate, Model model) {
        model.addAttribute("branchCd", branchCd);
        model.addAttribute("gg", gg);
        model.addAttribute("dutyDate", dutyDate);
        return "sample/duty-add :: modal";
    }

    /** 엔지니어 이름 확인 (CS Webtool getEmplNum) */
    @PostMapping("/sample/duty/engineer")
    @ResponseBody
    public ResponseEntity<Map<String, String>> dutyEngineer(@RequestParam(required = false) String korName) {
        SampleDutyStore.Engineer e = dutyStore.findEngineer(korName);
        if (e == null) return fail("korName", "엔지니어가 확인되지 않습니다.");
        return ok("엔지니어가 확인되었습니다.", Map.of("emplNo", e.emplNo(), "korName", e.korName()));
    }

    /** 당직 엔지니어 추가 저장 */
    @PostMapping("/sample/duty/save")
    @ResponseBody
    public ResponseEntity<Map<String, String>> dutySave(@RequestParam String branchCd, @RequestParam String gg,
                                                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dutyDate,
                                                        @RequestParam(required = false) String emplNo) {
        if (!dutyStore.exists(branchCd, gg)) return fail(null, "지역/GG 정보가 올바르지 않습니다.");
        SampleDutyStore.Engineer e = StringUtils.hasText(emplNo) ? dutyStore.findEngineerByNo(emplNo) : null;
        if (e == null) return fail("korName", "엔지니어 이름이 확인되지 않았습니다.");
        if (dutyStore.isAssigned(dutyDate, branchCd, gg, emplNo)) return fail("korName", "이미 배정된 엔지니어입니다.");
        dutyStore.add(dutyDate, branchCd, gg, e);
        return ok("저장이 완료되었습니다.", Map.of());
    }

    /** 당직 엔지니어 삭제 */
    @PostMapping("/sample/duty/delete")
    @ResponseBody
    public ResponseEntity<Map<String, String>> dutyDelete(@RequestParam String branchCd, @RequestParam String gg,
                                                          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dutyDate,
                                                          @RequestParam long dutySeq) {
        if (!dutyStore.delete(dutyDate, branchCd, gg, dutySeq)) return fail(null, "삭제할 당직을 찾을 수 없습니다.");
        return ok("삭제가 완료되었습니다.", Map.of());
    }

    /** 엔지니어 배정타입 저장 (JSON 배열: [{branchCd, gg, type}]) */
    @PostMapping("/sample/duty/type")
    @ResponseBody
    public ResponseEntity<Map<String, String>> dutyTypeSave(@RequestBody List<Map<String, String>> types) {
        for (Map<String, String> t : types) {
            if (dutyStore.exists(t.get("branchCd"), t.get("gg"))) {
                dutyStore.saveType(t.get("branchCd"), t.get("gg"), t.get("type"));
            }
        }
        return ok("저장이 완료되었습니다.", Map.of());
    }

    /** 자동배정 설정 모달 (조회 전용, CS Webtool 과 같음) */
    @GetMapping("/sample/duty/auto")
    public String dutyAuto(Model model) {
        model.addAttribute("rows", dutyStore.autoAssignRows());
        return "sample/duty-auto :: modal";
    }

    /** 당직 시간: 평일 18:00 ~ 익일 08:00, 주말 08:00 ~ 익일 08:00 (공휴일은 샘플에서 제외) */
    private static String dutyTime(LocalDate date) {
        DateTimeFormatter f = DateTimeFormatter.ofPattern("MM-dd(E)", Locale.KOREAN);
        boolean weekend = date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY;
        return date.format(f) + (weekend ? " 08:00" : " 18:00") + " ~ " + date.plusDays(1).format(f) + " 08:00";
    }

    private static ResponseEntity<Map<String, String>> ok(String message, Map<String, String> data) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("result", "OK");
        body.put("message", message);
        body.putAll(data);
        return ResponseEntity.ok(body);
    }

    // ---------------------------------------------------------------------
    // 일반 테이블 (라이브러리 없이 서버에서 그리는 표)
    // ---------------------------------------------------------------------

    /** 일반 테이블 샘플: 기본 목록 / 체크박스 선택 / 병합 헤더 + 첫 열 고정 + 합계 / 빈 목록 */
    @GetMapping("/sample/table")
    public String table(Model model) {
        model.addAttribute("users", sampleUsers(8));

        // 주간 근무 현황 (CS Webtool egn_daily 처럼 날짜별 정상/연장 2칸 헤더)
        LocalDate monday = LocalDate.of(2026, 10, 19);
        String[] weekNames = {"월", "화", "수", "목", "금", "토", "일"};
        List<WorkDay> days = new ArrayList<>();
        for (int d = 0; d < 7; d++) {
            days.add(new WorkDay(monday.plusDays(d).format(DateTimeFormatter.ofPattern("MM-dd")), weekNames[d]));
        }
        List<WorkRow> rows = new ArrayList<>();
        double[] sumNormal = new double[7];
        double[] sumExtra = new double[7];
        for (SampleUser u : sampleUsers(12)) {
            int seed = Integer.parseInt(u.emplNo().substring(1));
            double[] normal = new double[7];
            double[] extra = new double[7];
            for (int d = 0; d < 5; d++) {   // 평일만 근무
                normal[d] = 8;
                extra[d] = (seed + d) % 4 == 0 ? (seed % 3) + 1 : 0;
                sumNormal[d] += normal[d];
                sumExtra[d] += extra[d];
            }
            rows.add(new WorkRow(u.emplNo() + "(" + u.name() + ")", normal, extra));
        }
        model.addAttribute("days", days);
        model.addAttribute("workRows", rows);
        model.addAttribute("workTotal", new WorkRow("합계", sumNormal, sumExtra));
        return "sample/table";
    }

    /** 근무표 헤더 날짜 */
    public record WorkDay(String date, String week) {}

    /** 근무표 한 줄: 날짜별 정상/연장 시간 */
    public record WorkRow(String label, double[] normal, double[] extra) {
        public double normalTotal() { return Arrays.stream(normal).sum(); }
        public double extraTotal() { return Arrays.stream(extra).sum(); }
    }

    /** Handsontable 6.2.2 샘플 (엑셀처럼 칸을 고치는 순번 관리 표). 데이터는 화면 스크립트에 있음 */
    @GetMapping("/sample/handsontable")
    public String handsontable() {
        return "sample/handsontable";
    }

    /** 고정된 샘플 데이터 생성 (매번 같은 결과) */
    private static List<SampleUser> sampleUsers(int count) {
        LocalDateTime base = LocalDateTime.of(2026, 10, 24, 9, 0);
        List<SampleUser> users = new ArrayList<>(count);
        for (int i = 1; i <= count; i++) {
            String no = String.format("E%04d", i);
            String name = NAMES[(i - 1) % NAMES.length];
            String team = TEAMS[(i * 7) % TEAMS.length];
            String grade = i % 9 == 0 ? "9" : i % 4 == 0 ? "2" : "1";
            String phone = String.format("010-%04d-%04d", 1000 + i * 37 % 9000, 1000 + i * 113 % 9000);
            String email = "user" + i + "@example.com";
            boolean use = i % 6 != 0;
            String last = base.minusHours(i * 13L).format(DATE_TIME);
            users.add(new SampleUser(no, name, team, grade, phone, email, use, last));
        }
        return users;
    }
}
