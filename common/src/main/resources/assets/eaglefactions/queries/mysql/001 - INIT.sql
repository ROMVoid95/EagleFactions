
-- Create Version Table
CREATE TABLE `Version` (
  `Version` INT NOT NULL,
  PRIMARY KEY (`Version`),
  UNIQUE INDEX `Version_UNIQUE` (`Version` ASC) VISIBLE
);

-- Create Factions Table
CREATE TABLE `Factions` (
  `id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `Name` VARCHAR(200) NOT NULL,
  `Tag` VARCHAR(200) NOT NULL,
  `TagColor` VARCHAR(40) NULL,
  `Leader` VARCHAR(36) NOT NULL,
  `Home` VARCHAR(200) NULL,
  `LastOnline` VARCHAR(200) NOT NULL,
  `Truces` VARCHAR(200) NOT NULL,
  `Alliances` VARCHAR(255) NOT NULL,
  `Enemies` VARCHAR(255) NOT NULL,
  PRIMARY KEY (`id`, `Name`),
  UNIQUE INDEX `Name_UNIQUE` (`Name` ASC)
);

-- Create Recruits Table
CREATE TABLE `FactionRecruits` (
  `RecruitUUID` VARCHAR(36) NOT NULL,
  `FactionName` VARCHAR(200) NOT NULL,
  UNIQUE INDEX `RecruitUUID_UNIQUE` (`RecruitUUID` ASC) ,
  INDEX `FactionName_idx` (`FactionName` ASC) ,
  CONSTRAINT `Faction_Recruit`
    FOREIGN KEY (`FactionName`)
    REFERENCES `Factions` (`Name`)
    ON DELETE CASCADE
    ON UPDATE CASCADE
);

-- Create Members Table
CREATE TABLE `FactionMembers` (
  `MemberUUID` VARCHAR(36) NOT NULL,
  `FactionName` VARCHAR(200) NOT NULL,
  UNIQUE INDEX `MemberUUID_UNIQUE` (`MemberUUID` ASC) ,
  INDEX `FactionName_idx` (`FactionName` ASC) ,
  CONSTRAINT `Faction_Member`
    FOREIGN KEY (`FactionName`)
    REFERENCES `Factions` (`Name`)
    ON DELETE CASCADE
    ON UPDATE CASCADE
);

-- Create Officers Table

CREATE TABLE `FactionOfficers` (
  `OfficerUUID` VARCHAR(36) NOT NULL,
  `FactionName` VARCHAR(200) NOT NULL,
  UNIQUE INDEX `OfficerUUID_UNIQUE` (`OfficerUUID` ASC) ,
  INDEX `FactionName_idx` (`FactionName` ASC) ,
  CONSTRAINT `Faction_Officer`
    FOREIGN KEY (`FactionName`)
    REFERENCES `Factions` (`Name`)
    ON DELETE CASCADE
    ON UPDATE CASCADE
);

-- Create FactionAlliances Table
-- CREATE TABLE FactionAlliances (
--   FactionName  VARCHAR(200)      UNIQUE        NOT NULL,
--   AlliancesIds VARCHAR(200)                    NOT NULL,
--   FOREIGN KEY (FactionName) REFERENCES Factions(Name)
-- );
-- CREATE UNIQUE INDEX FactionAlliances_FactionName ON FactionAlliances (FactionName);
--
-- Create FactionEnemies Table
-- CREATE TABLE FactionEnemies (
--   FactionName VARCHAR(200)        UNIQUE      NOT NULL,
--   EnemiesIds  VARCHAR(200)                    NOT NULL,
--   FOREIGN KEY (FactionName) REFERENCES Factions(Name)
-- );
-- CREATE UNIQUE INDEX FactionEnemies_FactionName ON FactionEnemies (FactionName);

-- Create FactionTruces Table
-- CREATE TABLE `FactionTruces` (
--   `FactionName`   VARCHAR(200)                             NOT NULL,
--   `TrucesIds`  VARCHAR(200)        UNIQUE      NOT NULL,
-- );
-- CREATE UNIQUE INDEX FactionTruces_FactionName ON FactionTruces (FactionName);

-- Create OfficerPerms Table
CREATE TABLE `OfficerPerms` (
  `FactionName` VARCHAR(200) NOT NULL,
  `Use` TINYINT(1) NOT NULL,
  `Place` TINYINT(1) NOT NULL,
  `Destroy` TINYINT(1) NOT NULL,
  `Claim` TINYINT(1) NOT NULL,
  `Attack` TINYINT(1) NOT NULL,
  `Invite` TINYINT(1) NOT NULL,
  UNIQUE INDEX `FactionName_UNIQUE` (`FactionName` ASC),
  CONSTRAINT `Faction_OfficerPerms`
    FOREIGN KEY (`FactionName`)
    REFERENCES `Factions` (`Name`)
    ON DELETE CASCADE
    ON UPDATE CASCADE
);

-- Create MemberPerms Table
CREATE TABLE `MemberPerms` (
  `FactionName` VARCHAR(200) NOT NULL,
  `Use` TINYINT(1) NOT NULL,
  `Place` TINYINT(1) NOT NULL,
  `Destroy` TINYINT(1) NOT NULL,
  `Claim` TINYINT(1) NOT NULL,
  `Attack` TINYINT(1) NOT NULL,
  `Invite` TINYINT(1) NOT NULL,
  UNIQUE INDEX `FactionName_UNIQUE` (`FactionName` ASC),
  CONSTRAINT `Faction_MemberPerms`
    FOREIGN KEY (`FactionName`)
    REFERENCES `Factions` (`Name`)
    ON DELETE CASCADE
    ON UPDATE CASCADE
);

-- Create RecruitPerms Table
CREATE TABLE `RecruitPerms` (
  `FactionName` VARCHAR(200) NOT NULL,
  `Use` TINYINT(1) NOT NULL,
  `Place` TINYINT(1) NOT NULL,
  `Destroy` TINYINT(1) NOT NULL,
  `Claim` TINYINT(1) NOT NULL,
  `Attack` TINYINT(1) NOT NULL,
  `Invite` TINYINT(1) NOT NULL,
  UNIQUE INDEX `FactionName_UNIQUE` (`FactionName` ASC),
  CONSTRAINT `Faction_RecruitPerms`
    FOREIGN KEY (`FactionName`)
    REFERENCES `Factions` (`Name`)
    ON DELETE CASCADE
    ON UPDATE CASCADE
);

-- Create AllyPerms Table
CREATE TABLE `AllyPerms` (
  `FactionName` VARCHAR(200) NOT NULL,
  `Use` TINYINT(1) NOT NULL,
  `Place` TINYINT(1) NOT NULL,
  `Destroy` TINYINT(1) NOT NULL,
  UNIQUE INDEX `FactionName_UNIQUE` (`FactionName` ASC),
  CONSTRAINT `Faction_AllyPerms`
    FOREIGN KEY (`FactionName`)
    REFERENCES `Factions` (`Name`)
    ON DELETE CASCADE
    ON UPDATE CASCADE
);

-- Create TrucePerms Table
CREATE TABLE `TrucePerms` (
  `FactionName` VARCHAR(200) NOT NULL,
  `Use` TINYINT(1) NOT NULL,
  `Place` TINYINT(1) NOT NULL,
  `Destroy` TINYINT(1) NOT NULL,
  UNIQUE INDEX `FactionName_UNIQUE` (`FactionName` ASC),
  CONSTRAINT `Faction_TrucePerms`
    FOREIGN KEY (`FactionName`)
    REFERENCES `Factions` (`Name`)
    ON DELETE CASCADE
    ON UPDATE CASCADE
);

-- Create Claims Table
CREATE TABLE `Claims` (
  `FactionName` VARCHAR(200) NOT NULL,
  `WorldUUID` VARCHAR(36) NOT NULL,
  `ChunkPosition` VARCHAR(200) NOT NULL,
  UNIQUE INDEX `Claim_UNIQUE` (`WorldUUID`, `ChunkPosition`),
  CONSTRAINT `Faction_Claim`
    FOREIGN KEY (`FactionName`)
    REFERENCES `Factions` (`Name`)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  PRIMARY KEY (`WorldUUID`, `ChunkPosition`)
);

-- Create FactionsChest Table
CREATE TABLE `FactionChests` (
  `FactionName` VARCHAR(200) NOT NULL,
  `ChestItems` BLOB NOT NULL,
  UNIQUE INDEX `FactionName_UNIQUE` (`FactionName` ASC),
  CONSTRAINT `Faction_FactionChest`
    FOREIGN KEY (`FactionName`)
    REFERENCES `Factions` (`Name`)
    ON DELETE CASCADE
    ON UPDATE CASCADE
);

-- Create Players Table
CREATE TABLE `Players` (
  `PlayerUUID` VARCHAR(36) NOT NULL,
  `Name` VARCHAR(200) NOT NULL,
  `Faction` VARCHAR(200) NULL,
  `Power` FLOAT NOT NULL,
  `MaxPower` FLOAT NOT NULL,
  `DeathInWarzone` TINYINT(1) NOT NULL,
  PRIMARY KEY (`PlayerUUID`),
  UNIQUE INDEX `PlayerUUID_UNIQUE` (`PlayerUUID` ASC)
);

-- Set database version to 1
INSERT INTO Version VALUES (1);