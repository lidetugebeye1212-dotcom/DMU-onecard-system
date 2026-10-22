
from django.contrib import admin
from .models import (
    MealSession,
    MealServiceDay,
    MealTransaction,
)


@admin.register(MealSession)
class MealSessionAdmin(admin.ModelAdmin):
    list_display = (
        "meal_type",
        "start_time",
        "end_time",
        "is_enabled",
    )


@admin.register(MealServiceDay)
class MealServiceDayAdmin(admin.ModelAdmin):
    list_display = ("meal_session", "service_date")


@admin.register(MealTransaction)
class MealTransactionAdmin(admin.ModelAdmin):
    list_display = ("student", "service_day", "served_at")
    search_fields = ("student__student_id", "student__full_name")
