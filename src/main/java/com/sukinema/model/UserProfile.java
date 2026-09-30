package com.sukinema.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_profiles")
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre de perfil es obligatorio")
    @Column(nullable = false)
    private String name;

    @Column(length = 500)
    private String avatar;

    private String color;

    private boolean isKid = false;

    private LocalDateTime createdAt;

    public UserProfile() {
    }

    public UserProfile(String name, String avatar, String color, boolean isKid) {
        this.name = name;
        this.avatar = avatar;
        this.color = color != null ? color : "from-red-600 to-rose-700";
        this.isKid = isKid;
    }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (avatar == null || avatar.trim().isEmpty()) {
            this.avatar = "🍿";
        }
        if (color == null || color.trim().isEmpty()) {
            this.color = "from-red-600 to-rose-700";
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    @JsonProperty("isKid")
    public boolean isKid() {
        return isKid;
    }

    @JsonProperty("isKid")
    public void setKid(boolean kid) {
        isKid = kid;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
