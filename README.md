DMU One Card System
A university cafeteria meal management and student verification system for Debre Markos University (DMU).

The DMU One Card System is designed to digitize cafeteria meal distribution by validating student identification cards, checking meal-service schedules, preventing duplicate meal claims, and maintaining a reliable record of meal transactions.

The backend is built with Python, Django, Django REST Framework, and PostgreSQL. It exposes a REST API that can be used by a web dashboard, an Android application, barcode or QR-code scanners, or a manual student-ID entry interface.
Project status: Under development. Verify each feature against the current codebase before treating it as production-ready.

Table of Contents
- Overview
- Objectives
- Key Features
- Technology Stack
- System Architecture
- Meal Schedule
- Application Workflow
- Database Design
- REST API Reference
- Installation and Setup
- Configure Meal Sessions
- Run the Development Server
- Test the API
- Frontend Integration
- Project Structure
- Security and Privacy
- Troubleshooting
- Development Roadmap
- Contributing
- License
- Contact
Overview
Manual cafeteria meal distribution can make it difficult to verify student eligibility consistently, prevent repeat claims, and maintain accurate service records. DMU One Card aims to make this process faster and more accountable.

When a student presents a card, the scanning client sends the student ID to the Django API. The backend checks the student's record and card status, determines whether a meal session is open, and attempts to record the meal. PostgreSQL constraints provide an additional layer of protection against duplicate transactions.

The frontend communicates with the backend through HTTP requests and JSON responses. It must never connect directly to the PostgreSQL database.
Objectives
- Digitize student identification during cafeteria meal distribution.
- Validate student records and active-card status in real time.
- Manage breakfast, lunch, and dinner schedules.
- Enforce meal eligibility on the server.
- Prevent duplicate meal claims for the same student and service day.
- Maintain transaction history for auditing and reporting.
- Provide student profile information to authorized scanning clients.
- Support QR codes, barcodes, and manual student-ID entry.
- Provide a foundation for meal-service dashboards and administrative reports.
Key Features
1. Student Management
Student records can include:
- Unique student ID.
- Full name.
- Department.
- Optional student photo.
- Active or inactive card status.
An unknown student ID is rejected. An inactive card is not eligible to receive a meal.
2. Meal Schedule Management
Meal sessions define the meal type, opening time, closing time, and whether the session is enabled. Schedules are managed in the backend and evaluated using the configured Django timezone.

The time-checking logic can support a session that crosses midnight, provided its service-date handling and database configuration are correct.
3. Meal Eligibility Validation
For a scan request, the backend checks the student record, active-card status, and currently open meal session before recording a successful meal transaction.

The backend is the authority for eligibility. Client-side checks improve the user experience but are not a substitute for server-side validation.
4. Duplicate Meal Prevention
A successful transaction is associated with a student and a specific meal service day. A database uniqueness constraint should prevent a student from receiving more than one transaction for the same service day.

The database constraint is important because two scan requests may arrive close together.
5. Transaction Records
Successful meal transactions store the student, service-day record, and serving timestamp. These records can support reconciliation, auditing, and future reporting.
6. REST API Integration
The backend returns structured JSON for scan results and meal-status checks. A separate web or Android client can display approval, duplicate-claim, inactive-card, unknown-student, and closed-service messages.
Technology Stack
Technology	Purpose
Python	Backend programming language
Django	Web framework and ORM
Django REST Framework	REST API endpoints and request validation
PostgreSQL	Relational database
psycopg2-binary	PostgreSQL adapter for Python, if used by the project
Pillow	Image processing for student photos
python-dotenv	Optional loading of environment variables from .env
HTML, CSS, JavaScript	Potential web frontend
Kotlin and Jetpack Compose	Android client, if maintained as a separate application
Android camera / barcode library	Optional physical QR or barcode scanning integration


Use the versions pinned in requirements.txt when it is available. The exact dependencies must match the actual project configuration.
System Architecture
Student ID Card
      |
      v
Barcode / QR Scanner or Manual Entry
      |
      v
Web or Android Client
      |
      | HTTP + JSON
      v
Django REST Framework API
      |
      +---- Student and Card Validation
      |
      +---- Meal Schedule Validation
      |
      +---- Duplicate Transaction Protection
      |
      v
