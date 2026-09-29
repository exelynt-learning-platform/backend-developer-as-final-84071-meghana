package com.booking.resourcebooking.config;

import com.booking.resourcebooking.model.*;
import com.booking.resourcebooking.repository.ReservationRepository;
import com.booking.resourcebooking.repository.ResourceRepository;
import com.booking.resourcebooking.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final ResourceRepository resourceRepository;
    private final ReservationRepository reservationRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(
            UserRepository userRepository,
            ResourceRepository resourceRepository,
            ReservationRepository reservationRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.resourceRepository = resourceRepository;
        this.reservationRepository = reservationRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            logger.info("Database already seeded. Skipping initial data creation.");
            return;
        }

        logger.info("Initializing sample users, resources, and reservations...");

        // 1. Seed Users
        User admin = userRepository.save(new User(
                "admin",
                passwordEncoder.encode("admin123"),
                "admin@booking.com",
                Role.ROLE_ADMIN
        ));

        User user1 = userRepository.save(new User(
                "user1",
                passwordEncoder.encode("user123"),
                "user1@booking.com",
                Role.ROLE_USER
        ));

        User user2 = userRepository.save(new User(
                "user2",
                passwordEncoder.encode("user123"),
                "user2@booking.com",
                Role.ROLE_USER
        ));

        // 2. Seed Resources
        Resource roomA = resourceRepository.save(new Resource(
                "Executive Conference Room A",
                "Large board room with 4K interactive presentation display and hybrid meeting system",
                "ROOM",
                new BigDecimal("50.00"),
                true
        ));

        Resource sedan = resourceRepository.save(new Resource(
                "Tesla Model S Executive",
                "Premium electric sedan with chauffeur availability for corporate travel",
                "VEHICLE",
                new BigDecimal("80.00"),
                true
        ));

        Resource cameraKit = resourceRepository.save(new Resource(
                "Cinema 4K Camera & Lighting Kit",
                "Complete video shooting rig including Sony FX6, prime lenses, and Aputure lights",
                "EQUIPMENT",
                new BigDecimal("35.00"),
                true
        ));

        Resource auditorium = resourceRepository.save(new Resource(
                "Grand Auditorium",
                "300-seat amphitheater with stage lighting and concert acoustic setup",
                "ROOM",
                new BigDecimal("250.00"),
                true
        ));

        // 3. Seed Reservations
        LocalDateTime now = LocalDateTime.now();

        // Reservation 1 for user1: Confirmed
        reservationRepository.save(new Reservation(
                user1,
                roomA,
                now.plusDays(1).withHour(10).withMinute(0),
                now.plusDays(1).withHour(12).withMinute(0),
                new BigDecimal("100.00"),
                ReservationStatus.CONFIRMED
        ));

        // Reservation 2 for user1: Pending
        reservationRepository.save(new Reservation(
                user1,
                cameraKit,
                now.plusDays(2).withHour(14).withMinute(0),
                now.plusDays(2).withHour(18).withMinute(0),
                new BigDecimal("140.00"),
                ReservationStatus.PENDING
        ));

        // Reservation 3 for user1: Cancelled
        reservationRepository.save(new Reservation(
                user1,
                sedan,
                now.plusDays(3).withHour(9).withMinute(0),
                now.plusDays(3).withHour(11).withMinute(0),
                new BigDecimal("160.00"),
                ReservationStatus.CANCELLED
        ));

        // Reservation 4 for user2: Confirmed
        reservationRepository.save(new Reservation(
                user2,
                auditorium,
                now.plusDays(4).withHour(13).withMinute(0),
                now.plusDays(4).withHour(17).withMinute(0),
                new BigDecimal("1000.00"),
                ReservationStatus.CONFIRMED
        ));

        logger.info("Sample data initialization completed successfully!");
    }
}
