--
-- IMPORTANT: this script will create object in the current schema
-- please run the command before this script:
--
--     SET search_path = ... ;
--


SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
-- SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: aae_triphase_v22; Type: SCHEMA; Schema: -; Owner: annotation_admin
--

-- CREATE SCHEMA aae_triphase_v22;


-- ALTER SCHEMA aae_triphase_v22 OWNER TO annotation_admin;

-- SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: annotationset; Type: TABLE; Schema: aae_triphase_v22; Owner: annotation_admin
--

CREATE TABLE annotationset (
    description character varying(128) NOT NULL,
    head boolean NOT NULL,
    published timestamp without time zone,
    unmatched text,
    doc_id bigint NOT NULL,
    groups text NOT NULL,
    user_id bigint NOT NULL,
    revision integer NOT NULL,
    id bigint NOT NULL,
    text_annotations text NOT NULL,
    relations text NOT NULL,
    task_id bigint NOT NULL,
    type integer NOT NULL,
    campaign_id bigint NOT NULL,
    created timestamp without time zone NOT NULL
);


ALTER TABLE annotationset OWNER TO annotation_admin;

--
-- Name: annotationsetdependency; Type: TABLE; Schema: aae_triphase_v22; Owner: annotation_admin
--

CREATE TABLE annotationsetdependency (
    referent_id bigint NOT NULL,
    referred_id bigint NOT NULL
);


ALTER TABLE annotationsetdependency OWNER TO annotation_admin;

--
-- Name: authorization; Type: TABLE; Schema: aae_triphase_v22; Owner: annotation_admin
--

CREATE TABLE "authorization" (
    description character varying(128) NOT NULL,
    scope character varying(128) NOT NULL,
    id bigint NOT NULL,
    campaignrelated boolean NOT NULL
);


ALTER TABLE "authorization" OWNER TO annotation_admin;

--
-- Name: campaign; Type: TABLE; Schema: aae_triphase_v22; Owner: annotation_admin
--

CREATE TABLE campaign (
    name character varying(128) NOT NULL,
    "guidelinesUrl" character varying(128),
    id bigint NOT NULL,
    schema text NOT NULL
);


ALTER TABLE campaign OWNER TO annotation_admin;

--
-- Name: campaignannotator; Type: TABLE; Schema: aae_triphase_v22; Owner: annotation_admin
--

CREATE TABLE campaignannotator (
    user_id bigint NOT NULL,
    campaign_id bigint NOT NULL
);


ALTER TABLE campaignannotator OWNER TO annotation_admin;

--
-- Name: campaigndocument; Type: TABLE; Schema: aae_triphase_v22; Owner: annotation_admin
--

CREATE TABLE campaigndocument (
    doc_id bigint NOT NULL,
    alvisnlp_id character varying(128),
    campaign_id bigint NOT NULL
);


ALTER TABLE campaigndocument OWNER TO annotation_admin;

--
-- Name: databasemodelversion; Type: TABLE; Schema: aae_triphase_v22; Owner: annotation_admin
--

CREATE TABLE databasemodelversion (
    v2_2 character varying(128) NOT NULL
);


ALTER TABLE databasemodelversion OWNER TO annotation_admin;

--
-- Name: document; Type: TABLE; Schema: aae_triphase_v22; Owner: annotation_admin
--

CREATE TABLE document (
    description character varying(128) NOT NULL,
    contents text NOT NULL,
    id bigint NOT NULL,
    html_annset text,
    comment character varying(128) NOT NULL,
    external_id character varying(128),
    owner bigint NOT NULL,
    props text NOT NULL
);


ALTER TABLE document OWNER TO annotation_admin;

--
-- Name: documentassignment; Type: TABLE; Schema: aae_triphase_v22; Owner: annotation_admin
--

CREATE TABLE documentassignment (
    doc_id bigint NOT NULL,
    user_id bigint NOT NULL,
    campaign_id bigint NOT NULL
);


