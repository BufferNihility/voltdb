CREATE TABLE R1 (
	PKEY INTEGER NOT NULL,
	A1 INTEGER NOT NULL,
	B1 INTEGER NOT NULL,
	C1 INTEGER NOT NULL,
	D1 INTEGER NOT NULL,		
	PRIMARY KEY (PKEY)
);

CREATE VIEW V_R1 (V_A1, V_B1, V_CNT, V_SUM_C1, V_SUM_D1)
    AS SELECT A1, B1, COUNT(*), SUM(C1), COUNT(D1) 
    FROM R1  GROUP BY A1, B1;

CREATE TABLE R1V (
	V_A1 INTEGER NOT NULL,
	V_B1 INTEGER NOT NULL,
	V_CNT INTEGER NOT NULL,
	V_SUM_C1 INTEGER NOT NULL,
	V_SUM_D1 INTEGER NOT NULL,		
	PRIMARY KEY (V_A1, V_B1)
);

CREATE TABLE R2 (
	PKEY INTEGER NOT NULL,
	A INTEGER NOT NULL,
	B INTEGER NOT NULL,
	C INTEGER NOT NULL,
	PRIMARY KEY (PKEY)
);
CREATE INDEX partial_idx_R2 on R2 (A) where B > 3;

CREATE TABLE P1 (
	PKEY INTEGER NOT NULL,
	A1 INTEGER NOT NULL,
	B1 INTEGER NOT NULL,
	C1 INTEGER NOT NULL,
	D1 INTEGER NOT NULL,		
	PRIMARY KEY (PKEY)
);

PARTITION TABLE P1 ON COLUMN PKEY;

CREATE VIEW V_P1_NO_FIX_NEEDED (V_A1, V_PKEY, V_CNT, V_SUM_C1, V_SUM_D1)
    AS SELECT A1, PKEY, COUNT(*), SUM(C1), COUNT(D1) 
    FROM P1  GROUP BY A1, PKEY;

CREATE VIEW V_P1 (V_A1, V_B1, V_CNT, V_SUM_C1, V_SUM_D1)
    AS SELECT A1, B1, COUNT(*), SUM(C1), COUNT(D1) 
    FROM P1  GROUP BY A1, B1;
    
CREATE VIEW V_P1_ABS (V_A1, V_B1, V_CNT, V_SUM_C1, V_SUM_D1)
    AS SELECT abs(A1), B1, COUNT(*), SUM(C1), COUNT(D1) 
    FROM P1  GROUP BY abs(A1), B1;
    
CREATE VIEW V_P1_NEW (V_A1, V_B1, V_CNT, V_SUM_C1, V_MIN_C1, V_MAX_D1, V_SUM_D1)
    AS SELECT A1, B1, COUNT(*), SUM(C1), MIN(C1), MAX(D1), COUNT(D1)
    FROM P1  GROUP BY A1, B1;
        

CREATE TABLE P2 ( 
  ID INTEGER NOT NULL, 
  NUM INTEGER, 
  CONSTRAINT P2_PK_TREE PRIMARY KEY (ID) 
); 
CREATE INDEX P1_IDX_NUM_TREE ON P2 (NUM); 
PARTITION TABLE P2 ON COLUMN ID; 


CREATE TABLE T1 (
	PKEY INTEGER NOT NULL,
	A1 INTEGER NOT NULL,	
	PRIMARY KEY (PKEY)
);
PARTITION TABLE T1 ON COLUMN PKEY;

CREATE TABLE T2 (
	PKEY INTEGER NOT NULL,
	I INTEGER NOT NULL,
	PRIMARY KEY (PKEY)
);
PARTITION TABLE T2 ON COLUMN PKEY;

CREATE TABLE T3 (
	PKEY INTEGER NOT NULL,
	A3 INTEGER NOT NULL,
	B3 INTEGER NOT NULL,
	C3 INTEGER NOT NULL,
	D3 INTEGER NOT NULL,
	PRIMARY KEY (PKEY, A3)
);
PARTITION TABLE T3 ON COLUMN A3;
CREATE INDEX T3_TREE1 ON T3 (A3, B3);

CREATE TABLE T4 (
	PKEY INTEGER NOT NULL,
	A4 INTEGER NOT NULL,
	B4 INTEGER NOT NULL,
	C4 INTEGER NOT NULL,
	PRIMARY KEY (PKEY, A4)
);
PARTITION TABLE T4 ON COLUMN A4;

