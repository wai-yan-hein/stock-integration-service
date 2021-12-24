package com.cv.integration.repo;

import com.cv.integration.entity.SaleHis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;

public interface SaleHisRepo extends JpaRepository<SaleHis, String> {
    @Query("select o from SaleHis o where o.intgUpdStatus is null and o.vouDate >= :vou_date")
    List<SaleHis> unUploadVoucher(@Param("vou_date") Date date);
}
