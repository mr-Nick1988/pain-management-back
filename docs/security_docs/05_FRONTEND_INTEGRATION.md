# Интеграция Frontend с системой безопасности

## Структура Frontend

```
src/
├── services/
│   ├── authService.js          # Работа с аутентификацией
│   ├── api/
│   │   ├── axiosConfig.js      # Настройка axios с interceptors
│   │   ├── patientService.js   # API для работы с пациентами
│   │   ├── doctorService.js    # API для докторов
│   │   └── nurseService.js     # API для медсестер
├── components/
│   ├── auth/
│   │   ├── Login.jsx           # Форма логина
│   │   └── ProtectedRoute.jsx  # Защита роутов по ролям
│   ├── common/
│   │   └── Header.jsx          # Хедер с кнопкой Logout
├── pages/
│   ├── doctor/
│   │   └── PatientsPage.jsx
│   ├── nurse/
│   │   └── PatientsPage.jsx
│   └── admin/
│       └── Dashboard.jsx
└── App.jsx                     # Роутинг
```

---

## 1. authService.js

### Назначение
Сервис для работы с аутентификацией (login, logout, refresh, getAccessToken).

### Код

```javascript
// src/services/authService.js
import axios from 'axios';

const AUTH_BASE_URL = 'http://localhost:8082/api/auth';

class AuthService {
  /**
   * Логин пользователя
   * @param {Object} credentials - {login, password}
   * @returns {Promise<Object>} - Данные пользователя с токенами
   */
  async login(credentials) {
    try {
      const response = await axios.post(`${AUTH_BASE_URL}/login`, credentials);
      const data = response.data;
      
      // Сохраняем токены и данные пользователя в localStorage
      localStorage.setItem('accessToken', data.accessToken);
      localStorage.setItem('refreshToken', data.refreshToken);
      localStorage.setItem('personId', data.personId);
      localStorage.setItem('role', data.role);
      localStorage.setItem('firstName', data.firstName);
      
      return data;
    } catch (error) {
      throw new Error(error.response?.data?.message || 'Login failed');
    }
  }

  /**
   * Выход из системы
   */
  logout() {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('personId');
    localStorage.removeItem('role');
    localStorage.removeItem('firstName');
  }

  /**
   * Обновление access token через refresh token
   * @returns {Promise<string>} - Новый access token
   */
  async refreshToken() {
    try {
      const refreshToken = localStorage.getItem('refreshToken');
      
      if (!refreshToken) {
        throw new Error('No refresh token available');
      }
      
      const response = await axios.post(`${AUTH_BASE_URL}/refresh`, {
        refreshToken
      });
      
      const newAccessToken = response.data.accessToken;
      localStorage.setItem('accessToken', newAccessToken);
      
      return newAccessToken;
    } catch (error) {
      // Refresh token истек или невалидный → выходим из системы
      this.logout();
      window.location.href = '/login';
      throw error;
    }
  }

  /**
   * Получить текущий access token
   * @returns {string|null}
   */
  getAccessToken() {
    return localStorage.getItem('accessToken');
  }

  /**
   * Проверить, аутентифицирован ли пользователь
   * @returns {boolean}
   */
  isAuthenticated() {
    return !!this.getAccessToken();
  }

  /**
   * Получить роль текущего пользователя
   * @returns {string|null}
   */
  getUserRole() {
    return localStorage.getItem('role');
  }

  /**
   * Получить данные текущего пользователя
   * @returns {Object}
   */
  getCurrentUser() {
    return {
      personId: localStorage.getItem('personId'),
      role: localStorage.getItem('role'),
      firstName: localStorage.getItem('firstName')
    };
  }

  /**
   * Смена пароля
   * @param {Object} data - {oldPassword, newPassword}
   */
  async changePassword(data) {
    const response = await axios.post(`${AUTH_BASE_URL}/change-password`, data);
    return response.data;
  }
}

export default new AuthService();
```

