
from django.urls import path
from .views import scan_student

urlpatterns = [
    path("scan/", scan_student, name="scan-student"),
]