Django ORM
      |
      v
PostgreSQL Database
      |
      v
Structured JSON Response
      |
      v
Client Displays the Result
Architecture principle: the client submits a student ID and displays the result. All eligibility decisions and transaction writes must be performed by the backend.
Meal Schedule
The schedule is configured through MealSession records. The application timezone should be set to Africa/Addis_Ababa.

The schedule currently documented for the project is:
Meal	Start	End
Breakfast	01:00	02:00
Lunch	17:30	19:00
Dinner	22:30	01:00 the following day


These are configuration values, not a statement of DMU's official cafeteria timetable. Update them to match the approved university schedule.
Optional 24-hour operating plan
If the university chooses to divide the day into three six-hour meal windows and one six-hour closed period, the following is an example configuration:
Period	Start	End
Breakfast	06:00	12:00
Lunch	12:00	18:00
Dinner	18:00	00:00
Closed	00:00	06:00


The Closed period should normally be represented by no enabled meal session being active; it does not need to be stored as a meal type. Configure only the three meal sessions in the database.

Adjacent windows use an exclusive end time: at exactly 12:00, breakfast is closed and lunch is open. At exactly 18:00, lunch is closed and dinner is open.
Timezone configuration
In settings.py:
TIME_ZONE = "Africa/Addis_Ababa"
USE_TZ = True
Use Django's timezone utilities when evaluating schedules. Do not trust the Android or browser clock to authorize a meal.
Application Workflow
1. The scanner reads a student ID, or staff enter it manually.
2. The client sends a POST request to /api/scan/.
3. Django REST Framework validates the request body.
4. The backend looks up the student.
5. The backend rejects an unknown student or inactive card.
6. Django determines the active meal session using server-local time.
7. If no meal session is open, the backend rejects the scan without creating a meal transaction.
8. If a session is open, the backend identifies the appropriate service date.
9. The backend attempts to create the transaction.
10. The database uniqueness constraint prevents a duplicate transaction for that student and service day.
11. The API returns a JSON response for the client to display.
Database Design
The system uses a student model and three meal-related models. Confirm the exact field names and constraints in the current models.py files and migrations.
Student
Stores student identity and card status.
Field	Purpose
student_id	Unique student identifier
full_name	Student's name
department	Academic department
photo	Optional photo
is_active	Whether the card is active
created_at	Creation timestamp, if defined in the model


MealSession
Stores a meal type and its schedule.
Field	Purpose
meal_type	Breakfast, lunch, or dinner
start_time	Scheduled opening time
end_time	Scheduled closing time
is_enabled	Whether the session is enabled


MealServiceDay
Represents a particular meal session on a service date.
Field	Purpose
meal_session	Related meal session
service_date	Date to which the service belongs


A uniqueness constraint on (meal_session, service_date) should prevent duplicate service-day records.
MealTransaction
Records a successfully served meal.
Field	Purpose
student	Student who received the meal
service_day	Associated meal service day
served_at	Serving timestamp


A uniqueness constraint on (student, service_day) should prevent repeat claims for the same service day. This design permits one transaction per student per meal session per service date. Confirm that this matches the university's policy.
REST API Reference
The endpoints below describe the API implemented or intended by this project. Confirm them against meals/urls.py and the current views before publishing a deployed API address.
1. Scan a Student
Endpoint: POST /api/scan/

Content-Type: application/json

Request body:
{
  "student_id": "DMU1001"
}
Example successful response — 200 OK:
{
  "status": "MEAL_APPROVED",
  "message": "Meal approved. Enjoy your meal!",
  "student": {
    "student_id": "DMU1001",
    "full_name": "Example Student",
    "department": "Computer Science",
    "photo_url": null
  },
  "meal": "lunch",
  "service_date": "2026-10-25",
  "served_at": "2026-10-25T18:00:00+03:00"
}
This is illustrative sample data. Actual values depend on the database and the current server time.
Possible scan outcomes
HTTP status	API status	Meaning
200	MEAL_APPROVED	Meal was approved and recorded
400	Validation error	Request data is invalid or missing
403	STUDENT_INACTIVE	Student card is inactive
403	MEAL_SERVICE_NOT_OPEN	No enabled meal session is currently open
404	STUDENT_NOT_FOUND	Student ID does not exist
409	ALREADY_TAKEN	Student already has a transaction for this service day
500	Server error	Unexpected backend error; inspect server logs


