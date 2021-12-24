package com.cv.integration.repo;

import com.cv.integration.entity.PurHis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;

public interface PurHisRepo extends JpaRepository<PurHis, String> {
    @Query("select o from PurHis o where o.intgUpdStatus is null and date(o.vouDate) >= :vou_date")
    List<PurHis> unUploadVoucher(@Param("vou_date") Date date);
}
