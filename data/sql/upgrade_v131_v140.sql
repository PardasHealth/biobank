RENAME TABLE clinic_shipment_patient TO shipment_patient;
RENAME TABLE dispatch_shipment_aliquot TO dispatch_aliquot;

ALTER TABLE abstract_shipment
      CHANGE COLUMN DATE_SHIPPED DEPARTED DATETIME NULL DEFAULT NULL COMMENT '';

ALTER TABLE dispatch_aliquot
      CHANGE COLUMN DISPATCH_SHIPMENT_ID DISPATCH_ID INT(11) NOT NULL COMMENT '',
      DROP INDEX FKB1B76907D8CEA57A,
      DROP INDEX FKB1B76907898584F,
      ADD INDEX FK40A7EAC2898584F (ALIQUOT_ID),
      ADD INDEX FK40A7EAC2DE99CA25 (DISPATCH_ID);

ALTER TABLE patient_visit
      CHANGE COLUMN CLINIC_SHIPMENT_PATIENT_ID SHIPMENT_PATIENT_ID INT(11) NOT NULL COMMENT '',
      DROP INDEX FKA09CAF5183AE7BBB,
      ADD INDEX FKA09CAF51859BF35A (SHIPMENT_PATIENT_ID);

ALTER TABLE shipment_patient
      CHANGE COLUMN CLINIC_SHIPMENT_ID SHIPMENT_ID INT(11) NOT NULL COMMENT '',
      DROP INDEX FKF4B18BB7E5B2B216,
      DROP INDEX FKF4B18BB7B563F38F,
      ADD INDEX FK68484540B1D3625 (SHIPMENT_ID),
      ADD INDEX FK68484540B563F38F (PATIENT_ID);

CREATE TABLE entity (
    ID INT(11) NOT NULL,
    CLASS_NAME VARCHAR(255) CHARACTER SET latin1 COLLATE latin1_general_cs NULL DEFAULT NULL,
    NAME VARCHAR(255) CHARACTER SET latin1 COLLATE latin1_general_cs NULL DEFAULT NULL,
    PRIMARY KEY (ID)
) ENGINE=MyISAM COLLATE=latin1_general_cs;

CREATE TABLE entity_column (
    ID INT(11) NOT NULL,
    NAME VARCHAR(255) CHARACTER SET latin1 COLLATE latin1_general_cs NULL DEFAULT NULL,
    ENTITY_PROPERTY_ID INT(11) NOT NULL,
    INDEX FK16BD7321698D6AC (ENTITY_PROPERTY_ID),
    PRIMARY KEY (ID)
) ENGINE=MyISAM COLLATE=latin1_general_cs;

CREATE TABLE entity_filter (
    ID INT(11) NOT NULL,
    FILTER_TYPE INT(11) NULL DEFAULT NULL,
    NAME VARCHAR(255) CHARACTER SET latin1 COLLATE latin1_general_cs NULL DEFAULT NULL,
    ENTITY_PROPERTY_ID INT(11) NOT NULL,
    INDEX FK635CF541698D6AC (ENTITY_PROPERTY_ID),
    PRIMARY KEY (ID)
) ENGINE=MyISAM COLLATE=latin1_general_cs;

CREATE TABLE entity_property (
    ID INT(11) NOT NULL,
    PROPERTY VARCHAR(255) CHARACTER SET latin1 COLLATE latin1_general_cs NULL DEFAULT NULL,
    PROPERTY_TYPE_ID INT(11) NOT NULL,
    ENTITY_ID INT(11) NULL DEFAULT NULL,
    INDEX FK3FC956B191CFD445 (ENTITY_ID),
    INDEX FK3FC956B157C0C3B0 (PROPERTY_TYPE_ID),
    PRIMARY KEY (ID)
) ENGINE=MyISAM COLLATE=latin1_general_cs;

CREATE TABLE property_modifier (
    ID INT(11) NOT NULL,
    NAME TEXT CHARACTER SET latin1 COLLATE latin1_general_cs NULL DEFAULT NULL,
    PROPERTY_MODIFIER TEXT CHARACTER SET latin1 COLLATE latin1_general_cs NULL DEFAULT NULL,
    PROPERTY_TYPE_ID INT(11) NULL DEFAULT NULL,
    INDEX FK5DF9160157C0C3B0 (PROPERTY_TYPE_ID),
    PRIMARY KEY (ID)
) ENGINE=MyISAM COLLATE=latin1_general_cs;

CREATE TABLE property_type (
    ID INT(11) NOT NULL,
    NAME VARCHAR(255) CHARACTER SET latin1 COLLATE latin1_general_cs NULL DEFAULT NULL,
    PRIMARY KEY (ID)
) ENGINE=MyISAM COLLATE=latin1_general_cs;