The response body should contain a clear status and human-readable message. Actual HTTP codes and response fields are determined by the implementation.
2. Get Current Meal Status
Endpoint: GET /api/meal-status/

Example local development URL:
http://127.0.0.1:8000/api/meal-status/
Example response while a meal is open:
{
  "status": "OPEN",
  "message": "Lunch is open",
  "meal": "lunch",
  "meal_display": "Lunch",
  "start_time": "12:00",
  "end_time": "18:00",
  "service_date": "2026-10-25",
  "served_count": 42,
  "todays_total": 120,
  "server_time": "2026-10-25T13:00:00+03:00",
  "timezone": "Africa/Addis_Ababa"
}
Example response while service is closed:
{
  "status": "CLOSED",
  "message": "Meal service is closed. Please wait for the next session.",
  "meal": null,
  "served_count": 0,
  "todays_total": 120,
  "next_meal": "Breakfast",
  "next_opening": "2026-10-26 06:00",
  "server_time": "2026-10-26T02:00:00+03:00",
  "timezone": "Africa/Addis_Ababa"
}
These are examples, not live results. The actual response may differ based on the configured schedule and the current view code. If no sessions are enabled, there may be no next opening.
API behavior notes
- The scan endpoint makes the eligibility decision using backend time.
- The status endpoint reports the current schedule state when requested; it does not, by itself, push updates to connected clients.
- A frontend can poll /api/meal-status/ periodically to refresh the dashboard.
- The client must handle unsuccessful HTTP responses as well as successful responses.
- Never use a client-only duplicate check as a replacement for the database constraint.
Installation and Setup
Prerequisites
Install:
- Python 3.11 or another version supported by the project's dependencies.
- PostgreSQL.
- Git.
- A code editor such as Visual Studio Code.
- Optional: Android Studio for the Android client.
- Optional: Postman or another HTTP client for API testing.
1. Clone the repository
Replace the placeholder with the actual repository URL:
git clone YOUR_GITHUB_REPOSITORY_URL
cd DMU-onecard-system
2. Create and activate a virtual environment
Windows Command Prompt:
python -m venv venv
venv\Scripts\activate
PowerShell:
python -m venv venv
.\venv\Scripts\Activate.ps1
Linux or macOS:
python3 -m venv venv
source venv/bin/activate
3. Install dependencies
If requirements.txt exists:
python -m pip install --upgrade pip
pip install -r requirements.txt
If the project does not yet have a dependency file, install the required packages used by your configuration:
pip install django djangorestframework psycopg2-binary pillow python-dotenv
After confirming the application works, record the environment's dependencies:
pip freeze > requirements.txt
For a team or production project, review and pin dependencies deliberately rather than relying on an unreviewed environment dump.
4. Configure environment variables
Create a .env file in the project root if the Django settings are configured to read it.

Example:
SECRET_KEY=replace-with-a-secure-secret-key
DEBUG=True

DB_NAME=your_database_name
DB_USER=your_database_user
DB_PASSWORD=your_database_password
DB_HOST=localhost
DB_PORT=5432
These variable names are examples. They must match the names read by your config/settings.py. The application will not automatically use them unless the settings load and read them.

Keep .env out of version control. Never commit real passwords or secret keys.
5. Create the PostgreSQL database
Create an empty database and a database user with appropriate privileges using pgAdmin or PostgreSQL tools. Set the environment variables to match your configuration.

Confirm that PostgreSQL is running and accepts connections before applying migrations.
6. Apply migrations
python manage.py migrate
If migrations have been created for your local model changes but not yet generated, review the model changes and run:
python manage.py makemigrations
python manage.py migrate
Do not delete migration files or reset a production database to resolve migration problems without first understanding the impact and backing up the data.
7. Create an administrator
python manage.py createsuperuser
Follow the prompts. Use the admin account to manage students and meal sessions if the relevant models are registered in Django Admin.
8. Configure media files
If student photos are stored locally, configure Django's MEDIA_ROOT and MEDIA_URL and ensure the model and development URL configuration support media serving. Local development media serving is not a production file-serving strategy.
9. Check the project configuration
python manage.py check
Resolve reported errors before starting the server.
Configure Meal Sessions
Create or update meal sessions using Django Admin, the Django shell, or an approved management interface. The model's meal_type choices and any existing unique constraints determine which records can be created.

