package scacchi.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import scacchi.model.savemanager.SaveManager;
import scacchi.model.time.ChessClock;

class SaveLoadHandler {

    private static final String LOAD_GAME_TITLE = "Carica Partita";
    private static final String SAVE_GAME_TITLE = "Salva Partita";
    private static final String ERROR_TITLE = "Errore";
    private static final String DELETE_SAVES_TITLE = "Elimina Salvataggi";
    private static final String DELETE_ALL_OPTION = "--- Elimina TUTTI i salvataggi ---";

    private final Controller controller;
    private final SaveManager saveManager;

    SaveLoadHandler(final Controller controller, final SaveManager saveManager) {
        this.controller = controller;
        this.saveManager = saveManager;
    }

    void handleSave() {
        if (controller.getEngineHandler().preventActionIfEngineThinking(SAVE_GAME_TITLE)) {
            return;
        }
        if (controller.getView() == null) {
            return;
        }

        final Optional<String> inputOpt = controller.getView()
            .askText("Inserisci il nome del salvataggio:", SAVE_GAME_TITLE);

        if (inputOpt.isPresent() && !inputOpt.get().isBlank()) {
            String fileName = inputOpt.get().trim();
            if (fileName.toLowerCase(Locale.ROOT).endsWith(".fen")) {
                fileName = fileName.substring(0, fileName.length() - 4);
            }

            try {
                saveManager.saveGame(
                        fileName,
                        controller.getBoardImpl(),
                        controller.getChessClock().getWhiteTimeMs(),
                        controller.getChessClock().getBlackTimeMs()
                );
                controller.getView().showMessage(
                    "Partita salvata con successo come:\n" + fileName, SAVE_GAME_TITLE);
            } catch (final IOException e) {
                controller.getView().showErrorMessage(
                    "Errore durante il salvataggio: " + e.getMessage(), ERROR_TITLE);
            }
        }
    }

    void handleLoad() {
        processLoad();
    }

    boolean processLoad() {
        if (controller.getEngineHandler().preventActionIfEngineThinking("Carica Partita")) {
            return false;
        }
        if (controller.getView() == null) {
            return false;
        }

        final List<String> availableSaves = saveManager.getAvailableSaves();

        if (availableSaves.isEmpty()) {
            controller.getView().showWarningMessage("Nessun Salvataggio Trovato", LOAD_GAME_TITLE);
            return false;
        }

        final Optional<String> selectedSave = controller.getView().askChoice(
                "Seleziona il salvataggio da caricare:",
                LOAD_GAME_TITLE,
                availableSaves,
                availableSaves.getFirst()
        );

        if (selectedSave.isPresent()) {
            try {
                loadGame(selectedSave.get());
                controller.updateView();
                controller.getView().showMessage("Salvataggio caricato correttamente", LOAD_GAME_TITLE);
                controller.getEngineHandler().maybeTriggerEngineMove();
                return true;
            } catch (final IOException e) {
                controller.getView().showErrorMessage(
                    "Impossibile caricare il file: " + e.getMessage(), ERROR_TITLE);
                return false;
            }
        }
        return false;
    }

    void handleDeleteSaves() {
        if (controller.getEngineHandler().preventActionIfEngineThinking("Gestione Salvataggi")) {
            return;
        }
        if (controller.getView() == null) {
            return;
        }

        final List<String> availableSaves = saveManager.getAvailableSaves();

        if (availableSaves.isEmpty()) {
            controller.getView().showMessage("Non ci sono salvataggi da eliminare.", DELETE_SAVES_TITLE);
            return;
        }

        final List<String> deleteOptions = new ArrayList<>();
        deleteOptions.add(DELETE_ALL_OPTION);
        deleteOptions.addAll(availableSaves);

        final Optional<String> selectedOpt = controller.getView().askChoice(
                "Seleziona il salvataggio da eliminare, oppure scegli di eliminarli tutti:",
                "Gestione Salvataggi",
                deleteOptions,
                deleteOptions.getFirst()
        );

        if (selectedOpt.isPresent()) {
            final String selectedOption = selectedOpt.get();
            final boolean deleteAll = DELETE_ALL_OPTION.equals(selectedOption);

            final String confirmMessage = deleteAll
                    ? "Sei sicuro di voler eliminare TUTTI i salvataggi?\nQuesta azione è irreversibile."
                    : "Sei sicuro di voler eliminare il salvataggio:\n" + selectedOption + "?";

            if (controller.getView().askConfirmation(confirmMessage, "Conferma Eliminazione")) {
                try {
                    if (deleteAll) {
                        saveManager.deleteAllSaves();
                        controller.getView().showMessage(
                            "Tutti i salvataggi sono stati eliminati con successo.", DELETE_SAVES_TITLE);
                    } else {
                        saveManager.deleteSave(selectedOption);
                        controller.getView().showMessage(
                            "Salvataggio eliminato con successo.", DELETE_SAVES_TITLE);
                    }
                } catch (final IOException e) {
                    controller.getView().showErrorMessage(
                        "Errore durante l'eliminazione: " + e.getMessage(), ERROR_TITLE);
                }
            }
        }
    }

    private void loadGame(final String fileName) throws IOException {
        final long[] savedTimes = saveManager.loadGame(fileName, controller.getBoardImpl());
        controller.clearSelection();

        controller.getEngineHandler().getTrackedMoveLog().clear();
        controller.getEngineHandler().getPrecisionHistory().clear();

        controller.setChessClock(new ChessClock(Controller.INITIAL_TIME_MS, 0));
        controller.getChessClock().setRemainingTimes(savedTimes[0], savedTimes[1]);

        if (controller.getTimer() != null && !controller.getTimer().isRunning()) {
            controller.getTimer().start();
        }
    }
}
