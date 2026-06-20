# Context pentru redactarea documentatiei de licenta

Acest document este un rezumat al proiectului **Fleet Manager** si poate fi folosit ca prompt/context initial pentru ChatGPT atunci cand redactez capitolele lucrarii de licenta. Scopul este ca asistentul sa inteleaga rapid domeniul, arhitectura, functionalitatile si stadiul implementarii, fara sa fie nevoie sa analizeze din nou intregul cod sursa.

Important: nu include in documentatie valori reale din `.env`, parole, secrete JWT sau chei interne. Foloseste doar denumiri generice precum `JWT_SECRET`, `DOCUMENT_DB_PASSWORD` sau `OLLAMA_MODEL`.

## 1. Tema proiectului

Titlul orientativ al proiectului:

**Aplicatie web pentru managementul flotelor auto, cu evidenta vehiculelor, documentelor si alertelor de expirare**

Aplicatia este gandita pentru organizatii care administreaza o flota de autovehicule si au nevoie de:

- evidenta centralizata a vehiculelor;
- autentificare si autorizare pe roluri;
- incarcare si administrare de documente auto;
- extragere automata de informatii din documente PDF;
- validare manuala a datelor extrase;
- urmarirea termenelor de valabilitate pentru documente;
- interfata web pentru utilizatori si personal administrativ.

Proiectul este implementat ca sistem web bazat pe microservicii, cu backend Java/Spring Boot, frontend React si infrastructura containerizata cu Docker Compose.

## 2. Obiectivele proiectului

Obiectivul general este dezvoltarea unei platforme moderne pentru administrarea flotelor auto, care reduce munca manuala si centralizeaza datele despre vehicule si documente.

Obiective specifice:

- proiectarea unei arhitecturi pe microservicii, in care fiecare componenta are responsabilitati clare;
- implementarea autentificarii prin JWT si a autorizarii pe roluri;
- dezvoltarea unui registru de vehicule cu operatii CRUD, filtrare si alocare;
- implementarea unui serviciu de documente pentru incarcarea fisierelor PDF, stocarea metadatelor si fluxul de aprobare;
- integrarea parsarii automate in serviciul de documente, pentru extragerea informatiilor structurate din documente auto;
- folosirea unei baze de date PostgreSQL separate pentru fiecare domeniu principal;
- expunerea serviciilor printr-un API Gateway Traefik;
- realizarea unei interfete web React pentru autentificare, administrarea vehiculelor, detalii vehicul, documente si alerte;
- asigurarea documentarii API-urilor prin OpenAPI/Swagger;
- acoperirea unor componente critice prin teste automate.

## 3. Tehnologii folosite

Backend:

- Java 17;
- Spring Boot 3.3.5;
- Spring Web;
- Spring Security;
- Spring Data JPA;
- Bean Validation;
- Lombok;
- JJWT pentru generarea si validarea tokenurilor JWT;
- springdoc-openapi pentru Swagger/OpenAPI.

Baze de date:

- PostgreSQL 16 in containere separate;
- H2 pentru scenarii locale/testare;
- JSONB in PostgreSQL pentru date extrase si aprobate din documente.

Frontend:

- React 19;
- TypeScript;
- Vite;
- React Router;
- Axios.

Document parsing:

- Apache PDFBox pentru extragerea textului din PDF;
- serviciu OCR abstractizat, cu implementare stub in proiect;
- Ollama ca runtime local pentru model LLM;
- model configurabil prin variabila `OLLAMA_MODEL`, implicit `qwen2.5:3b`;
- logica dedicata pentru tipuri de documente precum RCA, ITP, rovinieta si facturi.

Infrastructura:

- Docker Compose;
- Traefik v3 ca API Gateway;
- volume Docker pentru persistenta bazelor de date, stocare documente si Ollama;
- retea Docker comuna `fleet-net`.

## 4. Structura proiectului

Repository-ul contine urmatoarele componente principale:

- `auth-service/` - microserviciu pentru autentificare, profil utilizator si roluri;
- `fleet-service/` - microserviciu pentru registrul de vehicule;
- `document-service/` - microserviciu pentru documente, review si date aprobate;
- `frontend/` - aplicatie React/Vite;
- `traefik/` - configuratie statica si dinamica pentru gateway;
- `docker-compose.yml` - orchestrarea serviciilor, bazelor de date si componentelor auxiliare;
- `README.md`, `API_REFERENCE.md`, `ARCHITECTURE.md` - documentatie tehnica existenta.

## 5. Arhitectura generala

Aplicatia foloseste o arhitectura pe microservicii. Clientul web comunica prin Traefik, care directioneaza cererile catre serviciile backend.

Componente:

- Frontend React: interfata utilizatorului;
- Traefik API Gateway: punct unic de intrare pentru API-uri;
- Auth Service: autentificare, inregistrare, token JWT, roluri;
- Fleet Service: gestiunea vehiculelor;
- Document Service: incarcarea si validarea documentelor;
- Ollama: runtime local folosit de Document Service pentru extragerea automata a datelor din PDF;
- PostgreSQL: baze de date separate pentru auth, fleet si document;
- Ollama: runtime local pentru modelul LLM folosit la parsare;
- RabbitMQ: inclus in infrastructura Docker, pregatit pentru comunicare asincrona, chiar daca fluxul curent foloseste apeluri HTTP directe.

Modelul de comunicare este in principal REST/HTTP. Serviciile sunt izolate pe domenii functionale, iar comunicarea interna se face prin endpointuri dedicate si tokenuri/chei interne.

## 6. Auth Service

Responsabilitati:

- inregistrare utilizator;
- autentificare cu username si parola;
- generare JWT;
- validare JWT;
- hashing parole cu BCrypt;
- profil utilizator curent;
- administrare roluri de catre utilizatorii ADMIN;
- creare cont admin initial prin variabile de mediu.

Roluri folosite:

- `USER`;
- `STAFF`;
- `ADMIN`;

Endpointuri principale:

- `POST /api/auth/register`;
- `POST /api/auth/login`;
- `GET /api/auth/users/me`;
- `PUT /api/auth/admin/users/{id}/roles`.

Date principale:

- `UserData` - date profil utilizator;
- `Credential` - date de autentificare;
- `RoleEntity` - roluri;
- `Role` - enum pentru rolurile aplicatiei.

JWT-ul contine informatii despre utilizator si rol. Celelalte servicii folosesc acelasi secret JWT pentru validare.

## 7. Fleet Service

Responsabilitati:

- creare vehicul;
- listare vehicule;
- filtrare dupa status, tip vehicul, combustibil, tip proprietate, departament, utilizator alocat si numar de inmatriculare;
- vizualizare detalii vehicul;
- actualizare vehicul;
- schimbare status;
- alocare vehicul catre un utilizator/sofer/departament;
- stergere vehicul;
- endpointuri interne pentru verificarea existentei vehiculelor.

Endpointuri principale:

- `POST /api/fleet/vehicles`;
- `GET /api/fleet/vehicles`;
- `GET /api/fleet/vehicles/{id}`;
- `PUT /api/fleet/vehicles/{id}`;
- `PATCH /api/fleet/vehicles/{id}/status`;
- `PATCH /api/fleet/vehicles/{id}/assignment`;
- `DELETE /api/fleet/vehicles/{id}`;
- `GET /api/fleet/internal/vehicles/{id}/exists`;
- `GET /api/fleet/internal/vehicles/{id}/basic-info`;
- `GET /api/fleet/internal/vehicles/active`.

Entitatea principala este `Vehicle`, cu campuri:

- `id`;
- `licensePlate`;
- `vin`;
- `brand`;
- `model`;
- `manufactureYear`;
- `vehicleType`;
- `fuelType`;
- `ownershipType`;
- `status`;
- `department`;
- `assignedUserId`;
- `assignedDriverName`;
- `currentMileage`;
- `createdAt`;
- `updatedAt`.

