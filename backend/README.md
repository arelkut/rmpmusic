# 🎵 Music App — Backend API

FastAPI + SQL Server (SSMS) + uvicorn

---

## 📋 Требования

- Python 3.11+
- SQL Server (SSMS) — LocalDB / Express / Full
- ODBC Driver 17 for SQL Server
- Windows 10/11

---

## 🗄️ Настройка базы данных

1. Открой **SQL Server Management Studio (SSMS)**
2. Подключись к серверу
3. Открой файл `../database/create_database.sql`
4. Выполни скрипт (F5)

База данных `MusicAppDB` будет создана автоматически с:
- Всеми таблицами
- Индексами
- Триггерами  
- Тестовыми данными (треки, жанры, исполнители)
- Тестовым пользователем: `adel@example.com` / `Test1234!`

---

## ⚙️ Настройка подключения

Отредактируй файл `.env`:

```env
# Для Windows Auth (рекомендуется):
DB_SERVER=localhost\SQLEXPRESS
DB_NAME=MusicAppDB
DB_TRUSTED=yes

# Для SQL Auth:
DB_SERVER=localhost\SQLEXPRESS
DB_NAME=MusicAppDB
DB_TRUSTED=no
DB_USER=sa
DB_PASSWORD=ВашПароль
```

---

## 🚀 Запуск

### Быстрый старт (Windows):
```
1. Запусти setup.bat  (первый раз)
2. Запусти start.bat  (каждый раз)
```

### Вручную:
```bash
cd backend
python -m venv venv
venv\Scripts\activate
pip install -r requirements.txt

uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

### Документация API:
- Swagger UI: http://localhost:8000/docs
- ReDoc:       http://localhost:8000/redoc
- Health:      http://localhost:8000/health

---

## 📡 API Endpoints

### 🔐 Auth (`/api/v1/auth`)
| Method | Endpoint | Описание |
|--------|----------|----------|
| POST | `/register` | Регистрация |
| POST | `/login` | Вход |
| POST | `/refresh` | Обновление токена |
| POST | `/logout` | Выход |
| POST | `/forgot-password` | Восстановление пароля |
| POST | `/reset-password` | Сброс пароля |

### 👤 Users (`/api/v1/users`)
| Method | Endpoint | Описание |
|--------|----------|----------|
| GET | `/me` | Текущий пользователь |
| PATCH | `/me` | Обновить профиль |
| POST | `/me/avatar` | Загрузить аватар |
| GET | `/me/stats` | Статистика |
| GET | `/me/settings` | Настройки |
| PATCH | `/me/settings` | Обновить настройки |

### 🎵 Tracks (`/api/v1/tracks`)
| Method | Endpoint | Описание |
|--------|----------|----------|
| GET | `/trending` | Треки в тренде |
| GET | `/recent` | Недавно прослушанные |
| GET | `/favorites/list` | Избранные треки |
| GET | `/{id}` | Детали трека |
| POST | `/{id}/play` | Записать прослушивание |
| POST | `/{id}/like` | Лайкнуть трек |
| DELETE | `/{id}/like` | Убрать лайк |
| GET | `/stream/{id}` | Стримить аудио |
| POST | `/upload` | Загрузить трек |

### 📁 Playlists (`/api/v1/playlists`)
| Method | Endpoint | Описание |
|--------|----------|----------|
| GET | `/my` | Мои плейлисты |
| GET | `/recommended` | Рекомендованные |
| POST | `/` | Создать плейлист |
| GET | `/{id}` | Детали плейлиста |
| PATCH | `/{id}` | Обновить плейлист |
| DELETE | `/{id}` | Удалить плейлист |
| GET | `/{id}/tracks` | Треки плейлиста |
| POST | `/{id}/tracks` | Добавить трек |
| DELETE | `/{id}/tracks/{track_id}` | Удалить трек |
| POST | `/{id}/cover` | Загрузить обложку |

### 🔍 Search (`/api/v1/search`)
| Method | Endpoint | Описание |
|--------|----------|----------|
| GET | `/?q=` | Поиск |
| GET | `/genres` | Список жанров |
| GET | `/by-genre/{id}` | Треки по жанру |
| GET | `/albums` | Топ альбомов |

---

## 🎵 Музыкальные файлы

### Хранение музыки:
Приложение поддерживает **два режима**:

1. **Локальные файлы** — положи `.mp3` файлы в папку `static/audio/`
   ```
   backend/static/audio/privet.mp3
   backend/static/audio/buket.mp3
   ...
   ```
   URL: `http://10.0.2.2:8000/static/audio/privet.mp3`

2. **Внешние URL** — в поле `file_url` трека укажи прямую ссылку на MP3

### Для Android эмулятора:
- `localhost` → используй `10.0.2.2`
- Например: `http://10.0.2.2:8000/api/v1/tracks/trending`

### Демо MP3 для тестирования:
Можно скачать бесплатные треки с:
- https://freemusicarchive.org
- https://pixabay.com/music/

---

## 🏗️ Структура проекта

```
backend/
├── app/
│   ├── __init__.py
│   ├── main.py          # FastAPI app
│   ├── config.py        # Settings
│   ├── database.py      # DB connection (pyodbc)
│   ├── security.py      # JWT + bcrypt
│   ├── dependencies.py  # Auth dependency
│   ├── schemas.py       # Pydantic models
│   └── routers/
│       ├── auth.py
│       ├── users.py
│       ├── tracks.py
│       ├── playlists.py
│       └── search.py
├── static/
│   ├── audio/           # MP3 файлы
│   ├── covers/          # Обложки
│   └── avatars/         # Аватары
├── .env
├── requirements.txt
├── setup.bat
└── start.bat
```
