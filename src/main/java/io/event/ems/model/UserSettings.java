package io.event.ems.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@Table(name = "user_settings")
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"user"})
@ToString(exclude = {"user"})
public class UserSettings {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "user_id")
    @JsonBackReference
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "theme", length = 16, nullable = false)
    private ThemeOption theme = ThemeOption.SYSTEM;

    @Column(name = "receive_event_reminders", nullable = false)
    private Boolean receiveEventReminders = true;

    @Column(name = "receive_new_event_notifications", nullable = false)
    private Boolean receiveNewEventNotifications = false;

    @Column(name = "receive_promotional_emails", nullable = false)
    private Boolean receivePromotionalEmails = false;

    @Column(name = "sync_with_google_calendar", nullable = false)
    private Boolean syncWithGoogleCalendar = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Constructor để dễ dàng tạo settings khi tạo User
    public UserSettings(User user) {
        this.user = user;
        this.userId = user.getId();
        // Các giá trị mặc định đã được đặt ở trên
    }

    // Quan trọng: Cần setter cho User để thiết lập quan hệ hai chiều đúng cách
    public void setUser(User user) {
        this.user = user;
        if (user != null) {
            this.userId = user.getId();
        }
    }

}
