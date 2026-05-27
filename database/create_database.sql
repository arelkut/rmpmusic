-- ============================================================
-- Music App Database
-- SSMS (SQL Server Management Studio)
-- ============================================================

USE master;
GO

-- Создание базы данных
IF EXISTS (SELECT name FROM sys.databases WHERE name = 'MusicAppDB')
BEGIN
    ALTER DATABASE MusicAppDB SET SINGLE_USER WITH ROLLBACK IMMEDIATE;
    DROP DATABASE MusicAppDB;
END
GO

CREATE DATABASE MusicAppDB
    COLLATE Cyrillic_General_CI_AS;
GO

USE MusicAppDB;
GO

-- ============================================================
-- ТАБЛИЦА: Пользователи
-- ============================================================
CREATE TABLE Users (
    user_id         INT IDENTITY(1,1) PRIMARY KEY,
    username        NVARCHAR(50)  NOT NULL UNIQUE,
    email           NVARCHAR(100) NOT NULL UNIQUE,
    password_hash   NVARCHAR(255) NOT NULL,
    display_name    NVARCHAR(100),
    avatar_url      NVARCHAR(500),
    bio             NVARCHAR(500),
    is_active       BIT DEFAULT 1,
    is_verified     BIT DEFAULT 0,
    created_at      DATETIME2 DEFAULT GETDATE(),
    updated_at      DATETIME2 DEFAULT GETDATE()
);
GO

-- ============================================================
-- ТАБЛИЦА: Токены сброса пароля
-- ============================================================
CREATE TABLE PasswordResetTokens (
    token_id        INT IDENTITY(1,1) PRIMARY KEY,
    user_id         INT NOT NULL,
    token           NVARCHAR(255) NOT NULL UNIQUE,
    expires_at      DATETIME2 NOT NULL,
    is_used         BIT DEFAULT 0,
    created_at      DATETIME2 DEFAULT GETDATE(),
    FOREIGN KEY (user_id) REFERENCES Users(user_id) ON DELETE CASCADE
);
GO

-- ============================================================
-- ТАБЛИЦА: Жанры
-- ============================================================
CREATE TABLE Genres (
    genre_id        INT IDENTITY(1,1) PRIMARY KEY,
    name            NVARCHAR(50) NOT NULL UNIQUE,
    color_hex       NVARCHAR(7) DEFAULT '#6B46C1',
    icon_url        NVARCHAR(500),
    created_at      DATETIME2 DEFAULT GETDATE()
);
GO

-- ============================================================
-- ТАБЛИЦА: Исполнители
-- ============================================================
CREATE TABLE Artists (
    artist_id       INT IDENTITY(1,1) PRIMARY KEY,
    name            NVARCHAR(100) NOT NULL,
    bio             NVARCHAR(MAX),
    avatar_url      NVARCHAR(500),
    is_verified     BIT DEFAULT 0,
    monthly_listeners INT DEFAULT 0,
    created_at      DATETIME2 DEFAULT GETDATE(),
    updated_at      DATETIME2 DEFAULT GETDATE()
);
GO

-- ============================================================
-- ТАБЛИЦА: Альбомы
-- ============================================================
CREATE TABLE Albums (
    album_id        INT IDENTITY(1,1) PRIMARY KEY,
    title           NVARCHAR(200) NOT NULL,
    artist_id       INT NOT NULL,
    cover_url       NVARCHAR(500),
    release_date    DATE,
    album_type      NVARCHAR(20) DEFAULT 'album',  -- album, single, ep
    listen_count    BIGINT DEFAULT 0,
    created_at      DATETIME2 DEFAULT GETDATE(),
    FOREIGN KEY (artist_id) REFERENCES Artists(artist_id) ON DELETE CASCADE
);
GO

