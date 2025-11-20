package pl.projekt.backend.config;

public class JwtPrincipal {
    public final Long userId;
    public final String role;

    public JwtPrincipal(Long userId, String role) {
        this.userId = userId;
        this.role = role;
    }
}

