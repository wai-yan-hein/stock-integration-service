package com.cv.integration.listener;

import com.cv.integration.common.Util1;
import com.cv.integration.entity.*;
import com.cv.integration.model.AccTrader;
import com.cv.integration.model.Gl;
import com.cv.integration.repo.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Session;
import java.text.DateFormat;
import java.util.*;

@Component
@Slf4j
@RequiredArgsConstructor
@PropertySource("file:config/application.properties")
public class StockMessageListener {
    private static final String LISTEN_QUEUE = "STOCK_QUEUE";
    private static final String ACC_QUEUE = "ACCOUNT_QUEUE";
    private final Gson gson = new GsonBuilder()
            .serializeNulls()
            .setDateFormat(DateFormat.FULL, DateFormat.FULL)
            .create();
    @Autowired
    private final SaleHisRepo saleHisRepo;
    @Autowired
    private final PurHisRepo purHisRepo;
    @Autowired
    private final ReturnInRepo returnInRepo;
    @Autowired
    private final ReturnOutRepo returnOutRepo;
    @Autowired
    private final TraderRepo traderRepo;
    @Autowired
    private final JmsTemplate jmsTemplate;
    @Value("${account.compcode}")
    private String compCode;
    private final String ACK = "ACK";
    private final String appName = "SM";
    private final Integer macId = 98;
    @Autowired
    private final HashMap<String, AccountSetting> hmAccSetting;


    private void sendMessage(String entity, String option, String data) {
        MessageCreator mc = (Session session) -> {
            MapMessage mm = session.createMapMessage();
            mm.setString("SENDER_QUEUE", LISTEN_QUEUE);
            mm.setString("ENTITY", entity);
            mm.setString("OPTION", option);
            mm.setString("DATA", data);
            return mm;
        };
        log.info(String.format("sendMessage: %s", option));
        jmsTemplate.send(ACC_QUEUE, mc);
    }

    @JmsListener(destination = LISTEN_QUEUE)
    public void receivedMessage(final MapMessage message) throws JMSException {
        String entity = message.getString("ENTITY");
        String code = message.getString("CODE");
        log.info(String.format("receiveMessage: %s", entity));
        switch (entity) {
            case "TRADER" -> sendTrader(code);
            case "SALE" -> sendSaleVoucherToAccount(code);
            case "PURCHASE" -> sendPurchaseVoucherToAccount(code);
            case "RETURN_IN" -> sendReturnInVoucherToAccount(code);
            case "RETURN_OUT" -> sendReturnOutVoucherToAccount(code);
            case "ACK_TRADER" -> updateTrader(code);
            case "ACK_SALE" -> updateSale(code);
            case "ACK_PURCHASE" -> updatePurchase(code);
            case "ACK_RETURN_IN" -> updateReturnIn(code);
            case "ACK_RETURN_OUT" -> updateReturnOut(code);
            default -> log.error("Unexpected value: " + message.getString("ENTITY"));
        }
    }

    public void sendTrader(String traderCode) {
        Optional<Trader> trader = traderRepo.findById(traderCode);
        if (trader.isPresent()) {
            Trader c = trader.get();
            String traderType = c.getTraderType();
            AccTrader accTrader = new AccTrader();
            accTrader.setTraderCode(c.getTraderCode());
            accTrader.setTraderName(c.getTraderName());
            accTrader.setUserCode(c.getUserCode());
            accTrader.setActive(true);
            accTrader.setCompCode(compCode);
            accTrader.setAppName(appName);
            accTrader.setMacId(macId);
            switch (traderType) {
                case "CUS" -> accTrader.setDiscriminator("C");
                case "SUP" -> accTrader.setDiscriminator("S");
                default -> accTrader.setDiscriminator("D");
            }

            String data = gson.toJson(accTrader);
            sendMessage("TRADER", "TRADER", data);
        }
    }

    private void updateTrader(String traderCode) {
        Optional<Trader> trader = traderRepo.findById(traderCode);
        if (trader.isPresent()) {
            Trader t = trader.get();
            t.setIntgUpdStatus(ACK);
            traderRepo.save(t);
            log.info(String.format("updateTrader: %s", traderCode));
        }
    }

