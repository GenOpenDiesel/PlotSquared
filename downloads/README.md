# PlotSquared - poprawka potek (2026-10-22)

Plik: PlotSquared-7.6.1-potion-fix.jar
Wersja bazowa: 7.6.1-SNAPSHOT z forka GenOpenDiesel/PlotSquared
Commit kodu: 304b7c60a4c9e2819437240a6aae9682a92302f4
SHA-256: 0d4a4964a057e86e8e3b1b6ccfefae0e5fff7f7c623f4098caa8744a47f9e3cc

Naprawiono traktowanie potki trafiajacej rzucajacego jako PvP oraz anulowanie calej potki przy mieszanych dozwolonych i zablokowanych celach. Ochrona innych graczy i cudzych mobow nadal obowiazuje.

Weryfikacja: 7 testow regresji przeszlo, kontrola formatowania i budowanie JAR zakonczone powodzeniem. Nie wykonywano testu na dzialajacym serwerze Minecraft.

Instalacja: zatrzymaj serwer, zastap dotychczasowy JAR PlotSquared tym plikiem i uruchom serwer. Pozostaw folder PlotSquared z konfiguracja i baza dzialek. W katalogu plugins ma byc tylko jeden JAR PlotSquared. Nie uzywaj /reload.