ALTER TABLE documentassignment OWNER TO annotation_admin;

--
-- Name: s_annotationset_id; Type: SEQUENCE; Schema: aae_triphase_v22; Owner: annotation_admin
--

CREATE SEQUENCE s_annotationset_id
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE s_annotationset_id OWNER TO annotation_admin;

--
-- Name: s_campaign_id; Type: SEQUENCE; Schema: aae_triphase_v22; Owner: annotation_admin
--

CREATE SEQUENCE s_campaign_id
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE s_campaign_id OWNER TO annotation_admin;

--
-- Name: s_document_id; Type: SEQUENCE; Schema: aae_triphase_v22; Owner: annotation_admin
--

CREATE SEQUENCE s_document_id
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE s_document_id OWNER TO annotation_admin;

--
-- Name: s_taskdefinition_id; Type: SEQUENCE; Schema: aae_triphase_v22; Owner: annotation_admin
--

CREATE SEQUENCE s_taskdefinition_id
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE s_taskdefinition_id OWNER TO annotation_admin;

--
-- Name: s_user_id; Type: SEQUENCE; Schema: aae_triphase_v22; Owner: annotation_admin
--

CREATE SEQUENCE s_user_id
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE s_user_id OWNER TO annotation_admin;

--
-- Name: taskdefinition; Type: TABLE; Schema: aae_triphase_v22; Owner: annotation_admin
--

CREATE TABLE taskdefinition (
    name character varying(128) NOT NULL,
    precedencelevel integer NOT NULL,
    cardinality integer NOT NULL,
    id bigint NOT NULL,
    annotationtypes text NOT NULL,
    visibility integer NOT NULL,
    campaign_id bigint NOT NULL
);


ALTER TABLE taskdefinition OWNER TO annotation_admin;

--
-- Name: taskprecedency; Type: TABLE; Schema: aae_triphase_v22; Owner: annotation_admin
--

CREATE TABLE taskprecedency (
    direct boolean NOT NULL,
    predecessor_id bigint NOT NULL,
    successor_id bigint NOT NULL,
    reviewing_dep boolean NOT NULL,
    typereferencing_dep boolean NOT NULL,
    succeeding_dep boolean NOT NULL,
    campaign_id bigint NOT NULL
);


ALTER TABLE taskprecedency OWNER TO annotation_admin;

--
-- Name: user; Type: TABLE; Schema: aae_triphase_v22; Owner: annotation_admin
--

CREATE TABLE "user" (
    is_active boolean NOT NULL,
    is_admin boolean NOT NULL,
    id bigint NOT NULL,
    login character varying(128) NOT NULL,
    props text NOT NULL,
    password character varying(128) NOT NULL
);


ALTER TABLE "user" OWNER TO annotation_admin;

--
-- Name: userauthorization; Type: TABLE; Schema: aae_triphase_v22; Owner: annotation_admin
--

CREATE TABLE userauthorization (
    auth_id bigint NOT NULL,
    user_id bigint NOT NULL
);


ALTER TABLE userauthorization OWNER TO annotation_admin;

--
-- Name: usercampaignauthorization; Type: TABLE; Schema: aae_triphase_v22; Owner: annotation_admin
--

CREATE TABLE usercampaignauthorization (
    auth_id bigint NOT NULL,
    user_id bigint NOT NULL,
    campaign_id bigint NOT NULL
);


ALTER TABLE usercampaignauthorization OWNER TO annotation_admin;



--
-- Data for Name: authorization; Type: TABLE DATA; Schema: aae_triphase_v22; Owner: annotation_admin
--

