package com.cv.integration.listener;

import com.cv.integration.common.Util1;
import com.cv.integration.entity.*;
import com.cv.integration.repo.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
@PropertySource("file:config/application.properties")
public class AutoUpload {
    @Value("${sync.date}")
    private String syncDate;
    @Autowired
    private StockMessageListener listener;
    @Autowired
    private SaleHisRepo saleHisRepo;
    @Autowired
    private PurHisRepo purHisRepo;
    @Autowired
    private ReturnInRepo returnInRepo;
    @Autowired
    private ReturnOutRepo returnOutRepo;
    @Autowired
    private TraderRepo traderRepo;
    private boolean syncing = false;

    @Scheduled(fixedRate = 1000000)
    private void autoUpload() {
        if (!syncing) {
            syncing = true;
            log.info("autoUpload: Start");
            uploadTrader();
            uploadSaleVoucher();
            uploadPurchaseVoucher();
            uploadReturnInVoucher();
            uploadReturnOutVoucher();
            log.info("autoUpload: End");
            syncing = false;
        }
    }

    private void uploadTrader() {
        List<Trader> traders = traderRepo.unUploadTrader();
        if (!traders.isEmpty()) {
            log.info(String.format("uploadTrader: %s", traders.size()));
            traders.forEach(t -> listener.sendTrader(t.getTraderCode()));
        }
    }

    private void uploadSaleVoucher() {
        List<SaleHis> vouchers = saleHisRepo.unUploadVoucher(Util1.toDate(syncDate));
        if (!vouchers.isEmpty()) {
            log.info(String.format("uploadSaleVoucher: %s", vouchers.size()));
            vouchers.forEach(vou -> listener.sendSaleVoucherToAccount(vou.getVouNo()));
        }
    }

    private void uploadPurchaseVoucher() {
        List<PurHis> vouchers = purHisRepo.unUploadVoucher(Util1.toDate(syncDate));
        if (!vouchers.isEmpty()) {
            log.info(String.format("uploadPurchaseVoucher: %s", vouchers.size()));
            vouchers.forEach(vou -> listener.sendPurchaseVoucherToAccount(vou.getVouNo()));
        }
    }

    private void uploadReturnInVoucher() {
        List<RetInHis> vouchers = returnInRepo.unUploadVoucher(Util1.toDate(syncDate));
        if (!vouchers.isEmpty()) {
            log.info(String.format("uploadReturnInVoucher: %s", vouchers.size()));
            vouchers.forEach(vou -> listener.sendReturnInVoucherToAccount(vou.getVouNo()));
        }
    }

    private void uploadReturnOutVoucher() {
        List<RetOutHis> vouchers = returnOutRepo.unUploadVoucher(Util1.toDate(syncDate));
        if (!vouchers.isEmpty()) {
            log.info(String.format("uploadReturnOutVoucher: %s", vouchers.size()));
            vouchers.forEach(vou -> listener.sendReturnOutVoucherToAccount(vou.getVouNo()));
        }
    }
}
