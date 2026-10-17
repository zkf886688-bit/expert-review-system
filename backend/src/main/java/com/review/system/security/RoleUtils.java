package com.review.system.security;

import java.util.List;

public final class RoleUtils {

    private RoleUtils() {
    }

    public static List<String> currentRoles() {
        LoginUser user = SecurityUtils.currentUser();
        return user != null ? user.getRoles() : List.of();
    }

    public static boolean hasRole(String role) {
        return currentRoles().contains(role);
    }

    public static boolean isFullAdmin() {
        return hasRole("ADMIN") || hasRole("REVIEW_ADMIN");
    }

    public static boolean isLeaderOnly() {
        return hasRole("LEADER") && !isFullAdmin();
    }

    public static boolean isApplicantOnly() {
        return hasRole("APPLICANT") && !isFullAdmin() && !hasRole("EXPERT") && !hasRole("LEADER");
    }

    public static boolean isApplicant() {
        return hasRole("APPLICANT");
    }

    public static boolean isLeader() {
        return hasRole("LEADER");
    }

    public static boolean isSystemAdmin() {
        return hasRole("ADMIN");
    }

    /** 领导视图：隐藏专家个人信息 */
    public static boolean shouldMaskExpertInfo() {
        return isLeaderOnly();
    }
}