COPY "authorization" (description, scope, id, campaignrelated) FROM stdin;
Connect	AlvisAE	1	f
Create campaign	AlvisAE	2	f
Close campaign	AlvisAE	3	t
Add document	AlvisAE	4	t
Remove document	AlvisAE	5	t
View documents	AlvisAE	6	t
Create annotations	AlvisAE	7	t
View other user''s annotations	AlvisAE	8	t
Export annotations	AlvisAE	9	t
Connect	AlvisIR	30	f
Upload document	AlvisIR	31	f
Acces to PDF document	AlvisIR	32	f
Start AlvisAE	AlvisIR	33	f
Connect	TyDI	60	f
View Ontology	TyDI	61	f
Edit Ontology	TyDI	62	f
\.


--
-- Data for Name: user; Type: TABLE DATA; Schema: aae_triphase_v22; Owner: annotation_admin
--

COPY "user" (is_active, is_admin, id, login, props, password) FROM stdin;
t	t	1	aae_root	{}	Tadmin
\.


--
-- Data for Name: userauthorization; Type: TABLE DATA; Schema: aae_triphase_v22; Owner: annotation_admin
--

COPY userauthorization (auth_id, user_id) FROM stdin;
1	1
\.


--
-- Name: s_user_id; Type: SEQUENCE SET; Schema: aae_triphase_v22; Owner: annotation_admin
--

SELECT pg_catalog.setval('s_user_id', 2, true);


--
-- Name: annotationset annotationset_pkey; Type: CONSTRAINT; Schema: aae_triphase_v22; Owner: annotation_admin
--

ALTER TABLE ONLY annotationset
    ADD CONSTRAINT annotationset_pkey PRIMARY KEY (id);


--
-- Name: annotationsetdependency annotationsetdependency_pkey; Type: CONSTRAINT; Schema: aae_triphase_v22; Owner: annotation_admin
--

ALTER TABLE ONLY annotationsetdependency
    ADD CONSTRAINT annotationsetdependency_pkey PRIMARY KEY (referent_id, referred_id);


--
-- Name: authorization authorization_pkey; Type: CONSTRAINT; Schema: aae_triphase_v22; Owner: annotation_admin
--

ALTER TABLE ONLY "authorization"
    ADD CONSTRAINT authorization_pkey PRIMARY KEY (id);


--
-- Name: campaign campaign_pkey; Type: CONSTRAINT; Schema: aae_triphase_v22; Owner: annotation_admin
--

ALTER TABLE ONLY campaign
    ADD CONSTRAINT campaign_pkey PRIMARY KEY (id);


--
-- Name: campaignannotator campaignannotator_pkey; Type: CONSTRAINT; Schema: aae_triphase_v22; Owner: annotation_admin
--

ALTER TABLE ONLY campaignannotator
    ADD CONSTRAINT campaignannotator_pkey PRIMARY KEY (campaign_id, user_id);


--
-- Name: campaigndocument campaigndocument_pkey; Type: CONSTRAINT; Schema: aae_triphase_v22; Owner: annotation_admin
--

ALTER TABLE ONLY campaigndocument
    ADD CONSTRAINT campaigndocument_pkey PRIMARY KEY (campaign_id, doc_id);


--
-- Name: document document_pkey; Type: CONSTRAINT; Schema: aae_triphase_v22; Owner: annotation_admin
--

ALTER TABLE ONLY document
    ADD CONSTRAINT document_pkey PRIMARY KEY (id);


--
-- Name: documentassignment documentassignment_pkey; Type: CONSTRAINT; Schema: aae_triphase_v22; Owner: annotation_admin
--

ALTER TABLE ONLY documentassignment
    ADD CONSTRAINT documentassignment_pkey PRIMARY KEY (campaign_id, user_id, doc_id);


--
-- Name: taskdefinition taskdefinition_pkey; Type: CONSTRAINT; Schema: aae_triphase_v22; Owner: annotation_admin
--

ALTER TABLE ONLY taskdefinition
    ADD CONSTRAINT taskdefinition_pkey PRIMARY KEY (id);


--
-- Name: taskprecedency taskprecedency_pkey; Type: CONSTRAINT; Schema: aae_triphase_v22; Owner: annotation_admin
--

