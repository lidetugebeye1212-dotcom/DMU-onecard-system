from django.shortcuts import render

# Create your views here.

from datetime import timedelta

from django.db import IntegrityError, transaction
from django.utils import timezone
from rest_framework import status
from rest_framework.decorators import api_view
from rest_framework.response import Response

from students.models import Student
from .models import MealSession, MealServiceDay, MealTransaction
from .serializers import ScanSerializer


@api_view(["POST"])
def scan_student(request):
    serializer = ScanSerializer(data=request.data)
    serializer.is_valid(raise_exception=True)

    student_id = serializer.validated_data["student_id"]

    try:
        student = Student.objects.get(student_id=student_id)
    except Student.DoesNotExist:
        return Response(
            {
                "status": "STUDENT_NOT_FOUND",
                "message": "Student ID was not found.",
            },
            status=status.HTTP_404_NOT_FOUND,
        )

    photo_url = None
    if student.photo:
        photo_url = request.build_absolute_uri(student.photo.url)

    profile = {
        "student_id": student.student_id,
        "full_name": student.full_name,
        "department": student.department,
        "photo_url": photo_url,
    }

    if not student.is_active:
        return Response(
            {
                "status": "STUDENT_INACTIVE",
                "message": "This student card is inactive.",
                "student": profile,
            },
            status=status.HTTP_403_FORBIDDEN,
        )

    # Use the current local date and time in Africa/Addis_Ababa.
    now = timezone.localtime(timezone.now())
    current_time = now.time().replace(tzinfo=None)
    today = now.date()

    sessions = MealSession.objects.filter(is_enabled=True)

    active_session = None
    service_date = today

    for session in sessions:
        start = session.start_time
        end = session.end_time

        if start < end:
            # Breakfast or lunch: same-day session.
            if start <= current_time < end:
                active_session = session
                service_date = today
                break
        else:
            # Dinner crosses midnight.
            if current_time >= start:
                active_session = session
                service_date = today
                break
            elif current_time < end:
                active_session = session
                service_date = today - timedelta(days=1)
                break

    if active_session is None:
        return Response(
            {
                "status": "MEAL_SERVICE_NOT_OPEN",
                "message": "Meal service is not open right now.",
                "student": profile,
                "server_time": now.isoformat(),
            },
            status=status.HTTP_403_FORBIDDEN,
        )

    # Create or retrieve today's service instance.
    service_day, _ = MealServiceDay.objects.get_or_create(
        meal_session=active_session,
        service_date=service_date,
    )

    # The database unique constraint prevents duplicate successful scans.
    try:
        with transaction.atomic():
            meal_transaction = MealTransaction.objects.create(
                student=student,
                service_day=service_day,
            )
    except IntegrityError:
        return Response(
            {
                "status": "ALREADY_TAKEN",
                "message": (
                    "This student has already taken this meal. "
                    "Please leave the cafeteria."
                ),
                "student": profile,
                "meal": active_session.meal_type,
                "service_date": str(service_date),
            },
            status=status.HTTP_409_CONFLICT,
        )

    return Response(
        {
            "status": "MEAL_APPROVED",
            "message": "Meal approved. Enjoy your meal!",
            "student": profile,
            "meal": active_session.meal_type,
            "service_date": str(service_date),
            "served_at": timezone.localtime(
                meal_transaction.served_at
            ).isoformat(),
        },
        status=status.HTTP_200_OK,
    )