For the optional 24-hour example schedule, use these values:
Meal	Start	End	Enabled
Breakfast	06:00	12:00	Yes
Lunch	12:00	18:00	Yes
Dinner	18:00	00:00	Yes


There is no separate enabled Closed meal. When no enabled meal session matches the current server time, the API should report CLOSED.

Before using this schedule operationally, confirm the official cafeteria timetable with the university. Do not change production meal times without authorization.
Run the Development Server
Start Django locally:
python manage.py runserver
Default local addresses:
- Django Admin: http://127.0.0.1:8000/admin/
- Scan endpoint: http://127.0.0.1:8000/api/scan/
- Meal-status endpoint: http://127.0.0.1:8000/api/meal-status/
To test from another device on the same trusted local network, run:
python manage.py runserver 0.0.0.0:8000
Find the development computer's LAN IP address and use it in the client's API base URL, for example:
http://192.168.1.10:8000/
Replace the example address with the actual address. Configure Django's ALLOWED_HOSTS appropriately. A development server is not intended for production use.
Test the API
Use Postman, curl, or another HTTP client. Ensure the test student exists and the configured meal schedule matches the current server time.
Example scan request with curl
curl -X POST http://127.0.0.1:8000/api/scan/ \
  -H "Content-Type: application/json" \
  -d "{\"student_id\":\"DMU1001\"}"
Check the current meal status
curl http://127.0.0.1:8000/api/meal-status/
Recommended test cases
- [ ] Existing active student scanned during an open meal.
- [ ] The same student scanned again for the same meal service day.
- [ ] Unknown student ID.
- [ ] Inactive student card.
- [ ] Scan when no meal service is open.
- [ ] Scan after the next meal session begins.
- [ ] Meal session crossing midnight.
- [ ] Exactly at a meal's start time.
- [ ] Exactly at a meal's end time.
- [ ] No meal sessions enabled.
- [ ] Two near-simultaneous scans for the same student.
- [ ] Student photo available and photo missing.
- [ ] Android client can reach the API over the configured network.
- [ ] Dashboard changes after the schedule transitions from one period to another.
Verify both the HTTP response and the database state. Rejected scans should not create successful meal transactions.
Frontend Integration
The frontend should submit the student ID to Django and display the returned status.
JavaScript example
async function scanStudent(studentId) {
  const response = await fetch("/api/scan/", {
    method: "POST",
    headers: {
      "Content-Type": "application/json"
    },
    body: JSON.stringify({
      student_id: studentId
    })
  });

  const result = await response.json();

  if (!response.ok) {
    // Display the returned rejection message to the operator.
    console.error(result.status, result.message);
  }

  return result;
}
For a frontend hosted separately from Django, use the correct API base URL and configure CORS if cross-origin requests are required. CORS is not authentication and does not replace access control.
Suggested client display behavior
API status	Suggested display
MEAL_APPROVED	Green success result
ALREADY_TAKEN	Red duplicate-meal warning
STUDENT_NOT_FOUND	Student ID not found
STUDENT_INACTIVE	Inactive card warning
MEAL_SERVICE_NOT_OPEN	Closed-service notice and next opening, when available
Network or server error	Retry or contact administrator


Automatic dashboard refresh
A client can request GET /api/meal-status/ periodically (for example, every 30 seconds) and when the app returns to the foreground. This updates the displayed status without requiring a manual restart.

