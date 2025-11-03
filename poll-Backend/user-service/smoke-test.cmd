@echo off
REM Smoke test for user-service (Windows cmd)
REM - Builds the service, starts it with the dev profile, then performs a few HTTP checks.

nSETLOCAL ENABLEDELAYEDEXPANSION
cd /d %~dp0

necho ===== user-service smoke-test =====
echo 1) Building project (this may download dependencies)...
.\mvnw.cmd -DskipTests package
nIF ERRORLEVEL 1 (
    echo Maven build failed. Fix build errors and retry.
    goto :end
)

nSET JAR=target\user-service-0.0.1-SNAPSHOT.jar
nIF NOT EXIST "%JAR%" (
    echo ERROR: %JAR% not found. Build may have failed.
    goto :end
)

necho 2) Starting user-service with profile=dev in a new window (will use application-dev.yml)...
start "user-service" cmd /k "java -jar %JAR% --spring.profiles.active=dev"
necho Waiting 6 seconds for the service to boot...
timeout /t 6 /nobreak >nul

necho 3) Running HTTP smoke tests against http://localhost:8000
necho (If your service runs on a different port, adjust the URLs accordingly.)
echo --- Sign up (register) ---
ncurl -i -X POST http://localhost:8000/auth/sign-up -H "Content-Type: application/json" -d "{\"name\":\"Smoke User\",\"email\":\"smoke@example.com\",\"password\":\"Test12345\"}"
echo --- Sign in (login) ---
ncurl -i -X POST http://localhost:8000/auth/sign-in -H "Content-Type: application/json" -d "{\"email\":\"smoke@example.com\",\"password\":\"Test12345\"}"
echo --- Get user details by email ---
ncurl -i -X POST http://localhost:8000/user/get-user-details -H "Content-Type: application/json" -d "{\"usernameOrEmail\":\"smoke@example.com\"}"

necho 4) Manual DB checks (run in MariaDB client):
echo   USE authdb;
echo   SELECT id, email, name, created_at FROM users WHERE email='smoke@example.com' LIMIT 1;
echo   SELECT user_id, role FROM user_roles WHERE user_id = <the id above>;

necho ===== smoke-test finished =====
:end
nENDLOCAL
nexit /b 0

