package com.fixflow.util;

import com.fixflow.enums.UserRole;
import com.fixflow.model.User;


public class SessionManager {

    private static User currentUser;

    private SessionManager() {
        // Utility class
    }


    public static synchronized void login(User user) {
        currentUser = user;
    }


    public static synchronized void logout() {
        currentUser = null;
    }


    public static synchronized User getCurrentUser() {
        return currentUser;
    }


    public static synchronized UserRole getCurrentRole() {
        return currentUser != null ? currentUser.getRole() : null;
    }

    public static synchronized Integer getCurrentUserId() {
        return currentUser != null ? currentUser.getUserId() : null;
    }


    public static synchronized boolean isLoggedIn() {
        return currentUser != null;
    }


    public static synchronized boolean hasRole(UserRole role) {
        return currentUser != null && currentUser.getRole() == role;
    }


    public static synchronized boolean isAdmin() {
        return hasRole(UserRole.ADMINISTRATOR);
    }
}

