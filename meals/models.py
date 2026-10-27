from django.db import models

# Create your models here.

from django.db import models
from django.db.models import Q


class MealSession(models.Model):
    class MealType(models.TextChoices):
        BREAKFAST = "breakfast", "Breakfast"
        LUNCH = "lunch", "Lunch"
        DINNER = "dinner", "Dinner"

    meal_type = models.CharField(
        max_length=20,
        choices=MealType.choices,
        unique=True,
    )
    start_time = models.TimeField()
    end_time = models.TimeField()
    is_enabled = models.BooleanField(default=True)

    def __str__(self):
        return self.get_meal_type_display()


class MealServiceDay(models.Model):
    meal_session = models.ForeignKey(
        MealSession,
        on_delete=models.PROTECT,
        related_name="service_days",
    )
    service_date = models.DateField()

    class Meta:
        constraints = [
            models.UniqueConstraint(
                fields=["meal_session", "service_date"],
                name="unique_meal_service_per_day",
            ),
        ]

    def __str__(self):
        return f"{self.meal_session} - {self.service_date}"


class MealTransaction(models.Model):
    student = models.ForeignKey(
        "students.Student",
        on_delete=models.PROTECT,
        related_name="meal_transactions",
    )
    service_day = models.ForeignKey(
        MealServiceDay,
        on_delete=models.PROTECT,
        related_name="transactions",
    )
    served_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        constraints = [
            models.UniqueConstraint(
                fields=["student", "service_day"],
                name="unique_student_meal_service",
            ),
        ]
        indexes = [
            models.Index(
                fields=["service_day", "served_at"],
                name="meal_service_served_idx",
            ),
        ]

    def __str__(self):
        return f"{self.student.student_id} - {self.service_day}"
