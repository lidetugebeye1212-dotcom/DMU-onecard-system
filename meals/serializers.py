
from rest_framework import serializers
from students.models import Student


class ScanSerializer(serializers.Serializer):
    student_id = serializers.CharField(max_length=50)

    def validate_student_id(self, value):
        return value.strip()
