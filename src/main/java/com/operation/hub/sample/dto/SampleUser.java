package com.operation.hub.sample.dto;

/**
 * 샘플 화면용 사용자 데이터. 실제 도메인 모델이 아니다.
 */
public record SampleUser(
        String emplNo,
        String name,
        String team,
        String grade,
        String phone,
        String email,
        boolean useYn,
        String lastLogin) {

    public String gradeName() {
        return switch (grade == null ? "" : grade) {
            case "2" -> "당직 관리";
            case "9" -> "Admin";
            default -> "일반";
        };
    }
}