ALTER TABLE ONLY taskprecedency
    ADD CONSTRAINT taskprecedency_pkey PRIMARY KEY (successor_id, predecessor_id);


--
-- Name: user user_pkey; Type: CONSTRAINT; Schema: aae_triphase_v22; Owner: annotation_admin
--

ALTER TABLE ONLY "user"
    ADD CONSTRAINT user_pkey PRIMARY KEY (id);


--
-- Name: userauthorization userauthorization_pkey; Type: CONSTRAINT; Schema: aae_triphase_v22; Owner: annotation_admin
--

ALTER TABLE ONLY userauthorization
    ADD CONSTRAINT userauthorization_pkey PRIMARY KEY (user_id, auth_id);


--
-- Name: usercampaignauthorization usercampaignauthorization_pkey; Type: CONSTRAINT; Schema: aae_triphase_v22; Owner: annotation_admin
--

ALTER TABLE ONLY usercampaignauthorization
    ADD CONSTRAINT usercampaignauthorization_pkey PRIMARY KEY (user_id, campaign_id, auth_id);


--
-- Name: campaigndocumentIDX1; Type: INDEX; Schema: aae_triphase_v22; Owner: annotation_admin
--

CREATE UNIQUE INDEX "campaigndocumentIDX1" ON campaigndocument USING btree (alvisnlp_id);


--
-- Name: documentIDX1; Type: INDEX; Schema: aae_triphase_v22; Owner: annotation_admin
--

CREATE UNIQUE INDEX "documentIDX1" ON document USING btree (external_id);


--
-- Name: idx16480406; Type: INDEX; Schema: aae_triphase_v22; Owner: annotation_admin
--

CREATE UNIQUE INDEX idx16480406 ON "user" USING btree (login);


--
-- Name: idx23c0050f; Type: INDEX; Schema: aae_triphase_v22; Owner: annotation_admin
--

CREATE UNIQUE INDEX idx23c0050f ON campaign USING btree (name);


--
-- Name: idxc8180c44; Type: INDEX; Schema: aae_triphase_v22; Owner: annotation_admin
--

CREATE UNIQUE INDEX idxc8180c44 ON taskdefinition USING btree (campaign_id, name);


--
-- Name: annotationset annotationsetFK13; Type: FK CONSTRAINT; Schema: aae_triphase_v22; Owner: annotation_admin
--

ALTER TABLE ONLY annotationset
    ADD CONSTRAINT "annotationsetFK13" FOREIGN KEY (task_id) REFERENCES taskdefinition(id);


--
-- Name: annotationset annotationsetFK16; Type: FK CONSTRAINT; Schema: aae_triphase_v22; Owner: annotation_admin
--

ALTER TABLE ONLY annotationset
    ADD CONSTRAINT "annotationsetFK16" FOREIGN KEY (doc_id) REFERENCES document(id);


--
-- Name: annotationsetdependency annotationsetdependencyFK14; Type: FK CONSTRAINT; Schema: aae_triphase_v22; Owner: annotation_admin
--

ALTER TABLE ONLY annotationsetdependency
    ADD CONSTRAINT "annotationsetdependencyFK14" FOREIGN KEY (referent_id) REFERENCES annotationset(id);


--
-- Name: annotationsetdependency annotationsetdependencyFK15; Type: FK CONSTRAINT; Schema: aae_triphase_v22; Owner: annotation_admin
--

ALTER TABLE ONLY annotationsetdependency
    ADD CONSTRAINT "annotationsetdependencyFK15" FOREIGN KEY (referred_id) REFERENCES annotationset(id);


--
-- Name: campaignannotator campaignannotatorFK6; Type: FK CONSTRAINT; Schema: aae_triphase_v22; Owner: annotation_admin
--

ALTER TABLE ONLY campaignannotator
    ADD CONSTRAINT "campaignannotatorFK6" FOREIGN KEY (campaign_id) REFERENCES campaign(id);