---

## 2. axiosConfig.js

### Назначение
Настройка axios с interceptors для автоматического добавления токена и обработки 401 ошибок.

### Код

```javascript
// src/services/api/axiosConfig.js
import axios from 'axios';
import authService from '../authService';

const API_BASE_URL = 'http://localhost:8080/api';

// Создаем экземпляр axios для API
const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json'
  }
});

// Request Interceptor: добавляем токен в каждый запрос
apiClient.interceptors.request.use(
  (config) => {
    const token = authService.getAccessToken();
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Response Interceptor: обрабатываем 401 ошибки
apiClient.interceptors.response.use(
  (response) => {
    return response;
  },
  async (error) => {
    const originalRequest = error.config;
    
    // Если 401 и мы еще не пытались обновить токен
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;
      
      try {
        // Пытаемся обновить токен
        const newAccessToken = await authService.refreshToken();
        
        // Обновляем заголовок в оригинальном запросе
        originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;
        
        // Повторяем оригинальный запрос с новым токеном
        return apiClient(originalRequest);
      } catch (refreshError) {
        // Refresh token тоже истек → редирект на login
        authService.logout();
        window.location.href = '/login';
        return Promise.reject(refreshError);
      }
    }
    
    return Promise.reject(error);
  }
);

export default apiClient;
```

---

## 3. Login.jsx

### Назначение
Компонент формы логина.

### Код

```javascript
// src/components/auth/Login.jsx
import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import authService from '../../services/authService';

const Login = () => {
  const [credentials, setCredentials] = useState({ login: '', password: '' });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const handleChange = (e) => {
    setCredentials({
      ...credentials,
      [e.target.name]: e.target.value
    });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      const data = await authService.login(credentials);
      
      // Редирект по роли
      switch (data.role) {
        case 'ADMIN':
          navigate('/admin/dashboard');
          break;
        case 'DOCTOR':
          navigate('/doctor/patients');
          break;
        case 'NURSE':
          navigate('/nurse/patients');
          break;
        case 'ANESTHESIOLOGIST':
          navigate('/anesthesiologist/escalations');
          break;
        default:
          navigate('/');
      }
    } catch (err) {
      setError(err.message || 'Invalid credentials');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-container">
      <div className="login-card">
        <h2>Pain Management System</h2>
        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label>Login</label>
            <input
              type="text"
              name="login"
              value={credentials.login}
              onChange={handleChange}
              required
              disabled={loading}
            />
          </div>
          
          <div className="form-group">
            <label>Password</label>
            <input
              type="password"
              name="password"
              value={credentials.password}
              onChange={handleChange}
              required
              disabled={loading}
            />
          </div>
          
          {error && <div className="error-message">{error}</div>}
          
          <button type="submit" disabled={loading}>
            {loading ? 'Logging in...' : 'Login'}
          </button>
        </form>
      </div>
    </div>
  );
};

export default Login;
```

---

## 4. ProtectedRoute.jsx

### Назначение
Компонент для защиты роутов по ролям.

### Код

```javascript
// src/components/auth/ProtectedRoute.jsx
import React from 'react';
import { Navigate } from 'react-router-dom';
import authService from '../../services/authService';

const ProtectedRoute = ({ children, allowedRoles }) => {
  const isAuthenticated = authService.isAuthenticated();
  const userRole = authService.getUserRole();

  // Не аутентифицирован → редирект на login
  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  // Аутентифицирован, но роль не подходит → редирект на forbidden
  if (allowedRoles && !allowedRoles.includes(userRole)) {
    return <Navigate to="/forbidden" replace />;
  }

  // Все проверки пройдены → показываем компонент
  return children;
};

export default ProtectedRoute;
```

---

## 5. App.jsx (Роутинг)

### Назначение
Настройка роутинга с защищенными роутами.

### Код

