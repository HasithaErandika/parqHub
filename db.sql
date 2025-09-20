-- Admin Table
CREATE TABLE Admins (
                        admin_id INT PRIMARY KEY AUTO_INCREMENT,
                        name VARCHAR(100) NOT NULL,
                        email VARCHAR(100) UNIQUE NOT NULL,
                        password VARCHAR(255) NOT NULL,
                        role ENUM('Operations Manager', 'Finance Officer', 'Security Supervisor', 'IT Support', 'Customer Service Officer', 'Super Admin') Default 'Super Admin';
);

-- User (Customer) Table
CREATE TABLE Users (
                       user_id INT PRIMARY KEY AUTO_INCREMENT,
                       name VARCHAR(100) NOT NULL,
                       email VARCHAR(100) UNIQUE NOT NULL,
                       password VARCHAR(255) NOT NULL,
                       contact_no VARCHAR(20)
);

-- Vehicle Table
CREATE TABLE Vehicle (
                         vehicle_id INT PRIMARY KEY AUTO_INCREMENT,
                         user_id INT NOT NULL,
                         vehicle_no VARCHAR(20) UNIQUE NOT NULL,
                         vehicle_type ENUM('Car', 'Bike', 'Van', 'Truck') NOT NULL,
                         brand VARCHAR(50),
                         model VARCHAR(50),
                         color VARCHAR(30),
                         FOREIGN KEY (user_id) REFERENCES Users(user_id)
);

-- ParkingLot Table
CREATE TABLE ParkingLot (
                            lot_id INT PRIMARY KEY AUTO_INCREMENT,
                            city VARCHAR(100) NOT NULL,
                            location VARCHAR(255) NOT NULL,
                            total_slots INT NOT NULL
);

-- ParkingSlot Table
CREATE TABLE ParkingSlot (
                             slot_id INT PRIMARY KEY AUTO_INCREMENT,
                             lot_id INT NOT NULL,
                             status ENUM('Available', 'Booked', 'Occupied') DEFAULT 'Available',
                             FOREIGN KEY (lot_id) REFERENCES ParkingLot(lot_id)
);

-- Booking Table
CREATE TABLE Booking (
                         booking_id INT PRIMARY KEY AUTO_INCREMENT,
                         user_id INT NOT NULL,
                         vehicle_id INT NOT NULL,
                         slot_id INT NOT NULL,
                         start_time DATETIME NOT NULL,
                         end_time DATETIME,
                         payment_status ENUM('Pending', 'Completed', 'Failed') DEFAULT 'Pending',
                         FOREIGN KEY (user_id) REFERENCES Users(user_id),
                         FOREIGN KEY (vehicle_id) REFERENCES Vehicle(vehicle_id),
                         FOREIGN KEY (slot_id) REFERENCES ParkingSlot(slot_id)
);

-- Payment Table
CREATE TABLE Payment (
                         payment_id INT PRIMARY KEY AUTO_INCREMENT,
                         booking_id INT NOT NULL,
                         amount DECIMAL(10,2) NOT NULL,
                         method ENUM('Card', 'Cash', 'Arrival') DEFAULT 'Arrival',
                         status ENUM('Pending', 'Completed', 'Failed') DEFAULT 'Pending',
                         timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
                         FOREIGN KEY (booking_id) REFERENCES Booking(booking_id)
);

-- VehicleLog Table
CREATE TABLE VehicleLog (
                            log_id INT PRIMARY KEY AUTO_INCREMENT,
                            vehicle_id INT NOT NULL,
                            entry_time DATETIME NOT NULL,
                            exit_time DATETIME,
                            lot_id INT NOT NULL,
                            FOREIGN KEY (vehicle_id) REFERENCES Vehicle(vehicle_id),
                            FOREIGN KEY (lot_id) REFERENCES ParkingLot(lot_id)
);

-- Report Table
CREATE TABLE Report (
                        report_id INT PRIMARY KEY AUTO_INCREMENT,
                        type ENUM('Financial', 'Occupancy', 'Performance') DEFAULT 'Performance',
                        generated_date DATETIME DEFAULT CURRENT_TIMESTAMP,
                        admin_id INT NOT NULL,
                        FOREIGN KEY (admin_id) REFERENCES Admins(admin_id)
);

-- Notification / Incident Table
CREATE TABLE Notification (
                              notification_id INT PRIMARY KEY AUTO_INCREMENT,
                              type ENUM('FULL_SLOT', 'OVERSTAY', 'SECURITY_INCIDENT', 'NONE') DEFAULT 'NONE',
                              description TEXT,
                              timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
                              user_id INT,
                              admin_id INT,
                              FOREIGN KEY (user_id) REFERENCES Users(user_id),
                              FOREIGN KEY (admin_id) REFERENCES Admins(admin_id)
);
