
SET SEARCH_PATH TO <Schema_TO_Upgrade>;

ALTER TABLE document 
     ADD COLUMN html_annset text ;
ALTER TABLE document 
     ADD COLUMN external_id character varying(128) ;


DROP TABLE databasemodelversion;

CREATE TABLE databasemodelversion ( v2_2 character varying(128) NOT NULL ) ;

GRANT ALL ON databasemodelversion TO annotation_admin ;