```javascript
// src/App.jsx
import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import Login from './components/auth/Login';
import ProtectedRoute from './components/auth/ProtectedRoute';
import DoctorPatientsPage from './pages/doctor/PatientsPage';
import NursePatientsPage from './pages/nurse/PatientsPage';
import AnesthesiologistEscalationsPage from './pages/anesthesiologist/EscalationsPage';
import AdminDashboard from './pages/admin/Dashboard';
import ForbiddenPage from './pages/ForbiddenPage';

function App() {
  return (
    <BrowserRouter>
      <Routes>
        {/* Публичные роуты */}
        <Route path="/login" element={<Login />} />
        <Route path="/forbidden" element={<ForbiddenPage />} />
        
        {/* Защищенные роуты для DOCTOR */}
        <Route
          path="/doctor/*"
          element={
            <ProtectedRoute allowedRoles={['DOCTOR', 'ADMIN']}>
              <Routes>
                <Route path="patients" element={<DoctorPatientsPage />} />
                <Route path="recommendations" element={<DoctorRecommendationsPage />} />
              </Routes>
            </ProtectedRoute>
          }
        />
        
        {/* Защищенные роуты для NURSE */}
        <Route
          path="/nurse/*"
          element={
            <ProtectedRoute allowedRoles={['NURSE', 'ADMIN']}>
              <Routes>
                <Route path="patients" element={<NursePatientsPage />} />
                <Route path="vas" element={<NurseVASPage />} />
              </Routes>
            </ProtectedRoute>
          }
        />
        
        {/* Защищенные роуты для ANESTHESIOLOGIST */}
        <Route
          path="/anesthesiologist/*"
          element={
            <ProtectedRoute allowedRoles={['ANESTHESIOLOGIST', 'ADMIN']}>
              <Routes>
                <Route path="escalations" element={<AnesthesiologistEscalationsPage />} />
                <Route path="protocols" element={<AnesthesiologistProtocolsPage />} />
              </Routes>
            </ProtectedRoute>
          }
        />
        
        {/* Защищенные роуты для ADMIN */}
        <Route
          path="/admin/*"
          element={
            <ProtectedRoute allowedRoles={['ADMIN']}>
              <Routes>
                <Route path="dashboard" element={<AdminDashboard />} />
                <Route path="users" element={<AdminUsersPage />} />
              </Routes>
            </ProtectedRoute>
          }
        />
        
        {/* Редирект с корня */}
        <Route path="/" element={<Navigate to="/login" replace />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
```

---

## 6. Header.jsx (с Logout)

### Назначение
Хедер приложения с кнопкой выхода.

### Код

```javascript
// src/components/common/Header.jsx
import React from 'react';
import { useNavigate } from 'react-router-dom';
import authService from '../../services/authService';

const Header = () => {
  const navigate = useNavigate();
  const user = authService.getCurrentUser();

  const handleLogout = () => {
    authService.logout();
    navigate('/login');
  };

  return (
    <header className="app-header">
      <div className="header-left">
        <h1>Pain Management System</h1>
      </div>
      
      <div className="header-right">
        <span className="user-info">
          {user.firstName} ({user.role})
        </span>
        <button onClick={handleLogout} className="logout-btn">
          Logout
        </button>
      </div>
    </header>
  );
};

export default Header;
```

---

## 7. Пример API Service (patientService.js)

### Назначение
Сервис для работы с API пациентов.

### Код

