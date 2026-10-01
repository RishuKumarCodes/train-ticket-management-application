-- ==============================================================================
-- RailFlow: Train Ticket Management System - Database Schema
-- Dialect: MySQL 8.x / ANSI SQL Compatible
-- ==============================================================================

CREATE DATABASE IF NOT EXISTS train_ticket_db;
USE train_ticket_db;

-- ==============================================================================
-- 1. Authentication & Role-Based Access Control (RBAC)
-- ==============================================================================

CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NULL UNIQUE,
    phone VARCHAR(20) NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    salt VARCHAR(64) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    role ENUM('PASSENGER', 'ADMIN') NOT NULL DEFAULT 'PASSENGER',
    status ENUM('ACTIVE', 'SUSPENDED') NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_users_username (username),
    INDEX idx_users_email (email),
    INDEX idx_users_phone (phone),
    INDEX idx_users_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Pre-seeded default Fixed Administrator (admin / admin)
INSERT IGNORE INTO users (id, username, email, phone, password_hash, salt, full_name, role)
VALUES (1, 'admin', 'admin@railflow.internal', '+910000000000', 
        '463d121d09680f21241659d31b0389901d0479bcf389c231258d6c3f5c36050f', 
        '0123456789abcdef0123456789abcdef', 'Station Master Admin', 'ADMIN');

-- ==============================================================================
-- 2. Stations
-- ==============================================================================

CREATE TABLE IF NOT EXISTS stations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(10) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    city VARCHAR(100) NOT NULL,
    state VARCHAR(100) NOT NULL,
    INDEX idx_stations_code (code),
    INDEX idx_stations_city (city)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Seed Major Indian Railway Stations
INSERT IGNORE INTO stations (id, code, name, city, state) VALUES
(1, 'NDLS', 'New Delhi', 'New Delhi', 'Delhi'),
(2, 'MMCT', 'Mumbai Central', 'Mumbai', 'Maharashtra'),
(3, 'CSMT', 'Chhatrapati Shivaji Maharaj Terminus', 'Mumbai', 'Maharashtra'),
(4, 'HWH', 'Howrah Junction', 'Kolkata', 'West Bengal'),
(5, 'BSB', 'Varanasi Junction', 'Varanasi', 'Uttar Pradesh'),
(6, 'CNB', 'Kanpur Central', 'Kanpur', 'Uttar Pradesh'),
(7, 'PRYJ', 'Prayagraj Junction', 'Prayagraj', 'Uttar Pradesh'),
(8, 'DDU', 'Pt. Deen Dayal Upadhyaya Junction', 'Mughalsarai', 'Uttar Pradesh'),
(9, 'KOTA', 'Kota Junction', 'Kota', 'Rajasthan'),
(10, 'BRC', 'Vadodara Junction', 'Vadodara', 'Gujarat'),
(11, 'ST', 'Surat', 'Surat', 'Gujarat'),
(12, 'LKO', 'Lucknow Charbagh', 'Lucknow', 'Uttar Pradesh'),
(13, 'GZB', 'Ghaziabad Junction', 'Ghaziabad', 'Uttar Pradesh'),
(14, 'ALJN', 'Aligarh Junction', 'Aligarh', 'Uttar Pradesh'),
(15, 'MAS', 'MGR Chennai Central', 'Chennai', 'Tamil Nadu'),
(16, 'SBC', 'KSR Bengaluru City', 'Bengaluru', 'Karnataka'),
(17, 'MYS', 'Mysuru Junction', 'Mysuru', 'Karnataka'),
(18, 'KPD', 'Katpadi Junction', 'Vellore', 'Tamil Nadu'),
(19, 'JP', 'Jaipur Junction', 'Jaipur', 'Rajasthan'),
(20, 'ADI', 'Ahmedabad Junction', 'Ahmedabad', 'Gujarat'),
(21, 'PUNE', 'Pune Junction', 'Pune', 'Maharashtra'),
(22, 'HYB', 'Hyderabad Deccan', 'Hyderabad', 'Telangana'),
(23, 'SC', 'Secunderabad Junction', 'Hyderabad', 'Telangana'),
(24, 'BPL', 'Bhopal Junction', 'Bhopal', 'Madhya Pradesh'),
(25, 'CDG', 'Chandigarh Junction', 'Chandigarh', 'Chandigarh'),
(26, 'ASR', 'Amritsar Junction', 'Amritsar', 'Punjab'),
(27, 'PNBE', 'Patna Junction', 'Patna', 'Bihar'),
(28, 'GHY', 'Guwahati', 'Guwahati', 'Assam'),
(29, 'BBS', 'Bhubaneswar', 'Bhubaneswar', 'Odisha'),
(30, 'TVC', 'Thiruvananthapuram Central', 'Thiruvananthapuram', 'Kerala'),
(31, 'ERS', 'Ernakulam Junction', 'Kochi', 'Kerala'),
(32, 'MAO', 'Madgaon Junction', 'Goa', 'Goa'),
(33, 'AGC', 'Agra Cantt', 'Agra', 'Uttar Pradesh'),
(34, 'GWL', 'Gwalior Junction', 'Gwalior', 'Madhya Pradesh'),
(35, 'JAT', 'Jammu Tawi', 'Jammu', 'Jammu & Kashmir'),
(36, 'DDN', 'Dehradun', 'Dehradun', 'Uttarakhand'),
(37, 'HW', 'Haridwar Junction', 'Haridwar', 'Uttarakhand'),
(38, 'GKP', 'Gorakhpur Junction', 'Gorakhpur', 'Uttar Pradesh'),
(39, 'NGP', 'Nagpur Junction', 'Nagpur', 'Maharashtra'),
(40, 'VSKP', 'Visakhapatnam Junction', 'Visakhapatnam', 'Andhra Pradesh'),
(41, 'INDB', 'Indore Junction', 'Indore', 'Madhya Pradesh'),
(42, 'JBP', 'Jabalpur Junction', 'Jabalpur', 'Madhya Pradesh'),
(43, 'R', 'Raipur Junction', 'Raipur', 'Chhattisgarh'),
(44, 'RNC', 'Ranchi Junction', 'Ranchi', 'Jharkhand'),
(45, 'BZA', 'Vijayawada Junction', 'Vijayawada', 'Andhra Pradesh'),
(46, 'CBE', 'Coimbatore Junction', 'Coimbatore', 'Tamil Nadu'),
(47, 'MDU', 'Madurai Junction', 'Madurai', 'Tamil Nadu'),
(48, 'NZM', 'Hazrat Nizamuddin', 'New Delhi', 'Delhi'),
(49, 'VGLJ', 'Virangana Lakshmibai Jhansi', 'Jhansi', 'Uttar Pradesh'),
(50, 'PURI', 'Puri', 'Puri', 'Odisha');