Enumuri relevante:

- `VehicleStatus`: `ACTIVE`, `IN_SERVICE`, `INACTIVE`, `SOLD`, `DECOMMISSIONED`;
- `VehicleType`: `CAR`, `VAN`, `TRUCK`, `MOTORCYCLE`, `OTHER`;
- `FuelType`: `DIESEL`, `PETROL`, `HYBRID`, `ELECTRIC`, `LPG`, `OTHER`;
- `OwnershipType`: `OWNED`, `LEASED`, `RENTED`, `OTHER`.

## 8. Document Service

Responsabilitati:

- incarcare document PDF pentru un vehicul;
- verificarea existentei vehiculului prin Fleet Service;
- stocare fisier local in volum Docker configurat;
- stocare metadate document in PostgreSQL;
- parsarea documentului prin `DocumentParsingService`;
- salvarea rezultatului de parsare ca draft;
- flux de review pentru STAFF/ADMIN;
- aprobare sau respingere document;
- arhivare document;
- descarcare document;
- extragerea atributelor normalizate pentru alerte de expirare.

Endpointuri principale:

- `POST /api/documents`;
- `GET /api/documents?vehicleId={vehicleId}`;
- `GET /api/documents/{id}`;
- `GET /api/documents/{id}/download`;
- `GET /api/documents/review-queue`;
- `POST /api/documents/{id}/review`;
- `POST /api/documents/{id}/approve`;
- `POST /api/documents/{id}/reject`;
- `POST /api/documents/{id}/archive`;
- `PATCH /api/documents/{id}/archive`;
- `GET /api/documents/vehicles/{vehicleId}/documents`;
- `GET /api/documents/vehicles/{vehicleId}/approved-document-data`;
- `GET /api/documents/vehicles/{vehicleId}/attributes`;
- `GET /api/documents/alerts/document-expirations`.

Entitati principale:

- `VehicleDocument` - document incarcat, metadate fisier si status;
- `DocumentExtractionDraft` - rezultat brut/normalizat de la parser, inca neaprobat;
- `ApprovedDocumentData` - date aprobate manual, considerate oficiale;
- `VehicleDocumentAttribute` - atribute normalizate pentru cautare si alerte.

Statusuri document:

- `PARSING`;
- `PARSING_FAILED`;
- `NEEDS_REVIEW`;
- `VALIDATED`;
- `REJECTED`;
- `ARCHIVED`.

Tipuri de documente in Document Service:

- `INSPECTION`;
- `INSURANCE`;
- `INVOICE`;
- `REGISTRATION`;
- `OTHER`.

## 9. Parsarea documentelor in Document Service

In versiunea curenta, parsarea documentelor este implementata direct in `document-service`, prin componenta `DocumentParsingService`. Nu exista un microserviciu parser separat in stack-ul Docker activ.

Responsabilitati:

- incarca PDF-ul salvat de Document Service;
- extrage text cu Apache PDFBox;
- detecteaza tipul documentului pe baza textului extras;
- construieste promptul pentru modelul LLM;
- apeleaza Ollama prin HTTP;
- normalizeaza raspunsul JSON;
- valideaza campurile extrase;
- calculeaza un scor de incredere;
- salveaza rezultatul ca draft de extragere in baza de date Document Service.

Tipuri detectate:

- `INSURANCE`, cu subtip `RCA`;
- `TECHNICAL_INSPECTION`, cu subtip `ITP`;
- `ROAD_TAX`, cu subtip `ROVINIETA`;
- `EXPENSE_INVOICE`;
- `OTHER`.

Statusuri parser:

- `PARSED`;
- `FAILED`.

Rezultatul parsarii este folosit in fluxul de review:

- daca datele sunt extrase cu succes, documentul trece in `NEEDS_REVIEW`;
- daca parsarea esueaza, documentul trece in `PARSING_FAILED`;
- un administrator poate aproba sau respinge datele extrase.

