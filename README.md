## 17.09.2025
Евгений:
-  Добавил в AdminServiceImpl @Transactional над сложными методами, чуть сократил метод createPerson с помощью modelMapper.
-  Убрал PatientDTO и VASInputDTO из admin/dto и пока перекинул в nurse/dto.
-  Из PersonLoginResponseDTO убрал token, так как он не используется, но добавил поле firstName для приветствия на фронте и в PersonService убрал всё что с токеном связано.
-  Добавил ENUM Roles, но пока не переделал в папке admin.
-  Перетащил общее для всех dto (LoginResponse,LoginRequest,ChangeCredentials) из admin/dto в папку common/dto.
-  Перетащил общий контроллер для всех из admin PersonalController в папку common/controller и PersonService в папку common/service.
-  Перетащил из admin/entity Approval в папку doctor/entity и anethesiologist/entity (может пригодиться).
-  Перетащил из admin/entity Patient и VAS в папку nurse/entity (может пригодиться).

 Nurse-logic:
 - Создал DTO со следующей структурой:
 * PatientDTO — базовые данные пациента + коллекции emr, vas, recommendations.
 * EmrDTO — медицинская карта (анализы, показатели организма).
 * VasDTO — жалобы на боль (VAS шкала).
 * RecommendationDTO — одна рекомендация (связана с конкретным случаем/жалобой).
 * DrugRecommendationDTO — конкретное лекарство внутри одной рекомендации.

## 18.09.2025
 Евгений:
 - Создал папку external_service с DTO и Service для реализации в будущем получения EMR данных пациента извне.

   Nurse-logic:
 - Создал Entity по схожей с DTO структурой.
 - Создал отдельные Repositories на все Entities.
 - Создал Controller со всеми эндпоинтами
 - Создал Service со всеми методами для Nurse (частично имплементировали)
 - Создал директорию treatment-protocol для хранения протокола лечения (Exel таблица)

## 19.09.2025
 Евгений:
 - Сделал папку nurse/dto/exceptions для красивых ошибок.
   Nurse-logic:
 - Реализовал все методы в Sevice. 
   Treatment Protocol:
 - 