package pain_helper_back.external_emr_integration_service.config;

/*
 * ЭТОТ ФАЙЛ УДАЛЕН - WebSocket конфигурация перенесена в:
 * pain_helper_back.config.WebSocketConfig
 * 
 * ПРИЧИНА: Конфликт двух @EnableWebSocketMessageBroker конфигураций
 * приводил к ошибке 404 на /ws endpoint
 * 
 * UNIFIED ENDPOINT: /ws (вместо /ws-notifications)
 * 
 * Все топики работают через единый endpoint:
 * - /topic/escalations/anesthesiologists
 * - /topic/escalations/doctors
 * - /topic/escalations/critical
 * - /topic/emr-alerts
 * 
 * Frontend подключение:
 * const socket = new SockJS('http://localhost:8080/ws');
 */
