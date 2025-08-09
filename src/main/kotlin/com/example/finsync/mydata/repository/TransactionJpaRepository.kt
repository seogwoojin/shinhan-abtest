package com.example.finsync.mydata.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface TransactionJpaRepository : JpaRepository<UserTransactionInfo, Long> {

    /**
     * 특정 사용자의 특정 기간 거래 내역 조회
     */
    @Query(
        """
        SELECT t FROM UserTransactionInfo t 
        WHERE t.username = :username 
        AND SUBSTRING(t.tranDtime, 1, 10) BETWEEN :startDate AND :endDate
        ORDER BY t.tranDtime DESC
    """
    )
    fun findByUsernameAndDateRange(
        username: String,
        startDate: String,
        endDate: String
    ): List<UserTransactionInfo>

    /**
     * 특정 사용자의 특정 날짜 거래 내역 조회
     */
    @Query(
        """
        SELECT t FROM UserTransactionInfo t 
        WHERE t.username = :username 
        AND SUBSTRING(t.tranDtime, 1, 10) = :date
        ORDER BY t.tranDtime DESC
    """
    )
    fun findByUsernameAndDate(
        username: String,
        date: String
    ): List<UserTransactionInfo>

    /**
     * 특정 사용자의 모든 거래 내역 조회 (최신순)
     */
    fun findByUsernameOrderByTranDtimeDesc(username: String): List<UserTransactionInfo>

    /**
     * 특정 사용자의 특정 월 거래 건수 조회
     */
    @Query(
        """
        SELECT COUNT(t) FROM UserTransactionInfo t 
        WHERE t.username = :username 
        AND SUBSTRING(t.tranDtime, 1, 7) = :yearMonth
    """
    )
    fun countByUsernameAndYearMonth(
        username: String,
        yearMonth: String // "YYYY-MM" 형식
    ): Long

    /**
     * 특정 사용자의 특정 월 거래 내역 조회
     */
    @Query(
        """
        SELECT t FROM UserTransactionInfo t 
        WHERE t.username = :username 
        AND SUBSTRING(t.tranDtime, 1, 7) = :yearMonth
        ORDER BY t.tranDtime DESC
    """
    )
    fun findByUsernameAndYearMonth(
        username: String,
        yearMonth: String // "YYYY-MM" 형식
    ): List<UserTransactionInfo>
}