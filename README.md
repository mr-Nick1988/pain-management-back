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

##17.09.2025
 Величайший Full Stack современности Ник:
-  Создал полноценный модуль doctor с правильной архитектурой по слоям (entity, dto, repository, service, controller).
-  Реализовал сущности Patient и Recommendation с @PrePersist/@PreUpdate для автоматического управления временными метками.
-  Создал ENUM RecommendationStatus (PENDING, APPROVED, REJECTED) для типизации статусов рекомендаций.
-  Разделил DTO на Request (PatientCreationDTO, RecommendationRequestDTO) и Response (PatientResponseDTO, RecommendationDTO) для четкого разделения входящих и исходящих данных.
-  Добавил @Transactional аннотации: для методов изменения данных и @Transactional(readOnly = true) для методов чтения.
-  Реализовал полный CRUD для рекомендаций: создание, просмотр, одобрение, отклонение, обновление, удаление.
-  Реализовал полный CRUD для пациентов: создание, просмотр, обновление, мягкое удаление (через поле active).
-  Создал DoctorController с REST API эндпоинтами, используя @RequestParam(defaultValue = "system") как временную заглушку до внедрения аутентификации.
-  Настроил валидацию входных данных через @Valid и @NotBlank/@NotNull аннотации.
-  Использовал ModelMapper для конвертации между Entity и DTO, что упрощает маппинг данных.
-  **ВАЖНО: Решил конфликты имен JPA сущностей** - переименовал nurse/entity/Patient в NursePatient и anesthesiologist/entity/Recommendation в AnesthesiologistRecommendation, так как JPA требует уникальные имена сущностей в рамках всего приложения.
-  Исправил конфликты типов String vs Roles enum в AdminServiceImpl и PersonService при работе с ролями пользователей.

## 18.09.2025
Nick:
-  **КРИТИЧЕСКОЕ ИСПРАВЛЕНИЕ: Починил смену креденшиалов** - добавил поле currentLogin в ChangeCredentialsDTO с валидацией @NotBlank(message = "Current login is required").
-  Исправил логику PersonService.changeCredentials() - теперь система ищет пользователя по currentLogin (текущий логин) вместо newLogin, что устранило ошибку "User not found".
-  Добавил обновление логина пользователя через person.setLogin(request.getNewLogin()) в методе changeCredentials для корректной смены логина.
-  **КРИТИЧЕСКОЕ ИСПРАВЛЕНИЕ: Починил удаление пользователей** - изменил сигнатуру AdminService.deletePerson() с Long id на String personId для работы с документным ID вместо технического.
-  Переписал AdminServiceImpl.deletePerson() - теперь использует personRepository.findByPersonId(personId) и personRepository.delete(person) вместо deleteById для корректного поиска по документному ID.
-  Обновил AdminController.deletePerson() - изменил @DeleteMapping("/{id}") на @DeleteMapping("/{personId}") и @PathVariable Long id на @PathVariable String personId.
-  Исправил валидационные сообщения в ChangeCredentialsDTO - изменил "Login is required" на "New login is required" для поля newLogin.
-  **АРХИТЕКТУРНОЕ РЕШЕНИЕ**: Использование personId (документный ID) вместо технического Long id обеспечивает более логичную работу API и соответствует бизнес-логике приложения.
