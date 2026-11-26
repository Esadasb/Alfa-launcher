# Alfa – launcher z rolką alfabetu

Czarno-biały ekran główny dla Androida 8.0+ (Kotlin + Jetpack Compose).

**Jak działa**
- Po prawej stronie jest pionowa rolka A–Z (i `#` dla nazw od cyfr). Przesuwasz po niej kciukiem, a litery pod palcem powiększają się falą jak pod lupą, rozsuwają i wychylają w lewo. Obok kciuka pojawia się okrągła „bańka” z dużą literą, a każda zmiana litery daje delikatne tyknięcie wibracji.
- Po 5 sekundach bez dotyku ikony litery gasną, a w ich miejscu pojawiają się Twoje skróty (z małym „+” w rogu do dodawania kolejnych). Dotknięcie rolki przywraca litery.
- Na środku ekranu od razu widać aplikacje na podświetloną literę: duża, cienka litera, liczba aplikacji i siatka ikon z podpisami. Zmiana litery to płynne przejście w górę lub w dół.
- Ikony to same białe kontury. Launcher sam je rysuje z ikon aplikacji: bierze wersję monochromatyczną (Android 13+) albo pierwszy plan ikony i wyciąga z niej krawędzie. Kontury są liczone w wysokiej rozdzielczości (216 px) i zapisywane w pamięci podręcznej, więc są ostre i nie spowalniają przewijania.
- Polskie litery trafiają do liter bazowych (Łódź → L, Żabka → Z), a sortowanie jest po polsku.
- Przesunięcie palcem z dołu ekranu do góry wysuwa panel szybkiego dostępu: duże przyciski **Telefon** i **Wiadomości** (otwierają domyślne aplikacje do dzwonienia i SMS-ów) oraz Twoje **skróty** (do 12). Mały „+” w rogu dodaje kolejny skrót, a przytrzymanie skrótu pozwala go zmienić albo usunąć. Ikona skrótu zmienia się razem z przypisaną aplikacją, a ustawienia zostają zapamiętane w telefonie. Panel zamyka się przesunięciem w dół, przyciskiem „wstecz”, dotknięciem tła lub przyciskiem Home.
- **Foldery.** Przy pierwszym uruchomieniu launcher sam tworzy folder **Banki** (aplikacje bankowe i płatnicze, rozpoznawane po nazwie) i **Gry** (jeśli są co najmniej dwie gry). Kolejne foldery tworzysz sam: przytrzymaj ikonę, a pojawią się opcje: *Dodaj do folderu „…”*, pole *Nowy folder* (np. „Banki”) oraz *Informacje o aplikacji* (stamtąd można ją odinstalować). Folder trafia pod literę swojej nazwy, więc „Banki” są pod B, a aplikacje z folderu znikają ze swoich liter. Po dotknięciu folderu rolka alfabetu płynnie przejeżdża falą na lewą stronę, a obok pokazuje się zawartość folderu. Rolka po lewej podświetla tylko litery aplikacji z folderu: przesunięcie po nich przewija do tych aplikacji i na chwilę je wyróżnia. Strzałka, przycisk „wstecz” lub Home zamykają folder, a rolka wraca na prawo. Aplikację wyjmiesz z folderu przytrzymaniem i opcją *Usuń z folderu*, a pusty folder znika sam.
- Dotknięcie ikony otwiera aplikację z animacją.
- Zainstalowane i usunięte aplikacje pojawiają się i znikają automatycznie. Obsługiwany jest też profil służbowy.

## Zbudowanie APK

### Sposób 1: GitHub (bez instalowania czegokolwiek)
1. Załóż nowe repozytorium na github.com i wrzuć do niego całą zawartość tego folderu (np. przyciskiem *Add file → Upload files*, razem z ukrytym folderem `.github`).
2. Wejdź w zakładkę **Actions**. Workflow „Zbuduj APK” uruchomi się sam (lub kliknij *Run workflow*).
3. Po ok. 5 minutach w zakładce **Releases** pojawi się nowa wersja z plikiem `Alfa-launcher.apk`. Najnowsza wersja jest zawsze pod adresem `https://github.com/Esadasb/Alfa-launcher/releases/latest/download/Alfa-launcher.apk`.

### Sposób 2: Android Studio
1. Zainstaluj Android Studio (Ladybug lub nowsze) i otwórz ten folder (*File → Open*).
2. Poczekaj na synchronizację Gradle.
3. *Build → Build App Bundle(s) / APK(s) → Build APK(s)* albo w terminalu: `./gradlew assembleRelease`.
4. Plik: `app/build/outputs/apk/release/app-release.apk`.

Wersja *release* jest zoptymalizowana (R8) i działa wyraźnie płynniej niż *debug*, więc instaluj właśnie ją.

## Instalacja na telefonie
1. Skopiuj `app-release.apk` na telefon i otwórz go. Zezwól na instalację z tego źródła, jeśli telefon zapyta.
2. Naciśnij przycisk Home i wybierz **Alfa**, albo ustaw go w *Ustawienia → Aplikacje → Aplikacje domyślne → Aplikacja ekranu głównego*.
3. Powrót do starego launchera: w tym samym miejscu wybierz poprzedni.

APK jest podpisane stałym kluczem z pliku `app/alfa.keystore`, więc nowsze wersje instalują się na starsze bez odinstalowywania. To klucz do użytku prywatnego. Przed ewentualną publikacją w Google Play podmień go na własny, tajny.

## Struktura
- `ui/AlphabetRail.kt` – rolka z efektem lupy (jeden Canvas, ruch palca odświeża tylko rysowanie)
- `ui/LauncherScreen.kt` – zegar, data, strona litery z siatką aplikacji
- `ui/FolderUi.kt` – ikona folderu, zawartość folderu, opcje po przytrzymaniu ikony
- `Folders.kt` – model i zapis folderów
- `ui/QuickPanel.kt` – panel z dołu: Telefon, Wiadomości, skróty i wybór aplikacji
- `ui/LineIcons.kt` – konturowe ikony Telefonu i Wiadomości
- `ui/AppTile.kt` – ikona z podpisem, animacja wciśnięcia, uruchamianie
- `OutlineIconRenderer.kt` – zamiana ikon w kontury
- `IconCache.kt` – pamięć podręczna konturów (RAM + dysk)
- `AppRepository.kt` – lista aplikacji, grupowanie po literach, uruchamianie

Parametry wyglądu łatwo zmienić: szerokość rolki i odstępy w `ui/Theme.kt`, siłę i zasięg „fali” (`sigma`, `1.3f`, `maxShift`) w `ui/AlphabetRail.kt`, liczbę kolumn (`GridCells.Fixed(3)`) w `ui/LauncherScreen.kt`.
