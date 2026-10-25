# DMU One Card System

A university cafeteria meal management system designed to validate student identity, manage meal service schedules, and prevent duplicate meal claims using a student identification card.

## Project Overview

The **DMU One Card System** is a web-based system designed to streamline cafeteria meal distribution at Debre Markos University (DMU).

The backend is built with Django and Django REST Framework, with PostgreSQL serving as the database. The system provides an API that a frontend application can use to submit student IDs, validate meal eligibility, and retrieve student information.

The system is designed to ensure that each eligible student receives each scheduled meal only once per service day.

## Project Objectives

- Digitize student identification during cafeteria meal distribution.
- Validate student eligibility in real time.
- Manage breakfast, lunch, and dinner schedules.
- Prevent duplicate meal claims.
- Maintain a record of meal transactions.
- Provide student information to the frontend through a REST API.
- Support integration with barcode scanners, QR code scanners, or manual student ID entry.

## Key Features

### 1. Student Management
- Store student ID, full name, department, and photo.
- Track whether a student's card is active or inactive.
- Retrieve student information when a card is scanned.

### 2. Meal Schedule Management
- Configure breakfast, lunch, and dinner schedules.
- Enable or disable individual meal sessions.
- Determine meal eligibility using the current local time.
- Support meal periods that extend past midnight.

### 3. Meal Eligibility Validation
- Verify that the student exists.
- Reject inactive student cards.
- Check whether a meal service is currently open.
- Approve eligible students when they have not already received the meal.

### 4. Duplicate Meal Prevention
- Prevent a student from receiving the same meal twice during the same service day.
- Enforce duplicate prevention using PostgreSQL database constraints.
- Preserve historical meal transaction records.

### 5. Meal Transaction Management
- Record the student who received a meal.
- Associate each transaction with a specific meal service date.
- Record the serving timestamp.
- Support transaction review and reporting.

### 6. REST API Integration
- Accept student IDs through HTTP requests.
- Return structured JSON responses.
- Provide student details and meal validation results to the frontend.
- Support integration with a separate web-based scanner interface.

## Technology Stack

| Technology | Purpose |
|---|---|
| Python | Backend programming language |
| Django | Backend web framework |
| Django REST Framework | REST API development |
| PostgreSQL | Relational database |
| Django ORM | Database queries and model management |
| Pillow | Student photo handling |
| python-dotenv | Environment variable configuration |
| HTML, CSS, JavaScript | Planned frontend technologies |

## System Architecture

The system follows a client-server architecture.

```text
Student ID Card
      |
      v
Scanner / Frontend Application
      |
      v
Django REST API
      |
      +---- Student Validation
      |
      +---- Meal Schedule Validation
      |
      +---- Duplicate Transaction Check
      |
      v
PostgreSQL Database
      |
      v
JSON Response
      |
      v
Frontend Displays the Result
```

The frontend communicates with Django through the REST API. It should not connect directly to PostgreSQL.

## Meal Schedule

The current configured schedule is:

| Meal | Start Time | End Time |
|---|---|---|
| Breakfast | 01:00 | 02:00 |
| Lunch | 17:30 | 19:00 |
| Dinner | 22:30 | 01:00 the following day |

**Timezone:** `Africa/Addis_Ababa`

These are the currently configured application times and can be adjusted to match the university's official cafeteria schedule.

Meal eligibility is evaluated when a scan request is received.

## Database Design

The system uses three main meal-related models and one student model.

### Student
Stores student identity and profile information.

- `student_id`: Unique student identifier.
- `full_name`: Student's full name.
- `department`: Academic department.
- `photo`: Optional student photo.
- `is_active`: Indicates whether the student card is active.
- `created_at`: Record creation timestamp.

### MealSession
Stores meal types and their schedules.

- `meal_type`: Breakfast, lunch, or dinner.
- `start_time`: Meal start time.
- `end_time`: Meal end time.
- `is_enabled`: Indicates whether the meal session is enabled.

### MealServiceDay
Represents a specific meal session on a specific service date.

- `meal_session`: Related meal session.
- `service_date`: Date associated with the meal service.

A database constraint prevents duplicate service-day records for the same meal session and date.

### MealTransaction
Stores successful meal distribution records.

- `student`: Student who received the meal.
- `service_day`: Relevant meal service date.
- `served_at`: Timestamp of the transaction.

A database uniqueness constraint prevents the same student from having more than one transaction for the same meal service day.

## REST API Documentation

### Scan Student

**Endpoint**

```http
POST /api/scan/
```

**Local development URL**

```text
http://127.0.0.1:8000/api/scan/
```

**Content-Type**

```http
Content-Type: application/json
```

**Request body**

```json
{
  "student_id": "DMU1001"
}
```

### Successful Response

HTTP status: `200 OK`

```json
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
```

The response above is an illustrative example; actual values depend on the database and current time.

### API Status Codes

| HTTP Status | API Status | Description |
|---|---|---|
| 200 | `MEAL_APPROVED` | Meal successfully approved and recorded. |
| 400 | Validation error | Request data is invalid. |
| 404 | `STUDENT_NOT_FOUND` | Student ID does not exist. |
| 403 | `STUDENT_INACTIVE` | Student card is inactive. |
| 403 | `MEAL_SERVICE_NOT_OPEN` | No enabled meal service is currently open. |
| 409 | `ALREADY_TAKEN` | Student already has a transaction for this meal service day. |

## Meal Validation Workflow