--
-- Name: campaignannotator campaignannotatorFK7; Type: FK CONSTRAINT; Schema: aae_triphase_v22; Owner: annotation_admin
--

ALTER TABLE ONLY campaignannotator
    ADD CONSTRAINT "campaignannotatorFK7" FOREIGN KEY (user_id) REFERENCES "user"(id);


--
-- Name: campaigndocument campaigndocumentFK8; Type: FK CONSTRAINT; Schema: aae_triphase_v22; Owner: annotation_admin
--

ALTER TABLE ONLY campaigndocument
    ADD CONSTRAINT "campaigndocumentFK8" FOREIGN KEY (campaign_id) REFERENCES campaign(id);


--
-- Name: campaigndocument campaigndocumentFK9; Type: FK CONSTRAINT; Schema: aae_triphase_v22; Owner: annotation_admin
--

ALTER TABLE ONLY campaigndocument
    ADD CONSTRAINT "campaigndocumentFK9" FOREIGN KEY (doc_id) REFERENCES document(id);


--
-- Name: taskdefinition taskdefinitionFK10; Type: FK CONSTRAINT; Schema: aae_triphase_v22; Owner: annotation_admin
--

ALTER TABLE ONLY taskdefinition
    ADD CONSTRAINT "taskdefinitionFK10" FOREIGN KEY (campaign_id) REFERENCES campaign(id);


--
-- Name: taskprecedency taskprecedencyFK11; Type: FK CONSTRAINT; Schema: aae_triphase_v22; Owner: annotation_admin
--

ALTER TABLE ONLY taskprecedency
    ADD CONSTRAINT "taskprecedencyFK11" FOREIGN KEY (successor_id) REFERENCES taskdefinition(id);


--
-- Name: taskprecedency taskprecedencyFK12; Type: FK CONSTRAINT; Schema: aae_triphase_v22; Owner: annotation_admin
--

ALTER TABLE ONLY taskprecedency
    ADD CONSTRAINT "taskprecedencyFK12" FOREIGN KEY (predecessor_id) REFERENCES taskdefinition(id);


--
-- Name: userauthorization userauthorizationFK1; Type: FK CONSTRAINT; Schema: aae_triphase_v22; Owner: annotation_admin
--

ALTER TABLE ONLY userauthorization
    ADD CONSTRAINT "userauthorizationFK1" FOREIGN KEY (user_id) REFERENCES "user"(id);


--
-- Name: userauthorization userauthorizationFK2; Type: FK CONSTRAINT; Schema: aae_triphase_v22; Owner: annotation_admin
--

ALTER TABLE ONLY userauthorization
    ADD CONSTRAINT "userauthorizationFK2" FOREIGN KEY (auth_id) REFERENCES "authorization"(id);


--
-- Name: usercampaignauthorization usercampaignauthorizationFK3; Type: FK CONSTRAINT; Schema: aae_triphase_v22; Owner: annotation_admin
--

ALTER TABLE ONLY usercampaignauthorization
    ADD CONSTRAINT "usercampaignauthorizationFK3" FOREIGN KEY (user_id) REFERENCES "user"(id);


--
-- Name: usercampaignauthorization usercampaignauthorizationFK4; Type: FK CONSTRAINT; Schema: aae_triphase_v22; Owner: annotation_admin
--

ALTER TABLE ONLY usercampaignauthorization
    ADD CONSTRAINT "usercampaignauthorizationFK4" FOREIGN KEY (campaign_id) REFERENCES campaign(id);


--
-- Name: usercampaignauthorization usercampaignauthorizationFK5; Type: FK CONSTRAINT; Schema: aae_triphase_v22; Owner: annotation_admin
--

ALTER TABLE ONLY usercampaignauthorization
    ADD CONSTRAINT "usercampaignauthorizationFK5" FOREIGN KEY (auth_id) REFERENCES "authorization"(id);


--
-- PostgreSQL database dump complete
--