-- ==============================================================================
-- 3. Trains
-- ==============================================================================

CREATE TABLE IF NOT EXISTS trains (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    train_number VARCHAR(10) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    type VARCHAR(50) NOT NULL, -- VANDE_BHARAT, RAJDHANI, SHATABDI, SUPERFAST, EXPRESS
    source_station_id BIGINT NOT NULL,
    dest_station_id BIGINT NOT NULL,
    runs_on VARCHAR(7) NOT NULL DEFAULT '1111111', -- Mon-Sun bitmask
    status VARCHAR(50) NOT NULL DEFAULT 'ON_TIME',
    FOREIGN KEY (source_station_id) REFERENCES stations(id),
    FOREIGN KEY (dest_station_id) REFERENCES stations(id),
    INDEX idx_trains_number (train_number),
    INDEX idx_trains_source_dest (source_station_id, dest_station_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT IGNORE INTO trains (id, train_number, name, type, source_station_id, dest_station_id, runs_on, status) VALUES
(1, '12952', 'New Delhi Tejas Rajdhani Express', 'RAJDHANI', 1, 2, '1111111', 'ON_TIME'),
(2, '12951', 'Mumbai Tejas Rajdhani Express', 'RAJDHANI', 2, 1, '1111111', 'ON_TIME'),
(3, '22436', 'Vande Bharat Express', 'VANDE_BHARAT', 1, 5, '0110111', 'ON_TIME'),
(4, '22435', 'Vande Bharat Express', 'VANDE_BHARAT', 5, 1, '0110111', 'ON_TIME'),
(5, '12004', 'Lucknow Shatabdi Express', 'SHATABDI', 1, 12, '1111111', 'ON_TIME'),
(6, '12302', 'Howrah Rajdhani Express', 'RAJDHANI', 1, 4, '1111111', 'DEPARTED'),
(7, '20608', 'Vande Bharat Express', 'VANDE_BHARAT', 15, 17, '1111101', 'ON_TIME'),
(8, '12626', 'Kerala Superfast Express', 'SUPERFAST', 1, 30, '1111111', 'ON_TIME'),
(9, '12625', 'Kerala Superfast Express', 'SUPERFAST', 30, 1, '1111111', 'ON_TIME'),
(10, '12138', 'Punjab Mail Express', 'EXPRESS', 26, 3, '1111111', 'ON_TIME'),
(11, '12137', 'Punjab Mail Express', 'EXPRESS', 3, 26, '1111111', 'ON_TIME'),
(12, '12009', 'Mumbai - Ahmedabad Shatabdi Express', 'SHATABDI', 2, 20, '1111110', 'ON_TIME'),
(13, '12010', 'Ahmedabad - Mumbai Shatabdi Express', 'SHATABDI', 20, 2, '1111110', 'ON_TIME'),
(14, '20901', 'Vande Bharat Express', 'VANDE_BHARAT', 2, 20, '1111101', 'ON_TIME'),
(15, '20902', 'Vande Bharat Express', 'VANDE_BHARAT', 20, 2, '1111101', 'ON_TIME'),
(16, '12015', 'Ajmer Shatabdi Express', 'SHATABDI', 1, 19, '1111111', 'ON_TIME'),
(17, '12016', 'Ajmer - New Delhi Shatabdi Express', 'SHATABDI', 19, 1, '1111111', 'ON_TIME'),
(18, '12393', 'Sampoorna Kranti Superfast Express', 'SUPERFAST', 27, 1, '1111111', 'ON_TIME'),
(19, '12394', 'Sampoorna Kranti Superfast Express', 'SUPERFAST', 1, 27, '1111111', 'ON_TIME'),
(20, '12046', 'Chandigarh Shatabdi Express', 'SHATABDI', 25, 1, '1111110', 'ON_TIME'),
(21, '12424', 'Dibrugarh Rajdhani Express', 'RAJDHANI', 1, 28, '1111111', 'ON_TIME'),
(22, '12724', 'Telangana Superfast Express', 'SUPERFAST', 1, 22, '1111111', 'ON_TIME');

-- ==============================================================================
-- 4. Station Route Stops (Ordered Halts)
-- ==============================================================================

CREATE TABLE IF NOT EXISTS train_routes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    train_id BIGINT NOT NULL,
    station_id BIGINT NOT NULL,
    stop_sequence INT NOT NULL,
    arrival_time TIME NULL,
    departure_time TIME NULL,
    halt_minutes INT DEFAULT 0,
    distance_km INT NOT NULL DEFAULT 0,
    day_count INT NOT NULL DEFAULT 1,
    FOREIGN KEY (train_id) REFERENCES trains(id) ON DELETE CASCADE,
    FOREIGN KEY (station_id) REFERENCES stations(id),
    UNIQUE KEY uk_train_seq (train_id, stop_sequence),
    INDEX idx_routes_train (train_id),
    INDEX idx_routes_station (station_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Halts for 12952 (NDLS -> MMCT)
INSERT IGNORE INTO train_routes (train_id, station_id, stop_sequence, arrival_time, departure_time, halt_minutes, distance_km, day_count) VALUES
(1, 1, 1, NULL, '16:55:00', 0, 0, 1),
(1, 9, 2, '21:30:00', '21:40:00', 10, 465, 1),
(1, 10, 3, '03:15:00', '03:25:00', 10, 992, 2),
(1, 11, 4, '04:50:00', '04:55:00', 5, 1122, 2),
(1, 2, 5, '08:35:00', NULL, 0, 1384, 2);

-- Halts for 12951 (MMCT -> NDLS)
INSERT IGNORE INTO train_routes (train_id, station_id, stop_sequence, arrival_time, departure_time, halt_minutes, distance_km, day_count) VALUES
(2, 2, 1, NULL, '17:00:00', 0, 0, 1),
(2, 11, 2, '19:43:00', '19:48:00', 5, 263, 1),
(2, 10, 3, '21:06:00', '21:16:00', 10, 392, 1),
(2, 9, 4, '03:15:00', '03:20:00', 5, 919, 2),
(2, 1, 5, '08:32:00', NULL, 0, 1384, 2);

-- Halts for 22436 (NDLS -> BSB Vande Bharat)
INSERT IGNORE INTO train_routes (train_id, station_id, stop_sequence, arrival_time, departure_time, halt_minutes, distance_km, day_count) VALUES
(3, 1, 1, NULL, '06:00:00', 0, 0, 1),
(3, 6, 2, '10:10:00', '10:14:00', 4, 440, 1),
(3, 7, 3, '12:08:00', '12:10:00', 2, 635, 1),
(3, 5, 4, '14:00:00', NULL, 0, 759, 1);

-- Halts for 22435 (BSB -> NDLS Vande Bharat)
INSERT IGNORE INTO train_routes (train_id, station_id, stop_sequence, arrival_time, departure_time, halt_minutes, distance_km, day_count) VALUES
(4, 5, 1, NULL, '15:00:00', 0, 0, 1),
(4, 7, 2, '16:30:00', '16:32:00', 2, 124, 1),
(4, 6, 3, '18:30:00', '18:34:00', 4, 319, 1),
(4, 1, 4, '23:00:00', NULL, 0, 759, 1);

-- Halts for 12004 (NDLS -> LKO Shatabdi)
INSERT IGNORE INTO train_routes (train_id, station_id, stop_sequence, arrival_time, departure_time, halt_minutes, distance_km, day_count) VALUES
(5, 1, 1, NULL, '06:10:00', 0, 0, 1),
(5, 13, 2, '06:45:00', '06:47:00', 2, 26, 1),
(5, 14, 3, '07:47:00', '07:49:00', 2, 131, 1),
(5, 6, 4, '11:20:00', '11:25:00', 5, 440, 1),
(5, 12, 5, '12:40:00', NULL, 0, 512, 1);

-- Halts for 12302 (NDLS -> HWH Rajdhani)
INSERT IGNORE INTO train_routes (train_id, station_id, stop_sequence, arrival_time, departure_time, halt_minutes, distance_km, day_count) VALUES
(6, 1, 1, NULL, '16:50:00', 0, 0, 1),
(6, 6, 2, '21:32:00', '21:37:00', 5, 440, 1),
(6, 7, 3, '23:43:00', '23:45:00', 2, 635, 1),
(6, 8, 4, '01:37:00', '01:47:00', 10, 788, 2),
(6, 4, 5, '09:55:00', NULL, 0, 1451, 2);

-- Halts for 20608 (MAS -> MYS Vande Bharat)
INSERT IGNORE INTO train_routes (train_id, station_id, stop_sequence, arrival_time, departure_time, halt_minutes, distance_km, day_count) VALUES
(7, 15, 1, NULL, '05:50:00', 0, 0, 1),
(7, 18, 2, '07:13:00', '07:15:00', 2, 130, 1),
(7, 16, 3, '10:15:00', '10:20:00', 5, 359, 1),
(7, 17, 4, '12:20:00', NULL, 0, 497, 1);

-- Halts for 12626 (NDLS -> TVC Kerala Superfast)
INSERT IGNORE INTO train_routes (train_id, station_id, stop_sequence, arrival_time, departure_time, halt_minutes, distance_km, day_count) VALUES
(8, 1, 1, NULL, '20:10:00', 0, 0, 1),
(8, 33, 2, '22:20:00', '22:25:00', 5, 195, 1),
(8, 34, 3, '23:43:00', '23:45:00', 2, 313, 1),
(8, 24, 4, '05:20:00', '05:25:00', 5, 705, 2),
(8, 39, 5, '11:45:00', '11:50:00', 5, 1090, 2),
(8, 45, 6, '22:15:00', '22:25:00', 10, 1754, 2),
(8, 15, 7, '04:30:00', '04:55:00', 25, 2185, 3),
(8, 18, 8, '06:48:00', '06:50:00', 2, 2315, 3),
(8, 31, 9, '14:15:00', '14:20:00', 5, 2800, 3),
(8, 30, 10, '18:00:00', NULL, 0, 3036, 3);

-- Halts for 12625 (TVC -> NDLS Kerala Superfast)
INSERT IGNORE INTO train_routes (train_id, station_id, stop_sequence, arrival_time, departure_time, halt_minutes, distance_km, day_count) VALUES
(9, 30, 1, NULL, '11:15:00', 0, 0, 1),
(9, 31, 2, '15:35:00', '15:40:00', 5, 236, 1),
(9, 18, 3, '22:58:00', '23:00:00', 2, 721, 1),
(9, 15, 4, '00:50:00', '01:15:00', 25, 851, 2),
(9, 45, 5, '07:10:00', '07:20:00', 10, 1282, 2),
(9, 39, 6, '17:40:00', '17:45:00', 5, 1946, 2),
(9, 24, 7, '23:55:00', '00:05:00', 10, 2331, 2),
(9, 34, 8, '05:28:00', '05:30:00', 2, 2723, 3),
(9, 33, 9, '07:10:00', '07:15:00', 5, 2841, 3),
(9, 1, 10, '10:25:00', NULL, 0, 3036, 3);

-- Halts for 12138 (ASR -> CSMT Punjab Mail)
INSERT IGNORE INTO train_routes (train_id, station_id, stop_sequence, arrival_time, departure_time, halt_minutes, distance_km, day_count) VALUES
(10, 26, 1, NULL, '21:45:00', 0, 0, 1),
(10, 25, 2, '01:40:00', '01:50:00', 10, 230, 2),
(10, 1, 3, '05:00:00', '05:15:00', 15, 490, 2),
(10, 33, 4, '07:45:00', '07:50:00', 5, 685, 2),
(10, 34, 5, '09:12:00', '09:14:00', 2, 803, 2),
(10, 24, 6, '16:30:00', '16:40:00', 10, 1195, 2),
(10, 39, 7, '23:25:00', '23:35:00', 10, 1580, 2),
(10, 3, 8, '07:35:00', NULL, 0, 1925, 3);

-- Halts for 12137 (CSMT -> ASR Punjab Mail)
INSERT IGNORE INTO train_routes (train_id, station_id, stop_sequence, arrival_time, departure_time, halt_minutes, distance_km, day_count) VALUES
(11, 3, 1, NULL, '19:35:00', 0, 0, 1),
(11, 39, 2, '03:45:00', '03:55:00', 10, 345, 2),
(11, 24, 3, '10:20:00', '10:30:00', 10, 730, 2),
(11, 34, 4, '17:35:00', '17:37:00', 2, 1122, 2),
(11, 33, 5, '19:10:00', '19:15:00', 5, 1240, 2),
(11, 1, 6, '21:30:00', '21:50:00', 20, 1435, 2),
(11, 25, 7, '01:10:00', '01:20:00', 10, 1695, 3),
(11, 26, 8, '05:10:00', NULL, 0, 1925, 3);

-- Halts for 12009 (MMCT -> ADI Shatabdi)
INSERT IGNORE INTO train_routes (train_id, station_id, stop_sequence, arrival_time, departure_time, halt_minutes, distance_km, day_count) VALUES
(12, 2, 1, NULL, '06:20:00', 0, 0, 1),
(12, 11, 2, '09:15:00', '09:18:00', 3, 263, 1),
(12, 10, 3, '10:48:00', '10:53:00', 5, 392, 1),
(12, 20, 4, '12:45:00', NULL, 0, 492, 1);

-- Halts for 12010 (ADI -> MMCT Shatabdi)
INSERT IGNORE INTO train_routes (train_id, station_id, stop_sequence, arrival_time, departure_time, halt_minutes, distance_km, day_count) VALUES
(13, 20, 1, NULL, '15:10:00', 0, 0, 1),
(13, 10, 2, '16:42:00', '16:47:00', 5, 100, 1),
(13, 11, 3, '18:02:00', '18:07:00', 5, 229, 1),
(13, 2, 4, '21:45:00', NULL, 0, 492, 1);

-- Halts for 20901 (MMCT -> ADI Vande Bharat)
INSERT IGNORE INTO train_routes (train_id, station_id, stop_sequence, arrival_time, departure_time, halt_minutes, distance_km, day_count) VALUES
(14, 2, 1, NULL, '06:00:00', 0, 0, 1),
(14, 11, 2, '08:37:00', '08:40:00', 3, 263, 1),
(14, 10, 3, '09:56:00', '09:59:00', 3, 392, 1),
(14, 20, 4, '11:25:00', NULL, 0, 492, 1);

-- Halts for 20902 (ADI -> MMCT Vande Bharat)
INSERT IGNORE INTO train_routes (train_id, station_id, stop_sequence, arrival_time, departure_time, halt_minutes, distance_km, day_count) VALUES
(15, 20, 1, NULL, '15:00:00', 0, 0, 1),
(15, 10, 2, '16:13:00', '16:15:00', 2, 100, 1),
(15, 11, 3, '17:33:00', '17:36:00', 3, 229, 1),
(15, 2, 4, '20:25:00', NULL, 0, 492, 1);

-- Halts for 12015 (NDLS -> JP Shatabdi)
INSERT IGNORE INTO train_routes (train_id, station_id, stop_sequence, arrival_time, departure_time, halt_minutes, distance_km, day_count) VALUES
(16, 1, 1, NULL, '06:10:00', 0, 0, 1),
(16, 13, 2, '06:48:00', '06:50:00', 2, 26, 1),
(16, 19, 3, '10:40:00', NULL, 0, 308, 1);

-- Halts for 12016 (JP -> NDLS Shatabdi)
INSERT IGNORE INTO train_routes (train_id, station_id, stop_sequence, arrival_time, departure_time, halt_minutes, distance_km, day_count) VALUES
(17, 19, 1, NULL, '17:50:00', 0, 0, 1),
(17, 13, 2, '21:40:00', '21:42:00', 2, 282, 1),
(17, 1, 3, '22:40:00', NULL, 0, 308, 1);

-- Halts for 12393 (PNBE -> NDLS Sampoorna Kranti)
INSERT IGNORE INTO train_routes (train_id, station_id, stop_sequence, arrival_time, departure_time, halt_minutes, distance_km, day_count) VALUES
(18, 27, 1, NULL, '19:25:00', 0, 0, 1),
(18, 8, 2, '22:20:00', '22:30:00', 10, 212, 1),
(18, 6, 3, '02:25:00', '02:30:00', 5, 558, 2),
(18, 1, 4, '07:55:00', NULL, 0, 998, 2);

-- Halts for 12394 (NDLS -> PNBE Sampoorna Kranti)
INSERT IGNORE INTO train_routes (train_id, station_id, stop_sequence, arrival_time, departure_time, halt_minutes, distance_km, day_count) VALUES
(19, 1, 1, NULL, '17:30:00', 0, 0, 1),
(19, 6, 2, '22:22:00', '22:30:00', 8, 440, 1),
(19, 8, 3, '02:25:00', '02:35:00', 10, 786, 2),
(19, 27, 4, '06:50:00', NULL, 0, 998, 2);

-- Halts for 12046 (CDG -> NDLS Shatabdi)
INSERT IGNORE INTO train_routes (train_id, station_id, stop_sequence, arrival_time, departure_time, halt_minutes, distance_km, day_count) VALUES
(20, 25, 1, NULL, '12:05:00', 0, 0, 1),
(20, 1, 2, '15:20:00', NULL, 0, 244, 1);

-- Halts for 12424 (NDLS -> GHY Dibrugarh Rajdhani)
INSERT IGNORE INTO train_routes (train_id, station_id, stop_sequence, arrival_time, departure_time, halt_minutes, distance_km, day_count) VALUES
(21, 1, 1, NULL, '16:20:00', 0, 0, 1),
(21, 6, 2, '21:02:00', '21:07:00', 5, 440, 1),
(21, 8, 3, '01:23:00', '01:33:00', 10, 788, 2),
(21, 27, 4, '04:10:00', '04:20:00', 10, 1000, 2),
(21, 28, 5, '19:30:00', NULL, 0, 1880, 2);

-- Halts for 12724 (NDLS -> HYB Telangana Superfast)
INSERT IGNORE INTO train_routes (train_id, station_id, stop_sequence, arrival_time, departure_time, halt_minutes, distance_km, day_count) VALUES
(22, 1, 1, NULL, '16:00:00', 0, 0, 1),
(22, 33, 2, '18:05:00', '18:07:00', 2, 195, 1),
(22, 34, 3, '19:28:00', '19:30:00', 2, 313, 1),
(22, 24, 4, '01:20:00', '01:30:00', 10, 705, 2),
(22, 39, 5, '07:10:00', '07:15:00', 5, 1090, 2),
(22, 23, 6, '16:15:00', '16:20:00', 5, 1665, 2),
(22, 22, 7, '17:10:00', NULL, 0, 1675, 2);

-- ==============================================================================
-- 5. Seat Inventory & Class Pricing
-- ==============================================================================

CREATE TABLE IF NOT EXISTS train_classes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    train_id BIGINT NOT NULL,
    class_code VARCHAR(10) NOT NULL, -- 1A, 2A, 3A, SL, CC, EC
    quota_code VARCHAR(20) NOT NULL DEFAULT 'GENERAL', -- GENERAL, TATKAL, PREMIUM_TATKAL
    total_seats INT NOT NULL,
    available_seats INT NOT NULL,
    rac_seats INT NOT NULL DEFAULT 0,
    waitlist_seats INT NOT NULL DEFAULT 0,
    base_fare DECIMAL(10, 2) NOT NULL,
    FOREIGN KEY (train_id) REFERENCES trains(id) ON DELETE CASCADE,
    INDEX idx_classes_train_code (train_id, class_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Classes for 12952 (Tejas Rajdhani)
INSERT IGNORE INTO train_classes (train_id, class_code, quota_code, total_seats, available_seats, rac_seats, waitlist_seats, base_fare) VALUES
(1, '1A', 'GENERAL', 24, 8, 0, 0, 4280.00),
(1, '2A', 'GENERAL', 120, 36, 12, 0, 2850.00),
(1, '3A', 'GENERAL', 360, 94, 28, 0, 2080.00);

-- Classes for 12951 (Mumbai Tejas Rajdhani)
INSERT IGNORE INTO train_classes (train_id, class_code, quota_code, total_seats, available_seats, rac_seats, waitlist_seats, base_fare) VALUES
(2, '1A', 'GENERAL', 24, 6, 0, 0, 4280.00),
(2, '2A', 'GENERAL', 120, 40, 10, 0, 2850.00),
(2, '3A', 'GENERAL', 360, 110, 15, 0, 2080.00);

-- Classes for 22436 (Vande Bharat Express)
INSERT IGNORE INTO train_classes (train_id, class_code, quota_code, total_seats, available_seats, rac_seats, waitlist_seats, base_fare) VALUES
(3, 'CC', 'GENERAL', 520, 142, 18, 0, 1750.00),
(3, 'EC', 'GENERAL', 52, 19, 0, 0, 3300.00);

-- Classes for 22435 (Vande Bharat Express)
INSERT IGNORE INTO train_classes (train_id, class_code, quota_code, total_seats, available_seats, rac_seats, waitlist_seats, base_fare) VALUES
(4, 'CC', 'GENERAL', 520, 120, 10, 0, 1750.00),
(4, 'EC', 'GENERAL', 52, 15, 0, 0, 3300.00);

-- Classes for 12004 (Lucknow Shatabdi)
INSERT IGNORE INTO train_classes (train_id, class_code, quota_code, total_seats, available_seats, rac_seats, waitlist_seats, base_fare) VALUES
(5, 'CC', 'GENERAL', 450, 115, 14, 0, 1165.00),
(5, 'EC', 'GENERAL', 46, 12, 0, 0, 2125.00);

-- Classes for 12302 (Howrah Rajdhani)
INSERT IGNORE INTO train_classes (train_id, class_code, quota_code, total_seats, available_seats, rac_seats, waitlist_seats, base_fare) VALUES
(6, '1A', 'GENERAL', 24, 2, 0, 0, 4650.00),
(6, '2A', 'GENERAL', 120, 42, 8, 0, 3050.00),
(6, '3A', 'GENERAL', 360, 110, 20, 0, 2220.00);

-- Classes for 20608 (Chennai-Mysuru Vande Bharat)
INSERT IGNORE INTO train_classes (train_id, class_code, quota_code, total_seats, available_seats, rac_seats, waitlist_seats, base_fare) VALUES
(7, 'CC', 'GENERAL', 520, 168, 24, 0, 1200.00),
(7, 'EC', 'GENERAL', 52, 22, 0, 0, 2295.00);

-- Classes for Superfast / Express Trains (8 to 22)
INSERT IGNORE INTO train_classes (train_id, class_code, quota_code, total_seats, available_seats, rac_seats, waitlist_seats, base_fare) VALUES
(8, '2A', 'GENERAL', 96, 28, 6, 0, 2450.00),
(8, '3A', 'GENERAL', 380, 112, 24, 0, 1720.00),
(8, 'SL', 'GENERAL', 640, 210, 45, 0, 680.00),
(9, '2A', 'GENERAL', 96, 24, 8, 0, 2450.00),
(9, '3A', 'GENERAL', 380, 95, 20, 0, 1720.00),
(9, 'SL', 'GENERAL', 640, 180, 40, 0, 680.00),
(10, '2A', 'GENERAL', 96, 32, 4, 0, 2450.00),
(10, '3A', 'GENERAL', 380, 124, 18, 0, 1720.00),
(10, 'SL', 'GENERAL', 640, 220, 35, 0, 680.00),
(11, '2A', 'GENERAL', 96, 30, 6, 0, 2450.00),
(11, '3A', 'GENERAL', 380, 115, 22, 0, 1720.00),
(11, 'SL', 'GENERAL', 640, 195, 42, 0, 680.00),
(12, 'CC', 'GENERAL', 480, 134, 16, 0, 1350.00),
(12, 'EC', 'GENERAL', 48, 16, 0, 0, 2400.00),
(13, 'CC', 'GENERAL', 480, 140, 12, 0, 1350.00),
(13, 'EC', 'GENERAL', 48, 18, 0, 0, 2400.00),
(14, 'CC', 'GENERAL', 520, 160, 20, 0, 1750.00),
(14, 'EC', 'GENERAL', 52, 24, 0, 0, 3300.00),
(15, 'CC', 'GENERAL', 520, 155, 18, 0, 1750.00),
(15, 'EC', 'GENERAL', 52, 20, 0, 0, 3300.00),
(16, 'CC', 'GENERAL', 480, 128, 14, 0, 1350.00),
(16, 'EC', 'GENERAL', 48, 14, 0, 0, 2400.00),
(17, 'CC', 'GENERAL', 480, 135, 15, 0, 1350.00),
(17, 'EC', 'GENERAL', 48, 15, 0, 0, 2400.00),
(18, '2A', 'GENERAL', 96, 26, 8, 0, 2450.00),
(18, '3A', 'GENERAL', 380, 105, 25, 0, 1720.00),
(18, 'SL', 'GENERAL', 640, 200, 50, 0, 680.00),
(19, '2A', 'GENERAL', 96, 34, 5, 0, 2450.00),
(19, '3A', 'GENERAL', 380, 130, 16, 0, 1720.00),
(19, 'SL', 'GENERAL', 640, 240, 30, 0, 680.00),
(20, 'CC', 'GENERAL', 480, 150, 10, 0, 1350.00),
(20, 'EC', 'GENERAL', 48, 20, 0, 0, 2400.00),
(21, '1A', 'GENERAL', 24, 4, 0, 0, 4650.00),
(21, '2A', 'GENERAL', 120, 45, 6, 0, 3050.00),
(21, '3A', 'GENERAL', 380, 120, 18, 0, 2220.00),
(22, '2A', 'GENERAL', 96, 25, 7, 0, 2450.00),
(22, '3A', 'GENERAL', 380, 108, 22, 0, 1720.00),
(22, 'SL', 'GENERAL', 640, 215, 40, 0, 680.00);

-- ==============================================================================
-- 6. Bookings & Passenger Tickets
-- ==============================================================================

CREATE TABLE IF NOT EXISTS bookings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    pnr VARCHAR(15) NOT NULL UNIQUE,
    user_id BIGINT NULL,
    train_id BIGINT NOT NULL,
    journey_date DATE NOT NULL,
    from_station_id BIGINT NOT NULL,
    to_station_id BIGINT NOT NULL,
    class_code VARCHAR(10) NOT NULL,
    quota_code VARCHAR(20) NOT NULL DEFAULT 'GENERAL',
    total_fare DECIMAL(10, 2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED', -- CONFIRMED, CANCELLED, WAITLIST
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (train_id) REFERENCES trains(id),
    FOREIGN KEY (from_station_id) REFERENCES stations(id),
    FOREIGN KEY (to_station_id) REFERENCES stations(id),
    INDEX idx_bookings_user (user_id),
    INDEX idx_bookings_pnr (pnr)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS booking_passengers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_id BIGINT NOT NULL,
    passenger_name VARCHAR(100) NOT NULL,
    age INT NOT NULL,
    gender VARCHAR(10) NOT NULL,
    berth_preference VARCHAR(20),
    coach_number VARCHAR(10),
    seat_number INT,
    status VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED',
    FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT IGNORE INTO bookings (id, pnr, user_id, train_id, journey_date, from_station_id, to_station_id, class_code, quota_code, total_fare, status) VALUES
(1, '234-8901234', 1, 1, DATE_ADD(CURRENT_DATE, INTERVAL 2 DAY), 1, 2, '3A', 'GENERAL', 2080.00, 'CONFIRMED');

INSERT IGNORE INTO booking_passengers (id, booking_id, passenger_name, age, gender, berth_preference, coach_number, seat_number, status) VALUES
(1, 1, 'Rishu Kumar', 26, 'M', 'LOWER', 'B2', 34, 'CONFIRMED');