```javascript
// src/services/api/patientService.js
import apiClient from './axiosConfig';

class PatientService {
  /**
   * Получить всех пациентов
   */
  async getAllPatients() {
    const response = await apiClient.get('/doctor/patients');
    return response.data;
  }

  /**
   * Получить пациента по MRN
   */
  async getPatientByMrn(mrn) {
    const response = await apiClient.get(`/doctor/patients/mrn/${mrn}`);
    return response.data;
  }

  /**
   * Создать пациента
   */
  async createPatient(patientData) {
    const response = await apiClient.post('/doctor/patients', patientData);
    return response.data;
  }

  /**
   * Обновить пациента
   */
  async updatePatient(mrn, patientData) {
    const response = await apiClient.patch(`/doctor/patients/${mrn}`, patientData);
    return response.data;
  }

  /**
   * Удалить пациента
   */
  async deletePatient(mrn) {
    await apiClient.delete(`/doctor/patients/${mrn}`);
  }

  /**
   * Поиск пациентов
   */
  async searchPatients(filters) {
    const response = await apiClient.get('/doctor/patients', { params: filters });
    return response.data;
  }
}

export default new PatientService();
```

---

## 8. Пример использования в компоненте

```javascript
// src/pages/doctor/PatientsPage.jsx
import React, { useState, useEffect } from 'react';
import patientService from '../../services/api/patientService';
import Header from '../../components/common/Header';

const PatientsPage = () => {
  const [patients, setPatients] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    fetchPatients();
  }, []);

  const fetchPatients = async () => {
    try {
      setLoading(true);
      const data = await patientService.getAllPatients();
      setPatients(data);
    } catch (err) {
      if (err.response?.status === 403) {
        setError('У вас нет прав для просмотра пациентов');
      } else {
        setError('Ошибка загрузки пациентов');
      }
    } finally {
      setLoading(false);
    }
  };

  if (loading) return <div>Loading...</div>;
  if (error) return <div className="error">{error}</div>;

  return (
    <div>
      <Header />
      <div className="patients-container">
        <h2>Пациенты</h2>
        <table>
          <thead>
            <tr>
              <th>MRN</th>
              <th>Имя</th>
              <th>Фамилия</th>
              <th>Дата рождения</th>
            </tr>
          </thead>
          <tbody>
            {patients.map(patient => (
              <tr key={patient.mrn}>
                <td>{patient.mrn}</td>
                <td>{patient.firstName}</td>
                <td>{patient.lastName}</td>
                <td>{patient.birthDate}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
};

export default PatientsPage;
```

---

## 9. Обработка ошибок

### 401 Unauthorized (токен истек)
```javascript
// Автоматически обрабатывается в axiosConfig.js
// 1. Перехватывается interceptor
// 2. Обновляется токен через /api/auth/refresh
// 3. Повторяется оригинальный запрос
// 4. Если refresh token тоже истек → редирект на /login
```

### 403 Forbidden (нет прав)
```javascript
try {
  await patientService.createPatient(data);
} catch (error) {
  if (error.response?.status === 403) {
    toast.error('У вас нет прав для выполнения этого действия');
  }
}
```

---

## 10. Переменные окружения (.env)

```env
REACT_APP_AUTH_SERVICE_URL=http://localhost:8082
REACT_APP_API_BASE_URL=http://localhost:8080
```

Использование:
```javascript
const AUTH_BASE_URL = process.env.REACT_APP_AUTH_SERVICE_URL + '/api/auth';
const API_BASE_URL = process.env.REACT_APP_API_BASE_URL + '/api';
```

---

## Полный поток работы Frontend

```
1. User открывает приложение
   ↓
2. ProtectedRoute проверяет наличие токена в localStorage
   ↓
3. Нет токена → редирект на /login
   ↓
4. User вводит credentials → authService.login()
   ↓
5. POST http://localhost:8082/api/auth/login
   ↓
6. Сохраняем токены в localStorage
   ↓
7. Редирект по роли (DOCTOR → /doctor/patients)
   ↓
8. User нажимает "Получить пациентов"
   ↓
9. patientService.getAllPatients()
   ↓
10. axios interceptor добавляет токен в заголовок
   ↓
11. GET http://localhost:8080/api/doctor/patients
    Headers: {Authorization: "Bearer <token>"}
   ↓
12. Если 401 → axios interceptor обновляет токен
   ↓
13. Если 403 → показываем ошибку "Нет прав"
   ↓
14. Если 200 → отображаем данные
```
