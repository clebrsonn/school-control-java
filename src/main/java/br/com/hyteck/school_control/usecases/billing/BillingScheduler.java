package br.com.hyteck.school_control.usecases.billing;

import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.YearMonth;

@Component
@Log4j2
public class BillingScheduler {
    private final GenerateInvoicesForParents generateInvoicesForParents;

    public BillingScheduler(GenerateInvoicesForParents generateInvoicesForParents) {
        this.generateInvoicesForParents = generateInvoicesForParents;
    }

    @Scheduled(cron = "0 0 1 * * *")
    public void generateMonthlyInvoices() {
        YearMonth currentMonth = YearMonth.now();
        log.info("Scheduler: Iniciando geração de faturas mensais para {}", currentMonth);
        try {
            generateInvoicesForParents.execute(currentMonth);
        } catch (Exception e) {
            log.error("Scheduler: Erro durante a geração agendada de faturas mensais para {}: {}", currentMonth, e.getMessage(), e);
        }

    }
}
