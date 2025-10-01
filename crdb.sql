USE parqhub_db;

SET FOREIGN_KEY_CHECKS = 0;

-- Drop tables if they exist
DROP TABLE IF EXISTS ParkingSlot;
DROP TABLE IF EXISTS ParkingLot;

-- Create ParkingLot table
CREATE TABLE ParkingLot (
                            lot_id INT PRIMARY KEY AUTO_INCREMENT,
                            city VARCHAR(100) NOT NULL,
                            location VARCHAR(255) NOT NULL,
                            total_slots INT NOT NULL,
                            price_hr DECIMAL(10,2) NOT NULL DEFAULT 0.00
);

-- ParkingSlot Table
CREATE TABLE ParkingSlot (
                             slot_id INT PRIMARY KEY AUTO_INCREMENT,
                             lot_id INT NOT NULL,
                             status ENUM('Available', 'Booked', 'Occupied') DEFAULT 'Available',
                             FOREIGN KEY (lot_id) REFERENCES ParkingLot(lot_id)
);

-- Adding Colombo parking lots
INSERT INTO ParkingLot (city, location, total_slots, price_hr) VALUES
                                                                   ('Colombo', 'Pettah', 50, 250.00),
                                                                   ('Colombo', 'Galle Face', 35, 200.00),
                                                                   ('Colombo', 'Maligakanda', 24, 140.00),
                                                                   ('Colombo', 'Kollupitiya', 30, 180.00),
                                                                   ('Colombo', 'SSC ', 22, 100.00);

-- Adding Galle parking lots
INSERT INTO ParkingLot (city, location, total_slots, price_hr) VALUES
                                                                   ('Galle', 'Galle Dutch Fort', 50, 250.00),
                                                                   ('Galle', 'Galle ICS', 35, 200.00),
                                                                   ('Galle', 'Galle Beach', 24, 140.00),
                                                                   ('Galle', 'Flag Rock Bastion', 30, 180.00),
                                                                   ('Galle', 'Galle Meusium 5', 22, 100.00);

-- Adding Kandy parking lots
INSERT INTO ParkingLot (city, location, total_slots, price_hr) VALUES
                                                                   ('Kandy', 'Dalada Maligawa', 50, 250.00),
                                                                   ('Kandy', 'Raja Veediya', 35, 200.00),
                                                                   ('Kandy', 'Old Royal Palace', 24, 140.00),
                                                                   ('Kandy', 'Lakeside', 30, 180.00);

SET FOREIGN_KEY_CHECKS = 1;


DELIMITER $$

DROP PROCEDURE IF EXISTS populate_random_parking_slots$$

CREATE PROCEDURE populate_random_parking_slots()
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE v_lot_id INT;
    DECLARE v_total_slots INT;
    DECLARE v_counter INT;

    DECLARE lot_cursor CURSOR FOR
SELECT lot_id, total_slots FROM ParkingLot;

DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

OPEN lot_cursor;

read_loop: LOOP
        FETCH lot_cursor INTO v_lot_id, v_total_slots;
        IF done THEN
            LEAVE read_loop;
END IF;

        SET v_counter = 1;
        WHILE v_counter <= v_total_slots DO
            INSERT INTO ParkingSlot (lot_id, status) VALUES (v_lot_id, 'Available');
            SET v_counter = v_counter + 1;
END WHILE;
END LOOP;

CLOSE lot_cursor;
END$$

DELIMITER ;


-- ✅ Now CALL the procedure to populate slots
CALL populate_random_parking_slots();

-- Optional: drop the procedure after use (use correct name!)
DROP PROCEDURE IF EXISTS populate_random_parking_slots;

-- View results
SELECT * FROM ParkingSlot;