CREATE TABLE report (
    ID INT(11) NOT NULL,
    NAME VARCHAR(255) CHARACTER SET latin1 COLLATE latin1_general_cs NULL DEFAULT NULL,
    DESCRIPTION TEXT CHARACTER SET latin1 COLLATE latin1_general_cs NULL DEFAULT NULL,
    USER_ID INT(11) NULL DEFAULT NULL,
    IS_PUBLIC TINYINT(1) NULL DEFAULT NULL,
    IS_COUNT TINYINT(1) NULL DEFAULT NULL,
    ENTITY_ID INT(11) NOT NULL,
    INDEX FK8FDF493491CFD445 (ENTITY_ID),
    PRIMARY KEY (ID)
) ENGINE=MyISAM COLLATE=latin1_general_cs;

CREATE TABLE report_column (
    ID INT(11) NOT NULL,
    POSITION INT(11) NULL DEFAULT NULL,
    COLUMN_ID INT(11) NOT NULL,
    PROPERTY_MODIFIER_ID INT(11) NULL DEFAULT NULL,
    REPORT_ID INT(11) NULL DEFAULT NULL,
    INDEX FKF0B78C1BE9306A5 (REPORT_ID),
    INDEX FKF0B78C1C2DE3790 (PROPERTY_MODIFIER_ID),
    INDEX FKF0B78C1A946D8E8 (COLUMN_ID),
    PRIMARY KEY (ID)
) ENGINE=MyISAM COLLATE=latin1_general_cs;

CREATE TABLE report_filter (
    ID INT(11) NOT NULL,
    POSITION INT(11) NULL DEFAULT NULL,
    OPERATOR INT(11) NULL DEFAULT NULL,
    ENTITY_FILTER_ID INT(11) NOT NULL,
    REPORT_ID INT(11) NULL DEFAULT NULL,
    INDEX FK13D570E3445CEC4C (ENTITY_FILTER_ID),
    INDEX FK13D570E3BE9306A5 (REPORT_ID),
    PRIMARY KEY (ID)
) ENGINE=MyISAM COLLATE=latin1_general_cs;

CREATE TABLE report_filter_value (
    ID INT(11) NOT NULL,
    POSITION INT(11) NULL DEFAULT NULL,
    VALUE TEXT CHARACTER SET latin1 COLLATE latin1_general_cs NULL DEFAULT NULL,
    SECOND_VALUE TEXT CHARACTER SET latin1 COLLATE latin1_general_cs NULL DEFAULT NULL,
    REPORT_FILTER_ID INT(11) NULL DEFAULT NULL,
    INDEX FK691EF6F59FFD1CEE (REPORT_FILTER_ID),
    PRIMARY KEY (ID)
) ENGINE=MyISAM COLLATE=latin1_general_cs;

# sample order tables

DROP TABLE IF EXISTS order_aliquot;

CREATE TABLE order_aliquot (
    ALIQUOT_ID INT(11) NOT NULL,
    ORDER_ID INT(11) NOT NULL,
    INDEX FK74AC5176898584F (ALIQUOT_ID),
    PRIMARY KEY (ORDER_ID, ALIQUOT_ID)
) ENGINE=MyISAM COLLATE=latin1_general_cs;

DROP TABLE IF EXISTS research_group;

CREATE TABLE research_group (
    ID INT(11) NOT NULL,
    NAME VARCHAR(150) CHARACTER SET latin1 COLLATE latin1_general_cs NULL DEFAULT NULL,
    NAME_SHORT VARCHAR(50) CHARACTER SET latin1 COLLATE latin1_general_cs NULL DEFAULT NULL,
    STUDY_ID INT(11) NOT NULL,
    ADDRESS_ID INT(11) NOT NULL,
    CONSTRAINT STUDY_ID UNIQUE KEY(STUDY_ID),
    INDEX FK7E0432BB6AF2992F (ADDRESS_ID),
    INDEX FK7E0432BBF2A2464F (STUDY_ID),
    CONSTRAINT ADDRESS_ID UNIQUE KEY(ADDRESS_ID),
    PRIMARY KEY (ID)
) ENGINE=MyISAM COLLATE=latin1_general_cs;

DROP TABLE IF EXISTS research_group_researcher;

CREATE TABLE research_group_researcher (
    RESEARCH_GROUP_ID INT(11) NOT NULL,
    RESEARCHER_ID INT(11) NOT NULL,
    INDEX FK83006F8C213C8A5 (RESEARCHER_ID),
    INDEX FK83006F8C4BD922D8 (RESEARCH_GROUP_ID),
    PRIMARY KEY (RESEARCHER_ID, RESEARCH_GROUP_ID)
) ENGINE=MyISAM COLLATE=latin1_general_cs;

DROP TABLE IF EXISTS researcher;

CREATE TABLE researcher (
    ID INT(11) NOT NULL,
    PRIMARY KEY (ID)
) ENGINE=MyISAM COLLATE=latin1_general_cs;
