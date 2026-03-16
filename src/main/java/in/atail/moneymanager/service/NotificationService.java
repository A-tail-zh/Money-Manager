package in.atail.moneymanager.service;

import in.atail.moneymanager.dto.ExpenseDTO;
import in.atail.moneymanager.entity.ProfileEntity;
import in.atail.moneymanager.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {
    private final EmailService emailService;
    private final ExpenseService expenseService;
    private final ProfileRepository profileRepository;

    @Value("${money.manager.frontend.url}")
    private String frontendUrl;


    @Scheduled(cron = "0 0 22 * * *",zone = "Asia/Shanghai")
    public void sendDailyIncomeExpenseReminder(){

        log.info("开始发送邮件:sendDailyIncomeExpenseReminder");
        List<ProfileEntity> profileEntities = profileRepository.findAll();
        for (ProfileEntity profileEntity : profileEntities) {
            String body = "<p>尊敬的用户，</p>" +
                    "<p>您于" +
                    "<a href=\"" + frontendUrl + "/profile/" + profileEntity.getId() + "\">" +
                    profileEntity.getName() +
                    "</a>的账户，" +
                    "于" +
                    "<a href=\"" + frontendUrl + "/profile/" + profileEntity.getId() + "\">" +
                    "今天" +
                    "</a>，" +
                    "支出了" + expenseService.getTotalExpenseForCurrentUser() + "元。" +
                    "</p>" +
                    "<p>请于" +
                    "<a href=\"" + frontendUrl + "/profile/" + profileEntity.getId() + "\">" +
                    "今天" +
                    "</a>，" +
                    "查看您的账户详情。" +
                    "</p>" +
                    "<p>爱你们，</p>" +
                    "<p>ATAIL</p>";
                    emailService.sendEmail(profileEntity.getEmail(),"ATail-MoneyManager-每日收支提醒",body);
        }
        log.info("邮件发送完毕:sendDailyIncomeExpenseReminder");
    }


    @Scheduled(cron = "0 0 22 * * *",zone = "Asia/Shanghai")
    public void sendDailyExpenseSummary(){
        log.info("开始发送邮件:sendDailyExpenseSummary");
        List<ProfileEntity> profileEntities = profileRepository.findAll();
        for (ProfileEntity profileEntity : profileEntities) {
            List<ExpenseDTO> todayExpenses = expenseService.getExpensesFoeUserOnDate(profileEntity.getId(), LocalDate.now());
            if(!todayExpenses.isEmpty()){
                //需要完善
            }
        }

    }


}
