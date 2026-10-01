package will.dev.smart_invite_v3.service;

import will.dev.smart_invite_v3.dto.finance.FinanceStatsResponse;
import will.dev.smart_invite_v3.dto.finance.FinanceStatsResponse.PeriodAmount;
import will.dev.smart_invite_v3.dto.finance.FinanceStatsResponse.PeriodCount;

import java.util.List;

/**
 * Contrat du service de statistiques financières (audit BE-F1).
 */
public interface FinanceServicePort {

    FinanceStatsResponse getStats();

    List<PeriodAmount> revenueByMonth(int months);
    List<PeriodAmount> revenueByYear();
    List<PeriodCount>  countApprovedByMonth(int months);
}
