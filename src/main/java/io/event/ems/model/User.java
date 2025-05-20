package io.event.ems.model;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@Table(name = "users")
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.RANDOM)
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    private String fullName;
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    private String AvatarUrl;

    @ManyToOne(optional = false)
    @JoinColumn(name = "status_id")
    private StatusCode status;

    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    private LocalDateTime lastLogin;

    @Column(columnDefinition = "boolean default false")
    private Boolean emailVerified;

    @Column(columnDefinition = "boolean default false")
    private Boolean twoFactorEnabled;

    // --- THÊM QUAN HỆ OneToOne VỚI UserSettings ---
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private UserSettings settings;

    // --- GETTER/SETTER CHO SETTINGS (QUAN TRỌNG) ---
    public UserSettings getSettings() {
        // Lazy initialization: Nếu chưa có settings, tạo mới khi được gọi
        // Điều này hữu ích nếu bạn thêm settings cho user đã tồn tại trước đó
        if (this.settings == null) {
            this.settings = new UserSettings(this);
        }
        return this.settings;
    }

    public void setSettings(UserSettings settings) {
        // Đảm bảo liên kết hai chiều
        if (settings != null) {
            settings.setUser(this);
        }
        this.settings = settings;
    }

}