    public void sendSaleVoucherToAccount(String vouNo) {
        String tranSource = "SALE";
        AccountSetting setting = hmAccSetting.get(tranSource);
        Optional<SaleHis> saleHis = saleHisRepo.findById(vouNo);
        if (saleHis.isPresent()) {
            SaleHis sh = saleHis.get();
            String srcAcc = setting.getSourceAcc();
            String payAcc = setting.getPayAcc();
            String disAcc = setting.getDiscountAcc();
            String balAcc = setting.getBalanceAcc();
            String taxAcc = setting.getTaxAcc();
            String deptCode = setting.getDeptCode();
            Date vouDate = sh.getVouDate();
            String traderCode = sh.getTraderCode();
            String curCode = sh.getCurCode();
            String remark = sh.getRemark();
            boolean deleted = sh.getDeleted();
            double vouBal = Util1.getDouble(sh.getBalance());
            double vouDis = Util1.getDouble(sh.getDiscount());
            double vouPaid = Util1.getDouble(sh.getPaid());
            double vouTax = Util1.getDouble(sh.getTaxAmt());
            double vouTotal = Util1.getDouble(sh.getVouTotal());
            double taxPercent = Util1.getDouble(sh.getTaxPercent());
            List<Gl> listGl = new ArrayList<>();
            //income
            if (vouBal > 0) {
                Gl gl = new Gl();
                gl.setGlDate(vouDate);
                gl.setDescription("Sale Voucher Balance");
                gl.setSrcAccCode(srcAcc);
                gl.setAccCode(balAcc);
                gl.setTraderCode(traderCode);
                gl.setCrAmt(vouBal);
                gl.setCurCode(curCode);
                gl.setReference(remark);
                gl.setDeptCode(deptCode);
                gl.setCompCode(compCode);
                gl.setCreatedDate(Util1.getTodayDate());
                gl.setCreatedBy(appName);
                gl.setTranSource(tranSource);
                gl.setRefNo(vouNo);
                gl.setDeleted(deleted);
                gl.setMacId(macId);
                listGl.add(gl);
            }
            //discount
            if (vouDis > 0) {
                Gl gl = new Gl();
                if (vouPaid > 0) {
                    gl.setSrcAccCode(payAcc);
                    gl.setCash(true);
                } else {
                    gl.setSrcAccCode(balAcc);
                    gl.setTraderCode(traderCode);
                }
                gl.setCrAmt(vouDis);
                gl.setAccCode(disAcc);
                gl.setGlDate(vouDate);
                gl.setDescription("Sale Voucher Discount");
                gl.setTraderCode(traderCode);
                gl.setCurCode(curCode);
                gl.setReference(remark);
                gl.setDeptCode(deptCode);
                gl.setCompCode(compCode);
                gl.setCreatedDate(Util1.getTodayDate());
                gl.setCreatedBy(appName);
                gl.setTranSource(tranSource);
                gl.setRefNo(vouNo);
                gl.setDeleted(deleted);
                gl.setMacId(macId);
                listGl.add(gl);
            }
            //payment
            if (vouPaid > 0) {
                Gl gl = new Gl();
                gl.setGlDate(vouDate);
                gl.setDescription("Sale Voucher Paid");
                gl.setSrcAccCode(payAcc);
                gl.setAccCode(srcAcc);
                gl.setDrAmt(vouTotal);
                gl.setCurCode(curCode);
                gl.setReference(remark);
                gl.setDeptCode(deptCode);
                gl.setCompCode(compCode);
                gl.setCreatedDate(Util1.getTodayDate());
                gl.setCreatedBy(appName);
                gl.setTranSource(tranSource);
                gl.setRefNo(vouNo);
                gl.setDeleted(deleted);
                gl.setMacId(macId);
                listGl.add(gl);
            }
            //tax
            if (vouTax > 0) {
                Gl gl = new Gl();
                if (vouPaid > 0) {
                    gl.setSrcAccCode(payAcc);
                    gl.setCash(true);
                } else {
                    gl.setSrcAccCode(balAcc);
                    gl.setTraderCode(traderCode);
                }
                gl.setAccCode(taxAcc);
                gl.setDrAmt(vouTax);
                gl.setGlDate(vouDate);
                gl.setDescription(String.format("Sale Voucher Tax (%s)", taxPercent));
                gl.setTraderCode(traderCode);
                gl.setCurCode(curCode);
                gl.setReference(remark);
                gl.setDeptCode(deptCode);
                gl.setCompCode(compCode);
                gl.setCreatedDate(Util1.getTodayDate());
                gl.setCreatedBy(appName);
                gl.setTranSource(tranSource);
                gl.setRefNo(vouNo);
                gl.setDeleted(deleted);
                gl.setMacId(macId);
                listGl.add(gl);
            }
            if (!listGl.isEmpty()) sendMessage("GL_LIST", tranSource, gson.toJson(listGl));

        } else {
            log.info(String.format("sendSaleVoucherToAccount: %s not found.", vouNo));
        }
    }

