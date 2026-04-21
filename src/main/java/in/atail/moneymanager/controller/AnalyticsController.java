package in.atail.moneymanager.controller;

import in.atail.moneymanager.dto.CategoryAnalyticsDTO;
import in.atail.moneymanager.dto.MonthlyAnalyticsDTO;
import in.atail.moneymanager.dto.SpendingBehaviorDTO;
import in.atail.moneymanager.dto.SpendingTrendDTO;
import in.atail.moneymanager.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/monthly")
    public ResponseEntity<MonthlyAnalyticsDTO> getMonthlyAnalytics() {
        return ResponseEntity.ok(analyticsService.getMonthlyAnalytics());
    }

    @GetMapping("/category")
    public ResponseEntity<List<CategoryAnalyticsDTO>> getCategoryAnalytics() {
        return ResponseEntity.ok(analyticsService.getCategoryAnalytics());
    }

    @GetMapping("/spending-trend")
    public ResponseEntity<SpendingTrendDTO> getSpendingTrend(
            @RequestParam(required = false) String month) {
        return ResponseEntity.ok(analyticsService.getSpendingTrend(month));
    }

    @GetMapping("/behavior")
    public ResponseEntity<SpendingBehaviorDTO> getSpendingBehavior(
            @RequestParam(required = false) String month) {
        return ResponseEntity.ok(analyticsService.getSpendingBehavior(month));
    }
}
