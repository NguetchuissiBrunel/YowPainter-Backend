package com.yowpainter.modules.auth.service;

import java.util.List;
import java.util.UUID;

import com.yowpainter.modules.artist.entity.Artist;
import com.yowpainter.modules.auth.dto.AdminRegisterRequest;
import com.yowpainter.modules.artist.repository.ArtistRepository;
import com.yowpainter.modules.auth.dto.AuthResponse;
import com.yowpainter.modules.auth.dto.LoginRequest;
import com.yowpainter.modules.auth.dto.RegisterRequest;
import com.yowpainter.modules.auth.entity.AppUser;
import com.yowpainter.modules.auth.entity.UserRole;
import com.yowpainter.modules.auth.repository.AppUserRepository;
import com.yowpainter.shared.security.JwtService;
import com.yowpainter.shared.tenant.TenantProvisioningService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AppUserRepository userRepository;
    private final ArtistRepository artistRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final TenantProvisioningService tenantProvisioningService;
    private final RefreshTokenService refreshTokenService;

    public List<String> getAvailableRoles() {
        return List.of(UserRole.ROLE_ARTIST.name(), UserRole.ROLE_BUYER.name());
    }

    @Transactional
    public AuthResponse registerAdmin(AdminRegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Un administrateur avec cet email existe deja");
        }

        AppUser admin = AppUser.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(UserRole.ROLE_ADMIN)
                .build();
        
        AppUser savedUser = userRepository.save(admin);

        return AuthResponse.builder()
                .accessToken(null)
                .email(savedUser.getEmail())
                .firstName(savedUser.getFirstName())
                .lastName(savedUser.getLastName())
                .profilePictureUrl(savedUser.getProfilePictureUrl())
                .role(savedUser.getRole().name())
                .tenantId("public")
                .message("Inscription réussie. Veuillez vous connecter.")
                .build();
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (request.getRole() == UserRole.ROLE_ADMIN) {
             throw new IllegalArgumentException("Le role ADMIN ne peut pas etre choisi publiquement");
        }
        
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Un utilisateur avec cet email existe deja");
        }

        AppUser savedUser;

        if (request.getRole() == UserRole.ROLE_ARTIST) {
            String slug = request.getSlug();
            if (slug == null || slug.isBlank()) {
                String baseName = (request.getArtistName() != null && !request.getArtistName().isBlank()) 
                                  ? request.getArtistName() 
                                  : (request.getFirstName() + " " + request.getLastName());
                slug = generateSlug(baseName);
            } else {
                slug = generateSlug(slug); // Normaliser même si fourni
            }

            // Unicité
            if (artistRepository.findBySlug(slug).isPresent()) {
                slug = slug + "-" + UUID.randomUUID().toString().substring(0, 5);
            }

            Artist artist = Artist.builder()
                    .firstName(request.getFirstName())
                    .lastName(request.getLastName())
                    .email(request.getEmail())
                    .passwordHash(passwordEncoder.encode(request.getPassword()))
                    .role(UserRole.ROLE_ARTIST)
                    .artistName(request.getArtistName())
                    .slug(slug)
                    .status("ACTIVE")
                    .build();
            savedUser = artistRepository.save(artist);

            // Provisionnement du schéma de base de données pour l'artiste
            tenantProvisioningService.provisionTenant(slug);

        } else if (request.getRole() == UserRole.ROLE_ADMIN) {
            AppUser admin = AppUser.builder()
                    .firstName(request.getFirstName())
                    .lastName(request.getLastName())
                    .email(request.getEmail())
                    .passwordHash(passwordEncoder.encode(request.getPassword()))
                    .role(UserRole.ROLE_ADMIN)
                    .build();
            savedUser = userRepository.save(admin);
        } else {
            AppUser buyer = AppUser.builder()
                    .firstName(request.getFirstName())
                    .lastName(request.getLastName())
                    .email(request.getEmail())
                    .passwordHash(passwordEncoder.encode(request.getPassword()))
                    .role(UserRole.ROLE_BUYER)
                    .build();
            savedUser = userRepository.save(buyer);
        }

        // Pour un visiteur/acheteur, tenant = "public". Pour un artiste, tenant = son slug
        String tenantId = savedUser.getRole() == UserRole.ROLE_ARTIST ? ((Artist) savedUser).getSlug() : "public";

        return AuthResponse.builder()
                .accessToken(null)
                .email(savedUser.getEmail())
                .firstName(savedUser.getFirstName())
                .lastName(savedUser.getLastName())
                .profilePictureUrl(savedUser.getProfilePictureUrl())
                .role(savedUser.getRole().name())
                .tenantId(tenantId)
                .artistName(savedUser instanceof Artist ? ((Artist) savedUser).getArtistName() : null)
                .message("Inscription réussie. Veuillez vous connecter.")
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        var user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Email ou mot de passe invalide"));

        String tenantId;
        if (user instanceof Artist) {
            tenantId = ((Artist) user).getSlug();
        } else {
            tenantId = "public";
        }

        var jwtToken = jwtService.generateToken(user, tenantId);
        var refreshToken = refreshTokenService.createRefreshToken(user.getId());

        return AuthResponse.builder()
                .accessToken(jwtToken)
                .refreshToken(refreshToken.getToken())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .profilePictureUrl(user.getProfilePictureUrl())
                .role(user.getRole().name())
                .tenantId(tenantId)
                .artistName(user instanceof Artist ? ((Artist) user).getArtistName() : null)
                .build();
    }

    public AuthResponse refreshToken(String requestToken) {
        return refreshTokenService.findByToken(requestToken)
                .map(refreshTokenService::verifyExpiration)
                .map(com.yowpainter.modules.auth.entity.RefreshToken::getUser)
                .map(user -> {
                    String tenantId = user instanceof Artist ? ((Artist) user).getSlug() : "public";
                    String token = jwtService.generateToken(user, tenantId);
                    return AuthResponse.builder()
                            .accessToken(token)
                            .refreshToken(requestToken)
                            .build();
                })
                .orElseThrow(() -> new RuntimeException("Refresh token is not in database!"));
    }

    @Transactional
    public void logout(AppUser user) {
        refreshTokenService.deleteByUserId(user.getId());
    }
    private String generateSlug(String input) {
        if (input == null || input.isBlank()) return "artist";
        return input.toLowerCase()
                .trim()
                .replaceAll("[^a-z0-9\\s-]", "") // Keep letters, numbers, spaces, and hyphens
                .replaceAll("\\s+", "-")       // Replace spaces with hyphens
                .replaceAll("-+", "-")        // Replace multiple hyphens with one
                .replaceAll("^-|-$", "");      // Remove leading/trailing hyphens
    }
}