    private void updateSale(String vouNo) {
        Optional<SaleHis> saleHis = saleHisRepo.findById(vouNo);
        if (saleHis.isPresent()) {
            SaleHis sh = saleHis.get();
            sh.setIntgUpdStatus(ACK);
            saleHisRepo.save(sh);
            log.info(String.format("updateSale %s", vouNo));
        }
    }

    public void sendPurchaseVoucherToAccount(String vouNo) {
        String tranSource = "PURCHASE";
        AccountSetting setting = hmAccSetting.get(tranSource);
        Optional<PurHis> purHis = purHisRepo.findById(vouNo);
        if (purHis.isPresent()) {
            PurHis ph = purHis.get();
            String srcAcc = setting.getSourceAcc();
            String payAcc = setting.getPayAcc();
            String disAcc = setting.getDiscountAcc();
            String balAcc = setting.getBalanceAcc();
            String deptCode = setting.getDeptCode();
            Date vouDate = ph.getVouDate();
            String traderCode = ph.getTraderCode();
            String curCode = ph.getCurCode();
            String remark = ph.getRemark();
            boolean deleted = ph.getDeleted();
            double vouTotal = Util1.getDouble(ph.getVouTotal());
            double vouDis = Util1.getDouble(ph.getDiscount());
            double vouPaid = Util1.getDouble(ph.getPaid());
            double vouBal = Util1.getDouble(ph.getBalance());
            List<Gl> listGl = new ArrayList<>();
            //income
            if (vouBal > 0) {
                Gl gl = new Gl();
                gl.setGlDate(vouDate);
                gl.setDescription("Purchase Voucher Balance");
                gl.setSrcAccCode(srcAcc);
                gl.setAccCode(balAcc);
                gl.setTraderCode(traderCode);
                gl.setDrAmt(vouBal);
                gl.setCurCode(curCode);
                gl.setReference(remark);
                gl.setDeptCode(deptCode);
                gl.setCompCode(compCode);
                gl.setCreatedDate(Util1.getTodayDate());
                gl.setCreatedBy(appName);
                gl.setTranSource(tranSource);
                gl.setRefNo(vouNo);
                gl.setDeleted(deleted);
                gl.setMacId(macId);
                listGl.add(gl);
            }
            //discount
            if (vouDis > 0) {
                Gl gl = new Gl();
                if (vouPaid > 0) {
                    gl.setSrcAccCode(payAcc);
                    gl.setCash(true);
                } else {
                    gl.setSrcAccCode(balAcc);
                    gl.setTraderCode(traderCode);
                }
                gl.setAccCode(disAcc);
                gl.setDrAmt(vouDis);
                gl.setGlDate(vouDate);
                gl.setDescription("Purchase Voucher Discount");
                gl.setCurCode(curCode);
                gl.setReference(remark);
                gl.setDeptCode(deptCode);
                gl.setCompCode(compCode);
                gl.setCreatedDate(Util1.getTodayDate());
                gl.setCreatedBy(appName);
                gl.setTranSource(tranSource);
                gl.setRefNo(vouNo);
                gl.setDeleted(deleted);
                gl.setMacId(macId);
                listGl.add(gl);
            }
            //payment
            if (vouPaid > 0) {
                Gl gl = new Gl();
                gl.setGlDate(vouDate);
                gl.setDescription("Purchase Voucher Paid");
                gl.setSrcAccCode(payAcc);
                gl.setAccCode(srcAcc);
                gl.setCrAmt(vouTotal);
                gl.setCurCode(curCode);
                gl.setReference(remark);
                gl.setDeptCode(deptCode);
                gl.setCompCode(compCode);
                gl.setCreatedDate(Util1.getTodayDate());
                gl.setCreatedBy(appName);
                gl.setTranSource(tranSource);
                gl.setRefNo(vouNo);
                gl.setDeleted(deleted);
                gl.setMacId(macId);
                listGl.add(gl);
            }
            if (!listGl.isEmpty()) sendMessage("GL_LIST", tranSource, gson.toJson(listGl));

        } else {
            log.info(String.format("sendPurchaseVoucherToAccount: %s not found.", vouNo));
        }

    }