CREATE TABLE D1 (
	D1_PKEY INTEGER NOT NULL,
	D1_NAME VARCHAR(10) NOT NULL,
	PRIMARY KEY (D1_PKEY)
);

CREATE TABLE D2 (
	D2_PKEY INTEGER NOT NULL,
	D2_NAME VARCHAR(10) NOT NULL,
	PRIMARY KEY (D2_PKEY)
);

CREATE TABLE D3 (
	D3_PKEY INTEGER NOT NULL,
	D3_NAME VARCHAR(10) NOT NULL,
	PRIMARY KEY (D3_PKEY)
);

CREATE TABLE F (
	F_PKEY INTEGER NOT NULL,
	F_D1   INTEGER NOT NULL,
	F_D2   INTEGER NOT NULL,
	F_D3   INTEGER NOT NULL,
	F_VAL1 INTEGER NOT NULL,
	F_VAL2 INTEGER NOT NULL,
	F_VAL3 INTEGER NOT NULL,
	PRIMARY KEY (F_PKEY)
);

PARTITION TABLE F ON COLUMN F_PKEY;

CREATE INDEX COL_F_TREE1 ON F (F_D1);
CREATE INDEX COL_F_TREE2 ON F (F_VAL1, F_VAL2);
CREATE INDEX EXPR_F_TREE1 ON F (F_D1 + F_D2);
CREATE INDEX EPXR_F_TREE2 ON F (ABS(F_D1), F_D2 - F_D3);

CREATE VIEW V (V_D1_PKEY, V_D2_PKEY, V_D3_PKEY, V_F_PKEY, CNT, SUM_V1, SUM_V2, SUM_V3)
    AS SELECT F_D1, F_D2, F_D3, F_PKEY, COUNT(*), SUM(F_VAL1), SUM(F_VAL2), SUM(F_VAL3)
    FROM F  GROUP BY F_D1, F_D2, F_D3, F_PKEY;

CREATE TABLE RF (
    F_PKEY INTEGER NOT NULL,
    F_D1   INTEGER NOT NULL,
    F_D2   INTEGER NOT NULL,
    F_D3   INTEGER NOT NULL,
    F_VAL1 INTEGER NOT NULL,
    F_VAL2 INTEGER NOT NULL,
    F_VAL3 INTEGER NOT NULL,
    PRIMARY KEY (F_PKEY)
);

CREATE INDEX COL_RF_TREE1 ON RF (F_D1);
CREATE INDEX COL_RF_TREE2 ON RF (F_VAL1, F_VAL2);
CREATE UNIQUE INDEX EXPR_RF_TREE1 ON RF (F_D1 + F_D2);
CREATE INDEX EPXR_RF_TREE2 ON RF (ABS(F_D1), F_D2 - F_D3);
CREATE INDEX COL_RF_HASH ON RF (F_VAL3);

CREATE TABLE G (
	G_PKEY INTEGER NOT NULL,
	G_D1   INTEGER NOT NULL,
	G_D2   INTEGER NOT NULL,
	G_D3   INTEGER NOT NULL
);
PARTITION TABLE G ON COLUMN G_PKEY;

CREATE INDEX COL_G_TREE1 ON G (G_D1);

CREATE TABLE B (
	B_PKEY INTEGER NOT NULL,
	B_VAL1 VARBINARY(6) NOT NULL,
	PRIMARY KEY (B_PKEY)
);


--- ENG-5386: voter query example.
CREATE TABLE contestants
(
  contestant_number integer     NOT NULL
, contestant_name   varchar(50) NOT NULL
, CONSTRAINT PK_contestants PRIMARY KEY
  (
    contestant_number
  )
);

CREATE TABLE votes
(
  phone_number       bigint     NOT NULL
, state              varchar(2) NOT NULL
, contestant_number  integer    NOT NULL
);

PARTITION TABLE votes ON COLUMN phone_number;

CREATE INDEX votes_state_tree1 ON votes (state);

CREATE VIEW v_votes_by_contestant_number_state
(
  contestant_number
, state
, num_votes
)
AS
   SELECT contestant_number
        , state
        , COUNT(*)
     FROM votes
 GROUP BY contestant_number
        , state
;


CREATE TABLE votesBytes
(
  phone_number       bigint     NOT NULL
, state              varchar(63 bytes) NOT NULL
, contestant_number  integer    NOT NULL
);

PARTITION TABLE votesBytes ON COLUMN phone_number;

CREATE INDEX votesBytes_state_tree1 ON votesBytes (state);