-- ============================================================
-- ТАБЛИЦА: Треки
-- ============================================================
CREATE TABLE Tracks (
    track_id        INT IDENTITY(1,1) PRIMARY KEY,
    title           NVARCHAR(200) NOT NULL,
    artist_id       INT NOT NULL,
    album_id        INT,
    genre_id        INT,
    duration_sec    INT NOT NULL DEFAULT 0,         -- длительность в секундах
    file_url        NVARCHAR(500),                  -- путь к файлу или URL
    cover_url       NVARCHAR(500),
    listen_count    BIGINT DEFAULT 0,
    like_count      BIGINT DEFAULT 0,
    is_explicit     BIT DEFAULT 0,
    is_active       BIT DEFAULT 1,
    track_order     INT DEFAULT 0,
    created_at      DATETIME2 DEFAULT GETDATE(),
    updated_at      DATETIME2 DEFAULT GETDATE(),
    FOREIGN KEY (artist_id) REFERENCES Artists(artist_id),
    FOREIGN KEY (album_id)  REFERENCES Albums(album_id) ON DELETE SET NULL,
    FOREIGN KEY (genre_id)  REFERENCES Genres(genre_id) ON DELETE SET NULL
);
GO

-- ============================================================
-- ТАБЛИЦА: Плейлисты
-- ============================================================
CREATE TABLE Playlists (
    playlist_id     INT IDENTITY(1,1) PRIMARY KEY,
    user_id         INT NOT NULL,
    name            NVARCHAR(200) NOT NULL,
    description     NVARCHAR(500),
    cover_url       NVARCHAR(500),
    is_public       BIT DEFAULT 1,
    track_count     INT DEFAULT 0,
    created_at      DATETIME2 DEFAULT GETDATE(),
    updated_at      DATETIME2 DEFAULT GETDATE(),
    FOREIGN KEY (user_id) REFERENCES Users(user_id) ON DELETE CASCADE
);
GO

-- ============================================================
-- ТАБЛИЦА: Треки в плейлисте
-- ============================================================
CREATE TABLE PlaylistTracks (
    playlist_track_id INT IDENTITY(1,1) PRIMARY KEY,
    playlist_id     INT NOT NULL,
    track_id        INT NOT NULL,
    added_at        DATETIME2 DEFAULT GETDATE(),
    track_order     INT DEFAULT 0,
    FOREIGN KEY (playlist_id) REFERENCES Playlists(playlist_id) ON DELETE CASCADE,
    FOREIGN KEY (track_id)    REFERENCES Tracks(track_id) ON DELETE CASCADE,
    UNIQUE (playlist_id, track_id)
);
GO

-- ============================================================
-- ТАБЛИЦА: Избранные треки
-- ============================================================
CREATE TABLE FavoriteTracks (
    favorite_id     INT IDENTITY(1,1) PRIMARY KEY,
    user_id         INT NOT NULL,
    track_id        INT NOT NULL,
    added_at        DATETIME2 DEFAULT GETDATE(),
    FOREIGN KEY (user_id)  REFERENCES Users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (track_id) REFERENCES Tracks(track_id) ON DELETE CASCADE,
    UNIQUE (user_id, track_id)
);
GO

-- ============================================================
-- ТАБЛИЦА: История прослушивания
-- ============================================================
CREATE TABLE ListenHistory (
    history_id      INT IDENTITY(1,1) PRIMARY KEY,
    user_id         INT NOT NULL,
    track_id        INT NOT NULL,
    listened_at     DATETIME2 DEFAULT GETDATE(),
    duration_listened INT DEFAULT 0,                -- сколько секунд прослушал
    FOREIGN KEY (user_id)  REFERENCES Users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (track_id) REFERENCES Tracks(track_id) ON DELETE CASCADE
);
GO