    private void updatePurchase(String vouNo) {
        Optional<PurHis> purHis = purHisRepo.findById(vouNo);
        if (purHis.isPresent()) {
            PurHis ph = purHis.get();
            ph.setIntgUpdStatus(ACK);
            purHisRepo.save(ph);
            log.info(String.format("updatePurchase %s", vouNo));
        }
    }

    public void sendReturnInVoucherToAccount(String vouNo) {
        String tranSource = "RETURN_IN";
        AccountSetting setting = hmAccSetting.get(tranSource);
        Optional<RetInHis> retInHis = returnInRepo.findById(vouNo);
        if (retInHis.isPresent()) {
            RetInHis ri = retInHis.get();
            String srcAcc = setting.getSourceAcc();
            String payAcc = setting.getPayAcc();
            String disAcc = setting.getDiscountAcc();
            String balAcc = setting.getBalanceAcc();
            String deptCode = setting.getDeptCode();
            Date vouDate = ri.getVouDate();
            String traderCode = ri.getTraderCode();
            String curCode = ri.getCurCode();
            String remark = ri.getRemark();
            boolean deleted = ri.getDeleted();
            List<Gl> listGl = new ArrayList<>();
            //income
            if (Util1.getDouble(ri.getVouTotal()) > 0) {
                Gl gl = new Gl();
                gl.setGlDate(vouDate);
                gl.setDescription("Return In Voucher Balance");
                gl.setSrcAccCode(srcAcc);
                gl.setAccCode(balAcc);
                gl.setTraderCode(traderCode);
                gl.setDrAmt(Util1.getDouble(ri.getVouTotal()));
                gl.setCrAmt(0.0);
                gl.setCurCode(curCode);
                gl.setReference(remark);
                gl.setDeptCode(deptCode);
                gl.setCompCode(compCode);
                gl.setCreatedDate(Util1.getTodayDate());
                gl.setCreatedBy(appName);
                gl.setTranSource(tranSource);
                gl.setRefNo(vouNo);
                gl.setDeleted(deleted);
                gl.setMacId(macId);
                listGl.add(gl);
            }
            //discount
            if (Util1.getDouble(ri.getDiscount()) > 0) {
                Gl gl = new Gl();
                gl.setGlDate(vouDate);
                gl.setDescription("Return In Voucher Discount");
                gl.setSrcAccCode(disAcc);
                gl.setAccCode(balAcc);
                gl.setTraderCode(traderCode);
                gl.setDrAmt(Util1.getDouble(ri.getDiscount()));
                gl.setCrAmt(0.0);
                gl.setCurCode(curCode);
                gl.setReference(remark);
                gl.setDeptCode(deptCode);
                gl.setCompCode(compCode);
                gl.setCreatedDate(Util1.getTodayDate());
                gl.setCreatedBy(appName);
                gl.setTranSource(tranSource);
                gl.setRefNo(vouNo);
                gl.setDeleted(deleted);
                gl.setMacId(macId);
                listGl.add(gl);
            }
            //payment
            if (Util1.getDouble(ri.getPaid()) > 0) {
                Gl gl = new Gl();
                gl.setGlDate(vouDate);
                gl.setDescription("Return In Voucher Paid");
                gl.setSrcAccCode(payAcc);
                gl.setAccCode(balAcc);
                gl.setTraderCode(traderCode);
                gl.setCrAmt(Util1.getDouble(ri.getPaid()));
                gl.setDrAmt(0.0);
                gl.setCurCode(curCode);
                gl.setReference(remark);
                gl.setDeptCode(deptCode);
                gl.setCompCode(compCode);
                gl.setCreatedDate(Util1.getTodayDate());
                gl.setCreatedBy(appName);
                gl.setTranSource(tranSource);
                gl.setRefNo(vouNo);
                gl.setDeleted(deleted);
                gl.setMacId(macId);
                listGl.add(gl);
            }
            if (!listGl.isEmpty()) sendMessage("GL_LIST", tranSource, gson.toJson(listGl));

        } else {
            log.info(String.format("sendReturnInVoucherToAccount: %s not found.", vouNo));
        }


    }