Polling updates the display; it does not control the backend schedule. The backend must re-check the active meal at every scan request, even if a client has stale dashboard data.
Project Structure
The following is a representative backend layout. The actual project may contain additional files or a separately maintained Android application.
DMU-onecard-system/
├── config/
│   ├── __init__.py
│   ├── settings.py
│   ├── urls.py
│   ├── asgi.py
│   └── wsgi.py
├── students/
│   ├── migrations/
│   ├── models.py
│   ├── admin.py
│   └── apps.py
├── meals/
│   ├── migrations/
│   ├── models.py
│   ├── serializers.py
│   ├── views.py
│   ├── urls.py
│   ├── admin.py
│   └── apps.py
├── media/
│   └── student_photos/
├── manage.py
├── requirements.txt
├── .env                 # Local only; do not commit
├── .gitignore
└── README.md
A separate Android client may contain Kotlin source files, Gradle configuration, and camera-scanning components in its own Android Studio project or repository.
Security and Privacy
Before deployment in a real university environment:
- Keep SECRET_KEY, database credentials, and other secrets outside source control.
- Set DEBUG=False in production.
- Configure ALLOWED_HOSTS for the intended deployment.
- Use HTTPS for network communication.
- Add suitable authentication and authorization for scanning clients and administrators.
- Restrict student data and photo access to authorized users.
- Validate all student IDs and eligibility on the backend.
- Enforce duplicate protection with database constraints.
- Apply least-privilege database permissions.
- Back up PostgreSQL regularly and test restoration procedures.
- Monitor server errors and unusual scan activity.
- Avoid logging passwords, secrets, or unnecessary student personal data.
- Establish appropriate retention and access policies for transaction history.
- Use a production-ready WSGI or ASGI server and deployment configuration instead of Django's development server.
Student names, IDs, departments, photos, and meal histories are personal information and should be handled accordingly.
Troubleshooting
Database connection error
- Confirm PostgreSQL is running.
- Verify database name, user, password, host, and port.
- Ensure settings.py reads the intended environment variables.
- Confirm the database user has the required permissions.
Meal status always reports closed
- Check the current server-local time and TIME_ZONE.
- Confirm at least one MealSession is enabled.
- Verify each session's start and end times.
- Check whether the current time falls inside the configured interval.
- Review the API response from /api/meal-status/.
Android or another device cannot reach Django
- Ensure both devices are on a reachable network.
- Start Django with runserver 0.0.0.0:8000 for local testing.
- Use the computer's current LAN IP, not 127.0.0.1, from a physical phone.
- Check ALLOWED_HOSTS, firewall rules, and Android network-security settings.
- Use HTTPS and an appropriate deployment configuration outside local development.
Student photo does not load
- Confirm MEDIA_ROOT, MEDIA_URL, and the photo file.
- Check that the API generates an absolute URL reachable from the client.
- Ensure the media route is configured for development and that access is appropriately protected in production.
Duplicate transaction error
- Confirm the unique constraint on (student, service_day) exists in the database.
- Ensure migrations have been applied.
- Check that duplicate database errors are handled without returning a false success.
- Review whether the uniqueness scope matches the university's meal policy.
Development Roadmap
The following items are potential improvements. Their completion should be verified against the current repository.
- [ ] Complete end-to-end API testing.
- [ ] Complete scanner interface integration.
- [ ] Integrate and test physical QR-code or barcode scanning.
- [ ] Implement periodic dashboard refresh and foreground refresh.
- [ ] Add authentication and role-based access for cafeteria staff and administrators.
- [ ] Add meal history, daily statistics, and monthly reports.
- [ ] Add CSV export for authorized administrators.
- [ ] Improve automated tests for schedule boundaries and midnight-crossing sessions.
- [ ] Add production deployment, monitoring, and database backup procedures.
- [ ] Document installation and configuration for the Android client.
- [ ] Review student-data access controls and retention policies.
Contributing
Contributions and testing are welcome as the project develops.
1. Create a branch for your change.
2. Make a focused change and add or update tests where appropriate.
3. Run project checks and relevant tests.
4. Document any configuration changes.
5. Submit a pull request describing the change and its testing.
Do not include real student records, credentials, private photos, or production database dumps in commits or issue reports.
License
No license has been specified yet. Until a license is added to the repository, reuse, modification, and redistribution are subject to applicable copyright law. Add an explicit license file if the project is intended for open-source distribution.
Contact
Project: DMU One Card System
Institution: Debre Markos University (DMU)
Maintainer: Add the project maintainer or team contact here.