1. The scanner reads the student's ID.
2. The frontend sends the ID to the Django REST API.
3. The API validates the incoming request.
4. Django retrieves the student from PostgreSQL.
5. The API checks whether the student is active.
6. Django determines which meal session is currently open.
7. The system identifies the appropriate meal service date.
8. Django attempts to create a meal transaction.
9. PostgreSQL enforces the uniqueness constraint.
10. The API returns the result to the frontend.
11. The frontend displays the student profile and approval or rejection message.

## Project Structure

```text
DMU-onecard-system/
|
├── config/
│   ├── __init__.py
│   ├── settings.py
│   ├── urls.py
│   ├── asgi.py
│   └── wsgi.py
|
├── students/
│   ├── migrations/
│   ├── models.py
│   ├── admin.py
│   └── apps.py
|
├── meals/
│   ├── migrations/
│   ├── models.py
│   ├── serializers.py
│   ├── views.py
│   ├── urls.py
│   ├── admin.py
│   └── apps.py
|
├── media/
│   └── student_photos/
|
├── manage.py
├── requirements.txt
├── .env
├── .gitignore
└── README.md
```

The frontend application may be maintained in a separate repository or added to this project later.

## Installation and Setup

### Prerequisites

Install the following software:

- Python 3.11 or another version supported by the project's dependencies.
- PostgreSQL.
- Git.
- A code editor such as Visual Studio Code.

### 1. Clone the Repository

```bash
git clone YOUR_GITHUB_REPOSITORY_URL
cd DMU-onecard-system
```

Replace `YOUR_GITHUB_REPOSITORY_URL` with the actual repository URL.

### 2. Create a Virtual Environment

Windows:

```bash
python -m venv venv
```

Activate it:

```bash
venv\Scripts\activate
```

### 3. Install Dependencies

```bash
pip install -r requirements.txt
```

If `requirements.txt` has not been generated yet, install the current backend dependencies:

```bash
pip install django djangorestframework psycopg2-binary pillow python-dotenv
```

Once the environment is working, save the installed dependencies:

```bash
pip freeze > requirements.txt
```

### 4. Configure Environment Variables

Create a `.env` file in the project root.

Example:

```dotenv
SECRET_KEY=replace-with-a-secure-secret-key
DEBUG=True

DB_NAME=your_database_name
DB_USER=your_database_user
DB_PASSWORD=your_database_password
DB_HOST=localhost
DB_PORT=5432
```

Replace the example database values with your local PostgreSQL configuration. Ensure that Django's `settings.py` reads the same environment variable names.

**Security:** Never commit `.env` to GitHub. For production, use a secure secret key and set `DEBUG=False`.

### 5. Create the PostgreSQL Database

Create an empty database in PostgreSQL using pgAdmin or the PostgreSQL command line.

Make sure the database name, user, password, host, and port match your environment configuration.

### 6. Apply Database Migrations

```bash
python manage.py migrate
```

### 7. Configure Meal Schedules

Create the breakfast, lunch, and dinner records through the Django shell or Django admin. The schedule values should match the official cafeteria times.

### 8. Create an Administrator

```bash
python manage.py createsuperuser
```

Follow the prompts to create an administrator account.

### 9. Start the Development Server

```bash
python manage.py runserver
```

Open the Django admin site:

```text
http://127.0.0.1:8000/admin/
```

The scan API is available at:

```text
http://127.0.0.1:8000/api/scan/
```

The API accepts POST requests with JSON data.

## Testing the API

Use Postman or another HTTP client to test the scan endpoint.

- Method: `POST`
- URL: `http://127.0.0.1:8000/api/scan/`
- Body type: `raw` → `JSON`

Example:

```json
{
  "student_id": "DMU1001"
}
```

Make sure the test student exists, is active, and is scanned during an open meal period.

Test the following cases:

1. Existing active student during an open meal.
2. Repeated scan during the same meal service day.
3. Nonexistent student ID.
4. Inactive student.
5. Scan outside meal service hours.
6. A scan during the next eligible meal.
7. Dinner validation across midnight.

## Frontend Integration

The frontend should send the student ID to the backend using a POST request.

Example JavaScript:

```javascript
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

  console.log(result);
  return result;
}
```

The frontend can use the returned `status` field to decide whether to display a success or rejection message.

For a separate frontend development server, configure the API base URL appropriately and configure Django CORS settings if cross-origin requests are required.

## Security Considerations

- Keep database credentials and Django secret keys outside source control.
- Never expose PostgreSQL credentials to the frontend.
- Restrict administrative access to authorized personnel.
- Validate student eligibility on the backend, not only in JavaScript.
- Use HTTPS and appropriate authentication for production deployment.
- Protect student photos and personal information.
- Back up the database regularly.
- Review transaction and error handling before production deployment.

## Current Development Status

- [x] Django project configuration.
- [x] PostgreSQL database connection.
- [x] Student model.
- [x] Meal session model.
- [x] Meal service-day model.
- [x] Meal transaction model.
- [x] Initial database migrations.
- [x] Meal schedule configuration.
- [x] Scan API implementation prepared.
- [ ] End-to-end API testing.
- [ ] Frontend scanner interface.
- [ ] Physical barcode or QR scanner integration.
- [ ] Authentication and production security hardening.
- [ ] Deployment and operational monitoring.

## Future Improvements

- Real-time cafeteria scanning dashboard.
- Student meal history and reporting.
- Daily and monthly meal statistics.
- Administrative reports and CSV exports.
- Barcode and QR code integration.
- Role-based access for administrators and cafeteria staff.
- Production deployment and monitoring.

## License

No license has been specified yet. Unless a license is added to this repository, reuse and redistribution are subject to the applicable copyright rules.

## Contributors

Developed for the DMU One Card System project.

Contributions, testing, and improvements are welcome as the project develops.
