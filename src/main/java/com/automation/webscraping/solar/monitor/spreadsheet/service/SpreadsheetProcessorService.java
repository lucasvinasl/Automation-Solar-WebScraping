package com.automation.webscraping.solar.monitor.spreadsheet.service;


import com.automation.webscraping.solar.monitor.spreadsheet.processingqueue.*;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Slf4j
@Service
@EnableScheduling
public class SpreadsheetProcessorService {

    @Autowired
    private ProcessingQueueEntryRepository processingQueueEntryRepository;

    @Autowired
    private ProcessingErrorLogRepository processingErrorLogRepository;

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void scheduleSpreadsheetProcessing() {
        log.debug("Verificando planilhas PENDING para processamento...");

        List<ProcessingQueueEntry> pendingEntries = processingQueueEntryRepository.findTop10ByStatusQueueOrderByInsertedAtAsc(ProcessingQueueStatus.PENDING);

        if (pendingEntries.isEmpty()) {
            log.debug("Nenhuma planilha PENDING encontrada no momento.");
            return;
        }
        log.info("Encontradas {} planilhas PENDING para processar.", pendingEntries.size());

        for (ProcessingQueueEntry entry : pendingEntries) {
            try {
                // Tenta carregar a entrada novamente para garantir que não foi alterada por outra thread
                Optional<ProcessingQueueEntry> currentEntryOpt = processingQueueEntryRepository.findById(entry.getId());
                if (currentEntryOpt.isPresent() && currentEntryOpt.get().getStatusQueue() == ProcessingQueueStatus.PENDING) {
                    ProcessingQueueEntry currentEntry = currentEntryOpt.get();
                    currentEntry.setStatusQueue(ProcessingQueueStatus.IN_PROGRESS);
                    processingQueueEntryRepository.save(currentEntry);

                    processSpreadsheetAsync(currentEntry.getId());
                } else {
                    log.warn("Planilha (ID: {}) já está sendo processada ou tem outro status. Ignorando no ciclo atual.", entry.getId());
                }
            } catch (Exception e) {
                log.error("Erro ao tentar mudar o status da planilha (ID: {}) para IN_PROGRESS: {}", entry.getId(), e.getMessage(), e);
            }
        }
    }

    @Async("spreadsheetTaskExecutor")
    @Transactional
    public void processSpreadsheetAsync(Long entryId) {
        ProcessingQueueEntry entry = null;
        try {
            Optional<ProcessingQueueEntry> optionalEntry = processingQueueEntryRepository.findById(entryId);
            if (optionalEntry.isEmpty()) {
                log.warn("Entrada da fila com ID {} não encontrada ou já removida. Pode ter sido processada por outra thread.", entryId);
                return;
            }
            entry = optionalEntry.get();

            // Uma verificação adicional (defensiva), embora o agendador tente pegar PENDING e marque IN_PROGRESS
            if (entry.getStatusQueue() != ProcessingQueueStatus.IN_PROGRESS) {
                log.warn("Planilha (ID: {}) não está no status IN_PROGRESS como esperado. Status atual: {}. Ignorando processamento real.", entryId, entry.getStatusQueue());
                return;
            }

            log.info("Iniciando processamento REAL da planilha '{}' (ID: {}) para o cliente '{}'.",
                    entry.getFileName(), entry.getId(), entry.getClientName());

            // --- LÓGICA REAL DE PROCESSAMENTO DA PLANILHA AQUI ---
            // Por enquanto, a simulação:
            log.info("Simulando abertura e fechamento da planilha: {}", entry.getFilePath());
            Thread.sleep(Duration.ofSeconds(2 + new Random().nextInt(3)).toMillis()); // Simula 2-4 segundos de trabalho

            // Ao final do processamento (sucesso)
            entry.setStatusQueue(ProcessingQueueStatus.COMPLETED);
            entry.setProcessedAt(ZonedDateTime.now(ZoneId.of("America/Fortaleza"))); // Define a hora de processamento
            processingQueueEntryRepository.save(entry);
            log.info("Planilha '{}' (ID: {}) processada com sucesso. Status: COMPLETED",
                    entry.getFileName(), entry.getId());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Restaura o estado de interrupção
            log.warn("Processamento da planilha (ID: {}) interrompido.", entryId);
            if (entry != null) {
                handleProcessingError(entry, "Processamento interrompido: " + e.getMessage(), getStackTrace(e));
            }
        } catch (Exception e) {
            log.error("Erro inesperado ao processar planilha (ID: {}): {}", entryId, e.getMessage(), e);
            if (entry != null) {
                handleProcessingError(entry, "Erro no processamento: " + e.getMessage(), getStackTrace(e));
            }
        }
    }

    @Transactional
    protected void handleProcessingError(ProcessingQueueEntry entry, String errorMessage, String stackTrace) {
        entry.setStatusQueue(ProcessingQueueStatus.ERROR);
        processingQueueEntryRepository.save(entry);
        ProcessingErrorLog errorLog = new ProcessingErrorLog(entry, errorMessage, stackTrace);
        processingErrorLogRepository.save(errorLog);
        log.error("Planilha '{}' (ID: {}) marcada como ERRO. Mensagem: {}",
                entry.getFileName(), entry.getId(), errorMessage);
    }

    private String getStackTrace(Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
}