-- ============================================================
-- ТАБЛИЦА: Статистика пользователя
-- ============================================================
CREATE TABLE UserStats (
    stat_id             INT IDENTITY(1,1) PRIMARY KEY,
    user_id             INT NOT NULL UNIQUE,
    total_listen_hours  DECIMAL(10,2) DEFAULT 0,
    total_tracks_played BIGINT DEFAULT 0,
    favorite_genre_id   INT,
    favorite_genre_pct  DECIMAL(5,2) DEFAULT 0,
    listener_top_pct    DECIMAL(5,2) DEFAULT 100,
    updated_at          DATETIME2 DEFAULT GETDATE(),
    FOREIGN KEY (user_id) REFERENCES Users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (favorite_genre_id) REFERENCES Genres(genre_id) ON DELETE SET NULL
);
GO

-- ============================================================
-- ТАБЛИЦА: Подписки (follow artists)
-- ============================================================
CREATE TABLE UserFollowArtists (
    follow_id       INT IDENTITY(1,1) PRIMARY KEY,
    user_id         INT NOT NULL,
    artist_id       INT NOT NULL,
    followed_at     DATETIME2 DEFAULT GETDATE(),
    FOREIGN KEY (user_id)   REFERENCES Users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (artist_id) REFERENCES Artists(artist_id) ON DELETE CASCADE,
    UNIQUE (user_id, artist_id)
);
GO

-- ============================================================
-- ТАБЛИЦА: Refresh токены (JWT)
-- ============================================================
CREATE TABLE RefreshTokens (
    token_id        INT IDENTITY(1,1) PRIMARY KEY,
    user_id         INT NOT NULL,
    token           NVARCHAR(500) NOT NULL UNIQUE,
    expires_at      DATETIME2 NOT NULL,
    is_revoked      BIT DEFAULT 0,
    created_at      DATETIME2 DEFAULT GETDATE(),
    FOREIGN KEY (user_id) REFERENCES Users(user_id) ON DELETE CASCADE
);
GO

-- ============================================================
-- ТАБЛИЦА: Настройки пользователя
-- ============================================================
CREATE TABLE UserSettings (
    setting_id          INT IDENTITY(1,1) PRIMARY KEY,
    user_id             INT NOT NULL UNIQUE,
    language            NVARCHAR(10) DEFAULT 'ru',
    audio_quality       NVARCHAR(20) DEFAULT 'high',    -- low, medium, high, lossless
    download_quality    NVARCHAR(20) DEFAULT 'high',
    data_saver          BIT DEFAULT 0,
    notifications_on    BIT DEFAULT 1,
    updated_at          DATETIME2 DEFAULT GETDATE(),
    FOREIGN KEY (user_id) REFERENCES Users(user_id) ON DELETE CASCADE
);
GO

-- ============================================================
-- ИНДЕКСЫ для производительности
-- ============================================================
CREATE INDEX IX_Tracks_ArtistId   ON Tracks(artist_id);
CREATE INDEX IX_Tracks_AlbumId    ON Tracks(album_id);
CREATE INDEX IX_Tracks_GenreId    ON Tracks(genre_id);
CREATE INDEX IX_Tracks_ListenCount ON Tracks(listen_count DESC);
CREATE INDEX IX_ListenHistory_UserId ON ListenHistory(user_id, listened_at DESC);
CREATE INDEX IX_FavoriteTracks_UserId ON FavoriteTracks(user_id);
CREATE INDEX IX_Playlists_UserId  ON Playlists(user_id);
CREATE INDEX IX_Albums_ArtistId   ON Albums(artist_id);
GO

-- ============================================================
-- ТРИГГЕР: Обновление track_count в плейлисте
-- ============================================================
CREATE TRIGGER TR_PlaylistTracks_UpdateCount
ON PlaylistTracks
AFTER INSERT, DELETE
AS
BEGIN
    SET NOCOUNT ON;
    
    DECLARE @playlist_id INT;
    
    SELECT @playlist_id = COALESCE(
        (SELECT TOP 1 playlist_id FROM inserted),
        (SELECT TOP 1 playlist_id FROM deleted)
    );
    
    UPDATE Playlists
    SET track_count = (
        SELECT COUNT(*) FROM PlaylistTracks 
        WHERE playlist_id = @playlist_id
    ),
    updated_at = GETDATE()
    WHERE playlist_id = @playlist_id;
