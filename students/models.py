from django.db import models

# Create your models here.

from django.db import models


class Student(models.Model):
    student_id = models.CharField(
        max_length=50,
        unique=True,
    )
    full_name = models.CharField(max_length=150)
    department = models.CharField(max_length=100)
    photo = models.ImageField(
        upload_to="student_photos/",
        blank=True,
        null=True,
    )
    is_active = models.BooleanField(default=True)
    created_at = models.DateTimeField(auto_now_add=True)

    def __str__(self):
        return f"{self.student_id} - {self.full_name}"

