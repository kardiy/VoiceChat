# VoiceChat - Minecraft Java plugin

## Описание

**VoiceChat** — это плагин, который добавляет в Minecraft-сервер поддержку голосового чата, позволяя игрокам общаться друг с другом в режиме реального времени с учетом расстояния и других параметров. Плагин создан для улучшения взаимодействия между игроками и повышения атмосферы игры.

## Основные особенности

- **Поддержка голосового общения**: Позволяет игрокам общаться голосом в пределах установленного радиуса.
- **Чат с учётом расстояния**: Громкость голосов уменьшается с увеличением расстояния между игроками.
- **Чат по группам**: Игроки могут создавать группы и общаться в голосовом чате независимо от других игроков.
- **Мульти-сервер**: плагин можно добавить на несколько серверов, при переходе с сервера на сервер звук не будет пропадать.
- **Конфиденциальность**: Возможность отключения голосового чата для отдельных игроков.
- **Настройка**: Администратор сервера может регулировать радиус действия голосового чата, максимальное количество участников в группах, включение/отключение чата на сервере и другие параметры.

## Требования

- Minecraft-сервер версии `1.16` или выше.
- Плагин совместим с такими ядрами, как [Spigot](https://www.spigotmc.org/) и [Paper](https://papermc.io/).
- **Java 11+** для запуска сервера.

## Установка

1. **Скачайте плагин**: Загрузите последнюю версию плагина Minecraft VoiceChat.

2. **Добавьте плагин на сервер**:
   - Поместите загруженный `.jar` файл в папку `plugins` на сервере Minecraft.

3. **Перезапустите сервер**:
   - После добавления плагина перезапустите сервер, чтобы активировать плагин.

4. **Настройте плагин**:
   - После перезапуска на сервере появится файл `config.yml` в папке `plugins/VoiceChat`.
   - Отредактируйте файл `config.yml`, чтобы изменить радиус действия голосового чата, максимальное количество участников в группах и другие настройки.

## Настройки

Теперь необходимо настроить конфигурацию. Файл `config.yml` содержит основные параметры плагина. Вот пример настроек:

```yaml
#порт, на котором работает голосовой чат, можно оставить без изменений
socket:
  port: 60777

#разрешённые IP, оставить без изменений
allowed_ips:
  - "185.197.33.12"
  - "194.87.209.184"

#радиус слышимости, можно оставить без изменений
radius: 50

#уникальное название вашего сервера, обязательно придумать своё
name: serverlink

#команды, доступные по умолчанию
commands:
  reload:
    default_access: false
  link:
    default_access: true
  code:
    default_access: true
  off:
    default_access: true
  on:
    default_access: true
  mute:
    default_access: true
  unmute:
    default_access: true
  listen:
    default_access: false
  myradius:
    default_access: false
```

## Активация
Активация происходит через нашего бота в VK.
