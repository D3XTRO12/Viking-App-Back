package com.ElVikingoStore.Viking_App.Repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ElVikingoStore.Viking_App.Models.DiagnosticPoint;

@Repository
public interface DiagnosticPointRepo extends JpaRepository<DiagnosticPoint, Long> {
    List<DiagnosticPoint> findByWorkOrder_Id(Long workOrderId);
}