## 10. Fluxuri functionale principale

### 10.1 Autentificare

1. Utilizatorul isi creeaza cont sau se autentifica.
2. Auth Service verifica credentialele.
3. Auth Service genereaza un JWT.
4. Frontend-ul trimite JWT-ul in headerul `Authorization: Bearer <token>`.
5. Serviciile backend valideaza JWT-ul si aplica regulile de autorizare.

### 10.2 Administrare vehicul

1. Utilizatorul autentificat acceseaza pagina de vehicule.
2. Frontend-ul cere lista vehiculelor de la Fleet Service.
3. In functie de rol si de datele din JWT, utilizatorul vede toate vehiculele sau doar vehiculele alocate.
4. Utilizatorii cu roluri administrative pot crea, modifica, aloca, schimba statusul sau sterge vehicule.

### 10.3 Incarcare si parsare document

1. Utilizatorul incarca un PDF pentru un vehicul.
2. Document Service verifica daca vehiculul exista prin Fleet Service.
3. Document Service salveaza fisierul si metadatele documentului.
4. Documentul primeste statusul `PARSING`.
5. `DocumentParsingService` incarca PDF-ul salvat, extrage textul, detecteaza tipul documentului si apeleaza LLM-ul prin Ollama.
6. Rezultatul parsarii contine date structurate, scor de incredere si eventuale avertismente.
7. Document Service transforma rezultatul in draft de extragere.
8. Document Service salveaza rezultatul in `DocumentExtractionDraft`.
9. Daca parsarea este valida, documentul devine `NEEDS_REVIEW`; altfel devine `PARSING_FAILED`.

### 10.4 Review document

1. Un utilizator `STAFF` sau `ADMIN` acceseaza coada de review.
2. Verifica datele extrase automat.
3. Poate aproba datele, respinge documentul sau arhiva documentul.
4. La aprobare, datele sunt salvate in `ApprovedDocumentData`.
5. Se genereaza/actualizeaza si `VehicleDocumentAttribute`, folosit pentru alerte.
6. Documentul devine `VALIDATED`.

### 10.5 Alerte de expirare

1. Sistemul pastreaza date precum `validFrom` si `validUntil` pentru documentele aprobate.
2. Endpointul de alerte returneaza documentele expirate sau care expira intr-un numar configurabil de zile.
3. Frontend-ul afiseaza pagina de alerte pentru utilizatorii cu acces.

## 11. Frontend

Aplicatia frontend este o aplicatie React cu rutare client-side.

Pagini principale:

- `/login` - autentificare;
- `/register` - creare cont;
- `/profile` - profil utilizator;
- `/vehicles` - lista vehicule;
- `/vehicles/new` - creare vehicul;
- `/vehicles/:id` - detalii vehicul;
- `/vehicles/:id/edit` - editare vehicul;
- `/alerts/documents` - alerte documente.

Componente relevante:

- `AuthContext` - gestioneaza starea de autentificare;
- `ProtectedRoute` - protejeaza rutele private;
- `Navbar` - navigatie;
- `VehicleForm` - formular creare/editare vehicul;
- `VehicleDocumentsSection` - documente asociate unui vehicul;
- `ComplianceSection` - informatii de conformitate;
- `DocumentAlertsPage` - afisarea documentelor expirate sau aproape de expirare.

Frontend-ul comunica cu backend-ul prin module API:

- `authApi.ts`;
- `vehicleApi.ts`;
- `documentApi.ts`;
- `axios.ts`.

## 12. Securitate

Securitatea aplicatiei este bazata pe:

- autentificare username/parola;
- parole hash-uite cu BCrypt;
- tokenuri JWT;
- validare JWT in fiecare microserviciu;
- roluri si reguli de acces;
- separarea endpointurilor publice de cele protejate;
- apelurile interne intre servicii sunt protejate prin JWT si reteaua Docker interna;
- CORS si headere de securitate configurate prin Traefik.