END;
GO

-- ============================================================
-- ТРИГГЕР: Обновление like_count в треке
-- ============================================================
CREATE TRIGGER TR_FavoriteTracks_UpdateLikeCount
ON FavoriteTracks
AFTER INSERT, DELETE
AS
BEGIN
    SET NOCOUNT ON;
    
    -- После добавления
    IF EXISTS (SELECT 1 FROM inserted)
    BEGIN
        UPDATE Tracks
        SET like_count = like_count + 1
        WHERE track_id IN (SELECT track_id FROM inserted);
    END
    
    -- После удаления
    IF EXISTS (SELECT 1 FROM deleted)
    BEGIN
        UPDATE Tracks
        SET like_count = CASE WHEN like_count > 0 THEN like_count - 1 ELSE 0 END
        WHERE track_id IN (SELECT track_id FROM deleted);
    END
END;
GO

-- ============================================================
-- SEED DATA: Жанры
-- ============================================================
INSERT INTO Genres (name, color_hex) VALUES
('Rap',       '#9B30FF'),
('Hip-Hop',   '#FF69B4'),
('Rock',      '#FF0000'),
('Jazz',      '#4169E1'),
('Pop',       '#00C851'),
('Classical', '#FFA500'),
('EDM',       '#7B5EA7'),
('Soul',      '#FF6600'),
('Country',   '#FFD700'),
('Indie',     '#00BFA5');
GO

-- ============================================================
-- SEED DATA: Исполнители
-- ============================================================
INSERT INTO Artists (name, bio, monthly_listeners, is_verified) VALUES
('JDFLAG',          N'Российский рэп исполнитель', 1200000, 1),
('ARLEKIN40000',    N'Рэп исполнитель', 987700, 1),
('Troye Sivan',     N'Австралийский певец и актёр', 5000000, 1),
('mehro',           N'Инди поп исполнитель', 800000, 1),
('ELLIANA',         N'R&B исполнительница', 600000, 1),
('Lil Skies',       N'Американский рэпер', 3000000, 1),
('CODE10',          N'Музыкальный коллектив', 400000, 0),
('fleurnothappy',   N'Инди исполнитель', 350000, 0),
('euro91',          N'Коллаборация', 200000, 0),
('Juice WRLD',      N'Американский рэпер (RIP)', 20000000, 1),
('FORTUNA812',      N'Российский рэпер', 500000, 1),
('tuborosho',       N'Российский рэпер', 450000, 1);
GO

-- ============================================================
-- SEED DATA: Альбомы
-- ============================================================
INSERT INTO Albums (title, artist_id, release_date, album_type, listen_count) VALUES
('FLAGSTRИК',       1,  '2023-01-01', 'album', 1200000),
('MONTANA',         2,  '2023-06-15', 'album', 987700),
('Easy',            3,  '2022-10-01', 'single', 500000),
('chance with you', 4,  '2023-03-01', 'single', 300000),
('Nirvana',         5,  '2023-05-01', 'single', 250000),
('букет',           8,  '2023-08-01', 'album', 800000),
('Lucid Dreams',    10, '2018-06-26', 'album', 50000000),
('FORTUNA',         11, '2023-01-01', 'album', 400000),
('chill tape',      12, '2023-02-01', 'album', 300000);
GO

