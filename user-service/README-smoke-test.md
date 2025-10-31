# user-service smoke test

This file contains quick steps to build, run and smoke-test the `user-service` on Windows (cmd.exe).

Prerequisites
- Java 17+ (JDK 21 preferred) installed and `java` on PATH
- Docker/MariaDB running with a database `authdb` or `users_db` configured as in `application-dev.yml`
- The project root is `.../polling-app-with-limits-service`

Steps
1. From `user-service` folder run:

```cmd
.\mvnw.cmd -DskipTests package
```

2. Run the jar with the `dev` profile (application-dev.yml will be used):

```cmd
java -jar target\user-service-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

3. Use the included `smoke-test.cmd` to automate build + quick HTTP smoke calls (it will start the service in a new window):

```cmd
smoke-test.cmd
```

What the smoke test does
- Register a new user: POST /auth/sign-up
- Login: POST /auth/sign-in
- Fetch user details: POST /user/get-user-details
- Shows manual DB queries to run

Notes
- Validation is intentionally disabled in the service (the API gateway must validate incoming requests).
- The sample curl commands in `smoke-test.cmd` use `curl`/`ncurl`. If `curl` isn't available on your Windows shell, install it or run the JSON payloads using Postman.

If you want I can:
- Add `ROLE_` prefix to JWT minted roles for compatibility with Spring Security authorities.
- Remove deprecated placeholder files.
- Run the build and smoke-test here (I attempted earlier but the terminal returned no output — running locally will be most reliable).