Reguli orientative:

- utilizatorii obisnuiti pot vedea datele la care au acces si pot incarca/descarca documente;
- `STAFF` si `ADMIN` pot face review pe documente;
- `ADMIN` poate modifica roluri si efectua operatii administrative;
- endpointurile interne nu sunt gandite pentru expunere directa catre utilizatorul final.

## 13. Persistenta datelor

Aplicatia foloseste baze de date separate pentru principalele domenii:

- `auth-postgres` pentru utilizatori si credentiale;
- `fleet-postgres` pentru vehicule;
- `document-postgres` pentru documente, drafturi de extragere si date aprobate.

Aceasta separare respecta principiul de izolare a datelor in arhitectura pe microservicii.

Documentele PDF sunt stocate in sistemul de fisiere, intr-un volum Docker dedicat, iar in baza de date se salveaza metadatele si calea fisierului.

Datele extrase din documente sunt stocate in campuri JSONB, deoarece structura campurilor poate varia in functie de tipul documentului.

## 14. API Gateway si rulare cu Docker

Traefik este folosit ca API Gateway si expune serviciile prin rute comune:

- `/api/auth/**`;
- `/api/fleet/**`;
- `/api/documents/**`;
- rute interne pentru verificari intre servicii, precum endpointurile Fleet Service folosite de Document Service.

Docker Compose porneste:

- Traefik;
- Auth Service si baza sa PostgreSQL;
- Fleet Service si baza sa PostgreSQL;
- Document Service si baza sa PostgreSQL;
- RabbitMQ;
- Ollama.

Comanda generala de rulare:

```powershell
docker compose up -d --build
```

Swagger/OpenAPI este disponibil pentru servicii, iar specificatiile pot fi regenerate cu scripturile `generate-openapi`.

## 15. Testare si validare

Proiectul contine teste automate pentru componente precum:

- Auth Service: integrare autentificare, JWT, roluri, repository;
- Document Service: stocare documente si logica de documente;
- Document Service: stocare documente, parsare, review si fluxuri de aprobare;
- Fleet Service: structura este pregatita pentru testare prin Spring Boot si Spring Security Test.

Tipuri de testare ce pot fi descrise in lucrare:

- testare unitara pentru servicii si utilitare;
- testare de integrare pentru autentificare si persistenta;
- testare API cu Swagger/cURL/Postman;
- testare manuala a fluxurilor frontend;
- validare a fluxului complet: login, creare vehicul, incarcare PDF, parsare, review, alerta.

## 16. Limitari si directii viitoare

Limitari actuale:

- OCR-ul este abstractizat, dar implementarea curenta poate fi stub/incompleta;
- RabbitMQ este inclus in infrastructura, dar fluxul principal de parsare foloseste apel HTTP sincron;
- modelul LLM local poate avea rezultate variabile si necesita review manual;
- nu exista inca notificari automate prin email/SMS;
- TLS/ACME pentru productie nu este complet configurat;
- sistemul este orientat spre rulare locala/dezvoltare, nu spre productie la scara mare.

Directii viitoare:

- notificari automate pentru documente care expira;
- comunicare asincrona pentru operatii de fundal prin RabbitMQ;
- OCR real pentru PDF-uri scanate;
- audit trail pentru operatii administrative;
- dashboard cu statistici despre flota;
- integrare cu servicii externe pentru verificarea RCA/ITP/rovinieta;
- imbunatatirea modelului de permisiuni;
- deployment productie cu TLS, monitorizare si backup.

## 17. Cum sa foloseasca ChatGPT acest context

Cand redactezi documentatia, foloseste un ton academic, clar si tehnic, potrivit pentru o lucrare de licenta. Nu inventa functionalitati care nu apar in acest context. Daca este nevoie de detalii suplimentare, formuleaza intrebari punctuale.

Pentru capitole, poate fi folosita structura urmatoare:

### Capitolul 1 - Introducere

