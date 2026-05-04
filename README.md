# Shopping List — Backend

Spring Boot 4 REST API for the shopping list apps (web + iOS).

## Stack

- Java 17, Spring Boot 4.0.3
- Spring Web, Security, Validation, Data JPA
- PostgreSQL (schema managed manually)
- JJWT (JWT access + refresh tokens)
- Lombok
- Maven (`war` packaging — deployable to external Tomcat or run standalone)

## Modules

```
src/main/java/dev/edgesecura/shoppingList/
├── auth/       JWT issue/refresh, register/login
├── catalog/    Categories, subcategories, products
├── userdata/   Per-user basket + saved lists
├── security/   Spring Security config + JWT filter
└── config/     App-wide beans / properties
```

## Prerequisites

- JDK 17+
- PostgreSQL 14+ with a database named `catalog` and schema `catalog`
- Database user with full privileges on that schema

## Configuration

Copy the template, then fill in real values (do not commit secrets):

```
cp src/main/resources/application.properties.template \
   src/main/resources/application.properties
```

Key properties:

| Key | Purpose |
|-----|---------|
| `spring.datasource.url` | JDBC URL (default `jdbc:postgresql://localhost:5432/catalog`) |
| `spring.datasource.username` / `.password` | DB creds |
| `app.jwt.secret` | HS256 signing key (≥32 bytes, base64) |
| `app.jwt.access-ttl-minutes` | Access token TTL (default 15) |
| `app.jwt.refresh-ttl-days` | Refresh token TTL (default 30) |

Schema is managed manually outside the app; Hibernate runs in `validate` mode and will fail to start if entities and DB diverge.

## Run

```
./mvnw spring-boot:run
```

API listens on `http://localhost:8080` by default.

## Build

```
./mvnw clean package
```

Produces `target/shoppingList-0.0.1-SNAPSHOT.war`. Deploy to a servlet container
or run directly:

```
java -jar target/shoppingList-0.0.1-SNAPSHOT.war
```

## Test

```
./mvnw test
```

## API surface

- `POST /api/auth/register` — email + password signup
- `POST /api/auth/login` — returns access + refresh tokens
- `POST /api/auth/refresh` — rotate refresh token
- `GET  /api/catalog/...` — categories / subcategories / products
- `GET|PUT /api/me/basket` — current cart
- `GET|POST|DELETE /api/me/lists` — saved shopping lists

All `/api/me/*` endpoints require `Authorization: Bearer <jwt>`.

## Deployment

Production: `https://shoppinglist.edgesecura.dev`. Independent deploy — no
coupling to web/iOS releases. Only contract is the JSON over HTTPS.

## Seeding

`src/main/resources/categories.json` holds the seed catalog. Loader runs once
on first boot if the catalog is empty.
