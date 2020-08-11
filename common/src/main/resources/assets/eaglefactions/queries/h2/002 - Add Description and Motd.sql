ALTER TABLE Factions ADD Description VARCHAR(255) NOT NULL DEFAULT '' AFTER LastOnline;
ALTER TABLE Factions ADD Motd VARCHAR(255) NOT NULL DEFAULT '' AFTER Description;

-- Set database version to 2
INSERT INTO Version VALUES (2);