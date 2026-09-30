package com.sukinema.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "user_profiles")
public class UserProfile {

    // id y fecha los fija el servidor: se ignoran si llegan en la petición
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;

    @NotBlank(message = "El nombre de perfil es obligatorio")
    @Size(max = 20, message = "El nombre de perfil no puede superar los 20 caracteres")
    @Column(nullable = false)
    private String name;

    @Size(max = 500, message = "El avatar no es válido")
    @Column(length = 500)
    private String avatar;

    @Size(max = 255, message = "El color no es válido")
    private String color;

    private boolean isKid = false;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime createdAt;

    // Cuenta a la que pertenece el perfil; nunca se expone ni se acepta desde fuera
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private Account account;

    // "Mi Lista" y los likes del perfil: ids de tráilers. Se consultan por /api/profiles/{id}/library
    @JsonIgnore
    @ElementCollection
    @CollectionTable(name = "profile_my_list", joinColumns = @JoinColumn(name = "profile_id"))
    @Column(name = "movie_id", nullable = false)
    private Set<Long> myListMovieIds = new HashSet<>();

    @JsonIgnore
    @ElementCollection
    @CollectionTable(name = "profile_likes", joinColumns = @JoinColumn(name = "profile_id"))
    @Column(name = "movie_id", nullable = false)
    private Set<Long> likedMovieIds = new HashSet<>();

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

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public Set<Long> getMyListMovieIds() {
        return myListMovieIds;
    }

    public Set<Long> getLikedMovieIds() {
        return likedMovieIds;
    }
}
