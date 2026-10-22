
from django.contrib import admin
from .models import Student


@admin.register(Student)
class StudentAdmin(admin.ModelAdmin):
    list_display = (
        "student_id",
        "full_name",
        "department",
        "is_active",
    )
    search_fields = ("student_id", "full_name", "department")
    list_filter = ("department", "is_active")
