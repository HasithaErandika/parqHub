USE parqhub_db;
SET FOREIGN_KEY_CHECKS = 0;

-- Add the new procedures for parking slot management
DELIMITER $$

-- Procedure to create parking slots for a parking lot
DROP PROCEDURE IF EXISTS create_parking_slots_for_lot$$
CREATE PROCEDURE create_parking_slots_for_lot(IN p_lot_id INT, IN p_total_slots INT)
BEGIN
    DECLARE v_counter INT DEFAULT 1;

    -- Delete existing slots for this lot
    DELETE FROM ParkingSlot WHERE lot_id = p_lot_id;

    -- Create new slots, all with "Available" status
    WHILE v_counter <= p_total_slots DO
        INSERT INTO ParkingSlot (lot_id, status) VALUES (p_lot_id, 'Available');
        SET v_counter = v_counter + 1;
    END WHILE;
END$$

-- Procedure to update parking slots for a parking lot
DROP PROCEDURE IF EXISTS update_parking_slots_for_lot$$
CREATE PROCEDURE update_parking_slots_for_lot(IN p_lot_id INT, IN p_old_total_slots INT, IN p_new_total_slots INT)
BEGIN
    DECLARE v_slots_to_add INT;
    DECLARE v_slots_to_remove INT;
    DECLARE v_counter INT DEFAULT 1;

    IF p_new_total_slots > p_old_total_slots THEN
        -- Need to add more slots
        SET v_slots_to_add = p_new_total_slots - p_old_total_slots;
        WHILE v_counter <= v_slots_to_add DO
            -- Add new slots with default 'Available' status
            INSERT INTO ParkingSlot (lot_id, status) VALUES (p_lot_id, 'Available');
            SET v_counter = v_counter + 1;
        END WHILE;
    ELSEIF p_new_total_slots < p_old_total_slots THEN
        -- Need to remove excess slots
        SET v_slots_to_remove = p_old_total_slots - p_new_total_slots;
        DELETE FROM ParkingSlot 
        WHERE lot_id = p_lot_id 
        ORDER BY slot_id DESC 
        LIMIT v_slots_to_remove;
    END IF;
    -- If equal, no action needed
END$$

-- Procedure to delete parking slots for a parking lot
DROP PROCEDURE IF EXISTS delete_parking_slots_for_lot$$
CREATE PROCEDURE delete_parking_slots_for_lot(IN p_lot_id INT)
BEGIN
    DELETE FROM ParkingSlot WHERE lot_id = p_lot_id;
END$$

DELIMITER ;

-- Existing parking lot data
DROP TABLE IF EXISTS parkinglot;

CREATE TABLE ParkingLot (
                            lot_id INT PRIMARY KEY AUTO_INCREMENT,
                            city VARCHAR(100) NOT NULL,
                            location VARCHAR(255) NOT NULL,
                            total_slots INT NOT NULL,
                            price_hr DECIMAL(10,2) NOT NULL DEFAULT 0.00
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


    USE parqhub_db;

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
            -- All slots are created with "Available" status by default
            INSERT INTO ParkingSlot (lot_id, status) VALUES (v_lot_id, 'Available');
            SET v_counter = v_counter + 1;
END WHILE;
END LOOP;

CLOSE lot_cursor;
END$$

DELIMITER ;

-- Execute the procedure to populate parking slots
CALL populate_random_parking_slots();

-- Optional: drop the procedure after use
DROP PROCEDURE populate_random_parking_slots;