-- ============================================================
-- SEED DATA: Треки (с демо URL — будут обслуживаться FastAPI)
-- ============================================================
INSERT INTO Tracks (title, artist_id, album_id, genre_id, duration_sec, file_url, listen_count, like_count) VALUES
('ПРIVET',              1, 1, 1, 238, '/static/audio/privet.mp3',              1200000, 85000),
('INTROСПЕКЦИЯ',        1, 1, 1, 195, '/static/audio/introspection.mp3',       900000,  70000),
('Easy',                3, 3, 5, 193, '/static/audio/easy.mp3',                500000,  45000),
('chance with you',     4, 4, 5, 187, '/static/audio/chance_with_you.mp3',     300000,  30000),
('Nirvana',             5, 5, 5, 202, '/static/audio/nirvana.mp3',             250000,  25000),
('Lust',                6, NULL, 1, 165, '/static/audio/lust.mp3',             400000,  38000),
('KissXO',              7, NULL, 1, 178, '/static/audio/kissxo.mp3',           350000,  32000),
('букет',               8, 6, 5, 212, '/static/audio/buket.mp3',              800000,  90000),
('Hate The Other Side',10, 7, 1, 160, '/static/audio/hate_the_other_side.mp3',3000000, 500000),
('Hollywood Highway',  11, 8, 1, 212, '/static/audio/hollywood_highway.mp3',  400000,  45000),
('А я Курю и Плачу',   12, 9, 1, 289, '/static/audio/a_ya_kuryu.mp3',         300000,  40000),
('MONTANA',             2, 2, 1, 225, '/static/audio/montana.mp3',             987700,  75000);
GO

-- ============================================================
-- SEED DATA: Тестовый пользователь (пароль: Test1234!)
-- bcrypt hash для "Test1234!"
-- ============================================================
INSERT INTO Users (username, email, password_hash, display_name, avatar_url, is_active, is_verified)
VALUES (
    'adelkrasnov',
    'adel@example.com',
    '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj5AqvM0y8bW',
    N'Адель Краснов',
    '/static/avatars/default.jpg',
    1, 1
);
GO

-- Статистика для тестового пользователя
INSERT INTO UserStats (user_id, total_listen_hours, total_tracks_played, favorite_genre_id, favorite_genre_pct, listener_top_pct)
VALUES (1, 7027.00, 100347, 1, 78.00, 2.00);
GO

-- Настройки для тестового пользователя
INSERT INTO UserSettings (user_id, language, audio_quality)
VALUES (1, 'ru', 'high');
GO

-- ============================================================
-- SEED DATA: Плейлисты
-- ============================================================
INSERT INTO Playlists (user_id, name, description, is_public) VALUES
(1, N'Любимые треки',  N'Мои любимые треки', 1),
(1, N'n',              N'Плейлист n',        1),
(1, N'chill',          N'Расслабляющая музыка', 1),
(1, N'nwm',            N'Плейлист nwm',      1),
(1, N'work',           N'Рабочая музыка',    1),
(1, N'CODE80',         N'CODE80',            1),
(1, N'LOVE.LIVE.TWO',  N'LOVE.LIVE.TWO',     1);
GO

-- Добавление треков в плейлисты (Любимые треки)
INSERT INTO PlaylistTracks (playlist_id, track_id, track_order) VALUES
(1, 1, 1), (1, 2, 2), (1, 3, 3), (1, 4, 4), (1, 5, 5),
(1, 6, 6), (1, 7, 7), (1, 8, 8), (1, 9, 9), (1, 10, 10), (1, 11, 11);
GO

-- Избранные треки
INSERT INTO FavoriteTracks (user_id, track_id) VALUES
(1, 9), (1, 10), (1, 11), (1, 1), (1, 8);
GO

-- История прослушивания
INSERT INTO ListenHistory (user_id, track_id, duration_listened) VALUES
(1, 2,  195), (1, 6, 165), (1, 7, 178),
(1, 8,  212), (1, 3, 193), (1, 4, 187),
(1, 5,  202), (1, 1, 238), (1, 9, 160),
(1, 10, 212), (1, 11, 289), (1, 12, 225);
GO

PRINT 'Database MusicAppDB created and seeded successfully!';
GO