Prezinta problema administrarii flotelor auto, nevoia de digitalizare, importanta centralizarii datelor si motivatia alegerii unei arhitecturi moderne.

### Capitolul 2 - Obiectivele proiectului

Descrie obiectivul general si obiectivele specifice enumerate mai sus. Accentueaza evidenta vehiculelor, documentele, autentificarea, parsarea automata si alertele.

### Capitolul 3 - Studiu bibliografic

Teme de inclus:

- aplicatii web moderne;
- arhitectura pe microservicii;
- REST API;
- autentificare JWT;
- API Gateway;
- containerizare cu Docker;
- baze de date relationale si JSONB;
- extragerea informatiilor din documente;
- utilizarea LLM-urilor in procesarea documentelor;
- aplicatii SPA cu React.

### Capitolul 4 - Analiza si fundamentare teoretica

Explica cerintele functionale si nefunctionale, actorii sistemului, cazurile de utilizare, modelul domeniului, principiile microserviciilor, securitatea si fluxurile de date.

### Capitolul 5 - Proiectare de detaliu si implementare

Descrie implementarea fiecarui microserviciu, modelele de date, endpointurile, frontend-ul, integrarea prin Traefik, Docker Compose si fluxul de parsare documente.

### Capitolul 6 - Testare si validare

Prezinta testele automate existente, scenariile manuale, validarea API-urilor si verificarea fluxului complet din aplicatie.

### Capitolul 7 - Manual de instalare si utilizare

Include cerinte software, configurare `.env`, pornire cu Docker Compose, accesarea frontend-ului/API-urilor, autentificare, creare vehicul, incarcare document si review.

### Capitolul 8 - Concluzii

Rezuma rezultatele obtinute, avantajele solutiei, limitele actuale si directiile viitoare.

### Anexe

Anexe posibile:

- fragmente relevante din cod;
- diagrame de arhitectura;
- exemple de request/response API;
- capturi de ecran;
- specificatii OpenAPI;
- exemple de date extrase din documente.

## 18. Prompt initial recomandat pentru ChatGPT

Poti copia textul urmator intr-o conversatie noua:

```text
Vreau sa ma ajuti sa redactez documentatia pentru lucrarea mea de licenta. Tema este o aplicatie web numita Fleet Manager, pentru managementul flotelor auto, dezvoltata cu microservicii Spring Boot, PostgreSQL, React, Docker Compose, Traefik si parsare interna de documente PDF in Document Service, bazata pe PDFBox si Ollama.

Foloseste contextul tehnic de mai jos ca sursa principala. Redacteaza in limba romana, intr-un stil academic, clar si coerent. Nu inventa functionalitati care nu sunt mentionate. Cand informatia nu este suficienta, intreaba-ma.

Am nevoie sa ma ajuti progresiv pe capitolele:
1. Introducere
2. Obiectivele proiectului
3. Studiu bibliografic
4. Analiza si fundamentare teoretica
5. Proiectare de detaliu si implementare
6. Testare si validare
7. Manual de instalare si utilizare
8. Concluzii
9. Anexe

Mai intai propune-mi o structura detaliata pentru capitolul la care lucram, apoi redacteaza subcapitolele pe rand.
```

## 19. Detalii care trebuie tratate cu atentie in redactare

- Foloseste termenul "microservicii" pentru arhitectura backend.
- Mentioneaza clar ca datele extrase automat din documente nu devin oficiale imediat, ci necesita review manual.
- Explica de ce JSONB este util pentru documente cu structuri diferite.
- Explica rolul API Gateway-ului in rutare, securitate si centralizarea accesului.
- Explica faptul ca parsarea este o componenta interna a Document Service si foloseste Ollama ca runtime local.
- Nu prezenta LLM-ul ca mecanism perfect; subliniaza necesitatea validarii umane.
- Nu include secrete, parole sau valori reale din configurarea locala.
- Daca folosesti exemple de cod in anexe, alege fragmente scurte si relevante.
