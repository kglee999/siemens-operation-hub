package com.operation.hub.sample.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 당직 관리 샘플용 메모리 데이터 (CS Webtool "724 당직 관리 (DI)" 와 같은 구성).
 * 서버 재시작 시 초기화된다.
 *
 * <ul>
 *   <li>지역(area) 아래에 GG(장비군)가 있고, GG마다 배정타입(CSE: 장비담당 CSE, EGN: 당직 엔지니어)과 자동배정 여부가 있다.</li>
 *   <li>배정타입이 EGN이면 날짜별로 당직 엔지니어를 순번대로 등록한다.</li>
 * </ul>
 */
public class SampleDutyStore {

    static final String TYPE_CSE = "CSE";
    static final String TYPE_EGN = "EGN";

    public record Area(String branchCd, String branchNm, int order, List<String> ggs) {}

    public record Engineer(String emplNo, String korName) {}

    public record Duty(long seq, String emplNo, String korName) {}

    /** 화면 한 줄 (지역 + GG) */
    public record Row(String branchCd, String branchNm, String gg, String type, String autoAssignYn, List<Duty> duties) {
        public boolean egn() { return TYPE_EGN.equals(type); }
    }

    /** 화면 한 블록 (지역 제목 + 줄들) */
    public record AreaView(Area area, List<Row> rows) {}

    private static final List<Area> AREAS = List.of(
        new Area("01", "서울/경기", 1, List.of("CT", "MR", "XP", "AX")),
        new Area("02", "충청/강원", 2, List.of("CT", "MR", "XP")),
        new Area("03", "영남", 3, List.of("CT", "MR", "US")),
        new Area("04", "호남", 4, List.of("CT", "MR")));

    private static final List<Engineer> ENGINEERS = List.of(
        new Engineer("E1001", "홍길동"), new Engineer("E1002", "김철수"), new Engineer("E1003", "이영희"),
        new Engineer("E1004", "박민수"), new Engineer("E1005", "최지원"), new Engineer("E1006", "정하늘"),
        new Engineer("E1007", "강서준"), new Engineer("E1008", "윤지우"), new Engineer("E1009", "임도윤"),
        new Engineer("E1010", "한예린"), new Engineer("E1011", "오세훈"), new Engineer("E1012", "서다은"));

    private final Map<String, String> types = new ConcurrentHashMap<>();
    private final Map<String, String> autoAssign = new ConcurrentHashMap<>();
    /** 날짜 -> (지역|GG -> 당직 순번) */
    private final Map<LocalDate, Map<String, List<Duty>>> schedule = new ConcurrentHashMap<>();
    private final AtomicLong seq = new AtomicLong();

    public SampleDutyStore() {
        int i = 0;
        for (Area a : AREAS) {
            for (String gg : a.ggs()) {
                types.put(key(a.branchCd(), gg), i % 3 == 2 ? TYPE_CSE : TYPE_EGN);
                autoAssign.put(key(a.branchCd(), gg), i % 4 == 3 ? "N" : "Y");
                i++;
            }
        }
    }

    public List<AreaView> board(LocalDate date) {
        Map<String, List<Duty>> day = day(date);
        List<AreaView> views = new ArrayList<>();
        for (Area a : AREAS) {
            List<Row> rows = new ArrayList<>();
            for (String gg : a.ggs()) {
                String k = key(a.branchCd(), gg);
                rows.add(new Row(a.branchCd(), a.branchNm(), gg, types.get(k), autoAssign.get(k), List.copyOf(day.get(k))));
            }
            views.add(new AreaView(a, rows));
        }
        return views;
    }

    /** 자동배정 설정 목록 (지역/GG/자동배정 여부) */
    public List<Row> autoAssignRows() {
        List<Row> rows = new ArrayList<>();
        for (Area a : AREAS) {
            for (String gg : a.ggs()) {
                String k = key(a.branchCd(), gg);
                rows.add(new Row(a.branchCd(), a.branchNm(), gg, types.get(k), autoAssign.get(k), List.of()));
            }
        }
        return rows;
    }

    public boolean exists(String branchCd, String gg) {
        return types.containsKey(key(branchCd, gg));
    }

    /** 이름으로 엔지니어 찾기 (CS Webtool 의 getEmplNum 과 같은 역할) */
    public Engineer findEngineer(String korName) {
        if (korName == null) return null;
        String name = korName.trim();
        return ENGINEERS.stream().filter(e -> e.korName().equals(name)).findFirst().orElse(null);
    }

    public Engineer findEngineerByNo(String emplNo) {
        return ENGINEERS.stream().filter(e -> e.emplNo().equals(emplNo)).findFirst().orElse(null);
    }

    public boolean isAssigned(LocalDate date, String branchCd, String gg, String emplNo) {
        return day(date).get(key(branchCd, gg)).stream().anyMatch(d -> d.emplNo().equals(emplNo));
    }

    public void add(LocalDate date, String branchCd, String gg, Engineer e) {
        day(date).get(key(branchCd, gg)).add(new Duty(seq.incrementAndGet(), e.emplNo(), e.korName()));
    }

    public boolean delete(LocalDate date, String branchCd, String gg, long dutySeq) {
        List<Duty> list = day(date).get(key(branchCd, gg));
        return list != null && list.removeIf(d -> d.seq() == dutySeq);
    }

    public void saveType(String branchCd, String gg, String type) {
        types.put(key(branchCd, gg), TYPE_EGN.equals(type) ? TYPE_EGN : TYPE_CSE);
    }

    /** 처음 조회하는 날짜는 날짜 값으로 정해지는 샘플 순번을 채운다 (매번 같은 결과) */
    private Map<String, List<Duty>> day(LocalDate date) {
        return schedule.computeIfAbsent(date, d -> {
            Map<String, List<Duty>> map = new ConcurrentHashMap<>();
            int n = d.getDayOfYear();
            for (Area a : AREAS) {
                for (String gg : a.ggs()) {
                    List<Duty> list = new CopyOnWriteArrayList<>();
                    int count = 1 + (n + gg.length() + a.order()) % 3;
                    for (int i = 0; i < count; i++) {
                        Engineer e = ENGINEERS.get((n + a.order() * 3 + gg.charAt(0) + i * 5) % ENGINEERS.size());
                        if (list.stream().noneMatch(x -> x.emplNo().equals(e.emplNo()))) {
                            list.add(new Duty(seq.incrementAndGet(), e.emplNo(), e.korName()));
                        }
                    }
                    map.put(key(a.branchCd(), gg), list);
                }
            }
            return map;
        });
    }

    private static String key(String branchCd, String gg) {
        return branchCd + "|" + gg;
    }
}