    private void updateReturnIn(String vouNo) {
        Optional<RetInHis> retInHis = returnInRepo.findById(vouNo);
        if (retInHis.isPresent()) {
            RetInHis ri = retInHis.get();
            ri.setIntgUpdStatus(ACK);
            returnInRepo.save(ri);
            log.info(String.format("updateReturnIn %s", vouNo));
        }
    }

    public void sendReturnOutVoucherToAccount(String vouNo) {
        String tranSource = "RETURN_OUT";
        AccountSetting setting = hmAccSetting.get(tranSource);
        Optional<RetOutHis> retOutHis = returnOutRepo.findById(vouNo);
        if (retOutHis.isPresent()) {
            RetOutHis ro = retOutHis.get();
            String srcAcc = setting.getSourceAcc();
            String payAcc = setting.getPayAcc();
            String disAcc = setting.getDiscountAcc();
            String balAcc = setting.getBalanceAcc();
            String deptCode = setting.getDeptCode();
            Date vouDate = ro.getVouDate();
            String traderCode = ro.getTraderCode();
            String curCode = ro.getCurCode();
            String remark = ro.getRemark();
            boolean deleted = ro.getDeleted();
            List<Gl> listGl = new ArrayList<>();
            //income
            if (Util1.getDouble(ro.getVouTotal()) > 0) {
                Gl gl = new Gl();
                gl.setGlDate(vouDate);
                gl.setDescription("Return Out Voucher Balance");
                gl.setSrcAccCode(srcAcc);
                gl.setAccCode(balAcc);
                gl.setTraderCode(traderCode);
                gl.setCrAmt(Util1.getDouble(ro.getVouTotal()));
                gl.setDrAmt(0.0);
                gl.setCurCode(curCode);
                gl.setReference(remark);
                gl.setDeptCode(deptCode);
                gl.setCompCode(compCode);
                gl.setCreatedDate(Util1.getTodayDate());
                gl.setCreatedBy(appName);
                gl.setTranSource(tranSource);
                gl.setRefNo(vouNo);
                gl.setDeleted(deleted);
                gl.setMacId(macId);
                listGl.add(gl);
            }
            //discount
            if (Util1.getDouble(ro.getDiscount()) > 0) {
                Gl gl = new Gl();
                gl.setGlDate(vouDate);
                gl.setDescription("Return Out Voucher Discount");
                gl.setSrcAccCode(disAcc);
                gl.setAccCode(balAcc);
                gl.setTraderCode(traderCode);
                gl.setDrAmt(Util1.getDouble(ro.getDiscount()));
                gl.setCrAmt(0.0);
                gl.setCurCode(curCode);
                gl.setReference(remark);
                gl.setDeptCode(deptCode);
                gl.setCompCode(compCode);
                gl.setCreatedDate(Util1.getTodayDate());
                gl.setCreatedBy(appName);
                gl.setTranSource(tranSource);
                gl.setRefNo(vouNo);
                gl.setDeleted(deleted);
                gl.setMacId(macId);
                listGl.add(gl);
            }
            //payment
            if (Util1.getDouble(ro.getPaid()) > 0) {
                Gl gl = new Gl();
                gl.setGlDate(vouDate);
                gl.setDescription("Return Out Voucher Paid");
                gl.setSrcAccCode(payAcc);
                gl.setAccCode(balAcc);
                gl.setTraderCode(traderCode);
                gl.setDrAmt(Util1.getDouble(ro.getPaid()));
                gl.setCrAmt(0.0);
                gl.setCurCode(curCode);
                gl.setReference(remark);
                gl.setDeptCode(deptCode);
                gl.setCompCode(compCode);
                gl.setCreatedDate(Util1.getTodayDate());
                gl.setCreatedBy(appName);
                gl.setTranSource(tranSource);
                gl.setRefNo(vouNo);
                gl.setDeleted(deleted);
                gl.setMacId(macId);
                listGl.add(gl);
            }
            if (!listGl.isEmpty()) sendMessage("GL_LIST", tranSource, gson.toJson(listGl));
        } else {
            log.info(String.format("sendReturnOutVoucherToAccount: %s not found.", vouNo));
        }
    }

    private void updateReturnOut(String vouNo) {
        Optional<RetOutHis> retOutHis = returnOutRepo.findById(vouNo);
        if (retOutHis.isPresent()) {
            RetOutHis ro = retOutHis.get();
            ro.setIntgUpdStatus(ACK);
            returnOutRepo.save(ro);
            log.info(String.format("updateReturnOut %s", vouNo));
        }
    }


}
