USE parqhub_db;
SET FOREIGN_KEY_CHECKS = 0;

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
    DECLARE v_rand INT;
    DECLARE v_status ENUM('Available','Booked','Occupied');

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
            -- Randomly assign status
            SET v_rand = FLOOR(1 + (RAND() * 3)); -- 1,2,3
            IF v_rand = 1 THEN
                SET v_status = 'Available';
            ELSEIF v_rand = 2 THEN
                SET v_status = 'Booked';
ELSE
                SET v_status = 'Occupied';
END IF;

INSERT INTO ParkingSlot (lot_id, status) VALUES (v_lot_id, v_status);
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
