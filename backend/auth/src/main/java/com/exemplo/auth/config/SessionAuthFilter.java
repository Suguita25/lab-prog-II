package com.exemplo.auth.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

@Component
public class SessionAuthFilter extends OncePerRequestFilter {

    private static final Set<String> OPEN = Set.of(
            "/", "/index.html",
            "/api/auth/login", "/api/auth/register",
            "/favicon.ico", "/styles.css", "/index.js", "/app.js"
    );

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String p = request.getServletPath();
        if (OPEN.contains(p)) return true;
        return p.startsWith("/static/") || p.startsWith("/css/") || p.startsWith("/js/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String p = req.getServletPath();
        boolean needsAuth = p.equals("/app.html") || p.startsWith("/api/collections");

        if (!needsAuth) { chain.doFilter(req, res); return; }

        var sess = req.getSession(false);
        Object auth = (sess == null) ? null : sess.getAttribute("auth"); // <<< checa "auth"
        if (!(auth instanceof Boolean) || !((Boolean) auth)) {
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            res.setContentType("application/json");
            res.getWriter().write("{\"error\":\"unauthorized\"}");
            return;
        }
        chain.doFilter(req, res);
    }
}
